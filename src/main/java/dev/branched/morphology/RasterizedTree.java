package dev.branched.morphology;

import java.util.List;

/** Result of converting a skeleton into discrete block placements. */
public record RasterizedTree(List<BlockPlacement> blocks,
                             int logBlocks,
                             int leafBlocks,
                             int estimatedQuads,
                             boolean budgetTruncated) {
    public RasterizedTree {
        blocks = List.copyOf(blocks);
        if (logBlocks < 0 || leafBlocks < 0 || estimatedQuads < 0) {
            throw new IllegalArgumentException("counters must be non-negative");
        }
    }

    public int totalBlocks() {
        return blocks.size();
    }
}
