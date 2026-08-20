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

package com.volmit.adapt.content.adaptation.brewing;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;


import com.volmit.adapt.Adapt;
import com.volmit.adapt.api.adaptation.SimpleAdaptation;
import com.volmit.adapt.api.data.WorldData;
import com.volmit.adapt.content.matter.BrewingStandOwner;
import com.volmit.adapt.util.*;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.world.WorldUnloadEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class BrewingSuperHeated extends SimpleAdaptation<BrewingSuperHeated.Config> {

    private static final int MAX_CHECKS_BEFORE_REMOVE = 20;
    private final Map<Block, Integer> activeStands = new HashMap<>();
    private final Map<StandKey, BrewingStandOwner> standOwners = new HashMap<>();
    private final Map<StandKey, CompletableFuture<BrewingStandOwner>> ownerLookups = new HashMap<>();
    private final BrewingOwnerLevelCache ownerLevelCache = new BrewingOwnerLevelCache();

    public BrewingSuperHeated() {
        super("brewing-super-heated");
        registerConfiguration(Config.class);
        setDescription(Localizer.component("brewing", "superheated", "description"));
        setDisplayName(Localizer.component("brewing", "superheated", "name"));
        setIcon(Material.LAVA_BUCKET);
        setBaseCost(getConfig().baseCost);
        setCostFactor(getConfig().costFactor);
        setMaxLevel(getConfig().maxLevel);
        setInitialCost(getConfig().initialCost);
        setInterval(253);
    }

    @Override
    public void addStats(int level, Element v) {
        v.addLore(Components.mini("<green><amount><gray> <lore>",
                Placeholder.unparsed("amount", Form.pc(getFireBoost(getLevelPercent(level)), 0)),
                Placeholder.component("lore", Localizer.component("brewing", "superheated", "lore1"))));
        v.addLore(Components.mini("<green><amount><gray> <lore>",
                Placeholder.unparsed("amount", Form.pc(getLavaBoost(getLevelPercent(level)), 0)),
                Placeholder.component("lore", Localizer.component("brewing", "superheated", "lore2"))));
    }

    public double getLavaBoost(double factor) {
        return (getConfig().lavaMultiplier) * (getConfig().multiplierFactor * factor);
    }

    public double getFireBoost(double factor) {
        return (getConfig().fireMultiplier) * (getConfig().multiplierFactor * factor);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(InventoryMoveItemEvent e) {
        if (e.isCancelled()) {
            return;
        }
        if (!e.getDestination().getType().equals(InventoryType.BREWING)
                || e.getDestination().getLocation() == null) {
            return;
        }
        Block block = e.getDestination().getLocation().getBlock();
        J.s(() -> track(block));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(BrewEvent e) {
        if (e.isCancelled()) {
            return;
        }
        Block block = e.getBlock();
        J.s(() -> {
            if (block.getState() instanceof BrewingStand stand && stand.getBrewingTime() > 0) {
                track(block);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(InventoryClickEvent e) {
        if (e.getClickedInventory() == null || e.isCancelled()) {
            return;
        }
        if (e.getView().getTopInventory().getType().equals(InventoryType.BREWING)
                && e.getView().getTopInventory().getLocation() != null) {
            track(e.getView().getTopInventory().getLocation().getBlock());
        }
    }

    private void track(Block block) {
        if (activeStands.put(block, MAX_CHECKS_BEFORE_REMOVE) == null) {
            StandKey key = StandKey.of(block);
            standOwners.remove(key);
            ownerLookups.remove(key);
        }
    }

    @Override
    public boolean needsTicking() {
        return true;
    }

    @Override
    public void onTick() {
        if (activeStands.isEmpty()) {
            return;
        }

        long now = M.ms();
        Iterator<Map.Entry<Block, Integer>> it = activeStands.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Block, Integer> entry = it.next();
            StandKey key = StandKey.of(entry.getKey());
            BlockState s = entry.getKey().getState();

            if (s instanceof BrewingStand b) {
                if (b.getBrewingTime() <= 0) {
                    int checksLeft = entry.getValue() - 1;
                    if (checksLeft <= 0) {
                        it.remove();
                        forget(key);
                    } else {
                        entry.setValue(checksLeft);
                    }
                    continue;
                }

                BrewingStandOwner owner = standOwners.get(key);
                if (owner == null) {
                    requestOwner(b.getBlock(), key);
                    continue;
                }

                Integer level = ownerLevelCache.get(owner.getOwner(), this, now).getNow(null);
                if (level == null) {
                    continue;
                }
                if (level > 0) {
                    updateHeat(b, getLevelPercent(level));
                } else {
                    it.remove();
                    forget(key);
                }
            } else {
                it.remove();
                forget(key);
            }
        }
    }

    private void requestOwner(Block block, StandKey key) {
        if (ownerLookups.containsKey(key)) {
            return;
        }
        CompletableFuture<BrewingStandOwner> lookup = WorldData.of(block.getWorld()).getMantle()
                .getAsync(key.x(), key.y(), key.z(), BrewingStandOwner.class);
        ownerLookups.put(key, lookup);
        lookup.whenComplete((owner, error) -> J.s(() -> {
            if (!ownerLookups.remove(key, lookup)) {
                return;
            }
            Block current = key.block();
            if (current == null || !activeStands.containsKey(current)) {
                return;
            }
            if (error != null || owner == null) {
                activeStands.remove(current);
                standOwners.remove(key);
                if (error != null) {
                    Adapt.verbose("Failed to load brewing stand owner at " + key);
                }
                return;
            }
            standOwners.put(key, owner);
        }));
    }

    private void forget(StandKey key) {
        standOwners.remove(key);
        ownerLookups.remove(key);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void on(BlockBreakEvent event) {
        if (!event.isCancelled() && event.getBlock().getType() == Material.BREWING_STAND) {
            activeStands.remove(event.getBlock());
            forget(StandKey.of(event.getBlock()));
        }
    }

    @EventHandler
    public void on(WorldUnloadEvent event) {
        UUID worldId = event.getWorld().getUID();
        activeStands.keySet().removeIf(block -> block.getWorld().equals(event.getWorld()));
        standOwners.keySet().removeIf(key -> key.worldId().equals(worldId));
        ownerLookups.keySet().removeIf(key -> key.worldId().equals(worldId));
    }

    private void updateHeat(BrewingStand b, double factor) {
        double l = 0;
        double f = 0;

        switch (b.getBlock().getRelative(BlockFace.DOWN).getType()) {
            case LAVA -> l = l + 1;
            case FIRE -> f = f + 1;
        }
        switch (b.getBlock().getRelative(BlockFace.NORTH).getType()) {
            case LAVA -> l = l + 1;
            case FIRE -> f = f + 1;
        }
        switch (b.getBlock().getRelative(BlockFace.SOUTH).getType()) {
            case LAVA -> l = l + 1;
            case FIRE -> f = f + 1;
        }
        switch (b.getBlock().getRelative(BlockFace.EAST).getType()) {
            case LAVA -> l = l + 1;
            case FIRE -> f = f + 1;
        }
        switch (b.getBlock().getRelative(BlockFace.WEST).getType()) {
            case LAVA -> l = l + 1;
            case FIRE -> f = f + 1;
        }

        double pct = (getFireBoost(factor) * f) + (getLavaBoost(factor) * l) + 1;
        int warp = (int) ((getInterval() / 50D) * pct);
        b.setBrewingTime(Math.max(1, b.getBrewingTime() - warp));
        b.update();

        if (M.r(1D / (333D / getInterval()))) {
            SoundPlayer spw = SoundPlayer.of(b.getBlock().getWorld());
            spw.play(b.getBlock().getLocation(), Sound.BLOCK_FIRE_AMBIENT, 1f, 1f + RNG.r.f(0.3f, 0.6f));
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
        boolean enabled = true;
        int baseCost = 3;
        double costFactor = 0.75;
        int maxLevel = 5;
        int initialCost = 5;
        double multiplierFactor = 1.33;
        double fireMultiplier = 0.14;
        double lavaMultiplier = 0.69;
    }

    private record StandKey(UUID worldId, int x, int y, int z) {
        private static StandKey of(Block block) {
            return new StandKey(block.getWorld().getUID(), block.getX(), block.getY(), block.getZ());
        }

        private Block block() {
            World world = Bukkit.getWorld(worldId);
            return world == null ? null : world.getBlockAt(x, y, z);
        }
    }
}
