package dev.branched.compat.adapter;

import dev.branched.compat.AbstractModAdapter;
import dev.branched.species.CompatibilityLevel;

/**
 * Twilight Delight 2.0+: preserves Ironwood tree generation while converting canopy
 * limbs into dedicated 3D {@code branched:ironwood_branch} blocks via {@code FeatureMixin}.
 */
public final class TwilightDelightAdapter extends AbstractModAdapter {
    public TwilightDelightAdapter() {
        super("twilightdelight", "Twilight Delight", "2.0.0", CompatibilityLevel.SPECIAL_PRESERVE);
        feature("twilightdelight:tree/ironwood_tree", "twilightdelight:ironwood", CompatibilityLevel.SPECIAL_PRESERVE,
                "ironwood dark canopy tree; branches converted via FeatureMixin");
    }
}
