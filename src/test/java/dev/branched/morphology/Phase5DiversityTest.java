package dev.branched.morphology;

import dev.branched.core.ResourceId;
import dev.branched.profile.ProfileParser;
import dev.branched.species.SpeciesProfile;
import dev.branched.worldgen.BranchMaterials;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 5 gates: structural diversity across seeds, geometry culling and the
 * {@code rendering.preset} contract.
 *
 * <p>Diversity is measured with rotation/translation/scale-invariant
 * signatures; the generator must not repeat a silhouette within a species.
 * Samples are synthetic (no world, no Minecraft bootstrap) but use the exact
 * worldgen pipeline.</p>
 */
class Phase5DiversityTest {
    private static final Path PROFILE_ROOT =
            Path.of("src/main/resources/data/branched/arboreal/species");
    private static final int SAMPLES = 32;
    private static final ResourceId OVERWORLD = ResourceId.parse("minecraft:overworld");

    private final SemiproceduralMorphologyGenerator generator =
            new SemiproceduralMorphologyGenerator();
    private final ReferenceBlockRasterizer rasterizer = new ReferenceBlockRasterizer();

    @Test
    void everyReplaceableSpeciesHasNoNormalizedDuplicateAcrossSeeds() throws IOException {
        for (SpeciesProfile profile : replaceableProfiles()) {
            Set<String> scaled = new HashSet<>();
            for (int i = 0; i < SAMPLES; i++) {
                scaled.add(CanonicalShape.scaled(rasterize(profile, i,
                        PerformancePreset.BALANCED).blocks()));
            }
            double duplicateRate = 1.0D - scaled.size() / (double) SAMPLES;
            assertTrue(duplicateRate <= 0.10D,
                    profile.id() + " repeats a normalized silhouette: " + duplicateRate);
        }
    }

    @Test
    void everyReplaceableSpeciesHasNoLiteralDuplicate() throws IOException {
        for (SpeciesProfile profile : replaceableProfiles()) {
            Set<String> exact = new HashSet<>();
            for (int i = 0; i < SAMPLES; i++) {
                exact.add(CanonicalShape.exact(rasterize(profile, i,
                        PerformancePreset.BALANCED).blocks()));
            }
            assertEquals(SAMPLES, exact.size(), profile.id() + " produced identical voxels");
        }
    }

    @Test
    void presetsScaleLeafGeometryMonotonically() throws IOException {
        for (String id : List.of("minecraft:oak", "minecraft:spruce",
                "regions_unexplored:maple", "twilightforest:canopy")) {
            SpeciesProfile profile = profile(id);
            double performance = meanLeaves(profile, PerformancePreset.PERFORMANCE);
            double balanced = meanLeaves(profile, PerformancePreset.BALANCED);
            double visual = meanLeaves(profile, PerformancePreset.VISUAL);
            assertTrue(performance < balanced,
                    id + " performance preset must thin the crown: " + performance + " vs " + balanced);
            assertTrue(balanced < visual,
                    id + " visual preset must densify the crown: " + balanced + " vs " + visual);
        }
    }

    @Test
    void balancedPresetReproducesTheUnconfiguredGeometry() throws IOException {
        for (SpeciesProfile profile : replaceableProfiles()) {
            RasterizedTree defaultTree = rasterize(profile, 3, PerformancePreset.BALANCED);
            RasterizedTree explicit = rasterizer.rasterize(skeleton(profile, 3), profile,
                    palette(profile), 0, 64, 0, PerformancePreset.BALANCED);
            RasterizedTree legacy = rasterizer.rasterize(skeleton(profile, 3), profile,
                    palette(profile), 0, 64, 0);
            assertEquals(explicit.blocks(), legacy.blocks(), profile.id() + " preset overload drifted");
            assertEquals(defaultTree.estimatedQuads(), legacy.estimatedQuads(),
                    profile.id() + " default quads drifted");
        }
    }

    @Test
    void exposedQuadsStayBelowTheNaiveSixFacesBound() throws IOException {
        for (SpeciesProfile profile : replaceableProfiles()) {
            for (int i = 0; i < 8; i++) {
                RasterizedTree tree = rasterize(profile, i, PerformancePreset.BALANCED);
                assertTrue(tree.estimatedQuads() <= tree.totalBlocks() * 6,
                        profile.id() + " reports more faces than a naive bound");
                assertTrue(tree.estimatedQuads() >= tree.totalBlocks(),
                        profile.id() + " reports fewer faces than one per block");
            }
        }
    }

