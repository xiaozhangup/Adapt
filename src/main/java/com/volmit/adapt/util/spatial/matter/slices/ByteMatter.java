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

package com.volmit.adapt.util.spatial.matter.slices;

import com.volmit.adapt.util.spatial.container.Palette;
import com.volmit.adapt.util.spatial.matter.Sliced;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

@Sliced
public class ByteMatter extends RawMatter<Byte> {
    public ByteMatter() {
        this(1, 1, 1);
    }

    public ByteMatter(int width, int height, int depth) {
        super(width, height, depth, Byte.class);
    }

    @Override
    public Palette<Byte> getGlobalPalette() {
        return null;
    }

    @Override
    public void writeNode(Byte b, DataOutputStream dos) throws IOException {
        dos.writeByte(b);
    }

    @Override
    public Byte readNode(DataInputStream din) throws IOException {
        return din.readByte();
    }
}
