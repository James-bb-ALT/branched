package dev.branched.mixin.immersivetreechop;

import com.mojang.blaze3d.vertex.PoseStack;
import dev.litewer.immersivetreechop.client.WoodMesh;
import dev.branched.compat.physics.TreePhysicsCompat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Renders {@code StaticBranchBlock} parts on Immersive Tree Chop's falling tree entity
 * using their baked 3D 6-way connected model instead of a full 16x16x16 log block.
 */
@Mixin(value = WoodMesh.class, remap = false)
public abstract class ImmersiveWoodMeshMixin {
    @Inject(method = "blockLog", at = @At("HEAD"), cancellable = true)
    private static void branched$renderBranchBlock(PoseStack poseStack, MultiBufferSource buffer,
                                                         BlockState state, int packedLight, CallbackInfo ci) {
        if (TreePhysicsCompat.isBranch(state)) {
            Minecraft.getInstance().getBlockRenderer().renderSingleBlock(
                    state, poseStack, buffer, packedLight, OverlayTexture.NO_OVERLAY);
            ci.cancel();
        }
    }
}
