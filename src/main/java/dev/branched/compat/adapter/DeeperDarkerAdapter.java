package dev.branched.compat.adapter;

import dev.branched.compat.AbstractModAdapter;
import dev.branched.species.CompatibilityLevel;

/** Deeper and Darker 1.3+: replaces Otherside echo trees with static 3D echo branches. */
public final class DeeperDarkerAdapter extends AbstractModAdapter {
    public DeeperDarkerAdapter() {
        super("deeperdarker", "Deeper and Darker", "1.3.0", CompatibilityLevel.STATIC_NORMAL);
        feature("deeperdarker:echo_tree", "deeperdarker:echo", CompatibilityLevel.STATIC_NORMAL,
                "echoing forest echo trees");
    }
}
