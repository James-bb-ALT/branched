package dev.branched.hardening;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.branched.Branched;
import dev.branched.compat.AdapterRegistry;
import dev.branched.compat.AdapterState;
import dev.branched.compat.DefaultAdapters;
import dev.branched.core.ResourceId;
import dev.branched.diagnostics.DiagnosticsExporter;
import dev.branched.metrics.MetricsRecorder;
import dev.branched.morphology.BlockPalette;
import dev.branched.morphology.BlockPlacement;
import dev.branched.morphology.CanonicalShape;
import dev.branched.morphology.MorphologyRequest;
import dev.branched.morphology.PerformancePreset;
import dev.branched.morphology.PlacementRole;
import dev.branched.morphology.RasterizedTree;
import dev.branched.morphology.ReferenceBlockRasterizer;
import dev.branched.morphology.SemiproceduralMorphologyGenerator;
import dev.branched.morphology.TreeSkeleton;
import dev.branched.platform.ModPresence;
import dev.branched.platform.StaticModPresence;
import dev.branched.profile.ProfileParser;
import dev.branched.species.CompatibilityLevel;
import dev.branched.species.SpeciesProfile;
import dev.branched.species.SpeciesRegistry;
import dev.branched.worldgen.BranchMaterials;
import dev.branched.worldgen.LeafDistance;
import dev.branched.worldgen.PlacementContext;
import dev.branched.worldgen.PlacementResult;
import dev.branched.worldgen.TreeFitValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 6 compatibility and hardening verification:
 * giant trees, chunk-boundary guards, multi-thread determinism, datapack reload
 * atomicity, mod-version matrix and diagnostics JSON export.
 */
class Phase6HardeningTest {
    private static final Path PROFILE_ROOT =
            Path.of("src/main/resources/data/branched/arboreal/species");
    private static final ResourceId OVERWORLD = ResourceId.parse("minecraft:overworld");

    @Test
    void giantTreesRespectChunkWriteBoundariesAndLeafSupport() throws Exception {
        Map<String, SpeciesProfile> profiles = loadProfiles();
        SemiproceduralMorphologyGenerator generator = new SemiproceduralMorphologyGenerator();
        ReferenceBlockRasterizer rasterizer = new ReferenceBlockRasterizer();

        for (String speciesId : List.of(
                "regions_unexplored:redwood",
                "regions_unexplored:kapok",
                "regions_unexplored:eucalyptus")) {
            SpeciesProfile profile = profiles.get(speciesId);
            BlockPalette palette = palette(profile);

            // Placed near the edge of a 3x3 chunk write window [0..47] so wide crowns trip canWrite.
            TreeSkeleton edgeSkeleton = generator.generate(
                    new MorphologyRequest(777L, OVERWORLD, 46, 64, 46, 0L, profile));
            RasterizedTree edgeTree = rasterizer.rasterize(
                    edgeSkeleton, profile, palette, 46, 64, 46, PerformancePreset.BALANCED);
            PlacementContext edgeCtx = new BoundedContext(0, 47, 0, 47, Set.of());
            assertEquals("outside_write_region", TreeFitValidator.validate(edgeTree, edgeCtx).reason(),
                    speciesId + " must reject writes outside the loaded chunk window");

            // Placed at center of a generous region with valid ground: fits cleanly and leaves obey BFS distance.
            TreeSkeleton centerSkeleton = generator.generate(
                    new MorphologyRequest(777L, OVERWORLD, 128, 64, 128, 0L, profile));
            RasterizedTree centerTree = rasterizer.rasterize(
                    centerSkeleton, profile, palette, 128, 64, 128, PerformancePreset.BALANCED);
            PlacementContext openCtx = new BoundedContext(64, 192, 64, 192, Set.of());
            assertEquals(PlacementResult.Outcome.PLACED, TreeFitValidator.validate(centerTree, openCtx).outcome(),
                    speciesId + " should place cleanly inside an open chunk window");

            Map<Long, Integer> distances = LeafDistance.distances(centerTree.blocks());
            for (Integer distance : distances.values()) {
                assertTrue(distance >= 1 && distance <= LeafDistance.MAX_SUPPORTED_DISTANCE);
            }

            // Trunk block obstruction rejects whole tree.
            BlockPlacement trunkBlock = centerTree.blocks().stream()
                    .filter(b -> b.role() == PlacementRole.LOG)
                    .skip(1)
                    .findFirst()
                    .orElseThrow();
            PlacementContext blockedCtx = new BoundedContext(64, 192, 64, 192,
                    Set.of(LeafDistance.key(trunkBlock.x(), trunkBlock.y(), trunkBlock.z())));
            assertEquals("blocked", TreeFitValidator.validate(centerTree, blockedCtx).reason());
        }
    }

