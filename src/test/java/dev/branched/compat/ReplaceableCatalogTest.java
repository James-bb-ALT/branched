package dev.branched.compat;

import dev.branched.core.ResourceId;
import dev.branched.species.CompatibilityLevel;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplaceableCatalogTest {
    private static final List<String> REPLACED = List.of(
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

    private static final List<String> PRESERVED = List.of(
            "trees_mangrove",
            "trees_plains",
            "trees_water");

    private final AdapterRegistry registry = new AdapterRegistry();
    private final Map<String, FeatureMapping> aggregates = new LinkedHashMap<>();

    ReplaceableCatalogTest() {
        registry.replaceAll(DefaultAdapters.create());
        for (ModAdapter adapter : registry.all()) {
            for (FeatureMapping mapping : adapter.features()) {
                String path = mapping.feature().path();
                if ("minecraft".equals(mapping.feature().namespace()) && path.startsWith("trees_")) {
                    aggregates.put(path, mapping);
                }
            }
        }
    }

    @Test
    void everyVanillaTreeAggregateIsAuditedExactlyOnce() {
        assertEquals(21, aggregates.size(), "vanilla core tree aggregates");
        assertEquals(REPLACED.size() + PRESERVED.size(), aggregates.size());
        assertTrue(aggregates.keySet().containsAll(REPLACED));
        assertTrue(aggregates.keySet().containsAll(PRESERVED));
    }

    @Test
    void phaseTwoReplaceSetIsAuditedAsReplaceable() {
        for (String path : REPLACED) {
            FeatureMapping mapping = aggregates.get(path);
            assertTrue(registry.isReplaceable(ResourceId.parse("minecraft:" + path)),
                    path + " must be audited as replaceable");
            assertEquals(CompatibilityLevel.STATIC_NORMAL, mapping.level(), path + " level");
        }
    }

    @Test
    void preservedFeaturesStayNativeWithADocumentedReason() {
        for (String path : PRESERVED) {
            FeatureMapping mapping = aggregates.get(path);
            assertFalse(registry.isReplaceable(ResourceId.parse("minecraft:" + path)),
                    path + " must stay native");
            assertEquals(CompatibilityLevel.SPECIAL_PRESERVE, mapping.level(), path + " level");
            assertFalse(mapping.notes().isBlank(), path + " needs a preserve reason");
        }
    }

    @Test
    void unknownFeaturesAreNeverReplaceable() {
        assertFalse(registry.isReplaceable(ResourceId.parse("some_mod:mystery_tree")));
        assertFalse(registry.isReplaceable(ResourceId.parse("minecraft:trees_not_a_real_feature")));
    }

    @Test
    void adapterAggregateCountsMatchThePhaseTwoPlan() {
        long replaceable = aggregates.values().stream()
                .filter(FeatureMapping::replaceable).count();
        long preserved = aggregates.size() - replaceable;
        assertEquals(REPLACED.size(), replaceable);
        assertEquals(PRESERVED.size(), preserved);
    }
}
