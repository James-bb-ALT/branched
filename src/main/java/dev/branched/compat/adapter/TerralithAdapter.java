package dev.branched.compat.adapter;

import dev.branched.compat.AbstractModAdapter;
import dev.branched.species.CompatibilityLevel;

/**
 * Terralith 2.5.4. Terralith adds no wood blocks of its own: every mirrored
 * feature reuses a vanilla wood family, so the species column names the
 * shipped morphology profile rather than a Terralith block.
 *
 * <p>Only nine features pass every Phase 3 rule. The rest stay native because
 * they force dirt, alter the ground, carry vines/cocoa/hives, use weighted
 * foliage or live outside the vegetal decoration step. Complete audit:
 * catalogs/phase3-preserved.csv.</p>
 */

public final class TerralithAdapter extends AbstractModAdapter {
    public TerralithAdapter() {

        super("terralith", "Terralith", "2.5.4", CompatibilityLevel.STATIC_NORMAL);

        // Phase 3: replaced by an audited static mirror (catalogs/phase3-replacements.csv).
        feature("terralith:highlands/forest/tall/trees_tall", "minecraft:spruce", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of minecraft:tree");
        feature("terralith:highlands/temperate/cloud_trees", "minecraft:birch", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of minecraft:tree");
        feature("terralith:highlands/temperate/dark_oak", "minecraft:dark_oak", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of minecraft:tree");
        feature("terralith:highlands/temperate/trees_orange", "minecraft:oak", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of minecraft:tree");
        feature("terralith:mountains/misty/trees_base", "minecraft:oak", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of minecraft:tree");
        feature("terralith:mountains/misty/trees_birch", "minecraft:birch", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of minecraft:tree");
        feature("terralith:mountains/misty/trees_mid", "minecraft:oak", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of minecraft:tree");
        feature("terralith:mountains/misty/trees_top", "minecraft:oak", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of minecraft:tree");
        feature("terralith:spires/oak_tree", "minecraft:oak", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of minecraft:tree");

        // Preserved: representative entries; the full audit with reasons is
        // catalogs/phase3-preserved.csv.
        feature("terralith:snowy/trees_center", "terralith:preserved", CompatibilityLevel.SPECIAL_PRESERVE,
                "decorators:minecraft:alter_ground");
        feature("terralith:shield/trees", "terralith:preserved", CompatibilityLevel.SPECIAL_PRESERVE,
                "force_dirt");
        feature("terralith:jungle/vanilla/trees_jungle", "terralith:preserved", CompatibilityLevel.SPECIAL_PRESERVE,
                "decorators:minecraft:cocoa,minecraft:leave_vine,minecraft:trunk_vine;decorators:minecraft:leave_vine,minecraft:trunk_vine;force_dirt;foreign_biome:minecraft:jungle;unresolved_foliage;unsupported_type:missing");
        feature("terralith:dark/trees", "terralith:preserved", CompatibilityLevel.SPECIAL_PRESERVE,
                "force_dirt;foreign_biome:minecraft:dark_forest;missing_profile:minecraft:mushroom_stem+minecraft:brown_mushroom_block;missing_profile:minecraft:mushroom_stem+minecraft:red_mushroom_block;unsupported_type:;unsupported_type:missing");
        feature("terralith:birch/birch_tall", "terralith:preserved", CompatibilityLevel.SPECIAL_PRESERVE,
                "decorators:minecraft:beehive;foreign_biome:minecraft:old_growth_birch_forest;unsupported_type:missing");
        feature("terralith:swamp/vanilla/trees", "terralith:preserved", CompatibilityLevel.SPECIAL_PRESERVE,
                "decorators:minecraft:attached_to_leaves,minecraft:beehive,minecraft:leave_vine;foreign_biome:minecraft:swamp");
        feature("terralith:skylands/trees", "terralith:preserved", CompatibilityLevel.SPECIAL_PRESERVE,
                "force_dirt;unresolved_foliage;unsupported_type:minecraft:no_op");
        feature("terralith:enchanted/trees", "terralith:preserved", CompatibilityLevel.SPECIAL_PRESERVE,
                "force_dirt;missing_profile:minecraft:stripped_warped_stem+minecraft:dark_oak_leaves;unresolved_foliage");
        feature("terralith:cherry/azalea_bush", "terralith:preserved", CompatibilityLevel.SPECIAL_PRESERVE,
                "shrub_silhouette");
    }
}
