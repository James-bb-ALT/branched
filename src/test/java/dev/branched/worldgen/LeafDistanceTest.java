package dev.branched.worldgen;

import dev.branched.core.ResourceId;
import dev.branched.morphology.BlockPlacement;
import dev.branched.morphology.PlacementRole;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeafDistanceTest {
    private static final ResourceId LOG = ResourceId.parse("minecraft:oak_log");
    private static final ResourceId LEAVES = ResourceId.parse("minecraft:oak_leaves");

    private static BlockPlacement wood(int x, int y, int z) {
        return new BlockPlacement(x, y, z, LOG, PlacementRole.LOG);
    }

    private static BlockPlacement branch(int x, int y, int z) {
        return new BlockPlacement(x, y, z, ResourceId.parse("branched:oak_branch"),
                PlacementRole.BRANCH);
    }

    private static BlockPlacement leaf(int x, int y, int z) {
        return new BlockPlacement(x, y, z, LEAVES, PlacementRole.LEAVES);
    }

    @Test
    void keyMatchesVanillaBlockPosPacking() {
        int[][] samples = {
                {0, 0, 0},
                {1, 2, 3},
                {-1, -64, -1},
                {29_999_999, 320, -29_999_999},
                {-29_999_999, -64, 29_999_999}
        };
        for (int[] sample : samples) {
            assertEquals(BlockPos.asLong(sample[0], sample[1], sample[2]),
                    LeafDistance.key(sample[0], sample[1], sample[2]),
                    "packing mismatch for " + sample[0] + "," + sample[1] + "," + sample[2]);
        }
    }

    @Test
    void leafNextToLogHasDistanceOne() {
        Map<Long, Integer> distances = LeafDistance.distances(List.of(
                wood(0, 0, 0), leaf(1, 0, 0)));
        assertEquals(1, distances.get(LeafDistance.key(1, 0, 0)));
    }

    @Test
    void branchBlocksSupportLeavesToo() {
        Map<Long, Integer> distances = LeafDistance.distances(List.of(
                wood(0, 0, 0), branch(2, 0, 0), leaf(3, 0, 0)));
        assertEquals(1, distances.get(LeafDistance.key(3, 0, 0)));
    }

    @Test
    void distanceIsShortestPathAroundCorners() {
        // Wood below the first leaf; the second leaf is diagonally adjacent to
        // wood but only one orthogonal step away through the first leaf.
        Map<Long, Integer> distances = LeafDistance.distances(List.of(
                wood(0, 0, 0),
                leaf(0, 1, 0),
                leaf(1, 1, 0),
                leaf(1, 1, 1)));
        assertEquals(1, distances.get(LeafDistance.key(0, 1, 0)));
        assertEquals(2, distances.get(LeafDistance.key(1, 1, 0)));
        assertEquals(3, distances.get(LeafDistance.key(1, 1, 1)));
    }

    @Test
    void chainBeyondSixIsOmitted() {
        List<BlockPlacement> blocks = new ArrayList<>();
        blocks.add(wood(0, 0, 0));
        for (int x = 1; x <= 7; x++) {
            blocks.add(leaf(x, 0, 0));
        }
        Map<Long, Integer> distances = LeafDistance.distances(blocks);
        for (int x = 1; x <= LeafDistance.MAX_SUPPORTED_DISTANCE; x++) {
            assertEquals(x, distances.get(LeafDistance.key(x, 0, 0)), "distance at x=" + x);
        }
        assertNull(distances.get(LeafDistance.key(7, 0, 0)),
                "distance 7 leaves would decay and must not be written");
    }

    @Test
    void leafClusterSeparatedByAirIsOmitted() {
        Map<Long, Integer> distances = LeafDistance.distances(List.of(
                wood(0, 0, 0), leaf(4, 0, 0)));
        assertTrue(distances.isEmpty());
    }

    @Test
    void onlyLeavesProducesNoDistances() {
        Map<Long, Integer> distances = LeafDistance.distances(List.of(
                leaf(0, 0, 0), leaf(1, 0, 0)));
        assertTrue(distances.isEmpty());
    }

    @Test
    void noLeavesProducesNoDistances() {
        assertTrue(LeafDistance.distances(List.of(wood(0, 0, 0))).isEmpty());
        assertTrue(LeafDistance.distances(List.of()).isEmpty());
        assertFalse(LeafDistance.distances(List.of(leaf(9, 9, 9))).containsKey(
                LeafDistance.key(9, 9, 9)));
    }
}
