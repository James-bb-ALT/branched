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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates the generated Phase 2 vanilla mirror without bootstrapping Minecraft.
 *
 * <p>The generated files are a closed graph: every biome modifier points at a
 * placed feature, every placed feature at a configured feature, every selector
 * at placed features, and every static tree at a shipped species profile. The
 * test also proves that no Phase 1 pilot file is left behind.</p>
 */
class VanillaPhase2ResourceTest {
    private static final Path RESOURCES = Path.of("src/main/resources");
    private static final Path DATA = RESOURCES.resolve("data/branched");
    private static final Path CONFIGURED = DATA.resolve("worldgen/configured_feature");
    private static final Path PLACED = DATA.resolve("worldgen/placed_feature");
    private static final Path MODIFIERS = DATA.resolve("forge/biome_modifier");
    private static final Path SPECIES = DATA.resolve("arboreal/species");

    private static final List<String> AGGREGATES = List.of(
            "trees_badlands",
            "trees_birch",
            "trees_birch_and_oak",
            "trees_cherry",
            "trees_flower_forest",
            "trees_grove",
            "trees_jungle",
            "trees_meadow",
            "trees_old_growth_pine_taiga",
            "trees_old_growth_spruce_taiga",
            "trees_savanna",
            "trees_snowy",
            "trees_sparse_jungle",
            "trees_swamp",
            "trees_taiga",
            "trees_windswept_forest",
            "trees_windswept_hills",
            "trees_windswept_savanna");

    @Test
    void everyModifierHasBiomesSpeciesAndResolvableFeatures() throws IOException {
        List<Path> modifiers = files(MODIFIERS, "vanilla_*.json");
        assertEquals(AGGREGATES.size(), modifiers.size());

        for (Path path : modifiers) {
            JsonObject modifier = read(path);
            assertEquals("branched:replace_trees", modifier.get("type").getAsString());
            assertEquals("vegetal_decoration", modifier.get("step").getAsString());

            JsonArray biomes = modifier.getAsJsonArray("biomes");
            assertNotNull(biomes, path + " biomes");
            assertFalse(biomes.isEmpty(), path + " needs at least one biome");
            for (JsonElement biome : biomes) {
                assertTrue(biome.getAsString().startsWith("minecraft:"),
                        path + " biome must be a vanilla id: " + biome);
            }

            String add = modifier.get("add").getAsString();
            assertTrue(Files.isRegularFile(placedFile(add)),
                    path + " adds missing placed feature " + add);

            JsonArray replace = modifier.getAsJsonArray("replace");
            assertEquals(1, replace.size(), path + " replaces one aggregate");
            String replaced = replace.get(0).getAsString();
            assertEquals("minecraft:" + path.getFileName().toString()
                            .replace("vanilla_", "").replace(".json", ""),
                    replaced, path + " replaces its own aggregate");
            assertTrue(AGGREGATES.contains(replaced.substring("minecraft:".length())));

            JsonArray species = speciesArray(modifier);
            assertFalse(species.isEmpty(), path + " needs a species gate");
            for (JsonElement element : species) {
                assertTrue(Files.isRegularFile(speciesFile(element.getAsString())),
                        path + " references missing profile " + element.getAsString());
            }
        }
    }

    @Test
    void everyStaticTreeReferencesAShippedProfile() throws IOException {
        for (Path path : files(CONFIGURED, "static_*.json")) {
            JsonObject configured = read(path);
            if (!"branched:static_tree".equals(configured.get("type").getAsString())) {
                continue;
            }
            JsonObject config = configured.getAsJsonObject("config");
            String species = config.get("species").getAsString();
            assertTrue(Files.isRegularFile(speciesFile(species)),
                    path + " references missing profile " + species);
            assertEquals(3, config.get("attempts").getAsInt(), path + " attempts");
        }
    }

    @Test
    void everySelectorReferencesExistingStaticPlacedFeatures() throws IOException {
        for (Path path : files(CONFIGURED, "static_*.json")) {
            JsonObject configured = read(path);
            String type = configured.get("type").getAsString();
            JsonObject config = configured.getAsJsonObject("config");
            switch (type) {
                case "minecraft:random_selector" -> {
                    assertPlacedExists(config.get("default").getAsString(), path);
                    for (JsonElement entry : config.getAsJsonArray("features")) {
                        JsonObject feature = entry.getAsJsonObject();
                        double chance = feature.get("chance").getAsDouble();
                        assertTrue(chance > 0.0D && chance <= 1.0D,
                                path + " chance out of range: " + chance);
                        assertPlacedExists(feature.get("feature").getAsString(), path);
                    }
                }
                case "minecraft:simple_random_selector" -> {
                    for (JsonElement entry : config.getAsJsonArray("features")) {
                        assertPlacedExists(entry.getAsString(), path);
                    }
                }
                case "minecraft:random_boolean_selector" -> {
                    assertPlacedExists(config.get("feature_true").getAsString(), path);
                    assertPlacedExists(config.get("feature_false").getAsString(), path);
                }
                default -> {
                    // static_tree is covered by its own test.
                }
            }
        }
    }

