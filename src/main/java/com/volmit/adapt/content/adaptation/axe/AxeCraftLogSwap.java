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

import com.volmit.adapt.api.adaptation.SimpleAdaptation;
import com.volmit.adapt.api.recipe.type.Shapeless;
import com.volmit.adapt.util.C;
import com.volmit.adapt.util.Element;
import com.volmit.adapt.util.Localizer;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class AxeCraftLogSwap extends SimpleAdaptation<AxeCraftLogSwap.Config> {

    private static final List<Material> logs = new ArrayList<>();
    private static final Map<Material, Material> saplings = new EnumMap<>(Material.class);

    public AxeCraftLogSwap() {
        super("axe-logswap");
        registerConfiguration(Config.class);
        setDescription(Localizer.dLocalize("axe", "logswap", "description"));
        setDisplayName(Localizer.dLocalize("axe", "logswap", "name"));
        setIcon(Material.MUDDY_MANGROVE_ROOTS);
        setBaseCost(getConfig().baseCost);
        setCostFactor(getConfig().costFactor);
        setMaxLevel(getConfig().maxLevel);
        setInitialCost(getConfig().initialCost);
        setInterval(17773);

        // 所有原木
        logs.add(Material.ACACIA_LOG);
        logs.add(Material.BIRCH_LOG);
        logs.add(Material.CHERRY_LOG);
        logs.add(Material.DARK_OAK_LOG);
        logs.add(Material.JUNGLE_LOG);
        logs.add(Material.MANGROVE_LOG);
        logs.add(Material.OAK_LOG);
        logs.add(Material.PALE_OAK_LOG);
        logs.add(Material.SPRUCE_LOG);
        logs.add(Material.CRIMSON_STEM);
        logs.add(Material.WARPED_STEM);
        logs.add(Material.BAMBOO_BLOCK);

        // 树苗和方块映射
        saplings.put(Material.ACACIA_SAPLING, Material.ACACIA_LOG);
        saplings.put(Material.BIRCH_SAPLING, Material.BIRCH_LOG);
        saplings.put(Material.CHERRY_SAPLING, Material.CHERRY_LOG);
        saplings.put(Material.DARK_OAK_SAPLING, Material.DARK_OAK_LOG);
        saplings.put(Material.JUNGLE_SAPLING, Material.JUNGLE_LOG);
        saplings.put(Material.MANGROVE_PROPAGULE, Material.MANGROVE_LOG);
        saplings.put(Material.OAK_SAPLING, Material.OAK_LOG);
        saplings.put(Material.SPRUCE_SAPLING, Material.SPRUCE_LOG);
        saplings.put(Material.PALE_OAK_SAPLING, Material.PALE_OAK_LOG);
        saplings.put(Material.CRIMSON_FUNGUS, Material.CRIMSON_STEM);
        saplings.put(Material.WARPED_FUNGUS, Material.WARPED_STEM);
        saplings.put(Material.BAMBOO, Material.BAMBOO_BLOCK);

        for (Material log : logs) {
            for (Map.Entry<Material, Material> sapling : saplings.entrySet()) {
                Material from = sapling.getKey();
                Material to = sapling.getValue();
                if (log == to) continue;

                StringBuilder key = new StringBuilder("axe-swap");
                String logName = log.toString().toLowerCase();
                logName = logName.substring(0, logName.length() - 4);
                String toName = to.toString().toLowerCase();
                toName = toName.substring(0, toName.length() - 4);
                key.append(logName).append(toName);
                registerRecipe(Shapeless.builder().key(key.toString()).ingredient(log)
                        .ingredient(log).ingredient(log).ingredient(log)
                        .ingredient(log).ingredient(log).ingredient(log)
                        .ingredient(log).ingredient(from)
                        .result(new ItemStack(to, 8)).build());
            }
        }
    }

    @Override
    public void addStats(int level, Element v) {
        v.addLore(C.GREEN + "+ " + C.GRAY + Localizer.dLocalize("axe", "logswap", "lore1"));
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
        boolean permanent = true;
        boolean enabled = true;
        int baseCost = 2;
        int maxLevel = 1;
        int initialCost = 2;
        double costFactor = 1;
    }
}