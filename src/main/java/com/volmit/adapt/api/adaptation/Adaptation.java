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

package com.volmit.adapt.api.adaptation;

import com.google.common.collect.ImmutableSet;
import com.volmit.adapt.Adapt;
import com.volmit.adapt.AdaptConfig;
import com.volmit.adapt.api.advancement.AdaptAdvancement;
import com.volmit.adapt.api.potion.BrewingRecipe;
import com.volmit.adapt.api.protection.Protector;
import com.volmit.adapt.api.recipe.AdaptRecipe;
import com.volmit.adapt.api.skill.Skill;
import com.volmit.adapt.api.tick.Ticked;
import com.volmit.adapt.api.world.AdaptPlayer;
import com.volmit.adapt.api.world.PlayerAdaptation;
import com.volmit.adapt.api.world.PlayerData;
import com.volmit.adapt.api.world.PlayerSkillLine;
import com.volmit.adapt.content.event.AdaptAdaptationUseEvent;
import com.volmit.adapt.util.*;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.title.Title;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Recipe;

import java.time.Duration;
import java.util.*;

public interface Adaptation<T> extends Ticked, com.volmit.adapt.api.Component {
    int getMaxLevel();

    default void xp(Player p, double amount) {
        getSkill().xp(p, amount);
    }

    default void xp(Player p, Location l, double amount) {
        getSkill().xp(p, l, amount);
    }

    default <F> F getStorage(Player p, String key, F defaultValue) {
        PlayerData data = getPlayer(p).getData();
        PlayerSkillLine line = data.getSkillLineNullable(getSkill().getName());
        if (line == null) return defaultValue;
        PlayerAdaptation adaptation = line.getAdaptation(getName());
        if (adaptation == null) return defaultValue;
        Object o = adaptation.getStorage().get(key);
            return o == null ? defaultValue : (F) o;
        }

    default <F> F getStorage(Player p, String key) {
        return getStorage(p, key, null);
    }

    default boolean setStorage(Player p, String key, Object value) {
        PlayerData data = getPlayer(p).getData();
        PlayerSkillLine line = data.getSkillLineNullable(getSkill().getName());
        if (line == null) return false;
        PlayerAdaptation adaptation = line.getAdaptation(getName());
        if (adaptation == null) return false;
        adaptation.getStorage().put(key, value);
        return true;
    }

    default boolean canUse(AdaptPlayer player) {
        Adapt.verbose("Checking if " + player.getPlayer().getName() + " can use " + getName() + "...");
        AdaptAdaptationUseEvent e = new AdaptAdaptationUseEvent(!Bukkit.isPrimaryThread(), player, this);
        Bukkit.getServer().getPluginManager().callEvent(e);
        return (!e.isCancelled());
    }

    default boolean canUse(Player player) {
        return canUse(getPlayer(player));
    }

    default boolean hasBlacklistPermission(Player p, Adaptation a) {
        if (p.isOp()) { // If the player is an operator, bypass the permission check
            return false;
        }
        String blacklistPermission = "adapt.blacklist." + a.getName().replaceAll("-", "");
        Adapt.verbose("Checking if player " + p.getName() + " has blacklist permission " + blacklistPermission);

        return p.hasPermission(blacklistPermission);
    }

    default String getStorageString(Player p, String key, String defaultValue) {
        return getStorage(p, key, defaultValue);
    }

    default String getStorageString(Player p, String key) {
        return getStorage(p, key);
    }

    default Integer getStorageInt(Player p, String key, Integer defaultValue) {
        return getStorage(p, key, defaultValue);
    }

    default Integer getStorageInt(Player p, String key) {
        return getStorage(p, key);
    }

    default Double getStorageDouble(Player p, String key, Double defaultValue) {
        return getStorage(p, key, defaultValue);
    }

    default Double getStorageDouble(Player p, String key) {
        return getStorage(p, key);
    }

