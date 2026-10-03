package com.volmit.adapt.api.advancement;

import com.github.retrooper.packetevents.protocol.advancements.AdvancementHolder;
import com.github.retrooper.packetevents.protocol.advancements.AdvancementProgress;
import com.github.retrooper.packetevents.resources.ResourceLocation;
import com.volmit.adapt.AdaptConfig;
import com.volmit.adapt.api.skill.Skill;
import com.volmit.adapt.api.world.AdaptPlayer;
import com.volmit.adapt.api.world.PlayerData;
import com.volmit.adapt.util.AdvancementUtils;
import com.volmit.adapt.util.J;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.volmit.adapt.Adapt.instance;

public class AdvancementManager {
    private final Map<String, RegisteredAdvancement> advancements = new LinkedHashMap<>();
    private final Map<String, List<RegisteredAdvancement>> tabs = new LinkedHashMap<>();
    private volatile boolean enabled;

    public void grant(AdaptPlayer player, String key, boolean toast) {
        player.getData().ensureGranted(key);
        if (!AdaptConfig.get().isAdvancements() || !enabled)
            return;

        J.s(() -> {
            if (!enabled || !player.isActive() || !player.getPlayer().isOnline())
                return;

            RegisteredAdvancement advancement = advancements.get(key);
            // Saved adaptations may have been disabled since the player last joined.
            if (advancement == null)
                return;
            syncTab(player, tabs.get(advancement.root()));
            if (toast || advancement.definition().isToast()) {
                AdaptAdvancement definition = advancement.definition();
                AdvancementUtils.displayToast(player.getPlayer(), definition.getDisplayIcon(), definition.getTitle(),
                        definition.getDescription(), definition.getFrame());
            }
        }, 5);
    }

    public void unlockExisting(AdaptPlayer player) {
        if (!AdaptConfig.get().isAdvancements() || !enabled)
            return;

        J.s(() -> {
            if (!enabled || !player.isActive() || !player.getPlayer().isOnline())
                return;

            tabs.values().forEach(tab -> syncTab(player, tab));
            player.getAdvancementHandler().setReady(true);
        }, 20);
    }

    private void syncTab(AdaptPlayer player, List<RegisteredAdvancement> tab) {
        PlayerData data = player.getData();
        Set<String> visible = new HashSet<>();
        if (data.isGranted(tab.getFirst().definition().getKey())) {
            for (RegisteredAdvancement advancement : tab) {
                String parent = advancement.parent();
                String grandparent = parent == null ? null : advancements.get(parent).parent();
                if (parent == null || advancement.definition().getVisibility()
                        .isVisible(data, advancement.definition().getKey(), parent, grandparent)) {
                    // Visible children need their ancestors for the client to attach them to the tree.
                    for (RegisteredAdvancement ancestor = advancement; ancestor != null;
                            ancestor = ancestor.parent() == null ? null : advancements.get(ancestor.parent())) {
                        visible.add(ancestor.definition().getKey());
                    }
                }
            }
        }

        Set<ResourceLocation> sent = player.getAdvancementHandler().getVisibleAdvancements();
        List<AdvancementHolder> added = new ArrayList<>();
        Set<ResourceLocation> removed = new HashSet<>();
        Map<ResourceLocation, AdvancementProgress> progress = new LinkedHashMap<>();
        for (RegisteredAdvancement advancement : tab) {
            ResourceLocation id = advancement.holder().getIdentifier();
            if (visible.contains(advancement.definition().getKey())) {
                if (!sent.contains(id))
                    added.add(advancement.holder());
                progress.put(id, AdvancementUtils.progress(data.isGranted(advancement.definition().getKey())));
            } else if (sent.contains(id)) {
                removed.add(id);
            }
        }

        if (!added.isEmpty() || !removed.isEmpty() || !progress.isEmpty()) {
            AdvancementUtils.send(player.getPlayer(), added, removed, progress, false);
            sent.removeAll(removed);
            added.forEach(advancement -> sent.add(advancement.getIdentifier()));
        }
    }

    public void enable() {
        if (!AdaptConfig.get().isAdvancements() || enabled)
            return;

        for (Skill<?> skill : instance.getAdaptServer().getSkillRegistry().getSkills()) {
            AdaptAdvancement root = skill.buildAdvancements();
            List<RegisteredAdvancement> tab = new ArrayList<>();
            register(root, null, root.getKey(), tab, 0, 0);
            tabs.put(root.getKey(), tab);
        }
        enabled = true;
        instance.getAdaptServer().getAdaptPlayers()
                .forEach(player -> unlockExisting(instance.getAdaptServer().getPlayer(player)));
    }

    private int register(AdaptAdvancement definition, RegisteredAdvancement parent, String root,
            List<RegisteredAdvancement> tab, int index, int depth) {
        ResourceLocation id = new ResourceLocation("adapt_" + root, definition.getKey());
        AdvancementHolder holder = definition.toAdvancement(id,
                parent == null ? null : parent.holder().getIdentifier(), index, depth);
        RegisteredAdvancement advancement = new RegisteredAdvancement(definition,
                parent == null ? null : parent.definition().getKey(), root, holder);
        advancements.put(definition.getKey(), advancement);
        tab.add(advancement);

        int descendants = 0;
        for (AdaptAdvancement child : definition.getChildren()) {
            descendants += register(child, advancement, root, tab, descendants, depth + 1);
        }
        return descendants + 1;
    }

    public void disable() {
        enabled = false;
        Set<ResourceLocation> removed = new HashSet<>();
        advancements.values().forEach(advancement -> removed.add(advancement.holder().getIdentifier()));
        if (!removed.isEmpty()) {
            Bukkit.getOnlinePlayers().forEach(player -> AdvancementUtils.send(player, List.of(), removed, Map.of(), false));
        }
        advancements.clear();
        tabs.clear();
    }

    private record RegisteredAdvancement(AdaptAdvancement definition, String parent, String root,
            AdvancementHolder holder) {
    }
}
