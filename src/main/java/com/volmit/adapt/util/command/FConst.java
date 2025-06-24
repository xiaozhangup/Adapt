package com.volmit.adapt.util.command;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Sound;

import java.awt.*;

public class FConst {
    public static final Color COLOR_ERROR = new Color(255, 0, 0);
    public static final Color COLOR_SUCCESS = new Color(0, 255, 0);
    public static final Color COLOR_WARNING = new Color(255, 255, 0);
    public static final Color COLOR_INFO = new Color(255, 255, 255);

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

    public static TextComponent errorText(String message, Object... args) {
        return Component.text(message.formatted(args)).color(TextColor.color(FConst.COLOR_ERROR.getRGB()));
    }

    public static TextComponent successText(String message, Object... args) {
        return Component.text(message.formatted(args)).color(TextColor.color(FConst.COLOR_SUCCESS.getRGB()));
    }

    public static TextComponent warningText(String message, Object... args) {
        return Component.text(message.formatted(args)).color(TextColor.color(FConst.COLOR_WARNING.getRGB()));
    }

    public static TextComponent infoText(String message, Object... args) {
        return Component.text(message.formatted(args)).color(TextColor.color(FConst.COLOR_INFO.getRGB()));
    }

}
