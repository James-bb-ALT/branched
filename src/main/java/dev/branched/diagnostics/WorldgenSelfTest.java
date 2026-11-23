package dev.branched.diagnostics;

import com.mojang.datafixers.util.Pair;
import dev.branched.Branched;
import dev.branched.block.StaticBranchBlock;
import dev.branched.compat.physics.TreePhysicsCompat;
import dev.branched.registry.ModBlocks;
import dev.branched.worldgen.feature.StaticTreeFeature;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Opt-in headless proof that the static tree feature really runs during
 * worldgen on a dedicated server.
 *
 * <p>Enabled only with {@code -Dbranched.selftest=true}. It finds one
 * region per target biome group, generates a 3x3 chunk block, counts the blocks
 * owned by this mod and the wood species written, logs one line per biome plus a
 * summary and stops the server. It is never active in normal gameplay.</p>
 */
public final class WorldgenSelfTest {
    public static final String PROPERTY = "branched.selftest";
    /** Optional dimension id; defaults to {@code minecraft:overworld}. */
    public static final String DIMENSION_PROPERTY = "branched.selftest.dimension";
    /** Optional comma-separated target labels; defaults to every Phase 0-3 target. */
    public static final String TARGETS_PROPERTY = "branched.selftest.targets";
    /** Horizontal biome search step in blocks (default 8; 1 checks every block). */
    public static final String STEP_PROPERTY = "branched.selftest.step";
    private static final Logger LOGGER = Branched.LOGGER;
    private static final int SEARCH_RADIUS = 6144;
    private static final int DEFAULT_SEARCH_STEP = 8;

    /** Wood-block suffixes used by the modded tree families counted here. */
    private static final List<String> LOG_SUFFIXES =
            List.of("_log", "_wood", "_stem", "_hyphae");
    private static final List<String> LEAF_SUFFIXES =
            List.of("_leaves", "_fronds", "_needles");

    private static final List<Target> TARGETS = List.of(
            new Target("taiga", List.of(Biomes.TAIGA, Biomes.SNOWY_TAIGA)),
            new Target("grove", List.of(Biomes.GROVE)),
            new Target("savanna", List.of(
                    Biomes.SAVANNA, Biomes.SAVANNA_PLATEAU, Biomes.WINDSWEPT_SAVANNA)),
            new Target("windswept", List.of(
                    Biomes.WINDSWEPT_HILLS, Biomes.WINDSWEPT_FOREST,
                    Biomes.WINDSWEPT_GRAVELLY_HILLS)),
            new Target("badlands", List.of(Biomes.WOODED_BADLANDS)),
            new Target("snowy", List.of(Biomes.SNOWY_PLAINS, Biomes.ICE_SPIKES)),
            // Phase 3 targets. The keys are resolved lazily by the biome source;
            // when the biome mod is absent the search simply finds nothing.
            new Target("ru_maple", keys(
                    "regions_unexplored:autumnal_maple_forest",
                    "regions_unexplored:maple_forest")),
            new Target("ru_blackwood", keys(
                    "regions_unexplored:blackwood_taiga",
                    "regions_unexplored:cold_deciduous_forest")),
            new Target("ru_rainforest", keys(
                    "regions_unexplored:rainforest",
                    "regions_unexplored:sparse_rainforest")),
            new Target("terralith_misty", keys("terralith:haze_mountain")),
            new Target("terralith_highlands", keys(
                    "terralith:temperate_highlands")),
            new Target("terralith_forest", keys(
                    "terralith:highlands",
                    "terralith:birch_forest")));

    /**
     * Phase 4 targets. They are never part of the default run: the Twilight
     * Forest biomes live in their own dimension and Fish of Thieves injects
     * into {@code minecraft:sparse_jungle}, so the runner selects them with
     * {@code -Dbranched.selftest.targets=...} and, for Twilight Forest,
     * {@code -Dbranched.selftest.dimension=twilightforest:twilight_forest}.
     *
     * <p>They exist to prove preservation at runtime: Branched ships no
     * Phase 4 resource, so these chunks must contain only the mods' native wood
     * and zero static placements.</p>
     */
    private static final List<Target> PHASE4_TARGETS = List.of(
            new Target("tf_forest", keys(
                    "twilightforest:forest",
                    "twilightforest:dense_forest")),
            new Target("tf_dark_forest", keys(
                    "twilightforest:dark_forest",
                    "twilightforest:dark_forest_center")),
            new Target("fot_sparse_jungle", List.of(Biomes.SPARSE_JUNGLE)),
            new Target("quark_weald", keys("quark:glimmering_weald")),
            new Target("aether_skyroot", keys(
                    "aether:skyroot_forest",
                    "aether:skyroot_woodland",
                    "aether:skyroot_grove",
                    "aether:skyroot_meadow")));

