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

package com.volmit.adapt.content.adaptation.herbalism;

import com.volmit.adapt.api.adaptation.SimpleAdaptation;
import com.volmit.adapt.util.C;
import com.volmit.adapt.util.Element;
import com.volmit.adapt.util.Localizer;
import lombok.NoArgsConstructor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityChangeBlockEvent;

public class HerbalismNoTrample extends SimpleAdaptation<HerbalismNoTrample.Config> {

    public HerbalismNoTrample() {
        super("herbalism-no-trample");
        registerConfiguration(Config.class);
        setDescription(Localizer.dLocalize("herbalism", "notrample", "description"));
        setDisplayName(Localizer.dLocalize("herbalism", "notrample", "name"));
        setIcon(Material.FARMLAND);
        setBaseCost(getConfig().baseCost);
        setMaxLevel(getConfig().maxLevel);
        setInterval(3000);
        setInitialCost(getConfig().initialCost);
        setCostFactor(getConfig().costFactor);
    }

    @Override
    public void addStats(int level, Element v) {
        v.addLore(C.GREEN + "+ " + C.GRAY + Localizer.dLocalize("herbalism", "notrample", "lore1"));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(EntityChangeBlockEvent e) {
        if (e.isCancelled()) {
            return;
        }
        if (!(e.getEntity() instanceof Player p)) {
            return;
        }
        if (e.getBlock().getType() != Material.FARMLAND) {
            return;
        }
        if (e.getTo() != Material.DIRT) {
            return;
        }
        if (!hasAdaptation(p)) {
            return;
        }
        e.setCancelled(true);
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
        boolean permanent = true;
        boolean enabled = true;
        int baseCost = 2;
        int maxLevel = 1;
        int initialCost = 3;
        double costFactor = 1;
    }
}

