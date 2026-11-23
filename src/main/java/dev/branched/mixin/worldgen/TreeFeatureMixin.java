package dev.branched.mixin.worldgen;

import com.google.common.collect.Iterables;
import dev.branched.worldgen.NativeTreeBranchConverter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.TreeFeature;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Set;

/**
 * Intercepts vanilla and third-party {@link TreeFeature#place} runs once trunk logs
 * and foliage sets are collected, converting lateral/forked logs or adding 3D
 * branches before {@code TreeFeature#updateLeaves} recomputes leaf distances.
 */
@Mixin(value = TreeFeature.class, remap = false)
public abstract class TreeFeatureMixin {
    @Redirect(
            method = {"place", "m_142674_"},
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/google/common/collect/Iterables;concat(Ljava/lang/Iterable;Ljava/lang/Iterable;Ljava/lang/Iterable;Ljava/lang/Iterable;)Ljava/lang/Iterable;"
            ),
            remap = false,
            require = 0
    )
    @SuppressWarnings("unchecked")
    private Iterable<BlockPos> branched$convertNativeTreeBranches(
            Iterable<BlockPos> roots,
            Iterable<BlockPos> trunkLogs,
            Iterable<BlockPos> leaves,
            Iterable<BlockPos> decorators,
            FeaturePlaceContext<TreeConfiguration> context) {
        if (trunkLogs instanceof Set<?> logRaw && leaves instanceof Set<?> leafRaw && context != null) {
            NativeTreeBranchConverter.convertPlacedTree(
                    context.level(),
                    context.origin(),
                    (Set<BlockPos>) logRaw,
                    (Set<BlockPos>) leafRaw);
        }
        return Iterables.concat(roots, trunkLogs, leaves, decorators);
    }
}
