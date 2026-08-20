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

package com.volmit.adapt.content.adaptation.stealth;

import com.volmit.adapt.Adapt;
import com.volmit.adapt.api.adaptation.SimpleAdaptation;
import com.volmit.adapt.api.version.Modifier;
import com.volmit.adapt.api.version.Version;
import com.volmit.adapt.util.*;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class StealthGhostArmor extends SimpleAdaptation<StealthGhostArmor.Config> {
    private static final UUID MODIFIER = UUID.nameUUIDFromBytes("adapt-ghost-armor".getBytes());
    private static final NamespacedKey MODIFIER_KEY = NamespacedKey.fromString("adapt:ghost-armor");

    public StealthGhostArmor() {
        super("stealth-ghost-armor");
        registerConfiguration(Config.class);
        setDescription(Localizer.component("stealth", "ghostarmor", "description"));
        setDisplayName(Localizer.component("stealth", "ghostarmor", "name"));
        setIcon(Material.NETHERITE_CHESTPLATE);
        setInterval(5353);
        setBaseCost(getConfig().baseCost);
        setInitialCost(getConfig().initialCost);
        setCostFactor(getConfig().costFactor);
        setMaxLevel(getConfig().maxLevel);
    }

    @Override
    public void addStats(int level, Element v) {
        v.addLore(stat(Form.f(getMaxArmorPoints(getLevelPercent(level)), 0), "lore1"));
        v.addLore(stat(Form.f(getMaxArmorPerTick(getLevelPercent(level)), 1), "lore2"));
    }

    private static net.kyori.adventure.text.Component stat(String amount, String key) {
        return Components.mini("<green>+ <amount><gray> <lore>",
                Placeholder.unparsed("amount", amount),
                Placeholder.component("lore", Localizer.component("stealth", "ghostarmor", key)));
    }

    public double getMaxArmorPoints(double factor) {
        return M.lerp(getConfig().minArmor, getConfig().maxArmor, factor);
    }

    public double getMaxArmorPerTick(double factor) {
        return M.lerp(getConfig().minArmorPerTick, getConfig().maxArmorPerTick, factor);
    }

    @Override
    public boolean needsTicking() {
        return true;
    }

    @Override
    public void onTick() {
        for (Player p : Adapt.instance.getAdaptServer().getAdaptPlayers()) {
                if (!p.clientConnected()) {
                    continue;
                }

                var attribute = Version.get().getAttribute(p, Attribute.ARMOR);

                if (!hasAdaptation(p)) {
                    attribute.removeModifier(MODIFIER, MODIFIER_KEY);
                    continue;
                }
                double oldArmor = attribute.getModifier(MODIFIER, MODIFIER_KEY).stream().mapToDouble(Modifier::getAmount)
                        .filter(d -> !Double.isNaN(d)).max().orElse(0);
                double armor = getMaxArmorPoints(getLevelPercent(p));
                armor = Double.isNaN(armor) ? 0 : armor;

                if (oldArmor < armor) {
                    attribute.setModifier(MODIFIER, MODIFIER_KEY,
                            Math.min(armor, oldArmor + getMaxArmorPerTick(getLevelPercent(p))),
                            AttributeModifier.Operation.ADD_NUMBER);
                } else if (oldArmor > armor) {
                    attribute.setModifier(MODIFIER, MODIFIER_KEY, armor, AttributeModifier.Operation.ADD_NUMBER);
                }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(EntityDamageEvent e) {
        if (e.isCancelled()) {
            return;
        }
        if (e.getEntity() instanceof Player p && hasAdaptation(p) && !e.isCancelled() && e.getDamage() > 0) {
            // Check if 2.5 * e.getDamage() is greater than 10 if so just set it to 10
            // otherwise use the value of 2.5 * e.getDamage()
            int damageXP = (int) Math.min(10, 2.5 * e.getDamage());
            xp(p, damageXP);
            J.s(() -> {
                if (!p.clientConnected()) {
                    return;
                }

                var attribute = Version.get().getAttribute(p, Attribute.ARMOR);
                if (attribute == null)
                    return;
                attribute.removeModifier(MODIFIER, MODIFIER_KEY);
            });
        }
    }

    @EventHandler
    public void on(PlayerQuitEvent event) {
        removeModifier(event.getPlayer());
    }

    private void removeModifier(Player player) {
        var attribute = Version.get().getAttribute(player, Attribute.ARMOR);
        if (attribute != null) {
            attribute.removeModifier(MODIFIER, MODIFIER_KEY);
        }
    }

    @Override
    public void unregister() {
        Adapt.instance.getAdaptServer().getAdaptPlayers().forEach(this::removeModifier);
        super.unregister();
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
        int baseCost = 3;
        int maxArmor = 16;
        int minArmor = 2;
        int maxArmorPerTick = 3;
        int minArmorPerTick = 1;
        int initialCost = 1;
        double costFactor = 0.335;
        int maxLevel = 7;
    }
}
