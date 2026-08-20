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

import com.volmit.adapt.api.adaptation.SimpleAdaptation;
import com.volmit.adapt.util.Element;
import com.volmit.adapt.util.Localizer;
import com.volmit.adapt.util.Components;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class TragoulGlobe extends SimpleAdaptation<TragoulGlobe.Config> {
    private final Map<Player, Long> cooldowns;

    public TragoulGlobe() {
        super("tragoul-globe");
        registerConfiguration(TragoulGlobe.Config.class);
        setDescription(Localizer.component("tragoul", "globe", "description"));
        setDisplayName(Localizer.component("tragoul", "globe", "name"));
        setIcon(Material.ENDER_PEARL);
        setInterval(25000);
        setBaseCost(getConfig().baseCost);
        setMaxLevel(getConfig().maxLevel);
        setInitialCost(getConfig().initialCost);
        setCostFactor(getConfig().costFactor);
        cooldowns = new WeakHashMap<>();
    }

    @Override
    public void addStats(int level, Element v) {
        v.addLore(Components.mini("<green><lore>", Placeholder.component("lore",
                Localizer.component("tragoul", "globe", "lore1"))));
        v.addLore(Components.mini("<yellow><lore><value>",
                Placeholder.component("lore", Localizer.component("tragoul", "globe", "lore2")),
                Placeholder.unparsed("value",
                        String.valueOf((getConfig().rangePerLevel * level) + getConfig().initalRange))));
        v.addLore(Components.mini("<yellow><lore><value>",
                Placeholder.component("lore", Localizer.component("tragoul", "globe", "lore3")),
                Placeholder.unparsed("value", String.valueOf(getConfig().bonusDamagePerLevel * level))));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(EntityDamageByEntityEvent e) {
        if (e.isCancelled() || !(e.getDamager() instanceof Player p) || !hasAdaptation(p)) {
            return;
        }

        Long cooldownTime = cooldowns.get(p);
        if (cooldownTime != null && cooldownTime + (1000 * getConfig().cooldown) > System.currentTimeMillis()) {
            return;
        }

        cooldowns.put(p, System.currentTimeMillis());
        double range = (getConfig().rangePerLevel * getLevel(p)) + getConfig().initalRange;

        List<LivingEntity> targets = new ArrayList<>();
        for (Entity entity : p.getNearbyEntities(range, range, range)) {
            if (entity instanceof LivingEntity living && !entity.equals(p)) {
                targets.add(living);
            }
        }

        if (targets.size() <= 1) {
            return;
        }

        double damagePerEntity = e.getDamage() / targets.size() + (getConfig().bonusDamagePerLevel * getLevel(p));
        e.setDamage(damagePerEntity);

        for (LivingEntity target : targets) {
            target.damage(damagePerEntity, p);
        }

        if (getConfig().showParticles) {
            vfxFastSphere(p.getLocation(), range, Color.BLACK, 400);
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

    @NoArgsConstructor
    protected static class Config {
        boolean permanent = false;
        boolean enabled = true;
        boolean showParticles = true;
        int baseCost = 5;
        int maxLevel = 5;
        int initialCost = 5;
        double cooldown = 1;
        double rangePerLevel = 3.0;
        double initalRange = 5.0;
        double costFactor = 1.10;
        double bonusDamagePerLevel = 1;
    }
}
