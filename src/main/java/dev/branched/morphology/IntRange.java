package dev.branched.morphology;

/** Inclusive integer range with validation and deterministic sampling. */
public record IntRange(int min, int max) {
    public IntRange {
        if (min > max) {
            throw new IllegalArgumentException("Range min must be <= max: [" + min + ", " + max + "]");
        }
    }

    public static IntRange of(int value) {
        return new IntRange(value, value);
    }

    public int sample(DeterministicRandom random) {
        if (min == max) {
            return min;
        }
        return min + random.nextInt(max - min + 1);
    }

    public int clamp(int value) {
        return Math.max(min, Math.min(max, value));
    }
}
