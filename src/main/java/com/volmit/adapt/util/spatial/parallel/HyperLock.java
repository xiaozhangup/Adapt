/*
 * Spatial is a spatial api for Java...
 * Copyright (c) 2021 Arcane Arts
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.volmit.adapt.util.spatial.parallel;

import com.volmit.adapt.util.spatial.util.CompressedNumbers;
import com.volmit.adapt.util.spatial.util.Run;

import java.io.IOException;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class HyperLock {
    private final ConcurrentHashMap<Long, ReentrantLock> locks;
    private volatile boolean enabled = true;
    private final boolean fair;

    public HyperLock() {
        this(1024, false);
    }

    public HyperLock(int capacity) {
        this(capacity, false);
    }

    public HyperLock(int capacity, boolean fair) {
        this.fair = fair;
        locks = new ConcurrentHashMap<>(capacity);
    }

    public void clear() {
        for (Long i : new HashSet<>(locks.keySet())) {
            ReentrantLock lock = locks.get(i);
            if (lock == null || lock.isLocked() || lock.hasQueuedThreads()) {
                continue;
            }

            locks.remove(i, lock);
        }
    }

    public void with(int x, int z, Runnable r) {
        ReentrantLock lock = enabled ? getLock(x, z) : null;
        if (lock != null) {
            lock.lock();
        }
        try {
            r.run();
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    public void withLong(long k, Runnable r) {
        with(CompressedNumbers.i2a(k), CompressedNumbers.i2b(k), r);
    }

    public void withNasty(int x, int z, Run.Throwable r) throws Throwable {
        ReentrantLock lock = enabled ? getLock(x, z) : null;
        if (lock != null) {
            lock.lock();
        }
        try {
            r.run();
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    public void withIO(int x, int z, Run.IO r) throws IOException {
        ReentrantLock lock = enabled ? getLock(x, z) : null;
        if (lock != null) {
            lock.lock();
        }
        try {
            r.run();
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    public <T> T withResult(int x, int z, Supplier<T> r) {
        ReentrantLock lock = enabled ? getLock(x, z) : null;
        if (lock != null) {
            lock.lock();
        }
        try {
            return r.get();
        } finally {
            if (lock != null) {
                lock.unlock();
            }
        }
    }

    public boolean tryLock(int x, int z) {
        if (!enabled) {
            return true;
        }
        return getLock(x, z).tryLock();
    }

    public boolean tryLock(int x, int z, long timeout) {
        if (!enabled) {
            return true;
        }
        try {
            return getLock(x, z).tryLock(timeout, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        return false;
    }

    private ReentrantLock getLock(int x, int z) {
        return locks.computeIfAbsent(CompressedNumbers.i2(x, z), k -> new ReentrantLock(fair));
    }

    public void lock(int x, int z) {
        if (!enabled) {
            return;
        }

        getLock(x, z).lock();
    }

    public void unlock(int x, int z) {
        ReentrantLock lock = locks.get(CompressedNumbers.i2(x, z));
        if (lock != null && lock.isHeldByCurrentThread()) {
            lock.unlock();
        }
    }

    public void disable() {
        enabled = false;
    }
}
