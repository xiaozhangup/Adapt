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

import com.volmit.adapt.Adapt;
import com.volmit.adapt.api.adaptation.SimpleAdaptation;
import com.volmit.adapt.api.world.AdaptPlayer;
import com.volmit.adapt.api.world.PlayerAdaptation;
import com.volmit.adapt.api.world.PlayerSkillLine;
import com.volmit.adapt.content.event.AdaptAdaptationTeleportEvent;
import com.volmit.adapt.util.*;
import lombok.NoArgsConstructor;
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.*;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.player.PlayerToggleFlightEvent;
import org.bukkit.util.Vector;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.volmit.adapt.api.adaptation.chunk.ChunkLoading.loadChunkAsync;

public class RiftBlink extends SimpleAdaptation<RiftBlink.Config> {
    private final Map<UUID, Long> lastJump = new HashMap<>();
    private final Map<UUID, BlinkWindow> blinkWindows = new HashMap<>();
    private long nextWindowGeneration;

    private final double jumpVelocity = -0.0784000015258789;

    public RiftBlink() {
        super("rift-blink");
        registerConfiguration(Config.class);
        setDescription(Localizer.component("rift", "blink", "description"));
        setDisplayName(Localizer.component("rift", "blink", "name"));
        setIcon(Material.FEATHER);
        setBaseCost(getConfig().baseCost);
        setCostFactor(getConfig().costFactor);
        setMaxLevel(getConfig().maxLevel);
        setInitialCost(getConfig().initialCost);
        setInterval(9288);
    }

    private double getBlinkDistance(int level) {
        return getConfig().baseDistance + (getLevelPercent(level) * getConfig().distanceFactor);
    }

    private int getCooldownDuration() {
        return 2000;
    }

    @Override
    public void addStats(int level, Element v) {
        v.addLore(Localizer.components("rift", "blink", "lore",
                Placeholder.unparsed("distance", String.valueOf(getBlinkDistance(level)))));
    }

