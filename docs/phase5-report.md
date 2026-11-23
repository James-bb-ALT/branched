# Phase 5 Report — Measurement-Driven Diversity and Presets

Phase 5 validates structural diversity across all 51 shipped species profiles,
quantifies exposed geometry and generation latency, and implements the
`rendering.preset` contract (`performance`, `balanced`, `visual`).

## Methodology and Tooling

- **Generator under test**: `SemiproceduralMorphologyGenerator` (`VERSION = 2`)
  paired with `ReferenceBlockRasterizer`.
- **Canonical signatures**: `CanonicalShape` hashes voxel layouts after
  exact placement normalization (`exact`), 90-degree Y-axis rotation
  normalization (`rotational`), and quantized scale normalization (`scaled`,
  `scale_bins = 10`).
- **Offline measurement runner**: `gradlew phase5Report`
  (`dev.branched.diagnostics.Phase5Report`) warms 8 trees per profile, then
  generates 64 samples per profile across deterministic `(x, z)` offsets using
  base seed `0x5EED_1A5L`. It emits
  [`docs/phase5-diversity.json`](file:///E:/Pycharm%20Projects/Branched/branched/docs/phase5-diversity.json)
  and
  [`catalogs/phase5-diversity.csv`](file:///E:/Pycharm%20Projects/Branched/branched/catalogs/phase5-diversity.csv).
- **Automated unit gates**: `Phase5DiversityTest` (6 tests in
  [`docs/test-logs/test-24-final.log`](file:///E:/Pycharm%20Projects/Branched/branched/docs/test-logs/test-24-final.log))
  runs on every `gradlew test` over 32 samples per replaceable species.

## Aggregate Results (64 Samples × 51 Profiles)

From `docs/phase5-diversity.json`:

| Metric | Value | Notes |
| --- | ---: | --- |
| Total shipped profiles measured | 51 | 48 replaceable (`STATIC_NORMAL` + `STATIC_FRUIT`), 3 `SPECIAL_PRESERVE` |
| Samples per profile | 64 | Warmup of 8 excluded from percentiles |
| Exact duplicate rate (`exact_dup`) | **0.000** | 64/64 unique topologies across all 51 profiles |
| Rotational duplicate rate (`rotational_dup`) | **0.000** | 64/64 rotation-invariant silhouettes unique |
| Scaled duplicate rate (`scaled_dup`) | **0.000** | `mean_scaled_dup_replaceable = 0.000`, `max_scaled_dup_replaceable = 0.000` (gate threshold: `<= 0.100`) |
| Exposed quads per block (`quads_per_block`) | **2.671 – 3.686** | Below naive 6.0 faces/block via face culling (`twilightforest:dark` 2.671, `regions_unexplored:cypress` 3.686) |
| Median generation latency (`gen_p50_us`) | **17.1 – 628.1 us** | `small_oak` 17.1 us, vanilla `spruce` 31.5 us, `oak` 53.2 us, `redwood` 93.2 us, `golden_oak` 628.1 us |
| Tail latency (`gen_p95_us`) | **25.8 – 1015.5 us** | All replaceable profiles under ~1.02 ms p95 in pure Java |

## `rendering.preset` Measurement (`PerformancePreset`)

`PerformancePreset` scales leaf density (`leafDensityFactor`) and leaf budget
(`leafBudgetFactor`) while keeping trunk, branch topology, and archetype intact:

| Preset | `leafDensityFactor` | `leafBudgetFactor` | Role |
| --- | ---: | ---: | --- |
| `PERFORMANCE` | `0.82` | `0.85` | Fewer leaves, smallest quad/overdraw footprint |
| `BALANCED` | `1.00` | `1.00` | Shipped default; bit-identical to unconfigured output |
| `VISUAL` | `1.16` | `1.15` | Denser foliage within species leaf budget |

Measured means over 16 seeds per preset (`docs/phase5-diversity.json`):

| Species | `PERFORMANCE` leaves / quads | `BALANCED` leaves / quads | `VISUAL` leaves / quads |
| --- | ---: | ---: | ---: |
| `minecraft:oak` | 121.438 / 518.500 | 132.063 / 529.625 | 141.063 / 535.250 |
| `minecraft:spruce` | 52.125 / 212.375 | 59.000 / 215.500 | 64.438 / 216.125 |
| `minecraft:jungle` | 408.938 / 1640.750 | 452.250 / 1642.750 | 492.563 / 1631.625 |
| `twilightforest:canopy` | 360.750 / 1311.625 | 397.188 / 1319.750 | 428.938 / 1305.625 |

(Note: On large broadleaf crowns such as `jungle` and `canopy`, higher interior leaf density culls additional hidden interior faces between adjacent leaf blocks, slightly reducing exposed surface quads even as leaf block count increases monotonically.)

## Sample Representative Families (`catalogs/phase5-diversity.csv`)

| Species | Compatibility | Mean height | Mean width | Mean logs | Mean leaves | Mean quads | Quads/block | `gen_p50_us` | `gen_p95_us` |
| --- | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| `minecraft:oak` | `STATIC_NORMAL` | 9.234 | 9.953 | 9.516 | 124.125 | 496.813 | 3.193 | 53.200 | 100.400 |
| `minecraft:spruce` | `STATIC_NORMAL` | 8.969 | 6.172 | 9.656 | 55.531 | 206.250 | 2.858 | 31.500 | 51.700 |
| `minecraft:birch` | `STATIC_NORMAL` | 9.734 | 6.922 | 11.219 | 36.656 | 206.906 | 3.444 | 45.100 | 107.600 |
| `minecraft:acacia` | `STATIC_NORMAL` | 11.406 | 10.734 | 10.141 | 128.531 | 535.688 | 3.385 | 92.800 | 220.700 |
| `minecraft:dark_oak` | `STATIC_NORMAL` | 11.484 | 12.609 | 17.172 | 309.297 | 991.375 | 2.762 | 311.700 | 502.500 |
| `regions_unexplored:maple` | `STATIC_NORMAL` | 11.375 | 12.500 | 11.281 | 260.000 | 877.281 | 2.961 | 98.400 | 146.900 |
| `regions_unexplored:blackwood` | `STATIC_NORMAL` | 14.313 | 14.547 | 17.656 | 374.109 | 1239.125 | 2.854 | 206.300 | 630.500 |
| `regions_unexplored:palm` | `STATIC_NORMAL` | 15.172 | 9.031 | 13.922 | 116.313 | 522.344 | 3.373 | 52.700 | 70.200 |
| `regions_unexplored:kapok` | `STATIC_NORMAL` | 24.094 | 21.516 | 70.484 | 448.641 | 1846.094 | 2.971 | 161.100 | 242.500 |
| `regions_unexplored:redwood` | `STATIC_NORMAL` | 26.844 | 20.891 | 85.063 | 198.969 | 1173.781 | 3.165 | 93.200 | 113.700 |

## Verification

- `Phase5DiversityTest` (**6/6 tests PASSED** in `docs/test-logs/test-24-final.log`):
  - `everyReplaceableSpeciesHasNoNormalizedDuplicateAcrossSeeds`
  - `everyReplaceableSpeciesHasNoLiteralDuplicate`
  - `presetsScaleLeafGeometryMonotonically`
  - `balancedPresetReproducesTheUnconfiguredGeometry`
  - `exposedQuadsStayBelowTheNaiveSixFacesBound`
  - `canonicalSignaturesAreTranslationAndRotationInvariant`
- `gradlew phase5Report` regenerated `docs/phase5-diversity.json` and `catalogs/phase5-diversity.csv` from the final 51 profiles cleanly.
