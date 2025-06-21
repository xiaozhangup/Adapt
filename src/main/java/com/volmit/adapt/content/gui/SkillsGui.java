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

package com.volmit.adapt.content.gui;

import com.volmit.adapt.Adapt;
import com.volmit.adapt.AdaptConfig;
import com.volmit.adapt.api.skill.Skill;
import com.volmit.adapt.api.world.AdaptPlayer;
import com.volmit.adapt.api.world.PlayerAdaptation;
import com.volmit.adapt.api.world.PlayerSkillLine;
import com.volmit.adapt.api.xp.XP;
import com.volmit.adapt.util.*;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SkillsGui {
    private static final List<Integer> slots = Arrays.asList(11, 12, 13, 14, 15, 20, 21, 22);

    public static void open(Player player) {
        if (!Bukkit.isPrimaryThread()) {
            J.s(() -> open(player));
            return;
        }

        Window w = new UIWindow(player);
        w.setViewportHeight(4); // Resize GUI
        w.setTag("/");
        w.setDecorator((window, position, row) -> new UIElement("bg")
                .setName(" ")
                .setMaterial(new MaterialBlock(Material.BLACK_STAINED_GLASS_PANE))
                .setModel(CustomModel.get(Material.BLACK_STAINED_GLASS_PANE, "snippets", "gui", "background")));

        AdaptPlayer adaptPlayer = Adapt.instance.getAdaptServer().getPlayer(player);
        if (adaptPlayer == null) {
            Adapt.error("Failed to open skills gui for " + player.getName() + " because they are not Online, Were Kicked, Or are a fake player.");
            return;
        }

        List<Integer> locked = new ArrayList<>(slots);

        if (!adaptPlayer.getData().getSkillLines().isEmpty()) {
            for (PlayerSkillLine i : adaptPlayer.getData().getSkillLines().sortV()) {
                if (i.getRawSkill(adaptPlayer).hasBlacklistPermission(adaptPlayer.getPlayer(), i.getRawSkill(adaptPlayer)) || i.getLevel() < 0) {
                    continue;
                }

                Skill<?> sk = Adapt.instance.getAdaptServer().getSkillRegistry().getSkill(i.getLine());
                int adaptationLevel = 0;
                for (PlayerAdaptation adaptation : i.getAdaptations().sortV()) {
                    adaptationLevel = adaptation.getLevel();
                }

                Integer location = getLocation(sk);
                if (location == null) continue;

                int pos = w.getPosition(location);
                int row = w.getRow(location);
                locked.remove(location);

                w.setElement(pos, row, new UIElement("skill-" + sk.getName())
                        .setMaterial(new MaterialBlock(sk.getIcon()))
                        .setModel(sk.getModel())
                        .setName(sk.getDisplayName(i.getLevel()))
                        .setProgress(1D)
                        .addLore(C.UNDERLINE + "" + C.WHITE + i.getKnowledge() + C.RESET + " " + C.GRAY + Localizer.dLocalize("snippets", "gui", "knowledge"))
                        .addLore(C.ITALIC + "" + C.DARK_GREEN + adaptationLevel + " " + C.GRAY + Localizer.dLocalize("snippets", "gui", "powerused"))
                        .onLeftClick((e) -> {
                            w.close();
                            sk.openGui(true, player);
                        }));
            }

            if (AdaptConfig.get().isUnlearnAllButton()) {
                int unlearnAllPos = w.getResolution().getWidth() - 1;
                int unlearnAllRow = w.getViewportHeight() - 1;
                if (w.getElement(unlearnAllPos, unlearnAllRow) != null) unlearnAllRow++;
                w.setElement(unlearnAllPos, unlearnAllRow, new UIElement("unlearn-all")
                        .setMaterial(new MaterialBlock(Material.BARRIER))
                        .setModel(CustomModel.get(Material.BARRIER, "snippets", "gui", "unlearnall"))
                        .setName("" + C.RESET + C.GRAY + Localizer.dLocalize("snippets", "gui", "unlearnall")
                                + (AdaptConfig.get().isHardcoreNoRefunds()
                                ? " " + C.DARK_RED + C.BOLD + Localizer.dLocalize("snippets", "adaptmenu", "norefunds")
                                : ""))
                        .onLeftClick((e) -> {
                            Adapt.instance.getAdaptServer().getSkillRegistry().getSkills().forEach(skill -> skill.getAdaptations().forEach(adaptation -> adaptation.unlearn(player, 1, false)));
                            SoundPlayer spw = SoundPlayer.of(player.getWorld());
                            spw.play(player.getLocation(), Sound.BLOCK_NETHER_GOLD_ORE_PLACE, 0.7f, 1.355f);
                            spw.play(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.4f, 0.755f);
                            w.close();
                            if (AdaptConfig.get().getLearnUnlearnButtonDelayTicks() != 0) {
                                player.sendTitle(" ", C.GRAY + Localizer.dLocalize("snippets", "gui", "unlearnedall"), 1, 5, 11);
                            }
                            J.s(() -> open(player), AdaptConfig.get().getLearnUnlearnButtonDelayTicks());
                        }));
            }

            w.setElement(w.getPosition(24), w.getRow(24), new UIElement("all_skill")
                    .setMaterial(new MaterialBlock(Material.WRITABLE_BOOK))
                    .setName(C.WHITE + "查看所有属性")
                    .addLore(C.GRAY + "总计 21 种")
                    .onLeftClick((e) -> {
                        AllSkillsGui.open(player);
                    }));

            for (int slot : locked) { // 未解锁的显示未点亮
                w.setElement(w.getPosition(slot), w.getRow(slot), new UIElement("locked_skill_" + slot)
                        .setMaterial(new MaterialBlock(Material.RED_STAINED_GLASS_PANE))
                        .setName(C.RED + "未点亮")
                        .addLore(C.GRAY + "在游戏过程中点亮")
                        .onLeftClick((e) -> {
                            w.close();
                            Adapt.messagePlayer(player, C.GRAY + "所有属性均会在游戏过程中根据你的经历(如伐木, 钓鱼, 探索等) 而点亮!");
                        }));
            }

            w.setTitle(Localizer.dLocalize("snippets", "gui", "level") + " " + (int) XP.getLevelForXp(adaptPlayer.getData().getMasterXp()) + " (" + adaptPlayer.getData().getUsedPower() + "/" + adaptPlayer.getData().getMaxPower() + " " + Localizer.dLocalize("snippets", "gui", "powerused") + ")");
            w.open();
            w.onClosed((e) -> Adapt.instance.getGuiLeftovers().remove(player.getUniqueId().toString()));
            Adapt.instance.getGuiLeftovers().put(player.getUniqueId().toString(), w);
        }
    }

    public static Integer getLocation(Skill<?> skill) {
        int slot = 0;

        switch (skill.getName()) {
            case "unarmed" -> slot = 11;
            case "discovery" -> slot = 12;
            case "tragoul" -> slot = 13;
            case "ranged" -> slot = 14;
            case "axes" -> slot = 15;
            case "hunter" -> slot = 20;
            case "seaborne" -> slot = 21;
            case "brewing" -> slot = 22;
        }

        if (slot == 0) return null;
        return slot;
    }
}
