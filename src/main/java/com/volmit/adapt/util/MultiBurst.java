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

public class MultiBurst {
    public static final MultiBurst burst = new MultiBurst();
    @Getter
    private final ExecutorService service;
    private final AtomicInteger tid = new AtomicInteger(0);
    private final AtomicLong lastWarningTime = new AtomicLong(0);

    public MultiBurst() {
        int corePoolSize = 2;
        int maxPoolSize = 8;

        service = new ThreadPoolExecutor(
                corePoolSize,
                maxPoolSize,
                10L, TimeUnit.SECONDS,
                new LinkedBlockingDeque<>(16),
                r -> {
                    Thread t = Executors.defaultThreadFactory().newThread(r);
                    t.setName("Adapt Dynamic Workgroup " + tid.incrementAndGet());
                    t.setUncaughtExceptionHandler((et, e) -> {
                        Adapt.info("Exception encountered in " + et.getName());
                        e.printStackTrace();
                    });
                    return t;
                },
                (r, executor) -> {
                    long now = System.currentTimeMillis();
                    if (now - lastWarningTime.get() > 90_000) { // 90秒内最多弹出一次
                        lastWarningTime.set(now);
                        Adapt.warn("MultiBurst thread pool is full! Running task in the calling thread. (Current " + executor.getTaskCount() + " tasks)");
                    }
                    if (!executor.isShutdown()) {
                        r.run(); // 确保任务执行
                    }
                }
        );

        ((ThreadPoolExecutor) service).allowCoreThreadTimeOut(true);
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
}
