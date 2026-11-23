package dev.branched.worldgen;

import dev.branched.morphology.BlockPlacement;
import dev.branched.morphology.RasterizedTree;

/**
 * Conservative placement guard used before a world writer exists. It verifies
 * vertical bounds and ground support; it does not mutate the world.
 */
public final class GuardedTreePlacement implements TreePlacement {
    @Override
    public PlacementResult attempt(RasterizedTree tree, PlacementContext context) {
        if (tree.blocks().isEmpty()) {
            return PlacementResult.rejected("empty_tree");
        }
        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (BlockPlacement block : tree.blocks()) {
            minY = Math.min(minY, block.y());
            maxY = Math.max(maxY, block.y());
            minX = Math.min(minX, block.x());
            maxX = Math.max(maxX, block.x());
            minZ = Math.min(minZ, block.z());
            maxZ = Math.max(maxZ, block.z());
        }
        if (minY < context.minBuildHeight() || maxY >= context.maxBuildHeight()) {
            return PlacementResult.rejected("vertical_bounds");
        }
        if ((maxX - minX) > 64 || (maxZ - minZ) > 64) {
            return PlacementResult.rejected("horizontal_bounds");
        }
        if (!context.hasSolidGround((minX + maxX) / 2, (minZ + maxZ) / 2)) {
            return PlacementResult.rejected("no_ground");
        }
        return PlacementResult.placed(tree.totalBlocks());
    }
}
