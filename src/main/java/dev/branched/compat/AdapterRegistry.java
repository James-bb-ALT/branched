package dev.branched.compat;

import dev.branched.platform.ModPresence;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Registry that resolves each adapter independently against loaded mods. */
public final class AdapterRegistry {
    private final Map<String, ModAdapter> adapters = new LinkedHashMap<>();

    public synchronized void register(ModAdapter adapter) {
        if (adapters.putIfAbsent(adapter.modId(), adapter) != null) {
            throw new IllegalStateException("Duplicate adapter for mod " + adapter.modId());
        }
    }

    public synchronized void replaceAll(List<ModAdapter> replacement) {
        adapters.clear();
        for (ModAdapter adapter : replacement) {
            register(adapter);
        }
    }

    public synchronized List<ModAdapter> all() {
        List<ModAdapter> list = new ArrayList<>(adapters.values());
        list.sort(java.util.Comparator.comparing(ModAdapter::modId));
        return Collections.unmodifiableList(list);
    }

    public synchronized Map<String, AdapterState> states(ModPresence presence) {
        Map<String, AdapterState> states = new LinkedHashMap<>();
        for (ModAdapter adapter : all()) {
            states.put(adapter.modId(), adapter.state(presence));
        }
        return states;
    }

    public synchronized List<ModAdapter> ready(ModPresence presence) {
        List<ModAdapter> ready = new ArrayList<>();
        for (ModAdapter adapter : all()) {
            if (adapter.state(presence) == AdapterState.READY) {
                ready.add(adapter);
            }
        }
        return ready;
    }

    public synchronized List<FeatureMapping> replaceableFeatures(ModPresence presence) {
        List<FeatureMapping> features = new ArrayList<>();
        for (ModAdapter adapter : ready(presence)) {
            for (FeatureMapping mapping : adapter.features()) {
                if (mapping.replaceable()) {
                    features.add(mapping);
                }
            }
        }
        return features;
    }

    /**
     * Whether the audited catalog declares this exact PlacedFeature as safe to
     * replace. Unknown ids return {@code false}, so an unlisted feature always
     * keeps its native implementation.
     */
    public synchronized boolean isReplaceable(dev.branched.core.ResourceId feature) {
        for (ModAdapter adapter : adapters.values()) {
            for (FeatureMapping mapping : adapter.features()) {
                if (mapping.feature().equals(feature)) {
                    return mapping.replaceable();
                }
            }
        }
        return false;
    }

    public synchronized int size() {
        return adapters.size();
    }

    public synchronized void clear() {
        adapters.clear();
    }
}
