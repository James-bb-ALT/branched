package dev.branched.worldgen;

import dev.branched.core.ResourceId;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Maps every vanilla and modded tree log/wood block to its dedicated 3D
 * {@code StaticBranchBlock} owned by Branched.
 *
 * <p>No tree species ever falls back to placing full 16x16x16 vertical logs on
 * lateral branches: any unknown modded log maps deterministically to the closest
 * matching branch material.</p>
 */
public final class BranchMaterials {
    public static final ResourceId OAK_BRANCH = ResourceId.parse("branched:oak_branch");
    public static final ResourceId SPRUCE_BRANCH = ResourceId.parse("branched:spruce_branch");
    public static final ResourceId BIRCH_BRANCH = ResourceId.parse("branched:birch_branch");
    public static final ResourceId JUNGLE_BRANCH = ResourceId.parse("branched:jungle_branch");
    public static final ResourceId ACACIA_BRANCH = ResourceId.parse("branched:acacia_branch");
    public static final ResourceId DARK_OAK_BRANCH = ResourceId.parse("branched:dark_oak_branch");
    public static final ResourceId MANGROVE_BRANCH = ResourceId.parse("branched:mangrove_branch");
    public static final ResourceId CHERRY_BRANCH = ResourceId.parse("branched:cherry_branch");
    public static final ResourceId CRIMSON_BRANCH = ResourceId.parse("branched:crimson_branch");
    public static final ResourceId WARPED_BRANCH = ResourceId.parse("branched:warped_branch");
    public static final ResourceId PALE_OAK_BRANCH = ResourceId.parse("branched:pale_oak_branch");
    public static final ResourceId POPLAR_BRANCH = ResourceId.parse("branched:poplar_branch");

    public static final ResourceId PINE_BRANCH = ResourceId.parse("branched:pine_branch");
    public static final ResourceId LARCH_BRANCH = ResourceId.parse("branched:larch_branch");
    public static final ResourceId MAPLE_BRANCH = ResourceId.parse("branched:maple_branch");
    public static final ResourceId MAGNOLIA_BRANCH = ResourceId.parse("branched:magnolia_branch");
    public static final ResourceId BLACKWOOD_BRANCH = ResourceId.parse("branched:blackwood_branch");
    public static final ResourceId REDWOOD_BRANCH = ResourceId.parse("branched:redwood_branch");
    public static final ResourceId KAPOK_BRANCH = ResourceId.parse("branched:kapok_branch");
    public static final ResourceId EUCALYPTUS_BRANCH = ResourceId.parse("branched:eucalyptus_branch");
    public static final ResourceId BAOBAB_BRANCH = ResourceId.parse("branched:baobab_branch");
    public static final ResourceId CYPRESS_BRANCH = ResourceId.parse("branched:cypress_branch");
    public static final ResourceId PALM_BRANCH = ResourceId.parse("branched:palm_branch");
    public static final ResourceId WILLOW_BRANCH = ResourceId.parse("branched:willow_branch");
    public static final ResourceId SILVER_BIRCH_BRANCH = ResourceId.parse("branched:silver_birch_branch");
    public static final ResourceId ASHEN_BRANCH = ResourceId.parse("branched:ashen_branch");
    public static final ResourceId DEAD_BRANCH = ResourceId.parse("branched:dead_branch");
    public static final ResourceId MAUVE_BRANCH = ResourceId.parse("branched:mauve_branch");
    public static final ResourceId SOCOTRA_BRANCH = ResourceId.parse("branched:socotra_branch");
    public static final ResourceId JOSHUA_BRANCH = ResourceId.parse("branched:joshua_branch");
    public static final ResourceId BRIMWOOD_BRANCH = ResourceId.parse("branched:brimwood_branch");
    public static final ResourceId COBALT_BRANCH = ResourceId.parse("branched:cobalt_branch");
    public static final ResourceId ALPHA_OAK_BRANCH = ResourceId.parse("branched:alpha_oak_branch");
    public static final ResourceId SMALL_OAK_BRANCH = ResourceId.parse("branched:small_oak_branch");

    public static final ResourceId SKYROOT_BRANCH = ResourceId.parse("branched:skyroot_branch");
    public static final ResourceId GOLDEN_OAK_BRANCH = ResourceId.parse("branched:golden_oak_branch");
    public static final ResourceId ROSEROOT_BRANCH = ResourceId.parse("branched:roseroot_branch");
    public static final ResourceId YAGROOT_BRANCH = ResourceId.parse("branched:yagroot_branch");
    public static final ResourceId CRUDEROOT_BRANCH = ResourceId.parse("branched:cruderoot_branch");
    public static final ResourceId CONBERRY_BRANCH = ResourceId.parse("branched:conberry_branch");
    public static final ResourceId SUNROOT_BRANCH = ResourceId.parse("branched:sunroot_branch");

    public static final ResourceId BLOSSOM_BRANCH = ResourceId.parse("branched:blossom_branch");
    public static final ResourceId ANCIENT_BRANCH = ResourceId.parse("branched:ancient_branch");
    public static final ResourceId AZALEA_BRANCH = ResourceId.parse("branched:azalea_branch");

    public static final ResourceId BANANA_BRANCH = ResourceId.parse("branched:banana_branch");
    public static final ResourceId FOT_COCONUT_BRANCH = ResourceId.parse("branched:fot_coconut_branch");

    public static final ResourceId TWILIGHT_OAK_BRANCH = ResourceId.parse("branched:twilight_oak_branch");
    public static final ResourceId CANOPY_BRANCH = ResourceId.parse("branched:canopy_branch");
    public static final ResourceId DARKWOOD_BRANCH = ResourceId.parse("branched:darkwood_branch");
    public static final ResourceId TWILIGHT_MANGROVE_BRANCH = ResourceId.parse("branched:twilight_mangrove_branch");
    public static final ResourceId MINING_BRANCH = ResourceId.parse("branched:mining_branch");
    public static final ResourceId TIME_BRANCH = ResourceId.parse("branched:time_branch");
    public static final ResourceId TRANSFORMATION_BRANCH = ResourceId.parse("branched:transformation_branch");
    public static final ResourceId SORTING_BRANCH = ResourceId.parse("branched:sorting_branch");

    public static final ResourceId BM_ANCIENT_OAK_BRANCH = ResourceId.parse("branched:bm_ancient_oak_branch");
    public static final ResourceId BM_BLIGHTED_BALSA_BRANCH = ResourceId.parse("branched:bm_blighted_balsa_branch");
    public static final ResourceId BM_SWAMP_CYPRESS_BRANCH = ResourceId.parse("branched:bm_swamp_cypress_branch");
    public static final ResourceId BM_WILLOW_BRANCH = ResourceId.parse("branched:bm_willow_branch");

