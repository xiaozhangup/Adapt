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

package com.volmit.adapt.util.spatial.matter;

import com.volmit.adapt.util.spatial.container.DataContainer;
import com.volmit.adapt.util.spatial.container.NodeWritable;
import com.volmit.adapt.util.spatial.container.Palette;
import com.volmit.adapt.util.spatial.container.PaletteType;
import com.volmit.adapt.util.spatial.hunk.Hunk;
import com.volmit.adapt.util.spatial.hunk.storage.PaletteOrHunk;
import com.volmit.adapt.util.spatial.util.CompressedNumbers;
import com.volmit.adapt.util.spatial.util.Pos;
import com.volmit.adapt.util.spatial.util.Varint;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public interface MatterSlice<T> extends Hunk<T>, PaletteType<T>, NodeWritable<T> {
    Class<T> getType();

    default Palette<T> getGlobalPalette() {
        return null;
    }

    @Override
    default void writePaletteNode(DataOutputStream dos, T s) throws IOException {
        writeNode(s, dos);
    }

    @Override
    default void writeNodeData(DataOutputStream dos, T s) throws IOException {
        writeNode(s, dos);
    }

    @Override
    default T readPaletteNode(DataInputStream din) throws IOException {
        return readNode(din);
    }

    @Override
    default T readNodeData(DataInputStream din) throws IOException {
        return readNode(din);
    }

    default void applyFilter(MatterFilter<T> filter) {
        updateSync(filter::update);
    }

    default void inject(MatterSlice<T> slice) {
        iterateSync(slice::set);
    }

    default void forceInject(MatterSlice<?> slice) {
        inject((MatterSlice<T>) slice);
    }

    void writeNode(T b, DataOutputStream dos) throws IOException;

    T readNode(DataInputStream din) throws IOException;

    <W> MatterWriter<W, T> writeInto(Class<W> mediumType);

    <W> MatterReader<W, T> readFrom(Class<W> mediumType);

    default Class<?> getClass(Object w) {
        return w.getClass();
    }

    default <W> boolean writeInto(W w, int x, int y, int z) {
        MatterWriter<W, T> injector = (MatterWriter<W, T>) writeInto(getClass(w));

        if (injector == null) {
            return false;
        }

        iterateSync((a, b, c, t) -> injector.writeMatter(w, t, a + x, b + y, c + z));

        return true;
    }

    default <W> boolean readFrom(W w, int x, int y, int z) {
        MatterReader<W, T> ejector = (MatterReader<W, T>) readFrom(getClass(w));

        if (ejector == null) {
            return false;
        }

        for (int i = x; i < x + getWidth(); i++) {
            for (int j = y; j < y + getHeight(); j++) {
                for (int k = z; k < z + getDepth(); k++) {
                    T v = ejector.readMatter(w, i, j, k);

                    if (v != null) {
                        set(i - x, j - y, k - z, v);
                    }
                }
            }
        }

        return true;
    }

    default boolean canWrite(Class<?> mediumType) {
        return writeInto(mediumType) != null;
    }

    default boolean canRead(Class<?> mediumType) {
        return readFrom(mediumType) != null;
    }

    default int getBitsPer(int needed) {
        int target = 1;
        for (int i = 1; i < 8; i++) {
            if (Math.pow(2, i) > needed) {
                target = i;
                break;
            }
        }

        return target;
    }

    default void write(DataOutputStream dos) throws IOException {
        dos.writeUTF(getType().getCanonicalName());

        if ((this instanceof PaletteOrHunk f && f.isPalette())) {
            f.palette().writeDos(dos);
            return;
        }

        int w = getWidth();
        int h = getHeight();
        MatterPalette<T> palette = new MatterPalette<T>(this);
        iterateSync((x, y, z, b) -> palette.assign(b));
        palette.writePalette(dos);
        dos.writeBoolean(isMapped());

        if (isMapped()) {
            Varint.writeUnsignedVarInt(getEntryCount(), dos);
            iterateSyncIO((x, y, z, b) -> {
                Varint.writeUnsignedVarInt(CompressedNumbers.index3Dto1D(x, y, z, w, h), dos);
                palette.writeNode(b, dos);
            });
        } else {
            iterateSyncIO((x, y, z, b) -> palette.writeNode(b, dos));
        }
    }

    default void read(DataInputStream din) throws IOException {
        if ((this instanceof PaletteOrHunk f && f.isPalette())) {
            f.setPalette(new DataContainer<>(din, this));
            return;
        }

        int w = getWidth();
        int h = getHeight();
        MatterPalette<T> palette = new MatterPalette<T>(this, din);
        if (din.readBoolean()) {
            int nodes = Varint.readUnsignedVarInt(din);
            int[] pos;

            while (nodes-- > 0) {
                pos = CompressedNumbers.index1Dto3D(Varint.readUnsignedVarInt(din), w, h);
                setRaw(pos[0], pos[1], pos[2], palette.readNode(din));
            }
        } else {
            iterateSyncIO((x, y, z, b) -> setRaw(x, y, z, palette.readNode(din)));
        }
    }

    default void rotateSliceInto(Matter n, double x, double y, double z) {
        rotate(x, y, z, (_x, _y, _z) -> n.slice(getType()));
    }

    default boolean containsKey(Pos v) {
        return get(v.getX(), v.getY(), v.getZ()) != null;
    }

    default void put(Pos v, T d) {
        set(v.getX(), v.getY(), v.getZ(), d);
    }

    default T get(Pos v) {
        return get(v.getX(), v.getY(), v.getZ());
    }
}