    @EventHandler
    public void on(PlayerQuitEvent e) {
        UUID playerId = e.getPlayer().getUniqueId();
        lastJump.remove(playerId);
        restoreTemporaryFlight(e.getPlayer(), blinkWindows.remove(playerId));
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void on(PlayerToggleFlightEvent e) {
        Player p = e.getPlayer();
        UUID playerId = p.getUniqueId();
        if (!Adapt.instance.getAdaptServer().isPlayerLoaded(playerId)) {
            return;
        }
        if (hasAdaptation(p) && p.getGameMode().equals(GameMode.SURVIVAL) && !p.isFlying()) {
            e.setCancelled(true);
            restoreTemporaryFlight(p, blinkWindows.remove(playerId));
            if (lastJump.get(playerId) != null && M.ms() - lastJump.get(playerId) <= getCooldownDuration()) {
                return;
            }
            if (p.isSprinting()) {
                Location loc = p.getLocation().clone();
                Location locOG = p.getLocation().clone();
                Vector dir = loc.getDirection();
                double dist = getBlinkDistance(getLevel(p));
                dir.multiply(dist);
                loc.add(dir);
                double cd = dist * 2;
                loc.subtract(0, dist, 0);
                while (!isSafe(loc) && cd-- > 0) {
                    loc.add(0, 1, 0);
                }
                SoundPlayer spw = SoundPlayer.of(p.getWorld());
                if (!isSafe(loc)) {
                    spw.play(p.getLocation(), Sound.BLOCK_CONDUIT_DEACTIVATE, 1f, 1.24f);
                    lastJump.put(playerId, M.ms());
                    return;
                }
                AdaptPlayer expectedPlayer = getPlayer(p);
                PlayerSkillLine line = expectedPlayer.getData().getSkillLineNullable("rift");
                PlayerAdaptation adaptation = line != null ? line.getAdaptation("rift-resist") : null;
                if (adaptation != null && adaptation.getLevel() > 0) {
                    RiftResist.riftResistStackAdd(p, 10, 5);
                }
                if (getConfig().showParticles) {

                    vfxParticleLine(locOG, loc, Particle.REVERSE_PORTAL, 50, 8, 0.1D, 1D, 0.1D, 0D, null, false,
                            l -> l.getBlock().isPassable());
                }
                Vector v = p.getVelocity().clone();
                Location target = loc.clone();
                Location origin = locOG.clone();
                UUID targetWorldId = target.getWorld().getUID();
                loadChunkAsync(target, chunk -> {
                    Player online = Bukkit.getPlayer(playerId);
                    World targetWorld = Bukkit.getWorld(targetWorldId);
                    if (online == null || targetWorld == null
                            || !Adapt.instance.getAdaptServer().isPlayerLoaded(playerId)
                            || !Adapt.instance.getAdaptServer().isCurrentPlayer(playerId, expectedPlayer)) {
                        return;
                    }
                    Location eventTarget = target.clone();
                    eventTarget.setWorld(targetWorld);
                    Location toLoc = eventTarget.clone().add(0, 1, 0);

                    AdaptAdaptationTeleportEvent event = new AdaptAdaptationTeleportEvent(!Bukkit.isPrimaryThread(),
                            expectedPlayer, this, origin, eventTarget);
                    Bukkit.getPluginManager().callEvent(event);
                    if (event.isCancelled()) {
                        return;
                    }

                    online.teleport(toLoc, PlayerTeleportEvent.TeleportCause.PLUGIN);
                    J.s(() -> {
                        Player current = Bukkit.getPlayer(playerId);
                        if (current != null && Adapt.instance.getAdaptServer().isCurrentPlayer(playerId,
                                expectedPlayer)) {
                            current.setVelocity(v.clone().multiply(3));
                        }
                    }, 2);
                });
                lastJump.put(playerId, M.ms());
                spw.play(p.getLocation(), Sound.ENTITY_ENDERMAN_TELEPORT, 0.50f, 1.0f);
                vfxLevelUp(p);
            }
        }
    }

    @EventHandler
    public void on(PlayerMoveEvent e) {
        if (e.getTo() == null || (e.getFrom().getX() == e.getTo().getX()
                && e.getFrom().getY() == e.getTo().getY() && e.getFrom().getZ() == e.getTo().getZ())) {
            return;
        }
        Player p = e.getPlayer();
        UUID playerId = p.getUniqueId();
        if (!Adapt.instance.getAdaptServer().isPlayerLoaded(playerId)) {
            return;
        }
        boolean isJumping = p.getVelocity().getY() > jumpVelocity;
        boolean canFlight = p.getAllowFlight();
        if (isJumping && !blinkWindows.containsKey(playerId) && hasAdaptation(p)
                && p.getGameMode().equals(GameMode.SURVIVAL)
                && p.isSprinting() && !p.isFlying()) {
            if (lastJump.get(playerId) != null && M.ms() - lastJump.get(playerId) <= getCooldownDuration()) {
                if (!canFlight)
                    p.setAllowFlight(false);
                return;
            }
            Location loc = p.getLocation().clone();
            Vector dir = loc.getDirection();
            double dist = getBlinkDistance(getLevel(p));
            dir.multiply(dist);
            loc.add(dir);
            double cd = dist * 2;
            loc.subtract(0, dist, 0);

            while (!isSafe(loc) && cd-- > 0) {
                loc.add(0, 1, 0);
            }

            if (isSafe(loc)) {
                BlinkWindow window = new BlinkWindow(++nextWindowGeneration, canFlight);
                blinkWindows.put(playerId, window);
                p.setAllowFlight(true);
                Adapt.verbose("Allowing flight for " + p.getName());
                J.s(() -> {
                    if (!blinkWindows.remove(playerId, window)) {
                        return;
                    }
                    Player online = Bukkit.getPlayer(playerId);
                    if (online != null && Adapt.instance.getAdaptServer().isPlayerLoaded(playerId)) {
                        if (!window.hadFlight()) {
                            online.setAllowFlight(false);
                        }
                        online.setFlying(false);
                        Adapt.verbose("Disabling flight for " + online.getName());
                    }
                }, 25);
            }
        }
    }

    private boolean isSafe(Location l) {
        return l.getBlock().getType().isSolid() && !l.getBlock().getRelative(BlockFace.UP).getType().isSolid()
                && !l.getBlock().getRelative(BlockFace.UP).getRelative(BlockFace.UP).getType().isSolid();
    }

    private void restoreTemporaryFlight(Player player, BlinkWindow window) {
        if (window != null && !window.hadFlight()) {
            player.setFlying(false);
            player.setAllowFlight(false);
        }
    }

    @Override
    public void unregister() {
        blinkWindows.forEach((playerId, window) -> {
            Player player = Bukkit.getPlayer(playerId);
            if (player != null) {
                restoreTemporaryFlight(player, window);
            }
        });
        blinkWindows.clear();
        lastJump.clear();
        super.unregister();
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
        double costFactor = 0.12;
        int maxLevel = 5;
        int initialCost = 1;
        double baseDistance = 6;
        double distanceFactor = 5;
    }

    private record BlinkWindow(long generation, boolean hadFlight) {
    }
}
