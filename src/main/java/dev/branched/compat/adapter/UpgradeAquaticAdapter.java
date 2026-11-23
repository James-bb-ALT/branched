package dev.branched.compat.adapter;

import dev.branched.compat.AbstractModAdapter;
import dev.branched.species.CompatibilityLevel;

/** Upgrade Aquatic 6.0+: replaces river biome river trees with static 3D river branches. */
public final class UpgradeAquaticAdapter extends AbstractModAdapter {
    public UpgradeAquaticAdapter() {
        super("upgrade_aquatic", "Upgrade Aquatic", "6.0.0", CompatibilityLevel.STATIC_NORMAL);
        feature("upgrade_aquatic:river_tree", "upgrade_aquatic:river", CompatibilityLevel.STATIC_NORMAL,
                "river tree replaced with static 3D river branches");
    }
}
