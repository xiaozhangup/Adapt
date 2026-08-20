package com.volmit.adapt.content.util;

import org.bukkit.block.Block;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Bounded, contiguous block search for vein-style adaptations.
 */
public final class BoundedBlockSearch {
    public static final int HARD_BLOCK_LIMIT = 256;

    private BoundedBlockSearch() {
    }

    public static List<Block> find(Block start, int radius, int requestedLimit, Predicate<Block> accepted) {
        int limit = Math.min(Math.max(1, requestedLimit), HARD_BLOCK_LIMIT);
        int radiusSquared = radius * radius;
        ArrayDeque<Block> pending = new ArrayDeque<>();
        Set<Block> visited = new HashSet<>();
        List<Block> result = new ArrayList<>(limit);

        pending.add(start);
        visited.add(start);

        while (!pending.isEmpty() && result.size() < limit) {
            Block current = pending.removeFirst();
            if (!withinRadius(start, current, radiusSquared) || !accepted.test(current)) {
                continue;
            }

            result.add(current);
            for (int x = -1; x <= 1; x++) {
                for (int y = -1; y <= 1; y++) {
                    for (int z = -1; z <= 1; z++) {
                        if (x == 0 && y == 0 && z == 0) {
                            continue;
                        }

                        Block next = current.getRelative(x, y, z);
                        if (withinRadius(start, next, radiusSquared) && visited.add(next)) {
                            pending.addLast(next);
                        }
                    }
                }
            }
        }

        return result;
    }

    private static boolean withinRadius(Block center, Block candidate, int radiusSquared) {
        int x = center.getX() - candidate.getX();
        int y = center.getY() - candidate.getY();
        int z = center.getZ() - candidate.getZ();
        return x * x + y * y + z * z <= radiusSquared;
    }
}
