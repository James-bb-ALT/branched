package dev.branched.compat.adapter;

import dev.branched.compat.AbstractModAdapter;
import dev.branched.species.CompatibilityLevel;

/**
 * Dynamic Trees Baobab 0.1.5 is a development reference, not an original mod.
 * The JAR was recovered locally (SHA-256 recorded in catalogs/jar-inventory.csv)
 * and its four species are catalogued, but there is no original Baobab mod to
 * audit and no verified public download URL. This adapter is intentionally
 * UNRESOLVED and is never required at runtime.
 */
public final class BaobabAdapter extends AbstractModAdapter {
    public BaobabAdapter() {
        super("dtbaobabtree", "Dynamic Trees Baobab (reference only)", "0.1.5", CompatibilityLevel.UNRESOLVED);
    }

    @Override
    public boolean present(dev.branched.platform.ModPresence presence) {
        // Never activate a replacement based on a development-only reference JAR.
        return false;
    }
}
