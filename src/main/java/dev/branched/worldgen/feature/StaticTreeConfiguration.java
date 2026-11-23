package dev.branched.worldgen.feature;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.branched.core.ResourceId;
import dev.branched.worldgen.MinecraftResourceIds;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;

/**
 * Configured-feature JSON for one static tree.
 *
 * <pre>{@code
 * { "type": "branched:static_tree",
 *   "config": { "species": "minecraft:spruce", "attempts": 3 } }
 * }</pre>
 *
 * <p>{@code attempts} is only a request; the effective value is capped by
 * {@code morphology.max_generation_attempts}. Because the field is optional,
 * an out-of-range value falls back to the default of 3 rather than failing the
 * datapack; the runtime cap is still applied afterwards.</p>
 */
public record StaticTreeConfiguration(ResourceId species, int attempts)
        implements FeatureConfiguration {

    public static final Codec<StaticTreeConfiguration> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    MinecraftResourceIds.CODEC.fieldOf("species")
                            .forGetter(StaticTreeConfiguration::species),
                    Codec.intRange(1, 8).optionalFieldOf("attempts", 3)
                            .forGetter(StaticTreeConfiguration::attempts)
            ).apply(instance, StaticTreeConfiguration::new));

    public StaticTreeConfiguration {
        if (species == null) {
            throw new IllegalArgumentException("species is required");
        }
        if (attempts < 1 || attempts > 8) {
            throw new IllegalArgumentException("attempts must be in [1, 8]");
        }
    }
}
