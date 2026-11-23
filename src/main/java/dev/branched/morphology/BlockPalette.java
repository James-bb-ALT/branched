package dev.branched.morphology;

import dev.branched.core.ResourceId;

/**
 * Semantic role to native block mapping. The branch block is the only block
 * owned by Branched; logs, leaves and saplings are referenced from the
 * original mod so textures/assets are never copied.
 */
public record BlockPalette(ResourceId log,
                           ResourceId branch,
                           ResourceId leaves,
                           ResourceId sapling,
                           ResourceId fruit) {
    public BlockPalette {
        if (log == null || branch == null || leaves == null) {
            throw new IllegalArgumentException("log, branch and leaves are required");
        }
    }
}