    /** Every selectable target, in the order the labels are reported. */
    private static final Map<String, Target> ALL_TARGETS = allTargets();

    private static Map<String, Target> allTargets() {
        Map<String, Target> targets = new LinkedHashMap<>();
        for (Target target : TARGETS) {
            targets.put(target.label(), target);
        }
        for (Target target : PHASE4_TARGETS) {
            targets.putIfAbsent(target.label(), target);
        }
        return targets;
    }

    private WorldgenSelfTest() {
    }

    public static void onServerStarted(ServerStartedEvent event) {
        if (!Boolean.getBoolean(PROPERTY)) {
            return;
        }
        LOGGER.info("SELFTEST requested");
        MinecraftServer server = event.getServer();
        server.execute(() -> run(server));
    }

    private static void verifyTreePhysicsCompat() {
        BlockState oakLog = Blocks.OAK_LOG.defaultBlockState();
        BlockState oakBranch = ModBlocks.OAK_BRANCH.get().defaultBlockState();
        BlockState pineBranch = ModBlocks.PINE_BRANCH.get().defaultBlockState();
        if (!TreePhysicsCompat.isBranch(oakBranch) || TreePhysicsCompat.isBranch(oakLog)) {
            throw new IllegalStateException("TreePhysicsCompat.isBranch check failed");
        }
        if (!TreePhysicsCompat.matchesTreeWood(oakLog, oakBranch)
                || TreePhysicsCompat.matchesTreeWood(oakLog, pineBranch)) {
            throw new IllegalStateException("TreePhysicsCompat.matchesTreeWood check failed");
        }
        BlockState connected = StaticBranchBlock.withConnection(
                StaticBranchBlock.withThickness(oakBranch, 3), Direction.EAST, true);
        if (!TreePhysicsCompat.branchCollisionBounds(connected).equals(
                ModBlocks.OAK_BRANCH.get().getShape(connected, null, BlockPos.ZERO, null).bounds())) {
            throw new IllegalStateException("TreePhysicsCompat.branchCollisionBounds mismatch");
        }
        LOGGER.info("SELFTEST physics_compat=OK");
    }

    private static void run(MinecraftServer server) {
        try {
            LOGGER.info("SELFTEST begin");
            verifyTreePhysicsCompat();
            long heapBefore = usedHeapBytes();
            ServerLevel level = selectLevel(server);
            List<Target> targets = selectTargets();
            LOGGER.info("SELFTEST level={} targets={}", level.dimension().location(), targets.size());
            int sampled = 0;
            Totals totals = new Totals();
            for (int index = 0; index < targets.size(); index++) {
                Target target = targets.get(index);
                LOGGER.info("SELFTEST sampling {}", target.label());
                Sample sample = sample(level, target, index);
                if (sample == null) {
                    LOGGER.info("SELFTEST {}: not found within {} blocks", target.label(), SEARCH_RADIUS);
                    continue;
                }
                sampled++;
                totals.add(sample);
                LOGGER.info("SELFTEST {}", sample.describe());
            }
            LOGGER.info("SELFTEST summary: level={} targets={} biomes={} branches={} trees={} logs={} leaves={} "
                            + "feature_placements={} feature_rejections={} rejections={} "
                            + "chunkgen_ms={} heap_mb={}",
                    level.dimension().location(), targets.size(),
                    sampled, totals.branches, totals.trees, totals.logsAsString(),
                    totals.leavesAsString(), StaticTreeFeature.placedCount(),
                    StaticTreeFeature.rejectedCount(),
                    Branched.metrics().rejectionsByReason(),
                    totals.chunkgenPercentiles(), heapReport(heapBefore));
            try {
                java.nio.file.Path exported = DiagnosticsExporter.exportDefault(
                        server.getServerDirectory().toPath());
                LOGGER.info("SELFTEST exported diagnostics to {}", exported);
            } catch (Exception exportFailure) {
                LOGGER.warn("SELFTEST failed to export diagnostics", exportFailure);
            }
        } catch (Throwable throwable) {
            LOGGER.error("SELFTEST failed", throwable);
        } finally {
            server.halt(false);
        }
    }