    default Boolean getStorageBoolean(Player p, String key, Boolean defaultValue) {
        return getStorage(p, key, defaultValue);
    }

    default Boolean getStorageBoolean(Player p, String key) {
        return getStorage(p, key);
    }

    default Long getStorageLong(Player p, String key, Long defaultValue) {
        return getStorage(p, key, defaultValue);
    }

    default Long getStorageLong(Player p, String key) {
        return getStorage(p, key);
    }

    Class<T> getConfigurationClass();

    void registerConfiguration(Class<T> type);

    boolean isEnabled();

    boolean isPermanent();

    T getConfig();

    AdaptAdvancement buildAdvancements();

    void addStats(int level, Element v);

    int getBaseCost();

    Component getDescription();

    Material getIcon();

    Skill<?> getSkill();

    void setSkill(Skill<?> skill);

    String getName();

    int getInitialCost();

    double getCostFactor();

    List<AdaptRecipe> getRecipes();

    List<BrewingRecipe> getBrewingRecipes();

    void onRegisterAdvancements(List<AdaptAdvancement> advancements);

    default Set<Protector> getProtectors() {
        Set<Protector> protectors = new HashSet<>(Adapt.instance.getProtectorRegistry().getDefaultProtectors());
        Map<String, Boolean> overrides = AdaptConfig.get().getProtectionOverrides().getOrDefault(this.getName(), Collections.emptyMap());
        overrides.forEach((protector, enabled) -> {
            if (enabled) {
                Protector p = Adapt.instance.getProtectorRegistry().getAllProtectors()
                        .stream()
                        .filter(pr -> pr.getName().equals(protector))
                        .findFirst()
                        .orElse(null);
                if (p == null) {
                    Adapt.error("Could not find protector " + protector + " for adaptation " + this.getName() + ". Skipping...");
                } else {
                    protectors.add(p);
                }
            } else {
                protectors.removeIf(pr -> pr.getName().equals(protector));
            }
        });
        return ImmutableSet.copyOf(protectors);
    }

    default boolean canBlockBreak(Player player, Block block) {
        return getProtectors().stream().allMatch(protector -> protector.canBlockBreak(player, block, this));
    }

    default boolean canBlockPlace(Player player, Block block) {
        return getProtectors().stream().allMatch(protector -> protector.canBlockPlace(player, block, this));
    }

    default boolean canPVP(Player player, Location victimLocation) {
        return getProtectors().stream().allMatch(protector -> protector.canPVP(player, victimLocation, this));
    }

    default boolean canPVE(Player player, Location victimLocation) {
        return getProtectors().stream().allMatch(protector -> protector.canPVE(player, victimLocation, this));
    }

    default boolean canInteract(Player player, Location targetLocation) {
        return getProtectors().stream().allMatch(protector -> protector.canInteract(player, targetLocation, this));
    }

    default boolean canAccessChest(Player player, Location chestLocation) {
        return getProtectors().stream().allMatch(protector -> protector.canAccessChest(player, chestLocation, this));
    }

    default boolean checkRegion(Player player) {
        return getProtectors().stream()
                .allMatch(protector -> protector.checkRegion(player, player.getLocation(), this));
    }

