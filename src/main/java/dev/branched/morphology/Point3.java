package dev.branched.morphology;

/** World-independent 3D point used by the morphology skeleton. */
public record Point3(double x, double y, double z) {
    public static final Point3 ORIGIN = new Point3(0, 0, 0);

    public Point3 add(double dx, double dy, double dz) {
        return new Point3(x + dx, y + dy, z + dz);
    }

    public Point3 add(Point3 other) {
        return new Point3(x + other.x, y + other.y, z + other.z);
    }

    public Point3 scale(double factor) {
        return new Point3(x * factor, y * factor, z * factor);
    }

    public double distanceTo(Point3 other) {
        double dx = x - other.x;
        double dy = y - other.y;
        double dz = z - other.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public Point3 lerp(Point3 other, double t) {
        return new Point3(
                x + (other.x - x) * t,
                y + (other.y - y) * t,
                z + (other.z - z) * t);
    }
}
