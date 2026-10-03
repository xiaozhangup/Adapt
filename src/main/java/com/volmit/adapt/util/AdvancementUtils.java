package com.volmit.adapt.util;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.protocol.advancements.Advancement;
import com.github.retrooper.packetevents.protocol.advancements.AdvancementDisplay;
import com.github.retrooper.packetevents.protocol.advancements.AdvancementHolder;
import com.github.retrooper.packetevents.protocol.advancements.AdvancementProgress;
import com.github.retrooper.packetevents.protocol.advancements.AdvancementType;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerUpdateAdvancements;
import io.github.retrooper.packetevents.util.SpigotConversionUtil;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class AdvancementUtils {
    private static final String CRITERION = "complete";
    private static final List<List<String>> REQUIREMENTS = List.of(List.of(CRITERION));
    private static final ResourceLocation ROOT_KEY = new ResourceLocation("adapt", "notification_root");
    private static final ResourceLocation NOTIFICATION_KEY = new ResourceLocation("adapt", "notification");

    public static AdvancementHolder createAdvancement(ResourceLocation id, ResourceLocation parent, ItemStack icon,
            Component title, Component description, AdvancementType frame, ResourceLocation background,
            boolean toast, boolean hidden, float x, float y) {
        AdvancementDisplay display = new AdvancementDisplay(title, description,
                SpigotConversionUtil.fromBukkitItemStack(icon), frame, background, toast, hidden, x, y);
        return new AdvancementHolder(id, new Advancement(parent, display, REQUIREMENTS, false));
    }

    public static AdvancementProgress progress(boolean granted) {
        return new AdvancementProgress(Map.of(CRITERION,
                new AdvancementProgress.CriterionProgress(granted ? System.currentTimeMillis() : null)));
    }

    public static void send(Player player, List<AdvancementHolder> added, Set<ResourceLocation> removed,
            Map<ResourceLocation, AdvancementProgress> progress, boolean showToast) {
        PacketEvents.getAPI().getPlayerManager().sendPacket(player,
                new WrapperPlayServerUpdateAdvancements(false, added, removed, progress, showToast));
    }

    public static void displayToast(Player player, ItemStack icon, Component title, Component description,
            AdvancementType frame) {
        if (!Bukkit.isPrimaryThread()) {
            J.s(() -> displayToast(player, icon, title, description, frame));
            return;
        }
        if (!player.isOnline())
            return;

        // A root without a display anchors the notification without creating a GUI tab.
        AdvancementHolder root = new AdvancementHolder(ROOT_KEY, new Advancement(null, null, REQUIREMENTS, false));
        AdvancementHolder notification = createAdvancement(NOTIFICATION_KEY, ROOT_KEY, icon, title, description,
                frame, null, true, true, 0, 0);
        send(player, List.of(root, notification), Set.of(),
                Map.of(ROOT_KEY, progress(true), NOTIFICATION_KEY, progress(true)), true);
        send(player, List.of(), Set.of(ROOT_KEY, NOTIFICATION_KEY), Map.of(), false);
    }
}
