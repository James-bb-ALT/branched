package dev.branched.mixin.worldgen;

import dev.branched.worldgen.NativeTreeBranchConverter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Hooks Baobab Tree's {@code BaobabTreeGenerator$GenerationState} around
 * {@code placeBranches} and {@code finalizeLeaves} so standing & sapling baobabs
 * keep their tapered/hollow trunks and convert their wide canopy limbs into 3D
 * {@code branched:baobabtree_branch} blocks.
 */
@Mixin(targets = "de.artemis.baobabtree.common.worldgen.feature.BaobabTreeGenerator$GenerationState", remap = false)
public abstract class BaobabTreeGenerationStateMixin {
    @Shadow
    private WorldGenLevel level;

    @Shadow
    private BlockPos origin;

    @Shadow
    private Set<BlockPos> logPositions;

    @Shadow
    private Set<BlockPos> leafPositions;

    @Unique
    private Set<BlockPos> branched$trunkSnapshot;

    @Inject(method = "placeBranches", at = @At("HEAD"), remap = false, require = 0)
    private void branched$captureTrunkSnapshot(CallbackInfo ci) {
        if (this.logPositions == null || this.logPositions.isEmpty()) {
            this.branched$trunkSnapshot = null;
            return;
        }
        this.branched$trunkSnapshot = new LinkedHashSet<>(this.logPositions);
    }

    @Inject(method = "finalizeLeaves", at = @At("HEAD"), remap = false, require = 0)
    private void branched$convertBaobabBranches(CallbackInfo ci) {
        Set<BlockPos> trunk = this.branched$trunkSnapshot;
        this.branched$trunkSnapshot = null;
        if (trunk != null && !trunk.isEmpty()) {
            NativeTreeBranchConverter.convertBaobabTree(
                    this.level,
                    this.origin,
                    trunk,
                    this.logPositions,
                    this.leafPositions);
        }
    }
}
