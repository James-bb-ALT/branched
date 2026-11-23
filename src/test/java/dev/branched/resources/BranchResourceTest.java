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
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates the 6-way multipart branch assets, models, item models, loot tables,
 * and tags without bootstrapping Minecraft.
 */
class BranchResourceTest {
    private static final Path RESOURCES = Path.of("src/main/resources");
    private static final Path ASSETS = RESOURCES.resolve("assets/branched");
    private static final Path DATA = RESOURCES.resolve("data/branched");
    private static final int EXPECTED_BRANCH_COUNT = 66;

    private static List<String> allBranchIds() throws IOException {
        try (Stream<Path> stream = Files.list(ASSETS.resolve("blockstates"))) {
            return stream.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith("_branch.json"))
                    .map(n -> n.substring(0, n.length() - ".json".length()))
                    .sorted()
                    .toList();
        }
    }

    @Test
    void everyBranchBlockstateIsSixWayMultipartWithExistingModels() throws IOException {
        List<String> blocks = allBranchIds();
        assertEquals(EXPECTED_BRANCH_COUNT, blocks.size(), "registered branch blockstate count");

        for (String block : blocks) {
            JsonObject blockstate = read(ASSETS.resolve("blockstates/" + block + ".json"));
            JsonArray multipart = blockstate.getAsJsonArray("multipart");
            assertEquals(21, multipart.size(), block + " must have 3 thickness gauges x (1 core + 6 directional sleeves)");

            Set<String> seenThicknesses = new LinkedHashSet<>();
            Set<String> seenDirections = new LinkedHashSet<>();
            for (JsonElement partEl : multipart) {
                JsonObject part = partEl.getAsJsonObject();
                String modelId = part.getAsJsonObject("apply").get("model").getAsString();
                assertTrue(Files.isRegularFile(modelPath(modelId)), "missing model " + modelId);
                assertTrue(part.has("when"), block + " multipart entry must specify thickness predicate");
                JsonObject when = part.getAsJsonObject("when");
                assertTrue(when.has("thickness"), block + " multipart entry missing thickness condition");
                seenThicknesses.add(when.get("thickness").getAsString());
                if (when.size() == 1) {
                    assertTrue(modelId.endsWith("_core"), "gauge core part must use a _core model: " + modelId);
                } else {
                    for (String key : when.keySet()) {
                        if (!"thickness".equals(key)) {
                            seenDirections.add(key);
                        }
                    }
                }
            }
            assertEquals(Set.of("1", "2", "3"), seenThicknesses, block + " thickness gauge coverage");
            assertEquals(Set.of("north", "east", "south", "west", "up", "down"),
                    seenDirections, block + " directional sleeve coverage");
        }
    }

    @Test
    void coreAndSleeveTemplatesUseBarkOnJointsAndBakedElementsOnly() throws IOException {
        for (String prefix : List.of("", "thin_", "thick_")) {
            JsonObject coreTemplate = read(ASSETS.resolve("models/block/branch_" + prefix + "core_template.json"));
            JsonArray coreElements = coreTemplate.getAsJsonArray("elements");
            assertEquals(1, coreElements.size());
            JsonObject coreFaces = coreElements.get(0).getAsJsonObject().getAsJsonObject("faces");
            assertEquals(6, coreFaces.size());
            for (String face : List.of("up", "down", "north", "south", "east", "west")) {
                assertEquals("#side", coreFaces.getAsJsonObject(face).get("texture").getAsString(),
                        "core joint must wrap all faces with bark (#side) so horizontal elbows do not show cut rings");
            }

            for (String suffix : List.of("sleeve_north_template", "sleeve_up_template", "sleeve_down_template")) {
                JsonObject model = read(ASSETS.resolve("models/block/branch_" + prefix + suffix + ".json"));
                assertEquals("minecraft:block/block", model.get("parent").getAsString());
                assertFalse(model.getAsJsonArray("elements").isEmpty(), prefix + suffix + " missing elements");
            }
        }
        JsonObject inventory = read(ASSETS.resolve("models/block/branch_inventory_template.json"));
        assertEquals("minecraft:block/block", inventory.get("parent").getAsString());
        assertFalse(inventory.getAsJsonArray("elements").isEmpty(), "branch_inventory_template missing elements");
    }

    @Test
    void itemModelsAndLootTablesMatchAllRegisteredBranchBlocks() throws IOException {
        for (String block : allBranchIds()) {
            JsonObject item = read(ASSETS.resolve("models/item/" + block + ".json"));
            assertTrue(Files.isRegularFile(modelPath(item.get("parent").getAsString())),
                    "item model parent missing for " + block);

            JsonObject loot = read(DATA.resolve("loot_tables/blocks/" + block + ".json"));
            assertEquals("minecraft:block", loot.get("type").getAsString());
            String drop = loot.getAsJsonArray("pools").get(0).getAsJsonObject()
                    .getAsJsonArray("entries").get(0).getAsJsonObject()
                    .get("name").getAsString();
            assertEquals("branched:" + block, drop);
        }
    }

    @Test
    void allBranchBlocksAreLogsAndAxeMineable() throws IOException {
        List<String> blocks = allBranchIds();
        for (String tag : List.of("logs", "mineable/axe")) {
            JsonObject json = read(RESOURCES.resolve("data/minecraft/tags/blocks/" + tag + ".json"));
            assertFalse(json.get("replace").getAsBoolean());
            Set<String> values = new LinkedHashSet<>();
            json.getAsJsonArray("values").forEach(value -> values.add(value.getAsString()));
            for (String block : blocks) {
                assertTrue(values.contains("branched:" + block),
                        block + " missing from #minecraft:" + tag);
            }
        }

        for (String tagPath : List.of("tags/blocks/branches.json", "tags/items/branches.json")) {
            JsonObject tagJson = read(DATA.resolve(tagPath));
            assertFalse(tagJson.get("replace").getAsBoolean());
            Set<String> values = new LinkedHashSet<>();
            tagJson.getAsJsonArray("values").forEach(value -> values.add(value.getAsString()));
            assertEquals(blocks.size(), values.size(), tagPath + " count mismatch");
            for (String block : blocks) {
                assertTrue(values.contains("branched:" + block),
                        block + " missing from " + tagPath);
            }
        }

        JsonObject recipe = read(DATA.resolve("recipes/branch_to_sticks.json"));
        assertEquals("minecraft:crafting_shapeless", recipe.get("type").getAsString());
        JsonArray ingredients = recipe.getAsJsonArray("ingredients");
        assertEquals(1, ingredients.size());
        assertEquals("branched:branches", ingredients.get(0).getAsJsonObject().get("tag").getAsString());
        JsonObject result = recipe.getAsJsonObject("result");
        assertEquals("minecraft:stick", result.get("item").getAsString());
        assertTrue(result.get("count").getAsInt() >= 1);
    }

    @Test
    void configuredFeatureUsesTheStaticTreeType() throws IOException {
        JsonObject feature = read(DATA.resolve("worldgen/configured_feature/static_spruce.json"));
        assertEquals("branched:static_tree", feature.get("type").getAsString());
        JsonObject config = feature.getAsJsonObject("config");
        assertEquals("minecraft:spruce", config.get("species").getAsString());
        assertEquals(3, config.get("attempts").getAsInt());
    }

    @Test
    void placedFeatureMirrorsVanillaTaigaPlacement() throws IOException {
        JsonObject placed = read(DATA.resolve("worldgen/placed_feature/static_trees_taiga.json"));
        assertEquals("branched:static_trees_taiga", placed.get("feature").getAsString());

        JsonArray placement = placed.getAsJsonArray("placement");
        List<String> types = new ArrayList<>();
        placement.forEach(step -> types.add(step.getAsJsonObject().get("type").getAsString()));
        assertEquals(List.of(
                "minecraft:count",
                "minecraft:in_square",
                "minecraft:surface_water_depth_filter",
                "minecraft:heightmap",
                "minecraft:biome"), types);
    }

    @Test
    void biomeModifierReplacesExactlyTheAuditedFeature() throws IOException {
        JsonObject modifier = read(DATA.resolve(
                "forge/biome_modifier/vanilla_trees_taiga.json"));
        assertEquals("branched:replace_trees", modifier.get("type").getAsString());
        assertEquals(List.of("minecraft:taiga", "minecraft:snowy_taiga"),
                strings(modifier.getAsJsonArray("biomes")));
        assertEquals(List.of("minecraft:trees_taiga"),
                strings(modifier.getAsJsonArray("replace")));
        assertEquals("branched:static_trees_taiga", modifier.get("add").getAsString());
        assertEquals(List.of("minecraft:spruce", "minecraft:pine"),
                strings(modifier.getAsJsonArray("species")));
        assertEquals("vegetal_decoration", modifier.get("step").getAsString());
    }

    @Test
    void pilotSpeciesProfileIsReplaceable() throws IOException {
        JsonObject profile = read(RESOURCES.resolve(
                "data/branched/arboreal/species/minecraft/spruce.json"));
        assertEquals("STATIC_NORMAL", profile.get("compatibility").getAsString());
        assertTrue(List.of("minecraft:spruce_log", "minecraft:spruce_wood")
                        .contains(profile.get("logs").getAsString()),
                "profile must declare a known spruce log");
    }

    private static JsonObject read(Path path) throws IOException {
        assertTrue(Files.isRegularFile(path), "missing resource " + path);
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    private static Path modelPath(String modelId) {
        String path = modelId.substring("branched:".length());
        return ASSETS.resolve("models/" + path + ".json");
    }

    private static List<String> strings(JsonArray array) {
        List<String> values = new ArrayList<>();
        array.forEach(value -> values.add(value.getAsString()));
        return values;
    }
}
