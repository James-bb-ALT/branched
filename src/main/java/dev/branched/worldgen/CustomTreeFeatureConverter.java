package dev.branched.worldgen;

import dev.branched.block.StaticBranchBlock;
import dev.branched.config.ArborealConfig;
import dev.branched.core.ResourceId;
import dev.branched.morphology.SeedMixer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Post-processes custom modded {@code Feature} tree placements (Regions Unexplored,
 * Twilight Forest, BetterEnd, BetterNether, Alex's Caves, Upgrade Aquatic) via a
 * bounded before-free connected-component scan and reuses
 * {@link NativeTreeBranchConverter} to place 3D {@link StaticBranchBlock} limbs.
 */
public final class CustomTreeFeatureConverter {
    private static final int MAX_LOGS = 1500;
    private static final int MAX_LEAVES = 6000;

    private static final TreeScanProfile NORMAL_TREE = new TreeScanProfile(16, -4, 32, false, false);
    private static final TreeScanProfile NORMAL_NON_TAG_LEAF = new TreeScanProfile(16, -4, 32, true, false);
    private static final TreeScanProfile GIANT_TREE = new TreeScanProfile(24, -6, 64, false, false);
    private static final TreeScanProfile CEILING_TREE = new TreeScanProfile(16, -28, 6, false, true);

    private static final Map<String, TreeScanProfile> ALLOWLIST = buildAllowlist();
    private static final ClassValue<Optional<TreeScanProfile>> PROFILE_CACHE = new ClassValue<>() {
        @Override
        protected Optional<TreeScanProfile> computeValue(Class<?> type) {
            return Optional.ofNullable(profileForClassName(type.getName()));
        }
    };
    private static final ThreadLocal<Boolean> IN_CONVERSION = ThreadLocal.withInitial(() -> Boolean.FALSE);

    private CustomTreeFeatureConverter() {
    }

    public record TreeScanProfile(int xzRadius,
                                  int minDy,
                                  int maxDy,
                                  boolean allowNonTagFoliage,
                                  boolean allowCeilingAnchor) {
    }

    private static Map<String, TreeScanProfile> buildAllowlist() {
        Map<String, TreeScanProfile> map = new LinkedHashMap<>();

        // Regions Unexplored (30 living-tree Feature subclasses; excludes palms, bamboo, cacti, bioshrooms, ice, fallen logs)
        String ru = "net.regions_unexplored.world.level.feature.tree.";
        map.put(ru + "LargeJoshuaTreeFeature", NORMAL_TREE);
        map.put(ru + "MediumJoshuaTreeFeature", NORMAL_TREE);
        map.put(ru + "SmallJoshuaTreeFeature", NORMAL_TREE);
        map.put(ru + "LargeSocotraTreeFeature", NORMAL_TREE);
        map.put(ru + "SmallSocotraTreeFeature", NORMAL_TREE);
        map.put(ru + "AshenTreeFeature", NORMAL_TREE);
        map.put(ru + "AspenTreeFeature", NORMAL_TREE);
        map.put(ru + "BlackwoodTreeFeature", NORMAL_TREE);
        map.put(ru + "CobaltShrubFeature", NORMAL_NON_TAG_LEAF);
        map.put(ru + "CypressTreeFeature", NORMAL_TREE);
        map.put(ru + "GiantCypressTreeFeature", GIANT_TREE);
        map.put(ru + "DeadTreeFeature", NORMAL_TREE);
        map.put(ru + "EucalyptusTreeFeature", NORMAL_TREE);
        map.put(ru + "SmallEucalyptusTreeFeature", NORMAL_TREE);
        map.put(ru + "KapokTreeFeature", NORMAL_TREE);
        map.put(ru + "LarchTreeFeature", NORMAL_TREE);
        map.put(ru + "LushPineTreeFeature", NORMAL_TREE);
        map.put(ru + "MapleTreeFeature", NORMAL_TREE);
        map.put(ru + "MegaBaobabTreeFeature", GIANT_TREE);
        map.put(ru + "UltraBaobabTreeFeature", GIANT_TREE);
        map.put(ru + "PineTreeFeature", NORMAL_TREE);
        map.put(ru + "StrippedPineTreeFeature", NORMAL_TREE);
        map.put(ru + "RedwoodTreeFeature", GIANT_TREE);
        map.put(ru + "SuperRedwoodTreeFeature", GIANT_TREE);
        map.put(ru + "UltraRedwoodTreeFeature", GIANT_TREE);
        map.put(ru + "SakuraTreeFeature", NORMAL_TREE);
        map.put(ru + "SmallOakTreeFeature", NORMAL_TREE);
        map.put(ru + "TreeShrubFeature", NORMAL_TREE);
        map.put(ru + "nether.BrimWillowFeature", NORMAL_TREE);
        map.put(ru + "nether.TallBrimWillowFeature", NORMAL_TREE);

        // Twilight Forest + Twilight Delight (8 tree Feature subclasses)
        String tf = "twilightforest.world.components.feature.trees.";
        map.put(tf + "CanopyTreeFeature", NORMAL_TREE);
        map.put(tf + "DarkCanopyTreeFeature", NORMAL_TREE);
        map.put(tf + "OakCanopyTreeFeature", NORMAL_TREE);
        map.put(tf + "HollowTreeFeature", GIANT_TREE);
        map.put(tf + "LargeWinterTreeFeature", GIANT_TREE);
        map.put(tf + "MiningTreeFeature", NORMAL_TREE);
        map.put(tf + "SnowTreeFeature", NORMAL_TREE);
        map.put(tf + "TimeTreeFeature", NORMAL_TREE);

        // BetterEnd (8 living-tree Feature subclasses; excludes mushroom/fungi features)
        String be = "org.betterx.betterend.world.features.trees.";
        map.put(be + "DragonTreeFeature", NORMAL_TREE);
        map.put(be + "DragonHelixTreeFeature", GIANT_TREE);
        map.put(be + "HelixTreeFeature", NORMAL_TREE);
        map.put(be + "LacugroveFeature", NORMAL_TREE);
        map.put(be + "LucerniaFeature", GIANT_TREE);
        map.put(be + "PythadendronTreeFeature", NORMAL_TREE);
        map.put(be + "TenaneaFeature", NORMAL_TREE);
        map.put(be + "UmbrellaTreeFeature", NORMAL_NON_TAG_LEAF);

        // BetterNether (7 living-tree Feature subclasses)
        String bn = "org.betterx.betternether.world.features.";
        map.put(bn + "AnchorTreeFeature", CEILING_TREE);
        map.put(bn + "GloomwoodTreeFeature", NORMAL_TREE);
        map.put(bn + "NetherSakuraFeature", NORMAL_TREE);
        map.put(bn + "OldWillowTree", NORMAL_TREE);
        map.put(bn + "RubeusTreeFeature", NORMAL_TREE);
        map.put(bn + "WartTreeFeature", NORMAL_NON_TAG_LEAF);
        map.put(bn + "WillowTreeFeature", NORMAL_TREE);

        // Alex's Caves (4 tree Feature subclasses; excludes Pewen & Thornwood which place native 3D branches)
        String ac = "com.github.alexmodguy.alexscaves.server.level.feature.";
        map.put(ac + "AncientTreeFeature", NORMAL_TREE);
        map.put(ac + "GiantAncientTreeFeature", GIANT_TREE);
        map.put(ac + "LicorootTreeFeature", NORMAL_NON_TAG_LEAF);
        map.put(ac + "LicorootTreeWithSproutsFeature", NORMAL_NON_TAG_LEAF);

        // Upgrade Aquatic (1 BlueprintTreeFeature subclass)
        map.put("com.teamabnormals.upgrade_aquatic.common.levelgen.feature.RiverTreeFeature", NORMAL_TREE);

        // Oh The Trees You'll Grow (used by Oh The Biomes We've Gone / NBT structure trees)
        String tyg = "dev.corgitaco.ohthetreesyoullgrow.world.level.levelgen.feature.";
        map.put(tyg + "TreeFromStructureNBTFeature", GIANT_TREE);
        map.put(tyg + "TreeFromStructureNBTFeatureV2", GIANT_TREE);

        // Nature's Spirit custom tree feature
        map.put("net.hibiscus.naturespirit.world.feature.JoshuaTreeFeature", NORMAL_TREE);

        // Atmospheric (11 living BlueprintTreeFeature/Feature subclasses)
        String atm = "com.teamabnormals.atmospheric.common.levelgen.feature.";
        map.put(atm + "AspenTreeFeature", NORMAL_TREE);
        map.put(atm + "BabyKousaTreeFeature", NORMAL_TREE);
        map.put(atm + "BabyYuccaTreeFeature", NORMAL_TREE);
        map.put(atm + "CurrantTreeFeature", NORMAL_TREE);
        map.put(atm + "GiantLaurelTreeFeature", GIANT_TREE);
        map.put(atm + "GrimwoodTreeFeature", NORMAL_TREE);
        map.put(atm + "KousaTreeFeature", NORMAL_TREE);
        map.put(atm + "LargeLaurelTreeFeature", GIANT_TREE);
        map.put(atm + "LaurelTreeFeature", NORMAL_TREE);
        map.put(atm + "RainforestTreeFeature", GIANT_TREE);
        map.put(atm + "YuccaTreeFeature", NORMAL_TREE);

        // Environmental (6 living tree features; excludes FallenPineTreeFeature & FallenTallPineTreeFeature)
        String env = "com.teamabnormals.environmental.common.levelgen.feature.";
        map.put(env + "PineTreeFeature", NORMAL_TREE);
        map.put(env + "PineTreeOnStoneFeature", NORMAL_TREE);
        map.put(env + "PlumTreeFeature", NORMAL_TREE);
        map.put(env + "WeepingWillowTreeFeature", NORMAL_TREE);
        map.put(env + "WillowTreePlacerFeature", NORMAL_TREE);
        map.put(env + "WisteriaTreeFeature", NORMAL_TREE);

        // Autumnity (2 maple tree features)
        String aut = "com.teamabnormals.autumnity.common.levelgen.feature.";
        map.put(aut + "FallenLeavesMapleTreeFeature", NORMAL_TREE);
        map.put(aut + "MapleTreeFeature", NORMAL_TREE);

        return Map.copyOf(map);
    }

    /** Fast-path runtime lookup cached per {@link Class}. */
    public static TreeScanProfile profileFor(Class<?> featureClass) {
        if (featureClass == null) {
            return null;
        }
        return PROFILE_CACHE.get(featureClass).orElse(null);
    }

    /** Pure-Java allowlist + auto-compat lookup by FQCN for offline tests and diagnostics. */
    public static TreeScanProfile profileForClassName(String className) {
        if (className == null || className.isEmpty()) {
            return null;
        }
        TreeScanProfile explicit = ALLOWLIST.get(className);
        if (explicit != null) {
            return explicit;
        }
        if (ArborealConfig.autoCompatEnabled()
                && AutoCompatResolver.isAutoCompatCustomTreeFeatureClassName(className)) {
            return NORMAL_TREE;
        }
        return null;
    }

    public static void convertPostPlacedFeature(WorldGenLevel level, BlockPos origin, TreeScanProfile profile) {
        if (!WorldgenGate.enabled() || level == null || origin == null || profile == null) {
            return;
        }
        if (IN_CONVERSION.get()) {
            return;
        }
        IN_CONVERSION.set(Boolean.TRUE);
        try {
            convertPostPlacedFeatureInternal(level, origin, profile);
        } finally {
            IN_CONVERSION.set(Boolean.FALSE);
        }
    }

    private static void convertPostPlacedFeatureInternal(WorldGenLevel level,
                                                         BlockPos origin,
                                                         TreeScanProfile profile) {
        if (level.getBlockState(origin).getBlock() instanceof StaticBranchBlock
                || level.getBlockState(origin.above()).getBlock() instanceof StaticBranchBlock) {
            return;
        }

        BlockPos seedPos = findNearestSeedLog(level, origin, profile.allowCeilingAnchor());
        if (seedPos == null) {
            return;
        }
        BlockState seedState = level.getBlockState(seedPos);
        ResourceId seedLogId = MinecraftResourceIds.fromLocationOrNull(
                BuiltInRegistries.BLOCK.getKey(seedState.getBlock()));
        ResourceId targetBranchId = NativeTreeBranchConverter.resolveBranchForLog(
                level, seedPos, seedState, seedLogId);
        if (targetBranchId == null) {
            return;
        }

        Set<BlockPos> placedLogs = new LinkedHashSet<>();
        Map<Block, Integer> logCounts = new LinkedHashMap<>();
        Set<BlockPos> horizontalAxisLogs = new LinkedHashSet<>();
        int minLogY = Integer.MAX_VALUE;
        int maxLogY = Integer.MIN_VALUE;

        int ox = origin.getX();
        int oy = origin.getY();
        int oz = origin.getZ();
        int minY = oy + profile.minDy();
        int maxY = oy + profile.maxDy();
        int r = profile.xzRadius();

        ArrayDeque<BlockPos> logQueue = new ArrayDeque<>();
        placedLogs.add(seedPos);
        logQueue.add(seedPos);
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        while (!logQueue.isEmpty() && placedLogs.size() < MAX_LOGS) {
            BlockPos cur = logQueue.poll();
            BlockState curState = level.getBlockState(cur);
            logCounts.merge(curState.getBlock(), 1, Integer::sum);
            if (curState.hasProperty(RotatedPillarBlock.AXIS)
                    && curState.getValue(RotatedPillarBlock.AXIS) != Direction.Axis.Y) {
                horizontalAxisLogs.add(cur);
            }
            minLogY = Math.min(minLogY, cur.getY());
            maxLogY = Math.max(maxLogY, cur.getY());

            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        int nx = cur.getX() + dx;
                        int ny = cur.getY() + dy;
                        int nz = cur.getZ() + dz;
                        if (Math.abs(nx - ox) > r || Math.abs(nz - oz) > r || ny < minY || ny > maxY) {
                            continue;
                        }
                        cursor.set(nx, ny, nz);
                        if (!level.ensureCanWrite(cursor)) {
                            continue;
                        }
                        BlockState neighborState = level.getBlockState(cursor);
                        if (!NativeTreeBranchConverter.isRecognizedTreeLog(neighborState)) {
                            continue;
                        }
                        ResourceId neighborLogId = MinecraftResourceIds.fromLocationOrNull(
                                BuiltInRegistries.BLOCK.getKey(neighborState.getBlock()));
                        if (!targetBranchId.equals(NativeTreeBranchConverter.resolveBranchForLog(
                                level, cursor, neighborState, neighborLogId))) {
                            continue;
                        }
                        BlockPos next = cursor.immutable();
                        if (placedLogs.add(next)) {
                            logQueue.add(next);
                            if (placedLogs.size() >= MAX_LOGS) {
                                break;
                            }
                        }
                    }
                    if (placedLogs.size() >= MAX_LOGS) {
                        break;
                    }
                }
                if (placedLogs.size() >= MAX_LOGS) {
                    break;
                }
            }
        }

        if (placedLogs.size() < 4) {
            return;
        }

        // Collect 6-connected foliage within decay distance 6 from placedLogs
        Set<BlockPos> placedLeaves = new LinkedHashSet<>();
        ArrayDeque<BlockPos> leafQueue = new ArrayDeque<>();
        ArrayDeque<Integer> leafDistQueue = new ArrayDeque<>();
        int leafMinY = minLogY - 6;
        int leafMaxY = maxLogY + 6;
        int leafRadius = r + 6;

        for (BlockPos logPos : placedLogs) {
            for (Direction dir : Direction.values()) {
                cursor.setWithOffset(logPos, dir);
                if (placedLeaves.size() >= MAX_LEAVES) {
                    break;
                }
                if (!level.ensureCanWrite(cursor)) {
                    continue;
                }
                BlockState state = level.getBlockState(cursor);
                if (isRecognizedFoliage(state, profile.allowNonTagFoliage())) {
                    BlockPos immutable = cursor.immutable();
                    if (!placedLogs.contains(immutable) && placedLeaves.add(immutable)) {
                        leafQueue.add(immutable);
                        leafDistQueue.add(1);
                    }
                }
            }
            if (placedLeaves.size() >= MAX_LEAVES) {
                break;
            }
        }

        while (!leafQueue.isEmpty() && placedLeaves.size() < MAX_LEAVES) {
            BlockPos cur = leafQueue.poll();
            int dist = leafDistQueue.poll();
            if (dist >= LeafDistance.MAX_SUPPORTED_DISTANCE) {
                continue;
            }
            for (Direction dir : Direction.values()) {
                cursor.setWithOffset(cur, dir);
                int nx = cursor.getX();
                int ny = cursor.getY();
                int nz = cursor.getZ();
                if (Math.abs(nx - ox) > leafRadius || Math.abs(nz - oz) > leafRadius
                        || ny < leafMinY || ny > leafMaxY) {
                    continue;
                }
                if (!level.ensureCanWrite(cursor)) {
                    continue;
                }
                BlockState state = level.getBlockState(cursor);
                if (isRecognizedFoliage(state, profile.allowNonTagFoliage())) {
                    BlockPos immutable = cursor.immutable();
                    if (!placedLogs.contains(immutable) && placedLeaves.add(immutable)) {
                        leafQueue.add(immutable);
                        leafDistQueue.add(dist + 1);
                        if (placedLeaves.size() >= MAX_LEAVES) {
                            break;
                        }
                    }
                }
            }
        }

        if (placedLeaves.isEmpty()) {
            return;
        }

        Block sampleLogBlock = NativeTreeBranchConverter.dominantBlock(logCounts);
        if (sampleLogBlock == null) {
            return;
        }
        ResourceId logId = MinecraftResourceIds.fromLocationOrNull(
                BuiltInRegistries.BLOCK.getKey(sampleLogBlock));
        BlockState rawLeafState = findDominantOrSampleLeafState(level, placedLeaves, profile.allowNonTagFoliage());
        if (rawLeafState == null) {
            return;
        }
        Block sampleLeafBlock = rawLeafState.getBlock();
        BlockState sampleLeafState = rawLeafState.hasProperty(LeavesBlock.PERSISTENT)
                ? rawLeafState.setValue(LeavesBlock.PERSISTENT, false)
                : rawLeafState;
        ResourceId leafId = MinecraftResourceIds.fromLocationOrNull(
                BuiltInRegistries.BLOCK.getKey(sampleLeafBlock));
        if (NativeTreeBranchConverter.isNonBranchingSpecies(logId, leafId)) {
            return;
        }

        BlockPos sampleLogPos = NativeTreeBranchConverter.findSamplePosForBlock(level, placedLogs, sampleLogBlock);
        ResourceId branchId = NativeTreeBranchConverter.resolveBranchForLog(
                level,
                sampleLogPos,
                sampleLogPos != null ? level.getBlockState(sampleLogPos) : sampleLogBlock.defaultBlockState(),
                logId);
        if (branchId == null) {
            return;
        }
        Block resolvedBranch = BuiltInRegistries.BLOCK.get(MinecraftResourceIds.toLocation(branchId));
        if (!(resolvedBranch instanceof StaticBranchBlock branchBlock)) {
            return;
        }

        long seed = SeedMixer.mix(
                SeedMixer.mix(level.getSeed(), ((long) origin.getX() << 32) ^ (origin.getZ() & 0xFFFFFFFFL)),
                origin.getY());

        NativeTreeBranchConverter.ConversionPlan plan;
        if (profile.allowCeilingAnchor() && maxLogY - origin.getY() <= 4 && origin.getY() - minLogY >= 6) {
            plan = NativeTreeBranchConverter.planCeilingConversion(
                    origin, placedLogs, placedLeaves, horizontalAxisLogs, seed);
        } else {
            plan = NativeTreeBranchConverter.planConversion(
                    origin, placedLogs, placedLeaves, horizontalAxisLogs, seed);
        }

        if (plan.branchPositions().isEmpty() && plan.prunedClutterLogs().isEmpty()) {
            return;
        }

        Set<BlockPos> mutableLogs = new LinkedHashSet<>(placedLogs);
        Set<BlockPos> mutableLeaves = new LinkedHashSet<>(placedLeaves);
        NativeTreeBranchConverter.applyConversionPlan(
                level, branchId, branchBlock, sampleLeafState, plan, mutableLogs, mutableLeaves);
        NativeTreeBranchConverter.recomputeLeafDistances(level, mutableLogs, mutableLeaves);
    }

    private static BlockPos findNearestSeedLog(WorldGenLevel level, BlockPos origin, boolean ceilingAnchor) {
        int dyMin = ceilingAnchor ? -4 : -2;
        int dyMax = ceilingAnchor ? 2 : 4;
        BlockPos bestPos = null;
        int bestDistSq = Integer.MAX_VALUE;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int dy = dyMin; dy <= dyMax; dy++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    int distSq = dx * dx + dy * dy + dz * dz;
                    if (distSq >= bestDistSq) {
                        continue;
                    }
                    cursor.set(origin.getX() + dx, origin.getY() + dy, origin.getZ() + dz);
                    if (!level.ensureCanWrite(cursor)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(cursor);
                    if (NativeTreeBranchConverter.isRecognizedTreeLog(state)) {
                        bestDistSq = distSq;
                        bestPos = cursor.immutable();
                    }
                }
            }
        }
        return bestPos;
    }

    private static boolean isRecognizedFoliage(BlockState state, boolean allowNonTagFoliage) {
        if (state == null || state.isAir()) {
            return false;
        }
        if (state.getBlock() instanceof LeavesBlock || state.is(BlockTags.LEAVES)) {
            return true;
        }
        ResourceId id = MinecraftResourceIds.fromLocationOrNull(
                BuiltInRegistries.BLOCK.getKey(state.getBlock()));
        if (id == null) {
            return false;
        }
        if (ArborealConfig.autoCompatEnabled() && AutoCompatResolver.isHeuristicFoliageId(id)) {
            return true;
        }
        if (!allowNonTagFoliage) {
            return false;
        }
        String path = id.path().toLowerCase(Locale.ROOT);
        return path.contains("leaves")
                || path.contains("webbing")
                || path.contains("membrane")
                || path.contains("wart")
                || path.contains("foliage")
                || path.contains("vine")
                || path.contains("crown")
                || path.contains("canopy");
    }

    private static BlockState findDominantOrSampleLeafState(WorldGenLevel level,
                                                            Set<BlockPos> leaves,
                                                            boolean allowNonTagFoliage) {
        BlockState preferred = null;
        for (BlockPos pos : leaves) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof LeavesBlock || state.is(BlockTags.LEAVES)) {
                return state;
            }
            if (preferred == null && isRecognizedFoliage(state, allowNonTagFoliage)) {
                preferred = state;
            }
        }
        return preferred;
    }
}
