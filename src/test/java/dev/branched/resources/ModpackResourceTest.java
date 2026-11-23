package dev.branched.resources;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.branched.compat.AdapterRegistry;
import dev.branched.compat.DefaultAdapters;
import dev.branched.core.ResourceId;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validates the additional modpack integrations (`ecologics`, `upgrade_aquatic`,
 * `deeperdarker`, `deep_aether`) ensuring all references to foreign
 * biomes/features sit inside optional tags and match registered adapters cleanly.
 */
class ModpackResourceTest {
    private static final Path DATA = Path.of("src/main/resources/data/branched");
    private static final Path MODIFIERS = DATA.resolve("forge/biome_modifier");
    private static final Path CONFIGURED = DATA.resolve("worldgen/configured_feature");
    private static final Path PLACED = DATA.resolve("worldgen/placed_feature");
    private static final Path BIOME_TAGS = DATA.resolve("tags/worldgen/biome");
    private static final Path PF_TAGS = DATA.resolve("tags/worldgen/placed_feature");
    private static final Path SPECIES = DATA.resolve("arboreal/species");

    @Test
    void modpackBiomeModifiersAreTagScopedAndAudited() throws IOException {
        AdapterRegistry registry = new AdapterRegistry();
        registry.replaceAll(DefaultAdapters.create());

        List<Path> packModifiers;
        try (Stream<Path> s = Files.list(MODIFIERS)) {
            packModifiers = s.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().startsWith("pack_"))
                    .sorted()
                    .toList();
        }
        assertEquals(9, packModifiers.size(), "expected 9 modpack biome modifiers");

        for (Path path : packModifiers) {
            JsonObject mod = read(path);
            assertEquals("branched:replace_trees", mod.get("type").getAsString());
            assertEquals("vegetal_decoration", mod.get("step").getAsString());

            String add = mod.get("add").getAsString();
            assertTrue(add.startsWith("branched:modpack/"), path + " add tag prefix");
            String sub = add.substring("branched:".length());
            assertTrue(Files.isRegularFile(PLACED.resolve(sub + ".json")), "missing placed feature " + add);
            assertTrue(Files.isRegularFile(CONFIGURED.resolve(sub + ".json")), "missing configured feature " + add);
            assertTrue(Files.isRegularFile(BIOME_TAGS.resolve(sub + ".json")), "missing biome tag " + add);
            assertTrue(Files.isRegularFile(PF_TAGS.resolve(sub + ".json")), "missing placed feature tag " + add);

            JsonObject pfTag = read(PF_TAGS.resolve(sub + ".json"));
            assertFalse(pfTag.getAsJsonArray("values").isEmpty());
            for (JsonElement el : pfTag.getAsJsonArray("values")) {
                String id = el.isJsonObject() ? el.getAsJsonObject().get("id").getAsString() : el.getAsString();
                assertTrue(registry.isReplaceable(ResourceId.parse(id)),
                        path + " replaces un-audited feature " + id);
            }

            for (JsonElement sp : mod.getAsJsonArray("species")) {
                String sId = sp.getAsString();
                int idx = sId.indexOf(':');
                assertTrue(Files.isRegularFile(SPECIES.resolve(sId.substring(0, idx)).resolve(sId.substring(idx + 1) + ".json")),
                        path + " missing species " + sId);
            }
        }
    }

    private static JsonObject read(Path path) throws IOException {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
