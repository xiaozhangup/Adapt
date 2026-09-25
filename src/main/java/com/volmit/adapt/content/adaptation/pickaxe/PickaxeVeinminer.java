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

package com.volmit.adapt.content.adaptation.pickaxe;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import com.volmit.adapt.Adapt;
import com.volmit.adapt.api.adaptation.SimpleAdaptation;
import com.volmit.adapt.api.world.AdaptPlayer;
import com.volmit.adapt.api.world.PlayerAdaptation;
import com.volmit.adapt.api.world.PlayerSkillLine;
import com.volmit.adapt.content.util.BoundedBlockSearch;
import com.volmit.adapt.util.*;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.List;
import java.util.UUID;

public class PickaxeVeinminer extends SimpleAdaptation<PickaxeVeinminer.Config> {
    private static final int DEFAULT_MAX_BLOCKS = 128;

    public PickaxeVeinminer() {
        super("pickaxe-veinminer");
        registerConfiguration(PickaxeVeinminer.Config.class);
        setDescription(Localizer.component("pickaxe", "veinminer", "description"));
        setDisplayName(Localizer.component("pickaxe", "veinminer", "name"));
        setIcon(Material.IRON_PICKAXE);
        setBaseCost(getConfig().baseCost);
        setMaxLevel(getConfig().maxLevel);
        setInitialCost(getConfig().initialCost);
        setCostFactor(getConfig().costFactor);
        setInterval(8484);
    }

    public void addStats(int level, Element v) {
        v.addLore(Localizer.components("pickaxe", "veinminer", "lore",
                Placeholder.unparsed("range", Integer.toString(level + getConfig().baseRange))));
    }

    private int getRadius(int lvl) {
        return lvl + getConfig().baseRange;
    }

    @EventHandler
    public void on(BlockBreakEvent e) {
        if (e.isCancelled()) {
            return;
        }
        Player p = e.getPlayer();
        if (!hasAdaptation(p)) {
            return;
        }
        if (!p.isSneaking()) {
            return;
        }

        if (!e.getBlock().getBlockData().getMaterial().name().endsWith("_ORE")) {
            if (!e.getBlock().getType().equals(Material.OBSIDIAN)) {
                return;
            }
        }

        Block block = e.getBlock();
        Material targetType = block.getType();
        int configuredLimit = getConfig().maxBlocks > 0 ? getConfig().maxBlocks : DEFAULT_MAX_BLOCKS;
        int radius = getRadius(getLevel(p));
        List<Block> blockMap = BoundedBlockSearch.find(block, radius, configuredLimit,
                candidate -> candidate.getType() == targetType && canBlockBreak(p, candidate));

        AdaptPlayer expectedPlayer = getPlayer(p);
        UUID playerId = p.getUniqueId();
        PlayerSkillLine line = expectedPlayer.getData().getSkillLineNullable("pickaxe");
        PlayerAdaptation autoSmelt = line != null ? line.getAdaptation("pickaxe-autosmelt") : null;
        PlayerAdaptation drop2Inv = line != null ? line.getAdaptation("pickaxe-drop-to-inventory") : null;
        J.s(() -> {
            Player online = Bukkit.getPlayer(playerId);
            if (online == null || !online.isOnline()
                    || !Adapt.instance.getAdaptServer().isPlayerLoaded(playerId)
                    || !Adapt.instance.getAdaptServer().isCurrentPlayer(playerId, expectedPlayer)) {
                return;
            }
            int processed = 0;
            for (Block b : blockMap) {
                if (b.getType() != targetType || !canBlockBreak(online, b)) {
                    Adapt.verbose("Player " + online.getName() + " doesn't have permission.");
                    continue;
                }
                if (autoSmelt != null && autoSmelt.getLevel() > 0) {
                    if (drop2Inv != null && drop2Inv.getLevel() > 0) {
                        PickaxeAutosmelt.autosmeltBlockDTI(b, online);
                    } else {
                        PickaxeAutosmelt.autosmeltBlock(b, online);
                    }
                } else {
                    if (drop2Inv != null
                            && drop2Inv.getLevel() > 0) {
                        b.getDrops(online.getInventory().getItemInMainHand(), online).forEach(item -> {
                            HashMap<Integer, ItemStack> extra = online.getInventory().addItem(item);
                            extra.forEach((k, v) -> online.getWorld().dropItem(online.getLocation(), v));
                        });
                        b.setType(Material.AIR);
                    } else {
                        b.breakNaturally(online.getInventory().getItemInMainHand());
                    }
                }
                processed++;
            }

            if (processed > 0) {
                SoundPlayer.of(block.getWorld()).play(block.getLocation(), Sound.BLOCK_FUNGUS_BREAK, 0.4f, 0.25f);
                if (getConfig().showParticles) {
                    block.getWorld().spawnParticle(Particle.ASH, block.getLocation().add(0.5, 0.5, 0.5),
                            Math.min(200, processed * 3), Math.min(2, radius / 2D),
                            Math.min(2, radius / 2D), Math.min(2, radius / 2D), 0.1);
                }
            }
        });
    }

    @Override
    public boolean isEnabled() {
        return getConfig().enabled;
    }

    @Override
    public void onTick() {
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
        int baseCost = 6;
        int maxLevel = 5;
        int initialCost = 4;
        double costFactor = 2.325;
        int baseRange = 2;
        int maxBlocks = DEFAULT_MAX_BLOCKS;
    }
}
