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

package com.volmit.adapt.util.spatial.hunk.view;

import com.volmit.adapt.util.spatial.hunk.Hunk;

@SuppressWarnings("ClassCanBeRecord")
public class FringedHunkView<T> implements Hunk<T> {
    private final Hunk<T> src;
    private final Hunk<T> out;

    public FringedHunkView(Hunk<T> src, Hunk<T> out) {
        this.src = src;
        this.out = out;
    }

    @Override
    public void setRaw(int x, int y, int z, T t) {
        out.setRaw(x, y, z, t);
    }

    @Override
    public T getRaw(int x, int y, int z) {
        return src.getRaw(x, y, z);
    }

    @Override
    public int getWidth() {
        return src.getWidth();
    }

    @Override
    public int getHeight() {
        return src.getHeight();
    }

    @Override
    public int getDepth() {
        return src.getDepth();
    }

    @Override
    public Hunk<T> getSource() {
        return src;
    }
}
