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
import com.volmit.adapt.nms.NMS;
import com.volmit.adapt.util.C;
import com.volmit.adapt.util.Element;
import com.volmit.adapt.util.Localizer;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;


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

        CraftingStation station = CraftingStation.getStation(hand.getType());
        if (station == null) {
            return;
        }

        NMS.get().sendCooldown(p, hand.getType(), 1000);
        p.setCooldown(hand.getType(), 1000);

        if (e.getAction().equals(Action.RIGHT_CLICK_AIR) || e.getAction().equals(Action.LEFT_CLICK_AIR) || e.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
            p.playSound(p.getLocation(), Sound.PARTICLE_SOUL_ESCAPE, 1f, 0.10f);
            p.playSound(p.getLocation(), Sound.BLOCK_ENDER_CHEST_OPEN, 1f, 0.10f);
            Inventory inv = Bukkit.createInventory(p, station.getInventoryType());
            p.openInventory(inv);
        }
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

    enum CraftingStation {
        CRAFTING_TABLE(Material.CRAFTING_TABLE, InventoryType.WORKBENCH),
        GRINDSTONE(Material.GRINDSTONE, InventoryType.GRINDSTONE),
        ANVIL(Material.ANVIL, InventoryType.ANVIL),
        STONECUTTER(Material.STONECUTTER, InventoryType.STONECUTTER),
        CARTOGRAPHY_TABLE(Material.CARTOGRAPHY_TABLE, InventoryType.CARTOGRAPHY),
        LOOM(Material.LOOM, InventoryType.LOOM);

        private Material material;
        private InventoryType inventoryType;

        CraftingStation(Material material, InventoryType inventoryType) {
            this.material = material;
            this.inventoryType = inventoryType;
        }

        public static CraftingStation getStation(Material material) {
            for (CraftingStation station : values()) {
                if (station.getMaterial() == material) {
                    return station;
                }
            }
            return null;
        }

        public Material getMaterial() {
            return material;
        }

        public InventoryType getInventoryType() {
            return inventoryType;
        }
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