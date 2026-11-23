package dev.branched.compat;

import dev.branched.platform.ModPresence;
import dev.branched.platform.StaticModPresence;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AdapterIsolationTest {
    private final AdapterRegistry registry = new AdapterRegistry();

    AdapterIsolationTest() {
        registry.replaceAll(DefaultAdapters.create());
    }

    @Test
    void vanillaOnlyEnvironmentDoesNotTouchOptionalMods() {
        ModPresence presence = new StaticModPresence(Set.of("minecraft", "forge"));
        Map<String, AdapterState> states = registry.states(presence);
        assertEquals(24, states.size());
        assertEquals(AdapterState.READY, states.get("minecraft"));
        assertEquals(AdapterState.ABSENT, states.get("regions_unexplored"));
        assertEquals(AdapterState.ABSENT, states.get("terralith"));
        assertEquals(AdapterState.ABSENT, states.get("aether"));
        assertEquals(AdapterState.ABSENT, states.get("deep_aether"));
        assertEquals(AdapterState.ABSENT, states.get("twilightforest"));
        assertEquals(AdapterState.ABSENT, states.get("quark"));
        assertEquals(AdapterState.ABSENT, states.get("fishofthieves"));
        assertEquals(AdapterState.ABSENT, states.get("ecologics"));
        assertEquals(AdapterState.ABSENT, states.get("upgrade_aquatic"));
        assertEquals(AdapterState.ABSENT, states.get("deeperdarker"));
        assertEquals(AdapterState.ABSENT, states.get("alexscaves"));
        assertEquals(AdapterState.ABSENT, states.get("baobabtree"));
        assertEquals(AdapterState.ABSENT, states.get("biomemakeover"));
        assertEquals(AdapterState.ABSENT, states.get("dtbaobabtree"));
        assertEquals(AdapterState.ABSENT, states.get("betterend"));
        assertEquals(AdapterState.ABSENT, states.get("betternether"));
        assertEquals(AdapterState.ABSENT, states.get("twilightdelight"));
        assertEquals(AdapterState.ABSENT, states.get("biomesoplenty"));
        assertEquals(AdapterState.ABSENT, states.get("biomeswevegone"));
        assertEquals(AdapterState.ABSENT, states.get("natures_spirit"));
        assertEquals(AdapterState.ABSENT, states.get("atmospheric"));
        assertEquals(AdapterState.ABSENT, states.get("environmental"));
        assertEquals(AdapterState.ABSENT, states.get("autumnity"));
        assertFalse(registry.replaceableFeatures(presence).isEmpty());
    }

    @Test
    void allIntegrationsPresentResolveIndependently() {
        ModPresence presence = new StaticModPresence(Set.of("minecraft", "regions_unexplored",
                "terralith", "aether", "twilightforest", "quark", "fishofthieves",
                "biomemakeover", "baobabtree", "betterend", "betternether", "twilightdelight",
                "biomesoplenty", "biomeswevegone", "natures_spirit",
                "atmospheric", "environmental", "autumnity"), Map.ofEntries(
                Map.entry("regions_unexplored", "0.5.6"),
                Map.entry("terralith", "2.5.4"),
                Map.entry("aether", "1.20.1-1.5.2-neoforge"),
                Map.entry("twilightforest", "4.3.2508"),
                Map.entry("quark", "4.0-462"),
                Map.entry("fishofthieves", "3.1.7"),
                Map.entry("biomemakeover", "1.11.4"),
                Map.entry("baobabtree", "1.2.3"),
                Map.entry("betterend", "20.0.13"),
                Map.entry("betternether", "20.0.13"),
                Map.entry("twilightdelight", "2.2.9"),
                Map.entry("biomesoplenty", "19.0.0.96"),
                Map.entry("biomeswevegone", "1.8.2"),
                Map.entry("natures_spirit", "2.2.5"),
                Map.entry("atmospheric", "6.1.1"),
                Map.entry("environmental", "4.1.2"),
                Map.entry("autumnity", "5.0.2")));
        Map<String, AdapterState> states = registry.states(presence);
        for (String mod : Set.of("regions_unexplored", "terralith", "aether", "twilightforest",
                "quark", "fishofthieves", "biomemakeover", "baobabtree", "betterend", "betternether",
                "twilightdelight", "biomesoplenty", "biomeswevegone", "natures_spirit",
                "atmospheric", "environmental", "autumnity")) {
            assertEquals(AdapterState.READY, states.get(mod), mod);
        }
        assertTrue(registry.ready(presence).size() >= 18);
    }

    @Test
    void baobabReferenceNeverActivates() {
        ModPresence presence = new StaticModPresence(Set.of("minecraft", "dtbaobabtree"));
        assertEquals(AdapterState.ABSENT, registry.states(presence).get("dtbaobabtree"));
    }

    @Test
    void specialTreesAreNotOfferedForReplacement() {
        ModPresence presence = new StaticModPresence(Set.of("minecraft", "twilightforest", "quark"));
        var features = registry.replaceableFeatures(presence);
        assertTrue(features.stream().noneMatch(f -> f.feature().toString().equals("twilightforest:tree/mining_tree")));
        assertTrue(features.stream().noneMatch(f -> f.feature().toString().equals("quark:glow_shrooms")));
        assertTrue(features.stream().anyMatch(f -> f.feature().toString().equals("twilightforest:tree/twilight_oak_tree")));
    }

    @Test
    void everyDeclaredFeatureBelongsToItsAdapterNamespace() {
        for (ModAdapter adapter : registry.all()) {
            for (FeatureMapping mapping : adapter.features()) {
                assertFalse(mapping.originalSpecies().path().isEmpty());
                assertTrue(mapping.feature().namespace().equals(adapter.modId())
                                || adapter.modId().equals("minecraft"),
                        adapter.modId() + " declares foreign feature " + mapping.feature());
            }
        }
    }
}
