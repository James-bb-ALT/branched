package dev.branched.compat.adapter;

import dev.branched.compat.AbstractModAdapter;
import dev.branched.species.CompatibilityLevel;

/**
 * Biome Makeover 1.11+: preserves custom tree decorators, water-root placement,
 * and Architectury biome injections while converting tree limbs into dedicated
 * 3D {@code StaticBranchBlock}s via {@code TreeFeatureMixin} and {@code WaterTreeFeatureMixin}.
 */
public final class BiomeMakeoverAdapter extends AbstractModAdapter {
    public BiomeMakeoverAdapter() {
        super("biomemakeover", "Biome Makeover", "1.11.0", CompatibilityLevel.SPECIAL_PRESERVE);
        feature("biomemakeover:dark_forest/trees", "biomemakeover:ancient_oak", CompatibilityLevel.SPECIAL_PRESERVE,
                "injected at UNDERGROUND_ORES with ivy decorators; branches converted via TreeFeatureMixin");
        feature("biomemakeover:mushroom_fields/blighted_balsa_trees", "biomemakeover:blighted_balsa",
                CompatibilityLevel.SPECIAL_PRESERVE,
                "mushroom fields balsa; branches converted via TreeFeatureMixin");
        feature("biomemakeover:swamp/swamp_cypress_trees", "biomemakeover:swamp_cypress",
                CompatibilityLevel.SPECIAL_PRESERVE,
                "water_tree with hanging leaves & vines; branches converted via WaterTreeFeatureMixin");
        feature("biomemakeover:swamp/willow_trees", "biomemakeover:willow",
                CompatibilityLevel.SPECIAL_PRESERVE,
                "swamp willow with willowing_branch & hanging_leaves decorators; converted via TreeFeatureMixin");
    }
}
