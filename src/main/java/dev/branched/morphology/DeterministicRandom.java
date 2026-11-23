package dev.branched.morphology;

/**
 * SplitMix64-based deterministic RNG. It has no global state, no dependency on
 * chunk generation order and produces the same sequence for the same seed on
 * every JVM. Not cryptographically secure and not intended to be.
 */
public final class DeterministicRandom {
    private long state;

    public DeterministicRandom(long seed) {
        this.state = seed;
    }

    public long nextLong() {
        long z = (state += 0x9E3779B97F4A7C15L);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** Uniform in [0, 1). */
    public double nextDouble() {
        return (nextLong() >>> 11) * 0x1.0p-53;
    }

    /** Uniform in [0, bound). */
    public int nextInt(int bound) {
        if (bound <= 0) {
            throw new IllegalArgumentException("bound must be positive");
        }
        long limit = Integer.MAX_VALUE - (Integer.MAX_VALUE % bound);
        long value;
        do {
            value = nextLong() >>> 33;
        } while (value >= limit);
        return (int) (value % bound);
    }

    public boolean nextBoolean(double probability) {
        return nextDouble() < probability;
    }

    public double between(DoubleRange range) {
        return range.sample(this);
    }

    public int between(IntRange range) {
        return range.sample(this);
    }

    /** A unit vector on the XZ plane. */
    public double[] nextHorizontalUnitVector() {
        double angle = nextDouble() * Math.PI * 2.0D;
        return new double[] {Math.cos(angle), Math.sin(angle)};
    }
}
