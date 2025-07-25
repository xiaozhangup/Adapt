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

package com.volmit.adapt.util.spatial.container;

import com.volmit.adapt.util.spatial.util.Consume;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class HashPalette<T> implements Palette<T> {
    private final Map<T, Integer> palette;
    private final Map<Integer, T> lookup;
    private final AtomicInteger size;

    public HashPalette() {
        this.size = new AtomicInteger(0);
        this.palette = new LinkedHashMap<>();
        this.lookup = new ConcurrentHashMap<>();
        add(null);
    }

    @Override
    public T get(int id) {
        if (id < 0 || id >= size.get()) {
            return null;
        }

        return lookup.get(id);
    }

    @Override
    public int add(T t) {
        int index = size.getAndIncrement();
        palette.put(t, index);

        if (t != null) {
            lookup.put(index, t);
        }

        return index;
    }

    @Override
    public int id(T t) {
        if (t == null) {
            return 0;
        }

        Integer v = palette.get(t);
        return v != null ? v : -1;
    }

    @Override
    public int size() {
        return size.get() - 1;
    }

    @Override
    public void iterate(Consume.Two<T, Integer> c) {
        for (T i : palette.keySet()) {
            if (i == null) {
                continue;
            }

            c.accept(i, id(i));
        }
    }
}
