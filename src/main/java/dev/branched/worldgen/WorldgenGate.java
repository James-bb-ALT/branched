package dev.branched.worldgen;

import dev.branched.Branched;
import dev.branched.config.ArborealConfig;
import dev.branched.core.ResourceId;
import dev.branched.morphology.PerformancePreset;
import dev.branched.species.SpeciesProfile;

import java.util.Optional;

/**
 * Single gate for every worldgen decision. Replacement only happens when the
 * master switch is on and the species profile exists and is replaceable; any
 * uncertainty preserves the native tree.
 */
public final class WorldgenGate {
    private WorldgenGate() {
    }

    public static boolean enabled() {
        return !ArborealConfig.SPEC.isLoaded() || ArborealConfig.WORLDGEN_ENABLED.get();
    }

    public static boolean requireKnownFeatures() {
        return !ArborealConfig.SPEC.isLoaded() || ArborealConfig.REPLACE_KNOWN_FEATURES.get();
    }

    public static int maxAttempts(int requested) {
        int cap = ArborealConfig.SPEC.isLoaded()
                ? ArborealConfig.MAX_GENERATION_ATTEMPTS.get()
                : 8;
        return Math.max(1, Math.min(requested, cap));
    }

    /** Resolves {@code rendering.preset}; unknown or unloaded config means balanced. */
    public static PerformancePreset performancePreset() {
        String value = ArborealConfig.SPEC.isLoaded()
                ? ArborealConfig.RENDER_PRESET.get()
                : "balanced";
        return PerformancePreset.parse(value);
    }

    public static Optional<SpeciesProfile> replaceableProfile(ResourceId species) {
        if (!enabled()) {
            return Optional.empty();
        }
        return Branched.profiles().get(species)
                .filter(profile -> profile.compatibility().mayReplace());
    }
}
