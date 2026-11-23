package dev.branched.morphology;

import dev.branched.core.ResourceId;
import dev.branched.species.SpeciesProfile;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MorphologyDeterminismTest {
    private final SemiproceduralMorphologyGenerator generator = new SemiproceduralMorphologyGenerator();
    private final SpeciesProfile oak = SpeciesProfile.builder(ResourceId.parse("minecraft:oak"))
            .log(ResourceId.parse("minecraft:oak_log"))
            .leaves(ResourceId.parse("minecraft:oak_leaves"))
            .trunkHeight(new DoubleRange(6, 10))
            .primaryBranches(new IntRange(4, 9))
            .crownClusters(new IntRange(6, 14))
            .maxSegments(128)
            .maxLeaves(420)
            .maxQuads(9000)
            .build();

    private MorphologyRequest request(long seed, int x, int z, SpeciesProfile profile) {
        return new MorphologyRequest(seed, ResourceId.parse("minecraft:overworld"), x, 64, z, 0L, profile);
    }

    @Test
    void sameRequestAlwaysProducesSameTopology() {
        MorphologyRequest request = request(123456789L, 128, -512, oak);
        TreeSkeleton first = generator.generate(request);
        TreeSkeleton second = generator.generate(request);
        assertEquals(first.topologySignature(), second.topologySignature());
        assertEquals(first.segments().size(), second.segments().size());
    }

    @Test
    void positionChangesTopology() {
        TreeSkeleton a = generator.generate(request(42L, 0, 0, oak));
        TreeSkeleton b = generator.generate(request(42L, 1, 0, oak));
        assertNotEquals(a.topologySignature(), b.topologySignature());
    }

    @Test
    void worldSeedChangesTopology() {
        TreeSkeleton a = generator.generate(request(1L, 10, 10, oak));
        TreeSkeleton b = generator.generate(request(2L, 10, 10, oak));
        assertNotEquals(a.topologySignature(), b.topologySignature());
    }

    @Test
    void generatorIsNotAffectedByCallOrder() {
        MorphologyRequest a = request(7L, 0, 0, oak);
        MorphologyRequest b = request(7L, 32, 32, oak);
        TreeSkeleton firstA = generator.generate(a);
        TreeSkeleton firstB = generator.generate(b);
        TreeSkeleton secondB = generator.generate(b);
        TreeSkeleton secondA = generator.generate(a);
        assertEquals(firstA.topologySignature(), secondA.topologySignature());
        assertEquals(firstB.topologySignature(), secondB.topologySignature());
    }

    @Test
    void producesStructuralDiversity() {
        Set<String> signatures = new HashSet<>();
        for (int i = 0; i < 40; i++) {
            signatures.add(generator.generate(request(99L, i * 17, i * 31, oak)).topologySignature());
        }
        assertTrue(signatures.size() >= 38, "expected varied topologies, got " + signatures.size());
    }

    @Test
    void respectsSegmentAndLeafBudgets() {
        SpeciesProfile small = SpeciesProfile.builder(ResourceId.parse("test:small"))
                .log(ResourceId.parse("minecraft:oak_log"))
                .leaves(ResourceId.parse("minecraft:oak_leaves"))
                .primaryBranches(new IntRange(10, 10))
                .branchDepth(new IntRange(3, 3))
                .crownClusters(new IntRange(30, 30))
                .maxSegments(24)
                .maxLeaves(12)
                .maxQuads(512)
                .build();
        TreeSkeleton skeleton = generator.generate(request(5L, 0, 0, small));
        assertTrue(skeleton.segments().size() <= 24);
        assertTrue(skeleton.leafClusters().size() <= 12);
    }

    @Test
    void noOpMetricsDoesNotFail() {
        assertSame(MetricsRecorderHolder.NOOP, MetricsRecorderHolder.NOOP);
        TreeSkeleton skeleton = generator.generate(request(1L, 0, 0, oak));
        assertTrue(skeleton.nodeCount() > 0);
    }

    private static final class MetricsRecorderHolder {
        static final dev.branched.metrics.MetricsRecorder NOOP =
                dev.branched.metrics.MetricsRecorder.NOOP;
    }
}
