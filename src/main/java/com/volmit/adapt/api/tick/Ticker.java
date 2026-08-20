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
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitTask;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class Ticker {
    private final AtomicLong idGenerator = new AtomicLong(0);
    private final ConcurrentHashMap<Long, Ticked> tickList = new ConcurrentHashMap<>();
    private final BukkitTask task;
    private boolean running = true;

    public Ticker() {
        task = Bukkit.getScheduler().runTaskTimer(Adapt.instance, this::tick, 0, 1);
    }

    public void register(Ticked ticked) {
        tickList.put(ticked.getIdentifier(), ticked);
    }

    public void unregister(Ticked ticked) {
        ticked.setUnregistered(true);
    }

    public void clear() {
        task.cancel();
        running = false;
        tickList.clear();
    }

    public long generateId() {
        return idGenerator.incrementAndGet();
    }

    private void tick() {
        if (!running) {
            return;
        }

        for (Ticked t : tickList.values()) {
            if (t.isUnregistered() || !t.shouldTick()) {
                continue;
            }

            try {
                t.tick();
            } catch (Throwable ex) {
                ex.printStackTrace();
            }
        }

        tickList.values().removeIf(Ticked::isUnregistered);
    }
}
