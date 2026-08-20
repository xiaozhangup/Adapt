package com.volmit.adapt;

import com.google.common.collect.Maps;
import com.volmit.adapt.api.adaptation.Adaptation;
import com.volmit.adapt.api.skill.Skill;
import com.volmit.adapt.api.skill.SkillRegistry;
import com.volmit.adapt.api.world.PlayerData;
import com.volmit.adapt.api.world.PlayerSkillLine;
import com.volmit.adapt.util.Color;
import com.volmit.adapt.util.Localizer;
import com.volmit.adapt.util.Components;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.JoinConfiguration;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiFunction;
import java.util.function.Function;

public class PapiExpansion extends PlaceholderExpansion {

    private final Map<String, Function<PlayerSkillLine, String>> skillMap = Maps.newHashMap();
    private final Map<String, Function<PlayerData, String>> playerMap = Maps.newHashMap();
    private final Map<String, BiFunction<PlayerData, Adaptation<?>, String>> adaptationMap = Maps.newHashMap();

    public PapiExpansion() {
        // this should be %adapt_skill_level%, %adapt_skill_knowledge%,
        // %adapt_skill_xp%, %adapt_skill_freshness%, %adapt_skill_multiplier%,
        // %adapt_skill_name%
        // where skill is the id of the skill eg: %adapt_herbalism_level%
        skillMap.put("level",
                skill -> String.valueOf(skill.getLevel()).equals("-5000") ? "0" : String.valueOf(skill.getLevel()));
        skillMap.put("knowledge",
                skill -> String.valueOf(skill.getKnowledge()).equals("-5000")
                        ? "0"
                        : String.valueOf(skill.getKnowledge()));
        skillMap.put("xp",
                skill -> String.format("%.2f", skill.getXp()).equals("-5000.00")
                        ? "0"
                        : String.format("%.2f", skill.getXp()));
        skillMap.put("freshness",
                skill -> String.valueOf(skill.getFreshness()).equals("-5000")
                        ? "0"
                        : String.valueOf(skill.getFreshness()));
        skillMap.put("multiplier",
                skill -> String.valueOf(skill.getMultiplier()).equals("-5000")
                        ? "0"
                        : String.valueOf(skill.getMultiplier()));
        skillMap.put("name", skill -> Components.legacyString(Localizer.component("skill", skill.getLine(), "name")));

        // this should be %adapt_player_level%, %adapt_player_multiplier%,
        // %adapt_player_availablepower%, %adapt_player_maxpower%,
        // %adapt_player_usedpower%, %adapt_player_wisdom%, %adapt_player_masterxp%,
        // %adapt_player_seenthings%
        // the player is provided by the ingame context
        playerMap.put("level",
                playerData -> String.valueOf(playerData.getMultiplier()).equals("-5000")
                        ? "0"
                        : String.valueOf(playerData.getLevel()));
        playerMap.put("multiplier",
                playerData -> String.valueOf(playerData.getMultiplier()).equals("-5000")
                        ? "0"
                        : String.valueOf(playerData.getMultiplier()));
        playerMap.put("availablepower",
                playerData -> String.valueOf(playerData.getAvailablePower()).equals("-5000")
                        ? "0"
                        : String.valueOf(playerData.getAvailablePower()));
        playerMap.put("maxpower",
                playerData -> String.valueOf(playerData.getMaxPower()).equals("-5000")
                        ? "0"
                        : String.valueOf(playerData.getMaxPower()));
        playerMap.put("usedpower",
                playerData -> String.valueOf(playerData.getUsedPower()).equals("-5000")
                        ? "0"
                        : String.valueOf(playerData.getUsedPower()));
        playerMap.put("wisdom",
                playerData -> String.valueOf(playerData.getWisdom()).equals("-5000")
                        ? "0"
                        : String.valueOf(playerData.getWisdom()));
        playerMap.put("masterxp",
                playerData -> String.valueOf(playerData.getMasterXp()).equals("-5000")
                        ? "0"
                        : String.valueOf(playerData.getMasterXp()));
        playerMap.put("seenthings", playerData -> String.valueOf(playerData.getSeenBlocks().getSeen().size()
                + playerData.getSeenBiomes().getSeen().size() + playerData.getSeenEnchants().getSeen().size()
                + playerData.getSeenEnvironments().getSeen().size() + playerData.getSeenFoods().getSeen().size()
                + playerData.getSeenItems().getSeen().size() + playerData.getSeenMobs().getSeen().size()
                + playerData.getSeenPeople().getSeen().size() + playerData.getSeenPotionEffects().getSeen().size()
                + playerData.getSeenRecipes().getSeen().size() + playerData.getSeenPotionEffects().getSeen().size()
                + playerData.getSeenWorlds().getSeen().size()));

        // this should be %adapt_adaptation_<ID>_level%,
        // %adapt_adaptation_<ID>_maxlevel%
        // where adaptation is the adaptation id (e.g.
        // %adapt_adaptation_stealth-ghost-armor_level%)
        adaptationMap.put("maxlevel", (playerData, adaptation) -> String.valueOf(adaptation.getMaxLevel()));
        adaptationMap.put("level",
                (playerData, adaptation) -> String.valueOf(getAdaptionLevel(adaptation, playerData)));
        adaptationMap.put("name", (playerData, adaptation) -> getAdaptionLocalizedName(adaptation));
    }

