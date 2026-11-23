package dev.branched.resources;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.branched.compat.AdapterRegistry;
import dev.branched.compat.DefaultAdapters;
import dev.branched.compat.ModAdapter;
import dev.branched.compat.FeatureMapping;
import dev.branched.core.ResourceId;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates the generated Phase 3 mirror without bootstrapping Minecraft.
 *
 * <p>The generated files are a closed graph, the biome tags resolve to files,
 * every replaced id is audited as replaceable by the adapter catalog, and no
 * modifier can remove a feature it did not declare. The JARs are not needed at
 * test time: the committed catalogs are the audit record.</p>
 */
class Phase3ResourceTest {
    private static final Path RESOURCES = Path.of("src/main/resources");
    private static final Path DATA = RESOURCES.resolve("data/branched");
    private static final Path CONFIGURED = DATA.resolve("worldgen/configured_feature");
    private static final Path PLACED = DATA.resolve("worldgen/placed_feature");
    private static final Path MODIFIERS = DATA.resolve("forge/biome_modifier");
    private static final Path TAGS = DATA.resolve("tags/worldgen/biome");
    private static final Path PLACED_TAGS = DATA.resolve("tags/worldgen/placed_feature");
    private static final Path BLOCK_TAGS = DATA.resolve("tags/blocks");
    private static final Path FLUID_TAGS = DATA.resolve("tags/fluids");
    private static final Path SPECIES = DATA.resolve("arboreal/species");
    private static final Path CATALOGS = Path.of("catalogs");

    private static final int REPLACED = 70;
    private static final int PRESERVED = 200;
    private static final Set<String> MODS = Set.of("regions_unexplored", "terralith");

    private final AdapterRegistry registry = new AdapterRegistry();

    Phase3ResourceTest() {
        registry.replaceAll(DefaultAdapters.create());
    }

    @Test
    void everyModdedModifierIsATagScopedSingleReplacement() throws IOException {
        List<Path> modifiers = moddedModifiers();
        assertEquals(REPLACED, modifiers.size());

        Set<String> replaced = new HashSet<>();
        Set<String> added = new HashSet<>();
        for (Path path : modifiers) {
            JsonObject modifier = read(path);
            assertEquals("branched:replace_trees", modifier.get("type").getAsString());
            assertEquals("vegetal_decoration", modifier.get("step").getAsString());

            JsonElement biomes = modifier.get("biomes");
            assertTrue(biomes.isJsonPrimitive() && biomes.getAsJsonPrimitive().isString(),
                    path + " must target one generated tag as a bare string");
            String tagReference = biomes.getAsString();
            assertTrue(tagReference.startsWith("#branched:phase3/"),
                    path + " biome tag " + tagReference);

            String add = modifier.get("add").getAsString();
            assertTrue(add.startsWith("branched:phase3/"), path + " add " + add);
            assertTrue(Files.isRegularFile(placedFile(add)), path + " missing " + add);
            assertTrue(added.add(add), "duplicate static placed feature " + add);

            JsonElement replace = modifier.get("replace");
            assertTrue(replace.isJsonPrimitive() && replace.getAsJsonPrimitive().isString(),
                    path + " must remove through a tag, never an inline holder list");
            assertEquals("#" + add, replace.getAsString(),
                    path + " must reuse its generated tag for the removed id");
            List<String> replacements = replacedIds(modifier);
            assertEquals(1, replacements.size(), path + " removes exactly one audited feature");
            String original = replacements.get(0);
            assertTrue(replaced.add(original), "feature replaced twice: " + original);

            String mod = modifierNamespace(original, path);
            assertTrue(add.startsWith("branched:phase3/" + mod + "/"),
                    path + " add does not belong to " + mod);

            JsonArray species = speciesArray(modifier);
            assertFalse(species.isEmpty(), path + " needs a species gate");
            for (JsonElement element : species) {
                assertTrue(Files.isRegularFile(speciesFile(element.getAsString())),
                        path + " references missing profile " + element.getAsString());
            }

            Path tagFile = tagFile(tagReference);
            assertTrue(Files.isRegularFile(tagFile), path + " missing tag file " + tagFile);
            JsonArray values = read(tagFile).getAsJsonArray("values");
            assertFalse(values.isEmpty(), tagFile + " must not be empty");
            for (JsonElement value : values) {
                assertTrue(value.getAsString().startsWith(mod + ":"),
                        tagFile + " links foreign biome " + value.getAsString());
            }
        }
    }

