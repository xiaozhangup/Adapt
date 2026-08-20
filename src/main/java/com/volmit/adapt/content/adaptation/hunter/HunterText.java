package com.volmit.adapt.content.adaptation.hunter;

import com.volmit.adapt.util.Element;
import com.volmit.adapt.util.Localizer;
import com.volmit.adapt.util.Components;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

final class HunterText {
    private HunterText() {
    }

    static void addStats(Element element, int level, String key, boolean extraPenalty) {
        element.addLore(line("<gray><lore>", key, "lore1", level));
        element.addLore(line("<green>+ <level><gray><lore>", key, "lore2", level));
        element.addLore(line("<red>- <penalty><gray><lore>", key, "lore3", level));
        element.addLore(line("<gray>* <level> <lore>", key, "lore4", level));
        element.addLore(line("<gray>* <level> <lore>", key, "lore5", level));
        if (extraPenalty) {
            element.addLore(Components.mini("<red>* <level><gray> <lore>",
                    Placeholder.unparsed("level", Integer.toString(level)),
                    Placeholder.component("lore", Localizer.component("hunter", "penalty", "lore1"))));
        }
        element.addLore(Components.mini("<gray>- <level><red> <lore>",
                Placeholder.unparsed("level", Integer.toString(level)),
                Placeholder.component("lore", Localizer.component("hunter", "penalty", "lore1"))));
    }

    private static Component line(String format, String key, String lore, int level) {
        return Components.mini(format,
                Placeholder.unparsed("level", Integer.toString(level)),
                Placeholder.unparsed("penalty", Integer.toString(5 + level)),
                Placeholder.component("lore", Localizer.component("hunter", key, lore)));
    }
}
