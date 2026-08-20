package com.volmit.adapt.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public final class Sphere implements Iterable<BlockPosition> {
    private final List<BlockPosition> blocks;

    public Sphere(int radius) {
        int dist = radius * radius;

        List<BlockPosition> positions = new ArrayList<>();
        for (int x = -radius; x <= radius; x++) {
            for (int z = -radius; z <= radius; z++) {
                for (int y = -radius; y <= radius; y++) {
                    if (x * x + z * z + y * y > dist)
                        continue;

                    positions.add(new BlockPosition(x, y, z));
                }
            }
        }
        blocks = List.copyOf(positions);
    }

    public int size() {
        return blocks.size();
    }

    @Override
    public Iterator<BlockPosition> iterator() {
        return blocks.iterator();
    }
}
