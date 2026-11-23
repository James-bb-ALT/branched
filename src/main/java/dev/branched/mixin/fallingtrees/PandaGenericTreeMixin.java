package dev.branched.mixin.fallingtrees;

import dev.branched.compat.physics.TreePhysicsCompat;
import me.pandamods.fallingtrees.trees.GenericTree;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Set;

/**
 * Expands Panda's Falling Trees log floodfill with connected downward/lateral
 * {@code StaticBranchBlock} segments (since {@code GenericTree.gatherLogs} only scans {@code dy >= 0}).
 */
@Mixin(value = GenericTree.class, remap = false)
public abstract class PandaGenericTreeMixin {
    @Inject(method = "gatherLogs", at = @At("RETURN"), cancellable = true)
    private void branched$includeDownwardBranches(Level level, BlockPos origin,
                                                        CallbackInfoReturnable<Set<BlockPos>> cir) {
        Set<BlockPos> gathered = cir.getReturnValue();
        if (level == null || gathered == null || gathered.isEmpty()) {
            return;
        }
        Set<BlockPos> expanded = TreePhysicsCompat.expandWithConnectedBranches(level, gathered, 512);
        if (expanded.size() > gathered.size()) {
            cir.setReturnValue(expanded);
        }
    }
}
