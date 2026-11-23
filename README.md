# Branched

Deterministic, semiprocedural **static** trees for Minecraft Forge 1.20.1.

Branched replaces selected tree worldgen features with trees that are
generated **once**, at placement time, from a deterministic seed. There is no
growth simulation, no periodic ticking, no root spreading and no per-tree
BlockEntity.

> **Status: Branched `0.7.0`.** The mod builds with JDK 17 (`138` JUnit
> tests, `0` failures), loads its config, detects all optional mods via `15` isolated
> adapters, validates `70` datapack species profiles (`0` errors) and `66` 6-way
> static branch blocks, converts forked/diagonal native trunks (vanilla, Quark,
> Biome Makeover, Baobab Tree, Twilight Forest) without branch meshes, integrates
> rigid-body felling physics (`Immersive Tree Chop`, `Panda's Falling Trees`, `TreeChop`),
> adds `#branched:branches` -> stick crafting, provides `PERFORMANCE` / `BALANCED` / `VISUAL`
> presets, `/branched status|dump` + `logs/branched-diagnostics.json`, compiled
> Forge GameTests, and an opt-in offline Phase 7 Dynamic Trees `.mca` migration
> utility (`WorldMigrationTool`). Verified across dedicated Forge servers
> (`server-20`–`server-23`) and live in the 415-mod Modrinth modpack `Optimized fps`.

---

## What Phase 4 delivers

Phase 4 is the integration phase for Aether, Fish of Thieves, Quark and Twilight
Forest (plus the Dynamic Trees add-ons). Its gate is "special features preserved
or opt-in tested", and it takes the first outcome for every audited tree: a
static mirror would drop fruit, roots, vines, custom canopy/hollow mechanics or
modded placement semantics, and Aether 1.20.1-1.5.2 has no Forge build at all.
The mod therefore ships **zero** `branched:phase4/*` worldgen resources, so
these mods generate exactly as they did before.

| Mod | Tree-shaped placed features | Replaced | Preserved |
| --- | ---: | ---: | ---: |
| Aether 1.20.1-1.5.2 | 6 | 0 | 6 |
| Fish of Thieves 3.1.7 | 7 | 0 | 7 |
| Quark 4.0-462 (+ Zeta) | 5 | 0 | 5 |
| Twilight Forest 4.3.2508 | 19 | 0 | 19 |
| **Total** | **37** | **0** | **37** |

Details: `docs/phase4-report.md`, `catalogs/phase4-preserved.csv`,
`docs/phase4-tree-audit.csv`. Dynamic Trees Baobab, DT Addon Lib and Plus
stay `UNRESOLVED`: no reference jar is available and Dynamic Trees is never a
runtime dependency.

---

## Pipeline and state

| Area | State |
| --- | --- |
| Forge 1.20.1 / Forge 47.4.29 project | builds with JDK 17 |
| Mod entry point, common config | done |
| Optional-mod detection, isolated adapters | done (8 adapters) |
| Species profile schema + strict validator | done (51 profiles) |
| Datapack profile loader (`arboreal/species/*.json`) | done, reload-safe |
| Deterministic seed derivation | done (`world seed + dimension + x/y/z + species + generator version`) |
| Morphology generator (trunk, branches, crown) | archetype crowns, version 2 |
| Block rasterizer + validated writer | done (trunk hard, branches/leaves soft) |
| Static branch blocks | oak + spruce + acacia, 3 cached shapes, no BlockEntity |
| BiomeModifier worldgen replacement (vanilla) | done for 8 aggregates / 12 biomes |
| BiomeModifier worldgen replacement (mods) | done for 61 Regions Unexplored + 9 Terralith placed features |
| Preserved features | 13 vanilla aggregates + 200 modded (Phase 3) + 37 Phase 4 tree features, each with a documented reason |
| Phase 4 integrations | audited and preserved: Aether, Fish of Thieves, Quark, Twilight Forest |
| Phase 5 diversity & presets | done (`gradlew phase5Report`, 64 samples × 51 profiles = 0.000 scaled duplicates, `PERFORMANCE`/`BALANCED`/`VISUAL` presets) |
| Phase 6 hardening & diagnostics | done (`DiagnosticsExporter`, `/branched status\|dump`, `ArborealGameTests` + `empty.nbt`, `Phase6HardeningTest`) |
| Phase 7 offline DT migration | done (`WorldMigrationTool`, `--dry-run`/`--apply`/`--rollback` with SHA-256 backup + `SimpleBitStorage` repacking; tested on `New World (27)`) |
| Dedicated-server proof | Phase 2 (`server-15`), Phase 3 no-mods (`server-20`) & modded (`server-21`), Phase 4 overworld (`server-22`) & Twilight (`server-23`) |
| Client & modpack benchmark | done in live 415-mod Forge 47.4.28 client `Optimized fps` (`modpack-client-26`, `modpack-bench-26`) |

The pipeline is `MorphologyGenerator → BlockRasterizer → TreeFitValidator →
WorldGenTreeWriter`, with a pure-Java core so almost all of it can be tested
without bootstrapping Minecraft.

---

## Building

Requirements:

- JDK 17 (`JAVA_HOME` must point at a JDK 17, not 21+)
- No local Minecraft installation required; ForgeGradle downloads it

```bash
./gradlew build          # compile + test + jar
./gradlew test           # 138 JUnit tests
./gradlew phase5Report   # regenerate docs/phase5-diversity.json + catalogs/phase5-diversity.csv
./gradlew migrateWorld --args="--dry-run --world <save>"  # opt-in offline DT migration
python tools/validate_profiles.py                         # validate all 70 species JSON profiles
```

The produced jar is `build/libs/branched-0.7.0.jar`.

