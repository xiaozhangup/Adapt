package com.volmit.adapt.content.adaptation.tragoul;/*------------------------------------------------------------------------------
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

import com.volmit.adapt.Adapt;
import com.volmit.adapt.api.adaptation.SimpleAdaptation;
import com.volmit.adapt.api.world.AdaptPlayer;
import com.volmit.adapt.util.Element;
import com.volmit.adapt.util.Localizer;
import com.volmit.adapt.util.Components;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

public class TragoulLance extends SimpleAdaptation<TragoulLance.Config> {
    private final Map<Player, Long> cooldowns;

    public TragoulLance() {
        super("tragoul-lance");
        registerConfiguration(TragoulLance.Config.class);
        setDescription(Localizer.component("tragoul", "lance", "description"));
        setDisplayName(Localizer.component("tragoul", "lance", "name"));
        setIcon(Material.TRIDENT);
        setBaseCost(getConfig().baseCost);
        setMaxLevel(getConfig().maxLevel);
        setInitialCost(getConfig().initialCost);
        setCostFactor(getConfig().costFactor);
        cooldowns = new WeakHashMap<>();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onEntityDeath(EntityDeathEvent event) {
        if (event.getEntity().getLastDamageCause() instanceof EntityDamageByEntityEvent e) {
            if (e.getDamager() instanceof Player p && hasAdaptation(p)) {
                Long cooldown = cooldowns.get(p);
                if (cooldown != null && cooldown + 5000 > System.currentTimeMillis())
                    return;

                cooldowns.put(p, System.currentTimeMillis());
                int level = getLevel(p);
                double baseSeekerRange = 5 + 4 * level;
                double damageDealt = e.getDamage();
                double seekerDamage = getConfig().seekerDamageMultiplier * damageDealt;

                triggerSeeker(p, getPlayer(p), event.getEntity(), seekerDamage, level, baseSeekerRange);
            }
        }
    }

    private void triggerSeeker(Player p, AdaptPlayer expectedPlayer, Entity origin, double damage,
            int remainingSeekers, double range) {
        if (remainingSeekers <= 0) {
            return;
        }

        LivingEntity nearest = null;
        double minDistance = range;

        for (Entity e : origin.getNearbyEntities(range, range, range)) {
            if (e instanceof LivingEntity le && le != p) {
                if (le.getLastDamageCause() instanceof EntityDamageByEntityEvent eve) {
                    if (eve.getDamager() != p) continue;
                    double distance = origin.getLocation().distance(le.getLocation());
                    if (distance < minDistance) {
                        nearest = le;
                        minDistance = distance;
                    }
                }
            }
        }

        if (nearest != null) {
            vfxMovingSphere(origin.getLocation(), nearest.getLocation(), getConfig().seekerDelay, Color.MAROON, 0.25,
                    4);
            double seekerDamage = getConfig().seekerDamageMultiplier * damage;
            double selfDamage = getConfig().selfDamageMultiplier * seekerDamage;
            Adapt.verbose("Seeker damage: " + seekerDamage + " Self damage: " + selfDamage);

            p.damage(selfDamage, p);

            LivingEntity finalNearest = nearest;
            UUID playerId = p.getUniqueId();
            Bukkit.getScheduler().runTaskLater(Adapt.instance, () -> {
                Player online = Bukkit.getPlayer(playerId);
                if (online == null || !online.isOnline() || !finalNearest.isValid() || finalNearest.isDead()
                        || !Adapt.instance.getAdaptServer().isPlayerLoaded(playerId)
                        || !Adapt.instance.getAdaptServer().isCurrentPlayer(playerId, expectedPlayer)) {
                    return;
                }
                double remainingHealth = finalNearest.getHealth() - damage;
                finalNearest.damage(damage, online);
                if (remainingHealth <= 0) {
                    triggerSeeker(online, expectedPlayer, finalNearest, damage * 0.5, remainingSeekers - 1, range);
                }
            }, getConfig().seekerDelay);
        }
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

    @Override
    public void addStats(int level, Element v) {
        v.addLore(Components.mini("<green><lore>", Placeholder.component("lore",
                Localizer.component("tragoul", "lance", "lore1"))));
        v.addLore(Components.mini("<yellow><lore>", Placeholder.component("lore",
                Localizer.component("tragoul", "lance", "lore2"))));
        v.addLore(Components.mini("<yellow><lore><level>",
                Placeholder.component("lore", Localizer.component("tragoul", "lance", "lore3")),
                Placeholder.unparsed("level", Integer.toString(level))));
    }

    @NoArgsConstructor
    protected static class Config {
        boolean permanent = false;
        boolean enabled = true;
        int baseCost = 5;
        int maxLevel = 5;
        int initialCost = 5;
        int seekerDelay = 20;
        double costFactor = 1.10;
        double seekerDamageMultiplier = 0.5;
        double selfDamageMultiplier = 0.5;
    }
}
