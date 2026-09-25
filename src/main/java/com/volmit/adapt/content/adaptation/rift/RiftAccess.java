/*------------------------------------------------------------------------------
-   Adapt is a Skill/Integration plugin  for Minecraft Bukkit Servers
-   Copyright (c) 2022 Arcane Arts (Volmit Software)
-
-   This program is free software: you can redistribute it and/or modify
-   it under the terms of the GNU General Public License as published by
-   the Free Software Foundation, either version 3 of the License, or
-   (at your option) any later version.
-
-   This program is distributed in the hope that it will be useful,
-   but WITHOUT ANY WARRANTY; without even the implied warranty of
-   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
-   GNU General Public License for more details.
-
-   You should have received a copy of the GNU General Public License
-   along with this program.  If not, see <https://www.gnu.org/licenses/>.
-----------------------------------------------------------------------------*/

package com.volmit.adapt.content.adaptation.rift;

import com.volmit.adapt.Adapt;
import com.volmit.adapt.AdaptConfig;
import com.volmit.adapt.api.adaptation.SimpleAdaptation;
import com.volmit.adapt.api.recipe.type.Shapeless;
import com.volmit.adapt.api.world.AdaptPlayer;
import com.volmit.adapt.content.item.BoundEnderPearl;
import com.volmit.adapt.util.*;
import lombok.NoArgsConstructor;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.*;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.world.WorldUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.volmit.adapt.api.adaptation.chunk.ChunkLoading.loadChunkAsync;

public class RiftAccess extends SimpleAdaptation<RiftAccess.Config> {
    private final Map<StorageKey, ActiveViews> activeViewsMap = new HashMap<>();
    private final Map<ChunkPos, Integer> tickets = new HashMap<>();

    public RiftAccess() {
        super("rift-access");
        registerConfiguration(Config.class);
        setDescription(Localizer.component("rift", "remoteaccess", "description"));
        setDisplayName(Localizer.component("rift", "remoteaccess", "name"));
        setMaxLevel(1);
        setIcon(Material.NETHER_STAR);
        setBaseCost(getConfig().baseCost);
        setCostFactor(getConfig().costFactor);
        setInitialCost(getConfig().initialCost);
        setInterval(1000);
        registerRecipe(Shapeless.builder().key("rift-remote-access").ingredient(Material.ENDER_PEARL)
                .ingredient(Material.COMPASS).result(BoundEnderPearl.io.withData(new BoundEnderPearl.Data(null)))
                .build());
    }

