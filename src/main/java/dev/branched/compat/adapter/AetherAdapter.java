package dev.branched.compat.adapter;

import dev.branched.compat.AbstractModAdapter;
import dev.branched.species.CompatibilityLevel;

/**
 * The Aether 1.20.1-1.5.2. The distributed file is named "neoforge" but its
 * mods.toml declares javafml and forge [47.1.0,), so it loads on Forge 47.x.
 */
public final class AetherAdapter extends AbstractModAdapter {
    public AetherAdapter() {
        super("aether", "The Aether", "1.5.2", CompatibilityLevel.STATIC_NORMAL);

        feature("aether:skyroot_forest_trees", "aether:skyroot", CompatibilityLevel.STATIC_NORMAL);
        feature("aether:skyroot_woodland_trees", "aether:skyroot", CompatibilityLevel.STATIC_NORMAL);
        feature("aether:skyroot_grove_trees", "aether:skyroot", CompatibilityLevel.STATIC_NORMAL);
        feature("aether:skyroot_meadow_trees", "aether:skyroot", CompatibilityLevel.STATIC_NORMAL);
        feature("aether:gold_dungeon_island_foliage", "aether:golden_oak", CompatibilityLevel.STATIC_FRUIT,
                "golden oak dungeon island foliage; golden apples preserved");
        feature("aether:holiday_tree", "aether:holiday", CompatibilityLevel.SPECIAL_PRESERVE,
                "seasonal decorative tree with custom decorators");
    }
}
