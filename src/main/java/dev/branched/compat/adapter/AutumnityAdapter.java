package dev.branched.compat.adapter;

import dev.branched.compat.AbstractModAdapter;
import dev.branched.species.CompatibilityLevel;

/**
 * Autumnity 5.0+: preserves maple forests while converting limbs into dedicated
 * 3D {@code branched:aut_maple_branch} blocks via {@code FeatureMixin}.
 */
public final class AutumnityAdapter extends AbstractModAdapter {
    public AutumnityAdapter() {
        super("autumnity", "Autumnity", "5.0.0", CompatibilityLevel.SPECIAL_PRESERVE);
        feature("autumnity:maple_tree", "autumnity:maple", CompatibilityLevel.SPECIAL_PRESERVE,
                "Autumnity maple tree; converted via FeatureMixin");
    }
}
