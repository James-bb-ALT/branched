package dev.litewer.immersivetreechop.server;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;

public final class TreeScanner {
    public static BlockState wood(Level level, BlockPos pos) {
        return Blocks.AIR.defaultBlockState();
    }

    public static boolean upright(BlockState state) {
        return false;
    }

    public static boolean naturalLeaf(BlockState state) {
        return false;
    }

    private static Scan scan(Level level, BlockPos cutPos, boolean legacy) {
        return new Scan(null, Failure.NONE);
    }

    private static boolean lambda$scan$2(BlockPos cutPos, BlockPos leafPos) {
        return false;
    }

    public record Tree(BlockPos cut, Map<BlockPos, BlockState> logs, Map<BlockPos, BlockState> leaves, int width) {
    }

    public record Scan(Tree tree, Failure failure) {
    }

    public enum Failure {
        NONE,
        NOT_TRUNK,
        STUMP,
        PLACED,
        UNLOADED,
        TOO_LARGE,
        NO_CROWN,
        CUT_TOO_HIGH,
        UNSUPPORTED_LOG
    }
}
