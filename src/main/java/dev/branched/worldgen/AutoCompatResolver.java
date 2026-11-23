package dev.branched.worldgen;

import dev.branched.core.ResourceId;

import java.util.List;
import java.util.Locale;

/**
 * Universal auto-compatibility resolver inspired by Dynamic Trees: Universal Compat's
 * heuristic detector and exclusion filters, pairing unknown modded wood blocks with the
 * closest curated {@code StaticBranchBlock} by wood-name keywords or {@code MapColor} RGB.
 */
public final class AutoCompatResolver {
    private static final List<String> LOG_SUFFIXES = List.of(
            "_log", "_wood", "_stem", "_stalk", "_hyphae", "_trunk", "_bark"
    );
    private static final List<String> LOG_EXCLUSIONS = List.of(
            "turnip", "lotus", "pumpkin", "melon", "berry", "bush", "flower", "reed",
            "pipe", "pillar", "cut_log", "driftwood", "chorus", "mushroom", "shroom",
            "bamboo", "cactus", "palm", "coconut", "banana"
    );

    private static final List<String> FOLIAGE_TOKENS = List.of(
            "_leaves", "_leaf", "_needles", "_foliage", "_fronds",
            "_webbing", "_membrane", "_canopy"
    );

    private static final List<String> EXCLUDED_FEATURE_PREFIXES = List.of(
            "net.minecraft.",
            "dev.branched.",
            "party.lemons.biomemakeover.",
            "de.artemis.baobabtree.",
            "biomesoplenty.",
            "com.github.alexmodguy.alexscaves."
    );

    private static final List<String> FEATURE_EXCLUSION_TOKENS = List.of(
            "fallen", "stump", "bush", "shrub", "mushroom", "shroom", "fungus", "fungi",
            "palm", "coconut", "banana", "bamboo", "cactus", "coral", "rock", "ore",
            "crystal", "vine", "snow", "ice", "decor", "patch", "flower", "grass",
            "moss", "kelp", "root", "pillar", "spike", "spire", "pool", "lake",
            "spring", "geode", "blob", "pile", "disk", "fossil", "amethyst",
            "dripstone", "sculk"
    );

    private record BarkSwatch(int rgb, ResourceId branchId) {
    }

    private static final List<BarkSwatch> BARK_PALETTE = List.of(
            new BarkSwatch(0xD8D4CC, BranchMaterials.BIRCH_BRANCH),
            new BarkSwatch(0xC7C1B8, BranchMaterials.PALE_OAK_BRANCH),
            new BarkSwatch(0x7A7773, BranchMaterials.ASHEN_BRANCH),
            new BarkSwatch(0x6E5536, BranchMaterials.OAK_BRANCH),
            new BarkSwatch(0x4A3620, BranchMaterials.SPRUCE_BRANCH),
            new BarkSwatch(0x3A2715, BranchMaterials.DARK_OAK_BRANCH),
            new BarkSwatch(0x221F24, BranchMaterials.BLACKWOOD_BRANCH),
            new BarkSwatch(0x7A372B, BranchMaterials.REDWOOD_BRANCH),
            new BarkSwatch(0x9B5A2E, BranchMaterials.ACACIA_BRANCH),
            new BarkSwatch(0xB86B84, BranchMaterials.CHERRY_BRANCH),
            new BarkSwatch(0x5E476E, BranchMaterials.BOP_JACARANDA_BRANCH),
            new BarkSwatch(0x2E6F78, BranchMaterials.WARPED_BRANCH),
            new BarkSwatch(0x5A6E43, BranchMaterials.WILLOW_BRANCH),
            new BarkSwatch(0x6B252E, BranchMaterials.CRIMSON_BRANCH),
            new BarkSwatch(0xB8933C, BranchMaterials.GOLDEN_OAK_BRANCH),
            new BarkSwatch(0xA68F6B, BranchMaterials.JOSHUA_BRANCH)
    );

    private AutoCompatResolver() {
    }

    public static boolean isHeuristicLogId(ResourceId id) {
        if (id == null) {
            return false;
        }
        String path = id.path().toLowerCase(Locale.ROOT);
        for (String excluded : LOG_EXCLUSIONS) {
            if (path.contains(excluded)) {
                return false;
            }
        }
        for (String suffix : LOG_SUFFIXES) {
            if (path.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isHeuristicFoliageId(ResourceId id) {
        if (id == null) {
            return false;
        }
        String path = id.path().toLowerCase(Locale.ROOT);
        for (String token : FOLIAGE_TOKENS) {
            if (path.endsWith(token) || path.contains(token + "_")) {
                return true;
            }
        }
        return false;
    }

    public static ResourceId resolveBranchForUnknownLog(ResourceId logId, int mapColorRgb) {
        if (logId == null) {
            return BranchMaterials.OAK_BRANCH;
        }
        if (BranchMaterials.hasExactMapping(logId)) {
            return BranchMaterials.resolve(logId);
        }
        ResourceId byKeyword = BranchMaterials.resolveKeywordOrNull(logId);
        if (byKeyword != null) {
            return byKeyword;
        }
        return matchByMapColor(mapColorRgb);
    }

    public static boolean isAutoCompatCustomTreeFeatureClassName(String fqcn) {
        if (fqcn == null || fqcn.isEmpty()) {
            return false;
        }
        for (String prefix : EXCLUDED_FEATURE_PREFIXES) {
            if (fqcn.startsWith(prefix)) {
                return false;
            }
        }
        int lastDot = fqcn.lastIndexOf('.');
        String simpleName = (lastDot >= 0 ? fqcn.substring(lastDot + 1) : fqcn).toLowerCase(Locale.ROOT);
        if (simpleName.contains("$")) {
            simpleName = simpleName.substring(0, simpleName.indexOf('$'));
        }
        for (String token : FEATURE_EXCLUSION_TOKENS) {
            if (simpleName.contains(token)) {
                return false;
            }
        }
        return simpleName.endsWith("treefeature") || simpleName.endsWith("tree");
    }

    private static ResourceId matchByMapColor(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        if (r == 0 && g == 0 && b == 0) {
            return BranchMaterials.OAK_BRANCH;
        }
        ResourceId best = BranchMaterials.OAK_BRANCH;
        long bestDistSq = Long.MAX_VALUE;
        for (BarkSwatch swatch : BARK_PALETTE) {
            int sr = (swatch.rgb() >> 16) & 0xFF;
            int sg = (swatch.rgb() >> 8) & 0xFF;
            int sb = swatch.rgb() & 0xFF;
            long dr = r - sr;
            long dg = g - sg;
            long db = b - sb;
            long distSq = dr * dr + dg * dg + db * db;
            if (distSq < bestDistSq) {
                bestDistSq = distSq;
                best = swatch.branchId();
            }
        }
        return best;
    }
}