    /** Resolves {@code branched.selftest.dimension}, falling back to the overworld. */
    private static ServerLevel selectLevel(MinecraftServer server) {
        String raw = System.getProperty(DIMENSION_PROPERTY, "").trim();
        if (raw.isEmpty()) {
            return server.overworld();
        }
        ResourceLocation id = ResourceLocation.tryParse(raw);
        if (id == null) {
            LOGGER.warn("SELFTEST ignoring malformed dimension '{}'", raw);
            return server.overworld();
        }
        ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, id));
        if (level == null) {
            LOGGER.warn("SELFTEST dimension '{}' is not loaded; using the overworld", raw);
            return server.overworld();
        }
        return level;
    }

    /**
     * Resolves {@code branched.selftest.targets}. Without the property the
     * historical Phase 0-3 target list is used unchanged, so older runs stay
     * comparable.
     */
    private static List<Target> selectTargets() {
        String raw = System.getProperty(TARGETS_PROPERTY, "").trim();
        if (raw.isEmpty()) {
            return TARGETS;
        }
        List<Target> selected = new ArrayList<>();
        for (String label : raw.split(",")) {
            String key = label.trim();
            if (key.isEmpty()) {
                continue;
            }
            Target target = ALL_TARGETS.get(key);
            if (target == null) {
                LOGGER.warn("SELFTEST unknown target '{}' (known: {})", key, ALL_TARGETS.keySet());
                continue;
            }
            selected.add(target);
        }
        if (selected.isEmpty()) {
            LOGGER.warn("SELFTEST no valid target selected; using the default list");
            return TARGETS;
        }
        return selected;
    }

    private static int searchStep() {
        String raw = System.getProperty(STEP_PROPERTY, "").trim();
        if (raw.isEmpty()) {
            return DEFAULT_SEARCH_STEP;
        }
        try {
            return Math.max(1, Integer.parseInt(raw));
        } catch (NumberFormatException ex) {
            return DEFAULT_SEARCH_STEP;
        }
    }

    /** Coarse heap snapshot; the JVM may not have collected yet, so read as a trend. */
    private static long usedHeapBytes() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static String heapReport(long before) {
        Runtime runtime = Runtime.getRuntime();
        return "[before=" + toMb(before)
                + ",after=" + toMb(usedHeapBytes())
                + ",max=" + toMb(runtime.maxMemory()) + "]";
    }

    private static String toMb(long bytes) {
        return String.format(java.util.Locale.ROOT, "%.0f", bytes / (1024.0D * 1024.0D));
    }

    private static Sample sample(ServerLevel level, Target target, int index) {
        BiomeSource source = level.getChunkSource().getGenerator().getBiomeSource();
        // A fresh, seed-derived RNG keeps the sampled region identical between
        // runs of the same world, so a vanilla baseline and the static tree run
        // can be compared chunk by chunk. The index separates the searches.
        RandomSource searchRandom = RandomSource.create(level.getSeed() + index * 7919L);
        Pair<BlockPos, Holder<Biome>> found = source.findBiomeHorizontal(
                0, level.getSeaLevel(), 0, SEARCH_RADIUS, searchStep(),
                holder -> target.keys().stream().anyMatch(key -> holder.is(key)),
                searchRandom, false,
                level.getChunkSource().randomState().sampler());
        if (found == null) {
            return null;
        }

        BlockPos center = found.getFirst();
        int chunkX = SectionPos.blockToSectionCoord(center.getX());
        int chunkZ = SectionPos.blockToSectionCoord(center.getZ());
        long placementsBefore = StaticTreeFeature.placedCount();
        long rejectionsBefore = StaticTreeFeature.rejectedCount();
        Sample sample = new Sample(target.label(), center, chunkX, chunkZ);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                long chunkStart = System.nanoTime();
                level.setChunkForced(chunkX + dx, chunkZ + dz, true);
                level.getChunk(chunkX + dx, chunkZ + dz);
                sample.chunkNanos.add(System.nanoTime() - chunkStart);
            }
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                LevelChunk chunk = level.getChunk(chunkX + dx, chunkZ + dz);
                ChunkPos pos = chunk.getPos();
                BlockPos biomePos = new BlockPos(pos.getMinBlockX() + 8,
                        level.getSeaLevel(), pos.getMinBlockZ() + 8);
                if (!target.matches(level.getBiome(biomePos))) {
                    continue;
                }
                sample.chunks++;
                for (int x = 0; x < 16; x++) {
                    for (int z = 0; z < 16; z++) {
                        for (int y = chunk.getMinBuildHeight(); y < chunk.getMaxBuildHeight(); y++) {
                            BlockState state = chunk.getBlockState(
                                    cursor.set(pos.getMinBlockX() + x, y, pos.getMinBlockZ() + z));
                            if (state.getBlock() instanceof dev.branched.block.StaticBranchBlock) {
                                sample.branches++;
                            } else if (state.is(Blocks.SPRUCE_LOG)) {
                                sample.logs.merge("spruce", 1, Integer::sum);
                                sample.logPositions.add(cursor.immutable());
                            } else if (state.is(Blocks.OAK_LOG)) {
                                sample.logs.merge("oak", 1, Integer::sum);
                                sample.logPositions.add(cursor.immutable());
                            } else if (state.is(Blocks.ACACIA_LOG)) {
                                sample.logs.merge("acacia", 1, Integer::sum);
                                sample.logPositions.add(cursor.immutable());
                            } else if (state.is(Blocks.BIRCH_LOG)) {
                                sample.logs.merge("birch", 1, Integer::sum);
                                sample.logPositions.add(cursor.immutable());
                            } else if (state.is(Blocks.SPRUCE_LEAVES)) {
                                sample.leaves.merge("spruce", 1, Integer::sum);
                            } else if (state.is(Blocks.OAK_LEAVES)) {
                                sample.leaves.merge("oak", 1, Integer::sum);
                            } else if (state.is(Blocks.ACACIA_LEAVES)) {
                                sample.leaves.merge("acacia", 1, Integer::sum);
                            } else if (state.is(Blocks.BIRCH_LEAVES)) {
                                sample.leaves.merge("birch", 1, Integer::sum);
                            } else {
                                // Phase 3 mods ship their own wood blocks. Vanilla
                                // namespaces stay on the explicit labels above so
                                // Phase 2 totals remain comparable.
                                ResourceLocation blockId = state.getBlock()
                                        .builtInRegistryHolder().key().location();
                                if (!"minecraft".equals(blockId.getNamespace())) {
                                    String path = blockId.getPath();
                                    String species = speciesKey(path);
                                    if (species != null) {
                                        if (isLog(path)) {
                                            sample.logs.merge(species, 1, Integer::sum);
                                            sample.logPositions.add(cursor.immutable());
                                        } else {
                                            sample.leaves.merge(species, 1, Integer::sum);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        // One tree = one connected run of logs. Static trunks bend, so counting
        // log columns would over-count them; a flood fill does not care.
        sample.trees = countLogComponents(sample.logPositions);
        sample.placements = StaticTreeFeature.placedCount() - placementsBefore;
        sample.rejections = StaticTreeFeature.rejectedCount() - rejectionsBefore;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setChunkForced(chunkX + dx, chunkZ + dz, false);
            }
        }
        return sample;
    }

    private static boolean isLog(String path) {
        return LOG_SUFFIXES.stream().anyMatch(path::endsWith);
    }

    /** Strips a wood suffix so log and leaf counters share one species key. */
    private static String speciesKey(String path) {
        for (String suffix : LOG_SUFFIXES) {
            if (path.endsWith(suffix)) {
                return path.substring(0, path.length() - suffix.length());
            }
        }
        for (String suffix : LEAF_SUFFIXES) {
            if (path.endsWith(suffix)) {
                return path.substring(0, path.length() - suffix.length());
            }
        }
        return null;
    }

    /** Flood fill over 6-neighbour log runs; each run is one trunk. */
    private static int countLogComponents(List<BlockPos> logPositions) {
        Set<Long> remaining = new HashSet<>(logPositions.size() * 2);
        for (BlockPos pos : logPositions) {
            remaining.add(pos.asLong());
        }
        int components = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (BlockPos start : logPositions) {
            if (!remaining.remove(start.asLong())) {
                continue;
            }
            components++;
            ArrayDeque<BlockPos> queue = new ArrayDeque<>();
            queue.add(start);
            while (!queue.isEmpty()) {
                BlockPos current = queue.poll();
                for (Direction direction : Direction.values()) {
                    cursor.setWithOffset(current, direction);
                    if (remaining.remove(cursor.asLong())) {
                        queue.add(cursor.immutable());
                    }
                }
            }
        }
        return components;
    }

    /** Lazily-resolved biome keys; an absent mod simply never matches. */
    private static List<ResourceKey<Biome>> keys(String... ids) {
        List<ResourceKey<Biome>> keys = new ArrayList<>(ids.length);
        for (String id : ids) {
            keys.add(ResourceKey.create(Registries.BIOME, new ResourceLocation(id)));
        }
        return keys;
    }

    private record Target(String label, List<ResourceKey<Biome>> keys) {
        private boolean matches(Holder<Biome> biome) {
            return keys.stream().anyMatch(key -> biome.is(key));
        }
    }

    private static final class Sample {
        private final String label;
        private final BlockPos center;
        private final int chunkX;
        private final int chunkZ;
        private final Map<String, Integer> logs = new LinkedHashMap<>();
        private final Map<String, Integer> leaves = new LinkedHashMap<>();
        private final List<BlockPos> logPositions = new ArrayList<>();
        private final List<Long> chunkNanos = new ArrayList<>();
        private int chunks;
        private int branches;
        private int trees;
        private long placements;
        private long rejections;

        private Sample(String label, BlockPos center, int chunkX, int chunkZ) {
            this.label = label;
            this.center = center;
            this.chunkX = chunkX;
            this.chunkZ = chunkZ;
        }

        private String describe() {
            return label + " at " + center.toShortString()
                    + " chunks[" + chunkX + "," + chunkZ + "]"
                    + " target_chunks=" + chunks + "/9"
                    + " branches=" + branches
                    + " trees=" + trees
                    + " logs=" + logs
                    + " leaves=" + leaves
                    + " placements_delta=" + placements
                    + " rejections_delta=" + rejections
                    + " chunkgen_ms=" + chunkgen();
        }

        /** p50/max of the nine cold chunk generations for this target. */
        private String chunkgen() {
            if (chunkNanos.isEmpty()) {
                return "[n=0]";
            }
            long[] sorted = chunkNanos.stream().mapToLong(Long::longValue).sorted().toArray();
            return String.format(java.util.Locale.ROOT, "[p50=%.2f,max=%.2f,n=%d]",
                    Totals.percentile(sorted, 0.50D),
                    sorted[sorted.length - 1] / 1_000_000.0D, sorted.length);
        }
    }

    private static final class Totals {
        private int branches;
        private int trees;
        private final Map<String, Integer> logs = new LinkedHashMap<>();
        private final Map<String, Integer> leaves = new LinkedHashMap<>();
        private final List<Long> chunkNanos = new ArrayList<>();

        private void add(Sample sample) {
            branches += sample.branches;
            trees += sample.trees;
            sample.logs.forEach((key, value) -> logs.merge(key, value, Integer::sum));
            sample.leaves.forEach((key, value) -> leaves.merge(key, value, Integer::sum));
            chunkNanos.addAll(sample.chunkNanos);
        }

        /** p50/p95/p99 wall time of a cold chunk generation, in milliseconds. */
        private String chunkgenPercentiles() {
            if (chunkNanos.isEmpty()) {
                return "[n=0]";
            }
            long[] sorted = chunkNanos.stream().mapToLong(Long::longValue).sorted().toArray();
            return String.format(java.util.Locale.ROOT,
                    "[p50=%.2f,p95=%.2f,p99=%.2f,max=%.2f,n=%d]",
                    percentile(sorted, 0.50D), percentile(sorted, 0.95D),
                    percentile(sorted, 0.99D), sorted[sorted.length - 1] / 1_000_000.0D,
                    sorted.length);
        }

        private static double percentile(long[] sorted, double quantile) {
            int index = (int) Math.ceil(quantile * sorted.length) - 1;
            index = Math.max(0, Math.min(sorted.length - 1, index));
            return sorted[index] / 1_000_000.0D;
        }

        private String logsAsString() {
            return logs.toString();
        }

        private String leavesAsString() {
            return leaves.toString();
        }
    }
}