    private static List<String> getElementsFromSecond(String[] array) {
        if (array == null || array.length < 2) {
            return new ArrayList<>();
        }

        return Arrays.asList(array).subList(1, array.length);
    }

    private Integer getAdaptionLevel(Adaptation<?> adaptation, PlayerData playerData) {
        List<Skill<?>> skills = Adapt.instance.getAdaptServer().getSkillRegistry().getSkills();
        for (Skill<?> skill : skills) {
            List<Adaptation<?>> adaptations = skill.getAdaptations();
            for (Adaptation<?> a : adaptations) {
                if (a.equals(adaptation)) {
                    PlayerSkillLine line = playerData.getNullableSkillLine(skill.getName());
                    return line == null ? 0 : line.getAdaptationLevel(adaptation.getName());
                }
            }
        }
        return 0;
    }

    private String getAdaptionLocalizedName(Adaptation<?> adaptation) {
        List<Skill<?>> skills = Adapt.instance.getAdaptServer().getSkillRegistry().getSkills();
        for (Skill<?> skill : skills) {
            List<Adaptation<?>> adaptations = skill.getAdaptations();
            for (Adaptation<?> a : adaptations) {
                if (a.equals(adaptation)) {
                    return Components.legacyString(adaptation.getDisplayName());
                }
            }
        }
        return "Unknown";
    }

    @Override
    public @NotNull String getIdentifier() {
        return Adapt.instance.getDescription().getName().toLowerCase();
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", Adapt.instance.getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return Adapt.instance.getDescription().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public @Nullable String onRequest(@Nullable OfflinePlayer player, @NotNull String params) {
        if (player == null || params == null || params.isBlank()) {
            return "";
        }

        Adapt plugin = Adapt.instance;
        if (plugin == null || !plugin.isEnabled()) {
            return "";
        }

        UUID playerId = player.getUniqueId();
        if (Bukkit.isPrimaryThread()) {
            return resolve(playerId, params);
        }

        Future<String> request;
        try {
            request = Bukkit.getScheduler().callSyncMethod(plugin, () -> resolve(playerId, params));
        } catch (RuntimeException error) {
            return "";
        }

        try {
            return request.get(2, TimeUnit.SECONDS);
        } catch (InterruptedException error) {
            request.cancel(false);
            Thread.currentThread().interrupt();
            return "";
        } catch (TimeoutException error) {
            request.cancel(false);
            return "";
        } catch (CancellationException | ExecutionException error) {
            return "";
        }
    }

