package dev.branched.morphology;

import dev.branched.core.ResourceId;
import dev.branched.species.SpeciesProfile;

import java.util.Objects;

/** Input for one deterministic tree generation. */
public record MorphologyRequest(long worldSeed,
                                ResourceId dimension,
                                int originX,
                                int originY,
                                int originZ,
                                long variantSalt,
                                SpeciesProfile profile) {
    public MorphologyRequest {
        Objects.requireNonNull(dimension, "dimension");
        Objects.requireNonNull(profile, "profile");
    }

    public long seed() {
        long seed = SeedMixer.forTree(worldSeed, dimension, originX, originY, originZ,
                profile.id(), profile.generatorVersion());
        return variantSalt == 0L ? seed : SeedMixer.mix(seed, variantSalt);
    }
}