    @Test
    void everyReplacedFeatureIsAuditedAsReplaceable() throws IOException {
        for (Path path : moddedModifiers()) {
            JsonObject modifier = read(path);
            for (String element : replacedIds(modifier)) {
                ResourceId id = ResourceId.parse(element);
                assertTrue(registry.isReplaceable(id),
                        path + " replaces un-audited feature " + id);
                ModAdapter adapter = registry.all().stream()
                        .filter(candidate -> candidate.modId().equals(id.namespace()))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError("no adapter for " + id));
                List<FeatureMapping> mappings = adapter.features().stream()
                        .filter(mapping -> mapping.feature().equals(id))
                        .toList();
                assertEquals(1, mappings.size(), "duplicate catalog entry for " + id);
                assertFalse(mappings.get(0).notes().isBlank(), id + " needs a note");
            }
        }
    }

    @Test
    void preservedCatalogIsDocumentedAndDisjointFromReplacements() throws IOException {
        List<Map<String, String>> preserved = csv(CATALOGS.resolve("phase3-preserved.csv"));
        assertEquals(PRESERVED, preserved.size());
        Set<String> preservedIds = new HashSet<>();
        for (Map<String, String> row : preserved) {
            assertFalse(row.get("reasons").isBlank(), row.get("placed_feature") + " needs a reason");
            assertTrue(preservedIds.add(row.get("placed_feature")),
                    "duplicate preserved row " + row.get("placed_feature"));
        }

        List<Map<String, String>> replaced = csv(CATALOGS.resolve("phase3-replacements.csv"));
        assertEquals(REPLACED, replaced.size());
        Set<String> replacedIds = new HashSet<>();
        for (Map<String, String> row : replaced) {
            assertTrue(replacedIds.add(row.get("placed_feature")),
                    "duplicate replacement row " + row.get("placed_feature"));
            assertTrue(row.get("tree_types").contains("tree")
                            || row.get("tree_types").contains("willow"),
                    row.get("placed_feature") + " is not a tree feature");
            assertFalse(row.get("species").isBlank(), row.get("placed_feature") + " needs a species");
            assertFalse(row.get("biomes").isBlank(), row.get("placed_feature") + " needs biomes");
        }
        for (String id : replacedIds) {
            assertFalse(preservedIds.contains(id), id + " is both replaced and preserved");
        }
    }

    @Test
    void generatedGraphIsClosedAndOnlyUsesVanillaPlacementModifiers() throws IOException {
        Set<String> reachablePlaced = new LinkedHashSet<>();
        Set<String> reachableConfigured = new LinkedHashSet<>();
        for (Path path : moddedModifiers()) {
            String add = read(path).get("add").getAsString();
            visitPlaced(add, reachablePlaced, reachableConfigured);
        }
        assertEquals(phase3Ids(PLACED), reachablePlaced,
                "unreferenced phase 3 placed features");
        assertEquals(phase3Ids(CONFIGURED), reachableConfigured,
                "unreferenced phase 3 configured features");

        for (String id : reachablePlaced) {
            JsonObject placed = read(placedFile(id));
            String feature = placed.get("feature").getAsString();
            assertTrue(feature.startsWith("branched:phase3/"),
                    id + " points outside the generated graph: " + feature);
            for (JsonElement step : placed.getAsJsonArray("placement")) {
                String type = step.getAsJsonObject().get("type").getAsString();
                assertTrue(type.startsWith("minecraft:"),
                        id + " uses a non-vanilla placement modifier " + type);
            }
        }
    }

    @Test
    void everyGeneratedPlacementLoadsWithoutTheBiomeMod() throws IOException {
        // A generated placed feature is parsed by every client and server, even
        // when the biome mod is absent. Anything it names outside minecraft and
        // branched is resolved at load time and would abort datapack
        // loading; foreign block ids are therefore moved into own tags.
        for (String id : phase3Ids(PLACED)) {
            Set<String> ids = new LinkedHashSet<>();
            Set<String> tagReferences = new LinkedHashSet<>();
            collectReferences(read(placedFile(id)), ids, tagReferences);
            for (String reference : ids) {
                String namespace = reference.substring(0, reference.indexOf(':'));
                assertTrue(namespace.equals("minecraft") || namespace.equals("branched"),
                        id + " resolves " + reference + " while loading");
            }
            for (String tag : tagReferences) {
                assertTrue(tag.startsWith("branched:phase3/"),
                        id + " references a tag it does not own: " + tag);
                String path = tag.substring(tag.indexOf(':') + 1) + ".json";
                Path blockTag = BLOCK_TAGS.resolve(path);
                Path fluidTag = FLUID_TAGS.resolve(path);
                assertTrue(Files.isRegularFile(blockTag) || Files.isRegularFile(fluidTag),
                        id + " references missing tag " + tag);
                Path file = Files.isRegularFile(blockTag) ? blockTag : fluidTag;
                assertFalse(read(file).getAsJsonArray("values").isEmpty(),
                        file + " must not be empty");
            }
        }
    }

    @Test
    void everyStaticTreeUsesAShippedReplaceableProfile() throws IOException {
        int trees = 0;
        for (String id : phase3Ids(CONFIGURED)) {
            JsonObject configured = read(configuredFile(id));
            if (!"branched:static_tree".equals(configured.get("type").getAsString())) {
                continue;
            }
            trees++;
            JsonObject config = configured.getAsJsonObject("config");
            String species = config.get("species").getAsString();
            assertTrue(Files.isRegularFile(speciesFile(species)),
                    id + " references missing profile " + species);
            assertEquals(3, config.get("attempts").getAsInt(), id + " attempts");
        }
        // Several placed features share one configured tree (for example the
        // six Scott's pine variants), so this is the size of the configured
        // graph, not the number of replacements.
        assertEquals(55, trees, "configured static tree nodes");
    }

    @Test
    void noModdedPlacedFeatureIsAlsoReplacedByAVanillaModifier() throws IOException {
        Set<String> vanillaReplacements = new HashSet<>();
        for (Path path : list(MODIFIERS, "vanilla_*.json")) {
            for (JsonElement element : read(path).getAsJsonArray("replace")) {
                vanillaReplacements.add(element.getAsString());
            }
        }
        for (Path path : moddedModifiers()) {
            for (String id : replacedIds(read(path))) {
                assertFalse(vanillaReplacements.contains(id),
                        "feature replaced by both Phase 2 and Phase 3: " + id);
            }
        }
    }

    /** Reads the one-element placed-feature tag a modifier uses for removal. */
    private static List<String> replacedIds(JsonObject modifier) throws IOException {
        JsonElement replace = modifier.get("replace");
        assertTrue(replace.isJsonPrimitive() && replace.getAsJsonPrimitive().isString(),
                "replace must be a tag reference: " + replace);
        String reference = replace.getAsString();
        assertTrue(reference.startsWith("#branched:phase3/"),
                "replace must use a generated tag: " + reference);
        String path = reference.substring(1).split(":", 2)[1] + ".json";
        Path file = PLACED_TAGS.resolve(path);
        assertTrue(Files.isRegularFile(file), "missing placed-feature tag " + file);
        List<String> ids = new ArrayList<>();
        for (JsonElement value : read(file).getAsJsonArray("values")) {
            ids.add(value.getAsString());
        }
        return ids;
    }

    private static void visitPlaced(String id, Set<String> placed, Set<String> configured)
            throws IOException {
        if (!placed.add(id)) {
            return;
        }
        JsonObject placedJson = read(placedFile(id));
        String feature = placedJson.get("feature").getAsString();
        if (!configured.add(feature)) {
            return;
        }
        JsonObject configuredJson = read(configuredFile(feature));
        String type = configuredJson.get("type").getAsString();
        JsonObject config = configuredJson.getAsJsonObject("config");
        switch (type) {
            case "minecraft:random_selector" -> {
                visitPlaced(config.get("default").getAsString(), placed, configured);
                for (JsonElement entry : config.getAsJsonArray("features")) {
                    visitPlaced(entry.getAsJsonObject().get("feature").getAsString(),
                            placed, configured);
                }
            }
            case "minecraft:simple_random_selector" -> {
                for (JsonElement entry : config.getAsJsonArray("features")) {
                    visitPlaced(entry.getAsString(), placed, configured);
                }
            }
            case "minecraft:random_boolean_selector" -> {
                visitPlaced(config.get("feature_true").getAsString(), placed, configured);
                visitPlaced(config.get("feature_false").getAsString(), placed, configured);
            }
            default -> {
                // static_tree
            }
        }
    }

    private static List<Path> moddedModifiers() throws IOException {
        return list(MODIFIERS, "modded_*.json");
    }

    private static String modifierNamespace(String original, Path path) {
        String namespace = original.substring(0, original.indexOf(':'));
        assertTrue(MODS.contains(namespace), path + " replaces non-Phase-3 mod " + original);
        assertTrue(path.getFileName().toString().startsWith("modded_" + namespace + "_"),
                path + " filename does not match " + namespace);
        return namespace;
    }

    private static void collectReferences(JsonElement element, Set<String> ids, Set<String> tags) {
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            String value = element.getAsString();
            if (value.startsWith("#")) {
                tags.add(value.substring(1));
            } else if (value.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
                ids.add(value);
            }
            return;
        }
        if (element.isJsonArray()) {
            for (JsonElement child : element.getAsJsonArray()) {
                collectReferences(child, ids, tags);
            }
            return;
        }
        if (element.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
                collectReferences(entry.getValue(), ids, tags);
            }
        }
    }

    private static Set<String> phase3Ids(Path root) throws IOException {
        Set<String> ids = new LinkedHashSet<>();
        Path phase3 = root.resolve("phase3");
        try (Stream<Path> stream = Files.walk(phase3)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .forEach(path -> {
                        String relative = phase3.relativize(path).toString().replace('\\', '/');
                        ids.add("branched:phase3/" + relative.substring(0, relative.length() - 5));
                    });
        }
        return ids;
    }

    private static Path tagFile(String reference) {
        String id = reference.substring(1); // drop '#'
        String path = id.substring(id.indexOf(':') + 1);
        return TAGS.resolve(path + ".json");
    }

    private static Path configuredFile(String id) {
        return CONFIGURED.resolve(id.substring(id.indexOf(':') + 1) + ".json");
    }

    private static Path placedFile(String id) {
        return PLACED.resolve(id.substring(id.indexOf(':') + 1) + ".json");
    }

    private static Path speciesFile(String id) {
        String namespace = id.substring(0, id.indexOf(':'));
        String path = id.substring(id.indexOf(':') + 1);
        return SPECIES.resolve(namespace).resolve(path + ".json");
    }

    private static JsonArray speciesArray(JsonObject modifier) {
        JsonElement species = modifier.get("species");
        return species.isJsonArray() ? species.getAsJsonArray() : new JsonArray();
    }

    private static List<Path> list(Path directory, String glob) throws IOException {
        List<Path> result = new ArrayList<>();
        try (Stream<Path> stream = Files.list(directory)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().matches(
                            glob.replace(".", "\\.").replace("*", ".*")))
                    .sorted()
                    .forEach(result::add);
        }
        return result;
    }

    private static List<Map<String, String>> csv(Path path) throws IOException {
        String[] lines = Files.readString(path).split("\\R");
        List<String> header = csvRow(lines[0]);
        List<Map<String, String>> rows = new ArrayList<>();
        for (int index = 1; index < lines.length; index++) {
            if (lines[index].isBlank()) {
                continue;
            }
            List<String> values = csvRow(lines[index]);
            assertEquals(header.size(), values.size(), "malformed csv row: " + lines[index]);
            Map<String, String> row = new LinkedHashMap<>();
            for (int column = 0; column < header.size(); column++) {
                row.put(header.get(column), values.get(column));
            }
            rows.add(row);
        }
        return rows;
    }

    /** Minimal RFC 4180 row splitter: the reason column may contain commas. */
    private static List<String> csvRow(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int index = 0; index < line.length(); index++) {
            char character = line.charAt(index);
            if (character == '"') {
                if (quoted && index + 1 < line.length() && line.charAt(index + 1) == '"') {
                    current.append('"');
                    index++;
                } else {
                    quoted = !quoted;
                }
            } else if (character == ',' && !quoted) {
                values.add(current.toString());
                current.setLength(0);
            } else {
                current.append(character);
            }
        }
        values.add(current.toString());
        return values;
    }

    private static JsonObject read(Path path) throws IOException {
        assertTrue(Files.isRegularFile(path), "missing resource " + path);
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
