package dev.branched.worldgen;

import dev.branched.block.StaticBranchBlock;
import dev.branched.core.ResourceId;
import dev.branched.morphology.BlockPlacement;
import dev.branched.morphology.DeterministicRandom;
import dev.branched.morphology.PlacementRole;
import dev.branched.morphology.SeedMixer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

/**
 * Post-processes vanilla and third-party {@code TreeFeature} placements after trunk
 * and foliage sets are collected so lateral/forking log limbs become 6-way connected
 * {@link StaticBranchBlock}s (including thick bifurcated stems) and unbranched canopy
 * trees gain subtle 3D lateral branches inside foliage, while preserving palms and coconuts.
 */
public final class NativeTreeBranchConverter {
    private static final ResourceId PLACEHOLDER_LOG = ResourceId.parse("minecraft:oak_log");
    private static final ResourceId PLACEHOLDER_BRANCH = ResourceId.parse("branched:oak_branch");
    private static final int UPDATE_FLAGS_KNOWN_SHAPE = Block.UPDATE_ALL | Block.UPDATE_KNOWN_SHAPE;

    private NativeTreeBranchConverter() {
    }

    public record ConversionPlan(Set<BlockPos> keptTrunkLogs,
                                 Set<BlockPos> branchPositions,
                                 Map<BlockPos, Integer> branchThicknesses,
                                 Set<BlockPos> prunedClutterLogs) {
    }

    /** Returns {@code true} for palms, coconuts, bananas, cacti, fungi, bamboos and chorus trunks. */
    public static boolean isNonBranchingSpecies(ResourceId logId, ResourceId leafId) {
        if (logId == null) {
            return true;
        }
        String logPath = logId.path().toLowerCase(Locale.ROOT);
        if (containsAnyToken(logPath,
                "coconut", "palm", "banana", "bamboo", "mushroom", "fungus", "shroom",
                "cactus", "chorus")) {
            return true;
        }
        if (leafId != null) {
            String leafPath = leafId.path().toLowerCase(Locale.ROOT);
            if (containsAnyToken(leafPath,
                    "coconut", "palm", "banana", "frond", "mushroom", "shroom")) {
                return true;
            }
            if (leafPath.contains("wart") && !"betternether".equals(logId.namespace())) {
                return true;
            }
            if (logPath.contains("jungle") && leafPath.contains("azalea")) {
                return true;
            }
        }
        return false;
    }

    /**
     * Pure-Java branch-conversion planner for unit testing and runtime conversion.
     * Separates the straight grounded trunk columns from bifurcating/lateral logs,
     * bridges diagonal limbs into 6-connected paths, or sprouts bounded inner-canopy
     * branches when a straight native trunk has no native limbs.
     */
    public static ConversionPlan planConversion(BlockPos origin,
                                                Set<BlockPos> rawTrunkLogs,
                                                Set<BlockPos> rawLeaves,
                                                Set<BlockPos> horizontalAxisLogs,
                                                long deterministicSeed) {
        if (origin == null || rawTrunkLogs == null || rawTrunkLogs.size() < 4
                || rawLeaves == null || rawLeaves.isEmpty()) {
            return emptyPlan(rawTrunkLogs);
        }

        int minY = Integer.MAX_VALUE;
        int maxY = Integer.MIN_VALUE;
        for (BlockPos pos : rawTrunkLogs) {
            minY = Math.min(minY, pos.getY());
            maxY = Math.max(maxY, pos.getY());
        }
        int height = maxY - minY + 1;
        if (height < 4) {
            return emptyPlan(rawTrunkLogs);
        }

        int baseY = findMainColumnBaseY(rawTrunkLogs, minY, maxY);
        Set<BlockPos> horizontalLogs = horizontalAxisLogs == null ? Set.of() : horizontalAxisLogs;
        Set<Long> baseColumns = detectBaseColumns(origin, rawTrunkLogs, baseY);
        if (baseColumns.isEmpty()) {
            return emptyPlan(rawTrunkLogs);
        }

        // Trace each grounded base column strictly vertically upward
        Map<Long, List<BlockPos>> columnLogs = new LinkedHashMap<>();
        int columnTopY = baseY;
        for (Long colKey : baseColumns) {
            int cx = unpackColX(colKey);
            int cz = unpackColZ(colKey);
            List<BlockPos> vertical = new ArrayList<>();
            for (int y = baseY; y <= maxY; y++) {
                BlockPos p = new BlockPos(cx, y, cz);
                if (!rawTrunkLogs.contains(p)) {
                    break;
                }
                if (y >= baseY + 3 && horizontalLogs.contains(p)) {
                    break;
                }
                vertical.add(p);
                columnTopY = Math.max(columnTopY, y);
            }
            columnLogs.put(colKey, vertical);
        }

        Set<BlockPos> verticalUnion = new LinkedHashSet<>();
        for (List<BlockPos> list : columnLogs.values()) {
            verticalUnion.addAll(list);
        }
        for (BlockPos pos : rawTrunkLogs) {
            if (isLowerRootLog(pos, baseY, origin.getY(), height, rawLeaves)) {
                verticalUnion.add(pos.immutable());
            }
        }
        boolean hasOffColumnLogs = verticalUnion.size() < rawTrunkLogs.size();

        Set<BlockPos> keptTrunkLogs = new LinkedHashSet<>();
        if (baseColumns.size() == 1 && hasOffColumnLogs && columnTopY > baseY + 2) {
            int proportionalCap = baseY + (int) Math.round((maxY - baseY + 1) * 0.68D);
            int maxCoreY = Math.max(baseY + 2, Math.min(columnTopY - 1, proportionalCap));
            for (List<BlockPos> list : columnLogs.values()) {
                for (BlockPos p : list) {
                    if (p.getY() <= maxCoreY) {
                        keptTrunkLogs.add(p);
                    }
                }
            }
            for (BlockPos pos : rawTrunkLogs) {
                if (isLowerRootLog(pos, baseY, origin.getY(), height, rawLeaves)) {
                    keptTrunkLogs.add(pos.immutable());
                }
            }
        } else {
            keptTrunkLogs.addAll(verticalUnion);
        }
        if (keptTrunkLogs.isEmpty()) {
            return emptyPlan(rawTrunkLogs);
        }

        Set<BlockPos> branchCandidates = new LinkedHashSet<>();
        for (BlockPos pos : rawTrunkLogs) {
            if (!keptTrunkLogs.contains(pos)) {
                branchCandidates.add(pos.immutable());
            }
        }

        Set<BlockPos> branchPositions = new LinkedHashSet<>();
        Set<BlockPos> prunedClutterLogs = new LinkedHashSet<>();
        boolean allowMultiTrunkSocket = baseColumns.size() >= 4;
        if (!branchCandidates.isEmpty()) {
            bridgeAndAbsorbCandidates(rawLeaves, keptTrunkLogs, branchCandidates, branchPositions,
                    prunedClutterLogs, allowMultiTrunkSocket);
        } else if (height >= 4 && rawLeaves.size() >= 8) {
            sproutInnerCanopyBranches(minY, maxY, height, keptTrunkLogs, rawLeaves,
                    deterministicSeed, branchPositions);
        }

        if (branchPositions.isEmpty()) {
            return new ConversionPlan(
                    Collections.unmodifiableSet(keptTrunkLogs),
                    Set.of(),
                    Map.of(),
                    Collections.unmodifiableSet(prunedClutterLogs));
        }

        Map<BlockPos, Integer> branchThicknesses = computeThicknessMap(keptTrunkLogs, branchPositions);
        return new ConversionPlan(
                Collections.unmodifiableSet(keptTrunkLogs),
                Collections.unmodifiableSet(branchPositions),
                Collections.unmodifiableMap(branchThicknesses),
                Collections.unmodifiableSet(prunedClutterLogs));
    }

