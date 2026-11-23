package dev.branched.profile;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.branched.core.ResourceId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileValidationTest {
    private final ProfileValidator validator = new ProfileValidator();

    private static final String VALID = """
            {
              "schema": 1,
              "species": "test:oak",
              "archetype": "broadleaf_irregular",
              "logs": "minecraft:oak_log",
              "leaves": "minecraft:oak_leaves",
              "trunk": {"height": [5, 9], "radius": [0.6, 0.9], "bend": [0.0, 0.2]},
              "branches": {"primaryCount": [4, 8], "depth": [1, 3], "angleDegrees": [20, 60],
                           "lengthRatio": [0.2, 0.5], "asymmetry": [0.1, 0.4], "pattern": "alternate"},
              "crown": {"widthRatio": [0.8, 1.4], "density": [0.4, 0.8], "clusterCount": [6, 12]},
              "generation": {"mode": "semiprocedural_once", "attempts": 3, "version": 1},
              "budget": {"maxSegments": 96, "maxLeaves": 256, "maxQuads": 4096},
              "compatibility": "STATIC_NORMAL",
              "source": {"feature": "minecraft:oak"},
              "biomes": ["#minecraft:is_forest"]
            }
            """;

    private JsonObject json(String text) {
        return JsonParser.parseString(text).getAsJsonObject();
    }

    @Test
    void acceptsValidProfile() {
        ValidationResult result = validator.validate(ResourceId.parse("branched:test/oak"), json(VALID));
        assertTrue(result.ok(), () -> result.format());
        assertTrue(result.profile().isPresent());
        assertEquals(ResourceId.parse("test:oak"), result.profile().get().id());
    }

    @Test
    void rejectsUnsupportedSchema() {
        JsonObject object = json(VALID);
        object.addProperty("schema", 2);
        ValidationResult result = validator.validate(ResourceId.parse("branched:test/oak"), object);
        assertFalse(result.ok());
        assertTrue(result.errors().stream().anyMatch(i -> i.code().equals("schema")));
    }

    @Test
    void rejectsInvertedRange() {
        JsonObject object = json(VALID);
        object.getAsJsonObject("trunk").add("height", JsonParser.parseString("[12, 4]"));
        ValidationResult result = validator.validate(ResourceId.parse("branched:test/oak"), object);
        assertFalse(result.ok());
        assertTrue(result.errors().stream().anyMatch(i -> i.code().equals("range_bounds")));
    }

    @Test
    void rejectsMissingLeaves() {
        JsonObject object = json(VALID);
        object.remove("leaves");
        ValidationResult result = validator.validate(ResourceId.parse("branched:test/oak"), object);
        assertFalse(result.ok());
        assertTrue(result.errors().stream().anyMatch(i -> i.code().equals("missing")));
    }

    @Test
    void warnsOnUnknownKeyWithoutRejecting() {
        JsonObject object = json(VALID);
        object.addProperty("invented_field", true);
        ValidationResult result = validator.validate(ResourceId.parse("branched:test/oak"), object);
        assertTrue(result.ok());
        assertTrue(result.warnings().stream().anyMatch(i -> i.code().equals("unknown_key")));
    }

    @Test
    void rejectsTemplateMode() {
        JsonObject object = json(VALID);
        object.getAsJsonObject("generation").addProperty("mode", "nbt_template");
        ValidationResult result = validator.validate(ResourceId.parse("branched:test/oak"), object);
        assertFalse(result.ok());
        assertTrue(result.errors().stream().anyMatch(i -> i.code().equals("generation_mode")));
    }
}
