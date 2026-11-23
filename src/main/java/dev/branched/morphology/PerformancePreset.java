package dev.branched.morphology;

import java.util.Locale;

/**
 * Rendering preset that trades crown fill for geometry, as required by the
 * {@code rendering.preset} config key.
 *
 * <p>Only the leaf density and the per-profile leaf budget are scaled; the
 * trunk, branch topology and archetype rules are untouched, so every preset
 * still produces the same silhouette family. {@link #BALANCED} multiplies both
 * factors by 1.0 and therefore reproduces the pre-Phase-5 output byte for
 * byte.</p>
 */
public enum PerformancePreset {
    /** Fewer leaves and a lower leaf cap: smallest quad count. */
    PERFORMANCE(0.82D, 0.85D),
    /** Shipped default, identical to the pre-Phase-5 geometry. */
    BALANCED(1.0D, 1.0D),
    /** Denser crown, larger leaf cap: more quads and overdraw. */
    VISUAL(1.16D, 1.15D);

    private final double leafDensityFactor;
    private final double leafBudgetFactor;

    PerformancePreset(double leafDensityFactor, double leafBudgetFactor) {
        this.leafDensityFactor = leafDensityFactor;
        this.leafBudgetFactor = leafBudgetFactor;
    }

    public double leafDensityFactor() {
        return leafDensityFactor;
    }

    public double leafBudgetFactor() {
        return leafBudgetFactor;
    }

    /** Applies the preset to a profile's {@code budget.maxLeaves}. */
    public int leafBudget(int profileMaxLeaves) {
        return Math.max(8, (int) Math.round(profileMaxLeaves * leafBudgetFactor));
    }

    /** Parses a config value; anything unknown falls back to {@link #BALANCED}. */
    public static PerformancePreset parse(String raw) {
        if (raw == null) {
            return BALANCED;
        }
        return switch (raw.trim().toLowerCase(Locale.ROOT)) {
            case "performance" -> PERFORMANCE;
            case "visual" -> VISUAL;
            default -> BALANCED;
        };
    }
}
