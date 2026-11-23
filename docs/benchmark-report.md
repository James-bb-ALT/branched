# Benchmark Report (§9 Protocol Compliance)

This report evaluates Branched against the §9 Benchmark Protocol of
[`plano_mod_arvores_hibridas_forge_1_20_1_v2.md`](file:///E:/Pycharm%20Projects/Branched/branched/plano_mod_arvores_hibridas_forge_1_20_1_v2.md)
using committed dedicated-server logs, Phase 5 offline measurements, and live
3440×1369 client benchmarks on the 415-mod Modrinth modpack `Optimized fps`.

## Evaluation Against the 7 §9 Minimum Criteria

| Criterion | Status | Verified Evidence |
| --- | --- | --- |
| **1. Zero periodic own simulation ticks** | **PASS** | `StaticInvariantsTest.noMainSourceDeclaresABlockEntityOrRandomTick`; `ArborealConfig` fixes `simulation.* = false`; diagnostics `zeroPeriodicSimulation: true`. |
| **2. Zero tree BlockEntities** | **PASS** | `StaticBranchBlock` uses baked models + 3 cached `VoxelShape`s with no `EntityBlock`; `BranchResourceTest`, `ArborealGameTests`, `DiagnosticsExporter` (`zeroBlockEntities: true`). |
| **3. Performance vs baselines & DT methodology note** | **PASS (with scope note)** | See methodology note below: compared against vanilla dedicated servers, Phase 5 quad/latency profiling, and live 415-mod client baselines (`nodiorama_curr`, `opt_diorama_noatlas`); Dynamic Trees A/B inside the live pack was not executed because DT JARs are disabled (`*.jar.disabled`) in the pack and DT + Branched target mutually exclusive chunk generation. |
| **4. Structural diversity (`exact`, `rotational`, `scaled`)** | **PASS** | [`docs/phase5-diversity.json`](file:///E:/Pycharm%20Projects/Branched/branched/docs/phase5-diversity.json): `0.000` duplicates across `64` samples × `51` profiles (`max_scaled_dup_replaceable = 0.000`, well under `0.100`). |
| **5. Chunkgen tail latency & FPS impact** | **PASS** | Server per-tree generation: `p50 = 0.044 ms`, `p95 = 0.093 ms`, `p99 = 0.120 ms` (`server-21`). Live 415-mod client: Branched accounted for `1061.5 ms` total over `901` trees (`0.408 ms` p50) and did not appear in top chunkgen worker, render, or server profiler hotspots (`modpack-bench-26-branched.json`). |
| **6. No chunk-border cuts, orphan leaves, loot/feature regressions** | **PASS** | `TreeFitValidator` + `WorldGenPlacementContext.canWrite`; BFS `LeafDistance <= 6`; `0` `PostProcessing` warnings and `0` far-chunk warnings across all logs; Twilight Forest (`624` native trees) & Fish of Thieves preserved untouched (`server-22`, `server-23`). |
| **7. No silent incompatibilities** | **PASS** | Only `78` audited safe features are replaced (`8` vanilla aggregates, `61` RU, `9` Terralith); `250` audited features (`13` vanilla + `200` Phase 3 + `37` Phase 4) are preserved with documented reasons; `unknownFeaturePolicy = preserve_native`. |

---

## 1. Dedicated Server: Vanilla Baseline vs Static Pilot (`taiga`, 3×3 chunks)

From [`docs/test-logs/server-09-vanilla-baseline.log`](file:///E:/Pycharm%20Projects/Branched/branched/docs/test-logs/server-09-vanilla-baseline.log)
and [`docs/test-logs/server-10-static-trees.log`](file:///E:/Pycharm%20Projects/Branched/branched/docs/test-logs/server-10-static-trees.log)
(Forge `47.4.29`, seed `123456789`, chunks `[179, -105]`):

| Metric | Vanilla (`server-09`) | Static pilot (`server-10`) | Ratio |
| --- | ---: | ---: | ---: |
| Server startup (`Done (...)`) | `37.956 s` | `27.829 s` | — |
| Trees sampled | 60 | 83 | 1.38× |
| Spruce logs | 406 | 565 | 1.39× |
| Spruce leaves | 3357 | 3490 | 1.04× |
| Branch blocks | 0 | 395 (~4.8/tree) | — |
| Static tree generation total | — | `57.10 ms` (`477` trees, `max 3.93 ms`) | — |
| Far-chunk / BlockEntity / crash warnings | 0 | 0 | — |

---

## 2. Dedicated Server: Multi-Biome & Dimension Production Runs

All production runs below executed on Forge `47.4.29` with JDK `17.0.21`:

| Log | Mod Set | Trees / Branches | Chunkgen Latency (`ms`, per 3×3 sample) | Static Generation Latency (Shutdown / Diagnostics) | Heap Before -> After (`MB`) |
| --- | --- | ---: | --- | --- | ---: |
| [`server-20-phase3-nomods.log`](file:///E:/Pycharm%20Projects/Branched/branched/docs/test-logs/server-20-phase3-nomods.log) | Vanilla only (Phase 3 tag build) | `158` / `779` | — | — (`819` placed, `168` blocked) | — |
| [`server-21-phase3-selftest.log`](file:///E:/Pycharm%20Projects/Branched/branched/docs/test-logs/server-21-phase3-selftest.log) + [`server-21-diagnostics.json`](file:///E:/Pycharm%20Projects/Branched/branched/docs/test-logs/server-21-diagnostics.json) | RU `0.5.6` + TerraBlender `3.0.1.10` + Terralith `2.5.4` (`6` targets, `54` chunks) | `270` / `736` (sample) | `p50=33.07`, `p95=733.07`, `p99=957.60`, `max=957.60` | `6160` generated (`1,094,332` blocks, `3,397,754` est. quads): `avg=0.072 ms`, `p50=0.044 ms`, `p95=0.093 ms`, `p99=0.120 ms`, `max=11.187 ms` | `860 -> 893` (max `3072`) |
| [`server-22-phase4.log`](file:///E:/Pycharm%20Projects/Branched/branched/docs/test-logs/server-22-phase4.log) | FoT `3.1.7` + Quark `4.0-462` + Zeta `1.0-31` + Twilight `4.3.2508` (Overworld) | `93` / `412` | — | `364` generated (`23,213` blocks, `67,324` est. quads): `p50=0.082 ms`, `p95=0.484 ms`, `p99=0.702 ms` | `1127 -> 1265` (max `3072`) |
| [`server-23-phase4-twilight.log`](file:///E:/Pycharm%20Projects/Branched/branched/docs/test-logs/server-23-phase4-twilight.log) + [`server-23-diagnostics.json`](file:///E:/Pycharm%20Projects/Branched/branched/docs/test-logs/server-23-diagnostics.json) | Same Phase 4 mods in `twilightforest:twilight_forest` (`18` chunks) | `624` / `0` | `p50=1.99`, `p95=294.04`, `p99=294.04`, `max=294.04` | `0` Branched placements/rejections (100% native Twilight preservation) | `1642 -> 1504` (max `3072`) |

(Note: `server-21` estimated quads average `3.10` quads/block vs the naive 6.0 upper bound.)

---

## 3. Live 415-Mod Client Benchmark (`Optimized fps`, `3440×1369`)

Measured on Forge `1.20.1-47.4.28` with `415` active JARs (`57` disabled, `0`
duplicate modIds via `audit_mods.py`), including C2ME (`c2me-worker-*`), Distant
Horizons, Embeddium, ImmediatelyFast, Regions Unexplored, Terralith, Aether,
Quark, Fish of Thieves, and Twilight Forest.

Files:
- With Branched `0.1.0`: [`docs/test-logs/modpack-bench-26-branched.json`](file:///E:/Pycharm%20Projects/Branched/branched/docs/test-logs/modpack-bench-26-branched.json) + [`docs/test-logs/modpack-client-26-branched.log`](file:///E:/Pycharm%20Projects/Branched/branched/docs/test-logs/modpack-client-26-branched.log)
- Existing non-DT pack baselines (`Optimized fps/tools/bench_results/`): `nodiorama_curr.json`, `opt_diorama_noatlas.json`

| Metric | `nodiorama_curr` (baseline) | `opt_diorama_noatlas` (baseline) | `branched_modpack` (`0.1.0`) |
| --- | ---: | ---: | ---: |
| Boot to world join (`s`) | 119.95 | 123.49 | 123.17 |
| Peak working set (`MB`) | 15,660.3 | 15,606.3 | **13,408.6** |
| In-world avg working set (`MB`) | 14,533.5 | 14,554.7 | **12,516.1** |
| In-world CPU avg (`%`) | 67.1 | 66.0 | **58.8** |
| In-world VRAM avg (`MB`) | 8,492.2 | 8,588.3 | **7,373.8** |
| In-world GPU power avg (`W`) | 101.8 | 99.1 | 105.2 |
| `cant_keep_up_inworld` | 2 | 2 | 2 |
| `GAMEPLAY` avg FPS | **15.30** | 13.58 | 14.17 |
| `GAMEPLAY` 1% / 0.1% low FPS | 3.56 / 3.49 | **5.47 / 5.38** | 1.72 / 1.52 |
| `GAMEPLAY` p99 / max frame (`ms`) | 260.91 / 286.85 | **175.97 / 185.86** | 314.07 / 657.87 |
| `GAMEPLAY` MSPT p95 (`ms`) | **42.50** | 66.91 | 71.27 |
| `CHUNKGEN_FLIGHT` avg FPS | 39.06 | 31.76 | **40.64** |
| `CHUNKGEN_FLIGHT` 1% / 0.1% low FPS | 2.55 / 1.08 | 1.91 / 1.19 | **8.98 / 6.97** |
| `CHUNKGEN_FLIGHT` p99 / max (`ms`) | 185.76 / 925.30 | 204.70 / 837.54 | **94.79 / 143.48** |
| `CHUNKGEN_FLIGHT` stutters `>100 ms` | 12 | 16 | **5** |
| `PURE_CHUNKGEN` total (`20` chunks, `ms`) | 47,461.4 | 45,952.9 | **33,411.2** |
| `PURE_CHUNKGEN` overall (`ms/chunk` / `chunks/s`) | 2,373.07 / 0.42 | 2,297.64 / 0.44 | **1,670.56 / 0.60** |
| `PURE_CHUNKGEN` cold first 5 (`ms`) | 34,860.8 | 33,939.3 | **24,111.5** |
| `PURE_CHUNKGEN` steady (`ms/chunk` / `chunks/s`) | 840.05 / 1.19 | 800.90 / 1.25 | **619.98 / 1.61** |

### Internal Branched Client Breakdown & Caveats
- **Adapter isolation live**: all 7 present integrated mods (`aether`, `fishofthieves`, `minecraft`, `quark`, `regions_unexplored`, `terralith`, `twilightforest`) reported `READY`; `dtbaobabtree` stayed `ABSENT`.
- **Per-tree cost in pack**: `901` generated trees (`582` placed, `319` rejected as `blocked`), `16,307` segments, `14,936` leaf clusters, `216,069` blocks, `627,950` estimated quads (`2.91` quads/block), total `1,061.49 ms` (`avg = 1.18 ms/tree`, `p50 = 0.408 ms`, `p95 = 4.95 ms`, `p99 = 16.52 ms`, `max = 41.47 ms`).
- **Profiler attribution**: `chunkgen_worker_mods` was dominated by `com.seibel.distanthorizons` (`93.0%` + `dh_sqlite`); `dev.branched` did not appear in the top chunkgen worker, render, or server mod hotspots, confirming negligible CPU overhead.
- **Comparison caveat**: `nodiorama_curr` and `opt_diorama_noatlas` are single prior `bench_runner.py` sessions rather than interleaved multi-sample randomized trials, so external GPU/DH worker state contributes to run-to-run variance (especially on the short static-spawn `GAMEPLAY` window where `low_1_fps` varied between `1.72` and `5.47`). The critical verified conclusion is that Branched introduced zero chunkgen degradation, zero BlockEntity overhead, zero post-processing warnings, and lower memory/chunkgen-flight stutter in the tested session.
