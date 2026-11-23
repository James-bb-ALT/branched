# Morphology Profiles Specification & Validator

Species morphology in Branched is strictly **data-driven** via datapack
JSON profiles at:

```
src/main/resources/data/branched/arboreal/species/<namespace>/<name>.json
```

All **51 shipped profiles** load cleanly (`0 errors, 0 warnings`) through both
Forge's runtime `ProfileReloadListener` (`AddReloadListenerEvent`) and the
offline Python validator `tools/validate_profiles.py`.

## Profile Schema (`schema: 1`)

- `schema`: integer, must be `1`.
- `species`: namespaced resource ID (`<namespace>:<name>`), unique across all loaded profiles.
- `archetype`: morphology archetype (`TreeArchetype` enum).
- `logs` and `leaves`: distinct namespaced block IDs; `branch` material is derived via `BranchMaterials.resolve(log)` (`branched:oak_branch`, `spruce_branch`, `acacia_branch` for matching wood families, native log fallback for others).
- `sapling` (optional): native sapling block ID used when mirrors preserve `would_survive` predicates.
- `trunk`: `{ "height": [min, max], "radius": [min, max], "bend": [min, max] }`.
- `branches`: `{ "primaryCount": [min, max], "depth": [min, max], "angleDegrees": [min, max], "lengthRatio": [min, max], "asymmetry": [min, max], "pattern": "alternate" | "opposite" | "whorled" }`.
- `crown`: `{ "widthRatio": [min, max], "density": [min, max], "clusterCount": [min, max] }`.
- `generation`: `{ "mode": "semiprocedural_once", "attempts": 1..8, "version": >= 2 }`. Any full-template mode (`nbt_template`) is rejected by `ProfileValidationTest`.
- `budget`: `{ "maxSegments": 8..4096, "maxLeaves": 8..8192, "maxQuads": 64..200000 }`.
- `compatibility`: `STATIC_NORMAL` | `STATIC_FRUIT` | `SPECIAL_PRESERVE` | `UNRESOLVED`. Only the first two may ever replace worldgen features via `WorldgenGate`.

## Shipped Profile Distribution (51 Profiles)

### By Namespace

| Namespace | Profiles | Replaced in Worldgen | Notes |
| --- | ---: | --- | --- |
| `minecraft` | 10 | 8 vanilla aggregates (12 biomes) | Includes `oak`, `fancy_oak`, `spruce`, `pine`, `birch`, `acacia`, `dark_oak`, `jungle`, `cherry`, `mangrove` |
| `regions_unexplored` | 30 | 61 placed features | 28 active families + 2 `SPECIAL_PRESERVE` (`alpha`, `bamboo`) |
| `twilightforest` | 5 | 0 (Phase 4 preserved) | `twilight_oak`, `canopy`, `dark`, `mangrove`, `mining` (`SPECIAL_PRESERVE`) |
| `fishofthieves` | 3 | 0 (Phase 4 preserved) | `banana`, `coconut`, `mango` (`STATIC_FRUIT` reference data) |
| `aether` | 2 | 0 (Phase 4 preserved) | `skyroot`, `golden_oak` (`STATIC_FRUIT` reference data) |
| `quark` | 1 | 0 (Phase 4 preserved) | `blossom` |

### By Compatibility Level

- `STATIC_NORMAL`: **44**
- `STATIC_FRUIT`: **4** (`aether:golden_oak`, `fishofthieves:banana`, `fishofthieves:coconut`, `fishofthieves:mango`) — preserved in Phase 4 worldgen because upstream fruit decorators are kept native
- `SPECIAL_PRESERVE`: **3** (`regions_unexplored:alpha`, `regions_unexplored:bamboo`, `twilightforest:mining`) — never eligible for `WorldgenGate` replacement

### By Archetype (`TreeArchetype`)

`TreeArchetype` defines 13 enum constants; 11 are populated by the 51 shipped
profiles (`BONSAI` and `MUSHROOM` are reserved in the enum for future or
datapack-supplied species):

| Archetype | Count | Example Species | Crown Shape Behaviour |
| --- | ---: | --- | --- |
| `broadleaf_round` | 17 | `regions_unexplored:maple`, `quark:blossom`, `minecraft:cherry` | Rounded upper canopy ellipsoid with primary + secondary radial scaffold branches |
| `broadleaf_irregular` | 9 | `minecraft:oak`, `minecraft:dark_oak`, `twilightforest:twilight_oak` | Asymmetric crown clusters and sinuous primary limbs |
| `conifer` | 8 | `minecraft:spruce`, `regions_unexplored:larch`, `pine` | Tiered conical whorl silhouette tapering toward the apex |
| `fantastic` | 3 | `regions_unexplored:blackwood`, `twilightforest:canopy`, `twilightforest:mining` | Broad multi-tier emergent canopy with elevated spread |
| `birch` | 3 | `minecraft:birch`, `regions_unexplored:silver_birch`, `enchanted_birch` | Narrow upright crown clustered in the upper third of the trunk |
| `mangrove` | 3 | `minecraft:mangrove`, `fishofthieves:mango`, `twilightforest:mangrove` | Compact mid-height canopy matching coastal/swamp scale |
| `palm` | 3 | `regions_unexplored:palm`, `fishofthieves:coconut`, `banana` | Curved slender stem with apical frond cluster |
| `columnar` | 2 | `minecraft:pine`, `regions_unexplored:redwood` | High-dominance straight trunk with compact high whorls |
| `acacia` | 1 | `minecraft:acacia` | Flattened horizontal umbrella crown |
| `baobab` | 1 | `regions_unexplored:baobab` | Heavy broad bole with concentrated top limbs |
| `cypress` | 1 | `regions_unexplored:cypress` | Tall narrow columnar conifer spire |

## Running the Offline Validator

```bash
python tools/validate_profiles.py
```

Latest verified output (`51` files under
`src/main/resources/data/branched/arboreal/species/`):

```text
Validated 51 profiles (48 replaceable, 3 preserved): 0 errors, 0 warnings
```

The validator checks schema version, unique namespaced IDs, archetype/compatibility
enums, distinct logs/leaves, `generation.mode == semiprocedural_once`,
`generation.version >= 2`, non-inverted parameter ranges within physical bounds,
and `budget` ceilings (`maxSegments`, `maxLeaves`, `maxQuads`).
