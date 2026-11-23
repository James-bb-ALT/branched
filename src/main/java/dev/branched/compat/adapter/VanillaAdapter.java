package dev.branched.compat.adapter;

import dev.branched.compat.AbstractModAdapter;
import dev.branched.species.CompatibilityLevel;

/** Vanilla tree features, covering taiga, savanna, forest, birch, cherry, jungle and swamp aggregates. */
public final class VanillaAdapter extends AbstractModAdapter {
    public VanillaAdapter() {
        super("minecraft", "Minecraft", "[1.20.1]", CompatibilityLevel.STATIC_NORMAL);
        feature("minecraft:oak", "minecraft:oak", CompatibilityLevel.STATIC_NORMAL);
        feature("minecraft:birch", "minecraft:birch", CompatibilityLevel.STATIC_NORMAL);
        feature("minecraft:spruce", "minecraft:spruce", CompatibilityLevel.STATIC_NORMAL);
        feature("minecraft:jungle_tree", "minecraft:jungle", CompatibilityLevel.STATIC_NORMAL);
        feature("minecraft:acacia", "minecraft:acacia", CompatibilityLevel.STATIC_NORMAL);
        feature("minecraft:dark_oak", "minecraft:dark_oak", CompatibilityLevel.STATIC_NORMAL);
        feature("minecraft:mangrove", "minecraft:mangrove", CompatibilityLevel.STATIC_NORMAL);
        feature("minecraft:cherry", "minecraft:cherry", CompatibilityLevel.STATIC_NORMAL);
        feature("minecraft:fancy_oak", "minecraft:fancy_oak", CompatibilityLevel.STATIC_NORMAL, "tall variant");
        feature("minecraft:pine", "minecraft:pine", CompatibilityLevel.STATIC_NORMAL, "spruce blocks, narrow silhouette");

        // Replaced vanilla biome aggregates
        feature("minecraft:trees_taiga", "minecraft:spruce", CompatibilityLevel.STATIC_NORMAL,
                "selector: spruce 2/3, pine 1/3");
        feature("minecraft:trees_snowy", "minecraft:spruce", CompatibilityLevel.STATIC_NORMAL,
                "snowy plains and ice spikes");
        feature("minecraft:trees_grove", "minecraft:spruce", CompatibilityLevel.STATIC_NORMAL,
                "snow layer placement; selector with pine 1/3");
        feature("minecraft:trees_savanna", "minecraft:oak", CompatibilityLevel.STATIC_NORMAL,
                "selector: oak default, acacia 80%");
        feature("minecraft:trees_windswept_savanna", "minecraft:oak", CompatibilityLevel.STATIC_NORMAL,
                "same selector as trees_savanna, different count");
        feature("minecraft:trees_windswept_forest", "minecraft:oak", CompatibilityLevel.STATIC_NORMAL,
                "selector: oak, spruce 2/3, fancy oak 10%");
        feature("minecraft:trees_windswept_hills", "minecraft:oak", CompatibilityLevel.STATIC_NORMAL,
                "same selector as windswept forest, lower count");
        feature("minecraft:trees_badlands", "minecraft:oak", CompatibilityLevel.STATIC_NORMAL,
                "wooded badlands dirt patches");
        feature("minecraft:trees_birch", "minecraft:birch", CompatibilityLevel.STATIC_NORMAL,
                "birch forest aggregate");
        feature("minecraft:trees_birch_and_oak", "minecraft:oak", CompatibilityLevel.STATIC_NORMAL,
                "forest aggregate: oak, birch 20%, fancy oak 10%");
        feature("minecraft:trees_cherry", "minecraft:cherry", CompatibilityLevel.STATIC_NORMAL,
                "cherry grove aggregate");
        feature("minecraft:trees_flower_forest", "minecraft:oak", CompatibilityLevel.STATIC_NORMAL,
                "flower forest aggregate");
        feature("minecraft:trees_meadow", "minecraft:birch", CompatibilityLevel.STATIC_NORMAL,
                "meadow solitary trees");
        feature("minecraft:trees_jungle", "minecraft:jungle", CompatibilityLevel.STATIC_NORMAL,
                "jungle canopy and parkour boughs with deterministic vines");
        feature("minecraft:trees_sparse_jungle", "minecraft:jungle", CompatibilityLevel.STATIC_NORMAL,
                "sparse jungle canopy with fancy oak");
        feature("minecraft:trees_swamp", "minecraft:oak", CompatibilityLevel.STATIC_NORMAL,
                "swamp oak trees");
        feature("minecraft:trees_old_growth_pine_taiga", "minecraft:pine", CompatibilityLevel.STATIC_NORMAL,
                "old growth pine taiga pine and spruce selector");
        feature("minecraft:trees_old_growth_spruce_taiga", "minecraft:spruce", CompatibilityLevel.STATIC_NORMAL,
                "old growth spruce taiga spruce and pine selector");

        // Preserved: specialized underwater/surface or externally overridden aggregates
        feature("minecraft:trees_plains", "minecraft:oak", CompatibilityLevel.SPECIAL_PRESERVE,
                "replaced by Ecologics walnut trees when ecologics is loaded");
        feature("minecraft:trees_mangrove", "minecraft:mangrove", CompatibilityLevel.SPECIAL_PRESERVE,
                "underwater prop roots, propagules and mud placement");
        feature("minecraft:trees_water", "minecraft:oak", CompatibilityLevel.SPECIAL_PRESERVE,
                "ocean/river surface semantics");
    }

    @Override
    public boolean present(dev.branched.platform.ModPresence presence) {
        return true;
    }
}
