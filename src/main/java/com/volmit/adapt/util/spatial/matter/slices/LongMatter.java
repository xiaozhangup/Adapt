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
import com.volmit.adapt.util.spatial.util.Varint;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

@Sliced
public class LongMatter extends RawMatter<Long> {
    public LongMatter() {
        this(1, 1, 1);
    }

    public LongMatter(int width, int height, int depth) {
        super(width, height, depth, Long.class);
    }

    @Override
    public Palette<Long> getGlobalPalette() {
        return null;
    }

    @Override
    public void writeNode(Long b, DataOutputStream dos) throws IOException {
        Varint.writeSignedVarLong(b, dos);
    }

    @Override
    public Long readNode(DataInputStream din) throws IOException {
        return Varint.readSignedVarLong(din);
    }
}
