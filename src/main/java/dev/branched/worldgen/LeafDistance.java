package dev.branched.worldgen;

import dev.branched.morphology.BlockPlacement;
import dev.branched.morphology.PlacementRole;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Computes vanilla-compatible leaf distances for one rasterized tree.
 *
 * <p>Vanilla leaves store a {@code distance} in {@code [1, 6]} and decay when
 * no log/branch is reachable within that range. Branched writes the
 * distance explicitly instead of letting a scheduled update discover it, so a
 * freshly generated tree never sheds leaves on its first tick.</p>
 *
 * <p>The search is a multi-source breadth-first search seeded at every wood
 * block (log or branch). Only positions occupied by leaves are traversed, so
 * leaves separated from the trunk by air are dropped, exactly like vanilla
 * worldgen would leave them unsupported.</p>
 *
 * <p>This class is pure Java and unit tested without a Minecraft bootstrap.</p>
 */
public final class LeafDistance {
    /** Highest distance a vanilla leaf may store; distance 7 means "decay". */
    public static final int MAX_SUPPORTED_DISTANCE = 6;

    private static final int[][] OFFSETS = {
            {1, 0, 0}, {-1, 0, 0},
            {0, 1, 0}, {0, -1, 0},
            {0, 0, 1}, {0, 0, -1}
    };

    private LeafDistance() {
    }

    /**
     * Returns the shortest wood distance for every leaf that is within
     * {@link #MAX_SUPPORTED_DISTANCE}. Leaves absent from the map must not be
     * written (they would decay immediately).
     */
    public static Map<Long, Integer> distances(Collection<BlockPlacement> blocks) {
        Set<Long> leaves = new HashSet<>();
        for (BlockPlacement block : blocks) {
            if (block.role() == PlacementRole.LEAVES) {
                leaves.add(key(block.x(), block.y(), block.z()));
            }
        }
        Map<Long, Integer> distances = new HashMap<>();
        if (leaves.isEmpty()) {
            return distances;
        }

        ArrayDeque<int[]> queue = new ArrayDeque<>();
        for (BlockPlacement block : blocks) {
            if (block.role() == PlacementRole.LEAVES) {
                continue;
            }
            for (int[] offset : OFFSETS) {
                int x = block.x() + offset[0];
                int y = block.y() + offset[1];
                int z = block.z() + offset[2];
                long key = key(x, y, z);
                if (leaves.contains(key) && distances.putIfAbsent(key, 1) == null) {
                    queue.add(new int[]{x, y, z, 1});
                }
            }
        }

        while (!queue.isEmpty()) {
            int[] current = queue.poll();
            int distance = current[3];
            if (distance >= MAX_SUPPORTED_DISTANCE) {
                continue;
            }
            for (int[] offset : OFFSETS) {
                int x = current[0] + offset[0];
                int y = current[1] + offset[1];
                int z = current[2] + offset[2];
                long key = key(x, y, z);
                if (leaves.contains(key) && distances.putIfAbsent(key, distance + 1) == null) {
                    queue.add(new int[]{x, y, z, distance + 1});
                }
            }
        }
        return distances;
    }

    /** Position key matching {@code BlockPos.asLong}: 26 bits X/Z, 12 bits Y. */
    public static long key(int x, int y, int z) {
        return ((long) (x & 0x3FFFFFF) << 38)
                | ((long) (z & 0x3FFFFFF) << 12)
                | (y & 0xFFFL);
    }
}
