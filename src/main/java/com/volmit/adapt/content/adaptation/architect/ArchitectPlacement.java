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

package com.volmit.adapt.content.adaptation.architect;




import com.volmit.adapt.api.adaptation.SimpleAdaptation;
import com.volmit.adapt.function.PlacementWand;
import com.volmit.adapt.util.Element;
import com.volmit.adapt.util.J;
import com.volmit.adapt.util.Localizer;
import lombok.NoArgsConstructor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ArchitectPlacement extends SimpleAdaptation<ArchitectPlacement.Config> {
    private final PlacementWand placementWand;
    private final int renderingTask;

    public ArchitectPlacement() {
        super("architect-placement");
        registerConfiguration(ArchitectPlacement.Config.class);
        setDescription(Localizer.component("architect", "placement", "description"));
        setDisplayName(Localizer.component("architect", "placement", "name"));
        setIcon(Material.SCAFFOLDING);
        setInterval(360);
        setBaseCost(getConfig().baseCost);
        setMaxLevel(getConfig().maxLevel);
        setInitialCost(getConfig().initialCost);
        setCostFactor(getConfig().costFactor);

        placementWand = new PlacementWand(this, getConfig().maxBlocks);
        renderingTask = J.sr(() -> {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (!hasAdaptation(player)) {
                    continue;
                }

                placementWand.renderBlockEntity(player);
            }
        }, 5);
    }

    @Override
    public void addStats(int level, Element v) {
        v.addLore(Localizer.components("architect", "placement", "lore"));
    }

    @EventHandler
    public void on(PlayerQuitEvent e) {
        Player p = e.getPlayer();
        placementWand.clearPlayerEntities(p);
    }

    @EventHandler
    public void on(PlayerInteractEvent e) {
        placementWand.onPlayerInteract(e);
    }

    @Override
    public boolean isEnabled() {
        return getConfig().enabled;
    }

    @Override
    public boolean isPermanent() {
        return getConfig().permanent;
    }

    @Override
    public void onTick() {
    }

    @Override
    public void unregister() {
        Bukkit.getScheduler().cancelTask(renderingTask);
        Set<UUID> players = new HashSet<>(placementWand.trackedPlayerIds());
        Bukkit.getOnlinePlayers().forEach(player -> players.add(player.getUniqueId()));
        players.forEach(placementWand::clearPlayerEntities);
        super.unregister();
    }

    @NoArgsConstructor
    protected static class Config {
        public int maxBlocks = 20;
        boolean permanent = false;
        boolean enabled = true;
        int baseCost = 6;
        int maxLevel = 1;
        int initialCost = 4;
        double costFactor = 2;
    }
}
