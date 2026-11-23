package dev.branched.migration;

import dev.branched.core.ResourceId;
import dev.branched.worldgen.BranchMaterials;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Pure-Java block palette mapper for converting persisted Dynamic Trees
 * blocks into vanilla/modded logs, leaves, soil and {@code StaticBranchBlock}s.
 *
 * <p>No Dynamic Trees JAR or runtime class is referenced.</p>
 */
public final class DynamicTreesBlockMapper {
    private static final Set<String> DT_NAMESPACES = Set.of(
            "dynamictrees",
            "dtru",
            "dtterralith",
            "dtaether",
            "dtquark",
            "dtfish_of_thieves",
            "dttwilightforest",
            "dynamictreesplus",
            "dtbaobabtree",
            "dtaddon_lib");

    private static final List<String> SPECIAL_KEYWORDS = List.of(
            "mining", "sorting", "time", "transformation", "crystal", "holiday", "hollow", "alpha");

    private static final Map<String, String> TARGET_NAMESPACE = Map.of(
            "dynamictrees", "minecraft",
            "dtterralith", "minecraft",
            "dtru", "regions_unexplored",
            "dtaether", "aether",
            "dtquark", "quark",
            "dtfish_of_thieves", "fishofthieves",
            "dttwilightforest", "twilightforest",
            "dynamictreesplus", "minecraft",
            "dtbaobabtree", "regions_unexplored",
            "dtaddon_lib", "minecraft");

    private static final Map<String, String> LOG_OVERRIDES = Map.ofEntries(
            Map.entry("dynamictrees:crimson", "minecraft:crimson_stem"),
            Map.entry("dynamictrees:warped", "minecraft:warped_stem"),
            Map.entry("dtfish_of_thieves:banana", "fishofthieves:banana_stem"),
            Map.entry("dtfish_of_thieves:mango", "minecraft:oak_log"),
            Map.entry("dtru:ashen_pine", "regions_unexplored:ashen_log"),
            Map.entry("dtru:blue_magnolia", "regions_unexplored:magnolia_log"),
            Map.entry("dtru:pink_magnolia", "regions_unexplored:magnolia_log"),
            Map.entry("dtru:white_magnolia", "regions_unexplored:magnolia_log"),
            Map.entry("dtru:red_maple", "regions_unexplored:maple_log"),
            Map.entry("dtru:orange_maple", "regions_unexplored:maple_log"),
            Map.entry("dtru:enchanted_birch", "regions_unexplored:silver_birch_log"),
            Map.entry("dtru:dead_pine", "regions_unexplored:pine_log"),
            Map.entry("dtru:golden_larch", "regions_unexplored:larch_log"),
            Map.entry("dtquark:blue_blossom", "quark:blossom_log"),
            Map.entry("dtquark:lavender_blossom", "quark:blossom_log"),
            Map.entry("dtquark:orange_blossom", "quark:blossom_log"),
            Map.entry("dtquark:red_blossom", "quark:blossom_log"),
            Map.entry("dtquark:yellow_blossom", "quark:blossom_log"),
            Map.entry("dttwilightforest:darkwood", "twilightforest:dark_log"),
            Map.entry("dttwilightforest:dark", "twilightforest:dark_log"),
            Map.entry("dttwilightforest:canopy_oak", "twilightforest:canopy_log"));

    private static final Map<String, String> LEAVES_OVERRIDES = Map.ofEntries(
            Map.entry("dtfish_of_thieves:coconut", "fishofthieves:coconut_fronds"),
            Map.entry("dtru:ashen_pine", "regions_unexplored:pine_leaves"),
            Map.entry("dtquark:blue_blossom", "quark:blue_blossom_leaves"),
            Map.entry("dtquark:lavender_blossom", "quark:lavender_blossom_leaves"),
            Map.entry("dtquark:orange_blossom", "quark:orange_blossom_leaves"),
            Map.entry("dtquark:red_blossom", "quark:red_blossom_leaves"),
            Map.entry("dtquark:yellow_blossom", "quark:yellow_blossom_leaves"),
            Map.entry("dttwilightforest:darkwood", "twilightforest:dark_leaves"),
            Map.entry("dttwilightforest:canopy_oak", "twilightforest:canopy_leaves"));

