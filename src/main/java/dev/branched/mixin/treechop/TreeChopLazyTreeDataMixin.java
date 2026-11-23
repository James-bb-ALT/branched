package dev.branched.mixin.treechop;

import dev.branched.compat.physics.TreePhysicsCompat;
import ht.treechop.common.chop.LazyTreeData;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Expands HT's TreeChop log stream with connected downward-drooping
 * {@code StaticBranchBlock}s missed by {@code BlockNeighbors.HORIZONTAL_AND_ABOVE}.
 */
@Mixin(value = LazyTreeData.class, remap = false)
public abstract class TreeChopLazyTreeDataMixin {
    @Shadow
    private Level level;

    @Inject(method = "streamLogs", at = @At("RETURN"), cancellable = true)
    private void branched$includeDownwardBranches(CallbackInfoReturnable<Stream<BlockPos>> cir) {
        Stream<BlockPos> original = cir.getReturnValue();
        if (original == null || this.level == null) {
            return;
        }
        Set<BlockPos> base = original.collect(Collectors.toCollection(LinkedHashSet::new));
        if (base.isEmpty()) {
            cir.setReturnValue(base.stream());
            return;
        }
        Set<BlockPos> expanded = TreePhysicsCompat.expandWithConnectedBranches(this.level, base, 512);
        cir.setReturnValue(expanded.stream());
    }
}
