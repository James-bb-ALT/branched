package dev.branched.morphology;

/**
 * One straight limb between two positions. Radii are quantized by the
 * rasterizer into a small shared set of branch shapes.
 */
public record Segment(Point3 start, Point3 end, double startRadius, double endRadius, int depth) {
    public Segment {
        if (start == null || end == null) {
            throw new IllegalArgumentException("Segment endpoints are required");
        }
        if (startRadius < 0 || endRadius < 0) {
            throw new IllegalArgumentException("Segment radii must be non-negative");
        }
        if (depth < 0) {
            throw new IllegalArgumentException("Segment depth must be non-negative");
        }
    }

    public double length() {
        return start.distanceTo(end);
    }
}
