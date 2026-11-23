package dev.branched.morphology;

/** Terminal foliage cluster, expressed as an ellipsoid before rasterization. */
public record LeafCluster(Point3 center, double radiusX, double radiusY, double radiusZ, int weight) {
    public LeafCluster {
        if (center == null) {
            throw new IllegalArgumentException("center is required");
        }
        if (radiusX <= 0 || radiusY <= 0 || radiusZ <= 0) {
            throw new IllegalArgumentException("leaf cluster radii must be positive");
        }
        if (weight <= 0) {
            throw new IllegalArgumentException("leaf cluster weight must be positive");
        }
    }
}
