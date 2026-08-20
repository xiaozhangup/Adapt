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

package com.volmit.adapt.content.adaptation.tragoul;

import com.volmit.adapt.Adapt;
import com.volmit.adapt.api.adaptation.SimpleAdaptation;
import com.volmit.adapt.api.version.Version;
import com.volmit.adapt.util.Element;
import com.volmit.adapt.util.Localizer;
import com.volmit.adapt.util.Components;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TragoulHealing extends SimpleAdaptation<TragoulHealing.Config> {
    private final Map<UUID, Long> cooldowns;
    private final Map<UUID, Long> healingWindows;
    private long nextWindowToken;

    public TragoulHealing() {
        super("tragoul-healing");
        registerConfiguration(TragoulHealing.Config.class);
        setDescription(Localizer.component("tragoul", "healing", "description"));
        setDisplayName(Localizer.component("tragoul", "healing", "name"));
        setIcon(Material.REDSTONE);
        setInterval(25000);
        setBaseCost(getConfig().baseCost);
        setMaxLevel(getConfig().maxLevel);
        setInitialCost(getConfig().initialCost);
        setCostFactor(getConfig().costFactor);
        cooldowns = new HashMap<>();
        healingWindows = new HashMap<>();
    }

    @Override
    public void addStats(int level, Element v) {
        v.addLore(Components.mini("<green><lore>", Placeholder.component("lore",
                Localizer.component("tragoul", "healing", "lore1"))));
        v.addLore(Components.mini("<yellow><lore>", Placeholder.component("lore",
                Localizer.component("tragoul", "healing", "lore2"))));
        double percent = getConfig().minHealPercent
                + (getConfig().maxHealPercent - getConfig().minHealPercent) * (level - 1)
                        / (getConfig().maxLevel - 1);
        v.addLore(Components.mini("<yellow><lore><percent>%",
                Placeholder.component("lore", Localizer.component("tragoul", "healing", "lore3")),
                Placeholder.unparsed("percent", String.valueOf(percent))));
    }

    @EventHandler
    public void on(EntityDamageByEntityEvent e) {
        if (e.getDamager() instanceof Player p && hasAdaptation(p)) {
            if (isOnCooldown(p)) {
                return;
            }

            if (!healingWindows.containsKey(p.getUniqueId())) {
                Adapt.verbose("Starting healing window for " + p.getName());
                startHealingWindow(p);
            }

            if (getConfig().showParticles) {
                vfxParticleLine(p.getLocation(), e.getEntity().getLocation(), 25, Particle.WHITE_ASH);
            }

            double healPercentage = getConfig().minHealPercent
                    + (getConfig().maxHealPercent - getConfig().minHealPercent) * (getLevel(p) - 1)
                            / (getConfig().maxLevel - 1);
            double healAmount = e.getDamage() * healPercentage;
            Adapt.verbose("Healing " + p.getName() + " for " + healAmount + " (" + healPercentage * 100 + "% of "
                    + e.getDamage() + " damage)");
            var attribute = Version.get().getAttribute(p, Attribute.MAX_HEALTH);
            p.setHealth(Math.min(attribute == null ? p.getHealth() : attribute.getValue(), p.getHealth() + healAmount));

        }
    }

    @EventHandler
    public void on(PlayerQuitEvent e) {
        UUID playerId = e.getPlayer().getUniqueId();
        healingWindows.remove(playerId);
        cooldowns.remove(playerId);
    }

    private boolean isOnCooldown(Player p) {
        Long cooldown = cooldowns.get(p.getUniqueId());
        return cooldown != null && cooldown > System.currentTimeMillis();
    }

    private void startHealingWindow(Player p) {
        long currentTime = System.currentTimeMillis();
        UUID playerId = p.getUniqueId();
        long token = ++nextWindowToken;
        healingWindows.put(playerId, token);
        Bukkit.getScheduler().runTaskLater(Adapt.instance, () -> {
            if (healingWindows.remove(playerId, token)) {
                cooldowns.put(playerId,
                        currentTime + getConfig().windowDuration + getConfig().cooldownDuration);
            }
        }, getConfig().windowDuration / 50);
    }

    @Override
    public void unregister() {
        healingWindows.clear();
        cooldowns.clear();
        super.unregister();
    }

    @Override
    public boolean isEnabled() {
        return getConfig().enabled;
    }

    @Override
    public void onTick() {
    }

    @Override
    public boolean isPermanent() {
        return getConfig().permanent;
    }

    @NoArgsConstructor
    protected static class Config {
        boolean permanent = false;
        boolean enabled = true;
        boolean showParticles = true;
        int baseCost = 5;
        int maxLevel = 5;
        int initialCost = 5;
        double costFactor = 1.10;
        double minHealPercent = 0.10; // 0.10%
        double maxHealPercent = 0.45; // 0.45%
        int cooldownDuration = 1000; // 1 second
        int windowDuration = 3000; // 3 seconds
    }
}
