package dev.branched.compat;

import dev.branched.compat.adapter.BaobabAdapter;
import dev.branched.core.ResourceId;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Proves the production artifact has no Dynamic Trees dependency, neither in
 * mods.toml nor through Java imports.
 */
class NoDynamicTreesDependencyTest {
    @Test
    void modsTomlHasNoDynamicTreesDependency() throws IOException {
        String toml = Files.readString(Path.of("src/main/resources/META-INF/mods.toml"));
        assertFalse(toml.toLowerCase().contains("dynamictrees"),
                "mods.toml must not declare Dynamic Trees");
        assertTrue(toml.contains("modId = \"${mod_id}\""));
        assertTrue(toml.contains("modId = \"regions_unexplored\""));
        assertTrue(toml.contains("mandatory = false"));
    }

    @Test
    void sourceTreeHasNoDynamicTreesImports() throws IOException {
        try (Stream<Path> stream = Files.walk(Path.of("src/main/java"))) {
            List<Path> sources = stream.filter(p -> p.toString().endsWith(".java")).toList();
            assertFalse(sources.isEmpty());
            for (Path source : sources) {
                String text = Files.readString(source).toLowerCase();
                assertFalse(text.contains("import dynamictrees") || text.contains("import com.dynamictrees"),
                        "unexpected Dynamic Trees import in " + source);
            }
        }
    }

    @Test
    void baobabRemainsUnresolvedAndInactive() {
        BaobabAdapter adapter = new BaobabAdapter();
        assertTrue(adapter.features().isEmpty());
        assertTrue(adapter.defaultLevel() == dev.branched.species.CompatibilityLevel.UNRESOLVED);
        assertFalse(adapter.present(new dev.branched.platform.StaticModPresence(
                java.util.Set.of("dtbaobabtree"))));
    }
}
