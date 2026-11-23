package dev.branched.mixin.immersivetreechop;

import dev.litewer.immersivetreechop.entity.LooseLogEntity;
import dev.litewer.immersivetreechop.wood.WoodVolume;
import dev.branched.compat.physics.TreePhysicsCompat;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.UUID;

/**
 * Adjusts loose branch thickness and orientation when a felled tree's branches
 * are released onto the ground by Immersive Tree Chop.
 */
@Mixin(value = LooseLogEntity.class, remap = false)
public abstract class ImmersiveLooseLogEntityMixin {
    @Shadow
    private BlockState wood;

    @Inject(method = "halfWidth", at = @At("HEAD"), cancellable = true)
    private void branched$branchHalfWidth(float partialTick, CallbackInfoReturnable<Double> cir) {
        if (TreePhysicsCompat.isBranch(this.wood)) {
            cir.setReturnValue(TreePhysicsCompat.branchHalfWidth(this.wood));
        }
    }

    @Inject(method = "fromTree", at = @At("RETURN"))
    private static void branched$orientReleasedBranch(
            Level level, BlockState state, List<ItemStack> drops, Vec3 pos,
            Quaternionf orientation, Vec3 velocity, Vec3 angular, float length,
            UUID owner, float cutHeight, WoodVolume volume,
            CallbackInfoReturnable<LooseLogEntity> cir) {
        LooseLogEntity loose = cir.getReturnValue();
        if (loose == null || !TreePhysicsCompat.isBranch(state)) {
            return;
        }
        Direction.Axis axis = TreePhysicsCompat.primaryBranchAxis(state);
        if (axis == Direction.Axis.X) {
            loose.body().orientation.rotateZ(-1.5707964F);
            loose.publish();
        } else if (axis == Direction.Axis.Z) {
            loose.body().orientation.rotateX(1.5707964F);
            loose.publish();
        }
    }
}
