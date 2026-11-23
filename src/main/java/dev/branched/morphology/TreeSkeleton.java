package dev.branched.morphology;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * World-independent tree skeleton. Produced once per worldgen placement and
 * then rasterized; it is never stored in the world and owns no BlockEntity.
 */
public final class TreeSkeleton {
    private final List<Segment> segments;
    private final List<LeafCluster> leafClusters;
    private final BranchNode root;
    private final Point3 min;
    private final Point3 max;

    public TreeSkeleton(BranchNode root, List<Segment> segments, List<LeafCluster> leafClusters) {
        if (root == null) {
            throw new IllegalArgumentException("root is required");
        }
        this.root = root;
        this.segments = List.copyOf(segments);
        this.leafClusters = List.copyOf(leafClusters);
        this.min = computeMin();
        this.max = computeMax();
    }

    private Point3 computeMin() {
        double x = Double.POSITIVE_INFINITY;
        double y = Double.POSITIVE_INFINITY;
        double z = Double.POSITIVE_INFINITY;
        for (Segment s : segments) {
            x = Math.min(x, Math.min(s.start().x(), s.end().x()));
            y = Math.min(y, Math.min(s.start().y(), s.end().y()));
            z = Math.min(z, Math.min(s.start().z(), s.end().z()));
        }
        for (LeafCluster c : leafClusters) {
            x = Math.min(x, c.center().x() - c.radiusX());
            y = Math.min(y, c.center().y() - c.radiusY());
            z = Math.min(z, c.center().z() - c.radiusZ());
        }
        return new Point3(x, y, z);
    }

    private Point3 computeMax() {
        double x = Double.NEGATIVE_INFINITY;
        double y = Double.NEGATIVE_INFINITY;
        double z = Double.NEGATIVE_INFINITY;
        for (Segment s : segments) {
            x = Math.max(x, Math.max(s.start().x(), s.end().x()));
            y = Math.max(y, Math.max(s.start().y(), s.end().y()));
            z = Math.max(z, Math.max(s.start().z(), s.end().z()));
        }
        for (LeafCluster c : leafClusters) {
            x = Math.max(x, c.center().x() + c.radiusX());
            y = Math.max(y, c.center().y() + c.radiusY());
            z = Math.max(z, c.center().z() + c.radiusZ());
        }
        return new Point3(x, y, z);
    }

    public BranchNode root() { return root; }
    public List<Segment> segments() { return segments; }
    public List<LeafCluster> leafClusters() { return Collections.unmodifiableList(leafClusters); }
    public Point3 min() { return min; }
    public Point3 max() { return max; }

    public double width() { return max.x() - min.x(); }
    public double depth() { return max.z() - min.z(); }
    public double height() { return max.y() - min.y(); }

    /**
     * Quantized structural signature. Two trees with the same signature are
     * effectively the same topology; used by duplicate-detection tests.
     */
    public String topologySignature() {
        StringBuilder sb = new StringBuilder(128 + segments.size() * 40);
        for (Segment s : segments) {
            append(sb, s.start());
            sb.append('>');
            append(sb, s.end());
            sb.append('@').append(round(s.startRadius())).append('/').append(round(s.endRadius()));
            sb.append(';');
        }
        sb.append('|');
        for (LeafCluster c : leafClusters) {
            append(sb, c.center());
            sb.append('~').append(round(c.radiusX())).append(',').append(round(c.radiusY()))
                    .append(',').append(round(c.radiusZ())).append(';');
        }
        return sb.toString();
    }

    private static void append(StringBuilder sb, Point3 p) {
        sb.append(round(p.x())).append(',').append(round(p.y())).append(',').append(round(p.z()));
    }

    private static String round(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    /** Flattens the branch hierarchy into segments, useful for diagnostics. */
    public int nodeCount() {
        List<BranchNode> stack = new ArrayList<>();
        stack.add(root);
        int count = 0;
        while (!stack.isEmpty()) {
            BranchNode node = stack.remove(stack.size() - 1);
            count++;
            stack.addAll(node.children());
        }
        return count;
    }
}
