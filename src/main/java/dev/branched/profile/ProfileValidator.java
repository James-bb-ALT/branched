package dev.branched.profile;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import dev.branched.core.ResourceId;
import dev.branched.morphology.DoubleRange;
import dev.branched.morphology.IntRange;
import dev.branched.species.BranchPattern;
import dev.branched.species.CompatibilityLevel;
import dev.branched.species.SpeciesProfile;
import dev.branched.species.TreeArchetype;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Strict validator for {@code arboreal/species/*.json}. It collects every issue
 * instead of failing fast, so a datapack author sees all problems at once.
 */
public final class ProfileValidator {
    public static final int CURRENT_SCHEMA = 1;

    private static final Set<String> ALLOWED_TOP_LEVEL = Set.of(
            "schema", "species", "archetype", "logs", "wood", "leaves", "sapling",
            "trunk", "branches", "crown", "generation", "budget", "biomes",
            "compatibility", "source", "fruitBearing", "notes", "comment");

    public ValidationResult validate(ResourceId source, JsonObject json) {
        List<ValidationIssue> issues = new ArrayList<>();
        if (json == null) {
            issues.add(ValidationIssue.error("null_json", "$", "profile is null"));
            return new ValidationResult(source, null, issues);
        }
        for (String key : json.keySet()) {
            if (!ALLOWED_TOP_LEVEL.contains(key)) {
                issues.add(ValidationIssue.warning("unknown_key", "$." + key, "unknown top-level key"));
            }
        }

        int schema = intValue(json, "schema", "$.schema", issues, -1);
        if (schema != CURRENT_SCHEMA) {
            issues.add(ValidationIssue.error("schema", "$.schema",
                    "unsupported schema " + schema + ", expected " + CURRENT_SCHEMA));
        }

        ResourceId id = resourceId(json, "species", "$.species", issues, true);
        TreeArchetype archetype = enumValue(json, "archetype", "$.archetype", TreeArchetype.class, issues);
        ResourceId log = resourceId(json, "logs", "$.logs", issues, true);
        ResourceId wood = resourceId(json, "wood", "$.wood", issues, false);
        ResourceId leaves = resourceId(json, "leaves", "$.leaves", issues, true);
        ResourceId sapling = resourceId(json, "sapling", "$.sapling", issues, false);
        if (log != null && log.equals(leaves)) {
            issues.add(ValidationIssue.error("log_equals_leaves", "$.leaves", "log and leaves must differ"));
        }

        JsonObject trunk = object(json, "trunk", "$.trunk", issues, true);
        DoubleRange height = range(trunk, "height", "$.trunk.height", 2.0D, 64.0D, issues, true);
        DoubleRange radius = range(trunk, "radius", "$.trunk.radius", 0.2D, 6.0D, issues, true);
        DoubleRange bend = range(trunk, "bend", "$.trunk.bend", 0.0D, 1.0D, issues, true);

        JsonObject branches = object(json, "branches", "$.branches", issues, true);
        IntRange primary = intRange(branches, "primaryCount", "$.branches.primaryCount", 0, 64, issues, true);
        IntRange depth = intRange(branches, "depth", "$.branches.depth", 1, 6, issues, true);
        DoubleRange angle = range(branches, "angleDegrees", "$.branches.angleDegrees", 0.0D, 110.0D, issues, true);
        DoubleRange lengthRatio = range(branches, "lengthRatio", "$.branches.lengthRatio", 0.05D, 1.0D, issues, true);
        DoubleRange asymmetry = range(branches, "asymmetry", "$.branches.asymmetry", 0.0D, 1.0D, issues, true);
        BranchPattern pattern = enumValue(branches, "pattern", "$.branches.pattern", BranchPattern.class, issues);

        JsonObject crown = object(json, "crown", "$.crown", issues, true);
        DoubleRange widthRatio = range(crown, "widthRatio", "$.crown.widthRatio", 0.2D, 3.0D, issues, true);
        DoubleRange density = range(crown, "density", "$.crown.density", 0.0D, 1.0D, issues, true);
        IntRange clusters = intRange(crown, "clusterCount", "$.crown.clusterCount", 1, 96, issues, true);

        JsonObject generation = object(json, "generation", "$.generation", issues, true);
        String mode = stringValue(generation, "mode", "$.generation.mode", issues, "semiprocedural_once");
        if (!"semiprocedural_once".equals(mode)) {
            issues.add(ValidationIssue.error("generation_mode", "$.generation.mode",
                    "only semiprocedural_once is supported, got '" + mode + "'"));
        }
        int generatorVersion = intValue(generation, "version", "$.generation.version", issues, 1);
        int attempts = intValue(generation, "attempts", "$.generation.attempts", issues, 3);
        if (attempts < 1 || attempts > 8) {
            issues.add(ValidationIssue.error("attempts", "$.generation.attempts", "attempts must be in [1,8]"));
        }

        JsonObject budget = object(json, "budget", "$.budget", issues, true);
        int maxSegments = intValue(budget, "maxSegments", "$.budget.maxSegments", issues, 64);
        int maxLeaves = intValue(budget, "maxLeaves", "$.budget.maxLeaves", issues, 256);
        int maxQuads = intValue(budget, "maxQuads", "$.budget.maxQuads", issues, 4096);
        if (maxSegments < 8 || maxSegments > 4096) {
            issues.add(ValidationIssue.error("budget_segments", "$.budget.maxSegments",
                    "maxSegments must be in [8,4096]"));
        }
        if (maxLeaves < 8 || maxLeaves > 8192) {
            issues.add(ValidationIssue.error("budget_leaves", "$.budget.maxLeaves",
                    "maxLeaves must be in [8,8192]"));
        }
        if (maxQuads < 64 || maxQuads > 200000) {
            issues.add(ValidationIssue.error("budget_quads", "$.budget.maxQuads",
                    "maxQuads must be in [64,200000]"));
        }

        CompatibilityLevel compatibility = enumValue(json, "compatibility", "$.compatibility",
                CompatibilityLevel.class, issues);
        boolean fruitBearing = boolValue(json, "fruitBearing", "$.fruitBearing", issues, false);
        if (compatibility == CompatibilityLevel.STATIC_FRUIT && !fruitBearing) {
            issues.add(ValidationIssue.warning("fruit_mismatch", "$.fruitBearing",
                    "STATIC_FRUIT profile should declare fruitBearing=true"));
        }
        if (compatibility != CompatibilityLevel.STATIC_FRUIT && fruitBearing) {
            issues.add(ValidationIssue.warning("fruit_mismatch", "$.fruitBearing",
                    "fruitBearing=true but compatibility is " + compatibility));
        }

        List<ResourceId> biomes = biomeList(json, "biomes", "$.biomes", issues);
        String sourceFeature = "";
        JsonObject sourceJson = object(json, "source", "$.source", issues, false);
        if (sourceJson != null) {
            sourceFeature = stringValue(sourceJson, "feature", "$.source.feature", issues, "");
            if (!sourceFeature.isEmpty() && !isResourceIdOrTag(sourceFeature)) {
                issues.add(ValidationIssue.error("source_feature", "$.source.feature",
                        "expected resource id or #tag, got '" + sourceFeature + "'"));
            }
        }

        if (issues.stream().anyMatch(i -> i.severity() == ValidationIssue.Severity.ERROR)) {
            return new ValidationResult(source, null, issues);
        }

        try {
            SpeciesProfile profile = SpeciesProfile.builder(id)
                    .schema(schema)
                    .archetype(archetype)
                    .log(log)
                    .wood(wood)
                    .leaves(leaves)
                    .sapling(sapling)
                    .trunkHeight(height)
                    .trunkRadius(radius)
                    .trunkBend(bend)
                    .primaryBranches(primary)
                    .branchDepth(depth)
                    .branchAngleDegrees(angle)
                    .branchLengthRatio(lengthRatio)
                    .asymmetry(asymmetry)
                    .branchPattern(pattern)
                    .crownWidthRatio(widthRatio)
                    .crownDensity(density)
                    .crownClusters(clusters)
                    .maxSegments(maxSegments)
                    .maxLeaves(maxLeaves)
                    .maxQuads(maxQuads)
                    .compatibility(compatibility)
                    .biomes(biomes)
                    .sourceFeature(sourceFeature)
                    .fruitBearing(fruitBearing)
                    .generatorVersion(generatorVersion)
                    .build();
            return new ValidationResult(source, profile, issues);
        } catch (RuntimeException ex) {
            issues.add(ValidationIssue.error("build", "$", ex.getMessage()));
            return new ValidationResult(source, null, issues);
        }
    }

    private static JsonObject object(JsonObject parent, String key, String path,
                                     List<ValidationIssue> issues, boolean required) {
        JsonElement element = parent == null ? null : parent.get(key);
        if (element == null || element.isJsonNull()) {
            if (required) {
                issues.add(ValidationIssue.error("missing", path, "required object is missing"));
            }
            return null;
        }
        if (!element.isJsonObject()) {
            issues.add(ValidationIssue.error("type", path, "expected object"));
            return null;
        }
        return element.getAsJsonObject();
    }

    private static ResourceId resourceId(JsonObject parent, String key, String path,
                                         List<ValidationIssue> issues, boolean required) {
        String value = stringValue(parent, key, path, issues, required ? null : "");
        if (value == null) {
            return null;
        }
        if (value.isEmpty()) {
            if (required) {
                issues.add(ValidationIssue.error("missing", path, "required resource id is missing"));
            }
            return null;
        }
        ResourceId id = ResourceId.tryParse(value);
        if (id == null) {
            issues.add(ValidationIssue.error("resource_id", path, "invalid resource id '" + value + "'"));
        }
        return id;
    }

    private static String stringValue(JsonObject parent, String key, String path,
                                      List<ValidationIssue> issues, String fallback) {
        JsonElement element = parent == null ? null : parent.get(key);
        if (element == null || element.isJsonNull()) {
            if (fallback == null) {
                issues.add(ValidationIssue.error("missing", path, "required string is missing"));
            }
            return fallback;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            issues.add(ValidationIssue.error("type", path, "expected string"));
            return fallback;
        }
        return element.getAsString();
    }

    private static int intValue(JsonObject parent, String key, String path,
                                List<ValidationIssue> issues, int fallback) {
        JsonElement element = parent == null ? null : parent.get(key);
        if (element == null || element.isJsonNull()) {
            if (fallback < 0) {
                issues.add(ValidationIssue.error("missing", path, "required integer is missing"));
            }
            return fallback;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
            issues.add(ValidationIssue.error("type", path, "expected integer"));
            return fallback;
        }
        double value = element.getAsDouble();
        if (value != Math.rint(value)) {
            issues.add(ValidationIssue.error("type", path, "expected integer"));
            return fallback;
        }
        return (int) value;
    }

    private static boolean boolValue(JsonObject parent, String key, String path,
                                     List<ValidationIssue> issues, boolean fallback) {
        JsonElement element = parent == null ? null : parent.get(key);
        if (element == null || element.isJsonNull()) {
            return fallback;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
            issues.add(ValidationIssue.error("type", path, "expected boolean"));
            return fallback;
        }
        return element.getAsBoolean();
    }

    private static <E extends Enum<E>> E enumValue(JsonObject parent, String key, String path,
                                                   Class<E> type, List<ValidationIssue> issues) {
        JsonElement element = parent == null ? null : parent.get(key);
        if (element == null || element.isJsonNull()) {
            issues.add(ValidationIssue.error("missing", path, "required value is missing"));
            return null;
        }
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            issues.add(ValidationIssue.error("type", path, "expected enum string"));
            return null;
        }
        try {
            return Enum.valueOf(type, element.getAsString().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            issues.add(ValidationIssue.error("enum", path, "unknown value '" + element.getAsString()
                    + "' for " + type.getSimpleName()));
            return null;
        }
    }

    private static DoubleRange range(JsonObject parent, String key, String path,
                                     double min, double max, List<ValidationIssue> issues, boolean required) {
        JsonElement element = parent == null ? null : parent.get(key);
        if (element == null || element.isJsonNull()) {
            if (required) {
                issues.add(ValidationIssue.error("missing", path, "required range is missing"));
            }
            return new DoubleRange(min, max);
        }
        if (!element.isJsonArray()) {
            issues.add(ValidationIssue.error("type", path, "expected [min,max] array"));
            return new DoubleRange(min, max);
        }
        JsonArray array = element.getAsJsonArray();
        if (array.size() != 2) {
            issues.add(ValidationIssue.error("range_size", path, "expected exactly two numbers"));
            return new DoubleRange(min, max);
        }
        try {
            double lo = array.get(0).getAsDouble();
            double hi = array.get(1).getAsDouble();
            if (!Double.isFinite(lo) || !Double.isFinite(hi)) {
                issues.add(ValidationIssue.error("range_value", path, "range values must be finite numbers"));
                return new DoubleRange(min, max);
            }
            if (lo > hi) {
                issues.add(ValidationIssue.error("range_bounds", path,
                        "inverted range [" + lo + "," + hi + "]: min is greater than max"));
                return new DoubleRange(min, max);
            }
            if (lo < min || hi > max) {
                issues.add(ValidationIssue.error("range_bounds", path,
                        "range [" + lo + "," + hi + "] outside allowed [" + min + "," + max + "]"));
            }
            return new DoubleRange(lo, hi);
        } catch (RuntimeException ex) {
            issues.add(ValidationIssue.error("range_value", path, "range values must be numbers"));
            return new DoubleRange(min, max);
        }
    }

    private static IntRange intRange(JsonObject parent, String key, String path,
                                     int min, int max, List<ValidationIssue> issues, boolean required) {
        DoubleRange range = range(parent, key, path, min, max, issues, required);
        int lo = (int) Math.round(range.min());
        int hi = (int) Math.round(range.max());
        return new IntRange(Math.max(min, Math.min(max, lo)), Math.max(min, Math.min(max, hi)));
    }

    private static List<ResourceId> biomeList(JsonObject parent, String key, String path,
                                              List<ValidationIssue> issues) {
        JsonElement element = parent == null ? null : parent.get(key);
        if (element == null || element.isJsonNull()) {
            return List.of();
        }
        if (!element.isJsonArray()) {
            issues.add(ValidationIssue.error("type", path, "expected array of biome ids/tags"));
            return List.of();
        }
        List<ResourceId> result = new ArrayList<>();
        Set<ResourceId> seen = new HashSet<>();
        for (JsonElement entry : element.getAsJsonArray()) {
            if (!entry.isJsonPrimitive() || !entry.getAsJsonPrimitive().isString()) {
                issues.add(ValidationIssue.error("biome_value", path, "biome entries must be strings"));
                continue;
            }
            String value = entry.getAsString();
            if (value.startsWith("#")) {
                value = value.substring(1);
            }
            ResourceId id = ResourceId.tryParse(value);
            if (id == null) {
                issues.add(ValidationIssue.error("biome_value", path, "invalid biome id '" + entry.getAsString() + "'"));
            } else if (!seen.add(id)) {
                issues.add(ValidationIssue.warning("duplicate_biome", path, "duplicate biome " + id));
            } else {
                result.add(id);
            }
        }
        return result;
    }

    private static boolean isResourceIdOrTag(String value) {
        String candidate = value.startsWith("#") ? value.substring(1) : value;
        return ResourceId.tryParse(candidate) != null;
    }

    private static boolean isNumber(JsonElement element) {
        return element != null && element.isJsonPrimitive() && ((JsonPrimitive) element).isNumber();
    }
}
