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

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class MultiArmor implements MultiItem {
    public static List<String> getLoreWithout(ItemMeta meta) {
        List<String> list = meta.getLore();
        if (list == null) {
            return null;
        }

        String targetText = "复合盔甲";
        List<String> removeList = new ArrayList<>();

        Iterator<String> iterator = list.iterator();
        while (iterator.hasNext()) {
            String current = iterator.next();
            if (current.contains(targetText)) {
                removeList.add(current);
                while (iterator.hasNext()) {
                    String next = iterator.next();
                    if (next.contains("-> ") || next.contains("-  ") || next.equals(" ")) {
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

    private static String legacy(Component component) {
        return LegacyComponentSerializer.legacySection().serialize(component);
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
        List<String> list = getLoreWithout(meta);
        meta.setLore(list); // 清理旧式的描述内容

        List<Component> lore = new ArrayList<>();
        lore.add(Component.text("复合盔甲 (" + (otherItems.size() + 1) + " 个物品)").color(NamedTextColor.GRAY)
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        lore.add(Component.text("-> ").append(Component.translatable(item.translationKey())).color(NamedTextColor.GRAY)
                .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));

        for (ItemStack i : otherItems) {
            lore.add(Component.text("-  ").append(Component.translatable(i.translationKey())).color(NamedTextColor.GRAY)
                    .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE));
        }

        List<Component> old = meta.lore();
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
