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

package com.volmit.adapt.api.advancement;

import com.github.retrooper.packetevents.protocol.advancements.AdvancementHolder;
import com.github.retrooper.packetevents.protocol.advancements.AdvancementType;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.volmit.adapt.util.AdvancementUtils;
import com.volmit.adapt.util.CustomModel;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import java.util.List;

@Builder
@Data
public class AdaptAdvancement {
    private String background;
    @Builder.Default
    private Material icon = Material.EMERALD;
    @Builder.Default
    private CustomModel model = null;
    @Builder.Default
    private Component title = Component.text("MISSING TITLE");
    @Builder.Default
    private Component description = Component.text("MISSING DESCRIPTION");
    @Builder.Default
    private AdvancementType frame = AdvancementType.TASK;
    @Builder.Default
    private boolean toast = false;
    @Builder.Default
    private boolean announce = false;
    @Builder.Default
    private AdvancementVisibility visibility = AdvancementVisibility.PARENT_GRANTED;
    @Builder.Default
    private String key = "root";
    @Singular
    private List<AdaptAdvancement> children;

    public ItemStack getDisplayIcon() {
        return model != null ? model.toItemStack() : new ItemStack(icon);
    }

    AdvancementHolder toAdvancement(ResourceLocation id, ResourceLocation parent, int index, int depth) {
        return AdvancementUtils.createAdvancement(id, parent, getDisplayIcon(), title, description, frame,
                parent == null ? new ResourceLocation(background) : null, false, false, 1f + depth, 1f + index);
    }
}
