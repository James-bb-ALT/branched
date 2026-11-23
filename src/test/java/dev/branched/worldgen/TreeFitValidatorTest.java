package dev.branched.worldgen;

import dev.branched.core.ResourceId;
import dev.branched.morphology.BlockPlacement;
import dev.branched.morphology.PlacementRole;
import dev.branched.morphology.RasterizedTree;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TreeFitValidatorTest {
    private static final ResourceId LOG = ResourceId.parse("minecraft:oak_log");
    private static final ResourceId LEAVES = ResourceId.parse("minecraft:oak_leaves");

    private final FakeContext context = new FakeContext();

    private static long key(int x, int y, int z) {
        return LeafDistance.key(x, y, z);
    }

    private static RasterizedTree tree(int baseY, int height) {
        List<BlockPlacement> blocks = new ArrayList<>();
        for (int i = 0; i < height; i++) {
            blocks.add(new BlockPlacement(0, baseY + i, 0, LOG, PlacementRole.LOG));
        }
        blocks.add(new BlockPlacement(1, baseY + height - 1, 0, LEAVES, PlacementRole.LEAVES));
        return new RasterizedTree(blocks, height, 1, height * 6 + 6, false);
    }

    private static RasterizedTree treeWithBranch() {
        List<BlockPlacement> blocks = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            blocks.add(new BlockPlacement(0, i, 0, LOG, PlacementRole.LOG));
        }
        blocks.add(new BlockPlacement(1, 1, 0,
                ResourceId.parse("branched:oak_branch"), PlacementRole.BRANCH));
        blocks.add(new BlockPlacement(2, 1, 0, LEAVES, PlacementRole.LEAVES));
        return new RasterizedTree(blocks, 3, 2, 3 * 6 + 12, false);
    }

    private static void allowAir(FakeContext context, RasterizedTree tree) {
        for (BlockPlacement block : tree.blocks()) {
            context.air.add(key(block.x(), block.y(), block.z()));
        }
    }

    @Test
    void acceptsFullyValidTreeWithoutTouchingTheWorld() {
        RasterizedTree tree = tree(0, 3);
        allowAir(context, tree);
        context.ground.add(key(0, -1, 0));

        PlacementResult result = TreeFitValidator.validate(tree, context);

        assertEquals(PlacementResult.Outcome.PLACED, result.outcome());
        assertEquals(tree.totalBlocks(), result.placedBlocks());
    }

    @Test
    void rejectsEmptyTree() {
        PlacementResult result = TreeFitValidator.validate(
                new RasterizedTree(List.of(), 0, 0, 0, false), context);
        assertEquals(PlacementResult.Outcome.REJECTED, result.outcome());
        assertEquals("empty_tree", result.reason());
    }

    @Test
    void rejectsTreeWithoutLogBase() {
        RasterizedTree leavesOnly = new RasterizedTree(
                List.of(new BlockPlacement(0, 0, 0, LEAVES, PlacementRole.LEAVES)),
                0, 1, 6, false);
        allowAir(context, leavesOnly);
        context.ground.add(key(0, -1, 0));

        assertEquals("no_base", TreeFitValidator.validate(leavesOnly, context).reason());
    }

    @Test
    void rejectsPositionOutsideWriteRegion() {
        RasterizedTree tree = tree(0, 3);
        allowAir(context, tree);
        context.ground.add(key(0, -1, 0));
        context.unwritable.add(key(1, 2, 0));

        assertEquals("outside_write_region",
                TreeFitValidator.validate(tree, context).reason());
    }

    @Test
    void rejectsTreeBelowBuildHeight() {
        RasterizedTree tree = tree(0, 3);
        allowAir(context, tree);
        context.ground.add(key(0, -1, 0));
        context.minY = 1;

        assertEquals("vertical_bounds", TreeFitValidator.validate(tree, context).reason());
    }

    @Test
    void rejectsTreeAboveBuildHeight() {
        RasterizedTree tree = tree(0, 3);
        allowAir(context, tree);
        context.ground.add(key(0, -1, 0));
        context.maxY = 2;

        assertEquals("vertical_bounds", TreeFitValidator.validate(tree, context).reason());
    }

    @Test
    void rejectsTreeWithoutGround() {
        RasterizedTree tree = tree(0, 3);
        allowAir(context, tree);

        assertEquals("no_ground", TreeFitValidator.validate(tree, context).reason());
    }

    @Test
    void rejectsObstructedStructuralBlock() {
        RasterizedTree tree = tree(0, 3);
        allowAir(context, tree);
        context.ground.add(key(0, -1, 0));
        // A trunk position occupied by a foreign block fails the whole tree.
        context.air.remove(key(0, 1, 0));

        assertEquals("blocked", TreeFitValidator.validate(tree, context).reason());
    }

    @Test
    void obstructedBranchesAreSoftlySkipped() {
        RasterizedTree tree = treeWithBranch();
        allowAir(context, tree);
        context.ground.add(key(0, -1, 0));
        // A branch inside terrain is omitted at write time, not fatal.
        context.air.remove(key(1, 1, 0));

        assertEquals(PlacementResult.Outcome.PLACED,
                TreeFitValidator.validate(tree, context).outcome());
    }

    @Test
    void obstructedLeavesAreSoftlySkipped() {
        RasterizedTree tree = tree(0, 3);
        allowAir(context, tree);
        context.ground.add(key(0, -1, 0));
        // A leaf inside terrain is omitted at write time, not fatal.
        context.air.remove(key(1, 2, 0));

        assertEquals(PlacementResult.Outcome.PLACED,
                TreeFitValidator.validate(tree, context).outcome());
    }

    @Test
    void acceptsReplaceableObstruction() {
        RasterizedTree tree = tree(0, 3);
        allowAir(context, tree);
        context.ground.add(key(0, -1, 0));
        // A replaceable block (snow layer, tall grass) does not block structure.
        context.air.remove(key(0, 1, 0));
        context.replaceable.add(key(0, 1, 0));

        assertEquals(PlacementResult.Outcome.PLACED,
                TreeFitValidator.validate(tree, context).outcome());
    }

    private static final class FakeContext implements PlacementContext {
        final Set<Long> air = new HashSet<>();
        final Set<Long> replaceable = new HashSet<>();
        final Set<Long> ground = new HashSet<>();
        final Set<Long> unwritable = new HashSet<>();
        int minY = -64;
        int maxY = 320;

        @Override
        public long worldSeed() {
            return 42L;
        }

        @Override
        public ResourceId dimension() {
            return ResourceId.parse("minecraft:overworld");
        }

        @Override
        public int minBuildHeight() {
            return minY;
        }

        @Override
        public int maxBuildHeight() {
            return maxY;
        }

        @Override
        public boolean isAir(int x, int y, int z) {
            return air.contains(key(x, y, z));
        }

        @Override
        public boolean isReplaceable(int x, int y, int z) {
            return replaceable.contains(key(x, y, z));
        }

        @Override
        public boolean hasSolidGround(int x, int z) {
            return true;
        }

        @Override
        public ResourceId blockAt(int x, int y, int z) {
            return ResourceId.parse("minecraft:air");
        }

        @Override
        public boolean canWrite(int x, int y, int z) {
            return !unwritable.contains(key(x, y, z));
        }

        @Override
        public boolean isGround(int x, int y, int z) {
            return ground.contains(key(x, y, z));
        }
    }
}
