# Phase 1 backlog

Phase 0 stopped at "profiles + skeleton + guard". Phase 1 adds a working
worldgen pilot; the items below are tracked against that. Status markers are
honest: **done** means code exists *and* the evidence listed in
`docs/test-logs/README.md` was executed.

> **Phases 2–7 update.** Development has progressed through Phase 7:
> - **Phase 2–4**: 78 tree features replaced (`8` vanilla aggregates / 12 biomes + `61` Regions Unexplored + `9` Terralith); `250` audited features preserved (`13` vanilla + `200` Phase 3 + `37` Phase 4 across Aether, Fish of Thieves, Quark, Twilight Forest).
> - **Phase 5**: `Phase5DiversityTest` + `gradlew phase5Report` (`0.000` duplicates across `64` samples × `51` profiles) and `PerformancePreset` (`PERFORMANCE`, `BALANCED`, `VISUAL`).
> - **Phase 6**: `DiagnosticsExporter`, `/branched status|dump`, `ArborealGameTests` + `empty.nbt`, `Phase6HardeningTest`, plus production dedicated-server proofs (`server-20`..`server-23`) and live 415-mod client verification (`modpack-client-26-branched.log`, `modpack-bench-26-branched.json`).
> - **Phase 7**: Opt-in offline Dynamic Trees migration tool (`WorldMigrationTool`, `NbtIo`, `AnvilRegionFile`, `DynamicTreesBlockMapper`), `Phase7MigrationTest`, and verified `--dry-run` / `--apply` / `--rollback` round-trip against `New World (27)`.
>
> The sections below remain preserved as historical Phase 1 tracking alongside status annotations where Phase 5–7 items resolved them. See `docs/phase5-report.md`, `docs/phase6-report.md`, `docs/phase7-report.md`, and `docs/benchmark-report.md`.

## Phase 1 pilot — done

| Item | Evidence |
| --- | --- |
| BiomeModifier replacement (`forge:remove_features` / `forge:add_features` semantics, no Mixin) | `StaticTreeBiomeModifier`, `StaticTreeFeature`, `data/branched/forge/biome_modifier/vanilla_trees_*.json`; dedicated-server run `server-15` |
| World writer | `WorldGenTreeWriter` (trunk, branches, leaves, rollback, `WorldGenLevel.canWrite` guard); `TreeFitValidator` |
| Cross-chunk safety | every position checked with `canWrite`; no "setBlock in a far chunk" in `server-09`/`server-10` |
| No BlockEntity, no growth tick | `StaticInvariantsTest`; branch block has no entity type |
| Leaf distance | `LeafDistance` multi-source BFS, clamp 1..6; `LeafDistanceTest` |
| Branch blocks | `StaticBranchBlock` with 3 shapes × 6 facings, cached `VoxelShape`, oak + spruce + acacia variants, `#minecraft:logs` + `#minecraft:mineable/axe` |
| Deterministic generator | unchanged seeding contract; `MorphologyDeterminismTest` |
| Config gate | `WorldgenGate` + `morphology.max_generation_attempts`; `WorldgenGateTest`, `StaticTreeConfigurationTest` |
| Server proof | `server-09` (vanilla baseline) vs `server-10` (static pilot), same seed and chunks |

### Measured result (3×3 chunks, seed `123456789`)

| Metric | Vanilla | Static pilot |
| --- | ---: | ---: |
| trees | 60 | 83 |
| logs | 406 | 565 |
| leaves | 3357 | 3490 |
| logs / tree | 6.8 | 6.81 |
| leaves / tree | 55.9 | 42.0 |
| leaves / log | 8.3 | 6.2 |
| branch blocks | 0 | 395 |

### Known losses in the pilot

- **Pine silhouette.** Vanilla `trees_taiga` is a `random_selector`: 2/3
  `spruce_checked`, 1/3 `pine_checked`. The pilot replaces the whole placed
  feature with a spruce-only static feature, so the narrow pine shape is gone.
  Blocks are unaffected: `minecraft:pine` also places spruce logs/leaves in
  1.20.1.
- **Density.** Trees and wood per chunk are ~1.38× vanilla because the same
  weighted count is applied while the pine share also becomes a full spruce.
  Leaves per tree are lower (42 vs 56) because static leaves must be supported
  within distance 6. Parity work is Phase 5.
- **Leaf decay margin.** Vanilla writes some leaves at distance 7 and lets them
  decay; the writer omits them up front, which is why `leaves/log` is 6.2
  instead of 8.3.
- **Branch visuals.** Branches are exposed as real blocks with three shapes.
  They read correctly up close but have not been visually reviewed in a client
  (pending item 17).

## P0 — required before wider replacement

1. **More vanilla mappings — Phase 2 done for eight aggregates.** Wired:
   `trees_taiga`, `trees_snowy`, `trees_grove`, `trees_savanna`,
   `trees_windswept_savanna`, `trees_windswept_forest`, `trees_windswept_hills`
   and `trees_badlands`. Still native: the thirteen aggregates in
   `docs/compatibility-matrix.md`, each blocked by a functional decorator
   (beehives, cocoa, vines, podzol, water semantics).