    private static final Map<String, String> ROOTY_SOIL = Map.ofEntries(
            Map.entry("rooty_dirt", "minecraft:dirt"),
            Map.entry("rooty_coarse_dirt", "minecraft:coarse_dirt"),
            Map.entry("rooty_grass_block", "minecraft:grass_block"),
            Map.entry("rooty_podzol", "minecraft:podzol"),
            Map.entry("rooty_mycelium", "minecraft:mycelium"),
            Map.entry("rooty_sand", "minecraft:sand"),
            Map.entry("rooty_red_sand", "minecraft:red_sand"),
            Map.entry("rooty_mud", "minecraft:mud"),
            Map.entry("rooty_moss_block", "minecraft:moss_block"),
            Map.entry("rooty_water", "minecraft:water"),
            Map.entry("rooty_netherrack", "minecraft:netherrack"),
            Map.entry("rooty_soul_soil", "minecraft:soul_soil"));

    public enum Action {
        UNCHANGED,
        CONVERTED,
        PRESERVED_SPECIAL
    }

    public record MappingOutcome(Action action,
                                 NbtTag.CompoundTag state,
                                 String category,
                                 String originalId) {
    }

    private DynamicTreesBlockMapper() {
    }

    public static boolean isDynamicTreesNamespace(String namespace) {
        return namespace != null && DT_NAMESPACES.contains(namespace.toLowerCase(Locale.ROOT));
    }

    public static boolean isDynamicTreesId(String blockId) {
        if (blockId == null) {
            return false;
        }
        int colon = blockId.indexOf(':');
        return colon > 0 && isDynamicTreesNamespace(blockId.substring(0, colon));
    }

    public static MappingOutcome map(NbtTag.CompoundTag paletteEntry, boolean preserveSpecialTrees) {
        String rawId = paletteEntry.getString("Name");
        int colon = rawId.indexOf(':');
        if (colon <= 0) {
            return new MappingOutcome(Action.UNCHANGED, paletteEntry, "other", rawId);
        }
        String ns = rawId.substring(0, colon).toLowerCase(Locale.ROOT);
        String path = rawId.substring(colon + 1).toLowerCase(Locale.ROOT);
        if (!DT_NAMESPACES.contains(ns)) {
            return new MappingOutcome(Action.UNCHANGED, paletteEntry, "other", rawId);
        }

        if (preserveSpecialTrees && isSpecialPath(path)) {
            return new MappingOutcome(Action.PRESERVED_SPECIAL, paletteEntry, "special_preserved", rawId);
        }

        if (path.startsWith("rooty_")) {
            return new MappingOutcome(Action.CONVERTED, mapRootySoil(path), "rooty_soil", rawId);
        }
        if (path.endsWith("_root")) {
            return new MappingOutcome(Action.CONVERTED, blockState("minecraft:air", Map.of()), "aerial_root", rawId);
        }
        if (path.endsWith("_branch")) {
            return mapBranch(ns, path, paletteEntry.getCompound("Properties"), rawId);
        }
        if (path.endsWith("_leaves") || path.endsWith("_fronds")) {
            return new MappingOutcome(Action.CONVERTED, mapLeaves(ns, path), "leaves", rawId);
        }
        if (path.endsWith("_seed") || path.endsWith("_sapling")) {
            return new MappingOutcome(Action.CONVERTED, mapSapling(ns, path), "sapling", rawId);
        }

        return new MappingOutcome(Action.UNCHANGED, paletteEntry, "unmapped_dt", rawId);
    }

