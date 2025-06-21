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

import com.volmit.adapt.util.BurstExecutor;
import com.volmit.adapt.util.J;
import com.volmit.adapt.util.MultiBurst;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class Ticker {
    private final List<Ticked> ticklist;
    private final ConcurrentLinkedQueue<Ticked> newTicks;
    private final Set<String> removeTicks;
    private volatile boolean ticking;

    public Ticker() {
        this.ticklist = new CopyOnWriteArrayList<>();
        this.newTicks = new ConcurrentLinkedQueue<>();
        this.removeTicks = ConcurrentHashMap.newKeySet();
        this.ticking = false;
        J.ar(() -> {
            if (!ticking) {
                tick();
            }
        }, 0);
    }

    public void register(Ticked ticked) {
        newTicks.add(ticked);
    }

    public void unregister(Ticked ticked) {
        removeTicks.add(ticked.getId());
    }

    public void clear() {
        ticklist.clear();
        newTicks.clear();
        removeTicks.clear();
    }

    private void tick() {
        ticking = true;
        AtomicInteger tc = new AtomicInteger(0);
        BurstExecutor e = MultiBurst.burst.burst(ticklist.size());

        for (Ticked t : ticklist) {
            e.queue(() -> {
                if (t.shouldTick()) {
                    tc.incrementAndGet();
                    try {
                        t.tick();
                    } catch (Throwable ex) {
                        ex.printStackTrace();
                    }
                }
            });
        }

        e.complete();

        Ticked t;
        while ((t = newTicks.poll()) != null) {
            ticklist.add(t);
        }

        ticklist.removeIf(ticked -> removeTicks.remove(ticked.getId()));

        ticking = false;
    }
}
