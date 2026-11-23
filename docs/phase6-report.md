# Phase 6 Report — Compatibility, Hardening, and Diagnostics

Phase 6 hardens Branched across multi-threaded chunk generation, chunk
write boundaries on giant trees, datapack reload atomicity, in-game/offline
diagnostics, Forge GameTests, and live 415-mod modpack execution.

## Implemented Components

### 1. Structured Diagnostics Exporter (`DiagnosticsExporter`)
- Class: `dev.branched.diagnostics.DiagnosticsExporter`
- Output path: `logs/branched-diagnostics.json` (`DEFAULT_RELATIVE_PATH`)
- Exports:
  - `modId`, `version` (`0.1.0`), `generatorVersion` (`2`)
  - `config` (`worldgenEnabled`, `replaceKnownFeatures`, `unknownFeaturePolicy`, `replaceExistingChunks`, `renderPreset`, `useBlockEntities`, `maxGenerationAttempts`, `specialTreesPolicy`)
  - `invariants` (`zeroBlockEntities`, `zeroPeriodicSimulation`, `noDynamicTreesDependency`, `dynamicTreesLoaded`)
  - `adapters` (`total: 8`, `readyCount`, and per-mod `state`, `minVersion`, `detectedVersion`, `replaceableCount`, `preservedCount`)
  - `profiles` (`total: 51`, `replaceable: 48`, `preserved: 3`, plus resolved `log`, `branch`, `leaves` per profile)
  - `metrics` (`generatedTrees`, `rejectedPlacements`, `PlacedCount`, `RejectedCount`, `segments`, `leafClusters`, `blocks`, `estimatedQuads`, `avgMs`, `maxMs`, `p50Ms`, `p95Ms`, `p99Ms`, `latencySamples`, `trackedProfiles`, `rejectionsByReason`)
- Automatically invoked at the end of `WorldgenSelfTest` (`SELFTEST exported diagnostics to .\logs\branched-diagnostics.json`).

### 2. Operator Command (`/branched`)
- Class: `dev.branched.diagnostics.ArborealCommands`
- Registered via `MinecraftForge.EVENT_BUS.addListener(ArborealCommands::onRegisterCommands)` in `Branched`.
- Requires permission level `2` (`Commands.literal("branched").requires(source -> source.hasPermission(2))`):
  - `/branched status` — reports `WorldgenGate.enabled()`, `PerformancePreset`, loaded/replaceable profile counts, `AdapterRegistry` `READY` count and per-adapter states, feature `placed`/`rejected` counters, and current `GenerationMetrics`.
  - `/branched dump` — calls `DiagnosticsExporter.exportDefault(source.getServer().getServerDirectory().toPath())` and prints the written path.

### 3. Self-Test Chunkgen Timing & Step Control (`WorldgenSelfTest`)
- Class: `dev.branched.diagnostics.WorldgenSelfTest`
- Fixed cold-chunkgen latency measurement by starting `chunkStart = System.nanoTime()` **before** `level.setChunkForced(...)`.
- Added `-Dbranched.selftest.step=<N>` (`STEP_PROPERTY`, default `8`) passed to the 9-arg `BiomeSource.findBiomeHorizontal(...)` so targeted dimension checks can sample dense modded worlds quickly.
- Added `aether_skyroot` target and exported `logs/branched-diagnostics.json` on completion.

