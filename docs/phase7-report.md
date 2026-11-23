# Phase 7 Report — Opt-In Offline Dynamic Trees Migration Tool

Phase 7 provides a standalone, opt-in **offline** Anvil (`.mca`) + binary NBT
migration utility for worlds previously generated with Dynamic Trees (`1.20.1`).
Branched never touches existing chunks automatically (`replace_existing_chunks = false`).

## Safety Guarantees

1. **Opt-in only**: runs exclusively via explicit CLI invocation (`--dry-run`, `--apply`, or `--rollback`).
2. **No Dynamic Trees dependency**: implemented in pure Java (`dev.branched.migration.*`) with zero DT JARs or classes required.
3. **Mandatory pre-migration SHA-256 backup**: `--apply` refuses to run without `--backup <dir>`, copies every `.mca` file into `<backupDir>/region/...` before touching the save, and writes `migration-manifest.json` storing per-file SHA-256 hashes.
4. **Verified rollback**: `--rollback` validates every backup file's SHA-256 against `migration-manifest.json` before restoring it byte-for-byte over the world directory.
5. **Chest/tile-entity & special-tree preservation**: only Dynamic Trees namespaces are removed from `block_entities` (`inventories`, vanilla/other-mod chests remain untouched), and paths matching `SPECIAL_KEYWORDS` (`mining`, `sorting`, `time`, `transformation`, `crystal`, `holiday`, `hollow`, `alpha`) stay preserved by default (`--no-preserve-special` to override).

## Architecture (`dev.branched.migration`)

- `NbtTag` & `NbtIo`: full pure-Java binary NBT reader/writer supporting GZIP, ZLIB, and uncompressed chunk/structure payloads.
- `AnvilRegionFile`: pure-Java `.mca` region parser/writer handling 4 KiB location and timestamp header tables and 4 KiB-aligned sectors.
- `DynamicTreesBlockMapper`: recognizes 10 Dynamic Trees namespaces (`dynamictrees`, `dtru`, `dtterralith`, `dtaether`, `dtquark`, `dtfish_of_thieves`, `dttwilightforest`, `dynamictreesplus`, `dtbaobabtree`, `dtaddon_lib`) and maps:
  - `rooty_*` -> native soils (`dirt`, `coarse_dirt`, `grass_block`, `podzol`, `mycelium`, `sand`, `red_sand`, `mud`, `moss_block`, `water`, `netherrack`, `soul_soil`)
  - `*_root` (aerial/surface roots) -> `minecraft:air`
  - `*_branch` (including `thick_*` / `stripped_*`) -> `StaticBranchBlock` (`branched:oak_branch`, `spruce_branch`, `acacia_branch` with `shape=core` when unstripped and `radius < 8` with matching branch material) or native `*_log` (`axis=y`)
  - `*_leaves` / `*_fronds` -> native leaves (`distance=1, persistent=false, waterlogged=false`)
  - `*_seed` / `*_sapling` -> native saplings (or `minecraft:air` where no block sapling exists, e.g., banana frond seeds)
- `WorldMigrationTool`: scans chunk `sections[].block_states.palette`, deduplicates collapsed palette entries, repacks `SimpleBitStorage` `long[]` bit arrays cleanly (removing `data` when `palette.size() <= 1`, resizing `4..N` bits/entry when bit-width changes), cleans DT `block_entities`, and emits JSON reports.

## CLI Usage

```bash
# 1. Inspect a world read-only (zero writes)
java -cp "<classes>;<gson.jar>" dev.branched.migration.WorldMigrationTool ^
  --dry-run --world "<save_dir>" --report dryrun.json

# 2. Apply migration with mandatory SHA-256 backup
java -cp "<classes>;<gson.jar>" dev.branched.migration.WorldMigrationTool ^
  --apply --world "<save_dir>" --backup "<backup_dir>" --report apply.json

# 3. Restore byte-identical pre-migration .mca files
java -cp "<classes>;<gson.jar>" dev.branched.migration.WorldMigrationTool ^
  --rollback --world "<save_dir>" --backup "<backup_dir>" --report rollback.json
```

Wrappers are also provided via `./gradlew migrateWorld --args="..."` and
`python tools/migrate_dt_world.py ...` (on Windows PowerShell, prefer direct
`java -cp ...` if `--world` contains spaces such as `Optimized fps`).

## Verification Results

### Unit & Integration Suite (`Phase7MigrationTest`, 3/3 PASSED)
Recorded in [`docs/test-logs/test-24-final.log`](file:///E:/Pycharm%20Projects/Branched/branched/docs/test-logs/test-24-final.log):
- `dryRunLeavesRegionFileByteIdenticalAndCountsAllCategories`
- `applyCreatesBackupConvertsDynamicTreesPreservesInventoryAndSpecialTrees` (verifies `palette` collapse `13 -> 11`, `SimpleBitStorage` `4`-bit repack over `256` longs, `minecraft:chest` + `minecraft:diamond` `64` preservation, `dttwilightforest:time_branch` preservation, `dynamictrees:species_tile` removal, and byte-identical SHA-256 rollback)
- `nbtCompoundPreservesInsertionOrderAndRoundTripsEmptyStructure` (including GZIP round-trip of `data/branched/structures/empty.nbt`)

### Real Modpack World Validation (`Optimized fps/saves/New World (27)`)

Tested against the user's real existing Dynamic Trees save `New World (27)`:

1. **Read-only `--dry-run` on live save** ([`docs/test-logs/modpack-migration-dryrun.json`](file:///E:/Pycharm%20Projects/Branched/branched/docs/test-logs/modpack-migration-dryrun.json)):
   - `regionFilesScanned`: `8`, `regionFilesModified`: `4`
   - `chunksScanned`: `1958`, `chunksModified`: `371`
   - `paletteEntriesConverted`: `8399` (`leaves=3148`, `branch=244`, `trunk=4186`, `rooty_soil=362`, `aerial_root=459`)
   - `tileEntitiesRemoved`: `113`, `tileEntitiesPreserved`: `1361`
2. **Temporary copy `--apply` + post-apply audit + `--rollback`** ([`docs/test-logs/modpack-migration-apply.json`](file:///E:/Pycharm%20Projects/Branched/branched/docs/test-logs/modpack-migration-apply.json), [`docs/test-logs/modpack-migration-rollback.json`](file:///E:/Pycharm%20Projects/Branched/branched/docs/test-logs/modpack-migration-rollback.json)):
   - `r.0.0.mca` SHA-256 before `--apply`: `0FE38E81A17DD752823C98CC139EC5D1FB60ECD5064B3E4CBDC88B27FB7AFC8E`
   - `r.0.0.mca` SHA-256 after `--apply`: `8E161CF0B83E935E2A5DF0684D9F418408DA8F90D3AB57A616A25180B48E987D`
   - Post-apply `--dry-run` reported `chunksModified = 0`, `paletteEntriesConverted = 0`, `tileEntitiesRemoved = 0`, and `tileEntitiesPreserved = 1361`.
   - `--rollback` restored all `4` modified `.mca` files to exact original SHA-256 (`r.0.0.mca` = `0FE38E81A17DD752823C98CC139EC5D1FB60ECD5064B3E4CBDC88B27FB7AFC8E`); `New World (27)` inside the modpack was left untouched.