    private static boolean isSpecialPath(String path) {
        for (String keyword : SPECIAL_KEYWORDS) {
            if (path.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static NbtTag.CompoundTag mapRootySoil(String path) {
        String target = ROOTY_SOIL.getOrDefault(path, "minecraft:dirt");
        if ("minecraft:grass_block".equals(target)
                || "minecraft:podzol".equals(target)
                || "minecraft:mycelium".equals(target)) {
            return blockState(target, Map.of("snowy", "false"));
        }
        if ("minecraft:water".equals(target)) {
            return blockState(target, Map.of("level", "0"));
        }
        return blockState(target, Map.of());
    }

    private static MappingOutcome mapBranch(String ns, String path, NbtTag.CompoundTag props, String rawId) {
        boolean stripped = false;
        String base = path.substring(0, path.length() - "_branch".length());
        if (base.startsWith("thick_")) {
            base = base.substring("thick_".length());
        }
        if (base.startsWith("stripped_")) {
            stripped = true;
            base = base.substring("stripped_".length());
        } else if (base.endsWith("_stripped")) {
            stripped = true;
            base = base.substring(0, base.length() - "_stripped".length());
        }

        int radius = parseRadius(props);
        String logId = resolveLogId(ns, base, stripped);
        if (!stripped && radius < 8) {
            ResourceId logRes = ResourceId.tryParse(logId);
            if (logRes != null && BranchMaterials.hasBranchMaterial(logRes)) {
                String branchId = BranchMaterials.resolve(logRes).toString();
                Map<String, String> defaultBranchProps = new java.util.LinkedHashMap<>();
                defaultBranchProps.put("north", "false");
                defaultBranchProps.put("south", "false");
                defaultBranchProps.put("east", "false");
                defaultBranchProps.put("west", "false");
                defaultBranchProps.put("up", "false");
                defaultBranchProps.put("down", "false");
                defaultBranchProps.put("thickness", "2");
                return new MappingOutcome(Action.CONVERTED,
                        blockState(branchId, defaultBranchProps),
                        "branch", rawId);
            }
        }
        return new MappingOutcome(Action.CONVERTED,
                blockState(logId, Map.of("axis", "y")),
                "trunk", rawId);
    }

    private static String resolveLogId(String ns, String speciesBase, boolean stripped) {
        String override = LOG_OVERRIDES.get(ns + ":" + speciesBase);
        if (override != null) {
            if (stripped) {
                int c = override.indexOf(':');
                return override.substring(0, c + 1) + "stripped_" + override.substring(c + 1);
            }
            return override;
        }
        String targetNs = TARGET_NAMESPACE.getOrDefault(ns, "minecraft");
        String prefix = stripped ? "stripped_" : "";
        return targetNs + ":" + prefix + speciesBase + "_log";
    }

    private static NbtTag.CompoundTag mapLeaves(String ns, String path) {
        String base = path
                .replaceAll("_undergrowth_leaves$", "")
                .replaceAll("_leaves$", "")
                .replaceAll("_fronds$", "");
        String target = LEAVES_OVERRIDES.get(ns + ":" + base);
        if (target == null) {
            String targetNs = TARGET_NAMESPACE.getOrDefault(ns, "minecraft");
            target = targetNs + ":" + base + "_leaves";
        }
        return blockState(target, Map.of(
                "distance", "1",
                "persistent", "false",
                "waterlogged", "false"));
    }

    private static NbtTag.CompoundTag mapSapling(String ns, String path) {
        String base = path.replaceAll("_seed$", "").replaceAll("_sapling$", "");
        if ("dtfish_of_thieves".equals(ns) && "banana".equals(base)) {
            return blockState("minecraft:air", Map.of());
        }
        if ("dttwilightforest".equals(ns) && ("dark".equals(base) || "darkwood".equals(base))) {
            return blockState("twilightforest:darkwood_sapling", Map.of("stage", "0"));
        }
        if ("dtquark".equals(ns) && "blossom".equals(base)) {
            return blockState("quark:blue_blossom_sapling", Map.of("stage", "0"));
        }
        String targetNs = TARGET_NAMESPACE.getOrDefault(ns, "minecraft");
        return blockState(targetNs + ":" + base + "_sapling", Map.of("stage", "0"));
    }

    private static int parseRadius(NbtTag.CompoundTag props) {
        if (props == null) {
            return 8;
        }
        String raw = props.getString("radius");
        if (raw.isEmpty()) {
            return props.getInt("radius", 8);
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ignored) {
            return 8;
        }
    }

    private static NbtTag.CompoundTag blockState(String name, Map<String, String> properties) {
        NbtTag.CompoundTag tag = new NbtTag.CompoundTag();
        tag.putString("Name", name);
        if (!properties.isEmpty()) {
            NbtTag.CompoundTag props = new NbtTag.CompoundTag();
            properties.forEach(props::putString);
            tag.put("Properties", props);
        }
        return tag;
    }
}
