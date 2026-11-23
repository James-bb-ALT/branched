package dev.branched.compat.adapter;

import dev.branched.compat.AbstractModAdapter;
import dev.branched.species.CompatibilityLevel;

/**
 * Nature's Spirit 2.0+: preserves custom Joshua & biome tree worldgen while converting
 * limbs into 17 dedicated 3D {@code StaticBranchBlock}s via {@code TreeFeatureMixin}
 * and {@code FeatureMixin}.
 */
public final class NaturesSpiritAdapter extends AbstractModAdapter {
    public NaturesSpiritAdapter() {
        super("natures_spirit", "Nature's Spirit", "2.0.0", CompatibilityLevel.SPECIAL_PRESERVE);
        feature("natures_spirit:aspen_tree", "natures_spirit:aspen", CompatibilityLevel.SPECIAL_PRESERVE,
                "Nature's Spirit aspen; converted via TreeFeatureMixin");
        feature("natures_spirit:cedar_tree", "natures_spirit:cedar", CompatibilityLevel.SPECIAL_PRESERVE,
                "Nature's Spirit cedar; converted via TreeFeatureMixin");
        feature("natures_spirit:cypress_tree", "natures_spirit:cypress", CompatibilityLevel.SPECIAL_PRESERVE,
                "Nature's Spirit cypress; converted via TreeFeatureMixin");
        feature("natures_spirit:fir_tree", "natures_spirit:fir", CompatibilityLevel.SPECIAL_PRESERVE,
                "Nature's Spirit fir; converted via TreeFeatureMixin");
        feature("natures_spirit:joshua_tree", "natures_spirit:joshua", CompatibilityLevel.SPECIAL_PRESERVE,
                "Nature's Spirit joshua tree; converted via FeatureMixin");
        feature("natures_spirit:larch_tree", "natures_spirit:larch", CompatibilityLevel.SPECIAL_PRESERVE,
                "Nature's Spirit larch; converted via TreeFeatureMixin");
        feature("natures_spirit:mahogany_tree", "natures_spirit:mahogany", CompatibilityLevel.SPECIAL_PRESERVE,
                "Nature's Spirit mahogany; converted via TreeFeatureMixin");
        feature("natures_spirit:maple_tree", "natures_spirit:maple", CompatibilityLevel.SPECIAL_PRESERVE,
                "Nature's Spirit maple; converted via TreeFeatureMixin");
        feature("natures_spirit:olive_tree", "natures_spirit:olive", CompatibilityLevel.SPECIAL_PRESERVE,
                "Nature's Spirit olive; converted via TreeFeatureMixin");
        feature("natures_spirit:redwood_tree", "natures_spirit:redwood", CompatibilityLevel.SPECIAL_PRESERVE,
                "Nature's Spirit redwood; converted via TreeFeatureMixin");
        feature("natures_spirit:sugi_tree", "natures_spirit:sugi", CompatibilityLevel.SPECIAL_PRESERVE,
                "Nature's Spirit sugi; converted via TreeFeatureMixin");
        feature("natures_spirit:willow_tree", "natures_spirit:willow", CompatibilityLevel.SPECIAL_PRESERVE,
                "Nature's Spirit willow; converted via TreeFeatureMixin");
        feature("natures_spirit:wisteria_tree", "natures_spirit:wisteria", CompatibilityLevel.SPECIAL_PRESERVE,
                "Nature's Spirit wisteria; converted via TreeFeatureMixin");
    }
}
