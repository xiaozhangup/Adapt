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
import org.bukkit.block.BlockState;
import org.bukkit.block.BrewingStand;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.inventory.BrewEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;

import java.util.UUID;

public class BrewingLingering extends SimpleAdaptation<BrewingLingering.Config> {
    private final BrewingOwnerLevelCache ownerLevelCache = new BrewingOwnerLevelCache();

    public BrewingLingering() {
        super("brewing-lingering");
        registerConfiguration(Config.class);
        setDescription(Localizer.component("brewing", "lingering", "description"));
        setDisplayName(Localizer.component("brewing", "lingering", "name"));
        setIcon(Material.CLOCK);
        setBaseCost(getConfig().baseCost);
        setCostFactor(getConfig().costFactor);
        setMaxLevel(getConfig().maxLevel);
        setInitialCost(getConfig().initialCost);
        setInterval(4788);
    }

    @Override
    public void addStats(int level, Element v) {
        v.addLore(Localizer.components("brewing", "lingering", "lore",
                Placeholder.unparsed("duration",
                        Form.duration((long) getDurationBoost(getLevelPercent(level)), 0)),
                Placeholder.unparsed("amount", Form.pc(getPercentBoost(getLevelPercent(level)), 0))));
    }

    public double getDurationBoost(double factor) {
        return (getConfig().durationBoostFactorTicks * factor) + getConfig().baseDurationBoostTicks;
    }

    public double getPercentBoost(double factor) {
        return 1 + ((factor * factor * getConfig().durationMultiplierFactor) + getConfig().baseDurationMultiplier);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(BrewEvent e) {
        if (e.isCancelled()) {
            return;
        }
        if (e.getBlock().getType().equals(Material.BREWING_STAND)) {
            UUID worldId = e.getBlock().getWorld().getUID();
            int x = e.getBlock().getX();
            int y = e.getBlock().getY();
            int z = e.getBlock().getZ();
            ItemStack[] expectedResults = new ItemStack[3];
            for (int i = 0; i < expectedResults.length && i < e.getResults().size(); i++) {
                ItemStack result = e.getResults().get(i);
                expectedResults[i] = result == null ? null : result.clone();
            }
            WorldData.of(e.getBlock().getWorld()).getMantle().getAsync(x, y, z, BrewingStandOwner.class)
                    .whenComplete((owner, error) -> J.s(() ->
                            requestEnhancement(worldId, x, y, z, expectedResults, owner, error)));
        }
    }

    private void requestEnhancement(UUID worldId, int x, int y, int z, ItemStack[] expectedResults,
            BrewingStandOwner owner, Throwable error) {
        if (error != null || owner == null) {
            Adapt.verbose(error == null ? "No Owner" : "Failed to load brewing stand owner");
            return;
        }
        ownerLevelCache.get(owner.getOwner(), this, M.ms()).thenAccept(level -> {
            if (level != null && level > 0) {
                J.s(() -> enhanceStand(worldId, x, y, z, expectedResults, level));
            }
        });
    }

    private void enhanceStand(UUID worldId, int x, int y, int z, ItemStack[] expectedResults, int level) {
        World world = Bukkit.getWorld(worldId);
        if (world == null) {
            return;
        }
        BlockState state = world.getBlockAt(x, y, z).getState();
        if (!(state instanceof BrewingStand stand)) {
            return;
        }

        boolean enhanced = false;
        double factor = getLevelPercent(level);
        for (int i = 0; i < expectedResults.length; i++) {
            ItemStack expected = expectedResults[i];
            ItemStack current = stand.getInventory().getItem(i);
            if (expected == null || current == null || !current.equals(expected)) {
                continue;
            }
            ItemStack copy = current.clone();
            if (copy.getItemMeta() instanceof PotionMeta potion && enhance(factor, copy, potion)) {
                stand.getInventory().setItem(i, copy);
                enhanced = true;
            }
        }

        if (enhanced) {
            SoundPlayer sounds = SoundPlayer.of(world);
            sounds.play(stand.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 1f, 0.75f);
            sounds.play(stand.getLocation(), Sound.BLOCK_BREWING_STAND_BREW, 1f, 1.75f);
        }
    }

    private boolean enhance(double factor, ItemStack is, PotionMeta p) {
        if (!p.getBasePotionType().isInstant()) {
            PotionEffect effect = getRawPotionEffect(is);

            if (effect != null) {
                p.addCustomEffect(new PotionEffect(effect.getType(),

                        (int) (getDurationBoost(factor) + (effect.getDuration() * getPercentBoost(factor))),

                        effect.getAmplifier()), true);
                is.setItemMeta(p);
                return true;
            }
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
        int baseCost = 3;
        double costFactor = 0.75;
        int maxLevel = 5;
        int initialCost = 5;
        double baseDurationBoostTicks = 100;
        double durationBoostFactorTicks = 500;
        double durationMultiplierFactor = 0.45;
        double baseDurationMultiplier = 0.05;
    }
}
