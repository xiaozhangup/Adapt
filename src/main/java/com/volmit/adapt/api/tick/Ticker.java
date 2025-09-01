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

package com.volmit.adapt.api.tick;

import com.volmit.adapt.Adapt;
import com.volmit.adapt.util.BurstExecutor;
import com.volmit.adapt.util.MultiBurst;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class Ticker {
    private final AtomicLong idGenerator = new AtomicLong(0);
    private final AtomicBoolean isRunning = new AtomicBoolean(true);
    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private final ConcurrentHashMap<Long, Ticked> tickList = new ConcurrentHashMap<>();
    private final BukkitTask task; // 任务执行线程

    public Ticker() {
        task = Bukkit.getScheduler().runTaskTimerAsynchronously(
                Adapt.instance,
                () -> {
                    if (!isProcessing.get()) {
                        try {
                            isProcessing.set(true);
                            tick();
                        } finally {
                            isProcessing.set(false);
                        }
                    }
                },
                0,
                1
        );
    }

    public void register(Ticked ticked) {
        tickList.put(ticked.getIdentifier(), ticked);
    }

    public void unregister(Ticked ticked) {
        ticked.setUnregistered(true);
    }

    public void clear() {
        task.cancel();
        isRunning.set(false);
        tickList.clear();
    }

    public long generateId() {
        return idGenerator.incrementAndGet();
    }

    private void tick() {
        if (!isRunning.get()) {
            return;
        }

        BurstExecutor e = MultiBurst.burst.burst(tickList.size());
        for (Ticked t : tickList.values()) {
            e.queue(() -> {
                if (t.shouldTick() && !t.isUnregistered()) {
                    try {
                        t.tick();
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                    }
                }
            });
        }

        e.complete();
        tickList.values().removeIf(Ticked::isUnregistered);
    }
}
