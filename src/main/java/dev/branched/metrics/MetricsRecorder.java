package dev.branched.metrics;

import dev.branched.morphology.RasterizedTree;
import dev.branched.morphology.TreeSkeleton;
import dev.branched.species.SpeciesProfile;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

/**
 * Lightweight, allocation-conscious counters. Disabled instances are no-ops so
 * production can turn metrics off without branching at every call site.
 */
public final class MetricsRecorder {
    public static final MetricsRecorder NOOP = new MetricsRecorder(false);

    private final boolean enabled;
    private final LongAdder generatedTrees = new LongAdder();
    private final LongAdder rejectedPlacements = new LongAdder();
    private final LongAdder segments = new LongAdder();
    private final LongAdder leafClusters = new LongAdder();
    private final LongAdder blocks = new LongAdder();
    private final LongAdder estimatedQuads = new LongAdder();
    private final LongAdder totalNanos = new LongAdder();
    private final AtomicLong maxNanos = new AtomicLong();
    /** Ring buffer of per-attempt latencies; bounded so long sessions stay cheap. */
    private static final int LATENCY_CAPACITY = 4096;
    private final long[] latencySamples = new long[LATENCY_CAPACITY];
    private final AtomicLong latencyCount = new AtomicLong();
    private final Map<String, LongAdder> perProfile = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> rejectionReasons = new ConcurrentHashMap<>();

    public MetricsRecorder(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean enabled() {
        return enabled;
    }

    public void recordGenerated(SpeciesProfile profile, TreeSkeleton skeleton, long elapsedNanos) {
        if (!enabled) {
            return;
        }
        generatedTrees.increment();
        segments.add(skeleton.segments().size());
        leafClusters.add(skeleton.leafClusters().size());
        totalNanos.add(Math.max(0L, elapsedNanos));
        maxNanos.accumulateAndGet(Math.max(0L, elapsedNanos), Math::max);
        perProfile.computeIfAbsent(profile.id().toString(), key -> new LongAdder()).increment();
    }

    public void recordRasterized(SpeciesProfile profile, RasterizedTree tree, long elapsedNanos) {
        if (!enabled) {
            return;
        }
        blocks.add(tree.totalBlocks());
        estimatedQuads.add(tree.estimatedQuads());
        totalNanos.add(Math.max(0L, elapsedNanos));
        maxNanos.accumulateAndGet(Math.max(0L, elapsedNanos), Math::max);
    }

    /**
     * Records a full per-tree attempt (topology + rasterization), used for the
     * latency percentiles. Safe to call from parallel worldgen threads.
     */
    public void recordAttempt(long elapsedNanos) {
        if (!enabled) {
            return;
        }
        long value = Math.max(0L, elapsedNanos);
        long index = latencyCount.getAndIncrement();
        latencySamples[(int) (index % LATENCY_CAPACITY)] = value;
    }

    public void recordRejection(String reason) {
        if (!enabled) {
            return;
        }
        rejectedPlacements.increment();
        rejectionReasons.computeIfAbsent(reason == null ? "unknown" : reason,
                key -> new LongAdder()).increment();
    }

    /** Rejections grouped by reason, ordered for stable log output. */
    public Map<String, Long> rejectionsByReason() {
        Map<String, Long> snapshot = new java.util.TreeMap<>();
        if (enabled) {
            rejectionReasons.forEach((reason, count) -> snapshot.put(reason, count.sum()));
        }
        return snapshot;
    }

    public GenerationMetrics snapshot() {
        if (!enabled) {
            return new GenerationMetrics(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        }
        int count = (int) Math.min(latencyCount.get(), LATENCY_CAPACITY);
        long[] sorted = GenerationMetrics.sorted(latencySamples, count);
        return new GenerationMetrics(
                generatedTrees.sum(),
                rejectedPlacements.sum(),
                segments.sum(),
                leafClusters.sum(),
                blocks.sum(),
                estimatedQuads.sum(),
                totalNanos.sum(),
                maxNanos.get(),
                perProfile.size(),
                GenerationMetrics.percentile(sorted, 0.50D),
                GenerationMetrics.percentile(sorted, 0.95D),
                GenerationMetrics.percentile(sorted, 0.99D),
                count);
    }

    public void reset() {
        generatedTrees.reset();
        rejectedPlacements.reset();
        segments.reset();
        leafClusters.reset();
        blocks.reset();
        estimatedQuads.reset();
        totalNanos.reset();
        maxNanos.set(0L);
        latencyCount.set(0L);
        java.util.Arrays.fill(latencySamples, 0L);
        perProfile.clear();
        rejectionReasons.clear();
    }
}
