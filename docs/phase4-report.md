# Phase 4 — Aether, Fish of Thieves, Quark and Twilight Forest

Phase 4 closes the integration surface named by the plan's roadmap:

> **Fase 4 - outras integrações.** Aether (32), Fish of Thieves (3), Quark (8),
> Baobab (4), DT Addon Lib (4 definições de biblioteca), Plus (9), Twilight (18).
> Gate: funcionalidades especiais preservadas ou opt-in testado.

The gate has two accepted outcomes: a special feature is **preserved**, or a
replacement is **tested as opt-in**. This phase takes the first outcome for
every audited tree, because every candidate fails at least one safety rule. The
result is a deliberate "no": the mod ships **zero** new worldgen files for these
mods, so their fruit, roots, magic mechanics and biome injection cannot be
damaged by Branched.

The decision for every feature is recorded in
`catalogs/phase4-preserved.csv` (37 rows), `docs/phase4-tree-audit.csv`
(every tree-shaped placed feature, including the ones with no biome link) and
`docs/phase4-audit.json` (the same data as JSON).

## Method

`tools/generate_modded_phase4.py` reads the reference JARs (never committed) and
applies the Phase 3 rule set to every placed feature owned by the mod. Three
details differ from Phase 3:

1. **Selectors are resolved through the placed registry.** A
   `minecraft:random_selector` holds *placed* features, not configured features.
   The Phase 4 resolver walks placed → configured → placed with the correct
   registry-qualified visited set, so a selector is judged by the trees it can
   actually place.
2. **Links are propagated to descendants.** A tree that is only reachable
   through a selector inherits the selector's biomes and generation step.
3. **New preservation reasons** describe the Phase 4 blockers:
   `modded_placement:*`, `special_feature_type:*`, `foreign_reference:*`,
   `unlinked_feature`, `platform_unsupported:*`, `no_reference_jar`.

A feature is replaced only when every Phase 3 rule still holds *and* its
placement array uses only vanilla modifiers, because a generated placed feature
is parsed by every client and server even when the mod is absent. An unknown
placement type aborts datapack loading — the exact failure recorded in
`docs/test-logs/server-16-phase3-datapack-failure.log`.

## Result

| Mod | Audited version | Placed features | Tree-shaped | Replaced | Preserved |
| --- | --- | ---: | ---: | ---: | ---: |
| Aether | 1.20.1-1.5.2 | 26 | 6 | 0 | 6 |
| Fish of Thieves | 3.1.7 | 22 | 7 | 0 | 7 |
| Quark (+ Zeta) | 4.0-462 | 8 | 5 | 0 | 5 |
| Twilight Forest | 4.3.2508 | 77 | 19 | 0 | 19 |
| **Total** | | **133** | **37** | **0** | **37** |

Reason occurrences (a feature can list several; each listed id counts once):

| Reason | Aether | Fish of Thieves | Quark | Twilight Forest |
| --- | ---: | ---: | ---: | ---: |
| `decorators:*` | 1 | 9 | — | 18 |
| `modded_placement:*` | 9 | 2 | — | 20 |
| `platform_unsupported:*` | 6 | — | — | — |
| `foreign_biome:*` | — | 5 | — | — |
| `foreign_reference:*` | — | 2 | — | 2 |
| `unlinked_feature` | 1 | — | 5 | — |
| `special_feature_type:*` | — | — | — | 6 |
| `custom_canopy_feature` | — | — | — | 5 |
| `missing_profile:*` | — | — | 5 | 1 |
| `hollow_tree_feature` | — | — | — | 1 |
| `landmark_scale_feature` | — | — | — | 1 |
| `unresolved_foliage` | 1 | — | — | 1 |
| `unsupported_type:*` | 1 | — | — | 2 |

## Why no replacement is safe

### Aether

Four placed features place real trees: `skyroot_forest_trees`,
`skyroot_grove_trees`, `skyroot_meadow_trees` and `skyroot_woodland_trees`. Each
resolves to `aether:trees_skyroot_and_golden_oak`, a `minecraft:random_selector`
that picks skyroot (99%) or golden oak (1%); both are plain `minecraft:tree`
nodes with no decorators, and both have shipped profiles. The blocker is
placement: every one of them uses `aether:improved_layer_placement` and
`aether:dungeon_blacklist_filter`. A generated placed feature containing those
types cannot be parsed when Aether is absent, and approximating them with
vanilla modifiers would change where trees generate (island layer selection,
dungeon exclusion).

On top of that, the audited 1.20.1 build (`1.20.1-1.5.2`) is **NeoForge-only** —
Modrinth publishes no Forge file for that version. The project targets Forge
47.x, so Aether cannot be loaded in a Forge server even for an opt-in test
(`platform_unsupported:neoforge_jar`). Aether therefore stays audit-only until a
Forge-compatible build exists.

