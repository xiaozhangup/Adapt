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

import lombok.Data;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

@Data
public class MatterHeader {
    private String author = "";
    private long createdAt = System.currentTimeMillis();
    private int version = Matter.VERSION;

    public void write(DataOutputStream out) throws IOException {
        out.writeUTF(author);
        out.writeLong(createdAt);
        out.writeShort(version);
    }

    public void read(DataInputStream din) throws IOException {
        setAuthor(din.readUTF());
        setCreatedAt(din.readLong());
        setVersion(din.readShort());
    }
}
