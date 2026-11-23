# Architecture

## Goals

1. Trees are generated **once per placement** and never simulated.
2. The same world seed, dimension, position and species always produce the same
   tree, independent of chunk generation order, server restarts or thread pool.
3. Replacements only ever happen during worldgen. Existing chunks are never
   rewritten.
4. Every third-party integration is isolated and optional.

## Layers

```
world seed ─┐
dimension  ─┤
x,y,z      ─┼─► SeedMixer ─► MorphologyRequest ─► MorphologyGenerator
species    ─┤                                            │
version    ─┘                                            ▼
                                                    TreeSkeleton
                                            (BranchNode / Segment / LeafCluster)
                                                         │
                                                         ▼
                                          BlockRasterizer ─► RasterizedTree
                                                         │      (BlockPlacement)
                                                         ▼
                                    TreeFitValidator ─► PlacementResult
                                                          │
                                                          ▼
                                                     WorldGenTreeWriter
                                             (trunk hard, branches/leaves soft)
```

Everything up to `TreeFitValidator` is **pure Java**: no Minecraft classes, no
world access, no BlockState. That is what makes the unit tests possible without a
server, and it keeps worldgen bugs out of the morphology code.

On the Forge side the chain is
`StaticTreeBiomeModifier` → `StaticTreeFeature` → `WorldGenPlacementContext` →
`TreeFitValidator` → `WorldGenTreeWriter`. The biome modifier only decides *what*
to replace; the feature does the per-position work during worldgen.

### Package map

| Package | Responsibility |
| --- | --- |
| `dev.branched.core` | `ResourceId` — validated `namespace:path`, Minecraft-free |
| `dev.branched.species` | `SpeciesProfile`, archetypes, branch patterns, compatibility levels, `SpeciesRegistry` |
| `dev.branched.morphology` | RNG, seed mixing, skeleton model, generator, rasterizer, palette |
| `dev.branched.worldgen` | `PlacementContext`, `TreeFitValidator`, `LeafDistance`, `WorldGenTreeWriter`, `WorldGenPlacementContext`, `WorldgenGate`, `BranchMaterials`, `MinecraftResourceIds` |
| `dev.branched.worldgen.feature` | `StaticTreeFeature` + its codec |
| `dev.branched.compat.biome` | `StaticTreeBiomeModifier` — the only replacement entry point |
| `dev.branched.block` | `StaticBranchBlock`, `BranchShape`, cached `VoxelShape`s |
| `dev.branched.registry` | `ModBlocks`, `ModItems`, `ModFeatures`, `ModBiomeModifiers` |
| `dev.branched.diagnostics` | opt-in `WorldgenSelfTest` (`-Dbranched.selftest=true`) |
| `dev.branched.profile` | JSON parser, strict validator, datapack reload listener |
| `dev.branched.metrics` | counters, rejection reasons, `GenerationMetrics` snapshot |
| `dev.branched.compat` | adapter contract, registry, per-mod mappings |
| `dev.branched.platform` | `ModPresence` abstraction (Forge + static fake) |
| `dev.branched.config` | common Forge config |

## Deterministic seeding

`SeedMixer.forTree` mixes, in order:

1. world seed
2. constant domain separator (`"STATIC"`)
3. hash of the dimension id
4. `(x << 32) ^ z`
5. `y`
6. hash of the species id
7. profile `generation.version`

`MorphologyRequest.seed()` adds an optional `variantSalt` for retry attempts.
`DeterministicRandom` is SplitMix64 with instance state only — no `java.util.Random`,
no global seed, no thread-local state.

Consequences enforced by tests:

- same request → identical topology signature;
- changing x, z or the world seed → different topology;
- call order does not affect results (no shared RNG);
- 40 different positions produce at least 38 distinct topologies.

Changing the generator algorithm **must** bump `SemiproceduralMorphologyGenerator.VERSION`
and the profile `generation.version`; otherwise previously generated chunks
would no longer reproduce their trees. Phase 2 bumped both from 1 to 2 when the
archetype crown library replaced the single ellipsoid crown, and all 42 shipped
profiles now carry `"version": 2`.

## Morphology

The generator produces a real, seed-varied skeleton:

- trunk height/radius sampled from the profile ranges;
- trunk bend + asymmetric lean, applied per layer;
- primary branches placed along the trunk according to the archetype
  (`CONIFER`/`CYPRESS`/`COLUMNAR` whorled low→high, `PALM` at the apex,
  `ACACIA` upper, `BAOBAB` mid-upper);
- azimuth from the branch pattern (`ALTERNATE` uses the golden angle,
  `OPPOSITE` pairs, `WHORLED` four per level);
- one second-order branch per primary branch when depth allows;
- an archetype-specific crown: layered cone for conifers, narrow stacked column
  for pines, flat umbrella for acacias, narrow ellipsoid for birches, round
  ellipsoid for broadleaves and an irregular ellipsoid for oaks.

Segment and leaf budgets from the profile are hard limits.