    default boolean hasAdaptation(Player p) {
        try {
            if (p == null || p.isDead() || !Adapt.instance.getAdaptServer().isPlayerLoaded(p.getUniqueId())) { // Check if player is not invalid
                return false;
            }
            if (!this.getSkill().isEnabled()) {
                Adapt.verbose(
                        "Skill " + this.getSkill().getName() + " is disabled. Skipping adaptation " + this.getName());
                this.unregister();
            }
            if (getLevel(p) > 0) {
                if (AdaptConfig.get().blacklistedWorlds.contains(p.getWorld().getName())) {
                    Adapt.verbose("Player " + p.getName() + " is in a blacklisted world. Skipping adaptation "
                            + this.getName());
                    return false;
                }
                if (p.getGameMode().equals(GameMode.CREATIVE) || p.getGameMode().equals(GameMode.SPECTATOR)) {
                    Adapt.verbose("Player " + p.getName() + " is in creative or spectator mode. Skipping adaptation "
                            + this.getName());
                    return false;
                }
                if (!checkRegion(p)) {
                    Adapt.verbose(
                            "Player " + p.getName() + " don't have adaptation - " + this.getName() + " permission.");
                    return false;
                }

                if (hasBlacklistPermission(p, this)) {
                    Adapt.verbose(
                            "Player " + p.getName() + " has blacklist permission for adaptation " + this.getName());
                    return false;
                }
                if (!canUse(p)) {
                    Adapt.verbose("Player " + p.getName() + " can't use adaptation, This is an API restriction"
                            + this.getName());
                    return false;
                }
                Adapt.verbose("Player " + p.getName() + " used adaptation " + this.getName());
                return true;
            } else {
                return false;
            }
        } catch (Exception e) {
            if (e instanceof IndexOutOfBoundsException) { // This is that fucking bug with Citizens Spoofing Players. I hate it.
                Adapt.verbose("Citizens/PacketSpoofing is Messing stuff up again. I hate it.");
                Adapt.verbose(e.getMessage());
            } else {
                e.printStackTrace();
            }
            return false;
        }
    }

    default int getLevel(Player p) {
        if (p == null) {
            return 0;
        }
        if (!p.getClass().getSimpleName().equals("CraftPlayer")) {
            Adapt.verbose("Simple name: " + p.getClass().getSimpleName());
            return 0;
        }
        if (!this.getSkill().isEnabled()) {
            this.unregister();
            return 0;
        } else {
            PlayerSkillLine line = getPlayer(p).getData().getSkillLine(getSkill().getName());
            if (line == null)
                return 0;
            return getPlayer(p).getData().getSkillLine(getSkill().getName()).getAdaptationLevel(getName());
        }
    }

    default double getLevelPercent(Player p) {
        if (!this.getSkill().isEnabled()) {
            this.unregister();
        }
        if (!p.getClass().getSimpleName().equals("CraftPlayer")) {
            return 0.0;
        }
        return Math.min(Math.max(0, M.lerpInverse(0, getMaxLevel(), getLevel(p))), 1);
    }

    default double getLevelPercent(int p) {
        return Math.min(Math.max(0, M.lerpInverse(0, getMaxLevel(), p)), 1);
    }

    default int getCostFor(int level) {
        return (int) (Math.max(1, getBaseCost() + (getBaseCost() * (level * getCostFactor()))))
                + (level == 1 ? getInitialCost() : 0);
    }

    default int getPowerCostFor(int level, int myLevel) {
        return level - myLevel;
    }

    default int getCostFor(int level, int myLevel) {
        if (myLevel >= level) {
            return 0;
        }

        int c = 0;

        for (int i = myLevel + 1; i <= level; i++) {
            c += getCostFor(i);
        }

        return c;
    }

    default int getRefundCostFor(int level, int myLevel) {
        if (myLevel <= level) {
            return 0;
        }

        int c = 0;

        for (int i = level + 1; i <= myLevel; i++) {
            c += getCostFor(i);
        }

        return c;
    }

    default Component getDisplayName() {
        if (!this.getSkill().isEnabled()) {
            this.unregister();
        }
        return Component.text(Form.capitalizeWords(
                        getName().replaceAll("\\Q" + getSkill().getName() + "-\\E", "").replaceAll("\\Q-\\E", " ")),
                        getSkill().getColor())
                .decoration(TextDecoration.OBFUSCATED, false).decoration(TextDecoration.BOLD, false)
                .decoration(TextDecoration.STRIKETHROUGH, false).decoration(TextDecoration.UNDERLINED, false)
                .decoration(TextDecoration.ITALIC, false);
    }