    @Test
    void canonicalSignaturesAreTranslationAndRotationInvariant() {
        List<BlockPlacement> shape = List.of(
                block(0, 0, 0, PlacementRole.LOG),
                block(0, 1, 0, PlacementRole.LOG),
                block(1, 1, 0, PlacementRole.BRANCH),
                block(2, 1, 0, PlacementRole.LEAVES));

        List<BlockPlacement> translated = new ArrayList<>();
        for (BlockPlacement block : shape) {
            translated.add(block(block.x() + 11, block.y() + 5, block.z() - 7, block.role()));
        }
        assertEquals(CanonicalShape.exact(shape), CanonicalShape.exact(translated),
                "translation must not change the exact signature");

        List<BlockPlacement> rotated = new ArrayList<>();
        for (BlockPlacement block : shape) {
            // one quarter turn: (x,z) -> (z,-x), then translate by (+11,+5,-7)
            rotated.add(block(block.z() + 11, block.y() + 5, -block.x() - 7, block.role()));
        }
        assertEquals(CanonicalShape.rotational(shape), CanonicalShape.rotational(rotated),
                "rotation must not change the rotational signature");
        assertEquals(CanonicalShape.scaled(shape), CanonicalShape.scaled(rotated),
                "rotation must not change the scaled signature");

        List<BlockPlacement> altered = new ArrayList<>(shape.subList(0, 3));
        altered.add(block(2, 2, 0, PlacementRole.LEAVES));
        assertNotEquals(CanonicalShape.rotational(shape), CanonicalShape.rotational(altered),
                "a moved block must produce a different signature");
    }

    @Test
    void everyGeneratedBranchIsStrictlySixConnectedToTrunk() throws IOException {
        for (SpeciesProfile profile : replaceableProfiles()) {
            for (int i = 0; i < 12; i++) {
                RasterizedTree tree = rasterize(profile, i, PerformancePreset.BALANCED);
                Set<Long> trunks = new HashSet<>();
                Set<Long> branches = new HashSet<>();
                java.util.ArrayDeque<long[]> queue = new java.util.ArrayDeque<>();
                for (BlockPlacement b : tree.blocks()) {
                    long k = pack(b.x(), b.y(), b.z());
                    if (b.role() == PlacementRole.LOG) {
                        trunks.add(k);
                        queue.add(new long[]{b.x(), b.y(), b.z()});
                    } else if (b.role() == PlacementRole.BRANCH) {
                        branches.add(k);
                    }
                }
                Set<Long> reached = new HashSet<>(trunks);
                int[][] dirs = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
                while (!queue.isEmpty()) {
                    long[] p = queue.poll();
                    for (int[] d : dirs) {
                        long nx = p[0] + d[0];
                        long ny = p[1] + d[1];
                        long nz = p[2] + d[2];
                        long nk = pack((int) nx, (int) ny, (int) nz);
                        if (branches.contains(nk) && reached.add(nk)) {
                            queue.add(new long[]{nx, ny, nz});
                        }
                    }
                }
                for (long bk : branches) {
                    assertTrue(reached.contains(bk),
                            profile.id() + " seed=" + i + " generated disconnected/diagonal-only branch voxel");
                }
            }
        }
    }

    @Test
    void everyLivingTreeHasZeroBareBranchTops() throws IOException {
        for (SpeciesProfile profile : replaceableProfiles()) {
            String path = profile.id().path();
            if ("dead".equals(path) || "dead_pine".equals(path)) {
                continue;
            }
            for (int i = 0; i < 12; i++) {
                RasterizedTree tree = rasterize(profile, i, PerformancePreset.BALANCED);
                Set<Long> trunks = new HashSet<>();
                Set<Long> occupiedAll = new HashSet<>();
                int maxTrunkY = Integer.MIN_VALUE;
                int topTrunkX = 0;
                int topTrunkZ = 0;
                for (BlockPlacement b : tree.blocks()) {
                    occupiedAll.add(pack(b.x(), b.y(), b.z()));
                    if (b.role() == PlacementRole.LOG) {
                        trunks.add(pack(b.x(), b.y(), b.z()));
                        if (b.y() > maxTrunkY) {
                            maxTrunkY = b.y();
                            topTrunkX = b.x();
                            topTrunkZ = b.z();
                        }
                    }
                }
                assertTrue(occupiedAll.contains(pack(topTrunkX, maxTrunkY + 1, topTrunkZ)),
                        profile.id() + " trunk apex exposed bare at y=" + maxTrunkY);
                for (BlockPlacement b : tree.blocks()) {
                    if (b.role() != PlacementRole.BRANCH) {
                        continue;
                    }
                    // Only allow an uncovered top if it's directly beneath an upper parallel trunk collar stub
                    if (occupiedAll.contains(pack(b.x(), b.y() + 2, b.z()))) {
                        continue;
                    }
                    assertTrue(occupiedAll.contains(pack(b.x(), b.y() + 1, b.z())),
                            profile.id() + " seed=" + i + " has bare branch top at ("
                                    + b.x() + "," + b.y() + "," + b.z() + ")");
                }
            }
        }
    }

