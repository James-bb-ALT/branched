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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Visual-diversity gate for Phase 2: different seeds must change the silhouette
 * within one species, and the shipped archetypes must remain visually distinct
 * from each other (conifers narrow, acacias umbrella-shaped, oaks broad, etc.).
 */
class MorphologyDiversityTest {
    private static final Path PROFILE_ROOT =
            Path.of("src/main/resources/data/branched/arboreal/species");
    private static final int SAMPLES = 18;

    private final SemiproceduralMorphologyGenerator generator =
            new SemiproceduralMorphologyGenerator();
    private final ReferenceBlockRasterizer rasterizer = new ReferenceBlockRasterizer();

    @Test
    void everyReplacedSpeciesVariesWithSeedAndPosition() throws IOException {
        Map<String, SpeciesProfile> profiles = loadProfiles();
        for (String id : List.of(
                "minecraft:oak",
                "minecraft:birch",
                "minecraft:spruce",
                "minecraft:pine",
                "minecraft:acacia",
                "minecraft:fancy_oak")) {
            Set<Set<Long>> silhouettes = new HashSet<>();
            for (int i = 0; i < SAMPLES; i++) {
                silhouettes.add(silhouette(profiles.get(id), i * 977L + 11L, i * 31, i * -47));
            }
            assertTrue(silhouettes.size() >= SAMPLES - 1,
                    id + " repeated silhouettes: " + silhouettes.size() + "/" + SAMPLES);
        }
    }

    @Test
    void archetypesKeepDistinctProportions() throws IOException {
        Map<String, SpeciesProfile> profiles = loadProfiles();
        Map<String, Metrics> average = new LinkedHashMap<>();
        for (String id : List.of(
                "minecraft:oak",
                "minecraft:birch",
                "minecraft:spruce",
                "minecraft:pine",
                "minecraft:acacia",
                "minecraft:fancy_oak")) {
            average.put(id, average(profiles.get(id)));
        }
        average.forEach((id, metrics) -> System.out.println(id + " " + metrics));

        Metrics oak = average.get("minecraft:oak");
        Metrics birch = average.get("minecraft:birch");
        Metrics spruce = average.get("minecraft:spruce");
        Metrics pine = average.get("minecraft:pine");
        Metrics acacia = average.get("minecraft:acacia");
        Metrics fancyOak = average.get("minecraft:fancy_oak");

        assertTrue(spruce.aspect() < oak.aspect(),
                "spruce must be narrower than oak: " + spruce.aspect() + " vs " + oak.aspect());
        assertTrue(pine.aspect() < spruce.aspect(),
                "pine must be narrower than spruce: " + pine.aspect() + " vs " + spruce.aspect());
        assertTrue(birch.aspect() < oak.aspect(),
                "birch must be narrower than oak: " + birch.aspect() + " vs " + oak.aspect());
        assertTrue(acacia.upperWidth() > acacia.lowerWidth(),
                "acacia crown must sit above the trunk: " + acacia);
        assertTrue(acacia.leafCentroid() > oak.leafCentroid(),
                "acacia leaves must sit higher than oak: " + acacia.leafCentroid()
                        + " vs " + oak.leafCentroid());
        assertTrue(fancyOak.height() > oak.height(),
                "fancy oak must be taller than oak: " + fancyOak.height() + " vs " + oak.height());
    }

    @Test
    void everyReplacedSpeciesStaysWithinLeafAndWoodBudgets() throws IOException {
        for (SpeciesProfile profile : loadProfiles().values()) {
            if (!List.of("minecraft:oak", "minecraft:birch", "minecraft:spruce",
                    "minecraft:pine", "minecraft:acacia", "minecraft:fancy_oak")
                    .contains(profile.id().toString())) {
                continue;
            }
            Metrics metrics = average(profile);
            assertTrue(metrics.leavesPerTree() >= 25.0D,
                    profile.id() + " too sparse: " + metrics.leavesPerTree());
            assertTrue(metrics.logsPerTree() >= 4.0D,
                    profile.id() + " too few logs: " + metrics.logsPerTree());
        }
    }

    private Metrics average(SpeciesProfile profile) {
        double height = 0.0D;
        double width = 0.0D;
        double leaves = 0.0D;
        double logs = 0.0D;
        double upper = 0.0D;
        double lower = 0.0D;
        double centroid = 0.0D;
        for (int i = 0; i < SAMPLES; i++) {
            Metrics metrics = measure(profile, i * 977L + 11L, i * 31, i * -47);
            height += metrics.height();
            width += metrics.width();
            leaves += metrics.leavesPerTree();
            logs += metrics.logsPerTree();
            upper += metrics.upperWidth();
            lower += metrics.lowerWidth();
            centroid += metrics.leafCentroid();
        }
        return new Metrics(height / SAMPLES, width / SAMPLES, leaves / SAMPLES, logs / SAMPLES,
                upper / SAMPLES, lower / SAMPLES, centroid / SAMPLES);
    }

