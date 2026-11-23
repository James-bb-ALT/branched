package dev.branched.mixin.worldgen;

import dev.branched.worldgen.CustomTreeFeatureConverter;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Universal post-placement hook for allowlisted custom modded tree {@link Feature}
 * subclasses that bypass vanilla {@code TreeFeature}, converting lateral/forked logs
 * or sprouting canopy branches and recomputing leaf distances.
 */
@Mixin(value = Feature.class, remap = false)
public abstract class FeatureMixin {
    @Inject(
            method = {
                    "place(Lnet/minecraft/world/level/levelgen/feature/configurations/FeatureConfiguration;Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z",
                    "m_225028_(Lnet/minecraft/world/level/levelgen/feature/configurations/FeatureConfiguration;Lnet/minecraft/world/level/WorldGenLevel;Lnet/minecraft/world/level/chunk/ChunkGenerator;Lnet/minecraft/util/RandomSource;Lnet/minecraft/core/BlockPos;)Z"
            },
            at = @At("RETURN"),
            remap = false,
            require = 0
    )
    private void branched$convertCustomTreeFeature(
            FeatureConfiguration config,
            WorldGenLevel level,
            ChunkGenerator generator,
            RandomSource random,
            BlockPos origin,
            CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) {
            return;
        }
        CustomTreeFeatureConverter.TreeScanProfile profile =
                CustomTreeFeatureConverter.profileFor(this.getClass());
        if (profile != null) {
            CustomTreeFeatureConverter.convertPostPlacedFeature(level, origin, profile);
        }
    }
}
