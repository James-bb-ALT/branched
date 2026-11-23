package dev.branched.mixin.worldgen;

import com.google.common.collect.Iterables;
import dev.branched.worldgen.NativeTreeBranchConverter;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import party.lemons.biomemakeover.level.feature.WaterTreeFeature;

import java.util.Set;

/**
 * Intercepts Biome Makeover's {@link WaterTreeFeature#place} (used by Swamp Cypress)
 * before {@code updateLeaves}, converting lateral logs into 3D {@code StaticBranchBlock}s.
 */
@Mixin(value = WaterTreeFeature.class, remap = false)
public abstract class WaterTreeFeatureMixin {
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
    private Iterable<BlockPos> branched$convertWaterTreeBranches(
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