Crown radius scales with trunk height instead of using a fixed base:

```
radius = clamp(trunkHeight × widthRatio × archetypeFactor × (0.85 + density × 0.35),
               archetypeMin, archetypeMax)
```

`widthRatio` and `density` are sampled per tree from the profile.
`archetypeFactor` keeps the silhouettes apart: `0.38` conifer, `0.27`
columnar/cypress, `0.26` birch, `0.32` acacia/broadleaf, `0.30` default. The
clamp keeps every cluster inside the leaf-support distance, which is why ~94%
of the pilot's leaves survive the support filter.

Two regression tests guard density and shape: `SpruceCrownProportionsTest` pins
the shipped spruce profile to vanilla-like leaf/wood ratios, and
`MorphologyDiversityTest` requires every replaced species to change silhouette
per seed/position while keeping conifer < pine in width and acacia crowns above
oak crowns.

What it deliberately does **not** do yet: light-driven growth, competition
between branches, per-species internode grammar. See `docs/phase1-backlog.md`.

## Rasterization

`ReferenceBlockRasterizer` converts segments to integer positions by walking
each segment and thickening thick limbs, then fills leaf-ellipsoid volumes with a
deterministic per-position noise gate. It tracks occupied positions so branches
and leaves never overlap, estimates quads (`blocks × 6`) and reports
`budgetTruncated`.

Log counters count **trunk positions only** (`Segment.depth() == 0`); branch
positions are reported separately. Previously every thickened limb incremented
the log count, which made the pilot look like it had 55 logs per tree.

The skeleton is built around its own origin, so worldgen callers must pass the
placement origin to `rasterize(skeleton, profile, palette, x, y, z)`. Only the
three-argument form produces origin-relative coordinates; it exists for tests and
diagnostics. Placement bounds can only be validated on absolute coordinates.

Block identity always comes from the profile palette: the original mod's log and
leaf ids are referenced, never copied.

## Placement

`TreeFitValidator` runs before the first write and checks every block for:

- non-empty tree;
- vertical bounds against `minBuildHeight`/`maxBuildHeight`;
- write permission for the containing chunk (`WorldGenLevel.canWrite`), so no
  "setBlock in a far chunk";
- solid ground under the trunk.

Only the **trunk** is all-or-nothing: if a trunk position is occupied by a
non-replaceable block the whole tree is rejected and the world is untouched.
Branches and leaves are **soft**, exactly like vanilla tree features — an
obstructed branch or leaf is skipped individually, so hillside taiga does not
turn into bare patches. A branch that loses its connection to the trunk is
dropped as well, so no floating wood is left behind.

`WorldGenTreeWriter` then:

- writes the trunk first and rolls back if a write fails mid-way;
- filters branch/leaf candidates against `isAir`/`isReplaceable`
  (`#minecraft:replaceable_by_trees`, which includes `#minecraft:leaves`);
- computes leaf distances by multi-source BFS from log **and branch** positions,
  clamps them to 1..6 and omits anything at distance ≥7;
- writes native `minecraft:leaves` with `persistent = false` and a real
  `distance` value, so vanilla decay logic behaves normally;
- never creates a BlockEntity.

## Worldgen integration

`StaticTreeBiomeModifier` is the only replacement entry point. It is a Forge
`BiomeModifier` with the codec `branched:replace_trees` and runs once per
biome during `runModifiers`, before any chunk generation:

1. in `Phase.REMOVE`, removes the `PlacedFeature` holders listed in the audited
   adapter mapping (only `STATIC_NORMAL`/`STATIC_FRUIT`);
2. in `Phase.ADD`, adds the static `PlacedFeature` for the same generation step.
   The modifier's `species` field (single id or list) is the gate: if any listed
   profile is missing or not replaceable, the native feature is preserved.

`StaticTreeFeature` reads its species from the configured feature, asks
`WorldgenGate` whether the profile exists and is replaceable, retries up to
`morphology.max_generation_attempts` with a salted seed, and reports
per-reason rejections to `MetricsRecorder`. A missing profile, a disabled master
switch or an `UNRESOLVED` mapping preserves the native tree.

Phase 2 wires **eight** aggregates, generated from the 1.20.1 client jar by
`tools/generate_vanilla_phase2.py`:

| Replaced aggregate | Biomes | Static mirror | Species gate |
| --- | --- | --- | --- |
| `minecraft:trees_taiga` | taiga, snowy_taiga | `static_trees_taiga` | spruce, pine |
| `minecraft:trees_snowy` | snowy_plains, ice_spikes | `static_trees_snowy` | spruce |
| `minecraft:trees_grove` | grove | `static_trees_grove` | spruce, pine |
| `minecraft:trees_savanna` | savanna, savanna_plateau | `static_trees_savanna` | oak, acacia |
| `minecraft:trees_windswept_savanna` | windswept_savanna | `static_trees_windswept_savanna` | oak, acacia |
| `minecraft:trees_windswept_forest` | windswept_forest | `static_trees_windswept_forest` | oak, spruce, fancy_oak |
| `minecraft:trees_windswept_hills` | windswept_hills, windswept_gravelly_hills | `static_trees_windswept_hills` | oak, spruce, fancy_oak |
| `minecraft:trees_badlands` | wooded_badlands | `static_trees_badlands` | oak |

