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
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.title.Title;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AllSkillsGui {
    private static final List<Integer> slots = Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16,
            17, 18, 19, 20);

    public static void open(Player player) {
        Window w = new UIWindow(player);
        w.setViewportHeight(4); // Resize GUI
        w.setTag("/all");
        w.setDecorator((window, position, row) -> new UIElement("bg").setName(Component.space())
                .setMaterial(new MaterialBlock(Material.BLACK_STAINED_GLASS_PANE)));

        AdaptPlayer adaptPlayer = Adapt.instance.getAdaptServer().getPlayer(player);
        if (adaptPlayer == null) {
            Adapt.error("Failed to open skills gui for " + player.getName()
                    + " because they are not Online, Were Kicked, Or are a fake player.");
            return;
        }

        List<Integer> locked = new ArrayList<>(slots);

        if (!adaptPlayer.getData().getSkillLines().isEmpty()) {
            int ind = 0;
            for (PlayerSkillLine i : adaptPlayer.getData().getSkillLines().sortV()) {
                if (i.getRawSkill(adaptPlayer).hasBlacklistPermission(adaptPlayer.getPlayer(),
                        i.getRawSkill(adaptPlayer))) {
                    continue;
                }
                Skill<?> sk = Adapt.instance.getAdaptServer().getSkillRegistry().getSkill(i.getLine());
                if (i.getLevel() < 0 && SkillsGui.getLocation(sk) == null)
                    continue;

                int pos = w.getPosition(ind);
                int row = w.getRow(ind);
                int adaptationLevel = 0;
                for (PlayerAdaptation adaptation : i.getAdaptations().sortV()) {
                    adaptationLevel = adaptation.getLevel();
                }

                w.setElement(pos, row,
                        new UIElement("skill-" + sk.getName()).setMaterial(new MaterialBlock(sk.getIcon()))
                                .setName(sk.getDisplayName(i.getLevel())).setProgress(1D)
                                .addLore(Components.mini("<gray><description></gray>",
                                        Placeholder.component("description", sk.getDescription())))
                                .addLore(Components.mini("<white><knowledge></white> <gray><label></gray>",
                                        Placeholder.unparsed("knowledge", Long.toString(i.getKnowledge())),
                                        Placeholder.component("label",
                                                Localizer.component("snippets", "gui", "knowledge"))))
                                .addLore(Components.mini("<dark_green><level> </dark_green><gray><label></gray>",
                                        Placeholder.unparsed("level", Integer.toString(adaptationLevel)),
                                        Placeholder.component("label",
                                                Localizer.component("snippets", "gui", "powerused"))))
                                .onLeftClick((e) -> {
                                    w.close();
                                    sk.openGui(player);
                                }));
                locked.remove((Object) ind);
                ind++;
            }

            if (AdaptConfig.get().isUnlearnAllButton()) {
                int unlearnAllPos = w.getResolution().getWidth() - 1;
                int unlearnAllRow = w.getViewportHeight() - 1;
                if (w.getElement(unlearnAllPos, unlearnAllRow) != null)
                    unlearnAllRow++;
                Component unlearnName = AdaptConfig.get().isHardcoreNoRefunds()
                        ? Components.mini(
                                "<gray><name> </gray><dark_red><bold><warning></bold></dark_red>",
                                Placeholder.component("name", Localizer.component("snippets", "gui", "unlearnall")),
                                Placeholder.component("warning",
                                        Localizer.component("snippets", "adaptmenu", "norefunds")))
                        : Components.mini("<gray><name></gray>",
                                Placeholder.component("name", Localizer.component("snippets", "gui", "unlearnall")));
                w.setElement(unlearnAllPos, unlearnAllRow,
                        new UIElement("unlearn-all").setMaterial(new MaterialBlock(Material.BARRIER))
                                .setName(unlearnName)
                                .onLeftClick((e) -> {
                                    Adapt.instance.getAdaptServer().getSkillRegistry().getSkills()
                                            .forEach(skill -> skill.getAdaptations()
                                                    .forEach(adaptation -> adaptation.unlearn(player, 1, false)));
                                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_NETHER_GOLD_ORE_PLACE,
                                            0.7f, 1.355f);
                                    player.getWorld().playSound(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE,
                                            0.4f, 0.755f);
                                    w.close();
                                    if (AdaptConfig.get().getLearnUnlearnButtonDelayTicks() != 0) {
                                        player.showTitle(Title.title(Components.mini(" "),
                                                Components.mini("<gray><message>",
                                                        Placeholder.component("message",
                                                                Localizer.component("snippets", "gui",
                                                                        "unlearnedall"))),
                                                Title.Times.times(Duration.ofMillis(50), Duration.ofMillis(250),
                                                        Duration.ofMillis(550))));
                                    }
                                    J.s(() -> open(player), AdaptConfig.get().getLearnUnlearnButtonDelayTicks());
                                }));
            }

            if (AdaptConfig.get().isGuiBackButton()) {
                int backPos = w.getResolution().getWidth() - 1;
                int backRow = w.getViewportHeight() - 1;
                if (w.getElement(backPos, backRow) != null)
                    backRow++;
                w.setElement(backPos, backRow,
                        new UIElement("back").setMaterial(new MaterialBlock(Material.RED_BED))
                                .setName(Components.mini("<red><label></red>",
                                        Placeholder.component("label", Localizer.component("snippets", "gui", "back"))))
                                .onLeftClick((e) -> SkillsGui.open(player)));
            }

            for (int slot : locked) { // 未解锁的显示未点亮
                w.setElement(w.getPosition(slot), w.getRow(slot),
                        new UIElement("locked_skill_" + slot)
                                .setMaterial(new MaterialBlock(Material.RED_STAINED_GLASS_PANE))
                                .setName(Components.mini("<red>未点亮</red>"))
                                .addLore(Components.mini("<gray>在游戏过程中点亮</gray>")).onLeftClick((e) -> {
                                    w.close();
                                    Adapt.messagePlayer(player, Components.mini(
                                            "<gray>所有属性均会在游戏过程中根据你的经历(如伐木, 钓鱼, 探索等) 而点亮!"));
                                }));
            }

            w.setTitle(Components.mini("<label> <level> (<used>/<max> <power>)",
                    Placeholder.component("label", Localizer.component("snippets", "gui", "level")),
                    Placeholder.unparsed("level",
                            Integer.toString((int) XP.getLevelForXp(adaptPlayer.getData().getMasterXp()))),
                    Placeholder.unparsed("used", Integer.toString(adaptPlayer.getData().getUsedPower())),
                    Placeholder.unparsed("max", Integer.toString(adaptPlayer.getData().getMaxPower())),
                    Placeholder.component("power", Localizer.component("snippets", "gui", "powerused"))));
            w.open();
            w.onClosed((vv) -> J.s(() -> onGuiClose(player, !AdaptConfig.get().isEscClosesAllGuis())));
            Adapt.instance.getGuiLeftovers().put(player.getUniqueId().toString(), w);
        }
    }

    private static void onGuiClose(Player player, boolean openPrevGui) {
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.1f, 1.255f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.7f, 1.455f);
        player.getWorld().playSound(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.3f, 1.855f);
        if (openPrevGui) {
            SkillsGui.open(player);
        }
    }
}
