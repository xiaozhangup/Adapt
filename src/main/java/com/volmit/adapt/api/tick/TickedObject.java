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
import com.volmit.adapt.util.M;
import org.bukkit.event.Listener;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

public abstract class TickedObject implements Ticked, Listener {
    private Long lastTick;
    private Long interval;
    private final String group;
    private final String id;
    private final Long identifier;
    private final AtomicBoolean unregistered;

    public TickedObject() {
        this("null");
    }

    public TickedObject(String group, String id) {
        this(group, id, 1000);
    }

    public TickedObject(String group) {
        this(group, UUID.randomUUID().toString(), 1000);
    }

    public TickedObject(String group, long interval) {
        this(group, UUID.randomUUID().toString(), interval);
    }

    public TickedObject(String group, String id, long interval) {
        this.group = group;
        this.id = id;
        this.interval = interval;
        this.lastTick = M.ms();
        this.identifier = Adapt.instance.getTicker().generateId();
        this.unregistered = new AtomicBoolean(false);
        Adapt.instance.getTicker().register(this);
        Adapt.instance.registerListener(this);
    }


    @Override
    public void unregister() {
        Adapt.instance.getTicker().unregister(this);
        Adapt.instance.unregisterListener(this);
    }

    @Override
    public long getLastTick() {
        return lastTick;
    }

    @Override
    public long getInterval() {
        return interval;
    }

    @Override
    public void setInterval(long ms) {
        interval = ms;
    }

    @Override
    public void tick() {
        lastTick = M.ms();
        onTick();
    }

    public abstract void onTick();

    @Override
    public String getGroup() {
        return group;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public long getIdentifier() {
        return identifier;
    }

    @Override
    public boolean isUnregistered() {
        return unregistered.get();
    }

    @Override
    public void setUnregistered(boolean unregistered) {
        this.unregistered.set(unregistered);
    }
}
