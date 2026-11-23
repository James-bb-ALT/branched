package dev.branched.worldgen;

import dev.branched.morphology.BlockPlacement;
import dev.branched.morphology.PlacementRole;
import dev.branched.morphology.RasterizedTree;

/**
 * Validates a rasterized tree before the first write happens.
 *
 * <p>The trunk is all-or-nothing: if any trunk position is occupied by a
 * non-replaceable block, the whole tree is rejected and the world stays
 * untouched. The write-radius and build-height checks still cover every block,
 * because the writer must never attempt a write outside the loaded region.</p>
 *
 * <p>Branches and leaves are soft, matching vanilla tree features: a branch or
 * leaf that cannot be placed is omitted individually by the writer instead of
 * rejecting the tree, which keeps hillside taiga from turning into bare
 * patches.</p>
 */
public final class TreeFitValidator {
    private TreeFitValidator() {
    }

    public static PlacementResult validate(RasterizedTree tree, PlacementContext context) {
        if (tree.blocks().isEmpty()) {
            return PlacementResult.rejected("empty_tree");
        }

        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        BlockPlacement base = null;
        for (BlockPlacement block : tree.blocks()) {
            minY = Math.min(minY, block.y());
            maxY = Math.max(maxY, block.y());
            if (block.role() == PlacementRole.LOG
                    && (base == null || block.y() < base.y())) {
                base = block;
            }
            if (!context.canWrite(block.x(), block.y(), block.z())) {
                return PlacementResult.rejected("outside_write_region");
            }
        }

        if (minY < context.minBuildHeight() || maxY >= context.maxBuildHeight()) {
            return PlacementResult.rejected("vertical_bounds");
        }
        if (base == null) {
            return PlacementResult.rejected("no_base");
        }
        if (!context.isGround(base.x(), base.y() - 1, base.z())) {
            return PlacementResult.rejected("no_ground");
        }

        for (BlockPlacement block : tree.blocks()) {
            if (block.role() != PlacementRole.LOG) {
                continue;
            }
            if (!context.isAir(block.x(), block.y(), block.z())
                    && !context.isReplaceable(block.x(), block.y(), block.z())) {
                return PlacementResult.rejected("blocked");
            }
        }
        return PlacementResult.placed(tree.totalBlocks());
    }
}
