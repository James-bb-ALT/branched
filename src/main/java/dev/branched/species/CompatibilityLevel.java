package dev.branched.species;

/**
 * Phase 0 compatibility classification. These four values are part of the
 * project contract and must not be reordered or renamed.
 */
public enum CompatibilityLevel {
    /** Safe static replacement: native wood, leaves, loot and sapling preserved. */
    STATIC_NORMAL,
    /** Replacement must preserve fruits/pods and their harvest behaviour. */
    STATIC_FRUIT,
    /** Original implementation must be preserved (functional or magic tree). */
    SPECIAL_PRESERVE,
    /** Not enough evidence to replace safely. */
    UNRESOLVED;

    public boolean mayReplace() {
        return this == STATIC_NORMAL || this == STATIC_FRUIT;
    }
}
