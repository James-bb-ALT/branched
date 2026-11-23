package dev.branched.platform;

import net.minecraftforge.fml.ModList;

import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/** Forge implementation. Never throws when a mod is absent. */
public final class ForgeModPresence implements ModPresence {
    @Override
    public boolean isLoaded(String modId) {
        return modId != null && ModList.get().isLoaded(modId);
    }

    @Override
    public Set<String> loadedMods() {
        Set<String> ids = new LinkedHashSet<>();
        ModList.get().getMods().forEach(info -> ids.add(info.getModId()));
        return Set.copyOf(ids);
    }

    @Override
    public Optional<String> version(String modId) {
        return ModList.get().getMods().stream()
                .filter(info -> info.getModId().equals(modId))
                .findFirst()
                .map(info -> info.getVersion().toString());
    }
}
