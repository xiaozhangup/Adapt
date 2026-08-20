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

package com.volmit.adapt.content.item;

import com.volmit.adapt.Adapt;
import com.volmit.adapt.api.item.DataItem;
import com.volmit.adapt.util.Localizer;
import com.volmit.adapt.util.Components;
import lombok.AllArgsConstructor;
import lombok.Data;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.List;

@AllArgsConstructor
@Data
public class BoundRedstoneTorch implements DataItem<BoundRedstoneTorch.Data> {
    public static BoundRedstoneTorch io = new BoundRedstoneTorch();

    public static Location getLocation(ItemStack stack) {
        if (io.getData(stack) != null) {
            return io.getData(stack).getLocation();
        }

        return null;
    }

    /*
     * renamed from hasData as the types are the same (ItemStack -> boolean), but
     * this is static
     */
    public static boolean hasItemData(ItemStack stack) {
        return io.hasData(stack);
    }

    public static void setData(ItemStack item, Location t) {
        io.setData(item, new Data(t));
    }

    public static ItemStack withData(Location t) {
        return io.withData(new Data(t));
    }

    public static boolean isBindableItem(ItemStack t) {
        if (t.getType().equals(Material.REDSTONE_TORCH)) {
            ItemMeta meta = t.getItemMeta();
            if (meta != null && meta.hasLore() && meta.lore() != null && !meta.lore().isEmpty()) {
                if (Components.plain(meta.lore().getFirst())
                        .contains(Components.plain(Localizer.component("items", "boundredstonetorch", "name")))) {
                    Adapt.verbose("Torch is bindable: " + t.getType().name());
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Material getMaterial() {
        return Material.REDSTONE_TORCH;
    }

    @Override
    public Class<Data> getType() {
        return BoundRedstoneTorch.Data.class;
    }

    @Override
    public String getDataKey() {
        return "bound_redstone_torch";
    }

    @Override
    public void applyLore(Data data, List<Component> lore) {
        lore.add(Components.mini("<white><text></white>",
                Placeholder.component("text", Localizer.component("items", "boundredstonetorch", "name"))));
        lore.add(Components.mini("<gray><text></gray>",
                Placeholder.component("text", Localizer.component("items", "boundredstonetorch", "usage1"))));
        lore.add(Components.mini("<gray><text></gray>",
                Placeholder.component("text", Localizer.component("items", "boundredstonetorch", "usage2"))));
    }

    @Override
    public void applyMeta(Data data, ItemMeta meta) {
        meta.addEnchant(Enchantment.BINDING_CURSE, 10, true);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_DYE);
        meta.displayName(Localizer.component("items", "boundredstonetorch", "name"));
    }

    @AllArgsConstructor
    @lombok.Data
    public static class Data {
        private Location location;

        public static BoundRedstoneTorch.Data at(Location l) {
            return new BoundRedstoneTorch.Data(l);
        }
    }
}
