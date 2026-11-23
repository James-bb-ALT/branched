package dev.branched.morphology;

import dev.branched.core.ResourceId;

/**
 * Stable seed mixing. The tree seed depends on world seed, dimension, exact
 * origin coordinates, species and generator version, never on chunk generation
 * order or global RNG state.
 */
public final class SeedMixer {
    private static final long GOLDEN = 0x9E3779B97F4A7C15L;

    private SeedMixer() {
    }

    public static long mix(long value) {
        long z = value + GOLDEN;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    public static long mix(long seed, long value) {
        return mix(seed ^ mix(value));
    }

    public static long hash(ResourceId id) {
        // FNV-1a 64-bit over the canonical id, then avalanched.
        long hash = 0xCBF29CE484222325L;
        String text = id.toString();
        for (int i = 0; i < text.length(); i++) {
            hash ^= text.charAt(i);
            hash *= 0x100000001B3L;
        }
        return mix(hash);
    }

    public static long forTree(long worldSeed,
                               ResourceId dimension,
                               int x,
                               int y,
                               int z,
                               ResourceId species,
                               int generatorVersion) {
        long seed = mix(worldSeed, 0x535441544943L);
        seed = mix(seed, hash(dimension));
        seed = mix(seed, ((long) x << 32) ^ (z & 0xFFFFFFFFL));
        seed = mix(seed, y);
        seed = mix(seed, hash(species));
        seed = mix(seed, generatorVersion);
        return seed;
    }
}
