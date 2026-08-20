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

package com.volmit.adapt.content.adaptation.brewing;

import com.volmit.adapt.Adapt;
import com.volmit.adapt.AdaptConfig;
import com.volmit.adapt.api.adaptation.SimpleAdaptation;
import com.volmit.adapt.api.world.PlayerAdaptation;
import com.volmit.adapt.api.world.PlayerData;
import com.volmit.adapt.api.world.PlayerSkillLine;
import com.volmit.adapt.util.J;
import com.volmit.adapt.util.M;
import com.volmit.adapt.util.SQLManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

final class BrewingOwnerLevelCache {
    private static final long TTL_MS = 60_000;
    private static final long FAILED_RETRY_MS = 5_000;
    private final Map<UUID, CachedLevel> levels = new HashMap<>();
    private final Map<UUID, CompletableFuture<Integer>> loading = new HashMap<>();
    private final Map<UUID, Long> retryAfter = new HashMap<>();
    private long nextCleanup;

    synchronized CompletableFuture<Integer> get(UUID owner, SimpleAdaptation<?> adaptation, long now) {
        cleanup(now);

        Player online = Bukkit.getPlayer(owner);
        if (online != null && online.clientConnected()
                && Adapt.instance.getAdaptServer().isPlayerLoaded(owner)) {
            int level = adaptation.getLevel(online);
            levels.put(owner, new CachedLevel(level, now + TTL_MS, now));
            retryAfter.remove(owner);
            return CompletableFuture.completedFuture(level);
        }

        CachedLevel cached = levels.get(owner);
        if (cached != null) {
            if (cached.expiresAt() <= now) {
                request(owner, adaptation, now);
            }
            return CompletableFuture.completedFuture(cached.level());
        }

        return request(owner, adaptation, now);
    }

    private CompletableFuture<Integer> request(UUID owner, SimpleAdaptation<?> adaptation, long requestedAt) {
        if (retryAfter.getOrDefault(owner, 0L) > requestedAt) {
            return CompletableFuture.completedFuture(null);
        }
        CompletableFuture<Integer> current = loading.get(owner);
        if (current != null) {
            return current;
        }

        String skillName = adaptation.getSkill().getName();
        String adaptationName = adaptation.getName();
        File localFile = new File(Adapt.instance.getDataFolder("data", "players"), owner + ".json");
        CompletableFuture<Integer> future = load(owner, localFile, skillName, adaptationName).exceptionally(error -> {
            if (AdaptConfig.get().isVerbose()) {
                Adapt.instance.getLogger().fine("Failed to load brewing owner " + owner);
            }
            return null;
        });
        loading.put(owner, future);
        future.whenComplete((level, error) -> complete(owner, requestedAt, future, level));
        return future;
    }

    private CompletableFuture<Integer> load(UUID owner, File localFile, String skillName, String adaptationName) {
        if (!AdaptConfig.get().isUseSql()) {
            return loadLocal(localFile, skillName, adaptationName);
        }

        Optional<String> cached = Adapt.instance.getSqlManager().getCachedData(owner);
        if (cached.isPresent()) {
            try {
                return CompletableFuture.completedFuture(readLevel(PlayerData.fromJson(cached.get()), skillName,
                        adaptationName));
            } catch (Throwable error) {
                return CompletableFuture.failedFuture(error);
            }
        }

        return Adapt.instance.getSqlManager().fetchDataAsync(owner).thenCompose(result -> {
            if (result.status() == SQLManager.FetchStatus.FOUND && result.data() != null) {
                try {
                    return CompletableFuture.completedFuture(readLevel(PlayerData.fromJson(result.data()), skillName,
                            adaptationName));
                } catch (Throwable error) {
                    return CompletableFuture.failedFuture(error);
                }
            }
            if (result.status() == SQLManager.FetchStatus.NOT_FOUND) {
                return loadLocal(localFile, skillName, adaptationName);
            }
            Throwable error = result.error() != null
                    ? result.error()
                    : new IllegalStateException("SQL owner lookup is " + result.status());
            return CompletableFuture.failedFuture(error);
        });
    }

    private CompletableFuture<Integer> loadLocal(File file, String skillName, String adaptationName) {
        CompletableFuture<Integer> future = new CompletableFuture<>();
        try {
            J.a(() -> {
                try {
                    PlayerData data = file.exists()
                            ? PlayerData.fromJson(com.volmit.adapt.util.IO.readAll(file))
                            : null;
                    future.complete(readLevel(data, skillName, adaptationName));
                } catch (Throwable error) {
                    future.completeExceptionally(error);
                }
            });
        } catch (Throwable error) {
            future.completeExceptionally(error);
        }
        return future;
    }

    private static int readLevel(PlayerData data, String skillName, String adaptationName) {
        if (data == null) {
            return 0;
        }
        PlayerSkillLine line = data.getSkillLineNullable(skillName);
        PlayerAdaptation adaptation = line != null ? line.getAdaptation(adaptationName) : null;
        return adaptation == null ? 0 : adaptation.getLevel();
    }

    private synchronized void complete(UUID owner, long requestedAt, CompletableFuture<Integer> future,
            Integer level) {
        loading.remove(owner, future);
        long loadedAt = M.ms();
        if (level == null) {
            retryAfter.put(owner, loadedAt + FAILED_RETRY_MS);
            return;
        }
        retryAfter.remove(owner);
        levels.compute(owner, (uuid, current) -> current != null && current.loadedAt() >= requestedAt
                ? current
                : new CachedLevel(level, loadedAt + TTL_MS, loadedAt));
    }

    private void cleanup(long now) {
        if (now < nextCleanup) {
            return;
        }
        nextCleanup = now + TTL_MS;
        levels.entrySet().removeIf(entry -> entry.getValue().expiresAt() + TTL_MS < now
                && !loading.containsKey(entry.getKey()));
        retryAfter.entrySet().removeIf(entry -> entry.getValue() <= now);
    }

    private record CachedLevel(int level, long expiresAt, long loadedAt) {
    }
}