    private Metrics measure(SpeciesProfile profile, long seed, int x, int z) {
        RasterizedTree tree = rasterize(profile, seed, x, z);
        int baseY = 64;
        int maxY = 0;
        int minX = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxZ = Integer.MIN_VALUE;

        for (BlockPlacement block : tree.blocks()) {
            maxY = Math.max(maxY, block.y() - baseY);
            minX = Math.min(minX, block.x());
            maxX = Math.max(maxX, block.x());
            minZ = Math.min(minZ, block.z());
            maxZ = Math.max(maxZ, block.z());
        }

        int upperMinX = Integer.MAX_VALUE;
        int upperMaxX = Integer.MIN_VALUE;
        int upperMinZ = Integer.MAX_VALUE;
        int upperMaxZ = Integer.MIN_VALUE;
        int lowerMinX = Integer.MAX_VALUE;
        int lowerMaxX = Integer.MIN_VALUE;
        int lowerMinZ = Integer.MAX_VALUE;
        int lowerMaxZ = Integer.MIN_VALUE;
        long leafY = 0;
        int leafCount = 0;
        for (BlockPlacement block : tree.blocks()) {
            if (block.role() != PlacementRole.LEAVES) {
                continue;
            }
            int relativeY = block.y() - baseY;
            leafY += relativeY;
            leafCount++;
            if (relativeY >= 0.70D * maxY) {
                upperMinX = Math.min(upperMinX, block.x());
                upperMaxX = Math.max(upperMaxX, block.x());
                upperMinZ = Math.min(upperMinZ, block.z());
                upperMaxZ = Math.max(upperMaxZ, block.z());
            }
            if (relativeY <= 0.35D * maxY) {
                lowerMinX = Math.min(lowerMinX, block.x());
                lowerMaxX = Math.max(lowerMaxX, block.x());
                lowerMinZ = Math.min(lowerMinZ, block.z());
                lowerMaxZ = Math.max(lowerMaxZ, block.z());
            }
        }

        int height = maxY + 1;
        int width = Math.max(maxX - minX, maxZ - minZ) + 1;
        int upperWidth = upperMaxX < upperMinX ? 0
                : Math.max(upperMaxX - upperMinX, upperMaxZ - upperMinZ) + 1;
        int lowerWidth = lowerMaxX < lowerMinX ? 0
                : Math.max(lowerMaxX - lowerMinX, lowerMaxZ - lowerMinZ) + 1;
        return new Metrics(height, width, tree.leafBlocks(), tree.logBlocks(),
                upperWidth, lowerWidth, leafCount == 0 ? 0.0D : leafY / (double) leafCount / height);
    }

    private Set<Long> silhouette(SpeciesProfile profile, long seed, int x, int z) {
        RasterizedTree tree = rasterize(profile, seed, x, z);
        int minX = tree.blocks().stream().mapToInt(BlockPlacement::x).min().orElse(0);
        int minZ = tree.blocks().stream().mapToInt(BlockPlacement::z).min().orElse(0);
        Set<Long> shape = new HashSet<>();
        for (BlockPlacement block : tree.blocks()) {
            long key = (((long) (block.x() - minX)) << 40)
                    | (((long) block.y()) << 20)
                    | ((block.z() - minZ) & 0xFFFFFL);
            shape.add(key);
        }
        return shape;
    }

    private RasterizedTree rasterize(SpeciesProfile profile, long seed, int x, int z) {
        TreeSkeleton skeleton = generator.generate(new MorphologyRequest(
                seed, ResourceId.parse("minecraft:overworld"), x, 64, z, 0L, profile));
        BlockPalette palette = new BlockPalette(profile.log(),
                BranchMaterials.resolve(profile.log()), profile.leaves(),
                profile.sapling().orElse(null), null);
        return rasterizer.rasterize(skeleton, profile, palette, x, 64, z);
    }

    private static Map<String, SpeciesProfile> loadProfiles() throws IOException {
        return new ProfileParser().parseDirectory(PROFILE_ROOT).profiles().stream()
                .collect(Collectors.toMap(profile -> profile.id().toString(), profile -> profile,
                        (first, second) -> first, LinkedHashMap::new));
    }

    private record Metrics(double height, double width, double leavesPerTree,
                           double logsPerTree, double upperWidth, double lowerWidth,
                           double leafCentroid) {
        double aspect() {
            return width / Math.max(1.0D, height);
        }

        @Override
        public String toString() {
            return String.format(
                    "height=%.2f width=%.2f aspect=%.2f leaves=%.1f logs=%.1f"
                            + " upper=%.1f lower=%.1f centroid=%.2f",
                    height, width, aspect(), leavesPerTree, logsPerTree,
                    upperWidth, lowerWidth, leafCentroid);
        }
    }
}
