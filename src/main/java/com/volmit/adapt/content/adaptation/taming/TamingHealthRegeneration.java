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

package com.volmit.adapt.content.adaptation.taming;

import com.volmit.adapt.Adapt;
import com.volmit.adapt.api.adaptation.SimpleAdaptation;
import com.volmit.adapt.api.version.Version;
import com.volmit.adapt.util.*;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.potion.PotionEffectType;

import static org.bukkit.Particle.HEART;

public class TamingHealthRegeneration extends SimpleAdaptation<TamingHealthRegeneration.Config> {
    private static final long DAMAGE_COOLDOWN_MS = 8_000;
    private final NamespacedKey lastDamageKey;

    public TamingHealthRegeneration() {
        super("tame-health-regeneration");
        registerConfiguration(Config.class);
        setDescription(Localizer.component("taming", "regeneration", "description"));
        setDisplayName(Localizer.component("taming", "regeneration", "name"));
        setIcon(Material.GOLDEN_APPLE);
        setBaseCost(getConfig().baseCost);
        setMaxLevel(getConfig().maxLevel);
        setInitialCost(getConfig().initialCost);
        setCostFactor(getConfig().costFactor);
        lastDamageKey = new NamespacedKey(Adapt.instance, "tame-regeneration-last-damage");
    }

    @Override
    public void addStats(int level, Element v) {
        v.addLore(Components.mini("<green>+ <amount><gray> <lore>",
                Placeholder.unparsed("amount", Form.f(getRegenSpeed(level), 0)),
                Placeholder.component("lore", Localizer.component("taming", "regeneration", "lore1"))));
    }

    @EventHandler
    public void on(EntityDamageByEntityEvent e) {
        if (e.isCancelled()) {
            return;
        }
        if (e.getEntity() instanceof Tameable tam && tam.getOwner() instanceof Player p && hasAdaptation(p)) {
            Long damagedAt = tam.getPersistentDataContainer().get(lastDamageKey, PersistentDataType.LONG);
            if (damagedAt != null && M.ms() - damagedAt <= DAMAGE_COOLDOWN_MS) {
                Adapt.verbose("Tamed Entity " + tam.getUniqueId() + " last damaged "
                        + (M.ms() - damagedAt) + "ms ago");
                return;
            }
            var attribute = Version.get().getAttribute(tam, Attribute.MAX_HEALTH);
            double mh = attribute == null ? tam.getHealth() : attribute.getValue();
            if (tam.isTamed() && tam.getOwner() instanceof Player && tam.getHealth() < mh) {
                Adapt.verbose("Successfully healed tamed entity " + tam.getUniqueId());
                int level = getLevel(p);
                if (level > 0) {
                    Adapt.verbose("[PRE] Current Health: " + tam.getHealth() + " Max Health: " + mh);
                    tam.addPotionEffect(PotionEffectType.REGENERATION.createEffect(25 * level, 3));

                    if (getConfig().showParticles) {
                        Adapt.verbose("Healing tamed entity " + tam.getUniqueId() + " with particles");
                        tam.getWorld().spawnParticle(HEART, tam.getLocation().add(0, 1, 0), 2 * p.getLevel());
                    } else {
                        Adapt.verbose("Healing tamed entity " + tam.getUniqueId() + " without particles");
                    }
                }
            }
            tam.getPersistentDataContainer().set(lastDamageKey, PersistentDataType.LONG, M.ms());
        }
    }

    private double getRegenSpeed(int level) {
        return ((getLevelPercent(level) * (getLevelPercent(level)) * getConfig().regenFactor) + getConfig().regenBase);
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
        boolean showParticles = true;
        int baseCost = 7;
        int maxLevel = 3;
        int initialCost = 8;
        double costFactor = 0.4;
        double regenFactor = 5;
        double regenBase = 1;
    }
}