    public static final ResourceId WALNUT_BRANCH = ResourceId.parse("branched:walnut_branch");
    public static final ResourceId COCONUT_BRANCH = ResourceId.parse("branched:coconut_branch");
    public static final ResourceId ECOLOGICS_AZALEA_BRANCH = ResourceId.parse("branched:ecologics_azalea_branch");
    public static final ResourceId RIVER_BRANCH = ResourceId.parse("branched:river_branch");
    public static final ResourceId BAOBABTREE_BRANCH = ResourceId.parse("branched:baobabtree_branch");
    public static final ResourceId ECHO_BRANCH = ResourceId.parse("branched:echo_branch");
    public static final ResourceId PEWEN_BRANCH = ResourceId.parse("branched:pewen_branch");
    public static final ResourceId THORNWOOD_BRANCH = ResourceId.parse("branched:thornwood_branch");
    public static final ResourceId LICOROOT_BRANCH = ResourceId.parse("branched:licoroot_branch");

    public static final ResourceId ROTTEN_ROSEROOT_BRANCH = ResourceId.parse("branched:rotten_roseroot_branch");
    public static final ResourceId IRONWOOD_BRANCH = ResourceId.parse("branched:ironwood_branch");

    public static final ResourceId DRAGON_TREE_BRANCH = ResourceId.parse("branched:dragon_tree_branch");
    public static final ResourceId HELIX_TREE_BRANCH = ResourceId.parse("branched:helix_tree_branch");
    public static final ResourceId LACUGROVE_BRANCH = ResourceId.parse("branched:lacugrove_branch");
    public static final ResourceId LUCERNIA_BRANCH = ResourceId.parse("branched:lucernia_branch");
    public static final ResourceId PYTHADENDRON_BRANCH = ResourceId.parse("branched:pythadendron_branch");
    public static final ResourceId TENANEA_BRANCH = ResourceId.parse("branched:tenanea_branch");
    public static final ResourceId UMBRELLA_TREE_BRANCH = ResourceId.parse("branched:umbrella_tree_branch");

    public static final ResourceId ANCHOR_TREE_BRANCH = ResourceId.parse("branched:anchor_tree_branch");
    public static final ResourceId GLOOMWOOD_BRANCH = ResourceId.parse("branched:gloomwood_branch");
    public static final ResourceId GLOOMWOOD_DARK_BRANCH = ResourceId.parse("branched:gloomwood_dark_branch");
    public static final ResourceId NETHER_SAKURA_BRANCH = ResourceId.parse("branched:nether_sakura_branch");
    public static final ResourceId RUBEUS_BRANCH = ResourceId.parse("branched:rubeus_branch");
    public static final ResourceId BN_WILLOW_BRANCH = ResourceId.parse("branched:bn_willow_branch");
    public static final ResourceId BN_WART_BRANCH = ResourceId.parse("branched:bn_wart_branch");

    // Biomes O' Plenty
    public static final ResourceId BOP_DEAD_BRANCH = ResourceId.parse("branched:bop_dead_branch");
    public static final ResourceId BOP_EMPYREAL_BRANCH = ResourceId.parse("branched:bop_empyreal_branch");
    public static final ResourceId BOP_FIR_BRANCH = ResourceId.parse("branched:bop_fir_branch");
    public static final ResourceId BOP_HELLBARK_BRANCH = ResourceId.parse("branched:bop_hellbark_branch");
    public static final ResourceId BOP_JACARANDA_BRANCH = ResourceId.parse("branched:bop_jacaranda_branch");
    public static final ResourceId BOP_MAGIC_BRANCH = ResourceId.parse("branched:bop_magic_branch");
    public static final ResourceId BOP_MAHOGANY_BRANCH = ResourceId.parse("branched:bop_mahogany_branch");
    public static final ResourceId BOP_MAPLE_BRANCH = ResourceId.parse("branched:bop_maple_branch");
    public static final ResourceId BOP_PALM_BRANCH = ResourceId.parse("branched:bop_palm_branch");
    public static final ResourceId BOP_PINE_BRANCH = ResourceId.parse("branched:bop_pine_branch");
    public static final ResourceId BOP_REDWOOD_BRANCH = ResourceId.parse("branched:bop_redwood_branch");
    public static final ResourceId BOP_UMBRAN_BRANCH = ResourceId.parse("branched:bop_umbran_branch");
    public static final ResourceId BOP_WILLOW_BRANCH = ResourceId.parse("branched:bop_willow_branch");

    // Oh The Biomes We've Gone
    public static final ResourceId BWG_ASPEN_BRANCH = ResourceId.parse("branched:bwg_aspen_branch");
    public static final ResourceId BWG_BAOBAB_BRANCH = ResourceId.parse("branched:bwg_baobab_branch");
    public static final ResourceId BWG_BLUE_ENCHANTED_BRANCH = ResourceId.parse("branched:bwg_blue_enchanted_branch");
    public static final ResourceId BWG_CIKA_BRANCH = ResourceId.parse("branched:bwg_cika_branch");
    public static final ResourceId BWG_CYPRESS_BRANCH = ResourceId.parse("branched:bwg_cypress_branch");
    public static final ResourceId BWG_EBONY_BRANCH = ResourceId.parse("branched:bwg_ebony_branch");
    public static final ResourceId BWG_FIR_BRANCH = ResourceId.parse("branched:bwg_fir_branch");
    public static final ResourceId BWG_GREEN_ENCHANTED_BRANCH = ResourceId.parse("branched:bwg_green_enchanted_branch");
    public static final ResourceId BWG_HOLLY_BRANCH = ResourceId.parse("branched:bwg_holly_branch");
    public static final ResourceId BWG_IRONWOOD_BRANCH = ResourceId.parse("branched:bwg_ironwood_branch");
    public static final ResourceId BWG_JACARANDA_BRANCH = ResourceId.parse("branched:bwg_jacaranda_branch");
    public static final ResourceId BWG_MAHOGANY_BRANCH = ResourceId.parse("branched:bwg_mahogany_branch");
    public static final ResourceId BWG_MAPLE_BRANCH = ResourceId.parse("branched:bwg_maple_branch");
    public static final ResourceId BWG_PALM_BRANCH = ResourceId.parse("branched:bwg_palm_branch");
    public static final ResourceId BWG_PALO_VERDE_BRANCH = ResourceId.parse("branched:bwg_palo_verde_branch");
    public static final ResourceId BWG_PINE_BRANCH = ResourceId.parse("branched:bwg_pine_branch");
    public static final ResourceId BWG_RAINBOW_EUCALYPTUS_BRANCH = ResourceId.parse("branched:bwg_rainbow_eucalyptus_branch");
    public static final ResourceId BWG_REDWOOD_BRANCH = ResourceId.parse("branched:bwg_redwood_branch");
    public static final ResourceId BWG_SAKURA_BRANCH = ResourceId.parse("branched:bwg_sakura_branch");
    public static final ResourceId BWG_SKYRIS_BRANCH = ResourceId.parse("branched:bwg_skyris_branch");
    public static final ResourceId BWG_WHITE_MANGROVE_BRANCH = ResourceId.parse("branched:bwg_white_mangrove_branch");
    public static final ResourceId BWG_WILLOW_BRANCH = ResourceId.parse("branched:bwg_willow_branch");
    public static final ResourceId BWG_WITCH_HAZEL_BRANCH = ResourceId.parse("branched:bwg_witch_hazel_branch");
    public static final ResourceId BWG_ZELKOVA_BRANCH = ResourceId.parse("branched:bwg_zelkova_branch");