    default Component getTitleDisplay() {
        if (!this.getSkill().isEnabled()) {
            this.unregister();
        }
        return Component.text(Form.capitalizeWords(
                        getName().replaceAll("\\Q" + getSkill().getName() + "-\\E", "").replaceAll("\\Q-\\E", " ")),
                        Components.darker(getSkill().getColor()))
                .decoration(TextDecoration.OBFUSCATED, false).decoration(TextDecoration.BOLD, false)
                .decoration(TextDecoration.STRIKETHROUGH, false).decoration(TextDecoration.UNDERLINED, false)
                .decoration(TextDecoration.ITALIC, false);
    }

    default Component getDisplayName(int level) {
        if (!this.getSkill().isEnabled()) {
            this.unregister();
        }
        if (level >= 1) {
            return Components.mini("<name><reset> <white><level><reset>",
                    Placeholder.component("name", getDisplayName()),
                    Placeholder.unparsed("level", Form.toRoman(level)));
        }

        return getDisplayName();
    }

    default Component getDisplayNameNoRoman(int level) {
        if (level >= 1) {
            return Components.mini("<name><reset> <white><level><reset>",
                    Placeholder.component("name", getDisplayName()),
                    Placeholder.unparsed("level", Integer.toString(level)));
        }

        return getDisplayName();
    }

    default BlockFace getBlockFace(Player player, int maxrange) {
        List<Block> lastTwoTargetBlocks = player.getLastTwoTargetBlocks(null, maxrange);
        if (lastTwoTargetBlocks.size() != 2 || !lastTwoTargetBlocks.get(1).getType().isOccluding())
            return null;
        Block targetBlock = lastTwoTargetBlocks.get(1);
        Block adjacentBlock = lastTwoTargetBlocks.get(0);
        return targetBlock.getFace(adjacentBlock);
    }

    default CustomModel getModel() {
        return CustomModel.get(getIcon(), "adaptation", getName(), "icon");
    }

    default CustomModel getModel(int level) {
        var model = CustomModel.get(getIcon(), "adaptation", getName(), "level-" + level);
        if (model.material() == getIcon() && model.model() == 0)
            model = CustomModel.get(Material.PAPER, "snippets", "gui", "level", String.valueOf(level));
        if (model.material() == Material.PAPER && model.model() == 0)
            model = getModel();
        return model;
    }

    default boolean openGui(Player player, boolean checkPermissions) {
        if (hasBlacklistPermission(player, this)) {
            return false;
        } else {
            openGui(player);
            return true;
        }
    }

