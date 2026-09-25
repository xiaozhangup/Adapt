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

package com.volmit.adapt.content.adaptation.axe;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;


import com.volmit.adapt.Adapt;
import com.volmit.adapt.api.adaptation.SimpleAdaptation;
import com.volmit.adapt.api.world.AdaptPlayer;
import com.volmit.adapt.api.world.PlayerAdaptation;
import com.volmit.adapt.api.world.PlayerSkillLine;
import com.volmit.adapt.content.item.ItemListings;
import com.volmit.adapt.content.util.BoundedBlockSearch;
import com.volmit.adapt.util.*;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.data.type.Leaves;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class AxeWoodVeinminer extends SimpleAdaptation<AxeWoodVeinminer.Config> {
    private static final BlockFace[] LEAF_CHECK_FACES = {
            BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.NORTH
    };

    public AxeWoodVeinminer() {
        super("axe-wood-veinminer");
        registerConfiguration(AxeWoodVeinminer.Config.class);
        setDescription(Localizer.component("axe", "woodminer", "description"));
        setDisplayName(Localizer.component("axe", "woodminer", "name"));
        setIcon(Material.DIAMOND_AXE);
        setBaseCost(getConfig().baseCost);
        setMaxLevel(getConfig().maxLevel);
        setInitialCost(getConfig().initialCost);
        setCostFactor(getConfig().costFactor);
        setInterval(5849);

    }

    @Override
    public boolean isEnabled() {
        return getConfig().enabled;
    }

    public void addStats(int level, Element v) {
        v.addLore(Localizer.components("axe", "woodminer", "lore",
                Placeholder.unparsed("range", Integer.toString(level + getConfig().baseRange))));
    }

    private int getRadius(int lvl) {
        return lvl + getConfig().baseRange;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(BlockBreakEvent e) {
        if (e.isCancelled()) {
            return;
        }
        Player p = e.getPlayer();
        if (hasAdaptation(p)) {

            if (!p.isSneaking()) {
                return;
            }

            ItemStack tool = p.getInventory().getItemInMainHand();
            if (!isAxe(tool)) {
                return;
            }

            Block block = e.getBlock();
            int level = getLevel(p);
            if (ItemListings.isLog(block.getType()) || (level >= 5 && ItemListings.isLeaves(block.getType()))) {
                if (hasPersistentAdjacentLeaves(block)) {
                    return;
                }

                Material targetType = block.getType();
                List<Block> blockMap = BoundedBlockSearch.find(block, getRadius(level), getConfig().maxBlocks,
                        candidate -> candidate.getType() == targetType && canBlockBreak(p, candidate));

                AdaptPlayer expectedPlayer = getPlayer(p);
                UUID playerId = p.getUniqueId();
                PlayerSkillLine line = expectedPlayer.getData().getSkillLineNullable("axes");
                PlayerAdaptation adaptation = line != null
                        ? line.getAdaptation("axe-drop-to-inventory") : null;

                J.s(() -> {
                    Player online = Bukkit.getPlayer(playerId);
                    if (online == null || !online.isOnline()
                            || !Adapt.instance.getAdaptServer().isPlayerLoaded(playerId)
                            || !Adapt.instance.getAdaptServer().isCurrentPlayer(playerId, expectedPlayer)) {
                        return;
                    }
                    int processed = 0;
                    for (Block blocks : blockMap) {
                        if (blocks.getType() != targetType || !canBlockBreak(online, blocks)) {
                            continue;
                        }
                        if (adaptation != null && adaptation.getLevel() > 0) {
                            Collection<ItemStack> items = blocks.getDrops(tool);
                            for (ItemStack item : items) {
                                safeGiveItem(online, item);
                                Adapt.verbose("Giving item: " + item);
                            }
                            blocks.setType(Material.AIR);
                        } else {
                            blocks.breakNaturally(tool);
                        }
                        processed++;
                    }

                    if (processed > 0) {
                        SoundPlayer.of(block.getWorld()).play(block.getLocation(), Sound.BLOCK_FUNGUS_BREAK, 0.01f,
                                0.25f);
                        if (getConfig().showParticles) {
                            block.getWorld().spawnParticle(Particle.ASH, block.getLocation().add(0.5, 0.5, 0.5),
                                    Math.min(200, processed * 3), 1, 1, 1, 0.1);
                            this.vfxCuboidOutline(block, Particle.ENCHANT);
                        }
                    }
                });
            }
        }
    }

    @Override
    public void onTick() {
    }

    private boolean hasPersistentAdjacentLeaves(Block block) {
        Material type = block.getType();
        Block current = block;
        int checked = 0;
        int maxHeight = block.getWorld().getMaxHeight();
        while (checked++ < 96 && current.getY() < maxHeight && current.getType() == type) {
            for (BlockFace face : LEAF_CHECK_FACES) {
                if (current.getRelative(face).getBlockData() instanceof Leaves leaves && leaves.isPersistent()) {
                    return true;
                }
            }
            current = current.getRelative(BlockFace.UP);
        }
        return false;
    }

    @Override
    public boolean isPermanent() {
        return getConfig().permanent;
    }

    @NoArgsConstructor
    protected static class Config {
        boolean permanent = false;
        boolean enabled = true;
        boolean showParticles = true;
        int baseCost = 3;
        int maxLevel = 5;
        int initialCost = 4;
        double costFactor = 2.325;
        int maxBlocks = 72;
        int baseRange = 3;
    }
}
