# Branched Workspace Rules & Steering

1. **Automatic Version Bump on Every Change**:
   - Every modification to code, assets, worldgen rules, or behavior MUST increment the mod version in BOTH [gradle.properties](gradle.properties) (`mod_version=...`) and [Branched.java](src/main/java/dev/branched/Branched.java) (`DEFAULT_VERSION = "..."`).
   - Use a **patch bump** (`0.x.Y`) for bug fixes, geometry/visual tweaks, or small adjustments.
   - Use a **minor bump** (`0.X.0`) for new features, new mod integrations, or new branch blocks.
   - Keep `Phase6HardeningTest` green (`gradle.properties` `mod_version` and `Branched.DEFAULT_VERSION` must always match).

2. **Modrinth Deployment Hygiene**:
   - When deploying a newly built JAR to `C:\Users\victo\AppData\Roaming\ModrinthApp\profiles\Optimized fps\mods\`, always delete any older `branched-*.jar` versions in that directory so only the latest version remains.

3. **Build & Wrapper Invariants**:
   - Keep `gradle/wrapper/gradle-wrapper.properties` pinned to `gradle-8.1.1-bin.zip` (ForgeGradle `[6.0,6.2)` does not support Gradle 9+). If an IDE extension bumps it, restore it before running Gradle or committing.
   - Uphold all `StaticInvariantsTest` rules in `src/main/java`: zero `BlockEntity`, zero `BlockEntityType`, zero `randomTick`, zero `scheduleTick`, zero `setBlockEntity`, and never consume `context.random()` / `ctx.random()`.
   - Never reintroduce any reference to the former project name.
