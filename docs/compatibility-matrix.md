# Compatibility matrix

The four levels are fixed by the project contract:

| Level | Meaning | Offered for replacement? |
| --- | --- | --- |
| `STATIC_NORMAL` | Safe static replacement; native logs, leaves, loot and sapling preserved | yes |
| `STATIC_FRUIT` | Replacement must preserve fruits/pods and harvest behaviour | yes |
| `SPECIAL_PRESERVE` | Original implementation must stay | **no** |
| `UNRESOLVED` | Not enough evidence | **no** |

Per-family detail, including the exact placed-feature ids, is in
`catalogs/species-matrix.csv`; the generated summary is
`catalogs/coverage-report.md`.

## Summary

| Mod | Audited version | Tree placed features | Profiles | Families covered | Adapter state |
| --- | --- | ---: | ---: | ---: | --- |
| Minecraft | 1.20.1 | 21 aggregates + 10 features | 10 | 10/10 | READY, 8 aggregates wired |
| Regions Unexplored | 0.5.6 | 392 referenced (61 replaced, 102 preserved) | 30 | 28/28 | READY, 61 features wired |
| Terralith | 2.5.4 | 505 referenced (9 replaced, 98 preserved) | 0 | 10/10 via vanilla blocks | READY, 9 features wired |
| The Aether | 1.20.1-1.5.2 | 9 | 2 | 2/2 | READY |
| Twilight Forest | 4.3.2508 | 28 | 5 | 8/8 | READY |
| Quark | 4.0-462 | 5 (+ glow shrooms) | 1 | 1/1 | READY |
| Fish of Thieves | 3.1.7 | 12 | 3 | 7/7 | READY |
| Dynamic Trees Baobab | 0.1.5 (reference) | 0 | 0 | - | UNRESOLVED, never active |

"Families covered" counts native wood families whose block ids are covered by a
profile, either directly or through a vanilla family that the feature places.
It does **not** mean the integration is finished.

## Phase 2 vanilla coverage

Eight vanilla tree aggregates are replaced during worldgen. Each static mirror
copies the vanilla placement array and selector chances exactly, and the
`species` gate preserves the native feature if a profile is missing.

| Feature | Biomes | Static mirror | Species |
| --- | --- | --- | --- |
| `minecraft:trees_taiga` | `taiga`, `snowy_taiga` | `branched:static_trees_taiga` | spruce 2/3, pine 1/3 |
| `minecraft:trees_snowy` | `snowy_plains`, `ice_spikes` | `branched:static_trees_snowy` | spruce |
| `minecraft:trees_grove` | `grove` | `branched:static_trees_grove` | spruce default, pine 1/3 |
| `minecraft:trees_savanna` | `savanna`, `savanna_plateau` | `branched:static_trees_savanna` | oak default, acacia 0.8 |
| `minecraft:trees_windswept_savanna` | `windswept_savanna` | `branched:static_trees_windswept_savanna` | oak default, acacia 0.8 |
| `minecraft:trees_windswept_forest` | `windswept_forest` | `branched:static_trees_windswept_forest` | oak, spruce 2/3, fancy oak 0.1 |
| `minecraft:trees_windswept_hills` | `windswept_hills`, `windswept_gravelly_hills` | `branched:static_trees_windswept_hills` | oak, spruce 2/3, fancy oak 0.1 |
| `minecraft:trees_badlands` | `wooded_badlands` | `branched:static_trees_badlands` | oak |

Evidence: `docs/test-logs/server-15-phase2-selftest.log` (dedicated server,
vanilla worldgen, seed `123456789`, six target biome groups, clean shutdown).
Each modifier adds and removes exactly one aggregate per biome:

```
Branched adds branched:static_trees_taiga to minecraft:taiga (vegetal_decoration)
Branched removed 1 native tree feature(s) from minecraft:taiga (vegetal_decoration)
```

### Deliberately preserved vanilla aggregates

All thirteen remain `SPECIAL_PRESERVE`; nothing is replaced:

| Feature | Reason |
| --- | --- |
| `trees_birch`, `trees_birch_and_oak`, `trees_cherry`, `trees_flower_forest`, `trees_meadow`, `trees_plains` | beehive decorators |
| `trees_jungle`, `trees_sparse_jungle` | cocoa pods and vines |
| `trees_mangrove` | roots, propagules and beehives |
| `trees_swamp` | vine decorator |
| `trees_old_growth_pine_taiga`, `trees_old_growth_spruce_taiga` | `alter_ground` podzol decorator |
| `trees_water` | ocean/river surface semantics |

