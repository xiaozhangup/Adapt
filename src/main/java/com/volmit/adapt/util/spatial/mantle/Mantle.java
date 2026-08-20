/*
 * Spatial is a spatial api for Java...
 * Copyright (c) 2021 Arcane Arts
 */
package com.volmit.adapt.util.spatial.mantle;

import com.volmit.adapt.util.spatial.matter.Matter;
import com.volmit.adapt.util.spatial.matter.MatterSlice;
import com.volmit.adapt.util.spatial.parallel.HyperLock;
import com.volmit.adapt.util.spatial.util.CompressedNumbers;
import com.volmit.adapt.util.spatial.util.Consume;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;
import java.util.function.UnaryOperator;

/**
 * Region-backed spatial storage. Disk IO is owned by one serial executor;
 * region locks protect in-memory mutations and lifecycle transitions.
 */
public class Mantle {
    private final File dataFolder;
    private final int minHeight;
    private final int maxHeight;
    private final int worldHeight;
    private final Map<Long, Long> lastUse = new ConcurrentHashMap<>();
    @Getter
    private final Map<Long, MantleRegion> loadedRegions = new ConcurrentHashMap<>();
    private final Map<Long, CompletableFuture<MantleRegion>> loadingRegions = new ConcurrentHashMap<>();
    private final HyperLock hyperLock = new HyperLock();
    private final AtomicBoolean closed = new AtomicBoolean(false);
    private final AtomicBoolean saveQueued = new AtomicBoolean(false);
    private final AtomicBoolean trimQueued = new AtomicBoolean(false);
    private final ReentrantReadWriteLock lifecycleLock = new ReentrantReadWriteLock();
    private final AtomicReference<Thread> ioThread = new AtomicReference<>();
    private final Object submissionGate = new Object();
    private final CompletableFuture<Void> predecessorClose;
    private final ExecutorService ioExecutor;
    private volatile CompletableFuture<Void> closeFuture;

    /** Backwards-compatible zero-based layout used by RawSpace. */
    public Mantle(File dataFolder, int worldHeight) {
        this(dataFolder, 0, worldHeight);
    }

    /** Creates storage for the half-open world Y range [minHeight, maxHeight). */
    public Mantle(File dataFolder, int minHeight, int maxHeight) {
        this(dataFolder, minHeight, maxHeight, CompletableFuture.completedFuture(null));
    }