`aether:holiday_tree` is a decorated seasonal variant and `aether:crystal_tree`
is not placed by any biome; both stay native.

### Fish of Thieves

The fruit trees are the point of the mod and carry their fruit as decorators:

| Feature | Decorators |
| --- | --- |
| `banana_tree_checked` | `fishofthieves:banana`, `fishofthieves:banana_shoots` |
| `coconut_tree_checked` | `fishofthieves:coconut` |
| `mango_tree_checked` | `directional_attached_to_leaves`, `attached_to_leaves`, `beehive` |

The three shipped profiles (`fishofthieves:banana`, `coconut`, `mango`) are
`STATIC_FRUIT`, but a static mirror cannot attach the fruit blocks. Preserving
the native feature keeps bananas, coconuts and mangoes exactly as designed.

The two umbrella features (`trees_tropical_island`, `trees_coconut`) only make
this worse: they mix the fruit trees with vanilla `minecraft:jungle_tree` and
`fancy_oak_checked`, and `trees_coconut_tropical_island` adds
`fishofthieves:vegetation_filter` and `continents_filter`. The mod's own
`forge:add_features` modifiers also inject `banana_tree_checked`,
`coconut_tree_checked` and `mango_tree_checked` into `minecraft:sparse_jungle`
and `minecraft:beach`, i.e. biomes outside the mod namespace, which Phase 3
already forbids (`foreign_biome`).

### Quark

Quark places its five coloured blossom trees with
`quark:blue_blossom_placed` and siblings. They are not referenced by any biome
JSON: Quark injects them at runtime into vanilla biomes. That yields
`unlinked_feature` (nothing to remove during worldgen) and
`foreign_biome`-class risk if we tried to add them back. The shipped
`quark:blossom` profile covers `blossom_log` + `blossom_leaves`, not the five
coloured leaf blocks, so the colour variants are `missing_profile` as well.
Quark's glow shrooms and ore features are not trees.

### Twilight Forest

Nineteen tree-shaped placed features, all preserved:

- **Magic and shaped trees** (`dark_canopy_tree`, `canopy_oak`, `hollow_tree`)
  use custom feature types that build their own trunk, roots and hollow
  interior. Their mechanics are exactly what the project must not replace
  (`special_feature_type`, `hollow_tree_feature`, `custom_canopy_feature`).
- **Ordinary oaks** (`twilight_oak_tree`, `savannah_oak_tree`, `mangrove_tree`,
  `large_twilight_oak_tree`, ...) all attach `twilightforest:tree_roots`, and
  the mangrove also attaches `trunkside_decorator` and `leave_vine`. Roots are
  native Twilight Forest blocks; a static mirror would drop them.
- **Placement**: every one of them uses `twilightforest:no_structure`, and the
  hollow oak adds `twilightforest:chunk_centerer`. Both are modded placement
  types, so a generated feature could not be parsed without Twilight Forest.
- `dead_canopy_tree` has no foliage provider and `dark_dead_bushes` and friends
  are vegetation, not trees.

`large_twilight_oak_tree` is additionally marked `landmark_scale_feature`: it is
the oversized variant, the same category Phase 3 preserves for RU and Terralith.

## Dynamic Trees add-ons

The plan names Baobab (4), DT Addon Lib (4 library definitions) and Plus (9).
The repository holds **no reference JAR** for them, Dynamic Trees is never a
runtime dependency, and their licenses are not registered for redistribution.
They stay `UNRESOLVED` and inactive; there is nothing to audit or wire.

## Generated resources and profiles

The generator produces **no** `branched:phase4/*` configured feature,
placed feature, biome tag or biome modifier. That is intentional: a partial
graph would be worse than none. `docs/phase4-audit.json` records
`replaced = 0`, and `catalogs/phase4-replacements.csv` contains only its header.

No profile was added in Phase 4. The eleven profiles that cover these mods
(`aether:skyroot`, `aether:golden_oak`, three Fish of Thieves fruits,
`quark:blossom`, five Twilight Forest woods) stay as audited reference data;
they are loaded and validated but not referenced by any placed feature.

## Tests

`resources.Phase4AuditTest` (pure JUnit, no Minecraft bootstrap) proves:

- `docs/phase4-audit.json` lists the four audited mods and the three
  unavailable Dynamic Trees add-ons, with `replaced = 0`;
- the preserved CSV has one row per tree-shaped feature, every row has a
  non-blank reason from the documented vocabulary, and every id is in the
  namespace of the mod it is attributed to;
