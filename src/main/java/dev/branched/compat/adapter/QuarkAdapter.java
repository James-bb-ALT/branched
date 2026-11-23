package dev.branched.compat.adapter;

import dev.branched.compat.AbstractModAdapter;
import dev.branched.species.CompatibilityLevel;

/**
 * Quark 4.0-462. Five blossom colour variants share the blossom wood family.
 * Ancient wood has no placed feature in the audited JAR (structure/loot use).
 */
public final class QuarkAdapter extends AbstractModAdapter {
    public QuarkAdapter() {
        super("quark", "Quark", "4.0-462", CompatibilityLevel.STATIC_NORMAL);

        feature("quark:blue_blossom_placed", "quark:blossom", CompatibilityLevel.STATIC_NORMAL);
        feature("quark:lavender_blossom_placed", "quark:blossom", CompatibilityLevel.STATIC_NORMAL);
        feature("quark:orange_blossom_placed", "quark:blossom", CompatibilityLevel.STATIC_NORMAL);
        feature("quark:red_blossom_placed", "quark:blossom", CompatibilityLevel.STATIC_NORMAL);
        feature("quark:yellow_blossom_placed", "quark:blossom", CompatibilityLevel.STATIC_NORMAL);
        feature("quark:glow_shrooms", "quark:glow_shroom", CompatibilityLevel.SPECIAL_PRESERVE,
                "custom glow-shroom feature type");
        feature("quark:ancient", "quark:ancient", CompatibilityLevel.UNRESOLVED,
                "no placed feature found in 4.0-462; wood available for structures only");
    }
}
