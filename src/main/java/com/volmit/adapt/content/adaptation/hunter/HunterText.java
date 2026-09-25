package com.volmit.adapt.content.adaptation.hunter;

import com.volmit.adapt.util.Element;
import com.volmit.adapt.util.Localizer;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;

final class HunterText {
    private HunterText() {
    }

    static void addStats(Element element, int level, String key) {
        element.addLore(Localizer.components("hunter", key, "lore",
                Placeholder.unparsed("level", Integer.toString(level)),
                Placeholder.unparsed("penalty", Integer.toString(5 + level))));
    }
}
