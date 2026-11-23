package dev.branched.compat.biome;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.branched.Branched;
import dev.branched.core.ResourceId;
import dev.branched.worldgen.MinecraftResourceIds;
import dev.branched.worldgen.WorldgenGate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.RegistryFixedCodec;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;
import net.minecraftforge.common.world.BiomeModifier;
import net.minecraftforge.common.world.ModifiableBiomeInfo;
import org.slf4j.Logger;

import java.util.List;

/**
 * Biome modifier that replaces exactly one or more audited PlacedFeatures with
 * a Branched placed feature.
 *
 * <p>Removal happens in {@link BiomeModifier.Phase#REMOVE}; addition happens in
 * {@link BiomeModifier.Phase#ADD}, matching Forge's built-in modifiers. Both
 * phases are guarded by the master config switch, the audited-feature catalog
 * and the presence of a replaceable species profile. If any check fails the
 * native feature is left untouched.</p>
 */
public record StaticTreeBiomeModifier(HolderSet<Biome> biomes,
                                      Holder<PlacedFeature> add,
                                      HolderSet<PlacedFeature> replace,
                                      List<ResourceId> species,
                                      GenerationStep.Decoration step)
        implements BiomeModifier {
    private static final Logger LOGGER = Branched.LOGGER;

    public static final Codec<StaticTreeBiomeModifier> CODEC = RecordCodecBuilder.create(instance ->
            instance.group(
                    RegistryCodecs.homogeneousList(Registries.BIOME)
                            .fieldOf("biomes").forGetter(StaticTreeBiomeModifier::biomes),
                    RegistryFixedCodec.create(Registries.PLACED_FEATURE)
                            .fieldOf("add").forGetter(StaticTreeBiomeModifier::add),
                    RegistryCodecs.homogeneousList(Registries.PLACED_FEATURE)
                            .fieldOf("replace").forGetter(StaticTreeBiomeModifier::replace),
                    // A single id is still accepted so Phase 1 datapacks keep
                    // loading; a list lets one aggregate gate every species it
                    // can select (for example spruce + pine for trees_taiga).
                    Codec.either(MinecraftResourceIds.CODEC, MinecraftResourceIds.CODEC.listOf())
                            .xmap(either -> either.map(List::of, list -> list),
                                    list -> list.size() == 1
                                            ? com.mojang.datafixers.util.Either.left(list.get(0))
                                            : com.mojang.datafixers.util.Either.right(list))
                            .fieldOf("species").forGetter(StaticTreeBiomeModifier::species),
                    GenerationStep.Decoration.CODEC.fieldOf("step")
                            .forGetter(StaticTreeBiomeModifier::step)
            ).apply(instance, StaticTreeBiomeModifier::new));

    @Override
    public Codec<? extends BiomeModifier> codec() {
        return CODEC;
    }

    @Override
    public void modify(Holder<Biome> biome, Phase phase,
                       ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.ADD && phase != Phase.REMOVE) {
            return;
        }
        if (!biomes.contains(biome)) {
            return;
        }

        for (ResourceId speciesId : species) {
            if (WorldgenGate.replaceableProfile(speciesId).isEmpty()) {
                LOGGER.info("Branched preserves native trees in {}: no replaceable profile for {}",
                        name(biome), speciesId);
                return;
            }
        }
        if (WorldgenGate.requireKnownFeatures() && !knownFeatures()) {
            LOGGER.info("Branched preserves native trees in {}: replacement set is not fully audited",
                    name(biome));
            return;
        }

        if (phase == Phase.ADD) {
            List<Holder<PlacedFeature>> features =
                    builder.getGenerationSettings().getFeatures(step);
            if (!features.contains(add)) {
                builder.getGenerationSettings().addFeature(step, add);
                LOGGER.info("Branched adds {} to {} ({})",
                        name(add), name(biome), step.getSerializedName());
            }
        } else {
            List<Holder<PlacedFeature>> features =
                    builder.getGenerationSettings().getFeatures(step);
            int before = features.size();
            features.removeIf(replace::contains);
            int removed = before - features.size();
            if (removed > 0) {
                LOGGER.info("Branched removed {} native tree feature(s) from {} ({})",
                        removed, name(biome), step.getSerializedName());
            }
        }
    }

    private boolean knownFeatures() {
        for (Holder<PlacedFeature> holder : replace) {
            ResourceId id = holder.unwrapKey()
                    .map(key -> MinecraftResourceIds.fromLocationOrNull(key.location()))
                    .orElse(null);
            if (id == null || !Branched.adapters().isReplaceable(id)) {
                return false;
            }
        }
        return true;
    }

    private static String name(Holder<?> holder) {
        return holder.unwrapKey()
                .map(key -> key.location().toString())
                .orElse("<unregistered>");
    }
}
