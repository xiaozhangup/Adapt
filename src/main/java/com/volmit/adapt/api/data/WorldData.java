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

package com.volmit.adapt.api.data;

import com.volmit.adapt.api.data.unit.Earnings;
import com.volmit.adapt.api.tick.TickedObject;
import com.volmit.adapt.util.collection.KMap;
import com.volmit.adapt.util.spatial.mantle.Mantle;
import com.volmit.adapt.util.spatial.matter.SpatialMatter;
import lombok.Getter;
import org.bukkit.World;
import org.bukkit.event.EventHandler;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.WorldSaveEvent;
import org.bukkit.event.world.WorldUnloadEvent;

import java.io.File;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class WorldData extends TickedObject {
    private static final KMap<World, WorldData> mantles = new KMap<>();
    private static final Set<CompletableFuture<Void>> closingMantles = ConcurrentHashMap.newKeySet();
    private static final ConcurrentHashMap<Path, CompletableFuture<Void>> closingFolders = new ConcurrentHashMap<>();
    private static final ConcurrentLinkedQueue<Throwable> closingFailures = new ConcurrentLinkedQueue<>();

    static {
        SpatialMatter.registerSliceType(new Earnings.EarningsMatter());
    }

    private final World world;
    private final String worldName;
    private final Path mantleFolder;
    @Getter
    private final Mantle mantle;

    public WorldData(World world) {
        super("world-data", world.getUID().toString(), 30_000);
        this.world = world;
        this.worldName = world.getName();
        mantleFolder = new File(world.getWorldFolder(), "adapt/mantle").toPath().toAbsolutePath().normalize();
        CompletableFuture<Void> predecessor = closingFolders.getOrDefault(mantleFolder,
                CompletableFuture.completedFuture(null));
        mantle = new Mantle(mantleFolder.toFile(), world.getMinHeight(), world.getMaxHeight(), predecessor);
        for (org.bukkit.Chunk chunk : world.getLoadedChunks()) {
            mantle.preloadChunk(chunk.getX(), chunk.getZ());
        }
    }

    public static void stop() {
        RuntimeException failure = null;
        for (WorldData worldData : mantles.v()) {
            try {
                worldData.unregister(true);
            } catch (RuntimeException e) {
                if (failure == null) {
                    failure = e;
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        CompletableFuture<Void> pending = CompletableFuture.allOf(closingMantles.toArray(CompletableFuture[]::new));
        try {
            pending.get(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            if (failure == null) {
                failure = new IllegalStateException("Failed to finish pending Mantle unloads", e);
            } else {
                failure.addSuppressed(e);
            }
        } catch (Throwable e) {
            if (failure == null) {
                failure = new IllegalStateException("Failed to finish pending Mantle unloads", e);
            } else {
                failure.addSuppressed(e);
            }
        } finally {
            mantles.clear();
        }
        for (Throwable closeFailure; (closeFailure = closingFailures.poll()) != null;) {
            if (failure == null) {
                failure = new IllegalStateException("One or more Mantle unloads failed", closeFailure);
            } else {
                failure.addSuppressed(closeFailure);
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    public static WorldData of(World world) {
        return mantles.computeIfAbsent(world, WorldData::new);
    }

    public CompletableFuture<Double> reportEarningsAsync(int x, int y, int z) {
        AtomicReference<Earnings> previous = new AtomicReference<>();
        return mantle.updateAsync(x, y, z, Earnings.class, earnings -> {
            Earnings current = earnings == null ? new Earnings(0) : earnings;
            previous.set(current);
            return current.increment();
        }).thenApply(ignored -> {
            Earnings earnings = previous.get();
            return 1D / (double) (earnings == null || earnings.getEarnings() == 0 ? 1 : earnings.getEarnings());
        });
    }

    public void unregister() {
        unregister(false);
    }

    private void unregister(boolean waitForFlush) {
        super.unregister();
        try {
            if (waitForFlush) {
                mantle.closeAndWait();
            } else {
                // Publish the folder fence before closeAsync so a concurrent reload
                // cannot open a second writer in the small hand-off window.
                CompletableFuture<Void> folderClosing = new CompletableFuture<>();
                closingFolders.put(mantleFolder, folderClosing);
                closingMantles.add(folderClosing);
                CompletableFuture<Void> closing;
                try {
                    closing = mantle.closeAsync();
                } catch (RuntimeException | Error error) {
                    folderClosing.completeExceptionally(error);
                    throw error;
                }
                closing.whenComplete((ignored, error) -> {
                    if (error == null) {
                        folderClosing.complete(null);
                    } else {
                        folderClosing.completeExceptionally(error);
                    }
                });
                folderClosing.whenComplete((ignored, error) -> {
                    closingMantles.remove(folderClosing);
                    closingFolders.remove(mantleFolder, folderClosing);
                    if (error != null) {
                        closingFailures.add(error);
                        System.err.println("[Adapt-Mantle] Failed to flush unloaded world " + worldName);
                        error.printStackTrace();
                    }
                });
            }
        } finally {
            mantles.remove(world, this);
        }
    }

    @EventHandler
    public void on(ChunkLoadEvent e) {
        if (e.getWorld() != world) {
            return;
        }
        mantle.preloadChunk(e.getChunk().getX(), e.getChunk().getZ());
    }

    @EventHandler
    public void on(WorldSaveEvent e) {
        if (e.getWorld() != world)
            return;
        mantle.saveAll();
    }

    @EventHandler
    public void on(WorldUnloadEvent e) {
        if (e.getWorld() != world)
            return;
        unregister();
    }

    @Override
    public void onTick() {
        mantle.trim(60_000);
    }
}
