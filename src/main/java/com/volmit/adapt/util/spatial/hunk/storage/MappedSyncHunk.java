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

package com.volmit.adapt.util.spatial.hunk.storage;

import com.volmit.adapt.util.spatial.hunk.Hunk;
import com.volmit.adapt.util.spatial.util.Consume;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings({"DefaultAnnotationParam", "Lombok"})
@Data
@EqualsAndHashCode(callSuper = false)
public class MappedSyncHunk<T> extends StorageHunk<T> implements Hunk<T> {
    private final Map<Integer, T> data;

    public MappedSyncHunk(int w, int h, int d) {
        super(w, h, d);
        data = new HashMap<>();
    }

    public int getEntryCount() {
        return data.size();
    }

    public boolean isMapped() {
        return true;
    }

    public boolean isEmpty() {
        synchronized (data) {
            return data.isEmpty();
        }
    }

    @Override
    public void setRaw(int x, int y, int z, T t) {
        synchronized (data) {
            if (t == null) {
                data.remove(index(x, y, z));
                return;
            }

            data.put(index(x, y, z), t);
        }
    }

    private Integer index(int x, int y, int z) {
        return (z * getWidth() * getHeight()) + (y * getWidth()) + x;
    }

    @Override
    public synchronized Hunk<T> iterateSync(Consume.Four<Integer, Integer, Integer, T> c) {
        synchronized (data) {
            int idx, z;

            for (Map.Entry<Integer, T> g : data.entrySet()) {
                idx = g.getKey();
                z = idx / (getWidth() * getHeight());
                idx -= (z * getWidth() * getHeight());
                c.accept(idx % getWidth(), idx / getWidth(), z, g.getValue());
            }

            return this;
        }
    }

    @Override
    public synchronized Hunk<T> iterateSyncIO(Consume.FourIO<Integer, Integer, Integer, T> c) throws IOException {
        synchronized (data) {
            int idx, z;

            for (Map.Entry<Integer, T> g : data.entrySet()) {
                idx = g.getKey();
                z = idx / (getWidth() * getHeight());
                idx -= (z * getWidth() * getHeight());
                c.accept(idx % getWidth(), idx / getWidth(), z, g.getValue());
            }

            return this;
        }
    }

    @Override
    public void empty(T b) {
        synchronized (data) {
            data.clear();
        }
    }

    @Override
    public T getRaw(int x, int y, int z) {
        synchronized (data) {
            return data.get(index(x, y, z));
        }
    }
}
