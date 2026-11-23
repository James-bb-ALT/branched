package dev.branched.compat.adapter;

import dev.branched.compat.AbstractModAdapter;
import dev.branched.species.CompatibilityLevel;

/**
 * Atmospheric 6.0+: preserves custom Abnormals biome trees while converting limbs into
 * 8 dedicated 3D {@code StaticBranchBlock}s via {@code FeatureMixin}.
 */
public final class AtmosphericAdapter extends AbstractModAdapter {
    public AtmosphericAdapter() {
        super("atmospheric", "Atmospheric", "6.0.0", CompatibilityLevel.SPECIAL_PRESERVE);
        feature("atmospheric:aspen_tree", "atmospheric:aspen", CompatibilityLevel.SPECIAL_PRESERVE,
                "Atmospheric aspen tree; converted via FeatureMixin");
        feature("atmospheric:grimwood_tree", "atmospheric:grimwood", CompatibilityLevel.SPECIAL_PRESERVE,
                "Atmospheric grimwood tree; converted via FeatureMixin");
        feature("atmospheric:kousa_tree", "atmospheric:kousa", CompatibilityLevel.SPECIAL_PRESERVE,
                "Atmospheric kousa tree; converted via FeatureMixin");
        feature("atmospheric:laurel_tree", "atmospheric:laurel", CompatibilityLevel.SPECIAL_PRESERVE,
                "Atmospheric laurel tree; converted via FeatureMixin");
        feature("atmospheric:morado_tree", "atmospheric:morado", CompatibilityLevel.SPECIAL_PRESERVE,
                "Atmospheric morado tree; converted via FeatureMixin");
        feature("atmospheric:rosewood_tree", "atmospheric:rosewood", CompatibilityLevel.SPECIAL_PRESERVE,
                "Atmospheric rosewood tree; converted via FeatureMixin");
        feature("atmospheric:yucca_tree", "atmospheric:yucca", CompatibilityLevel.SPECIAL_PRESERVE,
                "Atmospheric yucca tree; converted via FeatureMixin");
    }
}