    @Test
    void noInnerBranchHasSingleHangingLeafUnderneath() throws IOException {
        int[][] dirs = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
        int[][] card = {{1,0,0},{-1,0,0},{0,0,1},{0,0,-1}};
        for (SpeciesProfile profile : replaceableProfiles()) {
            if ("dead".equals(profile.id().path()) || "dead_pine".equals(profile.id().path())) {
                continue;
            }
            for (int i = 0; i < 10; i++) {
                RasterizedTree tree = rasterize(profile, i, PerformancePreset.BALANCED);
                Set<Long> trunks = new HashSet<>();
                Set<Long> branches = new HashSet<>();
                Set<Long> leaves = new HashSet<>();
                for (BlockPlacement b : tree.blocks()) {
                    long k = pack(b.x(), b.y(), b.z());
                    if (b.role() == PlacementRole.LOG) trunks.add(k);
                    else if (b.role() == PlacementRole.BRANCH) branches.add(k);
                    else if (b.role() == PlacementRole.LEAVES) leaves.add(k);
                }
                for (BlockPlacement b : tree.blocks()) {
                    if (b.role() != PlacementRole.LEAVES) {
                        continue;
                    }
                    int leafNeighbors = 0;
                    for (int[] d : dirs) {
                        if (leaves.contains(pack(b.x() + d[0], b.y() + d[1], b.z() + d[2]))) {
                            leafNeighbors++;
                        }
                    }
                    assertTrue(leafNeighbors >= 2,
                            profile.id() + " seed=" + i + " has isolated single leaf at ("
                                    + b.x() + "," + b.y() + "," + b.z() + ")");
                    if (branches.contains(pack(b.x(), b.y() + 1, b.z()))) {
                        boolean adjacentTrunkAbove = false;
                        for (int[] c : card) {
                            if (trunks.contains(pack(b.x() + c[0], b.y() + 1, b.z() + c[2]))) {
                                adjacentTrunkAbove = true;
                            }
                        }
                        if (adjacentTrunkAbove) {
                            assertTrue(leafNeighbors >= 3,
                                    profile.id() + " seed=" + i + " has sparse leaf dangling under inner stub");
                        }
                    }
                }
            }
        }
    }

    @Test
    void jungleTreesGenerateLongDiagonalLateralParkourBoughs() throws IOException {
        for (String id : List.of("minecraft:jungle", "regions_unexplored:kapok", "regions_unexplored:eucalyptus")) {
            SpeciesProfile profile = profile(id);
            int qualifiedTrees = 0;
            for (int i = 0; i < 8; i++) {
                RasterizedTree tree = rasterize(profile, i, PerformancePreset.BALANCED);
                int maxLogY = Integer.MIN_VALUE;
                int minX = Integer.MAX_VALUE;
                int minZ = Integer.MAX_VALUE;
                for (BlockPlacement b : tree.blocks()) {
                    if (b.role() == PlacementRole.LOG) {
                        maxLogY = Math.max(maxLogY, b.y());
                        minX = Math.min(minX, Math.abs(b.x()));
                    }
                }
                final double midLimitY = 64.0D + Math.max(4.0D, (maxLogY - 64) * 0.78D);
                long longMidBoughs = tree.blocks().stream()
                        .filter(b -> b.role() == PlacementRole.BRANCH)
                        .filter(b -> b.y() < midLimitY)
                        .filter(b -> Math.hypot(b.x(), b.z()) >= 3.0D)
                        .count();
                if (longMidBoughs >= 2) {
                    qualifiedTrees++;
                }
            }
            assertTrue(qualifiedTrees >= 6,
                    id + " must consistently generate long lateral diagonal parkour boughs along trunk");
        }
    }

