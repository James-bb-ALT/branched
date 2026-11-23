package dev.branched.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ResourceIdTest {
    @Test
    void parsesNamespacedId() {
        ResourceId id = ResourceId.parse("regions_unexplored:baobab_log");
        assertEquals("regions_unexplored", id.namespace());
        assertEquals("baobab_log", id.path());
        assertEquals("regions_unexplored:baobab_log", id.toString());
    }

    @Test
    void rejectsMalformedIds() {
        assertThrows(IllegalArgumentException.class, () -> ResourceId.parse("oak"));
        assertThrows(IllegalArgumentException.class, () -> ResourceId.parse(":oak"));
        assertThrows(IllegalArgumentException.class, () -> ResourceId.parse("Minecraft:Oak"));
        assertThrows(IllegalArgumentException.class, () -> ResourceId.parse("minecraft:oak log"));
    }

    @Test
    void tryParseIsNullSafe() {
        assertNull(ResourceId.tryParse(null));
        assertNull(ResourceId.tryParse("nope"));
        assertEquals("a:b", ResourceId.tryParse("a:b").toString());
    }

    @Test
    void sortsByNamespaceThenPath() {
        assertEquals(-1, ResourceId.parse("a:z").compareTo(ResourceId.parse("b:a")));
        assertEquals(-1, ResourceId.parse("a:a").compareTo(ResourceId.parse("a:b")));
    }
}
