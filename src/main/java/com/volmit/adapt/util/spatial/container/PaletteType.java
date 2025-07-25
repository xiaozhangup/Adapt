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

import com.volmit.adapt.util.spatial.util.Varint;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public interface PaletteType<T> {
    void writePaletteNode(DataOutputStream dos, T t) throws IOException;

    T readPaletteNode(DataInputStream din) throws IOException;

    default void writeList(DataOutputStream dos, List<T> list) throws IOException {
        Varint.writeUnsignedVarInt(list.size(), dos);
        for (T i : list) {
            writePaletteNode(dos, i);
        }
    }

    default List<T> readList(DataInputStream din) throws IOException {
        int v = Varint.readUnsignedVarInt(din);
        List<T> t = new ArrayList<>();

        for (int i = 0; i < v; i++) {
            t.add(readPaletteNode(din));
        }

        return t;
    }
}
