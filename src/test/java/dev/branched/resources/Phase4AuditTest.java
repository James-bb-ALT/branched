package dev.branched.resources;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates the Phase 4 decision without bootstrapping Minecraft.
 *
 * <p>Phase 4 is a preservation phase: the audit records 37 tree-shaped placed
 * features from four mods and replaces none of them. The test locks that
 * decision, the reason vocabulary and the absence of a half-generated
 * {@code branched:phase4/*} graph. The reference JARs are not needed: the
 * committed catalogs are the audit record.</p>
 */
class Phase4AuditTest {
    private static final Path DATA = Path.of("src/main/resources/data/branched");
    private static final Path DOCS = Path.of("docs");
    private static final Path CATALOGS = Path.of("catalogs");

    private static final Set<String> MODS =
            Set.of("aether", "fishofthieves", "quark", "twilightforest");
    private static final Set<String> UNAVAILABLE =
            Set.of("dynamictrees_baobab", "dynamictrees_addon_lib", "dynamictrees_plus");
    private static final Set<String> REASON_PREFIXES = Set.of(
            "decorators",
            "modded_placement",
            "platform_unsupported",
            "foreign_biome",
            "biome_tag",
            "foreign_reference",
            "unlinked_feature",
            "special_feature_type",
            "custom_canopy_feature",
            "missing_profile",
            "profile_not_replaceable",
            "hollow_tree_feature",
            "fallen_log_feature",
            "landmark_scale_feature",
            "fungus_feature",
            "cactus_feature",
            "bamboo_feature",
            "joshua_shrub_feature",
            "shrub_silhouette",
            "force_dirt",
            "unresolved_trunk",
            "unresolved_foliage",
            "non_vegetal_step",
            "unsupported_type");
    private static final int TREE_FEATURES = 37;

    @Test
    void auditMatchesTheCommittedCatalogs() throws IOException {
        JsonObject audit = readJson(DOCS.resolve("phase4-audit.json"));
        assertEquals(4, audit.get("phase").getAsInt());
        assertEquals(MODS, audit.getAsJsonObject("mods").keySet());
        assertEquals(UNAVAILABLE, audit.getAsJsonObject("unavailable_mods").keySet());

        JsonArray preserved = readCsv(CATALOGS.resolve("phase4-preserved.csv"));
        JsonArray replacements = readCsv(CATALOGS.resolve("phase4-replacements.csv"));
        assertEquals(0, replacements.size(), "Phase 4 must not ship a replacement");

        JsonObject totals = audit.getAsJsonObject("totals");
        assertEquals(TREE_FEATURES, totals.get("tree_placed_features").getAsInt());
        assertEquals(0, totals.get("replaced").getAsInt());
        assertEquals(TREE_FEATURES, totals.get("preserved").getAsInt());
        assertEquals(TREE_FEATURES, preserved.size(), "one preserved row per tree feature");

        Map<String, Integer> perMod = new LinkedHashMap<>();
        for (JsonElement element : preserved) {
            JsonObject row = element.getAsJsonObject();
            String mod = row.get("mod").getAsString();
            String placed = row.get("placed_feature").getAsString();
            assertTrue(MODS.contains(mod), "audited mod " + mod);
            assertTrue(placed.startsWith(mod + ":"), placed + " is not in " + mod);
            assertFalse(row.get("reasons").getAsString().isBlank(), placed + " has no reason");
            perMod.merge(mod, 1, Integer::sum);
        }

        JsonObject mods = audit.getAsJsonObject("mods");
        for (String mod : MODS) {
            JsonObject block = mods.getAsJsonObject(mod);
            assertEquals(0, block.get("replaced").getAsInt(), mod + " must replace nothing");
            assertEquals(perMod.getOrDefault(mod, 0), block.get("preserved").getAsInt(),
                    mod + " preserved count");
            assertEquals(block.get("preserved").getAsInt(), block.get("tree_placed_features").getAsInt(),
                    mod + " tree count must equal its preserved count");
        }
    }

    @Test
    void everyPreservedReasonIsDocumented() throws IOException {
        JsonArray preserved = readCsv(CATALOGS.resolve("phase4-preserved.csv"));
        for (JsonElement element : preserved) {
            JsonObject row = element.getAsJsonObject();
            String placed = row.get("placed_feature").getAsString();
            for (String reason : row.get("reasons").getAsString().split(";")) {
                assertFalse(reason.isBlank(), placed + " has an empty reason");
                String prefix = reason.split(":", 2)[0];
                assertTrue(REASON_PREFIXES.contains(prefix),
                        placed + " uses undocumented reason " + reason);
            }
        }
    }

    @Test
    void treeAuditCoversEveryTreeFeature() throws IOException {
        JsonArray rows = readCsv(DOCS.resolve("phase4-tree-audit.csv"));
        assertEquals(TREE_FEATURES, rows.size(), "one row per tree-shaped placed feature");
        for (JsonElement element : rows) {
            JsonObject row = element.getAsJsonObject();
            String placed = row.get("placed_feature").getAsString();
            assertTrue(row.get("tree_nodes").getAsInt() > 0, placed + " has no tree node");
            assertEquals("preserved", row.get("verdict").getAsString(), placed + " verdict");
            assertFalse(row.get("node_types").getAsString().isBlank(), placed + " node types");
        }
    }

    @Test
    void noPhase4WorldgenWasGenerated() throws IOException {
        try (Stream<Path> files = Files.walk(DATA)) {
            List<Path> generated = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.toString().replace('\\', '/').contains("/phase4/"))
                    .toList();
            assertTrue(generated.isEmpty(), "unexpected Phase 4 resources: " + generated);
        }
        try (Stream<Path> files = Files.walk(DATA.resolve("forge/biome_modifier"))) {
            List<Path> modifiers = files
                    .filter(path -> path.toString().endsWith(".json"))
                    .filter(path -> {
                        try {
                            JsonObject json = readJson(path);
                            JsonElement add = json.get("add");
                            return add != null && add.getAsString().startsWith("branched:phase4/");
                        } catch (IOException exception) {
                            return false;
                        }
                    })
                    .toList();
            assertTrue(modifiers.isEmpty(), "unexpected Phase 4 modifiers: " + modifiers);
        }
    }

    // --- tiny CSV/JSON helpers (Gson only; no extra dependency) -------------

    private static JsonObject readJson(Path path) throws IOException {
        assertTrue(Files.isRegularFile(path), "missing " + path);
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    private static JsonArray readCsv(Path path) throws IOException {
        assertTrue(Files.isRegularFile(path), "missing " + path);
        List<String> lines = Files.readAllLines(path);
        assertFalse(lines.isEmpty(), path + " is empty");
        List<String> header = parseCsvLine(lines.get(0));
        JsonArray rows = new JsonArray();
        for (int index = 1; index < lines.size(); index++) {
            if (lines.get(index).isBlank()) {
                continue;
            }
            List<String> cells = parseCsvLine(lines.get(index));
            assertEquals(header.size(), cells.size(), path + " line " + (index + 1) + " width");
            JsonObject row = new JsonObject();
            for (int column = 0; column < header.size(); column++) {
                row.addProperty(header.get(column), cells.get(column));
            }
            rows.add(row);
        }
        return rows;
    }

    private static List<String> parseCsvLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char ch = line.charAt(index);
            if (quoted) {
                if (ch == '"') {
                    if (index + 1 < line.length() && line.charAt(index + 1) == '"') {
                        current.append('"');
                        index++;
                    } else {
                        quoted = false;
                    }
                } else {
                    current.append(ch);
                }
                continue;
            }
            if (ch == '"') {
                quoted = true;
            } else if (ch == ',') {
                cells.add(current.toString());
                current.setLength(0);
            } else {
                current.append(ch);
            }
        }
        cells.add(current.toString());
        return cells;
    }
}
