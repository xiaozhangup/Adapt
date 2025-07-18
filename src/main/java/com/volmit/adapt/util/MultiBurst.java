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
import java.util.concurrent.atomic.LongAdder;

public class MultiBurst {
    public static final MultiBurst burst = new MultiBurst();
    public static final MultiBurst virtualBurst = new MultiBurst(Executors.newVirtualThreadPerTaskExecutor());
    @Getter
    private final ExecutorService service;
    private final AtomicInteger tid = new AtomicInteger(0);
    private final AtomicLong lastWarningTime = new AtomicLong(0);
    private final QPSCounter qpsCounter = new QPSCounter();

    public MultiBurst() {
        int corePoolSize = 2;
        int maxPoolSize = Math.max(8, Runtime.getRuntime().availableProcessors() - 4);

        service = new ThreadPoolExecutor(corePoolSize, maxPoolSize, 10L, TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(512), r -> {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setName("Adapt Dynamic Workgroup " + tid.incrementAndGet());
                    t.setUncaughtExceptionHandler((et, e) -> {
                        Adapt.info("Exception encountered in " + et.getName());
                        e.printStackTrace();
                    });
                    return t;
                }, (r, executor) -> {
                    long now = System.currentTimeMillis();
                    if (now - lastWarningTime.get() > 1200_000) { // 1200秒内最多弹出一次
                        lastWarningTime.set(now);
                        Adapt.warn("MultiBurst thread pool is full! Running task in the calling thread. ("
                                + qpsCounter.getQPS() + " overloaded tasks / s)");
                    }
                    qpsCounter.record();
                    if (!executor.isShutdown()) {
                        r.run(); // 确保任务执行
                    }
                });

        ((ThreadPoolExecutor) service).allowCoreThreadTimeOut(true);
    }

    public MultiBurst(ExecutorService service) {
        this.service = service;
    }

    public void burst(Runnable... r) {
        burst(r.length).queue(r).complete();
    }

    public void sync(Runnable... r) {
        for (Runnable i : r) {
            i.run();
        }
    }

    public BurstExecutor burst(int estimate) {
        return new BurstExecutor(service, estimate);
    }

    public BurstExecutor burst() {
        return burst(16);
    }

    public void lazy(Runnable o) {
        service.execute(o);
    }

    private static class QPSCounter {
        private static final int WINDOW_SIZE = 3; // 统计最近 3 秒
        private final LongAdder[] slots = new LongAdder[WINDOW_SIZE];
        private volatile long lastSecond;

        public QPSCounter() {
            for (int i = 0; i < WINDOW_SIZE; i++) {
                slots[i] = new LongAdder();
            }
            lastSecond = System.currentTimeMillis() / 1000;
        }

        public void record() {
            long now = System.currentTimeMillis() / 1000;
            int index = (int) (now % WINDOW_SIZE);

            if (now != lastSecond) {
                synchronized (this) {
                    if (now != lastSecond) {
                        slots[(int) (lastSecond % WINDOW_SIZE)].reset();
                        lastSecond = now;
                    }
                }
            }

            slots[index].increment();
        }

        public int getQPS() {
            long sum = 0;
            for (LongAdder slot : slots) {
                sum += slot.sum();
            }
            return (int) (sum / (double) WINDOW_SIZE);
        }
    }
}
