package dev.branched.worldgen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WorldgenGateTest {
    @Test
    void attemptsAreClampedToTheSupportedRange() {
        assertEquals(1, WorldgenGate.maxAttempts(0));
        assertEquals(1, WorldgenGate.maxAttempts(-5));
        assertEquals(3, WorldgenGate.maxAttempts(3));
        // Without a loaded config the documented absolute maximum applies.
        assertEquals(8, WorldgenGate.maxAttempts(99));
    }
}
