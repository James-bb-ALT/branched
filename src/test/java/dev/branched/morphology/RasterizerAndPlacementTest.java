package dev.branched.morphology;

import dev.branched.core.ResourceId;
import dev.branched.species.SpeciesProfile;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RasterizerAndPlacementTest {
    private final SpeciesProfile oak = SpeciesProfile.builder(ResourceId.parse("minecraft:oak"))
            .log(ResourceId.parse("minecraft:oak_log"))
            .leaves(ResourceId.parse("minecraft:oak_leaves"))
            .trunkHeight(new DoubleRange(5, 8))
            .primaryBranches(new IntRange(4, 7))
            .crownClusters(new IntRange(6, 10))
            .maxSegments(96)
            .maxLeaves(200)
            .maxQuads(3000)
            .build();

    private TreeSkeleton skeleton() {
        return new SemiproceduralMorphologyGenerator().generate(new MorphologyRequest(
                1L, ResourceId.parse("minecraft:overworld"), 0, 64, 0, 0L, oak));
    }

    @Test
    void rasterizesSkeletonIntoBoundedBlocks() {
        BlockPalette palette = new BlockPalette(
                ResourceId.parse("minecraft:oak_log"),
                ResourceId.parse("branched:oak_branch"),
                ResourceId.parse("minecraft:oak_leaves"),
                ResourceId.parse("minecraft:oak_sapling"),
                null);
        RasterizedTree tree = new ReferenceBlockRasterizer().rasterize(skeleton(), oak, palette);
        assertTrue(tree.logBlocks() > 0);
        assertTrue(tree.leafBlocks() > 0);
        assertTrue(tree.totalBlocks() <= oak.maxQuads() / 6);
        assertTrue(tree.estimatedQuads() <= oak.maxQuads());
        assertFalse(tree.blocks().isEmpty());
    }

    @Test
    void placementGuardAcceptsGroundedTree() {
        RasterizedTree tree = new ReferenceBlockRasterizer().rasterize(
                skeleton(), oak,
                new BlockPalette(ResourceId.parse("minecraft:oak_log"),
                        ResourceId.parse("branched:oak_branch"),
                        ResourceId.parse("minecraft:oak_leaves"),
                        ResourceId.parse("minecraft:oak_sapling"), null), 0, 64, 0);
        var result = new dev.branched.worldgen.GuardedTreePlacement().attempt(tree, context(-64, 320, true));
        assertEquals(dev.branched.worldgen.PlacementResult.Outcome.PLACED, result.outcome());
        assertEquals(tree.totalBlocks(), result.placedBlocks());
    }

    @Test
    void placementGuardRejectsTreeAboveBuildLimit() {
        RasterizedTree tree = new ReferenceBlockRasterizer().rasterize(
                skeleton(), oak,
                new BlockPalette(ResourceId.parse("minecraft:oak_log"),
                        ResourceId.parse("branched:oak_branch"),
                        ResourceId.parse("minecraft:oak_leaves"),
                        ResourceId.parse("minecraft:oak_sapling"), null), 0, 64, 0);
        var result = new dev.branched.worldgen.GuardedTreePlacement().attempt(tree, context(-64, 60, true));
        assertEquals(dev.branched.worldgen.PlacementResult.Outcome.REJECTED, result.outcome());
        assertEquals("vertical_bounds", result.reason());
    }

    @Test
    void placementGuardRejectsTreeWithoutGround() {
        RasterizedTree tree = new ReferenceBlockRasterizer().rasterize(
                skeleton(), oak,
                new BlockPalette(ResourceId.parse("minecraft:oak_log"),
                        ResourceId.parse("branched:oak_branch"),
                        ResourceId.parse("minecraft:oak_leaves"),
                        ResourceId.parse("minecraft:oak_sapling"), null));
        var result = new dev.branched.worldgen.GuardedTreePlacement().attempt(tree, context(-64, 320, false));
        assertEquals(dev.branched.worldgen.PlacementResult.Outcome.REJECTED, result.outcome());
        assertEquals("no_ground", result.reason());
    }

    private static dev.branched.worldgen.PlacementContext context(int minY, int maxY, boolean ground) {
        return new dev.branched.worldgen.PlacementContext() {
            @Override
            public long worldSeed() { return 1L; }

            @Override
            public ResourceId dimension() { return ResourceId.parse("minecraft:overworld"); }

            @Override
            public int minBuildHeight() { return minY; }

            @Override
            public int maxBuildHeight() { return maxY; }

            @Override
            public boolean isAir(int x, int y, int z) { return true; }

            @Override
            public boolean isReplaceable(int x, int y, int z) { return true; }

            @Override
            public boolean hasSolidGround(int x, int z) { return ground; }

            @Override
            public ResourceId blockAt(int x, int y, int z) { return null; }
        };
    }
}
