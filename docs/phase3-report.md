# Phase 3 — Regions Unexplored and Terralith coverage report

Phase 3 extends the biome-level replacement proven in Phase 2 to the two large
biome mods. The goal is narrow and testable:

- replace audited tree placed features with deterministic static mirrors;
- never delete a feature that also generates vegetation, decoration or terrain;
- never generate two tree features where the original generated one;
- never hang worldgen when an optional mod is absent.

Everything that does not pass every rule stays native. The decision for every
reference feature is recorded in `catalogs/phase3-replacements.csv` (replaced)
and `catalogs/phase3-preserved.csv` (preserved, with a machine-readable reason).
`docs/phase3-audit.json` is the same audit as JSON.

## Audit method

`tools/generate_modded_phase3.py` reads the two reference JARs (never committed)
and follows every placed feature that a biome references. Selectors
(`random_selector`, `simple_random_selector`, `random_boolean_selector`) are
expanded recursively so a feature is judged by the trees it can actually place,
not by its top-level id. A placed feature is replaced only when **all** of these
hold:

1. every reachable configured node is a plain tree with a simple `trunk_provider`
   and a simple non-air `foliage_provider` (weighted providers are rejected);
2. no decorator (`beehive`, `cocoa`, `vines`, `alter_ground`, ...) and no
   `force_dirt`;
3. the log/leaves pair has a shipped species profile whose compatibility level
   is `STATIC_NORMAL` or `STATIC_FRUIT`;
4. the placed feature is used in exactly one generation step, and that step is
   `vegetal_decoration` (step 9);
5. every placement modifier is vanilla, so the generated placed feature still
   loads when the biome mod is absent;
6. every linked biome belongs to the mod namespace, so the generated biome tag
   is empty (rather than foreign) when the mod is absent.

