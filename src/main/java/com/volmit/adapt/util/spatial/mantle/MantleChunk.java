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

import com.volmit.adapt.util.spatial.matter.Matter;
import com.volmit.adapt.util.spatial.matter.MatterSlice;
import com.volmit.adapt.util.spatial.matter.SpatialMatter;
import com.volmit.adapt.util.spatial.util.Consume;
import lombok.Getter;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * Represents a mantle chunk. Mantle chunks contain sections of matter (see
 * matter api) Mantle Chunks are fully atomic & thread safe
 */
public class MantleChunk {
    @Getter
    private final int x;
    @Getter
    private final int z;
    private final AtomicReferenceArray<Matter> sections;

    /**
     * Create a mantle chunk
     *
     * @param sectionHeight
     *            the height of the world in sections (blocks >> 4)
     */
    public MantleChunk(int sectionHeight, int x, int z) {
        sections = new AtomicReferenceArray<>(sectionHeight);
        this.x = x;
        this.z = z;
    }

    /**
     * Load a mantle chunk from a data stream
     *
     * @param sectionHeight
     *            the height of the world in sections (blocks >> 4)
     * @param din
     *            the data input
     * @throws IOException
     *             shit happens
     * @throws ClassNotFoundException
     *             shit happens
     */
    public MantleChunk(int sectionHeight, DataInputStream din) throws IOException, ClassNotFoundException {
        this(sectionHeight, din.readByte(), din.readByte());
        int s = din.readByte();

        for (int i = 0; i < s; i++) {
            if (din.readBoolean()) {
                sections.set(i, Matter.readDin(din));
            }
        }
    }

    /**
     * Check if a section exists (same as get(section) != null)
     *
     * @param section
     *            the section (0 - (worldHeight >> 4))
     * @return true if it exists
     */
    public boolean exists(int section) {
        return get(section) != null;
    }

    /**
     * Get thje matter at the given section or null if it doesnt exist
     *
     * @param section
     *            the section (0 - (worldHeight >> 4))
     * @return the matter or null if it doesnt exist
     */
    public Matter get(int section) {
        return sections.get(section);
    }

    /**
     * Clear all matter from this chunk
     */
    public void clear() {
        for (int i = 0; i < sections.length(); i++) {
            delete(i);
        }
    }

    /**
     * Delete the matter from the given section
     *
     * @param section
     *            the section (0 - (worldHeight >> 4))
     */
    public void delete(int section) {
        sections.set(section, null);
    }

    /**
     * Get or create a new matter section at the given section
     *
     * @param section
     *            the section (0 - (worldHeight >> 4))
     * @return the matter
     */
    public Matter getOrCreate(int section) {
        Matter matter = get(section);

        if (matter == null) {
            matter = new SpatialMatter(16, 16, 16);
            sections.set(section, matter);
        }

        return matter;
    }

    /**
     * Write this chunk to a data stream
     *
     * @param dos
     *            the stream
     * @throws IOException
     *             shit happens
     */
    public void write(DataOutputStream dos) throws IOException {
        dos.writeByte(x);
        dos.writeByte(z);
        dos.writeByte(sections.length());

        for (int i = 0; i < sections.length(); i++) {
            trimSlice(i);

            if (exists(i)) {
                dos.writeBoolean(true);
                Matter matter = get(i);
                matter.writeDos(dos);
            } else {
                dos.writeBoolean(false);
            }
        }
    }

    private void trimSlice(int i) {
        if (exists(i)) {
            Matter m = get(i);

            if (m.getSliceMap().isEmpty()) {
                sections.set(i, null);
            } else {
                m.trimSlices();
                if (m.getSliceMap().isEmpty()) {
                    sections.set(i, null);
                }
            }
        }
    }

    public <T> void iterate(Class<T> type, Consume.Four<Integer, Integer, Integer, T> iterator) {
        for (int i = 0; i < sections.length(); i++) {
            int bs = (i << 4);
            Matter matter = get(i);

            if (matter != null) {
                MatterSlice<T> t = matter.getSlice(type);

                if (t != null) {
                    t.iterateSync((a, b, c, f) -> iterator.accept(a, b + bs, c, f));
                }
            }
        }
    }

    public void deleteSlices(Class<?> c) {
        for (int i = 0; i < sections.length(); i++) {
            Matter m = sections.get(i);
            if (m != null && m.hasSlice(c)) {
                m.deleteSlice(c);
            }
        }
    }

    public void trimSlices() {
        for (int i = 0; i < sections.length(); i++) {
            if (exists(i)) {
                trimSlice(i);
            }
        }
    }
}
