package dev.branched.compat.adapter;

import dev.branched.compat.AbstractModAdapter;
import dev.branched.species.CompatibilityLevel;

/**
 * Biomes O' Plenty 19.0+: preserves biome tree features while converting limbs into
 * 13 dedicated 3D {@code StaticBranchBlock}s via {@code TreeFeatureMixin}.
 */
public final class BiomesOPlentyAdapter extends AbstractModAdapter {
    public BiomesOPlentyAdapter() {
        super("biomesoplenty", "Biomes O' Plenty", "19.0.0", CompatibilityLevel.SPECIAL_PRESERVE);
        feature("biomesoplenty:dead_tree", "biomesoplenty:dead", CompatibilityLevel.SPECIAL_PRESERVE,
                "BOP dead tree; branches converted via TreeFeatureMixin");
        feature("biomesoplenty:empyreal_tree", "biomesoplenty:empyreal", CompatibilityLevel.SPECIAL_PRESERVE,
                "BOP empyreal tree; branches converted via TreeFeatureMixin");
        feature("biomesoplenty:fir_tree", "biomesoplenty:fir", CompatibilityLevel.SPECIAL_PRESERVE,
                "BOP fir tree; branches converted via TreeFeatureMixin");
        feature("biomesoplenty:hellbark_tree", "biomesoplenty:hellbark", CompatibilityLevel.SPECIAL_PRESERVE,
                "BOP hellbark tree; branches converted via TreeFeatureMixin");
        feature("biomesoplenty:jacaranda_tree", "biomesoplenty:jacaranda", CompatibilityLevel.SPECIAL_PRESERVE,
                "BOP jacaranda tree; branches converted via TreeFeatureMixin");
        feature("biomesoplenty:magic_tree", "biomesoplenty:magic", CompatibilityLevel.SPECIAL_PRESERVE,
                "BOP magic tree; branches converted via TreeFeatureMixin");
        feature("biomesoplenty:mahogany_tree", "biomesoplenty:mahogany", CompatibilityLevel.SPECIAL_PRESERVE,
                "BOP mahogany tree; branches converted via TreeFeatureMixin");
        feature("biomesoplenty:maple_tree", "biomesoplenty:maple", CompatibilityLevel.SPECIAL_PRESERVE,
                "BOP maple tree; branches converted via TreeFeatureMixin");
        feature("biomesoplenty:pine_tree", "biomesoplenty:pine", CompatibilityLevel.SPECIAL_PRESERVE,
                "BOP pine tree; branches converted via TreeFeatureMixin");
        feature("biomesoplenty:redwood_tree", "biomesoplenty:redwood", CompatibilityLevel.SPECIAL_PRESERVE,
                "BOP redwood tree; branches converted via TreeFeatureMixin");
        feature("biomesoplenty:umbran_tree", "biomesoplenty:umbran", CompatibilityLevel.SPECIAL_PRESERVE,
                "BOP umbran tree; branches converted via TreeFeatureMixin");
        feature("biomesoplenty:willow_tree", "biomesoplenty:willow", CompatibilityLevel.SPECIAL_PRESERVE,
                "BOP willow tree; branches converted via TreeFeatureMixin");
    }
}
