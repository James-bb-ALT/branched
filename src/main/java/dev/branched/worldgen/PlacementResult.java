package dev.branched.worldgen;

/** Outcome of a placement attempt. Deferred means "retry later", never "damage". */
public record PlacementResult(Outcome outcome, int placedBlocks, String reason) {
    public enum Outcome {
        PLACED,
        REJECTED,
        DEFERRED
    }

    public static PlacementResult placed(int blocks) {
        return new PlacementResult(Outcome.PLACED, blocks, "");
    }

    public static PlacementResult rejected(String reason) {
        return new PlacementResult(Outcome.REJECTED, 0, reason);
    }

    public static PlacementResult deferred(String reason) {
        return new PlacementResult(Outcome.DEFERRED, 0, reason);
    }
}
