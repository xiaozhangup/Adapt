package com.volmit.adapt.util.command;

import com.volmit.adapt.util.Components;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Sound;

public class FConst {
    public static Feedback error(String message, Object... args) {
        return Feedback.builder().message(errorText(message, args))
                .sound(SoundFeedback.builder().sound(Sound.BLOCK_DEEPSLATE_BREAK).pitch(0.5f).volume(1f).build())
                .build();
    }

    public static Feedback success(String message, Object... args) {
        return Feedback.builder().message(successText(message, args))
                .sound(SoundFeedback.builder().sound(Sound.BLOCK_AMETHYST_BLOCK_PLACE).pitch(1.5f).volume(1f).build())
                .sound(SoundFeedback.builder().sound(Sound.ITEM_ARMOR_EQUIP_ELYTRA).pitch(1.1f).volume(1f).build())
                .build();
    }

    public static Feedback warning(String message, Object... args) {
        return Feedback.builder().message(warningText(message, args))
                .sound(SoundFeedback.builder().sound(Sound.ITEM_ARMOR_EQUIP_CHAIN).pitch(0.6f).volume(1f).build())
                .build();
    }

    public static Feedback info(String message, Object... args) {
        return Feedback.builder().message(infoText(message, args))
                .sound(SoundFeedback.builder().sound(Sound.ITEM_ARMOR_EQUIP_LEATHER).pitch(1.1f).volume(1f).build())
                .build();
    }

    public static Component errorText(String message, Object... args) {
        return colored("<#ff0000><message>", message, args);
    }

    public static Component successText(String message, Object... args) {
        return colored("<#00ff00><message>", message, args);
    }

    public static Component warningText(String message, Object... args) {
        return colored("<#ffff00><message>", message, args);
    }

    public static Component infoText(String message, Object... args) {
        return colored("<#ffffff><message>", message, args);
    }

    private static Component colored(String template, String message, Object... args) {
        return Components.mini(template, Placeholder.unparsed("message", message.formatted(args)));
    }
}
