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

package com.volmit.adapt.api.item;

import com.volmit.adapt.Adapt;
import com.volmit.adapt.util.BukkitGson;
import com.volmit.adapt.util.Components;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;

import java.util.ArrayList;
import java.util.List;

public interface DataItem<T> {
    Material getMaterial();

    Class<T> getType();

    String getDataKey();

    void applyLore(T data, List<Component> lore);

    void applyMeta(T data, ItemMeta meta);

    default ItemStack blank() {
        return new ItemStack(getMaterial());
    }

    default T getData(ItemStack stack) {
        String json = readAndMigrateData(stack);
        if (json != null) {
            return BukkitGson.gson.fromJson(json, getType());
        }
        return null;
    }

    default boolean hasData(ItemStack stack) {
        return readAndMigrateData(stack) != null;
    }

    default void setData(ItemStack item, T t) {
        item.setItemMeta(withData(t).getItemMeta());
    }

    default ItemStack withData(T t) {
        ItemStack item = blank();
        ItemMeta meta = item.getItemMeta();

        if (meta == null) {
            return null;
        }

        applyMeta(t, meta);
        List<Component> lore = new ArrayList<>();
        applyLore(t, lore);
        Component displayName = meta.displayName();
        if (displayName != null) {
            meta.displayName(Components.itemColors(displayName));
        }
        meta.lore(lore.stream().map(Components::itemColors).toList());
        meta.getPersistentDataContainer().set(
                dataKey(),
                PersistentDataType.STRING, BukkitGson.gson.toJson(t));
        item.setItemMeta(meta);
        return item;
    }

    private String readAndMigrateData(ItemStack stack) {
        if (stack == null || !stack.getType().equals(getMaterial())) {
            return null;
        }

        ItemMeta meta = stack.getItemMeta();
        if (meta == null) {
            return null;
        }

        PersistentDataContainer data = meta.getPersistentDataContainer();
        String json = data.get(dataKey(), PersistentDataType.STRING);
        if (json != null) {
            return json;
        }

        NamespacedKey legacyKey = legacyDataKey();
        json = data.get(legacyKey, PersistentDataType.STRING);
        if (json != null) {
            data.set(dataKey(), PersistentDataType.STRING, json);
            data.remove(legacyKey);
            stack.setItemMeta(meta);
        }
        return json;
    }

    private NamespacedKey dataKey() {
        return new NamespacedKey(Adapt.instance, "data_item/" + getDataKey());
    }

    private NamespacedKey legacyDataKey() {
        return new NamespacedKey(Adapt.instance, Integer.toString(getType().getCanonicalName().hashCode()));
    }
}
