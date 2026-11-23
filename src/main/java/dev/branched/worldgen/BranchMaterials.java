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
        String path = log.path().toLowerCase(Locale.ROOT);
        if (path.contains("warped")) return WARPED_BRANCH;
        if (path.contains("crimson")) return CRIMSON_BRANCH;
        if (path.contains("pale_oak")) return PALE_OAK_BRANCH;
        if (path.contains("pine")) return PINE_BRANCH;
        if (path.contains("larch")) return LARCH_BRANCH;
        if (path.contains("spruce") || path.contains("fir")) return SPRUCE_BRANCH;
        if (path.contains("birch") || path.contains("aspen") || path.contains("poplar")) return BIRCH_BRANCH;
        if (path.contains("jungle") || path.contains("mahogany")) return JUNGLE_BRANCH;
        if (path.contains("acacia") || path.contains("savanna")) return ACACIA_BRANCH;
        if (path.contains("dark") || path.contains("black") || path.contains("ebony")) return DARK_OAK_BRANCH;
        if (path.contains("cherry") || path.contains("blossom") || path.contains("pink")) return CHERRY_BRANCH;
        if (path.contains("willow")) return WILLOW_BRANCH;
        if (path.contains("cypress")) return CYPRESS_BRANCH;
        if (path.contains("balsa")) return BM_BLIGHTED_BALSA_BRANCH;
        if (path.contains("mangrove") || path.contains("swamp")) return MANGROVE_BRANCH;
        if (path.contains("maple")) return MAPLE_BRANCH;
        if (path.contains("redwood")) return REDWOOD_BRANCH;
        if (path.contains("baobab")) return BAOBAB_BRANCH;
        if (path.contains("palm") || path.contains("coconut")) return PALM_BRANCH;
        return OAK_BRANCH;
    }

    public static boolean hasBranchMaterial(ResourceId log) {
        if (log == null) {
            throw new IllegalArgumentException("log is required");
        }
        return resolve(log).namespace().equals("branched");
    }
}
