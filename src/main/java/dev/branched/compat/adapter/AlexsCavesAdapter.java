package dev.branched.compat.adapter;

import dev.branched.compat.AbstractModAdapter;
import dev.branched.species.CompatibilityLevel;

/**
 * Alex's Caves 2.0+: preserves primordial cave custom structures while
 * providing branch materials and species metadata for pewen and thornwood.
 */
public final class AlexsCavesAdapter extends AbstractModAdapter {
    public AlexsCavesAdapter() {
        super("alexscaves", "Alex's Caves", "2.0.0", CompatibilityLevel.SPECIAL_PRESERVE);
        feature("alexscaves:pewen_tree", "alexscaves:pewen", CompatibilityLevel.SPECIAL_PRESERVE,
                "uses native pewen_branch blocks inside subterranean cave biomes");
        feature("alexscaves:thornwood_tree", "alexscaves:thornwood", CompatibilityLevel.SPECIAL_PRESERVE,
                "uses native thornwood_branch blocks and cave ceiling thorn mechanics");
    }
}
