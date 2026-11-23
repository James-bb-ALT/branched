package dev.branched.platform;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

/** Deterministic fake used by unit tests and offline tooling. */
public final class StaticModPresence implements ModPresence {
    private final Set<String> loaded;
    private final Map<String, String> versions;

    public StaticModPresence(Set<String> loaded) {
        this(loaded, Map.of());
    }

    public StaticModPresence(Set<String> loaded, Map<String, String> versions) {
        this.loaded = Set.copyOf(loaded);
        this.versions = Map.copyOf(versions);
    }

    @Override
    public boolean isLoaded(String modId) {
        return loaded.contains(modId);
    }

    @Override
    public Set<String> loadedMods() {
        return loaded;
    }

    @Override
    public Optional<String> version(String modId) {
        return Optional.ofNullable(versions.get(modId));
    }
}
