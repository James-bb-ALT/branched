package dev.branched.worldgen;

import dev.branched.Branched;
import dev.branched.block.StaticBranchBlock;
import dev.branched.core.ResourceId;
import dev.branched.morphology.BlockPalette;
import dev.branched.morphology.BlockPlacement;
import dev.branched.morphology.PlacementRole;
import dev.branched.morphology.RasterizedTree;
import dev.branched.morphology.SeedMixer;
import dev.branched.species.SpeciesProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.VineBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Writes a validated rasterized tree into a worldgen region using 6-way connected
 * {@link StaticBranchBlock} states, precomputed vanilla leaf distances, optional
 * deterministic vines/ground cover and transactional rollback on partial failure.
 */
public final class WorldGenTreeWriter {
    private static final Logger LOGGER = Branched.LOGGER;
    private static final int WRITE_FLAGS = Block.UPDATE_CLIENTS;

    private WorldGenTreeWriter() {
    }

    /** Returns the number of blocks written, or {@code -1} after a rollback. */
    public static int write(WorldGenLevel level, RasterizedTree tree,
                            SpeciesProfile profile, BlockPalette palette) {
        Block logBlock = resolve(profile.log());
        Block leavesBlock = resolve(profile.leaves());
        Block branchBlock = resolve(palette.branch());
        if (logBlock == null || leavesBlock == null || branchBlock == null) {
            LOGGER.warn("Branched cannot resolve blocks for {} (log={}, leaves={}, branch={})",
                    profile.id(), profile.log(), profile.leaves(), palette.branch());
            return 0;
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        List<BlockPlacement> placements = filterSoftBlocks(level, cursor, tree.blocks());
        if (placements.isEmpty()) {
            return 0;
        }

        Map<Long, Integer> leafDistances = LeafDistance.distances(placements);
        Set<Long> trunkKeys = new HashSet<>();
        Map<Long, BlockPlacement> branches = new HashMap<>();
        Set<Long> leafKeys = new HashSet<>();
        for (BlockPlacement block : placements) {
            long k = LeafDistance.key(block.x(), block.y(), block.z());
            if (block.role() == PlacementRole.LOG) {
                trunkKeys.add(k);
            } else if (block.role() == PlacementRole.BRANCH) {
                branches.put(k, block);
            } else if (block.role() == PlacementRole.LEAVES && leafDistances.containsKey(k)) {
                leafKeys.add(k);
            }
        }
        Map<Long, Integer> branchThicknesses = computeBranchThicknesses(placements);

        BlockPlacement[] ordered = placements.toArray(new BlockPlacement[0]);
        int[] writtenIndices = new int[ordered.length];
        BlockState[] previous = new BlockState[ordered.length];
        int written = 0;

        for (int i = 0; i < ordered.length; i++) {
            BlockPlacement placement = ordered[i];
            BlockState state = stateFor(placement, logBlock, leavesBlock, branchBlock,
                    leafDistances, trunkKeys, branches, leafKeys, branchThicknesses);
            if (state == null) {
                continue;
            }
            cursor.set(placement.x(), placement.y(), placement.z());
            previous[i] = level.getBlockState(cursor);
            if (!level.setBlock(cursor, state, WRITE_FLAGS)) {
                rollback(level, ordered, writtenIndices, previous, written);
                LOGGER.warn("Branched rolled back {} at {} {} {} ({} blocks restored)",
                        profile.id(), placement.x(), placement.y(), placement.z(), written);
                return -1;
            }
            writtenIndices[written++] = i;
        }

        if (written > 0) {
            applyDeterministicDecorators(level, cursor, profile, placements, leafKeys);
        }
        return written;
    }

    private static List<BlockPlacement> filterSoftBlocks(WorldGenLevel level,
                                                         BlockPos.MutableBlockPos cursor,
                                                         List<BlockPlacement> blocks) {
        Set<Long> trunkKeys = new HashSet<>();
        for (BlockPlacement block : blocks) {
            if (block.role() == PlacementRole.LOG) {
                trunkKeys.add(LeafDistance.key(block.x(), block.y(), block.z()));
            }
        }

        List<BlockPlacement> logs = new ArrayList<>();
        List<BlockPlacement> leaves = new ArrayList<>();
        Map<Long, BlockPlacement> branchCandidates = new LinkedHashMap<>();
        for (BlockPlacement block : blocks) {
            if (block.role() == PlacementRole.LOG) {
                logs.add(block);
                continue;
            }
            if (!canPlaceSoft(level, cursor, block)) {
                continue;
            }
            if (block.role() == PlacementRole.BRANCH) {
                branchCandidates.put(LeafDistance.key(block.x(), block.y(), block.z()), block);
            } else if (block.role() == PlacementRole.LEAVES) {
                leaves.add(block);
            }
        }

        Set<Long> connected = connectedBranches(branchCandidates, trunkKeys);
        List<BlockPlacement> kept = new ArrayList<>(logs.size() + connected.size() + leaves.size());
        kept.addAll(logs);
        for (Map.Entry<Long, BlockPlacement> entry : branchCandidates.entrySet()) {
            if (connected.contains(entry.getKey())) {
                kept.add(entry.getValue());
            }
        }
        kept.addAll(leaves);
        return kept;
    }

    private static Set<Long> connectedBranches(Map<Long, BlockPlacement> branches,
                                               Set<Long> trunkKeys) {
        Set<Long> connected = new HashSet<>();
        if (branches.isEmpty()) {
            return connected;
        }
        ArrayDeque<BlockPlacement> queue = new ArrayDeque<>();
        for (Map.Entry<Long, BlockPlacement> entry : branches.entrySet()) {
            BlockPlacement branch = entry.getValue();
            if (touchesTrunk(branch, trunkKeys)) {
                connected.add(entry.getKey());
                queue.add(branch);
            }
        }
        while (!queue.isEmpty()) {
            BlockPlacement branch = queue.poll();
            for (Direction direction : Direction.values()) {
                long neighbour = LeafDistance.key(
                        branch.x() + direction.getStepX(),
                        branch.y() + direction.getStepY(),
                        branch.z() + direction.getStepZ());
                if (branches.containsKey(neighbour) && connected.add(neighbour)) {
                    queue.add(branches.get(neighbour));
                }
            }
        }
        return connected;
    }

    private static boolean touchesTrunk(BlockPlacement branch, Set<Long> trunkKeys) {
        for (Direction direction : Direction.values()) {
            long neighbour = LeafDistance.key(
                    branch.x() + direction.getStepX(),
                    branch.y() + direction.getStepY(),
                    branch.z() + direction.getStepZ());
            if (trunkKeys.contains(neighbour)) {
                return true;
            }
        }
        return false;
    }

    private static boolean canPlaceSoft(WorldGenLevel level, BlockPos.MutableBlockPos cursor,
                                        BlockPlacement block) {
        BlockState state = level.getBlockState(cursor.set(block.x(), block.y(), block.z()));
        return state.isAir()
                || state.canBeReplaced()
                || state.is(BlockTags.REPLACEABLE)
                || state.is(BlockTags.LEAVES);
    }

    private static BlockState stateFor(BlockPlacement placement,
                                       Block logBlock,
                                       Block leavesBlock,
                                       Block branchBlock,
                                       Map<Long, Integer> leafDistances,
                                       Set<Long> trunkKeys,
                                       Map<Long, BlockPlacement> branches,
                                       Set<Long> leafKeys,
                                       Map<Long, Integer> branchThicknesses) {
        return switch (placement.role()) {
            case LEAVES -> leafState(placement, leavesBlock, leafDistances);
            case BRANCH -> {
                if (!(branchBlock instanceof StaticBranchBlock)) {
                    yield withYAxis(logBlock.defaultBlockState());
                }
                int thickness = branchThicknesses.getOrDefault(
                        LeafDistance.key(placement.x(), placement.y(), placement.z()), 2);
                yield connectBranchState(branchBlock.defaultBlockState(), placement,
                        trunkKeys, branches, leafKeys, thickness);
            }
            case LOG -> withYAxis(logBlock.defaultBlockState());
            default -> null;
        };
    }

    /** Computes thickness gauge (1=thin 6px, 2=medium 8px, 3=thick 12px) from altitude and limb taper. */
    static Map<Long, Integer> computeBranchThicknesses(List<BlockPlacement> placements) {
        Map<Long, BlockPlacement> branchMap = new HashMap<>();
        Set<Long> trunkSet = new HashSet<>();
        int minTrunkY = Integer.MAX_VALUE;
        int maxTrunkY = Integer.MIN_VALUE;
        int maxWoodY = Integer.MIN_VALUE;

        for (BlockPlacement p : placements) {
            long k = LeafDistance.key(p.x(), p.y(), p.z());
            if (p.role() == PlacementRole.LOG) {
                trunkSet.add(k);
                minTrunkY = Math.min(minTrunkY, p.y());
                maxTrunkY = Math.max(maxTrunkY, p.y());
                maxWoodY = Math.max(maxWoodY, p.y());
            } else if (p.role() == PlacementRole.BRANCH) {
                branchMap.put(k, p);
                maxWoodY = Math.max(maxWoodY, p.y());
            }
        }
        if (branchMap.isEmpty()) {
            return Map.of();
        }
        if (minTrunkY == Integer.MAX_VALUE) {
            for (BlockPlacement p : branchMap.values()) {
                minTrunkY = Math.min(minTrunkY, p.y());
                maxTrunkY = Math.max(maxTrunkY, p.y());
            }
        }
        int treeHeight = Math.max(1, maxWoodY - minTrunkY + 1);

        Map<Long, Integer> distFromTrunk = new HashMap<>(branchMap.size() * 2);
        List<Long> bfsOrder = new ArrayList<>(branchMap.size());
        ArrayDeque<Long> queue = new ArrayDeque<>();
        for (Map.Entry<Long, BlockPlacement> entry : branchMap.entrySet()) {
            if (touchesTrunk(entry.getValue(), trunkSet)) {
                long k = entry.getKey();
                distFromTrunk.put(k, 1);
                bfsOrder.add(k);
                queue.add(k);
            }
        }
        while (!queue.isEmpty()) {
            long curKey = queue.poll();
            BlockPlacement cur = branchMap.get(curKey);
            int nextDist = distFromTrunk.get(curKey) + 1;
            for (Direction dir : Direction.values()) {
                long nk = LeafDistance.key(
                        cur.x() + dir.getStepX(), cur.y() + dir.getStepY(), cur.z() + dir.getStepZ());
                if (branchMap.containsKey(nk) && !distFromTrunk.containsKey(nk)) {
                    distFromTrunk.put(nk, nextDist);
                    bfsOrder.add(nk);
                    queue.add(nk);
                }
            }
        }

        Map<Long, Integer> outwardSteps = new HashMap<>(branchMap.size() * 2);
        for (int i = bfsOrder.size() - 1; i >= 0; i--) {
            long k = bfsOrder.get(i);
            BlockPlacement cur = branchMap.get(k);
            int d = distFromTrunk.get(k);
            int maxChild = -1;
            for (Direction dir : Direction.values()) {
                long nk = LeafDistance.key(
                        cur.x() + dir.getStepX(), cur.y() + dir.getStepY(), cur.z() + dir.getStepZ());
                if (distFromTrunk.getOrDefault(nk, -1) == d + 1) {
                    maxChild = Math.max(maxChild, outwardSteps.getOrDefault(nk, 0));
                }
            }
            outwardSteps.put(k, maxChild < 0 ? 0 : maxChild + 1);
        }

        Set<Long> trunkTopCollarKeys = new HashSet<>();
        for (long k : bfsOrder) {
            BlockPlacement bp = branchMap.get(k);
            if (trunkSet.contains(LeafDistance.key(bp.x(), bp.y() - 1, bp.z()))) {
                trunkTopCollarKeys.add(k);
            }
        }

        Map<Long, Integer> thicknessMap = new HashMap<>(branchMap.size() * 2);
        for (long k : bfsOrder) {
            BlockPlacement bp = branchMap.get(k);
            int dist = distFromTrunk.getOrDefault(k, 1);
            int outward = outwardSteps.getOrDefault(k, 0);
            double relY = (bp.y() - minTrunkY) / (double) treeHeight;
            boolean directlyAboveTrunk = trunkTopCollarKeys.contains(k);
            boolean nearTrunkTopFork = bp.y() >= maxTrunkY && dist <= 2 && outward >= 1
                    && touchesAny(bp, trunkTopCollarKeys);

            int thickness;
            if (directlyAboveTrunk
                    || nearTrunkTopFork
                    || (bp.y() >= maxTrunkY && dist == 1 && outward >= 1)
                    || (dist == 1 && outward >= 1 && relY <= 0.68D && treeHeight >= 7)
                    || (dist == 2 && outward >= 2 && relY <= 0.60D && treeHeight >= 10)
                    || (dist == 1 && outward >= 2 && relY <= 0.82D && treeHeight >= 10)) {
                thickness = 3;
            } else if ((outward == 0 && dist >= 2)
                    || (relY >= 0.80D && (outward == 0 || dist >= 2))
                    || (dist == 1 && outward == 0 && relY >= 0.45D)) {
                thickness = 1;
            } else {
                thickness = 2;
            }
            thicknessMap.put(k, thickness);
        }
        return thicknessMap;
    }

    private static boolean touchesAny(BlockPlacement branch, Set<Long> keys) {
        if (keys.isEmpty()) {
            return false;
        }
        for (Direction dir : Direction.values()) {
            if (keys.contains(LeafDistance.key(
                    branch.x() + dir.getStepX(),
                    branch.y() + dir.getStepY(),
                    branch.z() + dir.getStepZ()))) {
                return true;
            }
        }
        return false;
    }

    static BlockState connectBranchState(BlockState baseState,
                                         BlockPlacement placement,
                                         Set<Long> trunkKeys,
                                         Map<Long, BlockPlacement> branches,
                                         Set<Long> leafKeys) {
        return connectBranchState(baseState, placement, trunkKeys, branches, leafKeys, 2);
    }

    /**
     * Computes 6-way connections to every adjacent trunk or branch block and applies gauge thickness.
     * Terminal tips (with 1 wood connection) also extend into the leaf cluster
     * above or outward away from their parent so branch tips seat inside foliage.
     */
    static BlockState connectBranchState(BlockState baseState,
                                         BlockPlacement placement,
                                         Set<Long> trunkKeys,
                                         Map<Long, BlockPlacement> branches,
                                         Set<Long> leafKeys,
                                         int thickness) {
        boolean n = false;
        boolean s = false;
        boolean e = false;
        boolean w = false;
        boolean u = false;
        boolean d = false;
        int woodNeighbors = 0;
        Direction soleParent = null;

        for (Direction dir : Direction.values()) {
            long nk = LeafDistance.key(
                    placement.x() + dir.getStepX(),
                    placement.y() + dir.getStepY(),
                    placement.z() + dir.getStepZ());
            boolean connected = trunkKeys.contains(nk) || branches.containsKey(nk);
            if (connected) {
                woodNeighbors++;
                soleParent = dir;
                switch (dir) {
                    case NORTH -> n = true;
                    case SOUTH -> s = true;
                    case EAST -> e = true;
                    case WEST -> w = true;
                    case UP -> u = true;
                    case DOWN -> d = true;
                }
            }
        }

        if (woodNeighbors == 1 && soleParent != null) {
            long aboveKey = LeafDistance.key(placement.x(), placement.y() + 1, placement.z());
            if (!u && leafKeys.contains(aboveKey)) {
                u = true;
            }
            Direction outward = soleParent.getOpposite();
            long outKey = LeafDistance.key(
                    placement.x() + outward.getStepX(),
                    placement.y() + outward.getStepY(),
                    placement.z() + outward.getStepZ());
            if (leafKeys.contains(outKey)) {
                switch (outward) {
                    case NORTH -> n = true;
                    case SOUTH -> s = true;
                    case EAST -> e = true;
                    case WEST -> w = true;
                    case UP -> u = true;
                    case DOWN -> d = true;
                }
            }
        }

        BlockState connectedState = StaticBranchBlock.withConnections(baseState, n, s, e, w, u, d);
        return StaticBranchBlock.withThickness(connectedState, thickness);
    }

    private static BlockState leafState(BlockPlacement placement, Block leavesBlock,
                                        Map<Long, Integer> leafDistances) {
        Integer distance = leafDistances.get(
                LeafDistance.key(placement.x(), placement.y(), placement.z()));
        if (distance == null) {
            return null;
        }
        BlockState state = leavesBlock.defaultBlockState();
        if (state.hasProperty(LeavesBlock.DISTANCE)) {
            state = state.setValue(LeavesBlock.DISTANCE, distance);
        }
        if (state.hasProperty(LeavesBlock.PERSISTENT)) {
            state = state.setValue(LeavesBlock.PERSISTENT, false);
        }
        return state;
    }

    private static BlockState withYAxis(BlockState state) {
        if (state.hasProperty(RotatedPillarBlock.AXIS)) {
            return state.setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        }
        return state;
    }

    private static void applyDeterministicDecorators(WorldGenLevel level,
                                                     BlockPos.MutableBlockPos cursor,
                                                     SpeciesProfile profile,
                                                     List<BlockPlacement> placements,
                                                     Set<Long> leafKeys) {
        String path = profile.id().path().toLowerCase(Locale.ROOT);
        boolean wantsVines = path.contains("jungle")
                || path.contains("kapok")
                || path.contains("mangrove")
                || path.contains("willow");
        if (!wantsVines) {
            return;
        }
        for (BlockPlacement block : placements) {
            if (block.role() != PlacementRole.LEAVES) {
                continue;
            }
            if (!leafKeys.contains(LeafDistance.key(block.x(), block.y(), block.z()))) {
                continue;
            }
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                long sideKey = LeafDistance.key(
                        block.x() + dir.getStepX(), block.y(), block.z() + dir.getStepZ());
                if (leafKeys.contains(sideKey)) {
                    continue;
                }
                long h = SeedMixer.mix(
                        block.x() * 341873128712L + block.y() * 132897987541L
                                + block.z() * 7919L + dir.ordinal());
                if ((h & 0xF) != 0) {
                    continue;
                }
                int length = 2 + (int) ((h >>> 4) & 3);
                BlockState vine = Blocks.VINE.defaultBlockState()
                        .setValue(VineBlock.getPropertyForFace(dir.getOpposite()), true);
                for (int i = 0; i < length; i++) {
                    cursor.set(block.x() + dir.getStepX(), block.y() - i, block.z() + dir.getStepZ());
                    if (!level.ensureCanWrite(cursor) || !level.isEmptyBlock(cursor)) {
                        break;
                    }
                    level.setBlock(cursor, vine, WRITE_FLAGS);
                }
            }
        }
    }

    private static void rollback(WorldGenLevel level, BlockPlacement[] placements,
                                 int[] writtenIndices, BlockState[] previous, int written) {
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int j = written - 1; j >= 0; j--) {
            int index = writtenIndices[j];
            BlockPlacement placement = placements[index];
            BlockState state = previous[index];
            if (state != null) {
                level.setBlock(cursor.set(placement.x(), placement.y(), placement.z()),
                        state, WRITE_FLAGS);
            }
        }
    }

    private static Block resolve(ResourceId id) {
        ResourceLocation location = MinecraftResourceIds.toLocation(id);
        if (!BuiltInRegistries.BLOCK.containsKey(location)) {
            return null;
        }
        return BuiltInRegistries.BLOCK.get(location);
    }
}
