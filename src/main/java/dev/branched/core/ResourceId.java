package dev.branched.core;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * A validated {@code namespace:path} identifier that does not depend on any
 * Minecraft class. Keeping this type independent makes the morphology and
 * profile layers unit-testable without bootstrapping a Minecraft server.
 */
public record ResourceId(String namespace, String path) implements Comparable<ResourceId> {
    private static final Pattern NAMESPACE = Pattern.compile("[a-z0-9_.-]+");
    private static final Pattern PATH = Pattern.compile("[a-z0-9_./-]+");

    public ResourceId {
        Objects.requireNonNull(namespace, "namespace");
        Objects.requireNonNull(path, "path");
        if (!NAMESPACE.matcher(namespace).matches()) {
            throw new IllegalArgumentException("Invalid resource namespace: '" + namespace + "'");
        }
        if (!PATH.matcher(path).matches()) {
            throw new IllegalArgumentException("Invalid resource path: '" + path + "'");
        }
    }

    public static ResourceId parse(String value) {
        Objects.requireNonNull(value, "value");
        int colon = value.indexOf(':');
        if (colon <= 0 || colon == value.length() - 1) {
            throw new IllegalArgumentException("Expected namespace:path but got '" + value + "'");
        }
        return new ResourceId(value.substring(0, colon), value.substring(colon + 1));
    }

    public static ResourceId of(String namespace, String path) {
        return new ResourceId(namespace, path);
    }

    /** Returns {@code null} instead of throwing, for optional metadata fields. */
    public static ResourceId tryParse(String value) {
        try {
            return value == null ? null : parse(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }

    @Override
    public int compareTo(ResourceId other) {
        int byNamespace = namespace.compareTo(other.namespace);
        return byNamespace != 0 ? byNamespace : path.compareTo(other.path);
    }
}
