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
import com.volmit.adapt.api.adaptation.SimpleAdaptation;
import com.volmit.adapt.api.recipe.type.Shapeless;
import com.volmit.adapt.api.world.AdaptPlayer;
import com.volmit.adapt.content.event.AdaptAdaptationTeleportEvent;
import com.volmit.adapt.content.item.BoundEyeOfEnder;
import com.volmit.adapt.util.*;
import lombok.NoArgsConstructor;
import org.bukkit.*;
import org.bukkit.Color;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.UUID;

import static com.volmit.adapt.api.adaptation.chunk.ChunkLoading.loadChunkAsync;

public class RiftGate extends SimpleAdaptation<RiftGate.Config> {
    public RiftGate() {
        super("rift-gate");
        registerConfiguration(Config.class);
        setDescription(Localizer.component("rift", "gate", "description"));
        setDisplayName(Localizer.component("rift", "gate", "name"));
        setIcon(Material.END_PORTAL_FRAME);
        setBaseCost(0);
        setCostFactor(0);
        setMaxLevel(1);
        setInitialCost(30);
        setInterval(1322);
        registerRecipe(Shapeless.builder().key("rift-recall-gate").ingredient(Material.ENDER_PEARL)
                .ingredient(Material.AMETHYST_SHARD).ingredient(Material.EMERALD)
                .result(BoundEyeOfEnder.io.withData(new BoundEyeOfEnder.Data(null))).build());
    }

    @Override
    public void addStats(int level, Element v) {
        v.addLore(Localizer.components("rift", "gate", "lore"));
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void on(PlayerInteractEvent e) {
        if (e.isCancelled()) {
            return;
        }
        Player p = e.getPlayer();
        ItemStack hand = p.getInventory().getItemInMainHand();
        ItemStack offHand = p.getInventory().getItemInOffHand();
        Location location = e.getClickedBlock() == null ? p.getLocation() : e.getClickedBlock().getLocation();

        // Deny usage if the offhand contains a bindable item
        if (BoundEyeOfEnder.isBindableItem(offHand) && e.getHand() != null
                && e.getHand().equals(EquipmentSlot.OFF_HAND)) {
            e.setCancelled(true);
            return;
        }

        if (p.getInventory().getItemInMainHand().getType().equals(Material.ENDER_EYE)
                && !p.hasCooldown(Material.ENDER_EYE) && hasAdaptation(p) && BoundEyeOfEnder.isBindableItem(hand)) {

            e.setCancelled(true);
            Adapt.verbose(" - Player Main hand: " + hand.getType());
            switch (e.getAction()) {
                case LEFT_CLICK_BLOCK, LEFT_CLICK_AIR -> {
                    if (p.isSneaking()) {
                        Adapt.verbose("Linking eye");
                        linkEye(p, location);
                    }
                }
                case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> // use
                {
                    if (isBound(hand)) {
                        openEye(p);
                    }
                }
            }
        }
    }

    private void handleEyeOfEnderInteraction(PlayerInteractEvent event, Player player, Block block) {
        boolean sneaking = player.isSneaking();
        ItemStack mainHand = player.getInventory().getItemInMainHand();
        Location location = block == null ? player.getLocation() : block.getLocation();

        switch (event.getAction()) {
            case LEFT_CLICK_BLOCK, LEFT_CLICK_AIR -> {
                if (sneaking) {
                    if (isBound(mainHand)) {
                        unlinkEye(player);
                    } else {
                        linkEye(player, location);
                    }
                }
            }
            case RIGHT_CLICK_AIR, RIGHT_CLICK_BLOCK -> {
                if (isBound(mainHand)) {
                    openEye(player);
                }
            }
            default -> {
            }
        }
    }

    private boolean isBound(ItemStack stack) {
        return stack.getType().equals(Material.ENDER_EYE) && BoundEyeOfEnder.getLocation(stack) != null;
    }

    private void unlinkEye(Player p) {
        ItemStack hand = p.getInventory().getItemInMainHand();
        decrementItemstack(hand, p);
        ItemStack eye = new ItemStack(Material.ENDER_EYE);
        p.getInventory().addItem(eye).values().forEach(i -> p.getWorld().dropItemNaturally(p.getLocation(), i));
    }

    private void linkEye(Player p, Location location) {
        if (getConfig().showParticles) {
            vfxCuboidOutline(location.getBlock(), location.add(0, 1, 0).getBlock(), Particle.REVERSE_PORTAL);
        }
        SoundPlayer sp = SoundPlayer.of(p);
        sp.play(p.getLocation(), Sound.ENTITY_ENDER_EYE_DEATH, 0.50f, 0.22f);
        ItemStack hand = p.getInventory().getItemInMainHand();

        if (hand.getAmount() == 1) {
            BoundEyeOfEnder.setData(hand, location);
        } else {
            hand.setAmount(hand.getAmount() - 1);
            ItemStack eye = BoundEyeOfEnder.withData(location);
            p.getInventory().addItem(eye).values().forEach(i -> p.getWorld().dropItemNaturally(p.getLocation(), i));
        }
    }

    private void openEye(Player p) {
        Adapt.verbose("Using eye");
        SoundPlayer sp = SoundPlayer.of(p);
        Location l = BoundEyeOfEnder.getLocation(p.getInventory().getItemInMainHand());
        ItemStack hand = p.getInventory().getItemInMainHand();
        int eyeSlot = p.getInventory().getHeldItemSlot();
        ItemStack expectedEye = hand.clone();
        expectedEye.setAmount(1);
        AdaptPlayer expectedPlayer = getPlayer(p);

        if (!getConfig().consumeOnUse) {
            if (p.getCooldown(Material.ENDER_EYE) > 0) {
                sp.play(p.getLocation(), Sound.BLOCK_REDSTONE_TORCH_BURNOUT, 1, 1);
                return;
            }
        }
        p.setCooldown(Material.ENDER_EYE, 150);

        if (RiftResist.hasRiftResistPerk(expectedPlayer)) {
            RiftResist.riftResistStackAdd(p, 150, 3);
        }

        p.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 100, 10, true, false, false));
        p.addPotionEffect(new PotionEffect(PotionEffectType.LEVITATION, 85, 0, true, false, false));
        if (l != null) {
            sp.play(l, Sound.BLOCK_LODESTONE_PLACE, 1f, 0.1f);
            sp.play(l, Sound.BLOCK_BELL_RESONATE, 1f, 0.1f);
        }