    // Nature's Spirit
    public static final ResourceId NS_ASPEN_BRANCH = ResourceId.parse("branched:ns_aspen_branch");
    public static final ResourceId NS_CEDAR_BRANCH = ResourceId.parse("branched:ns_cedar_branch");
    public static final ResourceId NS_COCONUT_BRANCH = ResourceId.parse("branched:ns_coconut_branch");
    public static final ResourceId NS_CYPRESS_BRANCH = ResourceId.parse("branched:ns_cypress_branch");
    public static final ResourceId NS_FIR_BRANCH = ResourceId.parse("branched:ns_fir_branch");
    public static final ResourceId NS_GHAF_BRANCH = ResourceId.parse("branched:ns_ghaf_branch");
    public static final ResourceId NS_JOSHUA_BRANCH = ResourceId.parse("branched:ns_joshua_branch");
    public static final ResourceId NS_LARCH_BRANCH = ResourceId.parse("branched:ns_larch_branch");
    public static final ResourceId NS_MAHOGANY_BRANCH = ResourceId.parse("branched:ns_mahogany_branch");
    public static final ResourceId NS_MAPLE_BRANCH = ResourceId.parse("branched:ns_maple_branch");
    public static final ResourceId NS_OLIVE_BRANCH = ResourceId.parse("branched:ns_olive_branch");
    public static final ResourceId NS_PALO_VERDE_BRANCH = ResourceId.parse("branched:ns_palo_verde_branch");
    public static final ResourceId NS_REDWOOD_BRANCH = ResourceId.parse("branched:ns_redwood_branch");
    public static final ResourceId NS_SAXAUL_BRANCH = ResourceId.parse("branched:ns_saxaul_branch");
    public static final ResourceId NS_SUGI_BRANCH = ResourceId.parse("branched:ns_sugi_branch");
    public static final ResourceId NS_WILLOW_BRANCH = ResourceId.parse("branched:ns_willow_branch");
    public static final ResourceId NS_WISTERIA_BRANCH = ResourceId.parse("branched:ns_wisteria_branch");

    // Atmospheric
    public static final ResourceId ATM_ASPEN_BRANCH = ResourceId.parse("branched:atm_aspen_branch");
    public static final ResourceId ATM_CRUSTOSE_BRANCH = ResourceId.parse("branched:atm_crustose_branch");
    public static final ResourceId ATM_GRIMWOOD_BRANCH = ResourceId.parse("branched:atm_grimwood_branch");
    public static final ResourceId ATM_KOUSA_BRANCH = ResourceId.parse("branched:atm_kousa_branch");
    public static final ResourceId ATM_LAUREL_BRANCH = ResourceId.parse("branched:atm_laurel_branch");
    public static final ResourceId ATM_MORADO_BRANCH = ResourceId.parse("branched:atm_morado_branch");
    public static final ResourceId ATM_ROSEWOOD_BRANCH = ResourceId.parse("branched:atm_rosewood_branch");
    public static final ResourceId ATM_YUCCA_BRANCH = ResourceId.parse("branched:atm_yucca_branch");

    // Environmental
    public static final ResourceId ENV_PINE_BRANCH = ResourceId.parse("branched:env_pine_branch");
    public static final ResourceId ENV_PLUM_BRANCH = ResourceId.parse("branched:env_plum_branch");
    public static final ResourceId ENV_WILLOW_BRANCH = ResourceId.parse("branched:env_willow_branch");
    public static final ResourceId ENV_WISTERIA_BRANCH = ResourceId.parse("branched:env_wisteria_branch");

    // Autumnity
    public static final ResourceId AUT_MAPLE_BRANCH = ResourceId.parse("branched:aut_maple_branch");

    private static final Map<String, ResourceId> EXACT = buildExactMap();

    private BranchMaterials() {
    }

