package dev.branched.morphology;

import dev.branched.core.ResourceId;

/** One block in the rasterized tree, still world-independent. */
public record BlockPlacement(int x, int y, int z, ResourceId block, PlacementRole role) {
    public BlockPlacement {
        if (block == null || role == null) {
            throw new IllegalArgumentException("block and role are required");
        }
    }
}
