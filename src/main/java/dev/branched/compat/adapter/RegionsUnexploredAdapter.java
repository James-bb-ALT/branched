package dev.branched.compat.adapter;

import dev.branched.compat.AbstractModAdapter;
import dev.branched.species.CompatibilityLevel;

/**
 * Regions Unexplored 0.5.6.
 *
 * <p>Phase 3 audits every placed feature that resolves to a tree. Sixty-one
 * plain tree features are mirrored; shrubs, fallen logs, fungi, landmark-scale
 * trees, decorated trees, block-state-predicated trees and features in
 * non-vegetal steps stay native. The complete audit is
 * catalogs/phase3-{replacements,preserved}.csv and docs/phase3-audit.json.</p>
 */

public final class RegionsUnexploredAdapter extends AbstractModAdapter {
    public RegionsUnexploredAdapter() {

        super("regions_unexplored", "Regions Unexplored", "0.5.6", CompatibilityLevel.STATIC_NORMAL);

        // Phase 3: replaced by an audited static mirror (catalogs/phase3-replacements.csv).
        feature("regions_unexplored:acacia_tree", "minecraft:acacia", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of minecraft:tree");
        feature("regions_unexplored:ashen_pine_tree", "regions_unexplored:ashen_pine", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:ashen_tree");
        feature("regions_unexplored:ashen_tree", "regions_unexplored:ashen", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:ashen_tree");
        feature("regions_unexplored:big_maple_tree_sparse", "regions_unexplored:maple", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of minecraft:tree");
        feature("regions_unexplored:big_mauve_tree", "regions_unexplored:mauve", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of minecraft:tree");
        feature("regions_unexplored:big_oak_tree_dense", "minecraft:oak", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of minecraft:tree");
        feature("regions_unexplored:big_oak_tree_sparse", "minecraft:oak", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of minecraft:tree");
        feature("regions_unexplored:big_orange_maple_tree_on_grass", "regions_unexplored:orange_maple", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of minecraft:tree");
        feature("regions_unexplored:big_red_maple_tree_on_grass", "regions_unexplored:red_maple", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of minecraft:tree");
        feature("regions_unexplored:big_red_maple_tree_sparse", "regions_unexplored:red_maple", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of minecraft:tree");
        feature("regions_unexplored:birch_tree_aspen", "minecraft:birch", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:aspen_tree");
        feature("regions_unexplored:blackwood_tree", "regions_unexplored:blackwood", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of minecraft:tree");
        feature("regions_unexplored:blue_magnolia_tree", "regions_unexplored:blue_magnolia", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:sakura_tree");
        feature("regions_unexplored:cypress_tree", "regions_unexplored:cypress", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:cypress_tree");
        feature("regions_unexplored:dead_bog_tree", "regions_unexplored:dead", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:dead_tree");
        feature("regions_unexplored:dead_bog_tree_rare", "regions_unexplored:dead", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:dead_tree");
        feature("regions_unexplored:dead_scotts_pine_tree", "regions_unexplored:dead_pine", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:stripped_pine_tree");
        feature("regions_unexplored:dead_scotts_pine_tree_mountain", "regions_unexplored:dead_pine", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:stripped_pine_tree");
        feature("regions_unexplored:dead_scotts_pine_tree_mountain_on_snow", "regions_unexplored:dead_pine", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:stripped_pine_tree");
        feature("regions_unexplored:enchanted_birch", "regions_unexplored:enchanted_birch", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:aspen_tree");
        feature("regions_unexplored:enchanted_birch_tall", "regions_unexplored:enchanted_birch", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:aspen_tree");
        feature("regions_unexplored:eucalyptus_tree", "regions_unexplored:eucalyptus", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:eucalyptus_tree");
        feature("regions_unexplored:golden_larch_tree_dense", "regions_unexplored:golden_larch", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:larch_tree");
        feature("regions_unexplored:golden_larch_tree_sparse", "regions_unexplored:golden_larch", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:larch_tree");
        feature("regions_unexplored:jungle_tree_aquatic", "minecraft:jungle", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:sakura_tree");
        feature("regions_unexplored:kapok_tree_dense", "regions_unexplored:kapok", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:kapok_tree");
        feature("regions_unexplored:kapok_tree_sparse", "regions_unexplored:kapok", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:kapok_tree");
        feature("regions_unexplored:larch_tree_dense", "regions_unexplored:larch", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:larch_tree");
        feature("regions_unexplored:larch_tree_sparse", "regions_unexplored:larch", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:larch_tree");
        feature("regions_unexplored:lush_pine_tree", "regions_unexplored:pine", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:lush_pine_tree");
        feature("regions_unexplored:magnolia_tree", "regions_unexplored:magnolia", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:sakura_tree");
        feature("regions_unexplored:maple_tree_dense", "regions_unexplored:maple", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:maple_tree");
        feature("regions_unexplored:maple_tree_on_grass", "regions_unexplored:maple", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:maple_tree");
        feature("regions_unexplored:maple_tree_sparse", "regions_unexplored:maple", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:maple_tree");
        feature("regions_unexplored:oak_tree_with_branch", "minecraft:oak", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:maple_tree");
        feature("regions_unexplored:orange_maple_tree_on_grass", "regions_unexplored:orange_maple", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:maple_tree");
        feature("regions_unexplored:palm_tree_aquatic", "regions_unexplored:palm", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:palm_tree");
        feature("regions_unexplored:palm_tree_dense", "regions_unexplored:palm", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:palm_tree");
        feature("regions_unexplored:palm_tree_dense_tall", "regions_unexplored:palm", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:palm_tree");
        feature("regions_unexplored:palm_tree_sparse", "regions_unexplored:palm", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:palm_tree");
        feature("regions_unexplored:pine_tree", "regions_unexplored:pine", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:pine_tree");
        feature("regions_unexplored:pine_tree_on_dirt", "regions_unexplored:pine", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:pine_tree");
        feature("regions_unexplored:pine_tree_tall_on_dirt", "regions_unexplored:pine", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:pine_tree");
        feature("regions_unexplored:pink_magnolia_tree", "regions_unexplored:pink_magnolia", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:sakura_tree");
        feature("regions_unexplored:red_maple_tree_on_grass", "regions_unexplored:red_maple", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:maple_tree");
        feature("regions_unexplored:red_maple_tree_sparse", "regions_unexplored:red_maple", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:maple_tree");
        feature("regions_unexplored:redwood_tree", "regions_unexplored:redwood", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:redwood_tree");
        feature("regions_unexplored:scotts_pine_tree_mountain", "regions_unexplored:pine", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:stripped_pine_tree");
        feature("regions_unexplored:scotts_pine_tree_mountain_on_snow", "regions_unexplored:pine", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:stripped_pine_tree");
        feature("regions_unexplored:scotts_pine_tree_on_dirt", "regions_unexplored:pine", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:stripped_pine_tree");
        feature("regions_unexplored:scotts_pine_tree_on_snow", "regions_unexplored:pine", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:stripped_pine_tree");
        feature("regions_unexplored:scotts_pine_tree_tall_on_snow", "regions_unexplored:pine", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:stripped_pine_tree");
        feature("regions_unexplored:silver_birch_dense", "regions_unexplored:silver_birch", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:aspen_tree");
        feature("regions_unexplored:silver_birch_on_grass", "regions_unexplored:silver_birch", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:aspen_tree");
        feature("regions_unexplored:silver_birch_tall", "regions_unexplored:silver_birch", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:aspen_tree");
        feature("regions_unexplored:spruce_tree_tall_dense", "minecraft:spruce", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of minecraft:tree");
        feature("regions_unexplored:spruce_tree_tall_on_snow", "minecraft:spruce", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of minecraft:tree");
        feature("regions_unexplored:spruce_tree_tall_sparse", "minecraft:spruce", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of minecraft:tree");
        feature("regions_unexplored:tall_blackwood_tree", "regions_unexplored:blackwood", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:blackwood_tree");
        feature("regions_unexplored:tall_dark_oak", "minecraft:dark_oak", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of minecraft:tree");
        feature("regions_unexplored:white_magnolia_tree", "regions_unexplored:white_magnolia", CompatibilityLevel.STATIC_NORMAL,
                "Phase 3 static mirror of regions_unexplored:sakura_tree");

        // Preserved: representative entries; the full audit with reasons is
        // catalogs/phase3-preserved.csv.
        feature("regions_unexplored:alpha_tree", "regions_unexplored:preserved", CompatibilityLevel.SPECIAL_PRESERVE,
                "profile_not_replaceable:regions_unexplored:alpha");
        feature("regions_unexplored:bamboo_tree", "regions_unexplored:preserved", CompatibilityLevel.SPECIAL_PRESERVE,
                "bamboo_feature");
        feature("regions_unexplored:brim_willow_tree", "regions_unexplored:preserved", CompatibilityLevel.SPECIAL_PRESERVE,
                "non_vegetal_step:7;unresolved_foliage;unresolved_trunk");
        feature("regions_unexplored:cobalt_tree", "regions_unexplored:preserved", CompatibilityLevel.SPECIAL_PRESERVE,
                "non_vegetal_step:7;unresolved_foliage;unresolved_trunk");
        feature("regions_unexplored:fallen_pine_tree", "regions_unexplored:preserved", CompatibilityLevel.SPECIAL_PRESERVE,
                "fallen_log_feature");
        feature("regions_unexplored:giant_cypress_tree", "regions_unexplored:preserved", CompatibilityLevel.SPECIAL_PRESERVE,
                "landmark_scale_feature");
        feature("regions_unexplored:mega_baobab_tree", "regions_unexplored:preserved", CompatibilityLevel.SPECIAL_PRESERVE,
                "landmark_scale_feature");
        feature("regions_unexplored:large_joshua_tree", "regions_unexplored:preserved", CompatibilityLevel.SPECIAL_PRESERVE,
                "joshua_shrub_feature");
        feature("regions_unexplored:large_socotra_tree", "regions_unexplored:preserved", CompatibilityLevel.SPECIAL_PRESERVE,
                "socotra_landmark");
        feature("regions_unexplored:blackwood_mushrooms", "regions_unexplored:preserved", CompatibilityLevel.SPECIAL_PRESERVE,
                "fungus_feature");
        feature("regions_unexplored:big_willow_tree", "regions_unexplored:preserved", CompatibilityLevel.SPECIAL_PRESERVE,
                "decorators:regions_unexplored:chance_willow_trunk_decorator");
        feature("regions_unexplored:big_jungle_tree_dense", "regions_unexplored:preserved", CompatibilityLevel.SPECIAL_PRESERVE,
                "decorators:minecraft:leave_vine");
        feature("regions_unexplored:oak_bush_dense", "regions_unexplored:preserved", CompatibilityLevel.SPECIAL_PRESERVE,
                "shrub_silhouette");
        feature("regions_unexplored:palm_tree_on_sand", "regions_unexplored:preserved", CompatibilityLevel.SPECIAL_PRESERVE,
                "placement_block_state:regions_unexplored:palm_sapling");
        feature("regions_unexplored:giant_blue_bioshroom", "regions_unexplored:preserved", CompatibilityLevel.SPECIAL_PRESERVE,
                "fungus_feature");
    }
}
