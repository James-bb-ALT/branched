package dev.branched.profile;

import dev.branched.core.ResourceId;
import dev.branched.species.CompatibilityLevel;
import dev.branched.species.SpeciesProfile;
import dev.branched.species.SpeciesRegistry;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileLoadingTest {
    private static final Path PROFILE_ROOT =
            Path.of("src/main/resources/data/branched/arboreal/species");

    @Test
    void loadsEveryShippedProfileWithoutErrors() throws Exception {
        ProfileParser.ParseResult result = new ProfileParser().parseDirectory(PROFILE_ROOT);
        assertEquals(0, result.errorCount(), () -> String.join("\n", result.errorMessages()));
        assertEquals(70, result.profiles().size());
        assertTrue(result.profiles().stream().allMatch(p -> p.compatibility() != null));
    }

    @Test
    void profileIdsAreUniqueAndNamespaced() throws Exception {
        ProfileParser.ParseResult result = new ProfileParser().parseDirectory(PROFILE_ROOT);
        long distinct = result.profiles().stream().map(SpeciesProfile::id).distinct().count();
        assertEquals(result.profiles().size(), distinct);
        Set<String> namespaces = result.profiles().stream()
                .map(p -> p.id().namespace()).collect(Collectors.toSet());
        assertTrue(namespaces.containsAll(List.of(
                "minecraft", "regions_unexplored", "aether", "twilightforest", "quark", "fishofthieves",
                "ecologics", "upgrade_aquatic", "deep_aether", "deeperdarker", "alexscaves", "baobabtree",
                "biomemakeover")));
        for (SpeciesProfile profile : result.profiles()) {
            assertFalse(profile.log().equals(profile.leaves()));
            assertTrue(profile.maxSegments() > 0 && profile.maxQuads() > 0);
        }
    }

    @Test
    void registryRejectsDuplicates() throws Exception {
        ProfileParser.ParseResult result = new ProfileParser().parseDirectory(PROFILE_ROOT);
        SpeciesRegistry registry = new SpeciesRegistry();
        ProfileParser.apply(result, registry);
        assertEquals(result.profiles().size(), registry.size());
        assertTrue(registry.contains(ResourceId.parse("minecraft:oak")));
        assertTrue(registry.get(ResourceId.parse("regions_unexplored:baobab")).isPresent());
    }

    @Test
    void fruitProfilesDeclareFruitBearing() throws Exception {
        ProfileParser.ParseResult result = new ProfileParser().parseDirectory(PROFILE_ROOT);
        for (SpeciesProfile profile : result.profiles()) {
            if (profile.compatibility() == CompatibilityLevel.STATIC_FRUIT) {
                assertTrue(profile.fruitBearing(), profile.id() + " must preserve fruit");
            }
        }
    }

    @Test
    void specialTreesAreNeverReplaceable() throws Exception {
        ProfileParser.ParseResult result = new ProfileParser().parseDirectory(PROFILE_ROOT);
        for (SpeciesProfile profile : result.profiles()) {
            if (profile.compatibility() == CompatibilityLevel.SPECIAL_PRESERVE) {
                assertFalse(profile.compatibility().mayReplace());
            }
        }
    }
}
