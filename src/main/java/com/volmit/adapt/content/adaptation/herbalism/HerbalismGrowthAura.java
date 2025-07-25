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

import com.volmit.adapt.Adapt;
import com.volmit.adapt.api.adaptation.SimpleAdaptation;
import com.volmit.adapt.api.world.AdaptPlayer;
import com.volmit.adapt.util.*;
import com.volmit.adapt.util.reflect.registries.Particles;
import lombok.NoArgsConstructor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import java.util.ArrayList;
import java.util.List;

public class HerbalismGrowthAura extends SimpleAdaptation<HerbalismGrowthAura.Config> {
    private final List<Integer> holds = new ArrayList<>();

    public HerbalismGrowthAura() {
        super("herbalism-growth-aura");
        registerConfiguration(Config.class);
        setDescription(Localizer.dLocalize("herbalism", "growthaura", "description"));
        setDisplayName(Localizer.dLocalize("herbalism", "growthaura", "name"));
        setIcon(Material.BONE_MEAL);
        setBaseCost(getConfig().baseCost);
        setMaxLevel(getConfig().maxLevel);
        setInterval(850);
        setInitialCost(getConfig().initialCost);
        setCostFactor(getConfig().costFactor);
    }

    @Override
    public void addStats(int level, Element v) {
        v.addLore(C.GREEN + "+ " + Form.f(getRadius(getLevelPercent(level)), 0) + C.GRAY + " "
                + Localizer.dLocalize("herbalism", "growthaura", "lore1"));
        v.addLore(C.GREEN + "+ " + Form.pc(getStrength(level), 0) + C.GRAY + " "
                + Localizer.dLocalize("herbalism", "growthaura", "lore2"));
        v.addLore(C.YELLOW + "+ " + Form.f(getFoodCost(getLevelPercent(level)), 2) + C.GRAY + " "
                + Localizer.dLocalize("herbalism", "growthaura", "lore3"));
    }

    private double getRadius(double factor) {
        return factor * getConfig().radiusFactor;
    }

    private double getStrength(int level) {
        return level * getConfig().strengthFactor;
    }

    private double getFoodCost(double factor) {
        return M.lerp(1D - factor, getConfig().maxFoodCost, getConfig().minFoodCost);
    }

    @Override
    public void onTick() {
        for (Player p : Adapt.instance.getAdaptServer().getAdaptPlayers()) {
            try {
                if (hasAdaptation(p)) {
                    double rad = getRadius(getLevelPercent(p));
                    double strength = getStrength(getLevel(p));
                    double angle = Math.toRadians(Math.random() * 360);
                    double foodCost = getFoodCost(getLevelPercent(p));

                    for (int i = 0; i < Math.min(Math.min(rad * rad, 256), 3); i++) {
                        Location m = p.getLocation().clone()
                                .add(new Vector(Math.sin(angle), RNG.r.i(-1, 1), Math.cos(angle))
                                        .multiply(Math.random() * rad));
                        Block a = m.getBlock();
                        if (getConfig().surfaceOnly) {
                            int max = a.getWorld().getHighestBlockYAt(m);

                            if (max + 1 != a.getY())
                                continue;
                        }

                        SoundPlayer spw = SoundPlayer.of(a.getWorld());
                        if (a.getBlockData() instanceof Ageable ab) {
                            int toGrowLeft = ab.getMaximumAge() - ab.getAge();

                            if (toGrowLeft > 0) {
                                int add = (int) Math.max(1, Math.min(strength, toGrowLeft));
                                AdaptPlayer player = getPlayer(p);
                                if (ab.getMaximumAge() > ab.getAge() && player.canConsumeFood(foodCost, 10)) {
                                    while (add-- > 0) {
                                        J.s(() -> {
                                            if (!p.isOnline() || !player.consumeFood(foodCost, 10)
                                                    || !(a.getBlockData() instanceof Ageable aab)
                                                    || aab.getAge() == aab.getMaximumAge())
                                                return;

                                            aab.setAge(aab.getAge() + 1);
                                            a.setBlockData(aab, true);
                                            spw.play(a.getLocation(), Sound.BLOCK_CHORUS_FLOWER_DEATH, 0.25f,
                                                    RNG.r.f(0.3f, 0.7f));
                                            if (getConfig().showParticles) {
                                                p.spawnParticle(Particles.VILLAGER_HAPPY,
                                                        a.getLocation().clone().add(0.5, 0.5, 0.5), 3, 0.3, 0.3, 0.3,
                                                        0.9);
                                            }
                                            // xp(p, 1); // JESUS THIS IS FUCKING BUSTED
                                        }, RNG.r.i(30, 60));
                                    }
                                }
                            }

                        }
                    }
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
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
        boolean showParticles = true;
        boolean surfaceOnly = true;
        int baseCost = 8;
        int maxLevel = 7;
        int initialCost = 12;
        double costFactor = 0.325;
        double minFoodCost = 0.05;
        double maxFoodCost = 0.4;
        double radiusFactor = 18;
        double strengthFactor = 0.75;
    }
}
