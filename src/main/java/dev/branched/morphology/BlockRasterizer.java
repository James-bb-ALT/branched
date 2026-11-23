package dev.branched.morphology;

import dev.branched.species.SpeciesProfile;

/**
 * Converts the abstract skeleton into discrete block placements. Implementations
 * must quantize branch shapes and respect the species budget.
 *
 * <p>The three-argument form rasterizes around the skeleton origin, which is only
 * useful for tests and diagnostics. Worldgen callers must use the six-argument
 * form so the produced block coordinates are absolute and the placement guard can
 * check them against the dimension build limits.</p>
 */
public interface BlockRasterizer {
    RasterizedTree rasterize(TreeSkeleton skeleton, SpeciesProfile profile, BlockPalette palette);

    default RasterizedTree rasterize(TreeSkeleton skeleton, SpeciesProfile profile, BlockPalette palette,
                                     int originX, int originY, int originZ) {
        return rasterize(skeleton, profile, palette);
    }
}
