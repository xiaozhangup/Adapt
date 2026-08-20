package com.volmit.adapt.util.command;

import com.volmit.adapt.util.Components;
import com.volmit.adapt.util.VolmitSender;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.experimental.Accessors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

@Builder
@Data
@Accessors(chain = true, fluent = true)
public class Feedback {
    @Singular
    private List<SoundFeedback> sounds;
    @Singular
    private List<Component> messages;

    public void send(CommandSender serverOrPlayer) {
        if (serverOrPlayer instanceof Player p) {
            for (SoundFeedback i : sounds) {
                i.play(p);
            }
        }

        for (Component i : messages) {
            serverOrPlayer.sendMessage(Components.mini(
                    "<dark_gray>[<#cddced>属性<dark_gray>] <message>",
                    Placeholder.component("message", i)));
        }
    }

    public void send(VolmitSender sender) {
        send(sender.getS());
    }
}
