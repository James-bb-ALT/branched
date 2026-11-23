package dev.branched.compat.adapter;

import dev.branched.compat.AbstractModAdapter;
import dev.branched.species.CompatibilityLevel;

/** Ecologics 2.2+: replaces plains walnut trees while preserving beach coconuts and lush cave rooted azalea. */
public final class EcologicsAdapter extends AbstractModAdapter {
    public EcologicsAdapter() {
        super("ecologics", "Ecologics", "2.2.0", CompatibilityLevel.STATIC_NORMAL);
        feature("ecologics:trees_walnut", "ecologics:walnut", CompatibilityLevel.STATIC_NORMAL,
                "plains walnut tree replaced with static 3D walnut branches");
        feature("ecologics:trees_beach", "ecologics:coconut", CompatibilityLevel.SPECIAL_PRESERVE,
                "beach coconut trees grow harvestable coconut fruit pods");
        feature("ecologics:rooted_azalea_tree", "ecologics:azalea", CompatibilityLevel.SPECIAL_PRESERVE,
                "rooted dirt pillar semantics in lush caves");
    }
}
