# Phase 2 — vanilla coverage report

Phase 2 widens the Phase 1 taiga pilot to the vanilla tree aggregates whose
original features carry **no functional decorator**. Anything that places
beehives, cocoa pods, vines, podzol or water-surface semantics stays native.

## Audit result

All 21 vanilla core tree aggregates are audited exactly once in
`VanillaAdapter`; `ReplaceableCatalogTest` enforces the counts.

| Decision | Count | Evidence |
| --- | ---: | --- |
| `STATIC_NORMAL`, wired | 8 | see table below |
| `SPECIAL_PRESERVE`, untouched | 13 | see `docs/compatibility-matrix.md` |
| Total audited | 21 | |

Wired aggregates and their biomes:

| Aggregate | Biomes |
| --- | --- |
| `minecraft:trees_taiga` | taiga, snowy_taiga |
| `minecraft:trees_snowy` | snowy_plains, ice_spikes |
| `minecraft:trees_grove` | grove |
| `minecraft:trees_savanna` | savanna, savanna_plateau |
| `minecraft:trees_windswept_savanna` | windswept_savanna |
| `minecraft:trees_windswept_forest` | windswept_forest |
| `minecraft:trees_windswept_hills` | windswept_hills, windswept_gravelly_hills |
| `minecraft:trees_badlands` | wooded_badlands |

Twelve biomes in total. A missing profile on any species listed in a modifier's
`species` gate preserves the native feature, so a datapack that removes a
profile cannot produce a bare biome.

## Generated resources

`tools/generate_vanilla_phase2.py` reads the 1.20.1 client jar and mirrors the
vanilla graph. It is deterministic and committed output only; no jar is bundled.

Generation checks (script aborts on drift):

- the biome links of each aggregate in the jar must match the intent table;
- every mirrored tree feature must have a shipped species profile;
- selectors are mirrored recursively, including `random_selector` chances;
- placement arrays are copied verbatim.

Output: **8** biome modifiers, **9** configured features and **15** placed
features under `data/branched/`. The Phase 1 pilot files
(`static_spruce_taiga`) are removed. `VanillaPhase2ResourceTest` proves the
generated files form a closed graph with no stale leftovers, and that every
`would_survive` predicate names the sapling of its own profile.

Selector composition is preserved rather than approximated:

| Vanilla aggregate | Mirror |
| --- | --- |
| `trees_taiga` | `static_spruce_checked` default, `static_pine_checked` at 1/3 |
| `trees_grove` | `static_spruce_on_snow` default, `static_pine_on_snow` at 1/3 |
| `trees_savanna` | `static_oak_checked` default, `static_acacia_checked` at 0.8 |
| `trees_windswept_forest` / `_hills` | oak, spruce 0.666, fancy oak 0.1 |
| `trees_windswept_savanna` | same selector as savanna, different count |
| `trees_snowy` | direct `static_spruce` |
| `trees_badlands` | direct `static_oak` |

## Morphology (generator version 2)

The single ellipsoid crown became an archetype library:

| Archetype | Crown |
| --- | --- |
| `CONIFER`, `CYPRESS` | layered cone, wide base, narrow apex |
| `COLUMNAR` | narrow stacked column (pines) |
| `ACACIA` | flat umbrella above the trunk |
| `BIRCH` | narrow ellipsoid |
| `BROADLEAF_ROUND` | round ellipsoid |
| default (oak, fancy oak) | irregular ellipsoid |

`VERSION` and every profile's `generation.version` are 2. `MorphologyDiversityTest`
requires independent silhouettes per seed/position for every replaced species,
pines narrower than spruces, spruces narrower than oaks, and acacia foliage
higher than oak foliage. `SpruceCrownProportionsTest` keeps the spruce crown at
vanilla-like leaf/wood ratios.

Two profiles were added (`minecraft:pine`, `minecraft:fancy_oak`) plus an acacia
branch block, because savanna replaces acacia trees and the windswept selectors
place fancy oaks. Profile count: **42**.

## Tests

`gradlew test`: **93 tests, 0 failures**. Phase 2 added or rewrote:

| Class | Proves |
| --- | --- |
| `resources.VanillaPhase2ResourceTest` | closed resource graph, biome/species integrity, sapling predicates, no pilot leftovers |
| `morphology.MorphologyDiversityTest` | per-seed variance, archetype proportions, leaf/wood budgets |
| `compat.ReplaceableCatalogTest` | 21 audited, 8 replaceable, 13 preserved with reasons |
| `resources.BranchResourceTest` | Phase 2 modifiers/placement, acacia branch assets and loot |
| `worldgen.BranchMaterialsTest` | oak/spruce/acacia branch resolution |
| `profile.ProfileLoadingTest` | 42 profiles load and validate |

## Runtime evidence

<details>
<summary>Filled from `docs/test-logs/server-15-phase2-selftest.log`</summary>

Dedicated server, fresh level `selftest-static-phase2-d`, seed `123456789`,
`-Pbranched.selftest=true`, 10m07s wall clock, `BUILD SUCCESSFUL`, clean
shutdown. All 8 Phase 2 modifiers were applied, covering 12 biomes (12
biome/aggregate pairs; each biome maps to exactly one aggregate, and
`trees_windswept_hills` covers both `windswept_hills` and
`windswept_gravelly_hills`).

```
SELFTEST taiga      chunks=9/9 branches=406 trees=90 logs={spruce=585}  leaves={spruce=2553} placements_delta=186 rejections_delta=24
SELFTEST grove      chunks=9/9 branches=278 trees=54 logs={spruce=394}  leaves={spruce=1713} placements_delta=242 rejections_delta=27
SELFTEST savanna    chunks=9/9 branches=89  trees=12 logs={acacia=68}  leaves={acacia=508}  placements_delta=25  rejections_delta=14
SELFTEST windswept  chunks=5/9 branches=0   trees=1  logs={oak=5}      leaves={oak=44}      placements_delta=5   rejections_delta=0
SELFTEST badlands   chunks=8/9 branches=0   trees=0  logs={}           leaves={}            placements_delta=0   rejections_delta=0
SELFTEST snowy      chunks=9/9 branches=6   trees=1  logs={spruce=10}  leaves={spruce=75}   placements_delta=6   rejections_delta=0
SELFTEST summary: biomes=6 branches=779 trees=158 logs={spruce=989, acacia=68, oak=5}
                  leaves={spruce=4341, acacia=508, oak=44}
                  feature_placements=813 feature_rejections=168 rejections={blocked=168}
```

Totals after the sample (from the shutdown metrics line): 985 generated trees,
168 rejected placements, 14,110 segments, 20,180 leaf clusters, 67,369 blocks,
404,214 estimated quads, 60.1 ms total generation time, 2.8 ms worst tree.
Every rejection was `blocked` (dense canopies overlapping), never a missing
profile, missing ground, write-region or vertical-bounds failure. Windswept and
badlands are naturally sparse, which is why their 3x3 windows contain few or no
trees; the runner reports `target_chunks` so the sample size is explicit.

</details>

## Known limitations

- **Preserved decorators.** Thirteen aggregates stay native; replacing them
  would silently delete beehives, cocoa pods, vines or podzol.
- **Sparse biomes.** `windswept` and `wooded_badlands` sampled very few or zero
  trees inside a 3×3 chunk window, which reflects their natural density; the
  run reports `target_chunks` so the sample size is visible.
- **No client review.** Tree shape has not been inspected in a running client.
- **No optional mod run.** Adapters for Regions Unexplored, Terralith, Aether,
  Twilight Forest, Quark and Fish of Thieves are audited and unit-tested, not
  activated in a live server.
