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

import com.volmit.adapt.util.spatial.container.DataContainer;
import com.volmit.adapt.util.spatial.container.NodeWritable;
import com.volmit.adapt.util.spatial.hunk.Hunk;
import com.volmit.adapt.util.spatial.util.Consume;

import java.io.IOException;
import java.util.function.Supplier;

public abstract class PaletteOrHunk<T> extends StorageHunk<T> implements Hunk<T>, NodeWritable<T> {
    private final Hunk<T> hunk;

    public PaletteOrHunk(int width, int height, int depth, boolean allow, Supplier<Hunk<T>> factory) {
        super(width, height, depth);
        hunk = (allow && (width * height * depth <= 4096))
                ? new PaletteHunk<>(width, height, depth, this)
                : factory.get();
    }

    public DataContainer<T> palette() {
        return isPalette() ? ((PaletteHunk<T>) hunk).getData() : null;
    }

    public boolean isPalette() {
        return hunk instanceof PaletteHunk;
    }

    public void setPalette(DataContainer<T> c) {
        if (isPalette()) {
            ((PaletteHunk<T>) hunk).setPalette(c);
        }
    }

    @Override
    public void setRaw(int x, int y, int z, T t) {
        hunk.setRaw(x, y, z, t);
    }

    @Override
    public T getRaw(int x, int y, int z) {
        return hunk.getRaw(x, y, z);
    }

    public int getEntryCount() {
        return hunk.getEntryCount();
    }

    public boolean isMapped() {
        return hunk.isMapped();
    }

    public boolean isEmpty() {
        return hunk.isMapped();
    }

    @Override
    public synchronized Hunk<T> iterateSync(Consume.Four<Integer, Integer, Integer, T> c) {
        hunk.iterateSync(c);
        return this;
    }

    @Override
    public synchronized Hunk<T> iterateSyncIO(Consume.FourIO<Integer, Integer, Integer, T> c) throws IOException {
        hunk.iterateSyncIO(c);
        return this;
    }

    @Override
    public void empty(T b) {
        hunk.empty(b);
    }
}