    /**
     * Y-mirrored branch planner for ceiling-anchored trees (such as BetterNether's
     * Anchor Tree) that grow downward from cavern ceilings instead of upward from soil.
     */
    public static ConversionPlan planCeilingConversion(BlockPos origin,
                                                       Set<BlockPos> rawTrunkLogs,
                                                       Set<BlockPos> rawLeaves,
                                                       Set<BlockPos> horizontalAxisLogs,
                                                       long deterministicSeed) {
        if (origin == null || rawTrunkLogs == null || rawTrunkLogs.size() < 4
                || rawLeaves == null || rawLeaves.isEmpty()) {
            return emptyPlan(rawTrunkLogs);
        }
        BlockPos mirroredOrigin = mirrorY(origin);
        Set<BlockPos> mirroredLogs = mirrorYSet(rawTrunkLogs);
        Set<BlockPos> mirroredLeaves = mirrorYSet(rawLeaves);
        Set<BlockPos> mirroredHoriz = horizontalAxisLogs == null ? Set.of() : mirrorYSet(horizontalAxisLogs);
        ConversionPlan mirroredPlan = planConversion(
                mirroredOrigin, mirroredLogs, mirroredLeaves, mirroredHoriz, deterministicSeed);

        Map<BlockPos, Integer> thicknesses = new HashMap<>(mirroredPlan.branchThicknesses().size() * 2);
        for (Map.Entry<BlockPos, Integer> entry : mirroredPlan.branchThicknesses().entrySet()) {
            thicknesses.put(mirrorY(entry.getKey()), entry.getValue());
        }
        return new ConversionPlan(
                Collections.unmodifiableSet(mirrorYSet(mirroredPlan.keptTrunkLogs())),
                Collections.unmodifiableSet(mirrorYSet(mirroredPlan.branchPositions())),
                Collections.unmodifiableMap(thicknesses),
                Collections.unmodifiableSet(mirrorYSet(mirroredPlan.prunedClutterLogs())));
    }

    /**
     * Plans branch conversion when trunk logs and explicit limb candidates are already
     * separated (e.g. wide circular or hollow trunks such as {@code baobabtree}),
     * keeping the trunk intact while converting limbs and preventing 2x2 branch meshes.
     */
    public static ConversionPlan planExplicitBranchConversion(Set<BlockPos> keptTrunkLogs,
                                                              Set<BlockPos> candidateBranches,
                                                              Set<BlockPos> rawLeaves) {
        if (keptTrunkLogs == null || keptTrunkLogs.isEmpty()
                || candidateBranches == null || candidateBranches.isEmpty()
                || rawLeaves == null || rawLeaves.isEmpty()) {
            return emptyPlan(keptTrunkLogs);
        }

        Set<BlockPos> kept = new LinkedHashSet<>(keptTrunkLogs.size());
        for (BlockPos pos : keptTrunkLogs) {
            kept.add(pos.immutable());
        }
        Set<BlockPos> branchCandidates = new LinkedHashSet<>(candidateBranches.size());
        for (BlockPos pos : candidateBranches) {
            BlockPos immutable = pos.immutable();
            if (!kept.contains(immutable)) {
                branchCandidates.add(immutable);
            }
        }
        if (branchCandidates.isEmpty()) {
            return emptyPlan(kept);
        }

        Set<BlockPos> branchPositions = new LinkedHashSet<>();
        Set<BlockPos> prunedClutterLogs = new LinkedHashSet<>();
        bridgeAndAbsorbCandidates(rawLeaves, kept, branchCandidates, branchPositions, prunedClutterLogs, true);

        if (branchPositions.isEmpty()) {
            return new ConversionPlan(
                    Collections.unmodifiableSet(kept),
                    Set.of(),
                    Map.of(),
                    Collections.unmodifiableSet(prunedClutterLogs));
        }

        Map<BlockPos, Integer> branchThicknesses = computeThicknessMap(kept, branchPositions);
        return new ConversionPlan(
                Collections.unmodifiableSet(kept),
                Collections.unmodifiableSet(branchPositions),
                Collections.unmodifiableMap(branchThicknesses),
                Collections.unmodifiableSet(prunedClutterLogs));
    }

