package dev.branched.morphology;

/**
 * Semiprocedural generator contract. Implementations must be pure with respect
 * to the request: the same request always yields the same skeleton, with no
 * shared mutable RNG and no Minecraft world access.
 */
public interface MorphologyGenerator {
    TreeSkeleton generate(MorphologyRequest request);

    /** Bumped whenever topology output changes for the same request. */
    int version();
}