2. **Sapling / loot preservation.**
   - Sapling must stay native. Decide between `leaves` decay drop and an
     explicit `sapling` placement near the trunk.
   - Verify loot tables are untouched: the tree uses the original log/leaves
     blocks, so vanilla drops should apply, but this must be proven per species
     in a live world (resource presence alone is not proof).
3. **Fruit preservation (`STATIC_FRUIT`).**
   Fruits/pods are decorators in the original features. A static tree must place
   them explicitly. Affected: Aether golden oak, Fish of Thieves banana /
   coconut / mango, RU apple oak. Do not enable these species until verified.
4. **Adapter activation is still unit-tested only.** `AdapterRegistry` sees a
   fake `ModPresence`; no real optional jar has been loaded in a server.

## P1 — morphology quality

5. **Full branch grammar.** Replace the reference generator with the
   species-driven grammar (per-archetype internodes, apical dominance, branch
   shedding, competition for light). Bump `VERSION` and profile
   `generation.version` together.
6. **Leaf-distance correctness — done for the pilot**, including support on
   branches, not only the trunk. Still to do: a per-species `distance` policy
   (some species may want persistent leaves).
7. **Crown shape library — done in Phase 2.** Archetype crowns: layered cone
   (conifer), stacked narrow column (pine/cypress), flat umbrella (acacia),
   narrow ellipsoid (birch), round ellipsoid (broadleaf) and irregular ellipsoid
   (oak). `MorphologyDiversityTest` guards the silhouettes. Remaining: branch
   shedding and light competition.
8. **Budgets measured, not guessed.** The rasterizer estimates quads as
   `blocks × 6`; validate against real chunk mesh sizes.
9. **Branch block — done** for oak, spruce and acacia. Other families fall back
   to the native log until a branch texture/material exists.

## P2 — coverage

10. **Terralith profiles.** 342 tree features, 10 vanilla families, no wood of
    its own. Needs shape profiles keyed by feature, not by block.
11. **RU families without placed features.** `blue_bioshroom`,
    `green_bioshroom`, `pink_bioshroom`, `yellow_bioshroom`, `brimwood`,
    `cobalt`, `mushroom` — determine whether they are decoration-only or
    mis-detected before adding anything.
12. **Quark ancient.** Wood and sapling exist but no placed feature; confirm how
    it generates.
13. **Fish of Thieves 3.4.0 support.** Feature counts differ from 3.1.7; add
    version-aware mappings instead of assuming ids are stable.
14. **Twilight selectors.** `tree/selector/*` are random selectors; resolve them
    into per-family replacement sets.

## P3 — runtime proof (updated through Phase 7)

15. **GameTests** (`ArborealGameTests` + `empty.nbt` added in Phase 6):
    - deterministic topology (`deterministicTreeTopology`) — compiled + mirrored in JUnit; standalone `gradlew runGameTestServer` not invoked separately,
    - profile/adapter contracts (`profilesAndAdaptersLoaded`),
    - zero `BlockEntity` (`branchBlocksHaveNoBlockEntity`),
    - diagnostics export (`diagnosticsExportWritesValidJson`).
16. **Dedicated server & live modpack with all optional mods — done**:
    - Phase 3 (`server-20`, `server-21`), Phase 4 (`server-22`, `server-23`), and live 415-mod Forge 47.4.28 client `Optimized fps` (`modpack-client-26-branched.log`, all 7 active mods `READY`, without Dynamic Trees).
17. **Client launch — done in live modpack (`modpack-client-26-branched.log`)** (`901` trees generated via `c2me-worker-*`, `0` errors, `PostProcessing warns=0`).
18. **Datapack reload — done at unit/integration level (`Phase6HardeningTest.datapackReloadSwapsAtomicallyAndPreservesRegistryOnTotalFailure`)**; `ProfileReloadListener` also loads all 51 profiles on server/client startup; manual chat `/reload` remains unexecuted.
19. **Performance benchmark — done (`docs/benchmark-report.md`, `modpack-bench-26-branched.json`, `docs/phase5-diversity.json`)**.

## P4 — tooling (done in Phase 6–7)

20. **Migration tool — done (`WorldMigrationTool`, `NbtIo`, `AnvilRegionFile`, `DynamicTreesBlockMapper`, `tools/migrate_dt_world.py`)** with `--dry-run`, `--apply` (mandatory SHA-256 backup), `--rollback`, and verified real-save round-trip (`New World (27)`).
21. **Config / status command — done (`ArborealCommands`: `/branched status` and `/branched dump`)**.
22. **Diagnostics export — done (`DiagnosticsExporter` writing `logs/branched-diagnostics.json`, exercised in `server-21`, `server-23`, and `Phase6HardeningTest`)**.

## Known technical debt

- `ReferenceBlockRasterizer` allocates a `HashSet<Long>` per tree; fine for
  tests, needs an int-packed set for worldgen.
- Branch thickness currently switches at radius 1.05; needs proper radius →
  shape quantization.
- `pointAlongTrunk` rounds to whole blocks, so branch attachment is coarse.
- Metrics per-profile map grows with profile count and is never trimmed.
- `mods.toml` uses placeholder expansion; the built jar must be checked to
  confirm all `${...}` values were substituted.
- The selftest counts trees inside 3×3 chunks by flood-filling logs, while the
  feature counters (`feature_placements` / `feature_rejections`) cover the whole
  run. The two numbers measure different scopes and must not be compared.