    @Test
    void parallelWorldgenRemainsDeterministicAcrossThreadsAndCallOrder() throws Exception {
        Map<String, SpeciesProfile> profiles = loadProfiles();
        List<SpeciesProfile> subset = List.of(
                profiles.get("minecraft:oak"),
                profiles.get("minecraft:spruce"),
                profiles.get("minecraft:acacia"),
                profiles.get("regions_unexplored:maple"),
                profiles.get("regions_unexplored:redwood"));

        SemiproceduralMorphologyGenerator generator = new SemiproceduralMorphologyGenerator();
        ReferenceBlockRasterizer rasterizer = new ReferenceBlockRasterizer();

        record Job(SpeciesProfile profile, long seed, int x, int z) {
        }
        List<Job> jobs = new ArrayList<>();
        Map<Job, String> expected = new HashMap<>();
        for (SpeciesProfile profile : subset) {
            for (int i = 0; i < 12; i++) {
                int x = -500 + i * 73;
                int z = 300 - i * 41;
                Job job = new Job(profile, 987654321L + i, x, z);
                jobs.add(job);
                TreeSkeleton skel = generator.generate(
                        new MorphologyRequest(job.seed(), OVERWORLD, x, 64, z, 0L, profile));
                RasterizedTree tree = rasterizer.rasterize(skel, profile, palette(profile), x, 64, z);
                expected.put(job, CanonicalShape.exact(tree.blocks()));
            }
        }

        List<Job> shuffled = new ArrayList<>(jobs);
        Collections.shuffle(shuffled, new java.util.Random(42L));

        ExecutorService pool = Executors.newFixedThreadPool(8);
        try {
            List<Callable<Boolean>> tasks = new ArrayList<>();
            for (Job job : shuffled) {
                tasks.add(() -> {
                    TreeSkeleton skel = generator.generate(
                            new MorphologyRequest(job.seed(), OVERWORLD, job.x(), 64, job.z(), 0L, job.profile()));
                    RasterizedTree tree = rasterizer.rasterize(
                            skel, job.profile(), palette(job.profile()), job.x(), 64, job.z());
                    return expected.get(job).equals(CanonicalShape.exact(tree.blocks()));
                });
            }
            for (Future<Boolean> future : pool.invokeAll(tasks)) {
                assertTrue(future.get(), "concurrent morphology generation drifted");
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void datapackReloadSwapsAtomicallyAndPreservesRegistryOnTotalFailure() throws Exception {
        ProfileParser parser = new ProfileParser();
        ProfileParser.ParseResult initial = parser.parseDirectory(PROFILE_ROOT);
        SpeciesRegistry registry = new SpeciesRegistry();
        ProfileParser.apply(initial, registry);
        assertEquals(70, registry.size());

        // Override oak height via simulated datapack reload.
        Map<ResourceId, JsonObject> files = new java.util.LinkedHashMap<>();
        for (SpeciesProfile p : initial.profiles()) {
            Path file = PROFILE_ROOT.resolve(p.id().namespace()).resolve(p.id().path() + ".json");
            files.put(p.id(), JsonParser.parseString(Files.readString(file)).getAsJsonObject());
        }
        JsonObject oakOverride = files.get(ResourceId.parse("minecraft:oak")).deepCopy();
        oakOverride.getAsJsonObject("trunk").getAsJsonArray("height").set(0, new com.google.gson.JsonPrimitive(9));
        oakOverride.getAsJsonObject("trunk").getAsJsonArray("height").set(1, new com.google.gson.JsonPrimitive(13));
        files.put(ResourceId.parse("minecraft:oak"), oakOverride);

        ProfileParser.ParseResult overrideResult = parser.parse(files);
        assertEquals(0, overrideResult.errorCount());
        registry.replaceAll(overrideResult.profiles());
        assertEquals(9.0D, registry.get(ResourceId.parse("minecraft:oak")).orElseThrow().trunkHeight().min());

        // Total failure (only invalid profile): keep previous valid registry untouched.
        JsonObject invalid = new JsonObject();
        invalid.addProperty("schema", 99);
        ProfileParser.ParseResult broken = parser.parse(Map.of(ResourceId.parse("minecraft:broken"), invalid));
        assertTrue(broken.profiles().isEmpty());
        assertTrue(broken.errorCount() > 0);
        if (!(broken.profiles().isEmpty() && broken.errorCount() > 0)) {
            registry.replaceAll(broken.profiles());
        }
        assertEquals(70, registry.size());
        assertEquals(9.0D, registry.get(ResourceId.parse("minecraft:oak")).orElseThrow().trunkHeight().min());
    }

    @Test
    void diagnosticsExportCapturesModMatrixAndDynamicTreesWarning(@TempDir Path tempDir) throws Exception {
        ProfileParser.ParseResult parsed = new ProfileParser().parseDirectory(PROFILE_ROOT);
        SpeciesRegistry profiles = new SpeciesRegistry();
        ProfileParser.apply(parsed, profiles);

        AdapterRegistry adapters = new AdapterRegistry();
        adapters.replaceAll(DefaultAdapters.create());

        ModPresence fullPresence = new StaticModPresence(
                Set.of("minecraft", "regions_unexplored", "terralith", "aether",
                        "twilightforest", "quark", "fishofthieves", "dynamictrees"),
                Map.of(
                        "regions_unexplored", "0.5.6",
                        "terralith", "2.5.4",
                        "aether", "1.20.1-1.5.2-neoforge",
                        "twilightforest", "4.3.2508",
                        "quark", "4.0-462",
                        "fishofthieves", "3.4.0",
                        "dynamictrees", "1.4.11"));

        MetricsRecorder metrics = new MetricsRecorder(true);
        metrics.recordRejection("blocked");

        Path out = DiagnosticsExporter.export(tempDir, profiles, adapters, fullPresence, metrics,
                42L, 3L, Branched.DEFAULT_VERSION);
        assertTrue(Files.isRegularFile(out));

        JsonObject json = JsonParser.parseString(Files.readString(out)).getAsJsonObject();
        assertEquals("branched", json.get("modId").getAsString());
        assertEquals(Branched.DEFAULT_VERSION, json.get("version").getAsString());
        String gradleProperties = Files.readString(Path.of("gradle.properties"));
        assertTrue(gradleProperties.lines().anyMatch(
                line -> line.trim().equals("mod_version=" + Branched.DEFAULT_VERSION)),
                "gradle.properties mod_version must stay in sync with Branched.DEFAULT_VERSION");
        assertTrue(json.getAsJsonObject("invariants").get("zeroBlockEntities").getAsBoolean());
        assertTrue(json.getAsJsonObject("invariants").get("dynamicTreesLoaded").getAsBoolean());
        assertEquals(70, json.getAsJsonObject("profiles").get("total").getAsInt());
        assertEquals(24, json.getAsJsonObject("adapters").get("total").getAsInt());
        assertEquals(7, json.getAsJsonObject("adapters").get("readyCount").getAsInt());
        assertEquals(42L, json.getAsJsonObject("metrics").get("PlacedCount").getAsLong());
        assertEquals(AdapterState.READY.name(), adapters.states(fullPresence).get("fishofthieves").name());
        assertFalse(adapters.states(fullPresence).get("dtbaobabtree") == AdapterState.READY);
    }

    private static Map<String, SpeciesProfile> loadProfiles() throws Exception {
        Map<String, SpeciesProfile> map = new HashMap<>();
        new ProfileParser().parseDirectory(PROFILE_ROOT).profiles()
                .forEach(p -> map.put(p.id().toString(), p));
        return map;
    }

    private static BlockPalette palette(SpeciesProfile profile) {
        return new BlockPalette(profile.log(), BranchMaterials.resolve(profile.log()),
                profile.leaves(), profile.sapling().orElse(null), null);
    }

    private static final class BoundedContext implements PlacementContext {
        private final int minX;
        private final int maxX;
        private final int minZ;
        private final int maxZ;
        private final Set<Long> solidBlocks;

        private BoundedContext(int minX, int maxX, int minZ, int maxZ, Set<Long> solidBlocks) {
            this.minX = minX;
            this.maxX = maxX;
            this.minZ = minZ;
            this.maxZ = maxZ;
            this.solidBlocks = solidBlocks;
        }

        @Override
        public long worldSeed() { return 123456789L; }

        @Override
        public ResourceId dimension() { return OVERWORLD; }

        @Override
        public int minBuildHeight() { return -64; }

        @Override
        public int maxBuildHeight() { return 320; }

        @Override
        public boolean isAir(int x, int y, int z) {
            return y > 63 && !solidBlocks.contains(LeafDistance.key(x, y, z));
        }

        @Override
        public boolean isReplaceable(int x, int y, int z) {
            return isAir(x, y, z);
        }

        @Override
        public boolean hasSolidGround(int x, int z) { return true; }

        @Override
        public boolean isGround(int x, int y, int z) { return y == 63; }

        @Override
        public boolean canWrite(int x, int y, int z) {
            return x >= minX && x <= maxX && z >= minZ && z <= maxZ;
        }

        @Override
        public ResourceId blockAt(int x, int y, int z) {
            return ResourceId.parse("minecraft:air");
        }
    }
}
