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

package com.volmit.adapt.content.adaptation.herbalism;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

import com.volmit.adapt.Adapt;
import com.volmit.adapt.api.adaptation.SimpleAdaptation;
import com.volmit.adapt.api.world.AdaptPlayer;
import com.volmit.adapt.content.skill.SkillHerbalism;
import com.volmit.adapt.util.*;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class HerbalismReplant extends SimpleAdaptation<HerbalismReplant.Config> {

    public HerbalismReplant() {
        super("herbalism-replant");
        registerConfiguration(Config.class);
        setDescription(Localizer.component("herbalism", "replant", "description"));
        setDisplayName(Localizer.component("herbalism", "replant", "name"));
        setIcon(Material.PUMPKIN_SEEDS);
        setBaseCost(getConfig().baseCost);
        setMaxLevel(getConfig().maxLevel);
        setInterval(6090);
        setInitialCost(getConfig().initialCost);
        setCostFactor(getConfig().costFactor);
    }

    @Override
    public void addStats(int level, Element v) {
        v.addLore(Localizer.components("herbalism", "replant", "lore",
                Placeholder.unparsed("radius", Float.toString(getRadius(level)))));
    }

    private int getCooldown(double factor, int level) {
        if (level == 1) {
            return (int) getConfig().cooldownLvl1;
        }

        return (int) ((getConfig().baseCooldown - (getConfig().cooldownFactor * factor)) + getConfig().bonusCooldown);
    }

    private float getRadius(int lvl) {
        return lvl - getConfig().radiusSub;
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void on(PlayerInteractEvent e) {
        if (e.isCancelled()) {
            return;
        }
        Player p = e.getPlayer();
        if (e.getClickedBlock() == null) {
            return;
        }
        if (!e.getAction().equals(Action.RIGHT_CLICK_BLOCK)) { // you need to right-click to harvest!
            return;
        }

        if (!(e.getClickedBlock().getBlockData() instanceof Ageable)) {
            return;
        }

        if (!hasAdaptation(p)) {
            return;
        }

        int lvl = getLevel(p);
        if (lvl > 0) {
            ItemStack right = p.getInventory().getItemInMainHand();
            ItemStack left = p.getInventory().getItemInOffHand();
            boolean useOffHand;
            Material toolType;

            if (isTool(left) && isHoe(left) && !p.hasCooldown(left.getType())) {
                useOffHand = true;
                toolType = left.getType();
            } else if (isTool(right) && isHoe(right) && !p.hasCooldown(right.getType())) {
                useOffHand = false;
                toolType = right.getType();
            } else {
                return;
            }

            AdaptPlayer expectedPlayer = getPlayer(p);

            if (lvl > 1) {
                Cuboid c = new Cuboid(e.getClickedBlock().getLocation().clone().add(0.5, 0.5, 0.5));
                c = c.expand(Cuboid.CuboidDirection.Up, (int) Math.floor(getRadius(lvl)));
                c = c.expand(Cuboid.CuboidDirection.Down, (int) Math.floor(getRadius(lvl)));
                c = c.expand(Cuboid.CuboidDirection.North, Math.round(getRadius(lvl)));
                c = c.expand(Cuboid.CuboidDirection.South, Math.round(getRadius(lvl)));
                c = c.expand(Cuboid.CuboidDirection.East, Math.round(getRadius(lvl)));
                c = c.expand(Cuboid.CuboidDirection.West, Math.round(getRadius(lvl)));

                List<Block> crops = new ArrayList<>();
                for (Block block : c) {
                    if (block.getBlockData() instanceof Ageable crop && crop.getAge() > 0
                            && canBlockBreak(p, block)) {
                        crops.add(block);
                    }
                }
                if (!crops.isEmpty()) {
                    UUID playerId = p.getUniqueId();
                    J.s(() -> {
                        Player online = Bukkit.getPlayer(playerId);
                        if (online == null || !online.isOnline() || !hasAdaptation(online)
                                || !Adapt.instance.getAdaptServer().isPlayerLoaded(playerId)
                                || !Adapt.instance.getAdaptServer().isCurrentPlayer(playerId, expectedPlayer)) {
                            return;
                        }
                        ItemStack currentHoe = useOffHand
                                ? online.getInventory().getItemInOffHand()
                                : online.getInventory().getItemInMainHand();
                        if (currentHoe.getType() != toolType || !isTool(currentHoe) || !isHoe(currentHoe)) {
                            return;
                        }
                        boolean harvested = false;
                        for (Block crop : crops) {
                            if (canBlockBreak(online, crop)) {
                                harvested |= hit(online, crop, lvl);
                            }
                        }
                        if (harvested) {
                            consumeHoe(online, useOffHand, toolType, lvl);
                            playHarvestEffects(online, lvl);
                        }
                    }, M.irand(1, 6));
                } else {
                    return;
                }
            } else {
                if (!(e.getClickedBlock().getBlockData() instanceof Ageable crop) || crop.getAge() <= 0
                        || !canBlockBreak(p, e.getClickedBlock())) {
                    return;
                }
                if (hit(p, e.getClickedBlock(), lvl)) {
                    consumeHoe(p, useOffHand, toolType, lvl);
                }
            }
        }
    }

    private void playHarvestEffects(Player player, int level) {
        SoundPlayer sounds = SoundPlayer.of(player.getWorld());
        sounds.play(player.getLocation(), Sound.ITEM_SHOVEL_FLATTEN, 1f, 0.66f);
        sounds.play(player.getLocation(), Sound.BLOCK_BAMBOO_SAPLING_BREAK, 1f, 0.66f);
        if (getConfig().showParticles) {
            player.spawnParticle(Particle.HAPPY_VILLAGER, player.getLocation().clone().add(0.5, 0.5, 0.5),
                    level * 3, 0.3 * level, 0.3 * level, 0.3 * level, 0.9);
        }
    }

    private void consumeHoe(Player player, boolean offHand, Material toolType, int level) {
        if (offHand) {
            damageOffHand(player, 1 + ((level - 1) * 7));
        } else {
            damageHand(player, 1 + ((level - 1) * 7));
        }
        player.setCooldown(toolType, getCooldown(getLevelPercent(player), level));
    }

    private boolean hit(Player p, Block b, int level) {
        if (b != null && canBlockBreak(p, b) && b.getBlockData() instanceof Ageable aa) {
            if (aa.getAge() == 0) {
                return false;
            }

            int age = aa.getAge();
            if (!p.breakBlock(b)) {
                return false;
            }

            xp(p, b.getLocation().clone().add(0.5, 0.5, 0.5),
                    ((SkillHerbalism.Config) getSkill().getConfig()).harvestPerAgeXP * age);
            xp(p, b.getLocation().clone().add(0.5, 0.5, 0.5),
                    ((SkillHerbalism.Config) getSkill().getConfig()).plantCropSeedsXP);

            aa.setAge(0);
            b.setBlockData(aa, true);

            getPlayer(p).getData().addStat("harvest.blocks", 1);
            getPlayer(p).getData().addStat("harvest.planted", 1);

            if (M.r(1D / (double) level)) {
                SoundPlayer spw = SoundPlayer.of(p.getWorld());
                spw.play(b.getLocation(), Sound.ITEM_CROP_PLANT, 1f, 0.7f);
            }
            return true;
        }
        return false;
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
        boolean enabled = true;
        boolean showParticles = true;
        int baseCost = 6;
        int maxLevel = 3;
        int initialCost = 4;
        double costFactor = 2.325;
        double cooldownLvl1 = 2;
        double baseCooldown = 30;
        double cooldownFactor = 30;
        double bonusCooldown = 20;
        int radiusSub = 1;
    }
}
