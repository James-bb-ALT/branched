package dev.branched.migration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Phase 7 verification: dry-run, mandatory backup, Anvil .mca + NBT palette
 * conversion (including SimpleBitStorage deduplication/repacking), inventory
 * and special-tree preservation, and SHA-256 verified rollback.
 */
class Phase7MigrationTest {

    @Test
    void dryRunLeavesRegionFileByteIdenticalAndCountsAllCategories(@TempDir Path tempDir) throws Exception {
        Path worldDir = tempDir.resolve("world");
        Path mca = worldDir.resolve("region/r.0.0.mca");
        AnvilRegionFile.write(mca, List.of(sampleDynamicTreesChunk()));
        String beforeSha = WorldMigrationTool.sha256(mca);

        WorldMigrationTool.MigrationReport report = WorldMigrationTool.run(new WorldMigrationTool.Options(
                WorldMigrationTool.Mode.DRY_RUN, worldDir, null, null, true));

        assertEquals(beforeSha, WorldMigrationTool.sha256(mca), "dry-run must never modify region files");
        assertEquals(1, report.regionFilesScanned());
        assertEquals(1, report.regionFilesModified());
        assertEquals(1, report.chunksScanned());
        assertEquals(1, report.chunksModified());
        assertEquals(10, report.paletteEntriesConverted());
        assertEquals(1, report.specialEntriesPreserved());
        assertEquals(1, report.tileEntitiesRemoved());
        assertEquals(1, report.tileEntitiesPreserved());
    }

    @Test
    void applyCreatesBackupConvertsDynamicTreesPreservesInventoryAndSpecialTrees(@TempDir Path tempDir)
            throws Exception {
        Path worldDir = tempDir.resolve("world");
        Path backupDir = tempDir.resolve("backup");
        Path mca = worldDir.resolve("region/r.0.0.mca");
        AnvilRegionFile.write(mca, List.of(sampleDynamicTreesChunk()));
        String preSha = WorldMigrationTool.sha256(mca);

        WorldMigrationTool.MigrationReport applyReport = WorldMigrationTool.run(new WorldMigrationTool.Options(
                WorldMigrationTool.Mode.APPLY, worldDir, backupDir, null, true));

        assertEquals(1, applyReport.backedUpFiles().size());
        assertTrue(Files.isRegularFile(backupDir.resolve(WorldMigrationTool.MANIFEST_FILE)));
        assertEquals(preSha, WorldMigrationTool.sha256(backupDir.resolve("region/r.0.0.mca")));
        assertNotEquals(preSha, WorldMigrationTool.sha256(mca));

        List<AnvilRegionFile.ChunkRecord> migratedChunks = AnvilRegionFile.read(mca);
        assertEquals(1, migratedChunks.size());
        NbtTag.CompoundTag chunk = migratedChunks.get(0).nbt();

        NbtTag.CompoundTag section = (NbtTag.CompoundTag) chunk.getList("sections").elements().get(0);
        NbtTag.CompoundTag blockStates = section.getCompound("block_states");
        List<NbtTag> palette = blockStates.getList("palette").elements();
        // Two thin oak branches collapse to one branched:oak_branch[shape=core]
        // and oak_root collapses to minecraft:air (13 -> 11 unique entries).
        assertEquals(11, palette.size());

        Map<String, String> nameToProps = new java.util.LinkedHashMap<>();
        List<String> names = new java.util.ArrayList<>();
        for (NbtTag entry : palette) {
            NbtTag.CompoundTag state = (NbtTag.CompoundTag) entry;
            names.add(state.getString("Name"));
            NbtTag.CompoundTag props = state.getCompound("Properties");
            nameToProps.put(state.getString("Name"), props == null ? "" : props.entries().toString());
        }

        assertTrue(names.contains("minecraft:air"));
        assertTrue(names.contains("branched:oak_branch"));
        assertTrue(names.contains("branched:spruce_branch"));
        assertTrue(names.contains("minecraft:oak_log"));
        assertTrue(names.contains("minecraft:stripped_oak_log"));
        assertTrue(names.contains("regions_unexplored:maple_log"));
        assertTrue(names.contains("minecraft:oak_leaves"));
        assertTrue(names.contains("minecraft:dirt"));
        assertTrue(names.contains("minecraft:sand"));
        assertTrue(names.contains("dttwilightforest:time_branch"), "special magic tree must stay preserved");
        assertFalse(names.contains("dynamictrees:oak_branch"));
        assertFalse(names.contains("dynamictrees:rooty_dirt"));
        assertFalse(names.contains("dynamictrees:oak_root"));

        // Verify repacked block_states.data length matches 4 bits/block -> 256 longs.
        NbtTag.LongArrayTag repacked = (NbtTag.LongArrayTag) blockStates.get("data");
        assertEquals(256, repacked.value().length);
        // Block index 1 was old palette index 2 (oak_branch radius=4), which deduplicated to index 1 (branched:oak_branch).
        int decodedIndex1 = (int) ((repacked.value()[0] >>> 4) & 0xFL);
        assertEquals("branched:oak_branch", ((NbtTag.CompoundTag) palette.get(decodedIndex1)).getString("Name"));

        // Verify chest inventory was preserved untouched while DT tile entity was removed.
        NbtTag.ListTag tiles = chunk.getList("block_entities");
        assertEquals(1, tiles.size());
        NbtTag.CompoundTag chest = (NbtTag.CompoundTag) tiles.elements().get(0);
        assertEquals("minecraft:chest", chest.getString("id"));
        NbtTag.CompoundTag item = (NbtTag.CompoundTag) chest.getList("Items").elements().get(0);
        assertEquals("minecraft:diamond", item.getString("id"));
        assertEquals(64, item.getInt("Count", 0));

        // Verify rollback restores byte-identical pre-migration .mca.
        WorldMigrationTool.MigrationReport rollbackReport = WorldMigrationTool.run(new WorldMigrationTool.Options(
                WorldMigrationTool.Mode.ROLLBACK, worldDir, backupDir, null, true));
        assertEquals(1, rollbackReport.restoredFiles().size());
        assertEquals(preSha, WorldMigrationTool.sha256(mca));
    }