    /**
     * Pure-Java helper matching runtime wood sampling: filters non-log blocks (such as
     * dirt from {@code TrunkPlacer.setDirtAt}), selects the most frequent log id, and
     * returns its branch block id only if it has an exact curated bark mapping.
     */
    public static ResourceId selectDominantBranch(Iterable<ResourceId> placedTrunkIds,
                                                    Predicate<ResourceId> isLogId) {
        if (placedTrunkIds == null || isLogId == null) {
            return null;
        }
        Map<ResourceId, Integer> counts = new LinkedHashMap<>();
        for (ResourceId id : placedTrunkIds) {
            if (id != null && isLogId.test(id)) {
                counts.merge(id, 1, Integer::sum);
            }
        }
        ResourceId dominant = null;
        int bestCount = 0;
        for (Map.Entry<ResourceId, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > bestCount) {
                bestCount = entry.getValue();
                dominant = entry.getKey();
            }
        }
        if (dominant == null || !BranchMaterials.hasExactMapping(dominant)) {
            return null;
        }
        return BranchMaterials.resolve(dominant);
    }

    /**
     * Converts lateral/forked logs or adds 3D canopy branches in a live {@link WorldGenLevel}
     * inside {@code TreeFeature.place} / {@code WaterTreeFeature.place} before {@code updateLeaves}
     * recomputes leaf distances.
     */
    public static void convertPlacedTree(WorldGenLevel level,
                                         BlockPos origin,
                                         Set<BlockPos> trunkLogs,
                                         Set<BlockPos> leaves) {
        if (!WorldgenGate.enabled() || level == null || origin == null
                || trunkLogs == null || leaves == null
                || trunkLogs.size() < 4 || leaves.isEmpty()) {
            return;
        }

        Set<BlockPos> realLogPositions = new LinkedHashSet<>(trunkLogs.size());
        Map<Block, Integer> logCounts = new LinkedHashMap<>();
        for (BlockPos pos : trunkLogs) {
            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();
            if (block instanceof StaticBranchBlock) {
                return;
            }
            if (!isRecognizedTreeLog(state)) {
                continue;
            }
            realLogPositions.add(pos.immutable());
            logCounts.merge(block, 1, Integer::sum);
        }
        Block sampleLogBlock = dominantBlock(logCounts);
        if (sampleLogBlock == null || realLogPositions.size() < 4) {
            return;
        }

        BlockState rawLeafState = findSampleLeafState(level, leaves);
        if (rawLeafState == null) {
            return;
        }
        Block sampleLeafBlock = rawLeafState.getBlock();
        BlockState sampleLeafState = rawLeafState.hasProperty(LeavesBlock.PERSISTENT)
                ? rawLeafState.setValue(LeavesBlock.PERSISTENT, false)
                : rawLeafState;

        ResourceId logId = MinecraftResourceIds.fromLocationOrNull(BuiltInRegistries.BLOCK.getKey(sampleLogBlock));
        ResourceId leafId = MinecraftResourceIds.fromLocationOrNull(BuiltInRegistries.BLOCK.getKey(sampleLeafBlock));
        if (isNonBranchingSpecies(logId, leafId) || !BranchMaterials.hasExactMapping(logId)) {
            return;
        }

        ResourceId branchId = BranchMaterials.resolve(logId);
        Block resolvedBranch = BuiltInRegistries.BLOCK.get(MinecraftResourceIds.toLocation(branchId));
        if (!(resolvedBranch instanceof StaticBranchBlock branchBlock)) {
            return;
        }

        Set<BlockPos> horizontalAxisLogs = new LinkedHashSet<>();
        for (BlockPos pos : realLogPositions) {
            BlockState state = level.getBlockState(pos);
            if (state.hasProperty(RotatedPillarBlock.AXIS)
                    && state.getValue(RotatedPillarBlock.AXIS) != Direction.Axis.Y) {
                horizontalAxisLogs.add(pos.immutable());
            }
        }

        long seed = SeedMixer.mix(
                SeedMixer.mix(level.getSeed(), ((long) origin.getX() << 32) ^ (origin.getZ() & 0xFFFFFFFFL)),
                origin.getY());
        ConversionPlan plan = planConversion(origin, realLogPositions, leaves, horizontalAxisLogs, seed);
        applyConversionPlan(level, branchId, branchBlock, sampleLeafState, plan, trunkLogs, leaves);
    }

    /**
     * Converts standing & sapling limbs of {@code baobabtree} trees into connected 3D
     * {@code branched:baobabtree_branch} blocks while preserving tapered/hollow
     * trunks, root systems, and canopy leaf distances.
     */
    public static void convertBaobabTree(WorldGenLevel level,
                                         BlockPos origin,
                                         Set<BlockPos> trunkSnapshot,
                                         Set<BlockPos> allLogs,
                                         Set<BlockPos> leaves) {
        if (!WorldgenGate.enabled() || level == null || origin == null
                || trunkSnapshot == null || trunkSnapshot.isEmpty()
                || allLogs == null || allLogs.isEmpty()
                || leaves == null || leaves.isEmpty()) {
            return;
        }

        Set<BlockPos> realTrunk = new LinkedHashSet<>(trunkSnapshot.size());
        Map<Block, Integer> logCounts = new LinkedHashMap<>();
        for (BlockPos pos : trunkSnapshot) {
            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();
            if (block instanceof StaticBranchBlock) {
                return;
            }
            if (isRecognizedTreeLog(state)) {
                realTrunk.add(pos.immutable());
                logCounts.merge(block, 1, Integer::sum);
            }
        }

        Set<BlockPos> candidateBranches = new LinkedHashSet<>();
        for (BlockPos pos : allLogs) {
            if (trunkSnapshot.contains(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            Block block = state.getBlock();
            if (block instanceof StaticBranchBlock) {
                return;
            }
            if (isRecognizedTreeLog(state)) {
                candidateBranches.add(pos.immutable());
                logCounts.merge(block, 1, Integer::sum);
            }
        }
        if (realTrunk.isEmpty() || candidateBranches.isEmpty()) {
            return;
        }

        Block sampleLogBlock = dominantBlock(logCounts);
        if (sampleLogBlock == null) {
            return;
        }
        ResourceId logId = MinecraftResourceIds.fromLocationOrNull(BuiltInRegistries.BLOCK.getKey(sampleLogBlock));
        if (logId == null || !BranchMaterials.hasExactMapping(logId)) {
            return;
        }

        BlockState rawLeafState = findSampleLeafState(level, leaves);
        if (rawLeafState == null) {
            return;
        }
        Block sampleLeafBlock = rawLeafState.getBlock();
        BlockState sampleLeafState = rawLeafState.hasProperty(LeavesBlock.PERSISTENT)
                ? rawLeafState.setValue(LeavesBlock.PERSISTENT, false)
                : rawLeafState;
        ResourceId leafId = MinecraftResourceIds.fromLocationOrNull(BuiltInRegistries.BLOCK.getKey(sampleLeafBlock));
        if (isNonBranchingSpecies(logId, leafId)) {
            return;
        }

        ResourceId branchId = BranchMaterials.resolve(logId);
        Block resolvedBranch = BuiltInRegistries.BLOCK.get(MinecraftResourceIds.toLocation(branchId));
        if (!(resolvedBranch instanceof StaticBranchBlock branchBlock)) {
            return;
        }

        ConversionPlan plan = planExplicitBranchConversion(realTrunk, candidateBranches, leaves);
        applyConversionPlan(level, branchId, branchBlock, sampleLeafState, plan, allLogs, leaves);
    }

    static void applyConversionPlan(WorldGenLevel level,
                                    ResourceId branchId,
                                    StaticBranchBlock branchBlock,
                                    BlockState sampleLeafState,
                                    ConversionPlan plan,
                                    Set<BlockPos> mutableLogs,
                                    Set<BlockPos> mutableLeaves) {
        if (plan.branchPositions().isEmpty() && plan.prunedClutterLogs().isEmpty()) {
            return;
        }

        for (BlockPos clutterPos : plan.prunedClutterLogs()) {
            if (!level.ensureCanWrite(clutterPos) || !isRecognizedTreeLog(level.getBlockState(clutterPos))) {
                continue;
            }
            BlockPos immutable = clutterPos.immutable();
            mutableLogs.remove(immutable);
            if (sampleLeafState != null
                    && sampleLeafState.getBlock() instanceof LeavesBlock
                    && hasNeighborIn(immutable, mutableLeaves)) {
                level.setBlock(immutable, sampleLeafState, UPDATE_FLAGS_KNOWN_SHAPE);
                mutableLeaves.add(immutable);
            } else {
                level.setBlock(immutable, Blocks.AIR.defaultBlockState(), UPDATE_FLAGS_KNOWN_SHAPE);
            }
        }

        if (plan.branchPositions().isEmpty()) {
            return;
        }

        Set<Long> trunkKeys = new LinkedHashSet<>(plan.keptTrunkLogs().size() * 2);
        for (BlockPos pos : plan.keptTrunkLogs()) {
            trunkKeys.add(LeafDistance.key(pos.getX(), pos.getY(), pos.getZ()));
        }

        Map<Long, BlockPlacement> branchMap = new LinkedHashMap<>(plan.branchPositions().size() * 2);
        for (BlockPos pos : plan.branchPositions()) {
            branchMap.put(
                    LeafDistance.key(pos.getX(), pos.getY(), pos.getZ()),
                    new BlockPlacement(pos.getX(), pos.getY(), pos.getZ(), branchId, PlacementRole.BRANCH));
        }

        Set<Long> leafKeys = new LinkedHashSet<>(mutableLeaves.size() * 2);
        for (BlockPos pos : mutableLeaves) {
            if (!plan.branchPositions().contains(pos)) {
                leafKeys.add(LeafDistance.key(pos.getX(), pos.getY(), pos.getZ()));
            }
        }

        for (BlockPos pos : plan.branchPositions()) {
            if (!level.ensureCanWrite(pos) || !canReplaceForBranch(level.getBlockState(pos))) {
                continue;
            }
            BlockPlacement placement = branchMap.get(LeafDistance.key(pos.getX(), pos.getY(), pos.getZ()));
            int thickness = plan.branchThicknesses().getOrDefault(pos, 2);
            BlockState connected = WorldGenTreeWriter.connectBranchState(
                    branchBlock.defaultBlockState(), placement, trunkKeys, branchMap, leafKeys, thickness);
            level.setBlock(pos, connected, UPDATE_FLAGS_KNOWN_SHAPE);
            BlockPos immutable = pos.immutable();
            mutableLogs.add(immutable);
            mutableLeaves.remove(immutable);
        }
    }

    static void recomputeLeafDistances(WorldGenLevel level,
                                       Set<BlockPos> woodPositions,
                                       Set<BlockPos> leafPositions) {
        if (level == null || woodPositions == null || woodPositions.isEmpty()
                || leafPositions == null || leafPositions.isEmpty()) {
            return;
        }
        List<BlockPlacement> placements = new ArrayList<>(woodPositions.size() + leafPositions.size());
        for (BlockPos pos : woodPositions) {
            placements.add(new BlockPlacement(
                    pos.getX(), pos.getY(), pos.getZ(), PLACEHOLDER_LOG, PlacementRole.LOG));
        }
        for (BlockPos pos : leafPositions) {
            placements.add(new BlockPlacement(
                    pos.getX(), pos.getY(), pos.getZ(), PLACEHOLDER_BRANCH, PlacementRole.LEAVES));
        }
        Map<Long, Integer> distances = LeafDistance.distances(placements);
        int decayDistance = LeafDistance.MAX_SUPPORTED_DISTANCE + 1;
        for (BlockPos pos : leafPositions) {
            if (!level.ensureCanWrite(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!state.hasProperty(LeavesBlock.DISTANCE)) {
                continue;
            }
            if (state.hasProperty(LeavesBlock.PERSISTENT) && state.getValue(LeavesBlock.PERSISTENT)) {
                continue;
            }
            Integer computed = distances.get(LeafDistance.key(pos.getX(), pos.getY(), pos.getZ()));
            int targetDist = computed == null ? decayDistance : Math.max(1, Math.min(decayDistance, computed));
            if (state.getValue(LeavesBlock.DISTANCE) != targetDist) {
                level.setBlock(pos, state.setValue(LeavesBlock.DISTANCE, targetDist), UPDATE_FLAGS_KNOWN_SHAPE);
            }
        }
    }

    static boolean isRecognizedTreeLog(BlockState state) {
        if (state == null || state.getBlock() instanceof StaticBranchBlock) {
            return false;
        }
        if (state.is(BlockTags.LOGS)) {
            return true;
        }
        ResourceId id = MinecraftResourceIds.fromLocationOrNull(
                BuiltInRegistries.BLOCK.getKey(state.getBlock()));
        return id != null && BranchMaterials.hasExactMapping(id);
    }

    static boolean canReplaceForBranch(BlockState state) {
        if (state == null) {
            return false;
        }
        if (state.getBlock() instanceof LeavesBlock
                || state.is(BlockTags.LEAVES)
                || isRecognizedTreeLog(state)) {
            return true;
        }
        ResourceId id = MinecraftResourceIds.fromLocationOrNull(
                BuiltInRegistries.BLOCK.getKey(state.getBlock()));
        if (id != null && "regions_unexplored".equals(id.namespace())
                && (id.path().endsWith("_branch") || id.path().endsWith("_beard"))) {
            return true;
        }
        if (!state.getFluidState().isEmpty()) {
            return false;
        }
        return state.isAir() || state.canBeReplaced() || state.is(BlockTags.REPLACEABLE);
    }

    static Block dominantBlock(Map<Block, Integer> logCounts) {
        Block best = null;
        int bestCount = 0;
        for (Map.Entry<Block, Integer> entry : logCounts.entrySet()) {
            if (entry.getValue() > bestCount) {
                bestCount = entry.getValue();
                best = entry.getKey();
            }
        }
        return best;
    }

    private static BlockState findSampleLeafState(WorldGenLevel level, Set<BlockPos> leaves) {
        BlockState fallback = null;
        for (BlockPos pos : leaves) {
            BlockState state = level.getBlockState(pos);
            if (state.getBlock() instanceof LeavesBlock || state.is(BlockTags.LEAVES)) {
                return state;
            }
            if (fallback == null && !state.isAir()) {
                fallback = state;
            }
        }
        return fallback;
    }

    private static int findMainColumnBaseY(Set<BlockPos> rawTrunkLogs, int minY, int maxY) {
        for (int y = minY; y <= maxY - 2; y++) {
            for (BlockPos pos : rawTrunkLogs) {
                if (pos.getY() == y
                        && rawTrunkLogs.contains(pos.above())
                        && rawTrunkLogs.contains(pos.above(2))) {
                    return y;
                }
            }
        }
        return minY;
    }

    private static boolean isLowerRootLog(BlockPos pos,
                                          int baseY,
                                          int originY,
                                          int height,
                                          Set<BlockPos> rawLeaves) {
        if (pos.getY() < baseY) {
            return true;
        }
        return height >= 7
                && pos.getY() <= Math.max(baseY + 1, originY)
                && !rawLeaves.contains(pos.above());
    }

    private static ConversionPlan emptyPlan(Set<BlockPos> rawTrunkLogs) {
        Set<BlockPos> kept = rawTrunkLogs == null ? Set.of() : Set.copyOf(rawTrunkLogs);
        return new ConversionPlan(kept, Set.of(), Map.of(), Set.of());
    }

    private static boolean containsAnyToken(String value, String... tokens) {
        for (String token : tokens) {
            if (value.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private static Set<Long> detectBaseColumns(BlockPos origin, Set<BlockPos> rawTrunkLogs, int minY) {
        List<BlockPos> minLayer = new ArrayList<>();
        for (BlockPos pos : rawTrunkLogs) {
            if (pos.getY() == minY) {
                minLayer.add(pos);
            }
        }
        if (minLayer.isEmpty()) {
            return Set.of();
        }

        List<BlockPos> nearOrigin = new ArrayList<>();
        for (BlockPos pos : minLayer) {
            if (Math.abs(pos.getX() - origin.getX()) <= 1 && Math.abs(pos.getZ() - origin.getZ()) <= 1) {
                nearOrigin.add(pos);
            }
        }
        List<BlockPos> seedGroup = nearOrigin.isEmpty() ? minLayer : nearOrigin;
        BlockPos anchor = seedGroup.stream()
                .min(Comparator.comparingInt(p -> distSq2D(p, origin)))
                .orElse(seedGroup.get(0));

        Set<BlockPos> minLayerSet = new LinkedHashSet<>(minLayer.size() * 2);
        for (BlockPos pos : minLayer) {
            minLayerSet.add(pos.immutable());
        }
        BlockPos anchorImmutable = anchor.immutable();
        List<BlockPos> footprint = new ArrayList<>();
        Set<BlockPos> visited = new LinkedHashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        visited.add(anchorImmutable);
        queue.add(anchorImmutable);
        while (!queue.isEmpty()) {
            BlockPos cur = queue.poll();
            footprint.add(cur);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dz == 0) {
                        continue;
                    }
                    BlockPos next = cur.offset(dx, 0, dz);
                    if (Math.abs(next.getX() - anchorImmutable.getX()) <= 4
                            && Math.abs(next.getZ() - anchorImmutable.getZ()) <= 4
                            && minLayerSet.contains(next)
                            && visited.add(next)) {
                        queue.add(next);
                    }
                }
            }
        }
        if (footprint.size() > 1) {
            List<BlockPos> tallColumns = new ArrayList<>();
            for (BlockPos base : footprint) {
                boolean hasColHeight = rawTrunkLogs.contains(base.above())
                        && rawTrunkLogs.contains(base.above(2));
                if (hasColHeight) {
                    tallColumns.add(base);
                }
            }
            if (!tallColumns.isEmpty()) {
                footprint = tallColumns;
            } else {
                List<BlockPos> twoTallColumns = new ArrayList<>();
                for (BlockPos base : footprint) {
                    if (rawTrunkLogs.contains(base.above())) {
                        twoTallColumns.add(base);
                    }
                }
                footprint = twoTallColumns.isEmpty() ? List.of(anchorImmutable) : twoTallColumns;
            }
        }

        Set<Long> cols = new LinkedHashSet<>();
        for (BlockPos base : footprint) {
            cols.add(packCol(base.getX(), base.getZ()));
        }
        return cols;
    }

    private static void bridgeAndAbsorbCandidates(Set<BlockPos> rawLeaves,
                                                  Set<BlockPos> keptTrunkLogs,
                                                  Set<BlockPos> branchCandidates,
                                                  Set<BlockPos> branchPositions,
                                                  Set<BlockPos> prunedClutterLogs,
                                                  boolean allowMultiTrunkSocket) {
        Set<BlockPos> connected = new LinkedHashSet<>(keptTrunkLogs);
        Set<BlockPos> remaining = new LinkedHashSet<>(branchCandidates);

        absorbSixConnected(connected, remaining, keptTrunkLogs, branchPositions, prunedClutterLogs, allowMultiTrunkSocket);

        while (!remaining.isEmpty()) {
            BlockPos bestAnchor = null;
            BlockPos bestTarget = null;
            int bestDistSq = Integer.MAX_VALUE;

            for (BlockPos target : remaining) {
                for (BlockPos anchor : connected) {
                    int chebyshev = Math.max(Math.abs(target.getX() - anchor.getX()),
                            Math.max(Math.abs(target.getY() - anchor.getY()),
                                    Math.abs(target.getZ() - anchor.getZ())));
                    if (chebyshev > 2) {
                        continue;
                    }
                    int dx = target.getX() - anchor.getX();
                    int dy = target.getY() - anchor.getY();
                    int dz = target.getZ() - anchor.getZ();
                    int d2 = dx * dx + dy * dy + dz * dz;
                    if (d2 < bestDistSq) {
                        bestDistSq = d2;
                        bestAnchor = anchor;
                        bestTarget = target;
                    }
                }
            }

            if (bestAnchor == null || bestTarget == null) {
                for (BlockPos rem : remaining) {
                    BlockPos immutable = rem.immutable();
                    if (hasNeighborIn(immutable, rawLeaves)
                            || hasNeighborIn(immutable, prunedClutterLogs)
                            || hasNeighborIn(immutable, branchPositions)) {
                        prunedClutterLogs.add(immutable);
                    } else {
                        keptTrunkLogs.add(immutable);
                    }
                }
                remaining.clear();
                break;
            }

            List<BlockPos> addedInBridge = new ArrayList<>();
            boolean valid = true;
            BlockPos cursor = bestAnchor;
            while (!cursor.equals(bestTarget)) {
                cursor = pickNextBridgeStep(cursor, bestTarget, rawLeaves, keptTrunkLogs, branchPositions);
                if (keptTrunkLogs.contains(cursor) || branchPositions.contains(cursor)) {
                    continue;
                }
                BlockPos immutable = cursor.immutable();
                if (!canAddBranchPos(immutable, keptTrunkLogs, branchPositions, allowMultiTrunkSocket)) {
                    valid = false;
                    break;
                }
                branchPositions.add(immutable);
                connected.add(immutable);
                addedInBridge.add(immutable);
            }
            if (!valid) {
                branchPositions.removeAll(addedInBridge);
                connected.removeAll(addedInBridge);
                remaining.remove(bestTarget);
                prunedClutterLogs.add(bestTarget.immutable());
            } else {
                remaining.removeAll(addedInBridge);
                remaining.remove(bestTarget);
                absorbSixConnected(connected, remaining, keptTrunkLogs, branchPositions, prunedClutterLogs, allowMultiTrunkSocket);
            }
        }
    }

    private static void absorbSixConnected(Set<BlockPos> connected,
                                           Set<BlockPos> remaining,
                                           Set<BlockPos> keptTrunkLogs,
                                           Set<BlockPos> branchPositions,
                                           Set<BlockPos> prunedClutterLogs,
                                           boolean allowMultiTrunkSocket) {
        ArrayDeque<BlockPos> queue = new ArrayDeque<>(connected);
        while (!queue.isEmpty()) {
            BlockPos cur = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos next = cur.relative(dir);
                if (remaining.remove(next)) {
                    BlockPos immutable = next.immutable();
                    if (canAddBranchPos(immutable, keptTrunkLogs, branchPositions, allowMultiTrunkSocket)) {
                        branchPositions.add(immutable);
                        connected.add(immutable);
                        queue.add(immutable);
                    } else {
                        prunedClutterLogs.add(immutable);
                    }
                }
            }
        }
    }

    private static BlockPos pickNextBridgeStep(BlockPos from,
                                               BlockPos target,
                                               Set<BlockPos> rawLeaves,
                                               Set<BlockPos> keptTrunkLogs,
                                               Set<BlockPos> branchPositions) {
        int dx = Integer.compare(target.getX(), from.getX());
        int dy = Integer.compare(target.getY(), from.getY());
        int dz = Integer.compare(target.getZ(), from.getZ());

        List<BlockPos> candidates = new ArrayList<>(3);
        if (dx != 0) candidates.add(from.offset(dx, 0, 0));
        if (dz != 0) candidates.add(from.offset(0, 0, dz));
        if (dy != 0) candidates.add(from.offset(0, dy, 0));

        boolean forkAtTrunkTop = keptTrunkLogs.contains(from) && !keptTrunkLogs.contains(from.above()) && dy > 0;
        BlockPos best = candidates.get(0);
        int bestScore = Integer.MIN_VALUE;
        for (BlockPos cand : candidates) {
            if (cand.equals(target)) {
                return cand;
            }
            int score = 0;
            if (!keptTrunkLogs.contains(cand)) score += 100;
            if (branchPositions.contains(cand)) score += 50;
            if (rawLeaves.contains(cand)) score += 20;
            if (forkAtTrunkTop && cand.equals(from.above())) {
                score += 15;
            } else if (!forkAtTrunkTop && cand.getY() == from.getY()) {
                score += 10; // Horizontal elbow first from trunk side/parent limb
            }
            if (score > bestScore) {
                bestScore = score;
                best = cand;
            }
        }
        return best;
    }

    private static void sproutInnerCanopyBranches(int minY,
                                                  int maxY,
                                                  int height,
                                                  Set<BlockPos> keptTrunkLogs,
                                                  Set<BlockPos> rawLeaves,
                                                  long seed,
                                                  Set<BlockPos> branchPositions) {
        int lowBound = Math.min(maxY - 1, minY + Math.max(2, (int) Math.round(height * 0.38D)));
        int highBound = maxY - 1;
        if (highBound < lowBound) {
            return;
        }

        record SproutSite(int y, Direction dir, BlockPos[] positions) {
        }

        List<SproutSite> validSites = new ArrayList<>();
        Direction[] cardinals = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};
        for (BlockPos trunkPos : keptTrunkLogs) {
            int y = trunkPos.getY();
            if (y < lowBound || y > highBound) {
                continue;
            }
            for (Direction dir : cardinals) {
                BlockPos arm1 = trunkPos.relative(dir);
                if (!keptTrunkLogs.contains(arm1)
                        && rawLeaves.contains(arm1.above())
                        && (rawLeaves.contains(arm1)
                        || (height >= 5 && y >= minY + 2 && hasHorizontalNeighborIn(arm1, rawLeaves)))) {
                    BlockPos arm2 = arm1.relative(dir);
                    if (y <= maxY - 2 && !keptTrunkLogs.contains(arm2)
                            && rawLeaves.contains(arm2) && rawLeaves.contains(arm2.above())) {
                        validSites.add(new SproutSite(y, dir, new BlockPos[]{arm1.immutable(), arm2.immutable()}));
                    } else {
                        validSites.add(new SproutSite(y, dir, new BlockPos[]{arm1.immutable()}));
                    }
                }
            }
        }
        if (validSites.isEmpty()) {
            return;
        }

        DeterministicRandom rng = new DeterministicRandom(seed);
        for (int i = validSites.size() - 1; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            SproutSite tmp = validSites.get(i);
            validSites.set(i, validSites.get(j));
            validSites.set(j, tmp);
        }

        int desired = height <= 4 ? 2 : Math.min(4, 3 + rng.nextInt(2));
        int placedSites = 0;
        Set<Direction> usedDirs = new LinkedHashSet<>();
        Map<Integer, Integer> perHeightCount = new HashMap<>();

        // Pass 1: distinct horizontal compass directions (up to 2 opposite arms per Y level)
        for (SproutSite site : validSites) {
            if (placedSites >= desired) {
                break;
            }
            if (usedDirs.contains(site.dir()) || perHeightCount.getOrDefault(site.y(), 0) >= 2) {
                continue;
            }
            if (tryAddSproutSite(site.positions(), keptTrunkLogs, branchPositions)) {
                usedDirs.add(site.dir());
                perHeightCount.merge(site.y(), 1, Integer::sum);
                placedSites++;
            }
        }

        // Pass 2: fill up to desired across different heights / remaining cardinal slots
        if (placedSites < Math.min(validSites.size(), desired)) {
            for (SproutSite site : validSites) {
                if (placedSites >= desired) {
                    break;
                }
                if (perHeightCount.getOrDefault(site.y(), 0) >= 2) {
                    continue;
                }
                if (tryAddSproutSite(site.positions(), keptTrunkLogs, branchPositions)) {
                    perHeightCount.merge(site.y(), 1, Integer::sum);
                    placedSites++;
                }
            }
        }

        // Fallback if height-cap prevented any placement
        if (branchPositions.isEmpty()) {
            for (SproutSite site : validSites) {
                if (placedSites >= desired) {
                    break;
                }
                if (tryAddSproutSite(site.positions(), keptTrunkLogs, branchPositions)) {
                    placedSites++;
                }
            }
        }
    }

    private static boolean tryAddSproutSite(BlockPos[] site,
                                            Set<BlockPos> keptTrunkLogs,
                                            Set<BlockPos> branchPositions) {
        List<BlockPos> added = new ArrayList<>(site.length);
        for (BlockPos step : site) {
            if (!canAddBranchPos(step, keptTrunkLogs, branchPositions)) {
                branchPositions.removeAll(added);
                return false;
            }
            branchPositions.add(step);
            added.add(step);
        }
        return true;
    }

    private static boolean hasNeighborIn(BlockPos pos, Set<BlockPos> set) {
        for (Direction dir : Direction.values()) {
            if (set.contains(pos.relative(dir))) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasHorizontalNeighborIn(BlockPos pos, Set<BlockPos> set) {
        for (Direction dir : Direction.Plane.HORIZONTAL) {
            if (set.contains(pos.relative(dir))) {
                return true;
            }
        }
        return false;
    }

    private static BlockPos mirrorY(BlockPos pos) {
        return new BlockPos(pos.getX(), -pos.getY(), pos.getZ());
    }

    private static Set<BlockPos> mirrorYSet(Set<BlockPos> positions) {
        Set<BlockPos> mirrored = new LinkedHashSet<>(positions.size() * 2);
        for (BlockPos pos : positions) {
            mirrored.add(mirrorY(pos));
        }
        return mirrored;
    }

    private static boolean isWood(BlockPos pos, Set<BlockPos> keptTrunkLogs, Set<BlockPos> branchPositions) {
        return keptTrunkLogs.contains(pos) || branchPositions.contains(pos);
    }

    private static boolean formsWood2x2Square(BlockPos pos,
                                               Set<BlockPos> keptTrunkLogs,
                                               Set<BlockPos> branchPositions) {
        int[] offsets = {-1, 1};
        for (int dx : offsets) {
            for (int dz : offsets) {
                if (isWood(pos.offset(dx, 0, 0), keptTrunkLogs, branchPositions)
                        && isWood(pos.offset(0, 0, dz), keptTrunkLogs, branchPositions)
                        && isWood(pos.offset(dx, 0, dz), keptTrunkLogs, branchPositions)) {
                    return true;
                }
            }
        }
        for (int dx : offsets) {
            for (int dy : offsets) {
                if (isWood(pos.offset(dx, 0, 0), keptTrunkLogs, branchPositions)
                        && isWood(pos.offset(0, dy, 0), keptTrunkLogs, branchPositions)
                        && isWood(pos.offset(dx, dy, 0), keptTrunkLogs, branchPositions)) {
                    return true;
                }
            }
        }
        for (int dy : offsets) {
            for (int dz : offsets) {
                if (isWood(pos.offset(0, dy, 0), keptTrunkLogs, branchPositions)
                        && isWood(pos.offset(0, 0, dz), keptTrunkLogs, branchPositions)
                        && isWood(pos.offset(0, dy, dz), keptTrunkLogs, branchPositions)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean formsBranchInvolved2x2Square(BlockPos pos,
                                                         Set<BlockPos> keptTrunkLogs,
                                                         Set<BlockPos> branchPositions) {
        int[] offsets = {-1, 1};
        for (int dx : offsets) {
            for (int dz : offsets) {
                BlockPos a = pos.offset(dx, 0, 0);
                BlockPos b = pos.offset(0, 0, dz);
                BlockPos c = pos.offset(dx, 0, dz);
                if (isWood(a, keptTrunkLogs, branchPositions)
                        && isWood(b, keptTrunkLogs, branchPositions)
                        && isWood(c, keptTrunkLogs, branchPositions)
                        && (branchPositions.contains(a) || branchPositions.contains(b) || branchPositions.contains(c))) {
                    return true;
                }
            }
        }
        for (int dx : offsets) {
            for (int dy : offsets) {
                BlockPos a = pos.offset(dx, 0, 0);
                BlockPos b = pos.offset(0, dy, 0);
                BlockPos c = pos.offset(dx, dy, 0);
                if (isWood(a, keptTrunkLogs, branchPositions)
                        && isWood(b, keptTrunkLogs, branchPositions)
                        && isWood(c, keptTrunkLogs, branchPositions)
                        && (branchPositions.contains(a) || branchPositions.contains(b) || branchPositions.contains(c))) {
                    return true;
                }
            }
        }
        for (int dy : offsets) {
            for (int dz : offsets) {
                BlockPos a = pos.offset(0, dy, 0);
                BlockPos b = pos.offset(0, 0, dz);
                BlockPos c = pos.offset(0, dy, dz);
                if (isWood(a, keptTrunkLogs, branchPositions)
                        && isWood(b, keptTrunkLogs, branchPositions)
                        && isWood(c, keptTrunkLogs, branchPositions)
                        && (branchPositions.contains(a) || branchPositions.contains(b) || branchPositions.contains(c))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean canAddBranchPos(BlockPos pos,
                                           Set<BlockPos> keptTrunkLogs,
                                           Set<BlockPos> branchPositions) {
        return canAddBranchPos(pos, keptTrunkLogs, branchPositions, false);
    }

    private static boolean canAddBranchPos(BlockPos pos,
                                           Set<BlockPos> keptTrunkLogs,
                                           Set<BlockPos> branchPositions,
                                           boolean allowMultiTrunkSocket) {
        if (keptTrunkLogs.contains(pos) || branchPositions.contains(pos)) {
            return false;
        }
        int trunkNeighbors = 0;
        int branchNeighbors = 0;
        for (Direction dir : Direction.values()) {
            BlockPos adj = pos.relative(dir);
            if (keptTrunkLogs.contains(adj)) {
                trunkNeighbors++;
            } else if (branchPositions.contains(adj)) {
                branchNeighbors++;
            }
        }
        if (allowMultiTrunkSocket && branchNeighbors == 0 && trunkNeighbors >= 1) {
            return !formsBranchInvolved2x2Square(pos, keptTrunkLogs, branchPositions);
        }
        if (formsWood2x2Square(pos, keptTrunkLogs, branchPositions)) {
            return false;
        }
        return trunkNeighbors + branchNeighbors == 1;
    }

    private static Map<BlockPos, Integer> computeThicknessMap(Set<BlockPos> keptTrunkLogs,
                                                              Set<BlockPos> branchPositions) {
        List<BlockPlacement> placements = new ArrayList<>(keptTrunkLogs.size() + branchPositions.size());
        for (BlockPos pos : keptTrunkLogs) {
            placements.add(new BlockPlacement(pos.getX(), pos.getY(), pos.getZ(),
                    PLACEHOLDER_LOG, PlacementRole.LOG));
        }
        for (BlockPos pos : branchPositions) {
            placements.add(new BlockPlacement(pos.getX(), pos.getY(), pos.getZ(),
                    PLACEHOLDER_BRANCH, PlacementRole.BRANCH));
        }
        Map<Long, Integer> packedThicknesses = WorldGenTreeWriter.computeBranchThicknesses(placements);
        Map<BlockPos, Integer> result = new HashMap<>(branchPositions.size() * 2);
        for (BlockPos pos : branchPositions) {
            int t = packedThicknesses.getOrDefault(
                    LeafDistance.key(pos.getX(), pos.getY(), pos.getZ()), 2);
            result.put(pos, t);
        }
        return result;
    }

    private static int distSq2D(BlockPos a, BlockPos b) {
        int dx = a.getX() - b.getX();
        int dz = a.getZ() - b.getZ();
        return dx * dx + dz * dz;
    }

    private static long packCol(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }

    private static int unpackColX(long key) {
        return (int) (key >> 32);
    }

    private static int unpackColZ(long key) {
        return (int) key;
    }
}