        Color color = Color.fromBGR(0, 0, 0);
        vfxFastRing(p.getLocation(), 2.0, color);
        new BukkitRunnable() {
            private int ticks;
            private double radius = 2.0;
            private double height;

            @Override
            public void run() {
                if (!p.isOnline() || ticks++ >= 80) {
                    cancel();
                    return;
                }
                height += 0.02;
                radius *= 0.9;
                vfxFastRing(p.getLocation().add(0, height, 0), radius, color);
            }
        }.runTaskTimer(Adapt.instance, 1L, 1L);
        vfxLevelUp(p);
        sp.play(p.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 5.35f, 0.1f);
        UUID playerId = p.getUniqueId();
        UUID targetWorldId = l == null || l.getWorld() == null ? null : l.getWorld().getUID();
        double targetX = l == null ? 0 : l.getX();
        double targetY = l == null ? 0 : l.getY();
        double targetZ = l == null ? 0 : l.getZ();
        float targetYaw = l == null ? 0 : l.getYaw();
        float targetPitch = l == null ? 0 : l.getPitch();
        J.s(() -> {
            Player online = Bukkit.getPlayer(playerId);
            World targetWorld = targetWorldId == null ? null : Bukkit.getWorld(targetWorldId);
            if (online == null || !online.isOnline() || targetWorld == null
                    || !Adapt.instance.getAdaptServer().isPlayerLoaded(playerId)
                    || !Adapt.instance.getAdaptServer().isCurrentPlayer(playerId, expectedPlayer)) {
                return;
            }
            Location target = new Location(targetWorld, targetX, targetY, targetZ, targetYaw, targetPitch);
            loadChunkAsync(target, chunk -> {
                Player current = Bukkit.getPlayer(playerId);
                World currentTargetWorld = Bukkit.getWorld(targetWorldId);
                if (current == null || !current.isOnline() || currentTargetWorld == null
                        || !Adapt.instance.getAdaptServer().isPlayerLoaded(playerId)
                        || !Adapt.instance.getAdaptServer().isCurrentPlayer(playerId, expectedPlayer)) {
                    return;
                }
                Location loadedTarget = target.clone();
                loadedTarget.setWorld(currentTargetWorld);
                if (getConfig().consumeOnUse && !matchesEye(current, eyeSlot, expectedEye)) {
                    return;
                }
                AdaptAdaptationTeleportEvent event = new AdaptAdaptationTeleportEvent(!Bukkit.isPrimaryThread(),
                        expectedPlayer, this, current.getLocation(), loadedTarget);
                Bukkit.getPluginManager().callEvent(event);
                if (event.isCancelled()) {
                    return;
                }
                if (getConfig().consumeOnUse && !matchesEye(current, eyeSlot, expectedEye)) {
                    return;
                }

                if (!current.teleport(loadedTarget, PlayerTeleportEvent.TeleportCause.PLUGIN)) {
                    return;
                }
                if (getConfig().consumeOnUse && matchesEye(current, eyeSlot, expectedEye)) {
                    ItemStack eye = current.getInventory().getItem(eyeSlot);
                    if (eye.getAmount() > 1) {
                        eye.setAmount(eye.getAmount() - 1);
                    } else {
                        current.getInventory().setItem(eyeSlot, null);
                    }
                    xp(current, 75);
                }
                vfxLevelUp(current);
                SoundPlayer.of(current).play(current.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 5.35f, 0.1f);
            });
        }, 85);
    }

    private boolean matchesEye(Player player, int slot, ItemStack expectedEye) {
        ItemStack eye = player.getInventory().getItem(slot);
        return eye != null && eye.getAmount() > 0 && eye.isSimilar(expectedEye);
    }

    @Override
    public void onTick() {
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
        boolean consumeOnUse = true;
        boolean showParticles = true;
    }
}