    @Test
    void lowBranchedRealLifeSpeciesGenerateSubCanopyLateralBranches() throws IOException {
        List<String> lowBranchedSpecies = List.of(
                "minecraft:jungle", "regions_unexplored:kapok", "regions_unexplored:eucalyptus",
                "minecraft:oak", "minecraft:fancy_oak", "minecraft:dark_oak",
                "minecraft:spruce", "regions_unexplored:pine", "regions_unexplored:larch",
                "regions_unexplored:redwood", "regions_unexplored:maple", "minecraft:cherry",
                "regions_unexplored:willow", "twilightforest:canopy");
        for (String id : lowBranchedSpecies) {
            SpeciesProfile profile = profile(id);
            int lowBranchedTrees = 0;
            for (int i = 0; i < 8; i++) {
                RasterizedTree tree = rasterize(profile, i, PerformancePreset.BALANCED);
                int maxLogY = 64;
                for (BlockPlacement b : tree.blocks()) {
                    if (b.role() == PlacementRole.LOG) {
                        maxLogY = Math.max(maxLogY, b.y());
                    }
                }
                int trunkHeight = Math.max(4, maxLogY - 64);
                long lowCutoffY = 64L + Math.round(trunkHeight * 0.52D);
                boolean hasLowerLateralBranch = tree.blocks().stream()
                        .filter(b -> b.role() == PlacementRole.BRANCH)
                        .anyMatch(b -> b.y() <= lowCutoffY && Math.hypot(b.x(), b.z()) >= 1.5D);
                if (hasLowerLateralBranch) {
                    lowBranchedTrees++;
                }
            }
            assertTrue(lowBranchedTrees >= 6,
                    id + " should consistently produce low sub-canopy lateral branches below the upper crown (got "
                            + lowBranchedTrees + "/8)");
        }
    }

    @Test
    void palmsAndCoconutsNeverGenerateWoodenBranches() throws IOException {
        for (String id : List.of("regions_unexplored:palm", "fishofthieves:coconut",
                "fishofthieves:banana", "ecologics:coconut")) {
            SpeciesProfile profile = profile(id);
            for (int i = 0; i < 8; i++) {
                RasterizedTree tree = rasterize(profile, i, PerformancePreset.BALANCED);
                long branchCount = tree.blocks().stream()
                        .filter(b -> b.role() == PlacementRole.BRANCH)
                        .count();
                assertEquals(0L, branchCount, id + " seed=" + i + " must remain branchless");
            }
        }
    }

    @Test
    void crookedBifurcatingTrunksKeepLogsVerticalAndConvertOffAxisUpperStemsToBranches() throws IOException {
        for (String id : List.of("minecraft:acacia", "minecraft:fancy_oak", "minecraft:dark_oak")) {
            SpeciesProfile profile = profile(id);
            for (int i = 0; i < 8; i++) {
                RasterizedTree tree = rasterize(profile, i, PerformancePreset.BALANCED);
                boolean hasBranch = tree.blocks().stream().anyMatch(b -> b.role() == PlacementRole.BRANCH);
                assertTrue(hasBranch, id + " seed=" + i + " must generate branches");
                int minLogY = tree.blocks().stream()
                        .filter(b -> b.role() == PlacementRole.LOG)
                        .mapToInt(BlockPlacement::y)
                        .min()
                        .orElse(64);
                Set<Long> baseColumns = new HashSet<>();
                for (BlockPlacement block : tree.blocks()) {
                    if (block.role() == PlacementRole.LOG && block.y() == minLogY) {
                        baseColumns.add(pack(block.x(), 0, block.z()));
                        assertTrue(block.x() >= 0 && block.x() <= 1 && block.z() >= 0 && block.z() <= 1,
                                id + " base log out of bole footprint");
                    }
                }
                if (!"minecraft:dark_oak".equals(id)) {
                    assertEquals(1, baseColumns.size(), id + " single-trunk bole must stay 1x1");
                }
                for (BlockPlacement block : tree.blocks()) {
                    if (block.role() == PlacementRole.LOG) {
                        assertTrue(baseColumns.contains(pack(block.x(), 0, block.z())),
                                id + " log drifted off vertical bole at (" + block.x() + "," + block.z() + ")");
                    }
                }
            }
        }
    }

