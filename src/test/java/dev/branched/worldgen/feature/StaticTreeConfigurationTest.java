package dev.branched.worldgen.feature;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.JsonOps;
import dev.branched.core.ResourceId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StaticTreeConfigurationTest {
    @Test
    void decodesSpeciesAndAttempts() {
        JsonObject json = JsonParser.parseString(
                "{\"species\":\"minecraft:spruce\",\"attempts\":5}").getAsJsonObject();

        StaticTreeConfiguration config = decode(json);

        assertEquals(ResourceId.parse("minecraft:spruce"), config.species());
        assertEquals(5, config.attempts());
    }

    @Test
    void missingAttemptsDefaultsToThree() {
        JsonObject json = JsonParser.parseString(
                "{\"species\":\"regions_unexplored:baobab\"}").getAsJsonObject();

        assertEquals(3, decode(json).attempts());
    }

    @Test
    void outOfRangeAttemptsFallBackToTheDefault() {
        // The optional field is lenient by design: an out-of-range attempt count
        // never breaks worldgen, it falls back to the documented default.
        assertEquals(3, decode(JsonParser.parseString(
                "{\"species\":\"minecraft:oak\",\"attempts\":0}").getAsJsonObject()).attempts());
        assertEquals(3, decode(JsonParser.parseString(
                "{\"species\":\"minecraft:oak\",\"attempts\":9}").getAsJsonObject()).attempts());
    }

    @Test
    void missingOrMalformedSpeciesIsRejected() {
        assertTrue(failure("{}").error().isPresent());
        assertTrue(failure("{\"species\":\"minecraft:\"}").error().isPresent());
    }

    @Test
    void codecRoundTrips() {
        StaticTreeConfiguration original =
                new StaticTreeConfiguration(ResourceId.parse("minecraft:spruce"), 2);

        JsonObject encoded = StaticTreeConfiguration.CODEC
                .encodeStart(JsonOps.INSTANCE, original)
                .result()
                .orElseThrow(() -> new AssertionError("encode failed"))
                .getAsJsonObject();

        assertEquals("minecraft:spruce", encoded.get("species").getAsString());
        assertEquals(2, encoded.get("attempts").getAsInt());
        assertEquals(original, decode(encoded));
    }

    @Test
    void constructorRejectsInvalidAttempts() {
        assertTrue(throwsIllegalArgument(() ->
                new StaticTreeConfiguration(ResourceId.parse("minecraft:oak"), 0)));
        assertTrue(throwsIllegalArgument(() ->
                new StaticTreeConfiguration(ResourceId.parse("minecraft:oak"), 9)));
        assertFalse(throwsIllegalArgument(() ->
                new StaticTreeConfiguration(ResourceId.parse("minecraft:oak"), 3)));
    }

    private static StaticTreeConfiguration decode(JsonObject json) {
        return StaticTreeConfiguration.CODEC.parse(JsonOps.INSTANCE, json)
                .result()
                .orElseThrow(() -> new AssertionError("decode failed"));
    }

    private static DataResult<StaticTreeConfiguration> failure(String json) {
        return StaticTreeConfiguration.CODEC.parse(
                JsonOps.INSTANCE, JsonParser.parseString(json));
    }

    private static boolean throwsIllegalArgument(Runnable action) {
        try {
            action.run();
            return false;
        } catch (IllegalArgumentException expected) {
            return true;
        }
    }
}
