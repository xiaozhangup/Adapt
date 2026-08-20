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
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Tameable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.EntitiesLoadEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TamingHealthBoost extends SimpleAdaptation<TamingHealthBoost.Config> {
    private static final UUID MODIFIER = UUID.nameUUIDFromBytes("adapt-tame-health-boost".getBytes());
    private static final NamespacedKey MODIFIER_KEY = NamespacedKey.fromString("adapt:tame-health-boost");
    private final Map<UUID, Integer> ownerLevels = new HashMap<>();

    public TamingHealthBoost() {
        super("tame-health");
        registerConfiguration(Config.class);
        setDescription(Localizer.component("taming", "health", "description"));
        setDisplayName(Localizer.component("taming", "health", "name"));
        setIcon(Material.COOKED_BEEF);
        setBaseCost(getConfig().baseCost);
        setMaxLevel(getConfig().maxLevel);
        setInitialCost(getConfig().initialCost);
        setInterval(4753);
        setCostFactor(getConfig().costFactor);
    }

    @Override
    public void addStats(int level, Element v) {
        v.addLore(Components.mini("<green>+ <amount><gray> <lore>",
                Placeholder.unparsed("amount", Form.pc(getHealthBoost(level), 0)),
                Placeholder.component("lore", Localizer.component("taming", "health", "lore1"))));
    }

    private double getHealthBoost(int level) {
        return ((getLevelPercent(level) * getConfig().healthBoostFactor) + getConfig().healthBoostBase);
    }

    @Override
    public boolean needsTicking() {
        return true;
    }

    @Override
    public void onTick() {
        Map<UUID, Integer> changedOwners = new HashMap<>();
        for (Player player : Adapt.instance.getAdaptServer().getAdaptPlayers()) {
            int level = getLevel(player);
            Integer previous = ownerLevels.put(player.getUniqueId(), level);
            if (previous == null || previous != level) {
                changedOwners.put(player.getUniqueId(), level);
            }
        }
        if (!changedOwners.isEmpty()) {
            refreshOwners(changedOwners);
        }
    }

    @EventHandler
    public void on(EntityTameEvent event) {
        if (event.getEntity() instanceof Tameable tameable) {
            removeModifier(tameable);
            J.s(() -> updateForOnlineOwner(tameable));
        }
    }

    @EventHandler
    public void on(EntitiesLoadEvent event) {
        for (Entity entity : event.getEntities()) {
            if (entity instanceof Tameable tameable) {
                removeModifier(tameable);
                updateForOnlineOwner(tameable);
            }
        }
    }

    @EventHandler
    public void on(PlayerQuitEvent event) {
        ownerLevels.remove(event.getPlayer().getUniqueId());
    }

    private void refreshOwners(Map<UUID, Integer> changedOwners) {
        for (World world : Bukkit.getWorlds()) {
            for (Tameable tameable : world.getEntitiesByClass(Tameable.class)) {
                if (!tameable.isTamed() || tameable.getOwner() == null) {
                    continue;
                }
                Integer level = changedOwners.get(tameable.getOwner().getUniqueId());
                if (level != null) {
                    update(tameable, level);
                }
            }
        }
    }

    private void updateForOnlineOwner(Tameable tameable) {
        if (!tameable.isTamed() || tameable.getOwner() == null) {
            return;
        }
        Player owner = Bukkit.getPlayer(tameable.getOwner().getUniqueId());
        if (owner == null || !owner.clientConnected()
                || !Adapt.instance.getAdaptServer().isPlayerLoaded(owner.getUniqueId())) {
            return;
        }
        int level = getLevel(owner);
        ownerLevels.put(owner.getUniqueId(), level);
        update(tameable, level);
    }

    private void update(Tameable j, int level) {
        var attribute = Version.get().getAttribute(j, Attribute.MAX_HEALTH);
        if (attribute == null)
            return;

        var modifiers = attribute.getModifier(MODIFIER, MODIFIER_KEY);
        if (level <= 0) {
            if (!modifiers.isEmpty()) {
                attribute.removeModifier(MODIFIER, MODIFIER_KEY);
            }
            return;
        }

        double amount = getHealthBoost(level);
        if (modifiers.size() == 1 && Math.abs(modifiers.getFirst().getAmount() - amount) < 1.0E-9
                && modifiers.getFirst().getOperation() == AttributeModifier.Operation.ADD_SCALAR) {
            return;
        }
        attribute.removeModifier(MODIFIER, MODIFIER_KEY);
        attribute.addModifier(MODIFIER, MODIFIER_KEY, amount, AttributeModifier.Operation.ADD_SCALAR);
    }

    private void removeModifier(Tameable tameable) {
        var attribute = Version.get().getAttribute(tameable, Attribute.MAX_HEALTH);
        if (attribute != null) {
            attribute.removeModifier(MODIFIER, MODIFIER_KEY);
        }
    }

    @Override
    public void unregister() {
        for (World world : Bukkit.getWorlds()) {
            for (Tameable tameable : world.getEntitiesByClass(Tameable.class)) {
                removeModifier(tameable);
            }
        }
        ownerLevels.clear();
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
        int baseCost = 6;
        int maxLevel = 5;
        int initialCost = 3;
        double costFactor = 0.4;
        double healthBoostFactor = 2.5;
        double healthBoostBase = 0.57;
    }
}
