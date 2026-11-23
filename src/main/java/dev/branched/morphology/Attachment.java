package dev.branched.morphology;

/**
 * A non-block attachment slot (leaves, fruit, vines). It intentionally stores
 * no BlockState: the rasterizer resolves semantic roles to native mod blocks.
 */
public record Attachment(Point3 position, AttachmentType type, double scale, String tag) {
    public Attachment {
        if (position == null || type == null) {
            throw new IllegalArgumentException("Attachment position and type are required");
        }
        if (!Double.isFinite(scale) || scale <= 0) {
            throw new IllegalArgumentException("Attachment scale must be positive");
        }
    }
}