    @Test
    void nbtCompoundPreservesInsertionOrderAndRoundTripsEmptyStructure() throws Exception {
        NbtTag.CompoundTag structure = new NbtTag.CompoundTag();
        structure.putInt("DataVersion", 3465);
        NbtTag.ListTag size = new NbtTag.ListTag(NbtTag.TAG_INT, List.of(
                new NbtTag.IntTag(5), new NbtTag.IntTag(5), new NbtTag.IntTag(5)));
        structure.put("size", size);
        NbtTag.CompoundTag air = new NbtTag.CompoundTag();
        air.putString("Name", "minecraft:air");
        structure.put("palette", new NbtTag.ListTag(NbtTag.TAG_COMPOUND, List.of(air)));
        structure.put("blocks", new NbtTag.ListTag());
        structure.put("entities", new NbtTag.ListTag());

        byte[] gz = NbtIo.writeGzip("", structure);
        NbtIo.NamedCompound roundtrip = NbtIo.readGzip(gz);
        assertEquals(structure, roundtrip.root());
        assertNull(roundtrip.root().getCompound("missing"));

        NbtIo.NamedCompound shippedEmpty = NbtIo.readCompressed(
                Path.of("src/main/resources/data/branched/structures/empty.nbt"));
        assertEquals(3465, shippedEmpty.root().getInt("DataVersion", 0));
        assertEquals(3, shippedEmpty.root().getList("size").size());
    }

    private static AnvilRegionFile.ChunkRecord sampleDynamicTreesChunk() {
        NbtTag.CompoundTag chunk = new NbtTag.CompoundTag();
        chunk.putInt("DataVersion", 3465);
        chunk.putInt("xPos", 0);
        chunk.putInt("zPos", 0);
        chunk.putInt("yPos", -4);

        NbtTag.ListTag palette = new NbtTag.ListTag(NbtTag.TAG_COMPOUND, List.of(
                state("minecraft:air", Map.of()),
                state("dynamictrees:oak_branch", Map.of("radius", "3")),
                state("dynamictrees:oak_branch", Map.of("radius", "4")),
                state("dynamictrees:spruce_branch", Map.of("radius", "5")),
                state("dynamictrees:oak_branch", Map.of("radius", "10")),
                state("dynamictrees:stripped_oak_branch", Map.of("radius", "8")),
                state("dtru:maple_branch", Map.of("radius", "12")),
                state("dynamictrees:oak_leaves", Map.of("distance", "3", "hydro", "2")),
                state("dynamictrees:rooty_dirt", Map.of("fertility", "8")),
                state("dynamictrees:rooty_sand", Map.of("fertility", "4")),
                state("dynamictrees:oak_root", Map.of("radius", "3")),
                state("dttwilightforest:time_branch", Map.of("radius", "8")),
                state("minecraft:stone", Map.of())));

        // 4 bits per block, 16 blocks per long -> 256 longs.
        long[] data = new long[256];
        // Put index 2 (dynamictrees:oak_branch[radius=4]) at block position k=1.
        data[0] = (2L << 4) | (1L);

        NbtTag.CompoundTag blockStates = new NbtTag.CompoundTag();
        blockStates.put("palette", palette);
        blockStates.put("data", new NbtTag.LongArrayTag(data));

        NbtTag.CompoundTag section = new NbtTag.CompoundTag();
        section.putByte("Y", (byte) 4);
        section.put("block_states", blockStates);
        chunk.put("sections", new NbtTag.ListTag(NbtTag.TAG_COMPOUND, List.of(section)));

        NbtTag.CompoundTag chest = new NbtTag.CompoundTag();
        chest.putString("id", "minecraft:chest");
        chest.putInt("x", 4);
        chest.putInt("y", 64);
        chest.putInt("z", 4);
        NbtTag.CompoundTag diamond = new NbtTag.CompoundTag();
        diamond.putByte("Slot", (byte) 0);
        diamond.putString("id", "minecraft:diamond");
        diamond.putByte("Count", (byte) 64);
        chest.put("Items", new NbtTag.ListTag(NbtTag.TAG_COMPOUND, List.of(diamond)));

        NbtTag.CompoundTag dtTile = new NbtTag.CompoundTag();
        dtTile.putString("id", "dynamictrees:species_tile");
        dtTile.putInt("x", 8);
        dtTile.putInt("y", 63);
        dtTile.putInt("z", 8);

        chunk.put("block_entities", new NbtTag.ListTag(NbtTag.TAG_COMPOUND, List.of(chest, dtTile)));
        return new AnvilRegionFile.ChunkRecord(0, 0, 1700000000, AnvilRegionFile.COMPRESSION_ZLIB, "", chunk);
    }

    private static NbtTag.CompoundTag state(String name, Map<String, String> propsMap) {
        NbtTag.CompoundTag tag = new NbtTag.CompoundTag();
        tag.putString("Name", name);
        if (!propsMap.isEmpty()) {
            NbtTag.CompoundTag props = new NbtTag.CompoundTag();
            propsMap.forEach(props::putString);
            tag.put("Properties", props);
        }
        return tag;
    }
}
