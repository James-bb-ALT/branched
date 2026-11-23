package dev.branched.compat.adapter;

import dev.branched.compat.AbstractModAdapter;
import dev.branched.species.CompatibilityLevel;

/**
 * BetterNether 20.0+: preserves custom Nether worldgen while converting tree limbs
 * into dedicated 3D {@code StaticBranchBlock}s via {@code FeatureMixin}.
 */
public final class BetterNetherAdapter extends AbstractModAdapter {
    public BetterNetherAdapter() {
        super("betternether", "BetterNether", "20.0.0", CompatibilityLevel.SPECIAL_PRESERVE);
        feature("betternether:anchor_tree", "betternether:anchor_tree", CompatibilityLevel.SPECIAL_PRESERVE,
                "ceiling-anchored Nether tree; branches converted via FeatureMixin");
        feature("betternether:tree_gloomwood", "betternether:gloomwood", CompatibilityLevel.SPECIAL_PRESERVE,
                "gloomwood tree; branches converted via FeatureMixin");
        feature("betternether:tree_sakura", "betternether:nether_sakura", CompatibilityLevel.SPECIAL_PRESERVE,
                "nether sakura tree; branches converted via FeatureMixin");
        feature("betternether:tree_old_willow", "betternether:willow", CompatibilityLevel.SPECIAL_PRESERVE,
                "old Nether willow tree; branches converted via FeatureMixin");
        feature("betternether:tree_rubeus", "betternether:rubeus", CompatibilityLevel.SPECIAL_PRESERVE,
                "rubeus tree; branches converted via FeatureMixin");
        feature("betternether:tree_wart", "betternether:wart", CompatibilityLevel.SPECIAL_PRESERVE,
                "nether wart tree; branches converted via FeatureMixin");
        feature("betternether:tree_willow", "betternether:willow", CompatibilityLevel.SPECIAL_PRESERVE,
                "nether willow tree; branches converted via FeatureMixin");
    }
}
