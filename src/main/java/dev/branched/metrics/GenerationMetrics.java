package dev.branched.metrics;

import java.util.Arrays;

/** Immutable snapshot of generation metrics. */
public record GenerationMetrics(long generatedTrees,
                                long rejectedPlacements,
                                long segments,
                                long leafClusters,
                                long blocks,
                                long estimatedQuads,
                                long totalNanos,
                                long maxNanos,
                                int trackedProfiles,
                                long p50Nanos,
                                long p95Nanos,
                                long p99Nanos,
                                int latencySamples) {
    public double averageNanos() {
        return generatedTrees == 0 ? 0.0D : (double) totalNanos / (double) generatedTrees;
    }

    public String format() {
        return String.format(java.util.Locale.ROOT,
                "trees=%d rejected=%d segments=%d clusters=%d blocks=%d quads=%d "
                        + "avg=%.2fms max=%.2fms p50=%.2fms p95=%.2fms p99=%.2fms samples=%d profiles=%d",
                generatedTrees, rejectedPlacements, segments, leafClusters, blocks, estimatedQuads,
                averageNanos() / 1_000_000.0D, maxNanos / 1_000_000.0D,
                p50Nanos / 1_000_000.0D, p95Nanos / 1_000_000.0D, p99Nanos / 1_000_000.0D,
                latencySamples, trackedProfiles);
    }

    static long percentile(long[] sorted, double quantile) {
        if (sorted.length == 0) {
            return 0L;
        }
        int index = (int) Math.ceil(quantile * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(sorted.length - 1, index))];
    }

    static long[] sorted(long[] samples, int count) {
        long[] copy = Arrays.copyOf(samples, count);
        Arrays.sort(copy);
        return copy;
    }
}
