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

package com.volmit.adapt.content.skill;

import com.fren_gor.ultimateAdvancementAPI.advancement.display.AdvancementFrameType;
import com.volmit.adapt.Adapt;
import com.volmit.adapt.api.advancement.AdaptAdvancement;
import com.volmit.adapt.api.advancement.AdvancementVisibility;
import com.volmit.adapt.api.skill.SimpleSkill;
import com.volmit.adapt.api.world.AdaptPlayer;
import com.volmit.adapt.api.world.AdaptStatTracker;
import com.volmit.adapt.content.adaptation.agility.AgilityArmorUp;
import com.volmit.adapt.content.adaptation.agility.AgilitySuperJump;
import com.volmit.adapt.content.adaptation.agility.AgilityWallJump;
import com.volmit.adapt.content.adaptation.agility.AgilityWindUp;
import com.volmit.adapt.util.CustomModel;
import com.volmit.adapt.util.Localizer;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SkillAgility extends SimpleSkill<SkillAgility.Config> {
    private final Map<UUID, Movement> pendingMovement = new HashMap<>();

    public SkillAgility() {
        super("agility", Localizer.component("skill", "agility", "icon"));
        registerConfiguration(Config.class);
        setDescription(Localizer.component("skill", "agility", "description"));
        setDisplayName(Localizer.component("skill", "agility", "name"));
        setColor(TextColor.color(0x8cb684));
        setInterval(975);
        setIcon(Material.FEATHER);
        registerAdaptation(new AgilityWindUp());
        registerAdaptation(new AgilityWallJump());
        registerAdaptation(new AgilitySuperJump());
        registerAdaptation(new AgilityArmorUp());
        registerAdvancement(AdaptAdvancement.builder().icon(Material.LEATHER_BOOTS).key("challenge_move_1k")
                .title(Localizer.component("advancement", "challenge_move_1k", "title"))
                .description(Localizer.component("advancement", "challenge_move_1k", "description"))
                .model(CustomModel.get(Material.LEATHER_BOOTS, "advancement", "agility", "challenge_move_1k"))
                .frame(AdvancementFrameType.CHALLENGE).visibility(AdvancementVisibility.PARENT_GRANTED)
                .child(AdaptAdvancement.builder().icon(Material.IRON_BOOTS).key("challenge_sprint_5k")
                        .title(Localizer.component("advancement", "challenge_sprint_5k", "title"))
                        .description(Localizer.component("advancement", "challenge_sprint_5k", "description"))
                        .model(CustomModel.get(Material.IRON_BOOTS, "advancement", "agility", "challenge_sprint_5k"))
                        .frame(AdvancementFrameType.CHALLENGE).visibility(AdvancementVisibility.PARENT_GRANTED)
                        .child(AdaptAdvancement.builder().icon(Material.DIAMOND_BOOTS).key("challenge_sprint_50k")
                                .title(Localizer.component("advancement", "challenge_sprint_50k", "title"))
                                .description(Localizer.component("advancement", "challenge_sprint_50k", "description"))
                                .model(CustomModel.get(Material.DIAMOND_BOOTS, "advancement", "agility",
                                        "challenge_sprint_50k"))
                                .frame(AdvancementFrameType.CHALLENGE).visibility(AdvancementVisibility.PARENT_GRANTED)
                                .child(AdaptAdvancement.builder().icon(Material.NETHERITE_BOOTS)
                                        .key("challenge_sprint_500k")
                                        .title(Localizer.component("advancement", "challenge_sprint_500k", "title"))
                                        .description(Localizer.component("advancement", "challenge_sprint_500k",
                                                "description"))
                                        .model(CustomModel.get(Material.NETHERITE_BOOTS, "advancement", "agility",
                                                "challenge_sprint_500k"))
                                        .frame(AdvancementFrameType.CHALLENGE)
                                        .visibility(AdvancementVisibility.PARENT_GRANTED).build())
                                .build())
                        .build())
                .child(AdaptAdvancement.builder().icon(Material.GOLDEN_BOOTS).key("challenge_sprint_marathon")
                        .title(Localizer.component("advancement", "challenge_sprint_marathon", "title"))
                        .description(Localizer.component("advancement", "challenge_sprint_marathon", "description"))
                        .model(CustomModel.get(Material.GOLDEN_BOOTS, "advancement", "agility",
                                "challenge_sprint_marathon"))
                        .frame(AdvancementFrameType.CHALLENGE).visibility(AdvancementVisibility.PARENT_GRANTED).build())
                .build());
        registerStatTracker(AdaptStatTracker.builder().advancement("challenge_move_1k").goal(1000).stat("move")
                .reward(getConfig().challengeMove1kReward).build());
        registerStatTracker(AdaptStatTracker.builder().advancement("challenge_sprint_5k").goal(5000).stat("move")
                .reward(getConfig().challengeSprint5kReward).build());
        registerStatTracker(AdaptStatTracker.builder().advancement("challenge_sprint_50k").goal(50000).stat("move")
                .reward(getConfig().challengeSprint5kReward).build());
        registerStatTracker(AdaptStatTracker.builder().advancement("challenge_sprint_500k").goal(500000).stat("move")
                .reward(getConfig().challengeSprint5kReward).build());
        registerStatTracker(AdaptStatTracker.builder().advancement("challenge_sprint_marathon").goal(42195).stat("move")
                .reward(getConfig().challengeSprintMarathonReward).build());
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void on(PlayerMoveEvent e) {
        Location from = e.getFrom();
        Location to = e.getTo();
        if (e.isCancelled() || to == null || from.getWorld() == null || !from.getWorld().equals(to.getWorld())) {
            return;
        }

        double x = from.getX() - to.getX();
        double y = from.getY() - to.getY();
        double z = from.getZ() - to.getZ();
        double distanceSquared = x * x + y * y + z * z;
        if (distanceSquared == 0) {
            return;
        }

        Player p = e.getPlayer();
        UUID playerId = p.getUniqueId();
        if (!Adapt.instance.getAdaptServer().isPlayerLoaded(playerId) || !isEnabled()
                || hasBlacklistPermission(p, this) || isWorldBlacklisted(p) || isInCreativeOrSpectator(p)) {
            return;
        }

        double distance = Math.sqrt(distanceSquared);
        Movement movement = pendingMovement.computeIfAbsent(playerId, ignored -> new Movement());
        movement.total += distance;
        if (p.isSneaking()) {
            movement.sneak += distance;
        } else if (p.isFlying()) {
            movement.fly += distance;
        } else if (p.isSwimming()) {
            movement.swim += distance;
        } else if (p.isSprinting()) {
            movement.sprint += distance;
        }
    }

    @EventHandler
    public void on(PlayerQuitEvent event) {
        pendingMovement.remove(event.getPlayer().getUniqueId());
    }

    @Override
    public boolean needsTicking() {
        return true;
    }

    @Override
    public void onTick() {
        for (Player i : Adapt.instance.getAdaptServer().getAdaptPlayers()) {
            Movement movement = pendingMovement.remove(i.getUniqueId());
            shouldReturnForPlayer(i, () -> {
                AdaptPlayer adaptPlayer = getPlayer(i);
                if (movement != null) {
                    flushMovement(i, adaptPlayer, movement);
                }
                checkStatTrackers(adaptPlayer);

                // Check for sprinting
                if (i.isSprinting() && !i.isFlying() && !i.isSwimming() && !i.isSneaking()) {
                    xpSilent(i, getConfig().sprintXpPassive);
                }

                // Check for swimming
                if (i.isSwimming() && !i.isFlying() && !i.isSprinting() && !i.isSneaking()) {
                    xpSilent(i, getConfig().swimXpPassive);
                }

                // Check for jumping
                if (i.getLocation().subtract(0, 1, 0).getBlock().getType().isAir() && !i.isFlying()
                        && !i.isSneaking()) {
                    xpSilent(i, getConfig().jumpXpPassive);
                }

                // Check for climbing ladders
                if (i.getLocation().getBlock().getType() == Material.LADDER && !i.isFlying() && !i.isSneaking()) {
                    xpSilent(i, getConfig().climbXpPassive);
                }
            });
        }
        pendingMovement.keySet().removeIf(uuid -> !Adapt.instance.getAdaptServer().isPlayerLoaded(uuid));
    }

    private void flushMovement(Player player, AdaptPlayer adaptPlayer, Movement movement) {
        adaptPlayer.getData().addStat("move", movement.total);
        addMovementStat(adaptPlayer, "move.sneak", movement.sneak);
        addMovementStat(adaptPlayer, "move.fly", movement.fly);
        addMovementStat(adaptPlayer, "move.swim", movement.swim);
        addMovementStat(adaptPlayer, "move.sprint", movement.sprint);
        xpSilent(player, getConfig().moveXpPassive * movement.total);
    }

    private void addMovementStat(AdaptPlayer player, String stat, double distance) {
        if (distance > 0) {
            player.getData().addStat(stat, distance);
        }
    }

    @Override
    public boolean isEnabled() {
        return getConfig().enabled;
    }

    @NoArgsConstructor
    protected static class Config {
        boolean enabled = true;
        double challengeMove1kReward = 500;
        double challengeSprint5kReward = 2000;
        double challengeSprintMarathonReward = 6500;
        double sprintXpPassive = 1.25;
        double swimXpPassive = 1.25;
        double jumpXpPassive = 0.25;
        double climbXpPassive = 1.25;
        double moveXpPassive = 0.1;
    }

    private static final class Movement {
        private double total;
        private double sneak;
        private double fly;
        private double swim;
        private double sprint;
    }
}
