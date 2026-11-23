package dev.branched.morphology;

import java.util.Objects;

/** Inclusive double range with validation and deterministic sampling. */
public record DoubleRange(double min, double max) {
    public DoubleRange {
        if (!Double.isFinite(min) || !Double.isFinite(max)) {
            throw new IllegalArgumentException("Range bounds must be finite: [" + min + ", " + max + "]");
        }
        if (min > max) {
            throw new IllegalArgumentException("Range min must be <= max: [" + min + ", " + max + "]");
        }
    }

    public static DoubleRange of(double value) {
        return new DoubleRange(value, value);
    }

    public double sample(DeterministicRandom random) {
        Objects.requireNonNull(random, "random");
        return min == max ? min : min + (max - min) * random.nextDouble();
    }

    public double clamp(double value) {
        return Math.max(min, Math.min(max, value));
    }

    public double midpoint() {
        return (min + max) * 0.5D;
    }
}