    private static Map<String, ResourceId> buildExactMap() {
        Map<String, ResourceId> m = new LinkedHashMap<>();
        // Vanilla + VanillaBackport
        m.put("minecraft:oak_log", OAK_BRANCH);
        m.put("minecraft:oak_wood", OAK_BRANCH);
        m.put("minecraft:spruce_log", SPRUCE_BRANCH);
        m.put("minecraft:spruce_wood", SPRUCE_BRANCH);
        m.put("minecraft:birch_log", BIRCH_BRANCH);
        m.put("minecraft:birch_wood", BIRCH_BRANCH);
        m.put("minecraft:jungle_log", JUNGLE_BRANCH);
        m.put("minecraft:jungle_wood", JUNGLE_BRANCH);
        m.put("minecraft:acacia_log", ACACIA_BRANCH);
        m.put("minecraft:acacia_wood", ACACIA_BRANCH);
        m.put("minecraft:dark_oak_log", DARK_OAK_BRANCH);
        m.put("minecraft:dark_oak_wood", DARK_OAK_BRANCH);
        m.put("minecraft:mangrove_log", MANGROVE_BRANCH);
        m.put("minecraft:mangrove_wood", MANGROVE_BRANCH);
        m.put("minecraft:cherry_log", CHERRY_BRANCH);
        m.put("minecraft:cherry_wood", CHERRY_BRANCH);
        m.put("minecraft:crimson_stem", CRIMSON_BRANCH);
        m.put("minecraft:crimson_hyphae", CRIMSON_BRANCH);
        m.put("minecraft:warped_stem", WARPED_BRANCH);
        m.put("minecraft:warped_hyphae", WARPED_BRANCH);
        m.put("minecraft:pale_oak_log", PALE_OAK_BRANCH);
        m.put("minecraft:pale_oak_wood", PALE_OAK_BRANCH);
        m.put("minecraft:poplar_log", POPLAR_BRANCH);
        m.put("minecraft:poplar_wood", POPLAR_BRANCH);

        // Regions Unexplored
        m.put("regions_unexplored:pine_log", PINE_BRANCH);
        m.put("regions_unexplored:pine_wood", PINE_BRANCH);
        m.put("regions_unexplored:larch_log", LARCH_BRANCH);
        m.put("regions_unexplored:larch_wood", LARCH_BRANCH);
        m.put("regions_unexplored:maple_log", MAPLE_BRANCH);
        m.put("regions_unexplored:maple_wood", MAPLE_BRANCH);
        m.put("regions_unexplored:magnolia_log", MAGNOLIA_BRANCH);
        m.put("regions_unexplored:magnolia_wood", MAGNOLIA_BRANCH);
        m.put("regions_unexplored:blackwood_log", BLACKWOOD_BRANCH);
        m.put("regions_unexplored:blackwood_wood", BLACKWOOD_BRANCH);
        m.put("regions_unexplored:redwood_log", REDWOOD_BRANCH);
        m.put("regions_unexplored:redwood_wood", REDWOOD_BRANCH);
        m.put("regions_unexplored:kapok_log", KAPOK_BRANCH);
        m.put("regions_unexplored:kapok_wood", KAPOK_BRANCH);
        m.put("regions_unexplored:eucalyptus_log", EUCALYPTUS_BRANCH);
        m.put("regions_unexplored:eucalyptus_wood", EUCALYPTUS_BRANCH);
        m.put("regions_unexplored:baobab_log", BAOBAB_BRANCH);
        m.put("regions_unexplored:baobab_wood", BAOBAB_BRANCH);
        m.put("regions_unexplored:cypress_log", CYPRESS_BRANCH);
        m.put("regions_unexplored:cypress_wood", CYPRESS_BRANCH);
        m.put("regions_unexplored:palm_log", PALM_BRANCH);
        m.put("regions_unexplored:palm_wood", PALM_BRANCH);
        m.put("regions_unexplored:willow_log", WILLOW_BRANCH);
        m.put("regions_unexplored:willow_wood", WILLOW_BRANCH);
        m.put("regions_unexplored:silver_birch_log", SILVER_BIRCH_BRANCH);
        m.put("regions_unexplored:silver_birch_wood", SILVER_BIRCH_BRANCH);
        m.put("regions_unexplored:ashen_log", ASHEN_BRANCH);
        m.put("regions_unexplored:ashen_wood", ASHEN_BRANCH);
        m.put("regions_unexplored:dead_log", DEAD_BRANCH);
        m.put("regions_unexplored:dead_wood", DEAD_BRANCH);
        m.put("regions_unexplored:mauve_log", MAUVE_BRANCH);
        m.put("regions_unexplored:mauve_wood", MAUVE_BRANCH);
        m.put("regions_unexplored:socotra_log", SOCOTRA_BRANCH);
        m.put("regions_unexplored:socotra_wood", SOCOTRA_BRANCH);
        m.put("regions_unexplored:joshua_log", JOSHUA_BRANCH);
        m.put("regions_unexplored:joshua_wood", JOSHUA_BRANCH);
        m.put("regions_unexplored:brimwood_log", BRIMWOOD_BRANCH);
        m.put("regions_unexplored:brimwood_wood", BRIMWOOD_BRANCH);
        m.put("regions_unexplored:brimwood_log_magma", BRIMWOOD_BRANCH);
        m.put("regions_unexplored:cobalt_log", COBALT_BRANCH);
        m.put("regions_unexplored:cobalt_wood", COBALT_BRANCH);
        m.put("regions_unexplored:alpha_log", ALPHA_OAK_BRANCH);
        m.put("regions_unexplored:alpha_wood", ALPHA_OAK_BRANCH);
        m.put("regions_unexplored:small_oak_log", SMALL_OAK_BRANCH);
        m.put("regions_unexplored:small_oak_wood", SMALL_OAK_BRANCH);
        m.put("regions_unexplored:bamboo_log", JUNGLE_BRANCH);

        // Aether & Deep Aether
        m.put("aether:skyroot_log", SKYROOT_BRANCH);
        m.put("aether:skyroot_wood", SKYROOT_BRANCH);
        m.put("aether:golden_oak_log", GOLDEN_OAK_BRANCH);
        m.put("aether:golden_oak_wood", GOLDEN_OAK_BRANCH);
        m.put("deep_aether:roseroot_log", ROSEROOT_BRANCH);
        m.put("deep_aether:roseroot_wood", ROSEROOT_BRANCH);
        m.put("deep_aether:rotten_roseroot_log", ROTTEN_ROSEROOT_BRANCH);
        m.put("deep_aether:rotten_roseroot_wood", ROTTEN_ROSEROOT_BRANCH);
        m.put("deep_aether:yagroot_log", YAGROOT_BRANCH);
        m.put("deep_aether:yagroot_wood", YAGROOT_BRANCH);
        m.put("deep_aether:cruderoot_log", CRUDEROOT_BRANCH);
        m.put("deep_aether:cruderoot_wood", CRUDEROOT_BRANCH);
        m.put("deep_aether:conberry_log", CONBERRY_BRANCH);
        m.put("deep_aether:conberry_wood", CONBERRY_BRANCH);
        m.put("deep_aether:sunroot_log", SUNROOT_BRANCH);
        m.put("deep_aether:sunroot_wood", SUNROOT_BRANCH);

        // Quark
        m.put("quark:blossom_log", BLOSSOM_BRANCH);
        m.put("quark:blossom_wood", BLOSSOM_BRANCH);
        m.put("quark:ancient_log", ANCIENT_BRANCH);
        m.put("quark:ancient_wood", ANCIENT_BRANCH);
        m.put("quark:azalea_log", AZALEA_BRANCH);
        m.put("quark:azalea_wood", AZALEA_BRANCH);

        // Fish of Thieves
        m.put("fishofthieves:banana_stem", BANANA_BRANCH);
        m.put("fishofthieves:coconut_log", FOT_COCONUT_BRANCH);
        m.put("fishofthieves:coconut_wood", FOT_COCONUT_BRANCH);

        // Twilight Forest
        m.put("twilightforest:twilight_oak_log", TWILIGHT_OAK_BRANCH);
        m.put("twilightforest:twilight_oak_wood", TWILIGHT_OAK_BRANCH);
        m.put("twilightforest:canopy_log", CANOPY_BRANCH);
        m.put("twilightforest:canopy_wood", CANOPY_BRANCH);
        m.put("twilightforest:dark_log", DARKWOOD_BRANCH);
        m.put("twilightforest:dark_wood", DARKWOOD_BRANCH);
        m.put("twilightforest:mangrove_log", TWILIGHT_MANGROVE_BRANCH);
        m.put("twilightforest:mangrove_wood", TWILIGHT_MANGROVE_BRANCH);
        m.put("twilightforest:mining_log", MINING_BRANCH);
        m.put("twilightforest:mining_wood", MINING_BRANCH);
        m.put("twilightforest:time_log", TIME_BRANCH);
        m.put("twilightforest:time_wood", TIME_BRANCH);
        m.put("twilightforest:transformation_log", TRANSFORMATION_BRANCH);
        m.put("twilightforest:transformation_wood", TRANSFORMATION_BRANCH);
        m.put("twilightforest:sorting_log", SORTING_BRANCH);
        m.put("twilightforest:sorting_wood", SORTING_BRANCH);

        // Twilight Delight
        m.put("twilightdelight:ironwood_log", IRONWOOD_BRANCH);
        m.put("twilightdelight:ironwood_wood", IRONWOOD_BRANCH);

        // Biome Makeover
        m.put("biomemakeover:ancient_oak_log", BM_ANCIENT_OAK_BRANCH);
        m.put("biomemakeover:ancient_oak_wood", BM_ANCIENT_OAK_BRANCH);
        m.put("biomemakeover:blighted_balsa_log", BM_BLIGHTED_BALSA_BRANCH);
        m.put("biomemakeover:blighted_balsa_wood", BM_BLIGHTED_BALSA_BRANCH);
        m.put("biomemakeover:swamp_cypress_log", BM_SWAMP_CYPRESS_BRANCH);
        m.put("biomemakeover:swamp_cypress_wood", BM_SWAMP_CYPRESS_BRANCH);
        m.put("biomemakeover:willow_log", BM_WILLOW_BRANCH);
        m.put("biomemakeover:willow_wood", BM_WILLOW_BRANCH);

        // Ecologics, Upgrade Aquatic, BaobabTree, Deeper & Darker, Alex's Caves
        m.put("ecologics:walnut_log", WALNUT_BRANCH);
        m.put("ecologics:walnut_wood", WALNUT_BRANCH);
        m.put("ecologics:coconut_log", COCONUT_BRANCH);
        m.put("ecologics:coconut_wood", COCONUT_BRANCH);
        m.put("ecologics:azalea_log", ECOLOGICS_AZALEA_BRANCH);
        m.put("ecologics:flowering_azalea_log", ECOLOGICS_AZALEA_BRANCH);
        m.put("upgrade_aquatic:river_log", RIVER_BRANCH);
        m.put("upgrade_aquatic:river_wood", RIVER_BRANCH);
        m.put("baobabtree:baobab_log", BAOBABTREE_BRANCH);
        m.put("baobabtree:baobab_wood", BAOBABTREE_BRANCH);
        m.put("deeperdarker:echo_log", ECHO_BRANCH);
        m.put("deeperdarker:echo_wood", ECHO_BRANCH);
        m.put("alexscaves:pewen_log", PEWEN_BRANCH);
        m.put("alexscaves:pewen_wood", PEWEN_BRANCH);
        m.put("alexscaves:thornwood_log", THORNWOOD_BRANCH);
        m.put("alexscaves:thornwood_wood", THORNWOOD_BRANCH);
        m.put("alexscaves:licoroot", LICOROOT_BRANCH);

        // BetterEnd
        m.put("betterend:dragon_tree_log", DRAGON_TREE_BRANCH);
        m.put("betterend:dragon_tree_bark", DRAGON_TREE_BRANCH);
        m.put("betterend:dragon_tree_stripped_log", DRAGON_TREE_BRANCH);
        m.put("betterend:dragon_tree_stripped_bark", DRAGON_TREE_BRANCH);
        m.put("betterend:helix_tree_log", HELIX_TREE_BRANCH);
        m.put("betterend:helix_tree_bark", HELIX_TREE_BRANCH);
        m.put("betterend:helix_tree_stripped_log", HELIX_TREE_BRANCH);
        m.put("betterend:helix_tree_stripped_bark", HELIX_TREE_BRANCH);
        m.put("betterend:lacugrove_log", LACUGROVE_BRANCH);
        m.put("betterend:lacugrove_bark", LACUGROVE_BRANCH);
        m.put("betterend:lacugrove_stripped_log", LACUGROVE_BRANCH);
        m.put("betterend:lacugrove_stripped_bark", LACUGROVE_BRANCH);
        m.put("betterend:lucernia_log", LUCERNIA_BRANCH);
        m.put("betterend:lucernia_bark", LUCERNIA_BRANCH);
        m.put("betterend:lucernia_stripped_log", LUCERNIA_BRANCH);
        m.put("betterend:lucernia_stripped_bark", LUCERNIA_BRANCH);
        m.put("betterend:pythadendron_log", PYTHADENDRON_BRANCH);
        m.put("betterend:pythadendron_bark", PYTHADENDRON_BRANCH);
        m.put("betterend:pythadendron_stripped_log", PYTHADENDRON_BRANCH);
        m.put("betterend:pythadendron_stripped_bark", PYTHADENDRON_BRANCH);
        m.put("betterend:tenanea_log", TENANEA_BRANCH);
        m.put("betterend:tenanea_bark", TENANEA_BRANCH);
        m.put("betterend:tenanea_stripped_log", TENANEA_BRANCH);
        m.put("betterend:tenanea_stripped_bark", TENANEA_BRANCH);
        m.put("betterend:umbrella_tree_log", UMBRELLA_TREE_BRANCH);
        m.put("betterend:umbrella_tree_bark", UMBRELLA_TREE_BRANCH);
        m.put("betterend:umbrella_tree_stripped_log", UMBRELLA_TREE_BRANCH);
        m.put("betterend:umbrella_tree_stripped_bark", UMBRELLA_TREE_BRANCH);

        // BetterNether
        m.put("betternether:anchor_tree_log", ANCHOR_TREE_BRANCH);
        m.put("betternether:anchor_tree_bark", ANCHOR_TREE_BRANCH);
        m.put("betternether:anchor_tree_stripped_log", ANCHOR_TREE_BRANCH);
        m.put("betternether:anchor_tree_stripped_bark", ANCHOR_TREE_BRANCH);
        m.put("betternether:gloomwood_log", GLOOMWOOD_BRANCH);
        m.put("betternether:gloomwood_bark", GLOOMWOOD_BRANCH);
        m.put("betternether:gloomwood_stripped_log", GLOOMWOOD_BRANCH);
        m.put("betternether:gloomwood_stripped_bark", GLOOMWOOD_BRANCH);
        m.put("betternether:gloomwood_transition_log", GLOOMWOOD_BRANCH);
        m.put("betternether:gloomwood_transition_stripped_log", GLOOMWOOD_BRANCH);
        m.put("betternether:gloomwood_dark_log", GLOOMWOOD_DARK_BRANCH);
        m.put("betternether:gloomwood_dark_bark", GLOOMWOOD_DARK_BRANCH);
        m.put("betternether:gloomwood_dark_molten_log", GLOOMWOOD_DARK_BRANCH);
        m.put("betternether:gloomwood_dark_stripped_log", GLOOMWOOD_DARK_BRANCH);
        m.put("betternether:gloomwood_dark_stripped_bark", GLOOMWOOD_DARK_BRANCH);
        m.put("betternether:nether_sakura_log", NETHER_SAKURA_BRANCH);
        m.put("betternether:nether_sakura_bark", NETHER_SAKURA_BRANCH);
        m.put("betternether:nether_sakura_stripped_log", NETHER_SAKURA_BRANCH);
        m.put("betternether:nether_sakura_stripped_bark", NETHER_SAKURA_BRANCH);
        m.put("betternether:rubeus_log", RUBEUS_BRANCH);
        m.put("betternether:rubeus_bark", RUBEUS_BRANCH);
        m.put("betternether:rubeus_stripped_log", RUBEUS_BRANCH);
        m.put("betternether:rubeus_stripped_bark", RUBEUS_BRANCH);
        m.put("betternether:willow_log", BN_WILLOW_BRANCH);
        m.put("betternether:willow_bark", BN_WILLOW_BRANCH);
        m.put("betternether:willow_trunk", BN_WILLOW_BRANCH);
        m.put("betternether:willow_stripped_log", BN_WILLOW_BRANCH);
        m.put("betternether:willow_stripped_bark", BN_WILLOW_BRANCH);
        m.put("betternether:wart_log", BN_WART_BRANCH);
        m.put("betternether:wart_bark", BN_WART_BRANCH);
        m.put("betternether:wart_stripped_log", BN_WART_BRANCH);
        m.put("betternether:wart_stripped_bark", BN_WART_BRANCH);

        // Biomes O' Plenty
        m.put("biomesoplenty:dead_log", BOP_DEAD_BRANCH);
        m.put("biomesoplenty:dead_wood", BOP_DEAD_BRANCH);
        m.put("biomesoplenty:empyreal_log", BOP_EMPYREAL_BRANCH);
        m.put("biomesoplenty:empyreal_wood", BOP_EMPYREAL_BRANCH);
        m.put("biomesoplenty:fir_log", BOP_FIR_BRANCH);
        m.put("biomesoplenty:fir_wood", BOP_FIR_BRANCH);
        m.put("biomesoplenty:hellbark_log", BOP_HELLBARK_BRANCH);
        m.put("biomesoplenty:hellbark_wood", BOP_HELLBARK_BRANCH);
        m.put("biomesoplenty:jacaranda_log", BOP_JACARANDA_BRANCH);
        m.put("biomesoplenty:jacaranda_wood", BOP_JACARANDA_BRANCH);
        m.put("biomesoplenty:magic_log", BOP_MAGIC_BRANCH);
        m.put("biomesoplenty:magic_wood", BOP_MAGIC_BRANCH);
        m.put("biomesoplenty:mahogany_log", BOP_MAHOGANY_BRANCH);
        m.put("biomesoplenty:mahogany_wood", BOP_MAHOGANY_BRANCH);
        m.put("biomesoplenty:maple_log", BOP_MAPLE_BRANCH);
        m.put("biomesoplenty:maple_wood", BOP_MAPLE_BRANCH);
        m.put("biomesoplenty:palm_log", BOP_PALM_BRANCH);
        m.put("biomesoplenty:palm_wood", BOP_PALM_BRANCH);
        m.put("biomesoplenty:pine_log", BOP_PINE_BRANCH);
        m.put("biomesoplenty:pine_wood", BOP_PINE_BRANCH);
        m.put("biomesoplenty:redwood_log", BOP_REDWOOD_BRANCH);
        m.put("biomesoplenty:redwood_wood", BOP_REDWOOD_BRANCH);
        m.put("biomesoplenty:umbran_log", BOP_UMBRAN_BRANCH);
        m.put("biomesoplenty:umbran_wood", BOP_UMBRAN_BRANCH);
        m.put("biomesoplenty:willow_log", BOP_WILLOW_BRANCH);
        m.put("biomesoplenty:willow_wood", BOP_WILLOW_BRANCH);

        // Oh The Biomes We've Gone
        m.put("biomeswevegone:aspen_log", BWG_ASPEN_BRANCH);
        m.put("biomeswevegone:aspen_wood", BWG_ASPEN_BRANCH);
        m.put("biomeswevegone:baobab_log", BWG_BAOBAB_BRANCH);
        m.put("biomeswevegone:baobab_wood", BWG_BAOBAB_BRANCH);
        m.put("biomeswevegone:blue_enchanted_log", BWG_BLUE_ENCHANTED_BRANCH);
        m.put("biomeswevegone:blue_enchanted_wood", BWG_BLUE_ENCHANTED_BRANCH);
        m.put("biomeswevegone:cika_log", BWG_CIKA_BRANCH);
        m.put("biomeswevegone:cika_wood", BWG_CIKA_BRANCH);
        m.put("biomeswevegone:cypress_log", BWG_CYPRESS_BRANCH);
        m.put("biomeswevegone:cypress_wood", BWG_CYPRESS_BRANCH);
        m.put("biomeswevegone:ebony_log", BWG_EBONY_BRANCH);
        m.put("biomeswevegone:ebony_wood", BWG_EBONY_BRANCH);
        m.put("biomeswevegone:fir_log", BWG_FIR_BRANCH);
        m.put("biomeswevegone:fir_wood", BWG_FIR_BRANCH);
        m.put("biomeswevegone:green_enchanted_log", BWG_GREEN_ENCHANTED_BRANCH);
        m.put("biomeswevegone:green_enchanted_wood", BWG_GREEN_ENCHANTED_BRANCH);
        m.put("biomeswevegone:holly_log", BWG_HOLLY_BRANCH);
        m.put("biomeswevegone:holly_wood", BWG_HOLLY_BRANCH);
        m.put("biomeswevegone:ironwood_log", BWG_IRONWOOD_BRANCH);
        m.put("biomeswevegone:ironwood_wood", BWG_IRONWOOD_BRANCH);
        m.put("biomeswevegone:jacaranda_log", BWG_JACARANDA_BRANCH);
        m.put("biomeswevegone:jacaranda_wood", BWG_JACARANDA_BRANCH);
        m.put("biomeswevegone:mahogany_log", BWG_MAHOGANY_BRANCH);
        m.put("biomeswevegone:mahogany_wood", BWG_MAHOGANY_BRANCH);
        m.put("biomeswevegone:maple_log", BWG_MAPLE_BRANCH);
        m.put("biomeswevegone:maple_wood", BWG_MAPLE_BRANCH);
        m.put("biomeswevegone:palm_log", BWG_PALM_BRANCH);
        m.put("biomeswevegone:palm_wood", BWG_PALM_BRANCH);
        m.put("biomeswevegone:palo_verde_log", BWG_PALO_VERDE_BRANCH);
        m.put("biomeswevegone:palo_verde_wood", BWG_PALO_VERDE_BRANCH);
        m.put("biomeswevegone:pine_log", BWG_PINE_BRANCH);
        m.put("biomeswevegone:pine_wood", BWG_PINE_BRANCH);
        m.put("biomeswevegone:rainbow_eucalyptus_log", BWG_RAINBOW_EUCALYPTUS_BRANCH);
        m.put("biomeswevegone:rainbow_eucalyptus_wood", BWG_RAINBOW_EUCALYPTUS_BRANCH);
        m.put("biomeswevegone:redwood_log", BWG_REDWOOD_BRANCH);
        m.put("biomeswevegone:redwood_wood", BWG_REDWOOD_BRANCH);
        m.put("biomeswevegone:sakura_log", BWG_SAKURA_BRANCH);
        m.put("biomeswevegone:sakura_wood", BWG_SAKURA_BRANCH);
        m.put("biomeswevegone:skyris_log", BWG_SKYRIS_BRANCH);
        m.put("biomeswevegone:skyris_wood", BWG_SKYRIS_BRANCH);
        m.put("biomeswevegone:white_mangrove_log", BWG_WHITE_MANGROVE_BRANCH);
        m.put("biomeswevegone:white_mangrove_wood", BWG_WHITE_MANGROVE_BRANCH);
        m.put("biomeswevegone:willow_log", BWG_WILLOW_BRANCH);
        m.put("biomeswevegone:willow_wood", BWG_WILLOW_BRANCH);
        m.put("biomeswevegone:witch_hazel_log", BWG_WITCH_HAZEL_BRANCH);
        m.put("biomeswevegone:witch_hazel_wood", BWG_WITCH_HAZEL_BRANCH);
        m.put("biomeswevegone:zelkova_log", BWG_ZELKOVA_BRANCH);
        m.put("biomeswevegone:zelkova_wood", BWG_ZELKOVA_BRANCH);

        // Nature's Spirit
        m.put("natures_spirit:aspen_log", NS_ASPEN_BRANCH);
        m.put("natures_spirit:aspen_wood", NS_ASPEN_BRANCH);
        m.put("natures_spirit:cedar_log", NS_CEDAR_BRANCH);
        m.put("natures_spirit:cedar_wood", NS_CEDAR_BRANCH);
        m.put("natures_spirit:coconut_log", NS_COCONUT_BRANCH);
        m.put("natures_spirit:coconut_wood", NS_COCONUT_BRANCH);
        m.put("natures_spirit:cypress_log", NS_CYPRESS_BRANCH);
        m.put("natures_spirit:cypress_wood", NS_CYPRESS_BRANCH);
        m.put("natures_spirit:fir_log", NS_FIR_BRANCH);
        m.put("natures_spirit:fir_wood", NS_FIR_BRANCH);
        m.put("natures_spirit:ghaf_log", NS_GHAF_BRANCH);
        m.put("natures_spirit:ghaf_wood", NS_GHAF_BRANCH);
        m.put("natures_spirit:joshua_log", NS_JOSHUA_BRANCH);
        m.put("natures_spirit:joshua_wood", NS_JOSHUA_BRANCH);
        m.put("natures_spirit:larch_log", NS_LARCH_BRANCH);
        m.put("natures_spirit:larch_wood", NS_LARCH_BRANCH);
        m.put("natures_spirit:mahogany_log", NS_MAHOGANY_BRANCH);
        m.put("natures_spirit:mahogany_wood", NS_MAHOGANY_BRANCH);
        m.put("natures_spirit:maple_log", NS_MAPLE_BRANCH);
        m.put("natures_spirit:maple_wood", NS_MAPLE_BRANCH);
        m.put("natures_spirit:olive_log", NS_OLIVE_BRANCH);
        m.put("natures_spirit:olive_wood", NS_OLIVE_BRANCH);
        m.put("natures_spirit:palo_verde_log", NS_PALO_VERDE_BRANCH);
        m.put("natures_spirit:palo_verde_wood", NS_PALO_VERDE_BRANCH);
        m.put("natures_spirit:redwood_log", NS_REDWOOD_BRANCH);
        m.put("natures_spirit:redwood_wood", NS_REDWOOD_BRANCH);
        m.put("natures_spirit:saxaul_log", NS_SAXAUL_BRANCH);
        m.put("natures_spirit:saxaul_wood", NS_SAXAUL_BRANCH);
        m.put("natures_spirit:sugi_log", NS_SUGI_BRANCH);
        m.put("natures_spirit:sugi_wood", NS_SUGI_BRANCH);
        m.put("natures_spirit:willow_log", NS_WILLOW_BRANCH);
        m.put("natures_spirit:willow_wood", NS_WILLOW_BRANCH);
        m.put("natures_spirit:wisteria_log", NS_WISTERIA_BRANCH);
        m.put("natures_spirit:wisteria_wood", NS_WISTERIA_BRANCH);

        // Atmospheric
        m.put("atmospheric:aspen_log", ATM_ASPEN_BRANCH);
        m.put("atmospheric:aspen_wood", ATM_ASPEN_BRANCH);
        m.put("atmospheric:watchful_aspen_log", ATM_ASPEN_BRANCH);
        m.put("atmospheric:watchful_aspen_wood", ATM_ASPEN_BRANCH);
        m.put("atmospheric:crustose_log", ATM_CRUSTOSE_BRANCH);
        m.put("atmospheric:crustose_wood", ATM_CRUSTOSE_BRANCH);
        m.put("atmospheric:grimwood_log", ATM_GRIMWOOD_BRANCH);
        m.put("atmospheric:grimwood_wood", ATM_GRIMWOOD_BRANCH);
        m.put("atmospheric:kousa_log", ATM_KOUSA_BRANCH);
        m.put("atmospheric:kousa_wood", ATM_KOUSA_BRANCH);
        m.put("atmospheric:laurel_log", ATM_LAUREL_BRANCH);
        m.put("atmospheric:laurel_wood", ATM_LAUREL_BRANCH);
        m.put("atmospheric:morado_log", ATM_MORADO_BRANCH);
        m.put("atmospheric:morado_wood", ATM_MORADO_BRANCH);
        m.put("atmospheric:rosewood_log", ATM_ROSEWOOD_BRANCH);
        m.put("atmospheric:rosewood_wood", ATM_ROSEWOOD_BRANCH);
        m.put("atmospheric:yucca_log", ATM_YUCCA_BRANCH);
        m.put("atmospheric:yucca_wood", ATM_YUCCA_BRANCH);

        // Environmental
        m.put("environmental:pine_log", ENV_PINE_BRANCH);
        m.put("environmental:pine_wood", ENV_PINE_BRANCH);
        m.put("environmental:plum_log", ENV_PLUM_BRANCH);
        m.put("environmental:plum_wood", ENV_PLUM_BRANCH);
        m.put("environmental:willow_log", ENV_WILLOW_BRANCH);
        m.put("environmental:willow_wood", ENV_WILLOW_BRANCH);
        m.put("environmental:wisteria_log", ENV_WISTERIA_BRANCH);
        m.put("environmental:wisteria_wood", ENV_WISTERIA_BRANCH);

        // Autumnity
        m.put("autumnity:maple_log", AUT_MAPLE_BRANCH);
        m.put("autumnity:maple_wood", AUT_MAPLE_BRANCH);
        m.put("autumnity:sappy_maple_log", AUT_MAPLE_BRANCH);
        m.put("autumnity:sappy_maple_wood", AUT_MAPLE_BRANCH);
        return Map.copyOf(m);
    }

