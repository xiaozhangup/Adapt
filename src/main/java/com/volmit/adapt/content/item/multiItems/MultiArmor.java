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

package com.volmit.adapt.content.item.multiItems;

import com.volmit.adapt.util.Components;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MultiArmor implements MultiItem {
    public static List<Component> getLoreWithout(ItemMeta meta) {
        List<Component> currentLore = meta.lore();
        if (currentLore == null) {
            return null;
        }
        List<Component> list = new ArrayList<>(currentLore);

        String targetText = "复合盔甲";
        List<Component> removeList = new ArrayList<>();

        Iterator<Component> iterator = list.iterator();
        while (iterator.hasNext()) {
            Component current = iterator.next();
            if (Components.plain(current).contains(targetText)) {
                removeList.add(current);
                while (iterator.hasNext()) {
                    Component next = iterator.next();
                    String text = Components.plain(next);
                    if (text.contains("-> ") || text.contains("-  ") || text.equals(" ")) {
                        removeList.add(next);
                    } else {
                        break;
                    }
                }
                break;
            }
        }

        list.removeAll(removeList);
        return list;
    }

    @Override
    public boolean supportsItem(ItemStack itemStack) {
        return true;
    }

    @Override
    public String getKey() {
        return "multiarmor";
    }

    @Override
    public void onApplyMeta(ItemStack item, ItemMeta meta, List<ItemStack> otherItems) {
        List<Component> old = getLoreWithout(meta);

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("复合盔甲 (" + (otherItems.size() + 1) + " 个物品)").color(NamedTextColor.GRAY)
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        lore.add(Component.text("-> ").append(Component.translatable(item.translationKey())).color(NamedTextColor.GRAY)
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));

        for (ItemStack i : otherItems) {
            lore.add(Component.text("-  ").append(Component.translatable(i.translationKey())).color(NamedTextColor.GRAY)
                    .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        }

        if (old != null) {
            lore.add(Component.text(" "));
            lore.addAll(old);
        }
        meta.lore(lore);
    }

    public ItemStack nextElytra(ItemStack item) {
        return nextMatching(item, i -> i.getType().equals(Material.ELYTRA));
    }

    public ItemStack nextChestplate(ItemStack item) {
        return nextMatching(item, i -> i.getType().name().endsWith("_CHESTPLATE"));
    }

}
