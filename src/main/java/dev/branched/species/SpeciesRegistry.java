package dev.branched.species;

import dev.branched.core.ResourceId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Thread-safe registry of validated species profiles. */
public final class SpeciesRegistry {
    private final Map<ResourceId, SpeciesProfile> profiles = new ConcurrentHashMap<>();

    public void register(SpeciesProfile profile) {
        SpeciesProfile previous = profiles.putIfAbsent(profile.id(), profile);
        if (previous != null) {
            throw new IllegalStateException("Duplicate species profile: " + profile.id());
        }
    }

    /** Atomically replaces the registry content after a successful reload. */
    public void replaceAll(Collection<SpeciesProfile> replacement) {
        Map<ResourceId, SpeciesProfile> next = new ConcurrentHashMap<>();
        for (SpeciesProfile profile : replacement) {
            if (next.putIfAbsent(profile.id(), profile) != null) {
                throw new IllegalStateException("Duplicate species profile: " + profile.id());
            }
        }
        profiles.clear();
        profiles.putAll(next);
    }

    public Optional<SpeciesProfile> get(ResourceId id) {
        return Optional.ofNullable(profiles.get(id));
    }

    public boolean contains(ResourceId id) {
        return profiles.containsKey(id);
    }

    public int size() {
        return profiles.size();
    }

    public List<SpeciesProfile> all() {
        List<SpeciesProfile> result = new ArrayList<>(profiles.values());
        result.sort(Comparator.comparing(SpeciesProfile::id));
        return List.copyOf(result);
    }

    public void clear() {
        profiles.clear();
    }
}