### 4. Forge GameTests (`ArborealGameTests`)
- Class: `dev.branched.gametest.ArborealGameTests` (`@GameTestHolder("branched")`, `@PrefixGameTestTemplate(false)`)
- Template resource: [`src/main/resources/data/branched/structures/empty.nbt`](file:///E:/Pycharm%20Projects/Branched/branched/src/main/resources/data/branched/structures/empty.nbt) (GZIP-compressed 5×5×5 `minecraft:air` structure, `DataVersion = 3465`, verified byte-roundtrip in `Phase7MigrationTest`).
- Four `@GameTest(template = "empty", timeoutTicks = 40)` methods:
  1. `deterministicTreeTopology`
  2. `profilesAndAdaptersLoaded`
  3. `branchBlocksHaveNoBlockEntity`
  4. `diagnosticsExportWritesValidJson`

### 5. Datapack Reload Safety (`ProfileReloadListener`)
- Reads JSON profiles off-thread in preparation and swaps `SpeciesRegistry` in the apply stage via `registry.replaceAll(...)`.
- Rejecting reloads that produce `0` valid profiles alongside `errorCount > 0` leaves the previous registry intact so broken overrides cannot wipe worldgen.

## Verification Results

### Unit & Hardening Suite (`Phase6HardeningTest`, 4/4 PASSED)
Recorded in [`docs/test-logs/test-24-final.log`](file:///E:/Pycharm%20Projects/Branched/branched/docs/test-logs/test-24-final.log):
- `giantTreesRespectChunkWriteBoundariesAndLeafSupport`: tested `regions_unexplored:redwood`, `regions_unexplored:kapok`, and `regions_unexplored:eucalyptus` near the edge `[46, 64, 46]` of a `[0..47]` write window (`outside_write_region` cleanly rejects out-of-bounds placement), at the center `[128, 64, 128]` (`PLACED`), with all leaf BFS distances in `1..6`, and hard `blocked` rejection on trunk obstruction.
- `parallelWorldgenRemainsDeterministicAcrossThreadsAndCallOrder`: ran 60 trees across `minecraft:oak`, `spruce`, `acacia`, `regions_unexplored:maple`, and `redwood` across an 8-thread executor in shuffled order; bit-identical `CanonicalShape.exact` match on all jobs.
- `datapackReloadSwapsAtomicallyAndPreservesRegistryOnTotalFailure`: verified valid override swap (`oak` trunk min height updated to `9.0`) followed by total-failure rejection leaving all `51` profiles and overridden `oak` intact.
- `diagnosticsExportCapturesModMatrixAndDynamicTreesWarning`: simulated all 8 adapters including loaded `dynamictrees` `1.4.11` and verified `readyCount == 7`, `dtbaobabtree` non-`READY`, `zeroBlockEntities == true`, `dynamicTreesLoaded == true`, and `PlacedCount == 42`.

### Production Servers & Live 415-Mod Client Modpack
- [`docs/test-logs/server-21-phase3-selftest.log`](file:///E:/Pycharm%20Projects/Branched/branched/docs/test-logs/server-21-phase3-selftest.log) / [`docs/test-logs/server-21-diagnostics.json`](file:///E:/Pycharm%20Projects/Branched/branched/docs/test-logs/server-21-diagnostics.json): Forge 47.4.29 + Regions Unexplored `0.5.6` + TerraBlender `3.0.1.10` + Terralith `2.5.4`; `outside_write_region=1` caught safely, `avgMs=0.072`, `p99Ms=0.12`.
- [`docs/test-logs/server-23-phase4-twilight.log`](file:///E:/Pycharm%20Projects/Branched/branched/docs/test-logs/server-23-phase4-twilight.log) / [`docs/test-logs/server-23-diagnostics.json`](file:///E:/Pycharm%20Projects/Branched/branched/docs/test-logs/server-23-diagnostics.json): Twilight Forest `4.3.2508` + Quark `4.0-462` + Zeta `1.0-31` + Fish of Thieves `3.1.7`; `0` feature placements/rejections inside `twilightforest:twilight_forest`.
- [`docs/test-logs/modpack-client-26-branched.log`](file:///E:/Pycharm%20Projects/Branched/branched/docs/test-logs/modpack-client-26-branched.log): live client launch in `Optimized fps` (`Forge 1.20.1-47.4.28`, `415` active JARs, `0` duplicate modIds), including C2ME parallel workers (`c2me-worker-*`), Distant Horizons, Embeddium/Iris shaders ecosystem, and **all 7 integrated mods simultaneously `READY`** (`dtbaobabtree` `ABSENT`), generating `901` trees (`582` placed, `319` softly rejected as `blocked`, `0` Branched errors/warnings, `PostProcessing warns=0`).

### Verification Scope Notes
- `ArborealGameTests` compiled cleanly inside `branched-0.1.0.jar` and its `empty.nbt` asset and underlying assertions are covered in JUnit (`Phase7MigrationTest` + `Phase6HardeningTest`), while `gradlew runGameTestServer` was not executed as a standalone daemon process.
- Interactive operator execution of `/reload` and `/branched` in chat was not manually invoked; `ProfileReloadListener` executed automatically on server/client startup and atomic reload/rollback was verified in `Phase6HardeningTest`, while `DiagnosticsExporter.exportDefault` (the `/branched dump` backend) executed during dedicated-server selftest runs.
