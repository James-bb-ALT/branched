package dev.branched.compat.adapter;

import dev.branched.compat.AbstractModAdapter;
import dev.branched.species.CompatibilityLevel;

/** Deep Aether 1.1+: integrates roseroot, blue roseroot, yagroot, cruderoot, conberry and sunroot trees. */
public final class DeepAetherAdapter extends AbstractModAdapter {
    public DeepAetherAdapter() {
        super("deep_aether", "Deep Aether", "1.1.0", CompatibilityLevel.STATIC_NORMAL);
        feature("deep_aether:aerglow_forest_trees_placement", "deep_aether:roseroot",
                CompatibilityLevel.STATIC_NORMAL, "roseroot forest trees");
        feature("deep_aether:blue_aerglow_forest_trees_placement", "deep_aether:blue_roseroot",
                CompatibilityLevel.STATIC_NORMAL, "blue roseroot forest trees");
        feature("deep_aether:mystic_aerglow_forest_trees_placement", "deep_aether:roseroot",
                CompatibilityLevel.STATIC_NORMAL, "mystic roseroot trees");
        feature("deep_aether:yagroot_swamp_trees_placement", "deep_aether:yagroot",
                CompatibilityLevel.STATIC_NORMAL, "yagroot and cruderoot swamp trees");
        feature("deep_aether:golden_grove_trees", "deep_aether:conberry",
                CompatibilityLevel.STATIC_NORMAL, "conberry and sunroot trees");
        feature("deep_aether:golden_heights_trees", "deep_aether:conberry",
                CompatibilityLevel.STATIC_NORMAL, "golden heights conberry and sunroot trees");
    }
}