    default void openGui(Player player) {
        if (!Bukkit.isPrimaryThread()) {
            J.s(() -> openGui(player));
            return;
        }

        SoundPlayer spw = SoundPlayer.of(player.getWorld());
        spw.play(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.1f, 1.255f);
        spw.play(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.7f, 0.655f);
        spw.play(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.3f, 0.855f);
        Window w = new UIWindow(player);
        w.setTag("skill/" + getSkill().getName() + "/" + getName());
        w.setDecorator((window, position, row) -> new UIElement("bg")
                .setName(Component.space())
                .setMaterial(new MaterialBlock(Material.BLACK_STAINED_GLASS_PANE))
                .setModel(CustomModel.get(Material.BLACK_STAINED_GLASS_PANE, "snippets", "gui", "background")));
        w.setResolution(WindowResolution.W9_H6);
        int maxLevel = getMaxLevel();
        boolean wraps = maxLevel > 7;
        w.setViewportHeight(wraps ? 4 : 3);

        int mylevel = getPlayer(player).getSkillLine(getSkill().getName()).getAdaptationLevel(getName());

        long k = getPlayer(player).getData().getSkillLine(getSkill().getName()).getKnowledge();
        for (int i = 1; i <= maxLevel; i++) {
            int index = i - 1;
            int levelRow = wraps ? index / 7 : 0;
            int row = levelRow + 1;
            int rowStart = levelRow * 7;
            int rowSize = wraps ? Math.min(7, maxLevel - rowStart) : maxLevel;
            int pos = w.getPosition(index % 7 + (10 - rowSize) / 2);
            int c = getCostFor(i, mylevel);
            int rc = getRefundCostFor(i - 1, mylevel);
            int pc = getPowerCostFor(i, mylevel);
            int lvl = i;
            Component cost = Component.empty();
            if (mylevel < lvl) {
                cost = AdaptConfig.get().isHardcoreNoRefunds()
                        ? Components.mini(
                                "<white><cost><gray> <knowledge_cost> <dark_red><bold><no_refunds>",
                                Placeholder.unparsed("cost", Integer.toString(c)),
                                Placeholder.component("knowledge_cost",
                                        Localizer.component("snippets", "adaptmenu", "knowledgecost")),
                                Placeholder.component("no_refunds",
                                        Localizer.component("snippets", "adaptmenu", "norefunds")))
                        : Components.mini("<white><cost><gray> <knowledge_cost>",
                                Placeholder.unparsed("cost", Integer.toString(c)),
                                Placeholder.component("knowledge_cost",
                                        Localizer.component("snippets", "adaptmenu", "knowledgecost")));
            }

            Component state;
            if (mylevel >= lvl) {
                if (AdaptConfig.get().isHardcoreNoRefunds()) {
                    state = Components.mini("<green><learned> <dark_red><bold><no_refunds>",
                            Placeholder.component("learned",
                                    Localizer.component("snippets", "adaptmenu", "alreadylearned")),
                            Placeholder.component("no_refunds",
                                    Localizer.component("snippets", "adaptmenu", "norefunds")));
                } else if (isPermanent()) {
                    state = Component.empty();
                } else {
                    state = Components.mini(
                            "<green><learned><gray> <unlearn_refund> <green><refund> <knowledge_cost>",
                            Placeholder.component("learned",
                                    Localizer.component("snippets", "adaptmenu", "alreadylearned")),
                            Placeholder.component("unlearn_refund",
                                    Localizer.component("snippets", "adaptmenu", "unlearnrefund")),
                            Placeholder.unparsed("refund", Integer.toString(rc)),
                            Placeholder.component("knowledge_cost",
                                    Localizer.component("snippets", "adaptmenu", "knowledgecost")));
                }
            } else if (k >= c) {
                state = Components.mini("<blue><click_learn> <name>",
                        Placeholder.component("click_learn",
                                Localizer.component("snippets", "adaptmenu", "clicklearn")),
                        Placeholder.component("name", getDisplayName(i)));
            } else if (k == 0) {
                state = Components.mini("<red><no_knowledge>",
                        Placeholder.component("no_knowledge",
                                Localizer.component("snippets", "adaptmenu", "noknowledge")));
            } else {
                state = Components.mini("<red>(<you_only_have> <white><knowledge><red> <available>)",
                        Placeholder.component("you_only_have",
                                Localizer.component("snippets", "adaptmenu", "youonlyhave")),
                        Placeholder.unparsed("knowledge", Long.toString(k)),
                        Placeholder.component("available",
                                Localizer.component("snippets", "adaptmenu", "knowledgeavailable")));
            }

            Component power = mylevel < lvl && !getPlayer(player).getData().hasPowerAvailable(pc)
                    ? Components.mini("<red><not_enough_power>\n<how_to_level>",
                            Placeholder.component("not_enough_power",
                                    Localizer.component("snippets", "adaptmenu", "notenoughpower")),
                            Placeholder.component("how_to_level",
                                    Localizer.component("snippets", "adaptmenu", "howtolevelup")))
                    : Components.mini("<green><level> <power_drain>",
                            Placeholder.unparsed("level", Integer.toString(lvl)),
                            Placeholder.component("power_drain",
                                    Localizer.component("snippets", "adaptmenu", "powerdrain")));

            Component permanent = isPermanent()
                    ? Components.mini("<red><bold><permanent>",
                            Placeholder.component("permanent",
                                    Localizer.component("snippets", "adaptmenu", "maynotunlearn")))
                    : Component.empty();
            Element de = new UIElement("lp-" + i + "g").setMaterial(new MaterialBlock(getIcon())).setModel(getModel(i))
                    .setName(getDisplayName(i)).setEnchanted(mylevel >= lvl).setProgress(1D)
                    .addLore(Components.mini("<gray><description>",
                            Placeholder.component("description", getDescription())))
                    .addLore(cost).addLore(state).addLore(power).addLore(permanent)
                    .onLeftClick((e) -> {
                        if (mylevel >= lvl) {
                            unlearn(player, lvl, false);
                            spw.play(player.getLocation(), Sound.BLOCK_NETHER_GOLD_ORE_PLACE, 0.7f, 1.355f);
                            spw.play(player.getLocation(), Sound.BLOCK_BEACON_DEACTIVATE, 0.4f, 0.755f);
                            w.close();
                            if (AdaptConfig.get().getLearnUnlearnButtonDelayTicks() != 0) {
                                if (isPermanent()) {
                                    spw.play(player.getLocation(), Sound.ENTITY_BLAZE_DEATH, 0.5f, 1.355f);
                                    player.showTitle(Title.title(Component.space(),
                                            Components.mini("<red><bold><permanent> <name>",
                                                    Placeholder.component("permanent",
                                                            Localizer.component("snippets", "adaptmenu",
                                                                    "maynotunlearn")),
                                                    Placeholder.component("name", getDisplayName(mylevel))),
                                            Title.Times.times(Duration.ofMillis(50), Duration.ofMillis(500),
                                                    Duration.ofMillis(550))));
                                } else {
                                    player.showTitle(Title.title(Component.space(),
                                            Components.mini("<gray><unlearned> <name>",
                                                    Placeholder.component("unlearned",
                                                            Localizer.component("snippets", "adaptmenu", "unlearned")),
                                                    Placeholder.component("name", getDisplayName(mylevel))),
                                            Title.Times.times(Duration.ofMillis(50), Duration.ofMillis(500),
                                                    Duration.ofMillis(550))));
                                }
                            }
                            J.s(() -> openGui(player), AdaptConfig.get().getLearnUnlearnButtonDelayTicks());
                            return;
                        }

                        if (k >= c && getPlayer(player).getData().hasPowerAvailable(pc)) {
                            if (getPlayer(player).getData().getSkillLine(getSkill().getName()).spendKnowledge(c)) {
                                getPlayer(player).getData().getSkillLine(getSkill().getName()).setAdaptation(this, lvl);
                                spw.play(player.getLocation(), Sound.BLOCK_NETHER_GOLD_ORE_PLACE, 0.9f, 1.355f);
                                spw.play(player.getLocation(), Sound.BLOCK_ENCHANTMENT_TABLE_USE, 1.7f, 0.355f);
                                spw.play(player.getLocation(), Sound.BLOCK_BEACON_POWER_SELECT, 0.4f, 0.155f);
                                spw.play(player.getLocation(), Sound.BLOCK_BEACON_ACTIVATE, 0.2f, 1.455f);
                                if (isPermanent()) {
                                    spw.play(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.355f);
                                    spw.play(player.getLocation(), Sound.ITEM_GOAT_HORN_SOUND_1, 0.7f, 1.355f);
                                }
                                w.close();
                                if (AdaptConfig.get().getLearnUnlearnButtonDelayTicks() != 0) {
                                    player.showTitle(Title.title(Component.space(),
                                            Components.mini("<gray><learned> <name>",
                                                    Placeholder.component("learned",
                                                            Localizer.component("snippets", "adaptmenu", "learned")),
                                                    Placeholder.component("name", getDisplayName(lvl))),
                                            Title.Times.times(Duration.ofMillis(50), Duration.ofMillis(250),
                                                    Duration.ofMillis(550))));
                                }
                                J.s(() -> openGui(player), AdaptConfig.get().getLearnUnlearnButtonDelayTicks());
                            } else {
                                spw.play(player.getLocation(), Sound.BLOCK_BAMBOO_HIT, 0.7f, 1.855f);
                            }
                        } else {
                            spw.play(player.getLocation(), Sound.BLOCK_BAMBOO_HIT, 0.7f, 1.855f);
                        }
                    });
            de.addLore(Component.space());
            addStats(lvl, de);
            w.setElement(pos, row, de);
        }

        if (AdaptConfig.get().isGuiBackButton()) {
            int backPos = w.getResolution().getWidth() - 1;
            int backRow = w.getViewportHeight() - 1;
            w.setElement(backPos, backRow,
                    new UIElement("back").setMaterial(new MaterialBlock(Material.RED_BED))
                            .setModel(CustomModel.get(Material.RED_BED, "snippets", "gui", "back"))
                            .setName(Components.mini("<reset><red><!italic><back>",
                                    Placeholder.component("back", Localizer.component("snippets", "gui", "back"))))
                            .onLeftClick((e) -> onGuiClose(player, true)));
        }

        AdaptPlayer a = Adapt.instance.getAdaptServer().getPlayer(player);
        w.setTitle(Components.mini("<title> <dark_gray> <amount> <knowledge>",
                Placeholder.component("title", getTitleDisplay()),
                Placeholder.unparsed("amount",
                        Form.f(a.getSkillLine(getSkill().getName()).getKnowledge())),
                Placeholder.component("knowledge",
                        Localizer.component("snippets", "adaptmenu", "knowledge"))));
        w.onClosed((vv) -> J.s(() -> onGuiClose(player, !AdaptConfig.get().isEscClosesAllGuis())));
        w.open();
        Adapt.instance.getGuiLeftovers().put(player.getUniqueId().toString(), w);
    }