### What Phase 2 does not claim

- Client launch and visual review of a static tree next to a vanilla one are
  still pending.
- Sapling drops and leaf decay are vanilla by construction (native
  logs/leaves/saplings, `persistent = false`), but were not harvested in game.
- Phase 2 runtime evidence is vanilla-only. Optional-mod runtime evidence lives
  in Phase 3 (`docs/test-logs/server-17-phase3-selftest.log`).

## Phase 3 modded coverage

Each replaced placed feature gets its own biome modifier and its own generated
biome tag, so a feature is removed and added independently. The full decision
tables are `catalogs/phase3-replacements.csv` (70) and
`catalogs/phase3-preserved.csv` (200); the audit summary is
`docs/phase3-audit.json`.

| Mod | Audited | Replaced | Preserved | Not trees |
| --- | ---: | ---: | ---: | ---: |
| Regions Unexplored 0.5.6 | 303 | 61 | 102 | 140 |
| Terralith 2.5.4 | 404 | 9 | 98 | 297 |
| **Total** | **707** | **70** | **200** | **437** |

"Audited" counts the mod's own placed features that its biomes reference; the
same biome lists also reference 89 + 101 vanilla ids, which are outside this
audit.

A feature is replaced only when all reachable configured nodes are plain trees
(simple trunk + simple non-air foliage), there is no decorator and no
`force_dirt`, a shipped `STATIC_NORMAL`/`STATIC_FRUIT` profile exists, the
feature is used in exactly one `vegetal_decoration` step, every placement
modifier is vanilla, and every linked biome belongs to the mod namespace.

Preserved reasons: `shrub_silhouette` 76, `decorators:*` 40, `force_dirt` 35,
`unresolved_*` 25, `fungus_feature` 21, `unsupported_type:*` 18,
`foreign_biome:*` 12, `landmark_scale_feature` 10, `fallen_log_feature` 9,
`missing_profile:*` 9, `non_vegetal_step:*` 7, `joshua_shrub_feature` 5,
`cactus_feature` 4, `socotra_landmark` 4, `bamboo_feature` 3,
`placement_block_state:*` 2, `profile_not_replaceable:*` 1.

Generated files: 70 modifiers, 70 biome tags, 70 placed-feature tags, 14 block
tags, 56 configured features (55 `branched:static_tree` + one selector)
and 73 placed features. Ids and files live under
`branched:phase3/<mod>/<path>`.

### Generated files must load without the biome mod

Every generated file is parsed by every client and server, installed mod or not,
and two kinds of reference are resolved *while the datapack loads*:

- a `HolderSet` written as an inline list binds each id immediately;
- a block id inside a `matching_blocks` predicate is looked up in the block
  registry.

Both are therefore expressed as tags owned by this mod: the modifier's `biomes`
and `replace` fields are bare `"#branched:phase3/..."` strings, and every
foreign block id in a placement predicate sits in a
`tags/blocks/phase3/**` tag. Tags drop entries whose mod is absent, so the same
bytes load in both configurations — with the mod the tag resolves, without it the
tag is empty and the modifier is inert. The only unmirrorable case is a block
state in a `would_survive` predicate (`palm_tree_on_sand`, `cobalt_tree`), which
cannot be replaced by a tag; those two features stay native
(`placement_block_state`). Two production-server runs record the failure modes
that forced this design:
`docs/test-logs/server-18-phase3-nomods-failure.log` (inline `matching_blocks`
list,
"Not a string: [...]") and
`docs/test-logs/server-19-phase3-unbound-failure.log` (inline `replace` list,
"Unbound values in registry placed_feature").

## Phase 4 integration audit

Aether, Fish of Thieves, Quark and Twilight Forest were audited in Phase 4. The
gate ("special features preserved or opt-in tested") is met by **preserving every
feature**: all 37 tree-shaped placed features fail at least one Phase 3 safety
rule, so the mod ships no `branched:phase4/*` resource at all. Decisions:
`catalogs/phase4-preserved.csv`; per-feature evidence:
`catalogs/phase4-tree-audit.csv`; rationale: `docs/phase4-report.md`.

