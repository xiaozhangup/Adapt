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

package com.volmit.adapt.util;

import com.volmit.adapt.Adapt;
import lombok.Getter;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * MultiBurst - A high-performance thread pool manager for concurrent task execution
 *
 * Features:
 * - Dynamic thread pool with intelligent sizing
 * - Overflow protection with caller-runs policy
 * - Rate limiting for warning messages
 * - Support for both regular and virtual threads
 * - Sliding window performance monitoring
 */
public class MultiBurst {
    // Public static instances for global use
    public static final MultiBurst burst = new MultiBurst();
    public static final MultiBurst virtualBurst = new MultiBurst(Executors.newVirtualThreadPerTaskExecutor());

    // Configuration constants
    private static final int DEFAULT_CORE_POOL_SIZE = 2;
    private static final int DEFAULT_KEEP_ALIVE_SECONDS = 10;
    private static final int DEFAULT_QUEUE_CAPACITY = 512;
    private static final long WARNING_THROTTLE_MS = 20_000; // 20 seconds between warnings
    private static final long PERFORMANCE_WINDOW_MS = 1000; // 1 second window for QPS tracking

    @Getter
    private final ExecutorService service;
    private final AtomicInteger threadIdGenerator = new AtomicInteger(0);
    private final AtomicLong lastWarningTime = new AtomicLong(0);
    private final PerformanceMonitor performanceMonitor = new PerformanceMonitor(PERFORMANCE_WINDOW_MS);

    /**
     * Creates a MultiBurst with default dynamic thread pool configuration
     */
    public MultiBurst() {
        this.service = createDefaultThreadPool();
    }

    /**
     * Creates a MultiBurst with a custom ExecutorService
     *
     * @param service the ExecutorService to use
     */
    public MultiBurst(ExecutorService service) {
        this.service = service;
    }

    /**
     * Execute multiple tasks concurrently and wait for completion
     *
     * @param tasks the tasks to execute
     */
    public void burst(Runnable... tasks) {
        burst(tasks.length).queue(tasks).complete();
    }

    /**
     * Execute multiple tasks synchronously in the current thread
     *
     * @param tasks the tasks to execute
     */
    public void sync(Runnable... tasks) {
        for (Runnable task : tasks) {
            task.run();
        }
    }

    /**
     * Create a BurstExecutor for batch task execution
     *
     * @param estimatedTasks estimated number of tasks (for optimization)
     * @return a new BurstExecutor instance
     */
    public BurstExecutor burst(int estimatedTasks) {
        return new BurstExecutor(service, estimatedTasks);
    }

    /**
     * Create a BurstExecutor with default estimation
     *
     * @return a new BurstExecutor instance
     */
    public BurstExecutor burst() {
        return burst(16);
    }

    /**
     * Execute a task asynchronously without waiting for completion
     *
     * @param task the task to execute
     */
    public void lazy(Runnable task) {
        service.execute(task);
    }

    /**
     * Creates the default thread pool with optimized configuration
     */
    private ThreadPoolExecutor createDefaultThreadPool() {
        int maxPoolSize = calculateOptimalMaxPoolSize();

        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                DEFAULT_CORE_POOL_SIZE,
                maxPoolSize,
                DEFAULT_KEEP_ALIVE_SECONDS,
                TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(DEFAULT_QUEUE_CAPACITY),
                this::createWorkerThread,
                this::handleRejectedExecution
        );

        // Keep core threads alive to ensure immediate task processing
        // This guarantees that there are always DEFAULT_CORE_POOL_SIZE threads
        // ready to handle incoming tasks without creation delay
        executor.allowCoreThreadTimeOut(false);

        return executor;
    }

    /**
     * Calculates optimal maximum pool size based on system capabilities
     */
    private int calculateOptimalMaxPoolSize() {
        int availableProcessors = Runtime.getRuntime().availableProcessors();
        // Reserve some processors for system tasks, minimum of 8 threads
        return Math.max(8, availableProcessors - 4);
    }

    /**
     * Creates worker threads with proper naming and exception handling
     */
    private Thread createWorkerThread(Runnable runnable) {
        Thread thread = Executors.defaultThreadFactory().newThread(runnable);
        thread.setName("Adapt-MultiBurst-Worker-" + threadIdGenerator.incrementAndGet());
        thread.setUncaughtExceptionHandler(this::handleUncaughtException);
        return thread;
    }

    /**
     * Handles uncaught exceptions in worker threads
     */
    private void handleUncaughtException(Thread thread, Throwable exception) {
        Adapt.info("Uncaught exception in thread " + thread.getName());
        exception.printStackTrace();
    }

    /**
     * Handles rejected task execution with throttled warnings
     */
    private void handleRejectedExecution(Runnable task, ThreadPoolExecutor executor) {
        long currentTime = System.currentTimeMillis();

        // Throttle warning messages to prevent spam
        if (shouldLogWarning(currentTime)) {
            double overloadRate = performanceMonitor.recordAndGetRate();
            Adapt.warn(String.format(
                    "MultiBurst thread pool is overloaded! Running task in caller thread. " +
                            "(%.1f overloaded tasks/second)",
                    overloadRate
            ));
        } else {
            performanceMonitor.recordOverload();
        }

        // Execute in caller thread if executor is still running
        if (!executor.isShutdown()) {
            task.run();
        }
    }

    /**
     * Determines if a warning should be logged based on throttling rules
     */
    private boolean shouldLogWarning(long currentTime) {
        long lastWarning = lastWarningTime.get();
        if (currentTime - lastWarning > WARNING_THROTTLE_MS) {
            return lastWarningTime.compareAndSet(lastWarning, currentTime);
        }
        return false;
    }

    /**
     * High-performance sliding window counter for monitoring task overload rates
     */
    private static class PerformanceMonitor {
        private final long windowSizeMs;
        private final long[] timestamps;
        private final int[] counts;
        private final int bucketCount;
        private int writeIndex = 0;
        private int totalCount = 0;

        public PerformanceMonitor(long windowSizeMs) {
            this.windowSizeMs = windowSizeMs;
            this.bucketCount = 20; // 20 buckets for smooth averaging
            this.timestamps = new long[bucketCount];
            this.counts = new int[bucketCount];
        }

        /**
         * Records an overload event and returns the current rate
         */
        public synchronized double recordAndGetRate() {
            recordOverload();
            return getCurrentRate();
        }

        /**
         * Records an overload event
         */
        public synchronized void recordOverload() {
            long now = System.currentTimeMillis();
            long bucketTime = now / (windowSizeMs / bucketCount);

            // Find or create bucket for current time
            int targetIndex = (int) (bucketTime % bucketCount);

            if (timestamps[targetIndex] != bucketTime) {
                // New time bucket, reset old data
                totalCount -= counts[targetIndex];
                counts[targetIndex] = 0;
                timestamps[targetIndex] = bucketTime;
            }

            counts[targetIndex]++;
            totalCount++;
        }

        /**
         * Gets the current rate without recording an event
         */
        public synchronized double getCurrentRate() {
            cleanExpiredBuckets();
            return (double) totalCount * 1000.0 / windowSizeMs;
        }

        /**
         * Removes expired buckets from the calculation
         */
        private void cleanExpiredBuckets() {
            long now = System.currentTimeMillis();
            long currentBucketTime = now / (windowSizeMs / bucketCount);

            for (int i = 0; i < bucketCount; i++) {
                if (currentBucketTime - timestamps[i] > bucketCount && counts[i] > 0) {
                    totalCount -= counts[i];
                    counts[i] = 0;
                    timestamps[i] = 0;
                }
            }
        }
    }
}