The generated graph is one modifier per replaced placed feature. Modifiers are
never merged and counts are never summed, so removing one feature can never
remove another. Each modifier points at its own generated tags
(`#branched:phase3/<namespace>/<path>`) for both the biomes it applies to
and the one feature it removes, adds one static placed feature and removes
exactly that one audited id. Removal runs in `REMOVE`, addition in `ADD`, both
through `branched:replace_trees` (a Forge `BiomeModifier` equivalent to the
plan's `forge:remove_features` + `forge:add_features`).

## Result

| Mod | Audited placed features | Replaced | Preserved | Not trees |
| --- | ---: | ---: | ---: | ---: |
| Regions Unexplored 0.5.6 | 303 | 61 | 102 | 140 |
| Terralith 2.5.4 | 404 | 9 | 98 | 297 |
| **Total** | **707** | **70** | **200** | **437** |

"Audited" counts the mod's own placed features that at least one biome
references. The same biome lists also reference 89 vanilla ids (Regions
Unexplored) and 101 vanilla ids (Terralith); those belong to the vanilla
catalog, not to Phase 3, and stay untouched here. "Not trees" are owned
features with no resolved tree node (ore/vegetation/patch features) and are out
of scope. Replaced and preserved are disjoint by construction and by
`Phase3ResourceTest`.

Preserved reason counts (a feature can have more than one reason):

| Reason | Regions Unexplored | Terralith |
| --- | ---: | ---: |
| `shrub_silhouette` | 41 | 35 |
| `decorators:*` | 10 | 30 |
| `force_dirt` | — | 35 |
| `unresolved_foliage` | 5 | 17 |
| `fungus_feature` | 14 | 7 |
| `unsupported_type:*` | — | 18 |
| `foreign_biome:*` | — | 12 |
| `landmark_scale_feature` | 8 | 2 |
| `fallen_log_feature` | 8 | 1 |
| `missing_profile:*` | — | 9 |
| `non_vegetal_step:*` | 3 | 4 |
| `joshua_shrub_feature` | 5 | — |
| `socotra_landmark` | 4 | — |
| `unresolved_trunk` | 3 | — |
| `cactus_feature` | 3 | 1 |
| `bamboo_feature` | 2 | 1 |
| `placement_block_state:*` | 2 | — |
| `profile_not_replaceable:*` | 1 | — |

The `placement_block_state` cases (`palm_tree_on_sand`, `cobalt_tree`) use a
`would_survive` predicate on a Regions Unexplored block state. A block state is
a direct block reference, not a tag, so it cannot be mirrored without the mod;
they stay native.

The `foreign_biome` cases are Terralith features that Terralith links to
`minecraft:*` biomes (for example `terralith:dark/trees` in
`minecraft:dark_forest`). Replacing them would double-generate trees when
Terralith is present and would leave a stale static feature when it is absent,
so they stay native. This is the single largest reason Terralith has a lower
replacement count than its block families would suggest.

## Generated resources

`tools/generate_modded_phase3.py` is deterministic; rerunning it reproduces the
same files. Output:

| Artifact | Count |
| --- | ---: |
| Biome modifiers (`forge/biome_modifier/modded_*.json`) | 70 |
| Biome tags (`tags/worldgen/biome/phase3/**`) | 70 |
| Placed-feature tags (`tags/worldgen/placed_feature/phase3/**`) | 70 |
| Block tags (`tags/blocks/phase3/**`) | 14 |
| Configured features (`worldgen/configured_feature/phase3/**`) | 56 |
| — of which `branched:static_tree` | 55 |
| — of which `minecraft:random_selector` | 1 |
| Placed features (`worldgen/placed_feature/phase3/**`) | 73 |

There are more placed features than modifiers because selectors resolve to
several leaf placed features (for example the Scott's pine variants share one
configured tree). Ids and files live under `branched:phase3/<mod>/<path>`
so a generated file can never collide with a Phase 2 file or with the original
feature id.

### Why the generated files use tags

A generated datapack file is parsed by every client and server, whether or not
the biome mod is installed. Two things are resolved while the datapack loads and
would abort loading when the mod is absent:

- a `HolderSet` written as an **inline list** binds each id immediately, so
  `replace: ["regions_unexplored:maple_tree_on_grass"]` fails with "Unbound
  values in registry placed_feature";
- a block id inside a `matching_blocks` predicate is looked up in the block
  registry, so a foreign id fails with "Not a string: [...]".

Both are now expressed as tags owned by this mod. Tags drop entries whose mod is
absent, so the same bytes load in both configurations: with the mod the tag
resolves to the original blocks/features, without it the tag is empty and the
modifier is inert. The generated `replace` tag holds exactly the one audited id
and the modifier points its `biomes` and `replace` fields at their tags as bare
strings. The only id that cannot be hidden behind a tag is a `would_survive`
block state (used by two Regions Unexplored features); those stay native, as
listed above.

This is enforced by `Phase3ResourceTest.everyGeneratedPlacementLoadsWithoutTheBiomeMod`
and by both production-server runs (with and without the biome mods).

## Profiles

Phase 3 added **9** profiles that reuse an existing morphology but write a
different block pair, bringing the shipped total from 42 to **51**:

| Species | Base morphology | Blocks |
| --- | --- | --- |
| `regions_unexplored:red_maple` | maple | maple log, red maple leaves |
| `regions_unexplored:orange_maple` | maple | maple log, orange maple leaves |
| `regions_unexplored:blue_magnolia` | magnolia | magnolia log, blue magnolia leaves |
| `regions_unexplored:pink_magnolia` | magnolia | magnolia log, pink magnolia leaves |
| `regions_unexplored:white_magnolia` | magnolia | magnolia log, white magnolia leaves |
| `regions_unexplored:enchanted_birch` | silver birch | silver birch log, enchanted birch leaves |
| `regions_unexplored:golden_larch` | larch | larch log, golden larch leaves |
| `regions_unexplored:dead_pine` | pine | pine log, dead pine leaves |
| `regions_unexplored:ashen_pine` | pine | ashen log, pine leaves |

Terralith owns no blocks; its nine replaced features reuse vanilla oak, birch
or spruce profiles. That is why Terralith has no Terralith-named profile.

## Sand ground support

`WorldGenPlacementContext.isGround` now also accepts `#minecraft:sand`, because
the Regions Unexplored palm family legitimately grows on sand. This does not
widen replacement: the mirrored placed features keep the native
`block_predicate_filter` (`would_survive` on the matching sapling), so a palm is
only attempted where the original would have attempted it. The ground check is
the second gate, not the first.

## Tests

`gradlew test`:

- **Historical Phase 3 gate (`docs/test-logs/test-19-phase3.log`)**: **104 tests, 0 failures**.
- **Final suite (`docs/test-logs/test-24-final.log`)**: **117 tests, 0 failures**.

Phase 3 added or extended:

| Class | Proves |
| --- | --- |
| `resources.Phase3ResourceTest` | 70 single-feature tag-scoped modifiers; 70/200 catalogs; preserved/replaced disjointness; closed generated graph; vanilla-only placement modifiers; removal through a tag and no foreign inline reference in any generated placement; every static tree has a shipped profile with `attempts=3`; no Phase 2/Phase 3 overlap |
| `profile.ProfileLoadingTest` | 51 shipped profiles load and validate |
| `compat.AdapterIsolationTest` | adapters remain isolated; absent mods stay `ABSENT` |
| `compat.ReplaceableCatalogTest` | the vanilla Phase 2 catalog is unchanged |

## Runtime evidence

Production Forge 1.20.1 (`47.4.29`) dedicated-server runs and live modpack verification confirm the tag-based Phase 3 graph both with and without biome mods:

- [`docs/test-logs/server-20-phase3-nomods.log`](file:///E:/Pycharm%20Projects/Branched/branched/docs/test-logs/server-20-phase3-nomods.log) — **biome mods absent** (post-tag rewrite, fresh level `selftest-static-phase3-c`): server reached `Done` with zero unbound registry/tag errors; `minecraft` adapter `READY`, `regions_unexplored` and `terralith` `ABSENT`; `SELFTEST summary: biomes=6 branches=779 trees=158 logs={spruce=989, acacia=68, oak=5} leaves={spruce=4328, acacia=508, oak=44} feature_placements=819 feature_rejections=168`.
- [`docs/test-logs/server-21-phase3-selftest.log`](file:///E:/Pycharm%20Projects/Branched/branched/docs/test-logs/server-21-phase3-selftest.log) and [`docs/test-logs/server-21-diagnostics.json`](file:///E:/Pycharm%20Projects/Branched/branched/docs/test-logs/server-21-diagnostics.json) — **final Phase 3 production-server run** with Regions Unexplored `0.5.6`, TerraBlender `3.0.1.10`, and Terralith `2.5.4` (fresh level `selftest-static-phase3-final`, 6 targets: `taiga`, `ru_maple`, `ru_blackwood`, `ru_rainforest`, `terralith_misty`, `terralith_highlands`):
  - Adapters reported `READY`: `minecraft` (`18/13`), `regions_unexplored` (`61/15`, after moving `regions_unexplored:palm_tree_on_sand` to `SPECIAL_PRESERVE`), and `terralith` (`9/9`).
  - `SELFTEST summary: level=minecraft:overworld targets=6 biomes=6 branches=736 trees=270 logs={spruce=431, oak=411, maple=557, blackwood=970, palm=213, kapok=210, birch=590} leaves={spruce=2212, maple=3078, red_maple=535, blackwood=4888, cypress=8, oak=3364, palm=523, flowering=88, kapok=222, birch=988} feature_placements=1219 feature_rejections=4845 rejections={blocked=4844, outside_write_region=1} chunkgen_ms=[p50=33.07,p95=733.07,p99=957.60,max=957.60,n=54] heap_mb=[before=860,after=893,max=3072]`.
  - Exported diagnostics JSON recorded `avgMs=0.072`, `p50Ms=0.044`, `p95Ms=0.093`, `p99Ms=0.12`, `maxMs=11.187`, `zeroBlockEntities=true`, `zeroPeriodicSimulation=true`; shutdown logged `generatedTrees=6160, rejectedPlacements=4854, segments=105344, leafClusters=99021, blocks=1094332, estimatedQuads=3397754, totalNanos=443390300, maxNanos=11187100, trackedProfiles=14, p50Nanos=43800, p95Nanos=92800, p99Nanos=121900, latencySamples=4096`.
- [`docs/test-logs/server-17-phase3-selftest.log`](file:///E:/Pycharm%20Projects/Branched/branched/docs/test-logs/server-17-phase3-selftest.log) and [`docs/test-logs/server-18-phase3-nomods-failure.log`](file:///E:/Pycharm%20Projects/Branched/branched/docs/test-logs/server-18-phase3-nomods-failure.log) — retained as earlier 12-target and inline-failure baselines that drove the tag rewrite.

## Known limitations

- **Terralith coverage is deliberately narrow.** Nine features pass all rules;
  the rest force dirt, alter the ground, carry decorators, use weighted foliage,
  or link to vanilla biomes. Terralith replacement is therefore a proof that the
  pipeline works with a biome mod, not a claim of full Terralith coverage.
- **Regions Unexplored dev-harness caveat.** Regions Unexplored 0.5.6 ships a
  Mixin shadow without a refmap entry; its `ru.mixin.json` cannot be applied in
  a ForgeGradle `runServer` userdev environment, which uses official names.
  The production jar is unaffected. Phase 3 runtime evidence for Regions
  Unexplored therefore comes from a production Forge server and the live Modrinth
  modpack client, not `runServer`.
- **Client runtime verified under modpack load; `/reload` tested atomically.**
  Live client execution with Regions Unexplored and Terralith active is logged in
  [`docs/test-logs/modpack-client-26-branched.log`](file:///E:/Pycharm%20Projects/Branched/branched/docs/test-logs/modpack-client-26-branched.log)
  (including `regions_unexplored:blackwood` and vanilla species placed under
  C2ME workers). `ProfileReloadListener` executes on datapack load during startup,
  and atomic swap + failure rollback are verified by
  `Phase6HardeningTest.datapackReloadSwapsAtomicallyAndPreservesRegistryOnTotalFailure`;
  manual interactive `/reload` from an operator console remains unexecuted.
