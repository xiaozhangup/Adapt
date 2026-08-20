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

package com.volmit.adapt.util;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.util.ArrayList;

public final class Components {
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.builder()
            .character(LegacyComponentSerializer.SECTION_CHAR)
            .hexColors()
            .useUnusualXRepeatedCharacterHexFormat()
            .build();
    private static final MiniMessage MINI = MiniMessage.miniMessage();

    private Components() {
    }

    public static Component legacy(String text) {
        return LEGACY.deserialize(text == null ? "" : text);
    }

    public static Component mini(String text, TagResolver... resolvers) {
        return MINI.deserialize(text == null ? "" : text, resolvers);
    }

    public static String miniString(Component component) {
        return MINI.serialize(component == null ? Component.empty() : component);
    }

    public static String legacyString(Component component) {
        return LEGACY.serialize(component == null ? Component.empty() : component);
    }

    public static String legacyReset() {
        return new String(new char[]{LegacyComponentSerializer.SECTION_CHAR, 'r'});
    }

    public static TextColor darker(TextColor color) {
        java.awt.Color darker = new java.awt.Color(color.value()).darker();
        return TextColor.color(darker.getRGB() & 0xFFFFFF);
    }

    public static String plain(Component component) {
        return PlainTextComponentSerializer.plainText()
                .serialize(component == null ? Component.empty() : component);
    }

    public static Component capitalize(Component component) {
        return capitalize(component, new boolean[1]);
    }

    public static Component itemColors(Component component) {
        Component result = component;
        if (result.color() != null
                && result.decoration(TextDecoration.ITALIC) == TextDecoration.State.NOT_SET) {
            result = result.decoration(TextDecoration.ITALIC, TextDecoration.State.FALSE);
        }
        if (!result.children().isEmpty()) {
            var children = new ArrayList<Component>(result.children().size());
            for (Component child : result.children()) {
                children.add(itemColors(child));
            }
            result = result.children(children);
        }
        return result;
    }

    private static Component capitalize(Component component, boolean[] done) {
        Component result = component;
        if (!done[0] && component instanceof TextComponent text && !text.content().isEmpty()) {
            int codePoint = text.content().codePointAt(0);
            String first = new String(Character.toChars(Character.toUpperCase(codePoint)));
            result = text.content(first + text.content().substring(Character.charCount(codePoint)));
            done[0] = true;
        }
        if (!done[0] && !result.children().isEmpty()) {
            var children = new ArrayList<Component>(result.children().size());
            for (Component child : result.children()) {
                children.add(capitalize(child, done));
            }
            result = result.children(children);
        }
        return result;
    }
}
