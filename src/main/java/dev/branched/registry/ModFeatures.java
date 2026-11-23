package dev.branched.registry;

import dev.branched.Branched;
import dev.branched.worldgen.feature.StaticTreeFeature;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Feature types used by configured feature JSON. */
public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(ForgeRegistries.FEATURES, Branched.MOD_ID);

    public static final RegistryObject<StaticTreeFeature> STATIC_TREE =
            FEATURES.register("static_tree", StaticTreeFeature::new);

    private ModFeatures() {
    }
}
