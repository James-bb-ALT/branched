package dev.branched.compat.adapter;

import dev.branched.compat.AbstractModAdapter;
import dev.branched.species.CompatibilityLevel;

/** Baobab Tree 1.2+: preserves giant hollow/fruit-bearing baobabs and converts limbs via mixin. */
public final class BaobabTreeAdapter extends AbstractModAdapter {
    public BaobabTreeAdapter() {
        super("baobabtree", "Baobab Tree", "1.2.0", CompatibilityLevel.SPECIAL_PRESERVE);
        feature("baobabtree:young_baobab_placed", "baobabtree:baobab", CompatibilityLevel.SPECIAL_PRESERVE,
                "young savanna baobab; limbs converted via BaobabTreeGenerationStateMixin");
        feature("baobabtree:mature_baobab_placed", "baobabtree:baobab", CompatibilityLevel.SPECIAL_PRESERVE,
                "mature savanna baobab with fruit pods; limbs converted via BaobabTreeGenerationStateMixin");
        feature("baobabtree:ancient_baobab_placed", "baobabtree:baobab", CompatibilityLevel.SPECIAL_PRESERVE,
                "hollow ancient baobab with glow lichen & water pool; limbs converted via mixin");
        feature("baobabtree:fallen_baobab_placed", "baobabtree:baobab", CompatibilityLevel.SPECIAL_PRESERVE,
                "fallen savanna baobab log structure");
        feature("baobabtree:windswept_young_baobab_placed", "baobabtree:baobab", CompatibilityLevel.SPECIAL_PRESERVE,
                "windswept young savanna baobab; limbs converted via mixin");
        feature("baobabtree:windswept_mature_baobab_placed", "baobabtree:baobab", CompatibilityLevel.SPECIAL_PRESERVE,
                "windswept mature savanna baobab; limbs converted via mixin");
        feature("baobabtree:windswept_ancient_baobab_placed", "baobabtree:baobab", CompatibilityLevel.SPECIAL_PRESERVE,
                "windswept ancient baobab; limbs converted via mixin");
        feature("baobabtree:windswept_fallen_baobab_placed", "baobabtree:baobab", CompatibilityLevel.SPECIAL_PRESERVE,
                "windswept fallen baobab structure");
    }
}
