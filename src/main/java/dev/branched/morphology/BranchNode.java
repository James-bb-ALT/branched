package dev.branched.morphology;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Abstract branch hierarchy node. This is the topology representation that the
 * Phase 1 generator will grow; the Phase 0 preview already emits a real tree of
 * nodes so downstream rasterization can be developed against it.
 */
public final class BranchNode {
    private final Point3 position;
    private final double radius;
    private final int depth;
    private final List<BranchNode> children = new ArrayList<>();
    private final List<Attachment> attachments = new ArrayList<>();

    public BranchNode(Point3 position, double radius, int depth) {
        if (position == null) {
            throw new IllegalArgumentException("position is required");
        }
        if (radius < 0) {
            throw new IllegalArgumentException("radius must be non-negative");
        }
        if (depth < 0) {
            throw new IllegalArgumentException("depth must be non-negative");
        }
        this.position = position;
        this.radius = radius;
        this.depth = depth;
    }

    public Point3 position() { return position; }
    public double radius() { return radius; }
    public int depth() { return depth; }

    public BranchNode addChild(BranchNode child) {
        children.add(child);
        return child;
    }

    public void addAttachment(Attachment attachment) {
        attachments.add(attachment);
    }

    public List<BranchNode> children() {
        return Collections.unmodifiableList(children);
    }

    public List<Attachment> attachments() {
        return Collections.unmodifiableList(attachments);
    }
}
