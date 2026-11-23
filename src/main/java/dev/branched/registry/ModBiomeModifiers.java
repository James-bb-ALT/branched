package dev.branched.registry;

import com.mojang.serialization.Codec;
import dev.branched.Branched;
import dev.branched.compat.biome.StaticTreeBiomeModifier;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/** Biome modifier serializers, loaded from {@code data/<ns>/forge/biome_modifier/}. */
public final class ModBiomeModifiers {
    public static final DeferredRegister<Codec<? extends BiomeModifier>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS,
                    Branched.MOD_ID);

    public static final RegistryObject<Codec<? extends BiomeModifier>> REPLACE_TREES =
            SERIALIZERS.register("replace_trees", () -> StaticTreeBiomeModifier.CODEC);

    private ModBiomeModifiers() {
    }
}
