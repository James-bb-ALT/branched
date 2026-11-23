package dev.branched.compat.adapter;

import dev.branched.compat.AbstractModAdapter;
import dev.branched.species.CompatibilityLevel;

/**
 * Oh The Biomes We've Gone 1.0+: preserves NBT-templated and configured trees while
 * converting limbs into 24 dedicated 3D {@code StaticBranchBlock}s via
 * {@code FeatureMixin} ({@code TreeFromStructureNBTFeature}) and {@code TreeFeatureMixin}.
 */
public final class BiomesWeveGoneAdapter extends AbstractModAdapter {
    public BiomesWeveGoneAdapter() {
        super("biomeswevegone", "Oh The Biomes We've Gone", "1.0.0", CompatibilityLevel.SPECIAL_PRESERVE);
        feature("biomeswevegone:aspen_tree", "biomeswevegone:aspen", CompatibilityLevel.SPECIAL_PRESERVE,
                "BWG aspen tree; branches converted via FeatureMixin");
        feature("biomeswevegone:baobab_tree", "biomeswevegone:baobab", CompatibilityLevel.SPECIAL_PRESERVE,
                "BWG baobab tree; branches converted via FeatureMixin");
        feature("biomeswevegone:cypress_tree", "biomeswevegone:cypress", CompatibilityLevel.SPECIAL_PRESERVE,
                "BWG cypress tree; branches converted via FeatureMixin");
        feature("biomeswevegone:ebony_tree", "biomeswevegone:ebony", CompatibilityLevel.SPECIAL_PRESERVE,
                "BWG ebony tree; branches converted via FeatureMixin");
        feature("biomeswevegone:fir_tree", "biomeswevegone:fir", CompatibilityLevel.SPECIAL_PRESERVE,
                "BWG fir tree; branches converted via FeatureMixin");
        feature("biomeswevegone:jacaranda_tree", "biomeswevegone:jacaranda", CompatibilityLevel.SPECIAL_PRESERVE,
                "BWG jacaranda tree; branches converted via FeatureMixin");
        feature("biomeswevegone:mahogany_tree", "biomeswevegone:mahogany", CompatibilityLevel.SPECIAL_PRESERVE,
                "BWG mahogany tree; branches converted via FeatureMixin");
        feature("biomeswevegone:maple_tree", "biomeswevegone:maple", CompatibilityLevel.SPECIAL_PRESERVE,
                "BWG maple tree; branches converted via FeatureMixin");
        feature("biomeswevegone:redwood_tree", "biomeswevegone:redwood", CompatibilityLevel.SPECIAL_PRESERVE,
                "BWG redwood tree; branches converted via FeatureMixin");
        feature("biomeswevegone:sakura_tree", "biomeswevegone:sakura", CompatibilityLevel.SPECIAL_PRESERVE,
                "BWG sakura tree; branches converted via FeatureMixin");
        feature("biomeswevegone:skyris_tree", "biomeswevegone:skyris", CompatibilityLevel.SPECIAL_PRESERVE,
                "BWG skyris tree; branches converted via FeatureMixin");
        feature("biomeswevegone:willow_tree", "biomeswevegone:willow", CompatibilityLevel.SPECIAL_PRESERVE,
                "BWG willow tree; branches converted via FeatureMixin");
        feature("biomeswevegone:zelkova_tree", "biomeswevegone:zelkova", CompatibilityLevel.SPECIAL_PRESERVE,
                "BWG zelkova tree; branches converted via FeatureMixin");
    }
}
