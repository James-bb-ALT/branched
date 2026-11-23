package dev.branched.worldgen;

import dev.branched.morphology.RasterizedTree;

/**
 * Final pipeline stage: validates and writes a rasterized tree through the
 * worldgen writer. Implementations must not retain generator state in the world.
 */
public interface TreePlacement {
    PlacementResult attempt(RasterizedTree tree, PlacementContext context);
}