    private static ResourceId findExact(ResourceId log) {
        if (log == null) {
            return null;
        }
        ResourceId mapped = EXACT.get(log.toString());
        if (mapped != null) {
            return mapped;
        }
        String path = log.path().toLowerCase(Locale.ROOT);
        if (path.startsWith("stripped_")) {
            return EXACT.get(log.namespace() + ":" + path.substring("stripped_".length()));
        }
        if (path.startsWith("hollow_")) {
            return EXACT.get(log.namespace() + ":" + path.substring("hollow_".length()));
        }
        return null;
    }

    /**
     * Returns {@code true} only when the log/wood block (including its stripped or
     * hollow variant) has an exact curated bark material mapping.
     */
    public static boolean hasExactMapping(ResourceId log) {
        return log != null && findExact(log) != null;
    }

    /**
     * Resolves a species log/wood id to a Branched {@code StaticBranchBlock}.
     * Unknown logs map deterministically to the closest wood-family branch block
     * and never fall back to full 16x16x16 logs.
     */
    public static ResourceId resolve(ResourceId log) {
        if (log == null) {
            throw new IllegalArgumentException("log is required");
        }
        ResourceId mapped = findExact(log);
        if (mapped != null) {
            return mapped;
        }
        ResourceId byKeyword = resolveKeywordOrNull(log);
        return byKeyword != null ? byKeyword : OAK_BRANCH;
    }

