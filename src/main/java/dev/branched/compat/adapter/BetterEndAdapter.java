package dev.branched.compat.adapter;

import dev.branched.compat.AbstractModAdapter;
import dev.branched.species.CompatibilityLevel;

/**
 * BetterEnd 20.0+: preserves biome features while converting living End tree limbs
 * into dedicated 3D {@code StaticBranchBlock}s via {@code FeatureMixin}.
 */
public final class BetterEndAdapter extends AbstractModAdapter {
    public BetterEndAdapter() {
        super("betterend", "BetterEnd", "20.0.0", CompatibilityLevel.SPECIAL_PRESERVE);
        feature("betterend:dragon_tree", "betterend:dragon_tree", CompatibilityLevel.SPECIAL_PRESERVE,
                "custom SDF dragon tree; branches converted via FeatureMixin");
        feature("betterend:dragon_helix_tree", "betterend:dragon_tree", CompatibilityLevel.SPECIAL_PRESERVE,
                "giant dragon helix tree; branches converted via FeatureMixin");
        feature("betterend:helix_tree", "betterend:helix_tree", CompatibilityLevel.SPECIAL_PRESERVE,
                "spiral helix tree; branches converted via FeatureMixin");
        feature("betterend:lacugrove", "betterend:lacugrove", CompatibilityLevel.SPECIAL_PRESERVE,
                "lacugrove tree; branches converted via FeatureMixin");
        feature("betterend:lucernia", "betterend:lucernia", CompatibilityLevel.SPECIAL_PRESERVE,
                "lucernia canopy tree; branches converted via FeatureMixin");
        feature("betterend:pythadendron_tree", "betterend:pythadendron", CompatibilityLevel.SPECIAL_PRESERVE,
                "pythadendron tree; branches converted via FeatureMixin");
        feature("betterend:tenanea", "betterend:tenanea", CompatibilityLevel.SPECIAL_PRESERVE,
                "tenanea tree; branches converted via FeatureMixin");
        feature("betterend:umbrella_tree", "betterend:umbrella_tree", CompatibilityLevel.SPECIAL_PRESERVE,
                "umbrella tree; branches converted via FeatureMixin");
    }
}