    @Test
    void zeroBranchMeshOrTwoByTwoWoodSquaresAcrossAllSpecies() throws IOException {
        int[][] dirs = {{1,0,0},{-1,0,0},{0,1,0},{0,-1,0},{0,0,1},{0,0,-1}};
        for (SpeciesProfile profile : replaceableProfiles()) {
            for (int i = 0; i < 12; i++) {
                RasterizedTree tree = rasterize(profile, i, PerformancePreset.BALANCED);
                Set<Long> wood = new HashSet<>();
                Set<Long> branches = new HashSet<>();
                for (BlockPlacement b : tree.blocks()) {
                    long k = pack(b.x(), b.y(), b.z());
                    if (b.role() == PlacementRole.LOG) {
                        wood.add(k);
                    } else if (b.role() == PlacementRole.BRANCH) {
                        branches.add(k);
                        wood.add(k);
                    }
                }
                for (BlockPlacement b : tree.blocks()) {
                    if (b.role() != PlacementRole.BRANCH) {
                        continue;
                    }
                    int branchNeighbors = 0;
                    for (int[] d : dirs) {
                        if (branches.contains(pack(b.x() + d[0], b.y() + d[1], b.z() + d[2]))) {
                            branchNeighbors++;
                        }
                    }
                    assertTrue(branchNeighbors <= 3,
                            profile.id() + " seed=" + i + " formed over-connected branch hub ("
                                    + branchNeighbors + " neighbors) at (" + b.x() + "," + b.y() + "," + b.z() + ")");

                    int x = b.x();
                    int y = b.y();
                    int z = b.z();
                    for (int dx : new int[]{-1, 1}) {
                        for (int dz : new int[]{-1, 1}) {
                            assertFalse(wood.contains(pack(x + dx, y, z))
                                            && wood.contains(pack(x, y, z + dz))
                                            && wood.contains(pack(x + dx, y, z + dz)),
                                    profile.id() + " seed=" + i + " formed 2x2 XZ wood mesh around branch at ("
                                            + x + "," + y + "," + z + ")");
                        }
                    }
                    for (int dx : new int[]{-1, 1}) {
                        for (int dy : new int[]{-1, 1}) {
                            assertFalse(wood.contains(pack(x + dx, y, z))
                                            && wood.contains(pack(x, y + dy, z))
                                            && wood.contains(pack(x + dx, y + dy, z)),
                                    profile.id() + " seed=" + i + " formed 2x2 XY wood mesh around branch at ("
                                            + x + "," + y + "," + z + ")");
                        }
                    }
                    for (int dy : new int[]{-1, 1}) {
                        for (int dz : new int[]{-1, 1}) {
                            assertFalse(wood.contains(pack(x, y + dy, z))
                                            && wood.contains(pack(x, y, z + dz))
                                            && wood.contains(pack(x, y + dy, z + dz)),
                                    profile.id() + " seed=" + i + " formed 2x2 YZ wood mesh around branch at ("
                                            + x + "," + y + "," + z + ")");
                        }
                    }
                }
            }
        }
    }

    private static long pack(int x, int y, int z) {
        return ((long) (x & 0x1FFFFF) << 42) | ((long) (y & 0x1FFFFF) << 21) | (z & 0x1FFFFF);
    }

    private double meanLeaves(SpeciesProfile profile, PerformancePreset preset) {
        double leaves = 0.0D;
        for (int i = 0; i < 16; i++) {
            leaves += rasterize(profile, i, preset).leafBlocks();
        }
        return leaves / 16.0D;
    }

    private RasterizedTree rasterize(SpeciesProfile profile, int index, PerformancePreset preset) {
        return rasterizer.rasterize(skeleton(profile, index), profile, palette(profile),
                0, 64, 0, preset);
    }

    private TreeSkeleton skeleton(SpeciesProfile profile, int index) {
        return generator.generate(new MorphologyRequest(
                index * 977L + 11L, OVERWORLD, index * 31, 64, index * -47, 0L, profile));
    }

    private static BlockPalette palette(SpeciesProfile profile) {
        return new BlockPalette(profile.log(), BranchMaterials.resolve(profile.log()),
                profile.leaves(), profile.sapling().orElse(null), null);
    }

    private List<SpeciesProfile> replaceableProfiles() throws IOException {
        return loadProfiles().values().stream()
                .filter(profile -> profile.compatibility().mayReplace())
                .collect(Collectors.toList());
    }

    private SpeciesProfile profile(String id) throws IOException {
        SpeciesProfile profile = loadProfiles().get(id);
        if (profile == null) {
            throw new IllegalStateException("missing profile " + id);
        }
        return profile;
    }

    private static Map<String, SpeciesProfile> loadProfiles() throws IOException {
        return new ProfileParser().parseDirectory(PROFILE_ROOT).profiles().stream()
                .collect(Collectors.toMap(profile -> profile.id().toString(), profile -> profile,
                        (first, second) -> first, java.util.LinkedHashMap::new));
    }

    private static BlockPlacement block(int x, int y, int z, PlacementRole role) {
        return new BlockPlacement(x, y, z, ResourceId.parse("minecraft:oak_log"), role);
    }
}
