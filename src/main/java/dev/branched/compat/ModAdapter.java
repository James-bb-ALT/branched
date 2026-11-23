package dev.branched.compat;

import dev.branched.platform.ModPresence;
import dev.branched.species.CompatibilityLevel;

import java.util.List;

/**
 * Isolated integration adapter. A missing or incompatible mod disables only its
 * own adapter; no global worldgen behaviour depends on it.
 */
public interface ModAdapter {
    String modId();

    String displayName();

    String minimumVersion();

    CompatibilityLevel defaultLevel();

    List<FeatureMapping> features();

    default boolean present(ModPresence presence) {
        return presence.isLoaded(modId());
    }

    default AdapterState state(ModPresence presence) {
        if (!present(presence)) {
            return AdapterState.ABSENT;
        }
        if (features().isEmpty() && defaultLevel() == CompatibilityLevel.UNRESOLVED) {
            return AdapterState.UNRESOLVED;
        }
        return AdapterState.READY;
    }

    default long replaceableFeatureCount() {
        return features().stream().filter(FeatureMapping::replaceable).count();
    }

    default long preservedFeatureCount() {
        return features().stream().filter(f -> !f.replaceable()).count();
    }
}
