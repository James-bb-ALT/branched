package dev.branched.compat.physics;

import dev.branched.block.StaticBranchBlock;
import dev.branched.core.ResourceId;
import dev.branched.worldgen.BranchMaterials;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Shared geometric and species-matching helpers for tree-felling and
 * rigid-body tree-physics mod compatibility.
 */
public final class TreePhysicsCompat {
    private static final AABB FULL_BLOCK_AABB = new AABB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D);

    private TreePhysicsCompat() {
    }

    /** Abstraction over wood nodes enabling pure-Java unit testing without Mojang registry bootstrap. */
    public interface WoodNodeView<S> {
        S stateAt(BlockPos pos);

        boolean isBranch(S state);

        boolean isLogOrBranch(S state);

        boolean matchesTreeWood(S startWood, S candidate);

        boolean isConnectedBetween(S fromState, BlockPos fromPos, S toState, BlockPos toPos);
    }

    public static boolean isBranch(BlockState state) {
        return state != null && state.getBlock() instanceof StaticBranchBlock;
    }

    public static boolean matchesWoodIds(ResourceId trunkId, ResourceId branchId) {
        if (trunkId == null || branchId == null) {
            return false;
        }
        return BranchMaterials.resolve(trunkId).equals(branchId);
    }

    public static boolean matchesTreeWood(BlockState startWood, BlockState candidate) {
        if (startWood == null || candidate == null) {
            return false;
        }
        if (candidate.getBlock() == startWood.getBlock()) {
            return isLogOrBranch(candidate);
        }
        if (!isBranch(candidate)) {
            return false;
        }
        ResourceLocation trunkKey = blockKey(startWood.getBlock());
        ResourceLocation branchKey = blockKey(candidate.getBlock());
        if (trunkKey == null || branchKey == null) {
            return false;
        }
        return matchesWoodIds(
                new ResourceId(trunkKey.getNamespace(), trunkKey.getPath()),
                new ResourceId(branchKey.getNamespace(), branchKey.getPath()));
    }

    public static int connectionMask(boolean north, boolean south, boolean east, boolean west,
                                     boolean up, boolean down) {
        int mask = 0;
        if (north) mask |= 1 << Direction.NORTH.ordinal();
        if (south) mask |= 1 << Direction.SOUTH.ordinal();
        if (east) mask |= 1 << Direction.EAST.ordinal();
        if (west) mask |= 1 << Direction.WEST.ordinal();
        if (up) mask |= 1 << Direction.UP.ordinal();
        if (down) mask |= 1 << Direction.DOWN.ordinal();
        return mask;
    }

    public static int connectionMask(BlockState state) {
        if (!isBranch(state)) {
            return 0;
        }
        return connectionMask(
                StaticBranchBlock.isConnected(state, Direction.NORTH),
                StaticBranchBlock.isConnected(state, Direction.SOUTH),
                StaticBranchBlock.isConnected(state, Direction.EAST),
                StaticBranchBlock.isConnected(state, Direction.WEST),
                StaticBranchBlock.isConnected(state, Direction.UP),
                StaticBranchBlock.isConnected(state, Direction.DOWN));
    }

    public static boolean hasConnection(int mask, Direction dir) {
        return dir != null && (mask & (1 << dir.ordinal())) != 0;
    }

    public static boolean computeIsConnectedBetween(boolean fromIsBranch, boolean fromIsLog, int fromConnMask,
                                                    BlockPos fromPos,
                                                    boolean toIsBranch, boolean toIsLog, int toConnMask,
                                                    BlockPos toPos) {
        if (fromPos == null || toPos == null) {
            return false;
        }
        int dx = toPos.getX() - fromPos.getX();
        int dy = toPos.getY() - fromPos.getY();
        int dz = toPos.getZ() - fromPos.getZ();
        if (Math.abs(dx) > 1 || Math.abs(dy) > 1 || Math.abs(dz) > 1 || (dx == 0 && dy == 0 && dz == 0)) {
            return false;
        }
        int manhattan = Math.abs(dx) + Math.abs(dy) + Math.abs(dz);
        if (manhattan == 1) {
            Direction dir = Direction.fromDelta(dx, dy, dz);
            if (dir == null) {
                return false;
            }
            if (fromIsBranch && hasConnection(fromConnMask, dir)) {
                return true;
            }
            if (toIsBranch && hasConnection(toConnMask, dir.getOpposite())) {
                return true;
            }
            if (fromIsBranch && !toIsBranch && toIsLog) {
                return true;
            }
            if (!fromIsBranch && fromIsLog && toIsBranch) {
                return true;
            }
            return !fromIsBranch && !toIsBranch && fromIsLog && toIsLog;
        }
        return (fromIsBranch || toIsBranch) && fromIsLog && toIsLog;
    }

    public static boolean isConnectedBetween(BlockState fromState, BlockPos fromPos,
                                              BlockState toState, BlockPos toPos) {
        if (fromState == null || toState == null) {
            return false;
        }
        return computeIsConnectedBetween(
                isBranch(fromState), isLogOrBranch(fromState), connectionMask(fromState), fromPos,
                isBranch(toState), isLogOrBranch(toState), connectionMask(toState), toPos);
    }

    public static AABB computeBranchCollisionBounds(int thickness, boolean north, boolean south,
                                                    boolean east, boolean west, boolean up, boolean down) {
        double half = computeBranchHalfWidth(thickness);
        double min = 0.5D - half;
        double max = 0.5D + half;
        double minX = west ? 0.0D : min;
        double maxX = east ? 1.0D : max;
        double minY = down ? 0.0D : min;
        double maxY = up ? 1.0D : max;
        double minZ = north ? 0.0D : min;
        double maxZ = south ? 1.0D : max;
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    public static AABB branchCollisionBounds(BlockState state) {
        if (!isBranch(state)) {
            return FULL_BLOCK_AABB;
        }
        int thickness = state.hasProperty(StaticBranchBlock.THICKNESS)
                ? state.getValue(StaticBranchBlock.THICKNESS)
                : 2;
        return computeBranchCollisionBounds(
                thickness,
                StaticBranchBlock.isConnected(state, Direction.NORTH),
                StaticBranchBlock.isConnected(state, Direction.SOUTH),
                StaticBranchBlock.isConnected(state, Direction.EAST),
                StaticBranchBlock.isConnected(state, Direction.WEST),
                StaticBranchBlock.isConnected(state, Direction.UP),
                StaticBranchBlock.isConnected(state, Direction.DOWN));
    }

    public static double computeBranchMass(AABB bounds) {
        double volume = bounds.getXsize() * bounds.getYsize() * bounds.getZsize();
        return 4.0D * Math.max(0.18D, Math.min(1.0D, volume));
    }

    public static double branchMass(BlockState state) {
        if (!isBranch(state)) {
            return 4.0D;
        }
        return computeBranchMass(branchCollisionBounds(state));
    }

    public static double computeBranchHalfWidth(int thickness) {
        int clamped = Math.max(1, Math.min(3, thickness));
        double halfPixels = (clamped == 3) ? 6.0D : (2.0D + clamped);
        return halfPixels / 16.0D;
    }

    public static double branchHalfWidth(BlockState state) {
        if (!isBranch(state)) {
            return 0.46D;
        }
        int thickness = state.hasProperty(StaticBranchBlock.THICKNESS)
                ? state.getValue(StaticBranchBlock.THICKNESS)
                : 2;
        return computeBranchHalfWidth(thickness);
    }

    public static Direction.Axis computePrimaryBranchAxis(boolean north, boolean south,
                                                            boolean east, boolean west,
                                                            boolean up, boolean down) {
        int xConn = (east ? 1 : 0) + (west ? 1 : 0);
        int zConn = (north ? 1 : 0) + (south ? 1 : 0);
        int yConn = (up ? 1 : 0) + (down ? 1 : 0);
        if (yConn > 0 && xConn == 0 && zConn == 0) {
            return Direction.Axis.Y;
        }
        if (xConn > zConn) {
            return Direction.Axis.X;
        }
        if (zConn > xConn) {
            return Direction.Axis.Z;
        }
        return xConn > 0 ? Direction.Axis.X : Direction.Axis.Y;
    }

    public static Direction.Axis primaryBranchAxis(BlockState state) {
        if (!isBranch(state)) {
            return Direction.Axis.Y;
        }
        return computePrimaryBranchAxis(
                StaticBranchBlock.isConnected(state, Direction.NORTH),
                StaticBranchBlock.isConnected(state, Direction.SOUTH),
                StaticBranchBlock.isConnected(state, Direction.EAST),
                StaticBranchBlock.isConnected(state, Direction.WEST),
                StaticBranchBlock.isConnected(state, Direction.UP),
                StaticBranchBlock.isConnected(state, Direction.DOWN));
    }

    public static <S> Set<BlockPos> expandWithConnectedBranches(WoodNodeView<S> view,
                                                                Set<BlockPos> baseLogs,
                                                                int maxLogs) {
        if (view == null || baseLogs == null || baseLogs.isEmpty()) {
            return baseLogs == null ? Set.of() : baseLogs;
        }
        int cap = Math.max(baseLogs.size(), maxLogs);
        Set<BlockPos> collected = new LinkedHashSet<>(baseLogs.size());
        ArrayDeque<BlockPos> queue = new ArrayDeque<>(baseLogs.size());
        List<S> trunkStates = new ArrayList<>();

        for (BlockPos pos : baseLogs) {
            BlockPos immutable = pos.immutable();
            if (collected.add(immutable)) {
                queue.add(immutable);
                S state = view.stateAt(immutable);
                if (state != null && !view.isBranch(state) && view.isLogOrBranch(state)) {
                    trunkStates.add(state);
                }
            }
        }

        while (!queue.isEmpty() && collected.size() < cap) {
            BlockPos current = queue.removeFirst();
            S currentState = view.stateAt(current);
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = -1; dy <= 1; dy++) {
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) {
                            continue;
                        }
                        BlockPos next = current.offset(dx, dy, dz);
                        if (collected.contains(next)) {
                            continue;
                        }
                        S nextState = view.stateAt(next);
                        if (nextState == null || !view.isBranch(nextState)) {
                            continue;
                        }
                        if (!matchesExpandedBranch(view, trunkStates, currentState, nextState)) {
                            continue;
                        }
                        if (!view.isConnectedBetween(currentState, current, nextState, next)) {
                            continue;
                        }
                        collected.add(next);
                        queue.add(next);
                        if (collected.size() >= cap) {
                            return collected;
                        }
                    }
                }
            }
        }
        return collected;
    }

    public static Set<BlockPos> expandWithConnectedBranches(BlockGetter level, Set<BlockPos> baseLogs, int maxLogs) {
        if (level == null || baseLogs == null || baseLogs.isEmpty()) {
            return baseLogs == null ? Set.of() : baseLogs;
        }
        return expandWithConnectedBranches(new BlockGetterNodeView(level), baseLogs, maxLogs);
    }

    private static <S> boolean matchesExpandedBranch(WoodNodeView<S> view, List<S> trunkStates,
                                                     S currentState, S nextState) {
        if (trunkStates.isEmpty()) {
            return !view.isBranch(currentState) || view.matchesTreeWood(currentState, nextState);
        }
        for (S trunk : trunkStates) {
            if (view.matchesTreeWood(trunk, nextState)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isLogOrBranch(BlockState state) {
        return isBranch(state) || (state != null && state.is(BlockTags.LOGS));
    }

    @SuppressWarnings("deprecation")
    private static ResourceLocation blockKey(Block block) {
        ResourceLocation forgeKey = ForgeRegistries.BLOCKS.getKey(block);
        if (forgeKey != null) {
            return forgeKey;
        }
        ResourceLocation builtinKey = BuiltInRegistries.BLOCK.getKey(block);
        if (!BuiltInRegistries.BLOCK.getDefaultKey().equals(builtinKey) || block == BuiltInRegistries.BLOCK.get(builtinKey)) {
            return builtinKey;
        }
        return null;
    }

    private record BlockGetterNodeView(BlockGetter level) implements WoodNodeView<BlockState> {
        @Override
        public BlockState stateAt(BlockPos pos) {
            return level.getBlockState(pos);
        }

        @Override
        public boolean isBranch(BlockState state) {
            return TreePhysicsCompat.isBranch(state);
        }

        @Override
        public boolean isLogOrBranch(BlockState state) {
            return TreePhysicsCompat.isLogOrBranch(state);
        }

        @Override
        public boolean matchesTreeWood(BlockState startWood, BlockState candidate) {
            return TreePhysicsCompat.matchesTreeWood(startWood, candidate);
        }

        @Override
        public boolean isConnectedBetween(BlockState fromState, BlockPos fromPos, BlockState toState, BlockPos toPos) {
            return TreePhysicsCompat.isConnectedBetween(fromState, fromPos, toState, toPos);
        }
    }
}
