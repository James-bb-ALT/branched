package dev.branched.worldgen;

import dev.branched.core.ResourceId;

/** Minimal world view required by placement validation. */
public interface PlacementContext {
    long worldSeed();

    ResourceId dimension();

    int minBuildHeight();

    int maxBuildHeight();

    boolean isAir(int x, int y, int z);

    boolean isReplaceable(int x, int y, int z);

    boolean hasSolidGround(int x, int z);

    /** Returns {@code null} when the position is outside a loaded chunk. */
    ResourceId blockAt(int x, int y, int z);

    /**
     * Whether the worldgen region accepts a write at this position. Pure test
     * contexts accept everything; the Minecraft context forwards to
     * {@code WorldGenLevel.ensureCanWrite}.
     */
    default boolean canWrite(int x, int y, int z) {
        return true;
    }

    /**
     * Ground predicate for the trunk base. Defaults to the coarse Phase 0
     * "solid ground" check so existing test contexts keep working; worldgen
     * overrides it with the exact dirt/snow tag check.
     */
    default boolean isGround(int x, int y, int z) {
        return hasSolidGround(x, z);
    }
}
