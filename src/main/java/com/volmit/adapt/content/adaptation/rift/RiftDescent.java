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

package com.volmit.adapt.content.adaptation.rift;

import com.volmit.adapt.api.adaptation.SimpleAdaptation;
import com.volmit.adapt.util.*;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class RiftDescent extends SimpleAdaptation<RiftDescent.Config> {
    private final Map<UUID, Long> cooldown = new HashMap<>();

    public RiftDescent() {
        super("rift-descent");
        registerConfiguration(Config.class);
        setDescription(Localizer.component("rift", "descent", "description"));
        setDisplayName(Localizer.component("rift", "descent", "name"));
        setMaxLevel(1);
        setIcon(Material.SHULKER_BOX);
        setBaseCost(getConfig().baseCost);
        setCostFactor(getConfig().costFactor);
        setInitialCost(getConfig().initialCost);
        setInterval(9544);
    }

    @Override
    public void addStats(int level, Element v) {
        v.addLore(Localizer.components("rift", "descent", "lore",
                Placeholder.unparsed("cooldown", String.valueOf(getConfig().cooldown))));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(PlayerToggleSneakEvent e) {
        Player p = e.getPlayer();
        SoundPlayer sp = SoundPlayer.of(p);
        if (p.getPotionEffect(PotionEffectType.LEVITATION) == null) {
            return;
        }
        if (!hasAdaptation(p)) {
            return;
        }
        UUID playerId = p.getUniqueId();
        Long cooldownUntil = cooldown.get(playerId);
        if (cooldownUntil != null && cooldownUntil > M.ms()) {
            return;
        }
        cooldown.remove(playerId);

        PotionEffect levi = p.getPotionEffect(PotionEffectType.LEVITATION);

        if (!e.isSneaking() && (levi != null)) {
            p.removePotionEffect(PotionEffectType.LEVITATION);
            int cooldownTicks = Math.max(1, (int) Math.round(getConfig().cooldown * 20));
            long expiresAt = M.ms() + cooldownTicks * 50L;
            cooldown.put(playerId, expiresAt);
            J.s(() -> {
                if (!cooldown.remove(playerId, expiresAt)) {
                    return;
                }
                Player online = Bukkit.getPlayer(playerId);
                if (online != null && online.clientConnected()) {
                    SoundPlayer.of(online).play(online.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                }
            }, cooldownTicks);

            p.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, (int) cooldownTicks, 0));
            sp.play(p.getLocation(), Sound.ENTITY_ENDER_DRAGON_FLAP, 1f, 1f);
        }
    }

    @EventHandler
    public void on(PlayerQuitEvent event) {
        cooldown.remove(event.getPlayer().getUniqueId());
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
        double cooldown = 5.0;
        int baseCost = 1;
        double costFactor = 2;
        int initialCost = 3;
    }

}
