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

package com.volmit.adapt.util.spatial.mantle;

import com.volmit.adapt.util.spatial.util.CompressedNumbers;
import lombok.Getter;

import java.io.*;
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Tectonic Plates are essentially representations of regions in minecraft.
 * Tectonic Plates are fully atomic & thread safe
 */
public class MantleRegion {
    private final int sectionHeight;
    private final AtomicReferenceArray<MantleChunk> chunks;

    @Getter
    private final int x;

    @Getter
    private final int z;

    /**
     * Create a new tectonic plate
     *
     * @param worldHeight
     *            the height of the world
     */
    public MantleRegion(int worldHeight, int x, int z) {
        this.sectionHeight = worldHeight >> 4;
        this.chunks = new AtomicReferenceArray<>(1024);
        this.x = x;
        this.z = z;
    }

    /**
     * Load a tectonic plate from a data stream
     *
     * @param worldHeight
     *            the height of the world
     * @param din
     *            the data input
     * @throws IOException
     *             shit happens yo
     * @throws ClassNotFoundException
     *             real shit bro
     */
    public MantleRegion(int worldHeight, DataInputStream din) throws IOException, ClassNotFoundException {
        this(worldHeight, din.readInt(), din.readInt());
        for (int i = 0; i < chunks.length(); i++) {
            if (din.readBoolean()) {
                chunks.set(i, new MantleChunk(sectionHeight, din));
            }
        }
    }

    public static MantleRegion read(int worldHeight, File file) throws IOException, ClassNotFoundException {
        FileInputStream fin = new FileInputStream(file);
        GZIPInputStream gzi = new GZIPInputStream(fin);
        DataInputStream din = new DataInputStream(gzi);
        MantleRegion p = new MantleRegion(worldHeight, din);
        din.close();

        return p;
    }

    /**
     * Check if a chunk exists in this plate or not (same as get(x, z) != null)
     *
     * @param x
     *            the chunk relative x (0-31)
     * @param z
     *            the chunk relative z (0-31)
     * @return true if the chunk exists
     */
    public boolean exists(int x, int z) {
        return get(x, z) != null;
    }

    /**
     * Get a chunk at the given coordinates or null if it doesnt exist
     *
     * @param x
     *            the chunk relative x (0-31)
     * @param z
     *            the chunk relative z (0-31)
     * @return the chunk or null if it doesnt exist
     */
    public MantleChunk get(int x, int z) {
        return chunks.get(index(x, z));
    }

    /**
     * Clear all chunks from this tectonic plate
     */
    public void clear() {
        for (int i = 0; i < chunks.length(); i++) {
            chunks.set(i, null);
        }
    }

    /**
     * Delete a chunk from this tectonic plate
     *
     * @param x
     *            the chunk relative x (0-31)
     * @param z
     *            the chunk relative z (0-31)
     */
    public void delete(int x, int z) {
        chunks.set(index(x, z), null);
    }

    /**
     * Get a tectonic plate, or create one and insert it & return it if it diddnt
     * exist
     *
     * @param x
     *            the chunk relative x (0-31)
     * @param z
     *            the chunk relative z (0-31)
     * @return the chunk (read or created & inserted)
     */
    public MantleChunk getOrCreate(int x, int z) {
        MantleChunk chunk = get(x, z);

        if (chunk == null) {
            chunk = new MantleChunk(sectionHeight, x & 31, z & 31);
            chunks.set(index(x, z), chunk);
        }

        return chunk;
    }

    private int index(int x, int z) {
        return CompressedNumbers.index3Dto1D(x, z, 0, 32, 32);
    }

    /**
     * Write this tectonic plate to file
     *
     * @param file
     *            the file to writeNodeData it to
     * @throws IOException
     *             shit happens
     */
    public void write(File file) throws IOException {
        FileOutputStream fos = new FileOutputStream(file);
        GZIPOutputStream gzo = new GZIPOutputStream(fos);
        DataOutputStream dos = new DataOutputStream(gzo);
        write(dos);
        dos.close();
    }

    /**
     * Write this tectonic plate to a data stream
     *
     * @param dos
     *            the data output
     * @throws IOException
     *             shit happens
     */
    public void write(DataOutputStream dos) throws IOException {
        dos.writeInt(x);
        dos.writeInt(z);

        for (int i = 0; i < chunks.length(); i++) {
            MantleChunk chunk = chunks.get(i);

            if (chunk != null) {
                dos.writeBoolean(true);
                chunk.write(dos);
            } else {
                dos.writeBoolean(false);
            }
        }
    }
}
