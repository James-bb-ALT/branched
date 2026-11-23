package dev.branched.compat;

/** Adapter availability, resolved independently per mod. */
public enum AdapterState {
    READY,
    ABSENT,
    UNRESOLVED,
    CONFLICT
}
