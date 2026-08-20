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

package com.volmit.adapt.api.adaptation;

import com.volmit.adapt.Adapt;
import com.volmit.adapt.AdaptConfig;
import com.volmit.adapt.api.advancement.AdaptAdvancement;
import com.volmit.adapt.api.advancement.AdvancementVisibility;
import com.volmit.adapt.api.potion.BrewingRecipe;
import com.volmit.adapt.api.recipe.AdaptRecipe;
import com.volmit.adapt.api.skill.Skill;
import com.volmit.adapt.api.tick.TickedObject;
import com.volmit.adapt.util.*;
import com.volmit.adapt.util.IO;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@EqualsAndHashCode(callSuper = false)
@Data
public abstract class SimpleAdaptation<T> extends TickedObject implements Adaptation<T> {
    private int maxLevel;
    private int initialCost;
    private int baseCost;
    private double costFactor;
    private Component displayName;
    private Skill<?> skill;
    private Component description;
    private Material icon;
    private String name;
    private List<AdaptAdvancement> cachedAdvancements;
    private List<AdaptRecipe> recipes;
    private List<BrewingRecipe> brewingRecipes;
    private Class<T> configType;
    private T config;

    public SimpleAdaptation(String name) {
        super("adaptations", UUID.randomUUID() + "-" + name, 1000);
        cachedAdvancements = new ArrayList<>();
        recipes = new ArrayList<>();
        brewingRecipes = new ArrayList<>();
        setMaxLevel(5);
        setCostFactor(0.35);
        setBaseCost(3);
        setIcon(Material.PAPER);
        setInitialCost(1);
        setDescription(Component.text("No Description Provided"));
        this.name = name;
    }

    @Override
    protected boolean needsTicking() {
        return false;
    }

    @Override
    public Class<T> getConfigurationClass() {
        return configType;
    }

    @Override
    public void registerConfiguration(Class<T> type) {
        this.configType = type;
    }

    @Override
    public T getConfig() {
        try {
            if (config == null) {
                T dummy = getConfigurationClass().getConstructor().newInstance();
                File l = Adapt.instance.getDataFile("adapt", "adaptations", getName() + ".json");

                if (!l.exists()) {
                    try {
                        IO.writeAllAtomic(l, Json.toJson(dummy, true));
                    } catch (IOException e) {
                        e.printStackTrace();
                        config = dummy;
                        return config;
                    }
                }

                try {
                    T loaded = Json.fromJson(IO.readAll(l), getConfigurationClass());
                    config = loaded == null ? dummy : loaded;
                } catch (Throwable e) {
                    e.printStackTrace();
                    config = dummy;
                    return config;
                }
            }
        } catch (Throwable e) {
            Adapt.verbose("Failed to load config for " + getName());
        }

        return config;
    }

    public void registerRecipe(AdaptRecipe r) {
        recipes.add(r);
    }

    public void registerBrewingRecipe(BrewingRecipe r) {
        brewingRecipes.add(r);
    }

    @Override
    public Component getDisplayName() {
        try {
            return displayName == null
                    ? Adaptation.super.getDisplayName()
                    : Component.empty().color(getSkill().getColor())
                            .decoration(TextDecoration.OBFUSCATED, false).decoration(TextDecoration.BOLD, false)
                            .decoration(TextDecoration.STRIKETHROUGH, false)
                            .decoration(TextDecoration.UNDERLINED, false).decoration(TextDecoration.ITALIC, false)
                            .append(displayName);
        } catch (Exception ignored) {
            Adapt.verbose("Failed to get display name for " + getName());
            return null;
        }
    }

    @Override
    public Component getTitleDisplay() {
        try {
            return displayName == null
                    ? Adaptation.super.getDisplayName()
                    : Component.empty().color(Components.darker(getSkill().getColor()))
                            .decoration(TextDecoration.OBFUSCATED, false).decoration(TextDecoration.BOLD, false)
                            .decoration(TextDecoration.STRIKETHROUGH, false)
                            .decoration(TextDecoration.UNDERLINED, false).decoration(TextDecoration.ITALIC, false)
                            .append(displayName);
        } catch (Exception ignored) {
            Adapt.verbose("Failed to get display name for " + getName());
            return null;
        }
    }

    public void registerAdvancement(AdaptAdvancement a) {
        cachedAdvancements.add(a);
    }

    @Override
    public void onRegisterAdvancements(List<AdaptAdvancement> advancements) {
        advancements.addAll(cachedAdvancements);
    }

    public AdaptAdvancement buildAdvancements() {
        List<AdaptAdvancement> a = new ArrayList<>();
        onRegisterAdvancements(a);

        return AdaptAdvancement.builder().key("adaptation_" + getName()).title(getDisplayName())
                .description(Component.empty().append(getDescription()).append(Component.text(". "))
                        .append(Localizer.component("snippets", "gui", "unlockthisbyclicking"))
                        .append(Component.space())
                        .append(Localizer.configured(AdaptConfig.get().adaptActivatorBlockName)))
                .icon(getIcon()).children(a).visibility(AdvancementVisibility.PARENT_GRANTED).build();
    }
}