### Why no third-party jars are committed

`reference-mods/` holds the audited upstream jars used to build the catalogs.
They are **not** redistributed: the licenses range from MIT to *All rights
reserved* (Regions Unexplored), and the mod ships none of their code or assets.
The folder is git-ignored; `catalogs/jar-inventory.csv` records the SHA-256 of
every audited file instead. See `docs/dependency-license-register.md`.

---

## Species profiles

Species are **data**, not code. Each profile lives at:

```
src/main/resources/data/branched/arboreal/species/<namespace>/<name>.json
```

and can be overridden or extended by any datapack. Profiles are validated on
load; an invalid profile is rejected with a specific error and the remaining set
stays usable. A reload with zero valid profiles keeps the previous registry
instead of breaking worldgen.

Minimal profile:

```json
{
  "schema": 1,
  "species": "minecraft:oak",
  "archetype": "broadleaf_irregular",
  "logs": "minecraft:oak_log",
  "leaves": "minecraft:oak_leaves",
  "trunk":   { "height": [5, 9], "radius": [0.7, 0.95], "bend": [0.0, 0.15] },
  "branches":{ "primaryCount": [4, 8], "depth": [1, 3], "angleDegrees": [25, 55],
               "lengthRatio": [0.28, 0.50], "asymmetry": [0.1, 0.4], "pattern": "alternate" },
  "crown":   { "widthRatio": [0.9, 1.5], "density": [0.4, 0.75], "clusterCount": [6, 14] },
  "generation": { "mode": "semiprocedural_once", "attempts": 3, "version": 2 },
  "budget":  { "maxSegments": 128, "maxLeaves": 420, "maxQuads": 9000 },
  "compatibility": "STATIC_NORMAL",
  "source": { "feature": "minecraft:oak" },
  "biomes": ["#minecraft:is_forest"]
}
```

`generation.mode` accepts only `semiprocedural_once`: NBT templates are
explicitly rejected as a primary generation mechanism.

### Compatibility levels

Exactly four values, part of the project contract:

- `STATIC_NORMAL` — safe static replacement; native logs, leaves, loot, sapling.
- `STATIC_FRUIT` — replacement must preserve fruits/pods and harvesting.
- `SPECIAL_PRESERVE` — the original implementation must stay (magic/functional
  trees, hollow trees, bamboo, glow shrooms).
- `UNRESOLVED` — not enough evidence; native generation is preserved.

Only the first two are ever offered for replacement.

---

## Integrations

Adapters are isolated: a missing or incompatible mod disables **only** its own
adapter. Dynamic Trees is never a runtime dependency; it is a development
reference only.

| Mod | Audited version | Adapter state |
| --- | --- | --- |
| Minecraft (vanilla) | 1.20.1 | `READY`, 8 aggregates (18 replaced / 13 preserved tracked) |
| Regions Unexplored | 0.5.6 | `READY` (server + client), 61 features wired, 15 preserved (TerraBlender required) |
| Terralith | 2.5.4 | `READY` (server + client), 9 features wired, 9 preserved (shape-only; vanilla wood) |
| The Aether | 1.20.1-1.5.2 | `READY` in live 415-mod Forge client; 0 wired / 6 preserved |
| Twilight Forest | 4.3.2508 | `READY` (server + client), 0 wired / 19 feature audit rows preserved (magic and roots) |
| Quark | 4.0-462 | `READY` (server + client), 0 wired / 5 feature audit rows preserved (code-injected blossoms) |
| Fish of Thieves | 3.1.7 (3.4.0 audited too) | `READY` (server + client), 0 wired / 7 feature audit rows preserved (fruit decorators) |
| Dynamic Trees Baobab | 0.1.5 (reference only) | `ABSENT` / `UNRESOLVED`, never active |

---

## Documentation

- `docs/architecture.md` — layers, seed derivation, invariants
- `docs/audit-report.md` — what was measured in each upstream jar
- `docs/compatibility-matrix.md` — per-mod replacement policy
- `docs/dependency-license-register.md` — versions, SHA-256, licenses
- `docs/morphology-profiles.md` — profile schema v1, archetypes, `validate_profiles.py`
- `docs/benchmark-report.md` — §9 protocol compliance, dedicated-server & 415-mod client benchmarks
- `docs/phase1-backlog.md` — historical Phase 1 items updated with Phase 2–7 completion state
- `docs/phase2-report.md` — Phase 2 coverage, generated resources and evidence
- `docs/phase3-report.md` — Phase 3 Regions Unexplored / Terralith coverage, audit, and server proofs
- `docs/phase4-report.md` — Phase 4 Aether / Fish of Thieves / Quark / Twilight Forest preservation & runtime proofs
- `docs/phase5-report.md` — Phase 5 structural diversity (`0.000` duplicates) and rendering presets
- `docs/phase6-report.md` — Phase 6 hardening, `/branched`, diagnostics JSON, GameTests, modpack verification
- `docs/phase7-report.md` — Phase 7 opt-in offline `.mca` + NBT Dynamic Trees migration tool and round-trip proof
- `catalogs/phase3-replacements.csv` — 70 audited modded replacements
- `catalogs/phase3-preserved.csv` — 200 preserved modded features with reasons
- `catalogs/phase4-preserved.csv` — 37 Phase 4 tree features preserved with reasons
- `catalogs/phase5-diversity.csv` — per-species Phase 5 diversity, geometry, and timing table
- `catalogs/coverage-report.md` — generated coverage numbers
- `catalogs/species-matrix.csv` — per-family status

---

## License

Branched itself: see `LICENSE`. It contains no code or assets from the
audited mods. Referenced block/resource ids remain the property of their
respective authors.
