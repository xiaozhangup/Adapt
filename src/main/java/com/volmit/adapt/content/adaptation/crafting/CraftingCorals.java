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

package com.volmit.adapt.content.adaptation.crafting;

import com.volmit.adapt.api.adaptation.SimpleAdaptation;
import com.volmit.adapt.api.recipe.MaterialChar;
import com.volmit.adapt.api.recipe.type.Shaped;
import com.volmit.adapt.util.C;
import com.volmit.adapt.util.Element;
import com.volmit.adapt.util.Localizer;
import lombok.NoArgsConstructor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class CraftingCorals extends SimpleAdaptation<CraftingCorals.Config> {

    public CraftingCorals() {
        super("crafting-corals");
        registerConfiguration(Config.class);
        setDescription(Localizer.dLocalize("crafting", "corals", "description"));
        setDisplayName(Localizer.dLocalize("crafting", "corals", "name"));
        setIcon(Material.HORN_CORAL);
        setBaseCost(getConfig().baseCost);
        setCostFactor(getConfig().costFactor);
        setMaxLevel(getConfig().maxLevel);
        setInitialCost(getConfig().initialCost);
        setInterval(17776);
        registerRecipe(Shaped.builder().key("crafting-tubecoral")
                .ingredient(new MaterialChar('I', Material.TUBE_CORAL))
                .ingredient(new MaterialChar('X', Material.BONE_BLOCK))
                .shapes(List.of("III", "IXI", "III"))
                .result(new ItemStack(Material.TUBE_CORAL_BLOCK, 1)).build());
        registerRecipe(Shaped.builder().key("crafting-tubecoralfan")
                .ingredient(new MaterialChar('I', Material.TUBE_CORAL_FAN))
                .ingredient(new MaterialChar('X', Material.BONE_BLOCK))
                .shapes(List.of("III", "IXI", "III"))
                .result(new ItemStack(Material.TUBE_CORAL_BLOCK, 1)).build());

        registerRecipe(Shaped.builder().key("crafting-braincoral")
                .ingredient(new MaterialChar('I', Material.BRAIN_CORAL))
                .ingredient(new MaterialChar('X', Material.BONE_BLOCK))
                .shapes(List.of("III", "IXI", "III"))
                .result(new ItemStack(Material.BRAIN_CORAL_BLOCK, 1)).build());
        registerRecipe(Shaped.builder().key("crafting-braincoralfan")
                .ingredient(new MaterialChar('I', Material.BRAIN_CORAL_FAN))
                .ingredient(new MaterialChar('X', Material.BONE_BLOCK))
                .shapes(List.of("III", "IXI", "III"))
                .result(new ItemStack(Material.BRAIN_CORAL_BLOCK, 1)).build());

        registerRecipe(Shaped.builder().key("crafting-bubblecoral")
                .ingredient(new MaterialChar('I', Material.BUBBLE_CORAL))
                .ingredient(new MaterialChar('X', Material.BONE_BLOCK))
                .shapes(List.of("III", "IXI", "III"))
                .result(new ItemStack(Material.BUBBLE_CORAL_BLOCK, 1)).build());
        registerRecipe(Shaped.builder().key("crafting-bubblecoralfan")
                .ingredient(new MaterialChar('I', Material.BUBBLE_CORAL_FAN))
                .ingredient(new MaterialChar('X', Material.BONE_BLOCK))
                .shapes(List.of("III", "IXI", "III"))
                .result(new ItemStack(Material.BUBBLE_CORAL_BLOCK, 1)).build());

        registerRecipe(Shaped.builder().key("crafting-firecoral")
                .ingredient(new MaterialChar('I', Material.FIRE_CORAL))
                .ingredient(new MaterialChar('X', Material.BONE_BLOCK))
                .shapes(List.of("III", "IXI", "III"))
                .result(new ItemStack(Material.FIRE_CORAL_BLOCK, 1)).build());
        registerRecipe(Shaped.builder().key("crafting-firecoralfan")
                .ingredient(new MaterialChar('I', Material.FIRE_CORAL_FAN))
                .ingredient(new MaterialChar('X', Material.BONE_BLOCK))
                .shapes(List.of("III", "IXI", "III"))
                .result(new ItemStack(Material.FIRE_CORAL_BLOCK, 1)).build());

        registerRecipe(Shaped.builder().key("crafting-horncoral")
                .ingredient(new MaterialChar('I', Material.HORN_CORAL))
                .ingredient(new MaterialChar('X', Material.BONE_BLOCK))
                .shapes(List.of("III", "IXI", "III"))
                .result(new ItemStack(Material.HORN_CORAL_BLOCK, 1)).build());
        registerRecipe(Shaped.builder().key("crafting-horncoralfan")
                .ingredient(new MaterialChar('I', Material.HORN_CORAL_FAN))
                .ingredient(new MaterialChar('X', Material.BONE_BLOCK))
                .shapes(List.of("III", "IXI", "III"))
                .result(new ItemStack(Material.HORN_CORAL_BLOCK, 1)).build());
    }

    @Override
    public void addStats(int level, Element v) {
        v.addLore(C.GREEN + "+ " + C.GRAY + Localizer.dLocalize("crafting", "corals", "lore1"));
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
        int baseCost = 10;
        int maxLevel = 1;
        int initialCost = 2;
        double costFactor = 1;
    }
}