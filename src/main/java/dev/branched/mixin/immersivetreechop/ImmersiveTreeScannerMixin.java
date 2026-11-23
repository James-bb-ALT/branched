package dev.branched.mixin.immersivetreechop;

import dev.litewer.immersivetreechop.server.TreeScanner;
import dev.branched.compat.physics.TreePhysicsCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Integrates {@code StaticBranchBlock}s into Immersive Tree Chop's {@link TreeScanner}:
 * matches connected species branches during trunk floodfill, prevents branches from being
 * treated as grounded upright columns, and expands radius/log caps for wide canopies.
 */
@Mixin(value = TreeScanner.class, remap = false)
public abstract class ImmersiveTreeScannerMixin {
    @Unique
    private static final ThreadLocal<BlockState> BRANCHED_START_WOOD = new ThreadLocal<>();

    @Inject(method = "upright", at = @At("HEAD"), cancellable = true)
    private static void branched$branchesAreNotUprightTrunks(BlockState state, CallbackInfoReturnable<Boolean> cir) {
        if (TreePhysicsCompat.isBranch(state)) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "scan", at = @At("HEAD"))
    private static void branched$captureStartWood(Level level, BlockPos cutPos, boolean legacy,
                                                        CallbackInfoReturnable<TreeScanner.Scan> cir) {
        BRANCHED_START_WOOD.set(TreeScanner.wood(level, cutPos));
    }

    @Inject(method = "scan", at = @At("RETURN"))
    private static void branched$clearStartWood(Level level, BlockPos cutPos, boolean legacy,
                                                      CallbackInfoReturnable<TreeScanner.Scan> cir) {
        BRANCHED_START_WOOD.remove();
    }

    @Redirect(
            method = "scan",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/block/state/BlockState;m_60734_()Lnet/minecraft/world/level/block/Block;",
                    ordinal = 2
            ),
            require = 0
    )
    private static Block branched$matchBranchBlockToTrunk(BlockState candidate) {
        BlockState startWood = BRANCHED_START_WOOD.get();
        if (startWood != null && TreePhysicsCompat.isBranch(candidate)
                && TreePhysicsCompat.matchesTreeWood(startWood, candidate)) {
            return startWood.getBlock();
        }
        return candidate.getBlock();
    }

    @ModifyConstant(method = "scan", constant = @Constant(intValue = 12), require = 0)
    private static int branched$widenScanHorizontalRadius(int original) {
        return 20;
    }

    @ModifyConstant(method = "scan", constant = @Constant(intValue = 256), require = 0)
    private static int branched$widenMaxTreeLogs(int original) {
        return 640;
    }

    @ModifyConstant(method = "lambda$scan$2", constant = @Constant(intValue = 12), require = 0)
    private static int branched$widenLeafHorizontalRadius(int original) {
        return 22;
    }
}
