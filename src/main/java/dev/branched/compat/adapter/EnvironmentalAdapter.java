package dev.branched.compat.adapter;

import dev.branched.compat.AbstractModAdapter;
import dev.branched.species.CompatibilityLevel;

/**
 * Environmental 4.0+: preserves Abnormals biome trees while converting limbs into
 * 4 dedicated 3D {@code StaticBranchBlock}s via {@code FeatureMixin}.
 */
public final class EnvironmentalAdapter extends AbstractModAdapter {
    public EnvironmentalAdapter() {
        super("environmental", "Environmental", "4.0.0", CompatibilityLevel.SPECIAL_PRESERVE);
        feature("environmental:pine_tree", "environmental:pine", CompatibilityLevel.SPECIAL_PRESERVE,
                "Environmental pine tree; converted via FeatureMixin");
        feature("environmental:plum_tree", "environmental:plum", CompatibilityLevel.SPECIAL_PRESERVE,
                "Environmental plum tree; converted via FeatureMixin");
        feature("environmental:willow_tree", "environmental:willow", CompatibilityLevel.SPECIAL_PRESERVE,
                "Environmental willow tree; converted via FeatureMixin");
        feature("environmental:wisteria_tree", "environmental:wisteria", CompatibilityLevel.SPECIAL_PRESERVE,
                "Environmental wisteria tree; converted via FeatureMixin");
    }
}