    /**
     * Resolves an unknown log id using wood-family keyword matching, or returns
     * {@code null} when no keyword matches so auto-compat can fall back to MapColor RGB.
     */
    public static ResourceId resolveKeywordOrNull(ResourceId log) {
        if (log == null) {
            return null;
        }
        String path = log.path().toLowerCase(Locale.ROOT);
        if (path.contains("warped")) return WARPED_BRANCH;
        if (path.contains("crimson")) return CRIMSON_BRANCH;
        if (path.contains("pale_oak")) return PALE_OAK_BRANCH;
        if (path.contains("cedar")) return NS_CEDAR_BRANCH;
        if (path.contains("fir")) return BOP_FIR_BRANCH;
        if (path.contains("pine")) return PINE_BRANCH;
        if (path.contains("larch")) return LARCH_BRANCH;
        if (path.contains("spruce")) return SPRUCE_BRANCH;
        if (path.contains("birch") || path.contains("aspen") || path.contains("poplar")) return BIRCH_BRANCH;
        if (path.contains("mahogany")) return BOP_MAHOGANY_BRANCH;
        if (path.contains("jungle")) return JUNGLE_BRANCH;
        if (path.contains("acacia") || path.contains("savanna")) return ACACIA_BRANCH;
        if (path.contains("ebony") || path.contains("dark") || path.contains("black")) return DARK_OAK_BRANCH;
        if (path.contains("sakura")) return BWG_SAKURA_BRANCH;
        if (path.contains("cherry") || path.contains("blossom") || path.contains("pink")) return CHERRY_BRANCH;
        if (path.contains("jacaranda")) return BOP_JACARANDA_BRANCH;
        if (path.contains("magic")) return BOP_MAGIC_BRANCH;
        if (path.contains("umbran")) return BOP_UMBRAN_BRANCH;
        if (path.contains("hellbark")) return BOP_HELLBARK_BRANCH;
        if (path.contains("willow")) return WILLOW_BRANCH;
        if (path.contains("cypress")) return CYPRESS_BRANCH;
        if (path.contains("balsa")) return BM_BLIGHTED_BALSA_BRANCH;
        if (path.contains("mangrove") || path.contains("swamp")) return MANGROVE_BRANCH;
        if (path.contains("maple")) return MAPLE_BRANCH;
        if (path.contains("redwood")) return REDWOOD_BRANCH;
        if (path.contains("baobab")) return BAOBAB_BRANCH;
        if (path.contains("palm") || path.contains("coconut")) return PALM_BRANCH;
        if (path.contains("olive")) return NS_OLIVE_BRANCH;
        if (path.contains("wisteria")) return NS_WISTERIA_BRANCH;
        if (path.contains("plum")) return ENV_PLUM_BRANCH;
        if (path.contains("laurel")) return ATM_LAUREL_BRANCH;
        if (path.contains("rosewood")) return ATM_ROSEWOOD_BRANCH;
        if (path.contains("holly")) return BWG_HOLLY_BRANCH;
        if (path.contains("zelkova")) return BWG_ZELKOVA_BRANCH;
        if (path.contains("ironwood")) return IRONWOOD_BRANCH;
        if (path.contains("yucca")) return ATM_YUCCA_BRANCH;
        if (path.contains("sugi")) return NS_SUGI_BRANCH;
        if (path.contains("ghaf")) return NS_GHAF_BRANCH;
        if (path.contains("saxaul")) return NS_SAXAUL_BRANCH;
        if (path.contains("palo_verde")) return BWG_PALO_VERDE_BRANCH;
        if (path.contains("cika")) return BWG_CIKA_BRANCH;
        if (path.contains("skyris")) return BWG_SKYRIS_BRANCH;
        if (path.contains("witch_hazel")) return BWG_WITCH_HAZEL_BRANCH;
        if (path.contains("kousa")) return ATM_KOUSA_BRANCH;
        if (path.contains("morado")) return ATM_MORADO_BRANCH;
        if (path.contains("grimwood")) return ATM_GRIMWOOD_BRANCH;
        if (path.contains("joshua")) return JOSHUA_BRANCH;
        if (path.contains("socotra")) return SOCOTRA_BRANCH;
        if (path.contains("ashen")) return ASHEN_BRANCH;
        if (path.contains("dead")) return DEAD_BRANCH;
        if (path.contains("mauve")) return MAUVE_BRANCH;
        if (path.contains("magnolia")) return MAGNOLIA_BRANCH;
        if (path.contains("echo")) return ECHO_BRANCH;
        if (path.contains("cobalt")) return COBALT_BRANCH;
        if (path.contains("brimwood")) return BRIMWOOD_BRANCH;
        return null;
    }

    public static boolean hasBranchMaterial(ResourceId log) {
        if (log == null) {
            throw new IllegalArgumentException("log is required");
        }
        return resolve(log).namespace().equals("branched");
    }
}