    private @Nullable String resolve(UUID playerId, String params) {
        Adapt plugin = Adapt.instance;
        if (plugin == null || plugin.getAdaptServer() == null) {
            return "";
        }

        PlayerData p = plugin.getAdaptServer().getPlayerData(playerId).orElse(null);
        if (p == null) {
            return "";
        }

        String[] args = params.split("_", -1);
        String key = args[0];

        // Handle player attributes
        if (key.equals("player")) {
            String playerAttr = args.length > 1 ? args[1] : "";
            if (playerMap.containsKey(playerAttr)) {
                return playerMap.get(playerAttr).apply(p);
            }
        }

        // Handle skill attributes
        if (key.equals("skill")) {
            String skillID = args.length > 1 ? args[1] : "";
            if (skillID.isBlank() || SkillRegistry.skills.get(skillID) == null) {
                return "";
            }
            PlayerSkillLine line = p.getNullableSkillLine(skillID);
            String skillAttr = args.length > 2 ? args[2] : "";
            if (line != null && skillMap.containsKey(skillAttr)) {
                return skillMap.get(skillAttr).apply(line);
            }
        }

        // Handle colored icons
        if (key.equals("icons")) {
            List<Component> icons = new ArrayList<>();
            for (String s : getElementsFromSecond(args)) {
                var skill = SkillRegistry.skills.get(s);
                if (skill == null) {
                    continue;
                }
                var made = p.getNullableSkillLine(skill.getName());

                if (made == null) {
                    icons.add(Component.empty().color(NamedTextColor.WHITE).append(skill.getEmojiName()));
                } else {
                    var gradiented = Color.gradientWhiteColors(
                            Color.color2Hex(new java.awt.Color(skill.getColor().value())), 16);
                    var color = gradiented.get(Math.max(0, Math.min(made.getLevel(), 15)));

                    icons.add(Component.empty().color(TextColor.fromHexString(color)).append(skill.getEmojiName()));
                }
            }

            return String.join(" ", icons.stream()
                    .map(icon -> Components.legacyString(icon) + Components.legacyReset()).toList());
        }

        if (key.equals("pack")) {
            if (args.length < 2 || args[1].isBlank()) {
                return "";
            }
            var skill = SkillRegistry.skills.get(args[1]);
            if (skill == null) {
                return "";
            }
            var made = p.getNullableSkillLine(skill.getName());
            Component text;
            if (made == null) {
                text = Component.empty().color(NamedTextColor.WHITE).append(skill.getEmojiName())
                        .append(Component.text(" 0", NamedTextColor.RED));
            } else {
                var gradiented = Color.gradientWhiteColors(
                        Color.color2Hex(new java.awt.Color(skill.getColor().value())), 16);
                var color = gradiented.get(Math.max(0, Math.min(made.getLevel(), 15)));

                text = Component.empty().color(TextColor.fromHexString(color)).append(skill.getEmojiName())
                        .append(Component.text(" " + made.getLevel()));
            }

            return Components.legacyString(text);
        }

        if (key.equals("miniicons")) {
            List<Component> icons = new ArrayList<>();
            for (String s : getElementsFromSecond(args)) {
                var skill = SkillRegistry.skills.get(s);
                if (skill == null) {
                    continue;
                }
                var made = p.getNullableSkillLine(skill.getName());

                if (made == null) {
                    icons.add(Component.empty().color(NamedTextColor.WHITE).append(skill.getEmojiName()));
                } else {
                    var gradiented = Color.gradientWhiteColors(
                            Color.color2Hex(new java.awt.Color(skill.getColor().value())), 16);
                    var color = gradiented.get(Math.max(0, Math.min(made.getLevel(), 15)));

                    icons.add(Component.empty().color(TextColor.fromHexString(color)).append(skill.getEmojiName()));
                }
            }

            return Components.miniString(Component.join(JoinConfiguration.separator(Component.space()), icons));
        }

        // Handle adaptation attributes
        if (key.equals("adaptation")) {
            String adaptID = args.length > 1 ? args[1] : "";
            String adaptAttr = args.length > 2 ? args[2] : "";
            Adapt.verbose("Triggered adaptation Lookup: " + adaptID + " " + adaptAttr);
            List<Skill<?>> skill = Adapt.instance.getAdaptServer().getSkillRegistry().getSkills();

            for (Skill<?> s : skill) {
                List<Adaptation<?>> adaptations = s.getAdaptations();
                for (Adaptation<?> a : adaptations) {
                    String adaptationIdWithoutUUID = a.getName();
                    Adapt.verbose(adaptID + " " + adaptationIdWithoutUUID);
                    if (adaptationIdWithoutUUID.equals(adaptID)) {
                        Adapt.verbose("Found adaptation: " + a.getId());
                        if ("level".equalsIgnoreCase(adaptAttr)) {
                            Adapt.verbose("Doing Level Lookup");
                            return adaptationMap.get("level").apply(p, a);
                        } else if ("maxlevel".equalsIgnoreCase(adaptAttr)) {
                            Adapt.verbose("Doing MaxLevel Lookup");
                            return adaptationMap.get("maxlevel").apply(p, a);
                        } else if ("name".equalsIgnoreCase(adaptAttr)) {
                            Adapt.verbose("Doing Name Lookup");
                            return adaptationMap.get("name").apply(p, a);
                        }
                    }
                }
            }
        }
        return null;
    }
}