- the replacements CSV has no data rows;
- no `data/branched/**/phase4/**` resource and no
  `branched:phase4/*` biome modifier exists;
- the tree audit CSV contains one row per tree-shaped feature and no row is
  marked `replaced`.

## Runtime evidence

Phase 4 preservation was verified across two dedicated Forge 1.20.1 (`47.4.29`)
server runs and the live 415-mod Modrinth client benchmark:

- [`docs/test-logs/server-22-phase4.log`](file:///E:/Pycharm%20Projects/Branched/branched/docs/test-logs/server-22-phase4.log) — Overworld dedicated server with Fish of Thieves `3.1.7`, Quark `4.0-462` (+ Zeta `1.0-31`), and Twilight Forest `4.3.2508`. All loaded adapters reported `READY`. In `taiga` (`-3496, 63, -4708`), Branched replaced vanilla trees (`branches=412 trees=84`) while leaving Quark hollow logs intact (`hollow_spruce=11`). In `fot_sparse_jungle` (`2420, 63, -4724`), native generation ran unmodified (`branches=0 placements_delta=0 rejections_delta=0 hollow_jungle=3`). Summary: `branches=412 trees=93 feature_placements=330 feature_rejections=34`; shutdown metrics `generatedTrees=364, rejectedPlacements=34, blocks=23213, estimatedQuads=67324, p50Nanos=82400, p95Nanos=483900, p99Nanos=701900`.
- [`docs/test-logs/server-23-phase4-twilight.log`](file:///E:/Pycharm%20Projects/Branched/branched/docs/test-logs/server-23-phase4-twilight.log) and [`docs/test-logs/server-23-diagnostics.json`](file:///E:/Pycharm%20Projects/Branched/branched/docs/test-logs/server-23-diagnostics.json) — Twilight Forest dimension run (`level=twilightforest:twilight_forest`, `targets=tf_forest,tf_dark_forest`) with Fish of Thieves `3.1.7`, Quark `4.0-462` (+ Zeta `1.0-31`), and Twilight Forest `4.3.2508`:
  - `tf_forest`: `chunks[-292,264] target_chunks=6/9 branches=0 trees=187 logs={twilight_oak=1065, canopy=459} leaves={twilight_oak=3337, canopy=3851} placements_delta=0 rejections_delta=0 chunkgen_ms=[p50=4.26,max=294.04,n=9]`.
  - `tf_dark_forest`: `chunks[-114,-218] target_chunks=8/9 branches=0 trees=437 logs={dark=1996, birch=6, oak=22} leaves={hardened_dark=24658, birch=73, oak=244} placements_delta=0 rejections_delta=0 chunkgen_ms=[p50=1.99,max=228.57,n=9]`.
  - `SELFTEST summary: level=twilightforest:twilight_forest targets=2 biomes=2 branches=0 trees=624 ... feature_placements=0 feature_rejections=0 rejections={} chunkgen_ms=[p50=1.99,p95=294.04,p99=294.04,max=294.04,n=18]`.
  - Zero static replacements occurred anywhere in `twilightforest:twilight_forest`, proving 100% preservation of Twilight trees and foliage.
- [`docs/test-logs/modpack-client-26-branched.log`](file:///E:/Pycharm%20Projects/Branched/branched/docs/test-logs/modpack-client-26-branched.log) — Live 415-mod Forge `47.4.28` modpack (`Optimized fps`), where all 7 integrated mods including The Aether loaded simultaneously and every adapter reported `READY` (`aether` `5/1`, `fishofthieves` `4/0`, `minecraft` `18/13`, `quark` `5/2`, `regions_unexplored` `61/15`, `terralith` `9/9`, `twilightforest` `7/7`; `dtbaobabtree` `ABSENT`), while keeping all 37 Phase 4 features untouched.

## Known limitations

- **The Aether JAR name vs Forge runtime.** Although Aether `1.20.1-1.5.2` uses a `-neoforge.jar` suffix, its `mods.toml` declares `javafml` (`forge [47.1.0,)`). It was **runtime-loaded on Forge 47.4.28** inside the `Optimized fps` modpack (`Adapter aether (The Aether): READY | 5 replaceable, 1 preserved`) without crashing; its 6 tree features remain preserved in worldgen because they rely on modded placement modifiers (`aether:improved_layer_placement`, `aether:dungeon_blacklist_filter`) and decorators.
- **Quark blossom injection is code-side.** The audit sees the placed features
  but not the code that adds them to vanilla biomes; preservation is therefore
  the only safe decision.
- **The Phase 4 decision is a snapshot.** Rerunning the generator after a mod
  update is expected; the script refuses to write a replacement graph that it
  cannot generate, so a future integration cannot silently regress to a
  half-mirrored feature.
