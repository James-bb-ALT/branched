package dev.branched.compat;

import dev.branched.core.ResourceId;
import dev.branched.species.CompatibilityLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Small immutable builder shared by the per-mod adapters. */
public abstract class AbstractModAdapter implements ModAdapter {
    private final String modId;
    private final String displayName;
    private final String minimumVersion;
    private final CompatibilityLevel defaultLevel;
    private final List<FeatureMapping> features = new ArrayList<>();

    protected AbstractModAdapter(String modId, String displayName, String minimumVersion,
                                 CompatibilityLevel defaultLevel) {
        this.modId = Objects.requireNonNull(modId, "modId");
        this.displayName = Objects.requireNonNull(displayName, "displayName");
        this.minimumVersion = minimumVersion == null ? "" : minimumVersion;
        this.defaultLevel = Objects.requireNonNull(defaultLevel, "defaultLevel");
    }

    protected final void feature(String placedFeature, String species, CompatibilityLevel level, String notes) {
        features.add(new FeatureMapping(ResourceId.parse(placedFeature), ResourceId.parse(species), level, notes));
    }

    protected final void feature(String placedFeature, String species, CompatibilityLevel level) {
        feature(placedFeature, species, level, "");
    }

    @Override
    public final String modId() { return modId; }

    @Override
    public final String displayName() { return displayName; }

    @Override
    public final String minimumVersion() { return minimumVersion; }

    @Override
    public final CompatibilityLevel defaultLevel() { return defaultLevel; }

    @Override
    public final List<FeatureMapping> features() { return List.copyOf(features); }
}