| Mod | Placed features | Tree-shaped | Replaced | Preserved |
| --- | ---: | ---: | ---: | ---: |
| Aether 1.20.1-1.5.2 | 26 | 6 | 0 | 6 |
| Fish of Thieves 3.1.7 | 22 | 7 | 0 | 7 |
| Quark 4.0-462 | 8 | 5 | 0 | 5 |
| Twilight Forest 4.3.2508 | 77 | 19 | 0 | 19 |
| **Total** | **133** | **37** | **0** | **37** |

Dominant blockers: `decorators:*` (fruit, roots, vines), `modded_placement:*`
(Aether layer/dungeon filters, Twilight Forest `no_structure`/`chunk_centerer`),
`special_feature_type:*` and `custom_canopy_feature` (Twilight Forest magic and
hollow trees), `foreign_biome:*` (Fish of Thieves injecting into
`minecraft:sparse_jungle` and `minecraft:beach`), `unlinked_feature` (Quark
injects blossoms from code, so no biome link exists to replace), and
`platform_unsupported:neoforge_jar` (Aether 1.5.2 has no Forge build).

## Minecraft

All eight overworld wood families are `STATIC_NORMAL`, plus the two configured
variants that only reuse vanilla blocks: `minecraft:fancy_oak` and
`minecraft:pine`. `minecraft:trees_plains` is audited as `SPECIAL_PRESERVE`
because its feature carries a beehive decorator.

## Regions Unexplored

- 30 profiles: 21 native families plus 9 leaf/log variants (red/orange maple,
  blue/pink/white magnolia, enchanted birch, golden larch, dead pine, ashen
  pine). 28/28 native wood families are covered.
- 61 placed features replaced in Phase 3, each by its own biome modifier and
  generated biome tag. 102 are preserved and 140 are not tree features.
- `STATIC_NORMAL` except:
  - `alpha` — `SPECIAL_PRESERVE` (alpha wood behaviour still under audit)
  - `bamboo` — `SPECIAL_PRESERVE` (bamboo-shaped wood, non-standard growth)
- 7 vanilla families used by RU features (`acacia`, `birch`, `cherry`,
  `dark_oak`, `jungle`, `oak`, `spruce`) resolve to the vanilla profiles.
- `apple_oak_tree` — `STATIC_FRUIT` (apple-bearing variant).
- `blackwood_mushrooms` — `SPECIAL_PRESERVE` (fungus decoration, not a tree).
- `fallen_larch_tree` — `SPECIAL_PRESERVE` (fallen log feature; the standing
  `larch_tree` is `STATIC_NORMAL`).
- Preserved buckets: 41 shrub silhouettes, 14 fungus features, 10 decorator
  features, 8 fallen logs, 8 landmark-scale features, 5 Joshua shrubs,
  4 Socotra landmarks, 3 cactus features, 2 bamboo features, 3 features outside
  `vegetal_decoration`, 1 non-replaceable profile and 8 unresolved trees.
- 7 wood families with no tree placed feature are not integrated:
  `blue_bioshroom`, `green_bioshroom`, `pink_bioshroom`, `yellow_bioshroom`,
  `brimwood`, `cobalt`, `mushroom`.
- Dependency: TerraBlender `>=3.0.0.163` (RU declares it mandatory).
- Runtime caveat: RU 0.5.6 ships a Mixin shadow without a refmap entry, so its
  `ru.mixin.json` cannot boot a ForgeGradle `runServer` userdev environment
  (official names). The production jar is unaffected; Phase 3 evidence comes
  from a production Forge server.

## Terralith

Terralith has **no blocks**: every tree feature places vanilla logs. Coverage is
therefore shape/biome-based and logs/leaves stay vanilla, so the nine replaced
features reuse vanilla oak, birch or spruce profiles.

Phase 3 replaces 9 placed features, each by its own biome modifier and generated
biome tag. 98 are preserved and 297 are not tree features. The most common
preservation reasons are shrub silhouettes (35), `force_dirt` (35), decorators
(30), unresolved foliage (17), `unsupported_type` (18), foreign biomes (12) and
9 features with no shipped profile.

- `foreign_biome:*` — Terralith links some features to `minecraft:*` biomes
  (for example `terralith:dark/trees` in `minecraft:dark_forest`). Replacing
  them would double-generate trees when Terralith is present and leave a stale
  static feature when it is absent, so they stay native.
- 9 replaced features: `terralith:highlands/forest/tall/trees_tall`,
  `highlands/temperate/{cloud_trees,dark_oak,trees_orange}`,
  `mountains/misty/trees_{base,birch,mid,top}` and `spires/oak_tree`.