    private void onGuiClose(Player player, boolean openPrevGui) {
        SoundPlayer spw = SoundPlayer.of(player.getWorld());
        spw.play(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 1.1f, 1.255f);
        spw.play(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.7f, 0.655f);
        spw.play(player.getLocation(), Sound.ITEM_BOOK_PAGE_TURN, 0.3f, 0.855f);
        if (openPrevGui) {
            getSkill().openGui(player);
        } else {
            Adapt.instance.getGuiLeftovers().remove(player.getUniqueId().toString());
        }
    }

    default void unlearn(Player player, int lvl, boolean force) {
        if (isPermanent() && !force) {
            return;
        }
        int myLevel = getPlayer(player).getSkillLine(getSkill().getName()).getAdaptationLevel(getName());
        int rc = getRefundCostFor(lvl - 1, myLevel);
        if (!AdaptConfig.get().isHardcoreNoRefunds()) {
            getPlayer(player).getData().getSkillLine(getSkill().getName()).giveKnowledge(rc);
        }
        getPlayer(player).getData().getSkillLine(getSkill().getName()).setAdaptation(this, lvl - 1);
    }

    default void learn(Player player, int lvl, boolean force) {
        int myLevel = getPlayer(player).getSkillLine(getSkill().getName()).getAdaptationLevel(getName());
        int c = getCostFor(lvl, myLevel);
        if (getPlayer(player).getData().hasPowerAvailable(c) || force) {
            if (getPlayer(player).getData().getSkillLine(getSkill().getName()).spendKnowledge(c) || force) {
                getPlayer(player).getData().getSkillLine(getSkill().getName()).setAdaptation(this, lvl);
            }
        }
    }

    default boolean isAdaptationRecipe(Recipe recipe) {
        if (!this.getSkill().isEnabled()) {
            this.unregister();
        }
        for (AdaptRecipe i : getRecipes()) {
            if (i.is(recipe)) {
                return true;
            }
        }
        return false;
    }
}
