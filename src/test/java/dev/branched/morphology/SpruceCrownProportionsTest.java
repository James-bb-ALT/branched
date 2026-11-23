package dev.branched.morphology;

import dev.branched.core.ResourceId;
import dev.branched.profile.ProfileParser;
import dev.branched.species.SpeciesProfile;
import dev.branched.worldgen.LeafDistance;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the crown proportions of the shipped spruce profile.
 *
 * <p>The first Phase 1 pilot generated about 26 leaves against 15 logs per tree
 * (roughly 1.2 leaves per log), while vanilla taiga trees carry about 8. The
 * fix was a height-proportional crown and a much denser leaf budget. These
 * bounds keep that regression from coming back without pinning exact counts,
 * which would freeze legitimate morphology tuning.</p>
 */
class SpruceCrownProportionsTest {
    private static final ResourceId DIMENSION = ResourceId.parse("minecraft:overworld");
    private static final int SAMPLE_TREES = 60;

    private static SpeciesProfile spruce() throws IOException {
        return new ProfileParser()
                .parseDirectory(Path.of("src/main/resources/data/branched/arboreal/species"))
                .profiles().stream()
                .filter(profile -> profile.id().equals(ResourceId.parse("minecraft:spruce")))
                .findFirst()
                .orElseThrow(() -> new AssertionError("minecraft:spruce profile is missing"));
    }

    @Test
    void spruceCrownsCarryEnoughLeavesForTheirWood() throws IOException {
        SpeciesProfile profile = spruce();
        SemiproceduralMorphologyGenerator generator = new SemiproceduralMorphologyGenerator();
        ReferenceBlockRasterizer rasterizer = new ReferenceBlockRasterizer();
        BlockPalette palette = new BlockPalette(profile.log(),
                ResourceId.parse("branched:spruce_branch"), profile.leaves(),
                profile.sapling().orElse(null), null);

        long logs = 0;
        long leaves = 0;
        long supported = 0;

        for (long seed = 0; seed < SAMPLE_TREES; seed++) {
            TreeSkeleton skeleton = generator.generate(
                    new MorphologyRequest(seed, DIMENSION, 0, 64, 0, 0, profile));
            RasterizedTree tree = rasterizer.rasterize(skeleton, profile, palette, 0, 64, 0);
            Map<Long, Integer> distances = LeafDistance.distances(tree.blocks());
            for (BlockPlacement block : tree.blocks()) {
                if (block.role() == PlacementRole.LOG) {
                    logs++;
                } else if (block.role() == PlacementRole.LEAVES) {
                    leaves++;
                    if (distances.containsKey(
                            LeafDistance.key(block.x(), block.y(), block.z()))) {
                        supported++;
                    }
                }
            }
        }

        double leavesPerTree = leaves / (double) SAMPLE_TREES;
        double logsPerTree = logs / (double) SAMPLE_TREES;
        double leavesPerLog = leaves / (double) Math.max(1L, logs);
        double retention = supported / (double) Math.max(1L, leaves);

        // Vanilla spruce foliage treats ~8 leaves per log as normal; a static
        // tree with decorative branches sits a bit lower but must stay dense.
        System.out.println("spruce crown sample: leaves/tree=" + leavesPerTree
                + " logs/tree=" + logsPerTree
                + " leaves/log=" + leavesPerLog
                + " supported=" + retention);
        assertTrue(leavesPerTree >= 45.0D && leavesPerTree <= 170.0D,
                "leaves per tree out of range: " + leavesPerTree);
        assertTrue(logsPerTree >= 5.0D && logsPerTree <= 16.0D,
                "logs per tree out of range: " + logsPerTree);
        assertTrue(leavesPerLog >= 3.5D && leavesPerLog <= 18.0D,
                "leaves per log out of range: " + leavesPerLog);
        assertTrue(retention >= 0.85D,
                "too many leaves fall outside the support distance: " + retention);
    }

    @Test
    void trunkHeightStaysWithinTheVanillaTaigaRange() throws IOException {
        SpeciesProfile profile = spruce();
        assertEquals(5.0D, profile.trunkHeight().min());
        assertEquals(9.0D, profile.trunkHeight().max());
    }
}
