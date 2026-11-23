package dev.branched.platform;

import java.util.Optional;
import java.util.Set;

/** Read-only view of loaded mods, abstracted for testability. */
public interface ModPresence {
    boolean isLoaded(String modId);

    Set<String> loadedMods();

    Optional<String> version(String modId);
}
