package dev.branched.compat.adapter;

import dev.branched.compat.AbstractModAdapter;
import dev.branched.species.CompatibilityLevel;

/**
 * Fish of Thieves 3.1.7 (reference). 3.4.0 is audited separately: same wood
 * families, one extra configured/placed feature, so IDs must not be assumed
 * stable across versions.
 */
public final class FishOfThievesAdapter extends AbstractModAdapter {
    public FishOfThievesAdapter() {
        super("fishofthieves", "Fish of Thieves", "3.1.7", CompatibilityLevel.STATIC_FRUIT);

        feature("fishofthieves:banana_tree_checked", "fishofthieves:banana", CompatibilityLevel.STATIC_FRUIT,
                "banana clusters must remain harvestable");
        feature("fishofthieves:coconut_tree_checked", "fishofthieves:coconut", CompatibilityLevel.STATIC_FRUIT,
                "coconut growth stages and drops must be preserved");
        feature("fishofthieves:mango_tree_checked", "fishofthieves:mango", CompatibilityLevel.STATIC_FRUIT,
                "mango fruit decorator must be preserved");
        feature("fishofthieves:sparse_jungle_fruit_trees", "fishofthieves:mixed_fruit", CompatibilityLevel.STATIC_FRUIT,
                "selector feature; resolve only after checking every nested feature");
    }
}
