package dev.branched.mixin.immersivetreechop;

import dev.litewer.immersivetreechop.server.Chopping;
import dev.litewer.immersivetreechop.server.TreeScanner;
import dev.branched.compat.physics.TreePhysicsCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Map;

/**
 * Ensures low {@code StaticBranchBlock}s at {@code cutY} are neither included in saw-cut
 * cross-sections nor converted into persistent tree stumps during felling.
 */
@Mixin(value = Chopping.class, remap = false)
public abstract class ImmersiveChoppingMixin {
    @Inject(method = "lambda$section$9", at = @At("HEAD"), cancellable = true)
    private static void branched$excludeBranchesFromCutSection(TreeScanner.Tree tree, BlockPos pos,
                                                                     CallbackInfoReturnable<Boolean> cir) {
        if (tree != null && TreePhysicsCompat.isBranch(tree.logs().get(pos))) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "fell", at = @At("RETURN"))
    private static void branched$clearCutLevelBranchStumps(ServerPlayer player, TreeScanner.Tree tree,
                                                                 float cutHeight, Vec3 fallDir, Direction dir,
                                                                 ItemStack tool, CallbackInfoReturnable<Boolean> cir) {
        if (!Boolean.TRUE.equals(cir.getReturnValue()) || player == null || tree == null) {
            return;
        }
        ServerLevel level = player.serverLevel();
        int cutY = tree.cut().getY();
        for (Map.Entry<BlockPos, BlockState> entry : tree.logs().entrySet()) {
            BlockPos pos = entry.getKey();
            if (pos.getY() == cutY && TreePhysicsCompat.isBranch(entry.getValue())) {
                if (!level.getBlockState(pos).isAir()) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        }
    }
}
