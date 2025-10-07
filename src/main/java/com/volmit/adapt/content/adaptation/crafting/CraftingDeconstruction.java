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
import com.volmit.adapt.function.Deconstruction;
import com.volmit.adapt.util.C;
import com.volmit.adapt.util.Element;
import com.volmit.adapt.util.Localizer;
import lombok.NoArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.RayTraceResult;

import java.util.ArrayList;
import java.util.List;

public class CraftingDeconstruction extends SimpleAdaptation<CraftingDeconstruction.Config> {
    private final List<Integer> holds = new ArrayList<>();

    public CraftingDeconstruction() {
        super("crafting-deconstruction");
        registerConfiguration(Config.class);
        setDescription(Localizer.dLocalize("crafting", "deconstruction", "description"));
        setDisplayName(Localizer.dLocalize("crafting", "deconstruction", "name"));
        setIcon(Material.SHEARS);
        setBaseCost(getConfig().baseCost);
        setMaxLevel(1);
        setInterval(5590);
        setInitialCost(getConfig().initialCost);
        setCostFactor(getConfig().costFactor);
    }

    @Override
    public void addStats(int level, Element v) {
        v.addLore(C.GREEN + Localizer.dLocalize("crafting", "deconstruction", "lore1"));
        v.addLore(C.GREEN + Localizer.dLocalize("crafting", "deconstruction", "lore2"));
    }

    @EventHandler
    public void on(PlayerInteractEvent e) {
        Player player = e.getPlayer();
        ItemStack mainHandItem = player.getInventory().getItemInMainHand();
        if (!hasAdaptation(player)) {
            return;
        }

        if (!player.isSneaking() || mainHandItem.getType() != Material.SHEARS) {
            return;
        }

        // Perform a ray trace for 6 blocks looking for an item
        RayTraceResult rayTrace = player.getWorld().rayTraceEntities(player.getEyeLocation(),
                player.getLocation().getDirection(), 6, entity -> entity instanceof Item);
        if (rayTrace != null && rayTrace.getHitEntity() instanceof Item itemEntity) {
            Deconstruction.processItemInteraction(player, mainHandItem, itemEntity, this);
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

    @NoArgsConstructor
    protected static class Config {
        boolean permanent = false;
        boolean enabled = true;
        int baseCost = 9;
        int initialCost = 8;
        double costFactor = 1.355;
    }
}