Preserved examples:
- `terralith:skylands/trees` — floating-island placement, terrain dependent
- `terralith:cave/jungle/vegetation` — cave vegetation
- `terralith:enchanted/enchanted_oak` — mechanics under audit

## The Aether

Phase 4 verdict: **0 replaced, 6 preserved** (`platform_unsupported:neoforge_jar`,
`modded_placement:*`, `decorators:*`).

- `skyroot` — `STATIC_NORMAL` (four biome features share it)
- `golden_oak` via `gold_dungeon_island_foliage` — `STATIC_FRUIT`; golden apples
  and dungeon-island behaviour must be verified before enabling
- `aether:holiday_tree` — `SPECIAL_PRESERVE` (seasonal decorators)
- `berry_bush_stem` is **not** a tree and is excluded
- Runtime caveat: the file is named `neoforge` but declares `javafml` +
  `forge [47.1.0,)`. Verified loadable in the live 415-mod Forge 47.4.28 client
  modpack `Optimized fps` (`Adapter aether (The Aether): READY | 5 replaceable, 1 preserved`),
  while all 6 tree features remain preserved in worldgen (`0 replaced, 6 preserved`).

## Twilight Forest

Phase 4 verdict: **0 replaced, 19 preserved** (`special_feature_type:*`,
`custom_canopy_feature`, `modded_placement:*`, `decorators:*`).

- `STATIC_NORMAL`: `twilight_oak`, `canopy`, `dark` (darkwood), `mangrove`.
- `SPECIAL_PRESERVE`:
  - `tree/hollow_oak`, `hollow_stump` — interior spawner/loot
  - `tree/mining_tree`, `tree/sorting_tree`, `tree/time_tree`,
    `tree/transformation_tree` — **configured-only** magic trees generated by
    code/structures; they have no placed feature and must never be replaced
  - `mushroom/canopy_mushrooms_dense` — custom feature
- `tree/selector/*` features are random selectors over several families; they are
  not mapped one-to-one.
- The magic saplings exist as blocks but their trees are not data-driven.

## Quark

Phase 4 verdict: **0 replaced, 5 preserved** (`unlinked_feature`,
`missing_profile:*`).

- Five blossom variants (`blue`, `lavender`, `orange`, `red`, `yellow`) share the
  `quark:blossom` profile — `STATIC_NORMAL`.
- `quark:glow_shrooms` — `SPECIAL_PRESERVE` (custom `quark:glow_shroom` type).
- `quark:ancient` — `UNRESOLVED`: wood and sapling exist but there is **no**
  placed feature in 4.0-462.
- Quark injects blossom trees into biomes from code, so the audit shows 0 biome
  links. Replacement must therefore hook the feature, not the biome list.
- Dependency: Zeta `[1.0-28,2.0)` (mandatory).

## Fish of Thieves

Phase 4 verdict: **0 replaced, 7 preserved** (`decorators:*`, `foreign_biome:*`,
`foreign_reference:*`).

- `banana` — `STATIC_FRUIT`; trunk is `banana_stem`, leaves are
  `banana_leaves`, and there is **no sapling block**. Banana clusters grow on
  the trunk.
- `coconut` — `STATIC_FRUIT`; special trunk segments
  (`medium_/small_/small_top_coconut_log`) must be preserved.
- `mango` — `STATIC_FRUIT`; uses `minecraft:oak_log` +
  `fishofthieves:mango_leaves`; fruits and beehives are attached-to-leaves
  decorators.
- `sparse_jungle_fruit_trees` — selector over several fruit trees; only
  replaceable after every nested feature is resolved.
- 3.4.0 was audited separately: 21 configured / 23 placed vs 20/22 in 3.1.7.
  Feature ids must not be assumed stable across versions.

## Dynamic Trees (development reference only)

233 species were catalogued (`catalogs/dt-species.csv`) to compare morphology.
**No** Dynamic Trees code, species or assets are used, and `mods.toml` declares
no dependency on it.

`DynamicTreesBaobab 0.1.5`:
- JAR recovered locally (`DT_Baobab_Forge_1.20.1_v015.jar`, SHA-256 recorded);
- contains 4 species under `dtbaobabtree`;
- no verified public download and no original mod to audit;
- adapter is permanently inactive and reports `UNRESOLVED`.
- The original `baobab` species comes from Regions Unexplored 0.5.6 and **is**
  covered (`regions_unexplored:baobab`, `STATIC_NORMAL`).
