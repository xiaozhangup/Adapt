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

package com.volmit.adapt.content.adaptation.crafting;

import com.volmit.adapt.api.adaptation.SimpleAdaptation;
import com.volmit.adapt.util.C;
import com.volmit.adapt.util.Element;
import com.volmit.adapt.util.Localizer;
import com.volmit.adapt.util.SoundPlayer;
import lombok.NoArgsConstructor;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MenuType;

public class CraftingStations extends SimpleAdaptation<CraftingStations.Config> {
    public CraftingStations() {
        super("crafting-stations");
        registerConfiguration(Config.class);
        setDescription(Localizer.dLocalize("crafting", "stations", "description"));
        setDisplayName(Localizer.dLocalize("crafting", "stations", "name"));
        setIcon(Material.CRAFTING_TABLE);
        setBaseCost(getConfig().baseCost);
        setCostFactor(getConfig().costFactor);
        setMaxLevel(getConfig().maxLevel);
        setInitialCost(getConfig().initialCost);
        setInterval(9248);
    }

    @Override
    public void addStats(int level, Element v) {
        v.addLore(C.GRAY + Localizer.dLocalize("crafting", "stations", "lore2"));
        v.addLore(C.GRAY + "");
        v.addLore(C.RED + Localizer.dLocalize("crafting", "stations", "lore3"));
        v.addLore(C.GRAY + Localizer.dLocalize("crafting", "stations", "lore4"));
    }

    @SuppressWarnings("UnstableApiUsage")
    @EventHandler
    public void on(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        if (!hasAdaptation(p)) {
            return;
        }
        if (!p.isSneaking()) {
            return;
        }

        ItemStack hand = p.getInventory().getItemInMainHand();

        if (p.hasCooldown(hand.getType())) {
            e.setCancelled(true);
            return;
        }

        if ((e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.LEFT_CLICK_AIR)
                || e.getAction().equals(Action.LEFT_CLICK_BLOCK))) {

            SoundPlayer sp = SoundPlayer.of(p);
            switch (hand.getType()) {
                case CRAFTING_TABLE -> {
                    playEffect(p, hand, sp);
                    p.openInventory(MenuType.CRAFTING.create(p));
                }
                case GRINDSTONE -> {
                    playEffect(p, hand, sp);
                    p.openInventory(MenuType.GRINDSTONE.create(p));
                }
                case ANVIL -> {
                    playEffect(p, hand, sp);
                    p.openInventory(MenuType.ANVIL.create(p));
                }
                case STONECUTTER -> {
                    playEffect(p, hand, sp);
                    p.openInventory(MenuType.STONECUTTER.create(p));
                }
                case CARTOGRAPHY_TABLE -> {
                    playEffect(p, hand, sp);
                    p.openInventory(MenuType.CARTOGRAPHY_TABLE.create(p));
                }
                case LOOM -> {
                    playEffect(p, hand, sp);
                    p.openInventory(MenuType.LOOM.create(p));
                }
            }
        }
    }

    private static void playEffect(Player p, ItemStack hand, SoundPlayer sp) {
        p.setCooldown(hand.getType(), 1000);
        sp.play(p.getLocation(), Sound.PARTICLE_SOUL_ESCAPE, 1f, 0.10f);
        sp.play(p.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1f, 0.10f);
    }

    @Override
    public void onTick() {

    }

    @Override
    public boolean isEnabled() {
        return getConfig().enabled;
    }

    @Override
    public boolean isPermanent() {
        return getConfig().permanent;
    }

    @NoArgsConstructor
    protected static class Config {
        public int cooldown = 125;
        boolean permanent = true;
        boolean enabled = true;
        int baseCost = 5;
        int maxLevel = 1;
        int initialCost = 2;
        double costFactor = 1;
    }
}