    @Override
    public void addStats(int level, Element v) {
        v.addLore(Localizer.components("rift", "remoteaccess", "lore"));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void on(PlayerInteractEvent e) {
        if (e.isCancelled()) {
            return;
        }
        Player p = e.getPlayer();
        ItemStack mainHand = p.getInventory().getItemInMainHand();
        ItemStack offHand = p.getInventory().getItemInOffHand();
        Block block = e.getClickedBlock();

        boolean mainHandBound = BoundEnderPearl.isBindableItem(mainHand);
        boolean offHandBound = BoundEnderPearl.isBindableItem(offHand);

        // Cancel event if the enderpearl is in the offhand
        if (offHandBound && e.getHand() != null && e.getHand().equals(EquipmentSlot.OFF_HAND)) {
            e.setCancelled(true);
            return;
        }

        // If the main hand is holding a bound enderpearl
        if (mainHandBound) {
            e.setCancelled(true);
            if (hasAdaptation(p)) {
                Adapt.verbose("Player using bound enderpearl.");
                handleEnderPearlInteraction(e, p, block);
            }
        }
    }

    private void handleEnderPearlInteraction(PlayerInteractEvent event, Player player, Block block) {
        boolean canUseInCreative = AdaptConfig.get().allowAdaptationsInCreative;
        boolean isCreative = player.getGameMode() == GameMode.CREATIVE;
        boolean sneaking = player.isSneaking();
        boolean allowed = canUseInCreative || !isCreative;

        // Check if the player is allowed to use the bound item in creative
        if (!allowed) {
            Adapt.info("Player " + player.getName() + " tried to use the bound item in creative mode.");
            return;
        }

        switch (event.getAction()) {
            case LEFT_CLICK_BLOCK -> {
                // If player is sneaking and left-clicking a container
                if (sneaking && isStorage(block.getBlockData())) {
                    if (canAccessChest(player, block.getLocation())) {
                        linkPearl(player, block, event);
                    } else {
                        Adapt.verbose("Player " + player.getName() + " doesn't have permission.");
                    }
                }
            }
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK ->
                // If player right-clicks on air or any block
                openPearl(player);
            default -> {
            }
        }
    }

    private void linkPearl(Player p, Block block, PlayerInteractEvent event) {
        event.setCancelled(true);
        if (getConfig().showParticles) {
            vfxCuboidOutline(block, Particle.REVERSE_PORTAL);
        }
        ItemStack hand = p.getInventory().getItemInMainHand();
        SoundPlayer sp = SoundPlayer.of(p);
        sp.play(p.getLocation(), Sound.BLOCK_ENDER_CHEST_CLOSE, 0.5f, 0.8f);

        if (hand.getAmount() == 1) {
            BoundEnderPearl.setData(hand, block);
        } else {
            hand.setAmount(hand.getAmount() - 1);
            ItemStack pearl = BoundEnderPearl.withData(block);
            p.getInventory().addItem(pearl).values().forEach(i -> p.getWorld().dropItemNaturally(p.getLocation(), i));
        }
    }

    private void openPearl(Player p) {
        SoundPlayer sp = SoundPlayer.of(p);
        Block b = BoundEnderPearl.getBlock(p.getInventory().getItemInMainHand());
        if (b == null || !canAccessChest(p, b.getLocation())) {
            sp.play(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
            return;
        }
        UUID playerId = p.getUniqueId();
        AdaptPlayer expectedPlayer = getPlayer(p);
        UUID worldId = b.getWorld().getUID();
        int x = b.getX();
        int y = b.getY();
        int z = b.getZ();
        loadChunkAsync(b.getLocation(), chunk -> {
            Player current = Bukkit.getPlayer(playerId);
            World world = Bukkit.getWorld(worldId);
            if (isUnregistered() || current == null || !current.isOnline() || world == null
                    || !chunk.getWorld().equals(world) || chunk.getX() != x >> 4 || chunk.getZ() != z >> 4
                    || !Adapt.instance.getAdaptServer().isPlayerLoaded(playerId)
                    || !Adapt.instance.getAdaptServer().isCurrentPlayer(playerId, expectedPlayer)) {
                return;
            }
            Block currentBlock = world.getBlockAt(x, y, z);
            if (!canAccessChest(current, currentBlock.getLocation())) {
                SoundPlayer.of(current).play(current.getLocation(), Sound.BLOCK_NOTE_BLOCK_BASS, 1f, 1f);
                return;
            }
            if (!(currentBlock.getState() instanceof InventoryHolder holder)) {
                return;
            }
            InventoryView view = current.openInventory(holder.getInventory());
            if (view == null)
                return;
            activeViewsMap.compute(StorageKey.of(currentBlock), (location, active) -> {
                ActiveViews result = active == null
                        ? new ActiveViews(addTicket(new ChunkPos(worldId, x >> 4, z >> 4)), new ArrayList<>())
                        : active;
                result.views().add(view);
                return result;
            });
            SoundPlayer sounds = SoundPlayer.of(current);
            sounds.play(current.getLocation(), Sound.PARTICLE_SOUL_ESCAPE, 1f, 0.10f);
            sounds.play(current.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1f, 0.10f);
        });
    }

    @Override
    public boolean needsTicking() {
        return true;
    }

    @Override
    public void onTick() {
        checkActiveViews();
    }

    private void checkActiveViews() {
        Iterator<Map.Entry<StorageKey, ActiveViews>> mapIterator = activeViewsMap.entrySet().iterator();
        while (mapIterator.hasNext()) {
            Map.Entry<StorageKey, ActiveViews> entry = mapIterator.next();
            removeInvalidViews(entry);
            removeEntryIfViewsEmpty(mapIterator, entry);
        }
    }

    private void removeInvalidViews(Map.Entry<StorageKey, ActiveViews> entry) {
        List<InventoryView> views = entry.getValue().views();
        for (int ii = views.size() - 1; ii >= 0; ii--) {
            InventoryView i = views.get(ii);
            if (shouldRemoveView(i)) {
                views.remove(ii);
            }
        }
    }

    private boolean shouldRemoveView(InventoryView i) {
        Location location = i.getTopInventory().getLocation();
        return !i.getPlayer().getOpenInventory().equals(i)
                || (location == null || !isStorage(location.getBlock().getBlockData()));
    }

    private void removeEntryIfViewsEmpty(Iterator<Map.Entry<StorageKey, ActiveViews>> mapIterator,
            Map.Entry<StorageKey, ActiveViews> entry) {
        List<InventoryView> views = entry.getValue().views();
        if (views.isEmpty()) {
            mapIterator.remove();
            removeTicket(entry.getValue().chunk(), true);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void on(BlockBurnEvent event) {
        if (event.isCancelled())
            return;
        invClose(event.getBlock());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void on(BlockPistonRetractEvent event) {
        if (event.isCancelled())
            return;
        for (Block b : event.getBlocks()) {
            invClose(b);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void on(BlockPistonExtendEvent event) {
        if (event.isCancelled())
            return;
        for (Block b : event.getBlocks()) {
            invClose(b);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void on(BlockExplodeEvent event) {
        if (event.isCancelled())
            return;
        for (Block b : event.blockList()) {
            invClose(b);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void on(BlockBreakEvent event) {
        if (event.isCancelled())
            return;
        invClose(event.getBlock());
    }

    private void invClose(Block block) {
        ActiveViews active = activeViewsMap.remove(StorageKey.of(block));
        if (active != null) {
            closeViews(active.views());
            removeTicket(active.chunk(), true);
        }
    }

    @EventHandler
    public void on(WorldUnloadEvent event) {
        UUID worldId = event.getWorld().getUID();
        Iterator<Map.Entry<StorageKey, ActiveViews>> views = activeViewsMap.entrySet().iterator();
        while (views.hasNext()) {
            Map.Entry<StorageKey, ActiveViews> entry = views.next();
            if (entry.getKey().worldId().equals(worldId)) {
                ActiveViews active = entry.getValue();
                views.remove();
                closeViews(active.views());
                removeTicket(active.chunk(), false);
            }
        }

        Iterator<ChunkPos> chunks = tickets.keySet().iterator();
        while (chunks.hasNext()) {
            ChunkPos chunk = chunks.next();
            if (chunk.worldId().equals(worldId)) {
                event.getWorld().removePluginChunkTicket(chunk.x(), chunk.z(), Adapt.instance);
                chunks.remove();
            }
        }
    }

    @Override
    public void unregister() {
        for (ActiveViews active : activeViewsMap.values()) {
            closeViews(active.views());
            removeTicket(active.chunk(), false);
        }
        activeViewsMap.clear();
        for (ChunkPos chunk : tickets.keySet()) {
            World world = Bukkit.getWorld(chunk.worldId());
            if (world != null) {
                world.removePluginChunkTicket(chunk.x(), chunk.z(), Adapt.instance);
            }
        }
        tickets.clear();
        super.unregister();
    }

    private void closeViews(List<InventoryView> views) {
        for (InventoryView view : views) {
            view.getPlayer().closeInventory();
        }
    }

    @Override
    public boolean isEnabled() {
        return getConfig().enabled;
    }

    @Override
    public boolean isPermanent() {
        return getConfig().permanent;
    }

    @NoArgsConstructor
    protected static class Config {
        boolean permanent = false;
        boolean enabled = false;
        boolean showParticles = true;
        int baseCost = 3;
        double costFactor = 0.2;
        int initialCost = 15;
    }

    private ChunkPos addTicket(ChunkPos chunk) {
        World world = Bukkit.getWorld(chunk.worldId());
        if (world == null) {
            return chunk;
        }
        int count = tickets.getOrDefault(chunk, 0);
        tickets.put(chunk, count + 1);
        if (count == 0) {
            world.addPluginChunkTicket(chunk.x(), chunk.z(), Adapt.instance);
        }
        return chunk;
    }

    private void removeTicket(ChunkPos chunk, boolean requestUnload) {
        Integer count = tickets.get(chunk);
        if (count == null) {
            return;
        }
        if (count > 1) {
            tickets.put(chunk, count - 1);
            return;
        }

        tickets.remove(chunk);
        World world = Bukkit.getWorld(chunk.worldId());
        if (world != null) {
            world.removePluginChunkTicket(chunk.x(), chunk.z(), Adapt.instance);
            if (requestUnload) {
                world.unloadChunkRequest(chunk.x(), chunk.z());
            }
        }
    }

    private record StorageKey(UUID worldId, int x, int y, int z) {
        private static StorageKey of(Block block) {
            return new StorageKey(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
        }
    }

    private record ChunkPos(UUID worldId, int x, int z) {
    }

    private record ActiveViews(ChunkPos chunk, List<InventoryView> views) {
    }
}
