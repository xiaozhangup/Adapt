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

import com.volmit.adapt.api.adaptation.SimpleAdaptation;
import com.volmit.adapt.util.*;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerToggleSneakEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

public class StealthSpeed extends SimpleAdaptation<StealthSpeed.Config> {
    private static final int EFFECT_DURATION = PotionEffect.INFINITE_DURATION;
    private final Map<UUID, AppliedEffect> sneaking;

    public StealthSpeed() {
        super("stealth-speed");
        registerConfiguration(Config.class);
        setDescription(Localizer.component("stealth", "speed", "description"));
        setDisplayName(Localizer.component("stealth", "speed", "name"));
        setIcon(Material.MUSHROOM_STEW);
        setBaseCost(getConfig().baseCost);
        setInterval(2000);
        setInitialCost(getConfig().initialCost);
        setCostFactor(getConfig().costFactor);
        sneaking = new HashMap<>();

    }

    @Override
    public void addStats(int level, Element v) {
        v.addLore(Localizer.components("stealth", "speed", "lore",
                Placeholder.unparsed("amount", Form.pc(getSpeed(getLevelPercent(level)), 0))));
    }

    @EventHandler
    public void on(PlayerToggleSneakEvent e) {
        if (e.isCancelled()) {
            return;
        }
        Player p = e.getPlayer();
        SoundPlayer sp = SoundPlayer.of(p);
        double factor = getLevelPercent(p);
        if (!hasAdaptation(p)) {
            return;
        }

        if (factor == 0) {
            return;
        }

        if (e.isSneaking()) {
            sp.play(p.getLocation(), Sound.BLOCK_FUNGUS_BREAK, 1, 0.99f);
            applyEffect(p, getLevel(p));
        } else {
            restoreEffect(p, sneaking.remove(p.getUniqueId()));
        }

    }

    private double getSpeed(double factor) {
        return factor * getConfig().factor;
    }

    @Override
    public boolean needsTicking() {
        return true;
    }

    @Override
    public void onTick() {
        Iterator<Map.Entry<UUID, AppliedEffect>> iterator = sneaking.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, AppliedEffect> entry = iterator.next();
            Player player = org.bukkit.Bukkit.getPlayer(entry.getKey());
            if (player == null || !player.isOnline()) {
                iterator.remove();
                continue;
            }
            if (!hasAdaptation(player) || !player.isSneaking()) {
                iterator.remove();
                restoreEffect(player, entry.getValue());
                continue;
            }
            PotionEffect current = player.getPotionEffect(PotionEffectType.SPEED);
            if (!entry.getValue().matches(current)) {
                iterator.remove();
            } else {
                entry.getValue().lastDuration = current.getDuration();
            }
        }
    }

    @EventHandler
    public void on(PlayerQuitEvent event) {
        restoreEffect(event.getPlayer(), sneaking.remove(event.getPlayer().getUniqueId()));
    }

    @Override
    public void unregister() {
        for (Map.Entry<UUID, AppliedEffect> entry : sneaking.entrySet()) {
            Player player = org.bukkit.Bukkit.getPlayer(entry.getKey());
            if (player != null) {
                restoreEffect(player, entry.getValue());
            }
        }
        sneaking.clear();
        super.unregister();
    }

    private void applyEffect(Player player, int amplifier) {
        PotionEffect original = player.getPotionEffect(PotionEffectType.SPEED);
        PotionEffect applied = new PotionEffect(PotionEffectType.SPEED, EFFECT_DURATION, amplifier,
                false, false, true);
        if (player.addPotionEffect(applied)) {
            PotionEffect current = player.getPotionEffect(PotionEffectType.SPEED);
            if (current != null && current.getAmplifier() == amplifier && !current.isAmbient()
                    && !current.hasParticles() && current.hasIcon()) {
                sneaking.put(player.getUniqueId(), new AppliedEffect(original, amplifier, current.getDuration(), M.ms()));
            }
        }
    }

    private void restoreEffect(Player player, AppliedEffect applied) {
        if (applied == null || !applied.matches(player.getPotionEffect(PotionEffectType.SPEED))) {
            return;
        }
        player.removePotionEffect(PotionEffectType.SPEED);
        PotionEffect original = applied.original;
        if (original == null) {
            return;
        }
        int remaining = original.isInfinite()
                ? PotionEffect.INFINITE_DURATION
                : original.getDuration() - (int) ((M.ms() - applied.appliedAt) / 50L);
        if (remaining > 0) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, remaining, original.getAmplifier(),
                    original.isAmbient(), original.hasParticles(), original.hasIcon()));
        }
    }

    private static final class AppliedEffect {
        private final PotionEffect original;
        private final int amplifier;
        private final long appliedAt;
        private int lastDuration;

        private AppliedEffect(PotionEffect original, int amplifier, int lastDuration, long appliedAt) {
            this.original = original;
            this.amplifier = amplifier;
            this.lastDuration = lastDuration;
            this.appliedAt = appliedAt;
        }

        private boolean matches(PotionEffect current) {
            return current != null && current.getAmplifier() == amplifier && !current.isAmbient()
                    && !current.hasParticles() && current.hasIcon()
                    && (current.isInfinite() && lastDuration == PotionEffect.INFINITE_DURATION
                            || !current.isInfinite() && current.getDuration() > 0
                                    && current.getDuration() <= lastDuration);
        }
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
        int baseCost = 2;
        int initialCost = 5;
        double costFactor = 0.6;
        double factor = 1.25;
    }
}