    @Test
    void everyPlacedFeatureReferencesAnExistingConfiguredFeature() throws IOException {
        for (Path path : files(PLACED, "static_*.json")) {
            JsonObject placed = read(path);
            String feature = placed.get("feature").getAsString();
            assertTrue(feature.startsWith("branched:"),
                    path + " must point at a static configured feature");
            assertTrue(Files.isRegularFile(configuredFile(feature)),
                    path + " references missing configured feature " + feature);
            assertNotNull(placed.getAsJsonArray("placement"), path + " placement");
        }
    }

    @Test
    void generatedFilesFormAClosedGraphWithoutPilotLeftovers() throws IOException {
        Set<String> reachableConfigured = new LinkedHashSet<>();
        Set<String> reachablePlaced = new LinkedHashSet<>();

        for (Path path : files(MODIFIERS, "vanilla_*.json")) {
            JsonObject modifier = read(path);
            visitPlaced(modifier.get("add").getAsString(), reachablePlaced, reachableConfigured);
        }

        assertEquals(staticIds(CONFIGURED), reachableConfigured,
                "unreferenced configured features (stale Phase 1 output?)");
        assertEquals(staticIds(PLACED), reachablePlaced,
                "unreferenced placed features (stale Phase 1 output?)");
        assertFalse(Files.exists(MODIFIERS.resolve("static_spruce_taiga.json")),
                "Phase 1 pilot modifier must be removed");
        assertFalse(Files.exists(PLACED.resolve("static_spruce_taiga.json")),
                "Phase 1 pilot placed feature must be removed");
    }

    @Test
    void checkedPlacementsUseTheNativeSaplingOfTheirProfile() throws IOException {
        assertEquals("minecraft:oak_sapling", saplingOf("static_oak_checked"));
        assertEquals("minecraft:acacia_sapling", saplingOf("static_acacia_checked"));
        assertEquals("minecraft:spruce_sapling", saplingOf("static_spruce_checked"));
        assertEquals("minecraft:spruce_sapling", saplingOf("static_pine_checked"));
        assertEquals("minecraft:oak_sapling", saplingOf("static_fancy_oak_checked"));
        assertEquals("minecraft:birch_sapling", saplingOf("static_birch_checked"));
        assertEquals("minecraft:cherry_sapling", saplingOf("static_cherry_checked"));
        assertEquals("minecraft:jungle_sapling", saplingOf("static_jungle_checked"));
    }

    private static String saplingOf(String placed) throws IOException {
        JsonObject json = read(PLACED.resolve(placed + ".json"));
        JsonArray placement = json.getAsJsonArray("placement");
        assertEquals(1, placement.size(), placed + " has one placement step");
        JsonObject step = placement.get(0).getAsJsonObject();
        assertEquals("minecraft:block_predicate_filter", step.get("type").getAsString());
        JsonObject predicate = step.getAsJsonObject("predicate");
        assertEquals("minecraft:would_survive", predicate.get("type").getAsString());
        return predicate.getAsJsonObject("state").get("Name").getAsString();
    }

    private static void visitPlaced(String id, Set<String> placed, Set<String> configured)
            throws IOException {
        if (!placed.add(id)) {
            return;
        }
        JsonObject placedJson = read(placedFile(id));
        String feature = placedJson.get("feature").getAsString();
        if (configured.add(feature)) {
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
                }
            }
        }
    }

    private static void assertPlacedExists(String id, Path source) {
        assertTrue(Files.isRegularFile(placedFile(id)),
                source + " references missing placed feature " + id);
    }

    private static Set<String> staticIds(Path directory) throws IOException {
        Set<String> ids = new LinkedHashSet<>();
        for (Path path : files(directory, "static_*.json")) {
            ids.add("branched:" + path.getFileName().toString().replace(".json", ""));
        }
        return ids;
    }

    private static JsonArray speciesArray(JsonObject modifier) {
        JsonElement species = modifier.get("species");
        if (species.isJsonArray()) {
            return species.getAsJsonArray();
        }
        JsonArray array = new JsonArray();
        array.add(species);
        return array;
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

    private static List<Path> files(Path directory, String glob) throws IOException {
        try (Stream<Path> stream = Files.list(directory)) {
            List<Path> result = new ArrayList<>();
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().matches(
                            glob.replace(".", "\\.").replace("*", ".*")))
                    .sorted()
                    .forEach(result::add);
            return result;
        }
    }

    private static JsonObject read(Path path) throws IOException {
        assertTrue(Files.isRegularFile(path), "missing resource " + path);
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