    /**
     * Creates storage after a previous owner of the same folder has finished its
     * final flush. Waiting happens only on this Mantle's IO lane.
     */
    public Mantle(File dataFolder, int minHeight, int maxHeight, CompletableFuture<Void> predecessorClose) {
        MantleHeight.validate(minHeight, maxHeight);
        this.dataFolder = dataFolder;
        this.minHeight = minHeight;
        this.maxHeight = maxHeight;
        this.worldHeight = maxHeight - minHeight;
        this.predecessorClose = predecessorClose;
        this.ioExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "Adapt-Mantle-" + Integer.toHexString(dataFolder.hashCode()));
            thread.setDaemon(true);
            ioThread.set(thread);
            return thread;
        });
    }

    /** Legacy (zero-based) region filename retained for migration. */
    public static File fileForRegion(File folder, int x, int z) {
        return fileForRegion(folder, key(x, z));
    }

    /** Legacy (zero-based) region filename retained for migration. */
    public static File fileForRegion(File folder, Long key) {
        return new File(folder, "p." + key + ".ttp");
    }

    private static File currentFileForRegion(File folder, long key) {
        return new File(folder, "p2." + key + ".ttp");
    }

    public static Long key(int x, int z) {
        return CompressedNumbers.i2(x, z);
    }

    /** Destructive clear keeps its historical synchronous semantics. */
    public void clear() {
        lifecycleLock.writeLock().lock();
        try {
            ensureOpen();
            loadedRegions.clear();
            lastUse.clear();
            hyperLock.clear();
            File[] files = dataFolder.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!file.delete()) {
                        file.deleteOnExit();
                    }
                }
            }
            if (dataFolder.exists() && !dataFolder.delete()) {
                dataFolder.deleteOnExit();
            }
        } finally {
            lifecycleLock.writeLock().unlock();
        }
    }

    /** Queues a cold region read without blocking the caller. */
    public void preloadChunk(int chunkX, int chunkZ) {
        if (closed.get()) {
            return;
        }
        loadRegionAsync(chunkX >> 5, chunkZ >> 5, false).exceptionally(error -> {
            System.err.println("[Adapt-Mantle] Failed to preload region for chunk " + chunkX + "," + chunkZ);
            error.printStackTrace();
            return null;
        });
    }

    public MantleChunk getChunk(int x, int z) {
        return withRegion(x >> 5, z >> 5, true, region -> region.getOrCreate(x & 31, z & 31));
    }

    public void deleteChunk(int x, int z) {
        withRegion(x >> 5, z >> 5, false, region -> {
            region.delete(x & 31, z & 31);
            return null;
        });
    }

    public boolean hasTectonicPlate(int x, int z) {
        if (closed.get()) {
            return false;
        }
        long regionKey = key(x, z);
        return loadedRegions.containsKey(regionKey)
                || currentFileForRegion(dataFolder, regionKey).exists()
                || fileForRegion(dataFolder, regionKey).exists();
    }

    public <T> void iterateChunk(int x, int z, Class<T> type,
            Consume.Four<Integer, Integer, Integer, T> iterator) {
        withRegion(x >> 5, z >> 5, false, region -> {
            MantleChunk chunk = region.get(x & 31, z & 31);
            if (chunk != null) {
                chunk.iterate(type, (xx, yy, zz, value) -> iterator.accept(xx, yy + minHeight, zz, value));
            }
            return null;
        });
    }

    public <T> void set(int x, int y, int z, T value) {
        int internalY = MantleHeight.toInternalY(y, minHeight, maxHeight);
        if (internalY < 0) {
            return;
        }
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        withRegion(chunkX >> 5, chunkZ >> 5, true, region -> {
            Matter matter = region.getOrCreate(chunkX & 31, chunkZ & 31).getOrCreate(internalY >> 4);
            matter.slice(matter.getClass(value)).set(x & 15, internalY & 15, z & 15, value);
            return null;
        });
    }

    public <T> void remove(int x, int y, int z, Class<T> type) {
        int internalY = MantleHeight.toInternalY(y, minHeight, maxHeight);
        if (internalY < 0) {
            return;
        }
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        withRegion(chunkX >> 5, chunkZ >> 5, false, region -> {
            MantleChunk chunk = region.get(chunkX & 31, chunkZ & 31);
            Matter matter = chunk == null ? null : chunk.get(internalY >> 4);
            MatterSlice<T> slice = matter == null ? null : matter.getSlice(type);
            if (slice != null) {
                slice.set(x & 15, internalY & 15, z & 15, null);
            }
            return null;
        });
    }

    @SuppressWarnings("unchecked")
    public <T> T get(int x, int y, int z, Class<T> type) {
        int internalY = MantleHeight.toInternalY(y, minHeight, maxHeight);
        if (internalY < 0) {
            return null;
        }
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        return withRegion(chunkX >> 5, chunkZ >> 5, false, region -> {
            MantleChunk chunk = region.get(chunkX & 31, chunkZ & 31);
            Matter matter = chunk == null ? null : chunk.get(internalY >> 4);
            MatterSlice<T> slice = matter == null ? null : matter.getSlice(type);
            return slice == null ? null : (T) slice.get(x & 15, internalY & 15, z & 15);
        });
    }

    public <T> CompletableFuture<T> getAsync(int x, int y, int z, Class<T> type) {
        int internalY = MantleHeight.toInternalY(y, minHeight, maxHeight);
        if (internalY < 0) {
            return CompletableFuture.completedFuture(null);
        }
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        return withRegionAsync(chunkX >> 5, chunkZ >> 5, false, region -> {
            MantleChunk chunk = region.get(chunkX & 31, chunkZ & 31);
            Matter matter = chunk == null ? null : chunk.get(internalY >> 4);
            MatterSlice<T> slice = matter == null ? null : matter.getSlice(type);
            return slice == null ? null : slice.get(x & 15, internalY & 15, z & 15);
        });
    }

    public <T> CompletableFuture<Void> setAsync(int x, int y, int z, T value) {
        int internalY = MantleHeight.toInternalY(y, minHeight, maxHeight);
        if (internalY < 0) {
            return CompletableFuture.completedFuture(null);
        }
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        return withRegionAsync(chunkX >> 5, chunkZ >> 5, true, region -> {
            Matter matter = region.getOrCreate(chunkX & 31, chunkZ & 31).getOrCreate(internalY >> 4);
            matter.slice(matter.getClass(value)).set(x & 15, internalY & 15, z & 15, value);
            return null;
        });
    }

    public <T> CompletableFuture<Void> removeAsync(int x, int y, int z, Class<T> type) {
        int internalY = MantleHeight.toInternalY(y, minHeight, maxHeight);
        if (internalY < 0) {
            return CompletableFuture.completedFuture(null);
        }
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        return withRegionAsync(chunkX >> 5, chunkZ >> 5, false, region -> {
            MantleChunk chunk = region.get(chunkX & 31, chunkZ & 31);
            Matter matter = chunk == null ? null : chunk.get(internalY >> 4);
            MatterSlice<T> slice = matter == null ? null : matter.getSlice(type);
            if (slice != null) {
                slice.set(x & 15, internalY & 15, z & 15, null);
            }
            return null;
        });
    }

    /** Atomically updates one value under its region lock on the IO lane. */
    public <T> CompletableFuture<T> updateAsync(int x, int y, int z, Class<T> type, UnaryOperator<T> update) {
        int internalY = MantleHeight.toInternalY(y, minHeight, maxHeight);
        if (internalY < 0) {
            return CompletableFuture.completedFuture(null);
        }
        int chunkX = x >> 4;
        int chunkZ = z >> 4;
        return withRegionAsync(chunkX >> 5, chunkZ >> 5, true, region -> {
            MantleChunk chunk = region.get(chunkX & 31, chunkZ & 31);
            Matter matter = chunk == null ? null : chunk.get(internalY >> 4);
            MatterSlice<T> existingSlice = matter == null ? null : matter.getSlice(type);
            T current = existingSlice == null ? null : existingSlice.get(x & 15, internalY & 15, z & 15);
            T updated = update.apply(current);
            if (updated == null) {
                if (existingSlice != null) {
                    existingSlice.set(x & 15, internalY & 15, z & 15, null);
                }
                return null;
            }

            Matter target = region.getOrCreate(chunkX & 31, chunkZ & 31).getOrCreate(internalY >> 4);
            MatterSlice<T> targetSlice = target.slice(type);
            targetSlice.set(x & 15, internalY & 15, z & 15, updated);
            return updated;
        });
    }

    public boolean isClosed() {
        return closed.get();
    }

    /** Starts a final ordered flush and returns immediately. */
    public void close() {
        closeAsync();
    }

    public synchronized CompletableFuture<Void> closeAsync() {
        synchronized (submissionGate) {
            if (closeFuture != null) {
                return closeFuture;
            }
            closed.set(true);
            // The gate makes this final flush queue strictly after every accepted
            // async mutation and prevents later mutations from entering the lane.
            closeFuture = submitIo(() -> {
                lifecycleLock.writeLock().lock();
                try {
                    saveAllUnlocked();
                    loadedRegions.clear();
                    lastUse.clear();
                    loadingRegions.clear();
                    hyperLock.clear();
                } finally {
                    lifecycleLock.writeLock().unlock();
                }
                return null;
            });
            closeFuture.whenComplete((ignored, error) -> ioExecutor.shutdown());
            return closeFuture;
        }
    }

    /** Used only by final plugin shutdown, after gameplay has stopped. */
    public void closeAndWait() {
        try {
            closeAsync().get(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while flushing Mantle", e);
        } catch (ExecutionException | TimeoutException e) {
            throw new IllegalStateException("Failed to flush Mantle during shutdown", e);
        }
    }

    /** Queues idle-region persistence and unloading on the IO lane. */
    public void trim(long idleDuration) {
        if (closed.get() || !trimQueued.compareAndSet(false, true)) {
            return;
        }
        submitIo(() -> {
            try {
                trimOnIoThread(idleDuration);
            } finally {
                trimQueued.set(false);
            }
            return null;
        }).exceptionally(error -> {
            System.err.println("[Adapt-Mantle] Asynchronous trim failed");
            error.printStackTrace();
            return null;
        });
    }

    private void trimOnIoThread(long idleDuration) {
        if (closed.get()) {
            return;
        }
        long now = System.currentTimeMillis();
        lifecycleLock.readLock().lock();
        try {
            for (Long regionKey : new ArrayList<>(lastUse.keySet())) {
                hyperLock.withLong(regionKey, () -> {
                    Long used = lastUse.get(regionKey);
                    MantleRegion region = loadedRegions.get(regionKey);
                    if (used == null || region == null || now - used < idleDuration) {
                        return;
                    }
                    try {
                        writeRegion(regionKey, region);
                        if (lastUse.get(regionKey) == used) {
                            loadedRegions.remove(regionKey, region);
                            lastUse.remove(regionKey, used);
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            }
        } finally {
            lifecycleLock.readLock().unlock();
        }
        lifecycleLock.writeLock().lock();
        try {
            hyperLock.clear();
        } finally {
            lifecycleLock.writeLock().unlock();
        }
    }

    /** Queues a coalesced snapshot flush on the IO lane. */
    public void saveAll() {
        if (closed.get() || !saveQueued.compareAndSet(false, true)) {
            return;
        }
        submitIo(() -> {
            try {
                if (closed.get()) {
                    return null;
                }
                lifecycleLock.readLock().lock();
                try {
                    saveAllUnlocked();
                } finally {
                    lifecycleLock.readLock().unlock();
                }
                return null;
            } finally {
                saveQueued.set(false);
            }
        }).exceptionally(error -> {
            System.err.println("[Adapt-Mantle] Asynchronous save failed");
            error.printStackTrace();
            return null;
        });
    }

    /** Runs only on the Mantle IO lane, with at least a lifecycle read lock. */
    private void saveAllUnlocked() {
        RuntimeException failure = null;
        for (Map.Entry<Long, MantleRegion> entry : new ArrayList<>(loadedRegions.entrySet())) {
            try {
                hyperLock.withLong(entry.getKey(), () -> {
                    MantleRegion current = loadedRegions.get(entry.getKey());
                    if (current == null) {
                        return;
                    }
                    try {
                        writeRegion(entry.getKey(), current);
                    } catch (IOException e) {
                        throw new UncheckedIOException(e);
                    }
                });
            } catch (UncheckedIOException e) {
                if (failure == null) {
                    failure = new IllegalStateException("One or more Mantle regions could not be saved", e);
                } else {
                    failure.addSuppressed(e);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    /**
     * Cold reads retain the synchronous API, but the disk access itself executes
     * on the IO lane. Chunk-load prefetch normally resolves it before gameplay.
     */
    private <T> T withRegion(int regionX, int regionZ, boolean create, Function<MantleRegion, T> operation) {
        ensureOpen();
        long regionKey = key(regionX, regionZ);
        while (true) {
            MantleRegion region = loadedRegions.get(regionKey);
            if (region == null) {
                try {
                    region = loadRegionAsync(regionX, regionZ, create).join();
                } catch (CompletionException e) {
                    throw new IllegalStateException("Failed to load Mantle region " + regionX + "," + regionZ,
                            e.getCause());
                }
                if (region == null) {
                    if (create) {
                        loadingRegions.remove(regionKey);
                        continue;
                    }
                    return null;
                }
            }

            lifecycleLock.readLock().lock();
            try {
                ensureOpen();
                RegionResult<T> result = hyperLock.withResult(regionX, regionZ, () -> {
                    MantleRegion current = loadedRegions.get(regionKey);
                    if (current == null) {
                        return new RegionResult<>(false, null);
                    }
                    lastUse.put(regionKey, System.currentTimeMillis());
                    return new RegionResult<>(true, operation.apply(current));
                });
                if (result.applied()) {
                    return result.value();
                }
            } finally {
                lifecycleLock.readLock().unlock();
            }
        }
    }

    private <T> CompletableFuture<T> withRegionAsync(int regionX, int regionZ, boolean create,
            Function<MantleRegion, T> operation) {
        return submitOperation(() -> withRegionOnIoThread(regionX, regionZ, create, operation));
    }

    /** One accepted mutation is one FIFO IO-lane job, including a possible cold load. */
    private <T> T withRegionOnIoThread(int regionX, int regionZ, boolean create,
            Function<MantleRegion, T> operation) {
        long regionKey = key(regionX, regionZ);
        lifecycleLock.readLock().lock();
        try {
            return hyperLock.withResult(regionX, regionZ, () -> {
                MantleRegion region = loadedRegions.get(regionKey);
                if (region == null) {
                    region = loadRegionOnIoThread(regionX, regionZ, create);
                }
                if (region == null) {
                    return null;
                }
                lastUse.put(regionKey, System.currentTimeMillis());
                return operation.apply(region);
            });
        } finally {
            lifecycleLock.readLock().unlock();
        }
    }

    private CompletableFuture<MantleRegion> loadRegionAsync(int regionX, int regionZ, boolean create) {
        synchronized (submissionGate) {
            long regionKey = key(regionX, regionZ);
            MantleRegion present = loadedRegions.get(regionKey);
            if (present != null) {
                return CompletableFuture.completedFuture(present);
            }
            if (closed.get()) {
                return CompletableFuture.failedFuture(new IllegalStateException("The Mantle is closed"));
            }

            CompletableFuture<MantleRegion> existing = loadingRegions.get(regionKey);
            if (existing != null) {
                return existing;
            }
            CompletableFuture<MantleRegion> result = new CompletableFuture<>();
            CompletableFuture<MantleRegion> raced = loadingRegions.putIfAbsent(regionKey, result);
            if (raced != null) {
                return raced;
            }

            CompletableFuture<MantleRegion> load = submitIo(() -> {
                lifecycleLock.readLock().lock();
                try {
                    return hyperLock.withResult(regionX, regionZ,
                            () -> loadRegionOnIoThread(regionX, regionZ, create));
                } finally {
                    lifecycleLock.readLock().unlock();
                }
            });
            load.whenComplete((loaded, error) -> {
                loadingRegions.remove(regionKey, result);
                if (error == null) {
                    result.complete(loaded);
                } else {
                    result.completeExceptionally(error);
                }
            });
            return result;
        }
    }

    /** Called on the IO lane while the matching HyperLock is held. */
    private MantleRegion loadRegionOnIoThread(int regionX, int regionZ, boolean create) {
        long regionKey = key(regionX, regionZ);
        MantleRegion loaded = loadedRegions.get(regionKey);
        if (loaded != null) {
            return loaded;
        }

        File current = currentFileForRegion(dataFolder, regionKey);
        File legacy = fileForRegion(dataFolder, regionKey);
        try {
            MantleRegion region;
            if (current.exists()) {
                MantleRegion.StoredRegion stored = MantleRegion.readVersioned(current);
                int sectionOffset = (stored.minHeight() - minHeight) >> 4;
                if (stored.minHeight() == minHeight && stored.maxHeight() == maxHeight) {
                    region = stored.region();
                } else {
                    region = stored.region().shiftedCopy(worldHeight, sectionOffset);
                    writeRegion(regionKey, region);
                }
            } else if (legacy.exists()) {
                int legacyHeight = Math.max(16, maxHeight);
                MantleRegion old = MantleRegion.read(legacyHeight, legacy);
                region = old.shiftedCopy(worldHeight, MantleHeight.legacySectionOffset(minHeight));
                writeRegion(regionKey, region);
            } else if (create) {
                region = new MantleRegion(worldHeight, regionX, regionZ);
            } else {
                return null;
            }
            lastUse.put(regionKey, System.currentTimeMillis());
            loadedRegions.put(regionKey, region);
            return region;
        } catch (Throwable e) {
            throw new IllegalStateException("Failed to load Mantle region " + regionX + "," + regionZ
                    + "; the on-disk file was left untouched", e);
        }
    }

    private void writeRegion(long regionKey, MantleRegion region) throws IOException {
        if (Thread.currentThread() != ioThread.get()) {
            throw new IllegalStateException("Mantle disk IO escaped its owner thread");
        }
        Files.createDirectories(dataFolder.toPath());
        File target = currentFileForRegion(dataFolder, regionKey);
        File temporary = new File(dataFolder, target.getName() + ".tmp");
        region.writeVersioned(temporary, minHeight, maxHeight);
        try {
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException ignored) {
            Files.move(temporary.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private <T> CompletableFuture<T> submitIo(IoCallable<T> work) {
        CompletableFuture<T> future = new CompletableFuture<>();
        try {
            ioExecutor.execute(() -> {
                try {
                    predecessorClose.join();
                    future.complete(work.call());
                } catch (Throwable e) {
                    future.completeExceptionally(e);
                }
            });
        } catch (RejectedExecutionException e) {
            future.completeExceptionally(e);
        }
        return future;
    }

    private <T> CompletableFuture<T> submitOperation(IoCallable<T> work) {
        synchronized (submissionGate) {
            if (closed.get()) {
                return CompletableFuture.failedFuture(new IllegalStateException("The Mantle is closed"));
            }
            return submitIo(work);
        }
    }

    private void ensureOpen() {
        if (closed.get()) {
            throw new IllegalStateException("The Mantle is closed");
        }
    }

    public int getWorldHeight() {
        return worldHeight;
    }

    public int getMinHeight() {
        return minHeight;
    }

    public int getMaxHeight() {
        return maxHeight;
    }

    public void deleteChunkSlice(int x, int z, Class<?> type) {
        withRegion(x >> 5, z >> 5, false, region -> {
            MantleChunk chunk = region.get(x & 31, z & 31);
            if (chunk != null) {
                chunk.deleteSlices(type);
            }
            return null;
        });
    }

    public int getLoadedRegionCount() {
        return loadedRegions.size();
    }

    public <T> void set(int x, int y, int z, MatterSlice<T> slice) {
        if (slice.isEmpty()) {
            return;
        }
        slice.iterateSync((xx, yy, zz, value) -> set(x + xx, y + yy, z + zz, value));
    }

    public boolean isChunkLoaded(int x, int z) {
        return loadedRegions.containsKey(key(x >> 5, z >> 5));
    }

    private record RegionResult<T>(boolean applied, T value) {
    }

    @FunctionalInterface
    private interface IoCallable<T> {
        T call() throws Exception;
    }
}