Each placed mirror keeps the vanilla placement array verbatim (count weighted
lists, `in_square`, water-depth filter, heightmap, biome filter, sapling
predicates), and each `random_selector` keeps its original chances: for example
`trees_taiga` stays 2/3 spruce and 1/3 pine, and `trees_savanna` stays oak
default with acacia at 0.8. Selectors point at mirrored **placed** features
because that is how 1.20.1 `random_selector` works.

Thirteen aggregates are deliberately **not** replaced and stay
`SPECIAL_PRESERVE`: `trees_birch`, `trees_birch_and_oak`, `trees_cherry`,
`trees_flower_forest`, `trees_meadow`, `trees_plains` (beehives), `trees_jungle`,
`trees_sparse_jungle` (cocoa/vines), `trees_mangrove` (roots/propagules),
`trees_swamp` (vines), `trees_old_growth_pine_taiga`,
`trees_old_growth_spruce_taiga` (`alter_ground` podzol) and `trees_water`
(ocean/river semantics). `ReplaceableCatalogTest` asserts exactly 21 audited
aggregates, 8 replaceable and 13 preserved with non-blank reasons.


## Profile loading

Profiles are read through `ResourceManager.listResources("arboreal/species", …)`,
so datapacks and resource packs can override them.

- reading + parsing happen off-thread;
- the registry is swapped atomically in the apply stage;
- a reload that yields zero valid profiles keeps the previous registry;
- each invalid profile produces a `ValidationIssue` with a code and JSON path;
- duplicates across files are reported, not silently merged.

The validator rejects, among other things: unknown schema, inverted ranges,
missing required blocks, `log == leaves`, non-`semiprocedural_once` generation
modes, out-of-range budgets, and malformed biome ids. Unknown top-level keys are
warnings so forward-compatible datapacks do not break.

## Compatibility adapters

`ModAdapter` declares a mod id, minimum version, default level and a list of
`FeatureMapping(feature, originalSpecies, level, notes)`.

- `AdapterRegistry` resolves each adapter against `ModPresence`.
- `AdapterState` is one of `READY`, `ABSENT`, `UNRESOLVED`, `CONFLICT`.
- `replaceableFeatures()` returns mappings whose level is `STATIC_NORMAL` or
  `STATIC_FRUIT` — never `SPECIAL_PRESERVE` or `UNRESOLVED`.
- `BaobabAdapter.present()` always returns `false`: a development-only reference
  jar must never activate a replacement.

## Invariants

These are asserted by tests and must not regress:

1. **No Dynamic Trees dependency.** `mods.toml` declares none, and no source file
   imports `dynamictrees`/`com.dynamictrees`.
2. **No BlockEntity per tree.** `rendering.use_block_entities` is false and no
   BlockEntity type is registered.
3. **No simulation.** `simulation.progressive_growth`, `root_simulation`,
   `custom_leaf_hydration` and `whole_tree_felling` default to false.
4. **No templates as primary mechanism.** `morphology.use_full_tree_templates_as_primary`
   is false and the validator rejects template generation modes.
5. **No retroactive replacement.** `worldgen.replace_existing_chunks` is false.
6. **Unknown features are preserved.** `worldgen.unknown_feature_policy = preserve_native`.
7. **Trunk-only hard rejection.** An obstructed branch or leaf never rejects a
   tree; only an obstructed trunk does.
8. **No floating wood.** Branches that would lose their path to the trunk are
   filtered before writing.

## Configuration (common)

| Key | Default | Meaning |
| --- | --- | --- |
| `worldgen.enabled` | `true` | master switch; when false the biome modifier preserves the native feature |
| `worldgen.replace_known_features` | `true` | only audited features may be replaced |
| `worldgen.unknown_feature_policy` | `preserve_native` | `preserve_native` / `static_if_profile` / `disable` |
| `worldgen.replace_existing_chunks` | `false` | must stay false |
| `simulation.*` | `false` | growth/root/hydration/felling, all forbidden |
| `rendering.preset` | `balanced` | `performance` / `balanced` / `visual` |
| `rendering.use_block_entities` | `false` | must stay false |
| `rendering.cache_baked_geometry` | `true` | branch shapes are baked once |
| `morphology.mode` | `semiprocedural_once` | only supported mode |
| `morphology.deterministic_by_world_seed` | `true` | seed derivation switch |
| `morphology.max_generation_attempts` | `3` | retries per placement before the native feature is left alone (clamped 1..8) |
| `compatibility.special_trees` | `preserve_native` | special trees are never replaced |
| `compatibility.warn_if_dynamictrees_loaded` | `true` | diagnostic only |
| `diagnostics.metrics_enabled` | `true` | counters, rejection reasons and periodic summary |
