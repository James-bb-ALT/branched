package dev.branched.migration;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

/**
 * Opt-in offline migration utility (Phase 7) for converting saves previously
 * generated with Dynamic Trees into Branched / native blocks.
 *
 * <p>Never runs automatically: requires explicit invocation in {@link Mode#DRY_RUN},
 * {@link Mode#APPLY} (with mandatory pre-migration SHA-256 backup), or
 * {@link Mode#ROLLBACK}.</p>
 */
public final class WorldMigrationTool {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String MANIFEST_FILE = "migration-manifest.json";

    public enum Mode {
        DRY_RUN,
        APPLY,
        ROLLBACK
    }

    public record Options(Mode mode,
                          Path worldDir,
                          Path backupDir,
                          Path reportPath,
                          boolean preserveSpecialTrees) {
        public Options {
            Objects.requireNonNull(mode, "mode");
            Objects.requireNonNull(worldDir, "worldDir");
            if (mode != Mode.DRY_RUN && backupDir == null) {
                throw new IllegalArgumentException("backupDir is mandatory for " + mode);
            }
        }
    }

    public record MigrationReport(Mode mode,
                                  Path worldDir,
                                  Path backupDir,
                                  int regionFilesScanned,
                                  int regionFilesModified,
                                  int chunksScanned,
                                  int chunksModified,
                                  int paletteEntriesConverted,
                                  int specialEntriesPreserved,
                                  int tileEntitiesRemoved,
                                  int tileEntitiesPreserved,
                                  Map<String, Integer> conversionsByCategory,
                                  List<String> backedUpFiles,
                                  List<String> restoredFiles) {
        public String toJson() {
            JsonObject root = new JsonObject();
            root.addProperty("mode", mode.name());
            root.addProperty("timestamp", Instant.now().toString());
            root.addProperty("worldDir", worldDir.toAbsolutePath().toString());
            if (backupDir != null) {
                root.addProperty("backupDir", backupDir.toAbsolutePath().toString());
            }
            root.addProperty("regionFilesScanned", regionFilesScanned);
            root.addProperty("regionFilesModified", regionFilesModified);
            root.addProperty("chunksScanned", chunksScanned);
            root.addProperty("chunksModified", chunksModified);
            root.addProperty("paletteEntriesConverted", paletteEntriesConverted);
            root.addProperty("specialEntriesPreserved", specialEntriesPreserved);
            root.addProperty("tileEntitiesRemoved", tileEntitiesRemoved);
            root.addProperty("tileEntitiesPreserved", tileEntitiesPreserved);

            JsonObject categories = new JsonObject();
            conversionsByCategory.forEach(categories::addProperty);
            root.add("conversionsByCategory", categories);

            JsonArray backups = new JsonArray();
            backedUpFiles.forEach(backups::add);
            root.add("backedUpFiles", backups);

            JsonArray restored = new JsonArray();
            restoredFiles.forEach(restored::add);
            root.add("restoredFiles", restored);
            return GSON.toJson(root) + "\n";
        }
    }

    private WorldMigrationTool() {
    }

    public static void main(String[] args) throws Exception {
        Options options = parseArgs(args);
        MigrationReport report = run(options);
        String json = report.toJson();
        if (options.reportPath() != null) {
            if (options.reportPath().getParent() != null) {
                Files.createDirectories(options.reportPath().getParent());
            }
            Files.writeString(options.reportPath(), json, StandardCharsets.UTF_8);
        }
        System.out.println(json);
    }

    static Options parseArgs(String[] args) {
        Mode mode = null;
        Path worldDir = null;
        Path backupDir = null;
        Path reportPath = null;
        boolean preserveSpecial = true;

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            switch (arg) {
                case "--dry-run" -> mode = Mode.DRY_RUN;
                case "--apply" -> mode = Mode.APPLY;
                case "--rollback" -> mode = Mode.ROLLBACK;
                case "--world" -> worldDir = Path.of(requireNext(args, ++i, "--world"));
                case "--backup" -> backupDir = Path.of(requireNext(args, ++i, "--backup"));
                case "--report" -> reportPath = Path.of(requireNext(args, ++i, "--report"));
                case "--no-preserve-special" -> preserveSpecial = false;
                default -> throw new IllegalArgumentException("Unknown argument: " + arg);
            }
        }
        if (mode == null || worldDir == null) {
            throw new IllegalArgumentException(
                    "Usage: --dry-run|--apply|--rollback --world <path> [--backup <path>] [--report <path>]");
        }
        return new Options(mode, worldDir, backupDir, reportPath, preserveSpecial);
    }

    private static String requireNext(String[] args, int index, String flag) {
        if (index >= args.length) {
            throw new IllegalArgumentException("Missing value for " + flag);
        }
        return args[index];
    }

    public static MigrationReport run(Options options) throws IOException {
        if (options.mode() == Mode.ROLLBACK) {
            return rollback(options);
        }
        List<Path> regions = discoverRegionFiles(options.worldDir());
        List<String> backedUp = new ArrayList<>();
        if (options.mode() == Mode.APPLY) {
            backedUp = createMandatoryBackup(options.worldDir(), options.backupDir(), regions);
        }

        int regionsModified = 0;
        int chunksScanned = 0;
        int chunksModified = 0;
        int convertedTotal = 0;
        int specialTotal = 0;
        int tilesRemoved = 0;
        int tilesPreserved = 0;
        Map<String, Integer> byCategory = new LinkedHashMap<>();

        for (Path regionPath : regions) {
            List<AnvilRegionFile.ChunkRecord> chunks = AnvilRegionFile.read(regionPath);
            boolean regionDirty = false;
            List<AnvilRegionFile.ChunkRecord> updatedChunks = new ArrayList<>(chunks.size());

            for (AnvilRegionFile.ChunkRecord chunk : chunks) {
                chunksScanned++;
                NbtTag.CompoundTag working = options.mode() == Mode.APPLY ? chunk.nbt().copy() : chunk.nbt();
                ChunkOutcome outcome = processChunk(working, options.preserveSpecialTrees());

                convertedTotal += outcome.converted();
                specialTotal += outcome.preservedSpecial();
                tilesRemoved += outcome.tilesRemoved();
                tilesPreserved += outcome.tilesPreserved();
                outcome.categories().forEach((k, v) -> byCategory.merge(k, v, Integer::sum));

                if (outcome.dirty()) {
                    chunksModified++;
                    regionDirty = true;
                }
                updatedChunks.add(new AnvilRegionFile.ChunkRecord(
                        chunk.localX(), chunk.localZ(), chunk.timestamp(),
                        chunk.compressionType(), chunk.rootName(), working));
            }

            if (options.mode() == Mode.APPLY && regionDirty) {
                AnvilRegionFile.write(regionPath, updatedChunks);
                regionsModified++;
            } else if (options.mode() == Mode.DRY_RUN && regionDirty) {
                regionsModified++;
            }
        }

        return new MigrationReport(
                options.mode(),
                options.worldDir(),
                options.backupDir(),
                regions.size(),
                regionsModified,
                chunksScanned,
                chunksModified,
                convertedTotal,
                specialTotal,
                tilesRemoved,
                tilesPreserved,
                byCategory,
                backedUp,
                List.of());
    }

    private static List<String> createMandatoryBackup(Path worldDir, Path backupDir, List<Path> regions)
            throws IOException {
        Files.createDirectories(backupDir);
        JsonObject manifest = new JsonObject();
        manifest.addProperty("createdAt", Instant.now().toString());
        manifest.addProperty("worldDir", worldDir.toAbsolutePath().toString());
        JsonArray filesArray = new JsonArray();
        List<String> relativePaths = new ArrayList<>();

        for (Path region : regions) {
            String rel = worldDir.toAbsolutePath().relativize(region.toAbsolutePath())
                    .toString().replace('\\', '/');
            Path dest = backupDir.resolve(rel);
            if (dest.getParent() != null) {
                Files.createDirectories(dest.getParent());
            }
            Files.copy(region, dest, StandardCopyOption.REPLACE_EXISTING);
            String sha = sha256(region);
            JsonObject entry = new JsonObject();
            entry.addProperty("relativePath", rel);
            entry.addProperty("sha256", sha);
            entry.addProperty("bytes", Files.size(region));
            filesArray.add(entry);
            relativePaths.add(rel);
        }
        manifest.add("files", filesArray);
        Files.writeString(backupDir.resolve(MANIFEST_FILE), GSON.toJson(manifest) + "\n",
                StandardCharsets.UTF_8);
        return relativePaths;
    }

    private static MigrationReport rollback(Options options) throws IOException {
        Path manifestPath = options.backupDir().resolve(MANIFEST_FILE);
        if (!Files.isRegularFile(manifestPath)) {
            throw new IOException("Missing backup manifest: " + manifestPath);
        }
        JsonObject manifest = JsonParser.parseString(Files.readString(manifestPath, StandardCharsets.UTF_8))
                .getAsJsonObject();
        JsonArray files = manifest.getAsJsonArray("files");
        List<String> restored = new ArrayList<>();

        for (JsonElement element : files) {
            JsonObject entry = element.getAsJsonObject();
            String rel = entry.get("relativePath").getAsString();
            String expectedSha = entry.get("sha256").getAsString();
            Path backupFile = options.backupDir().resolve(rel);
            if (!Files.isRegularFile(backupFile)) {
                throw new IOException("Missing backup file during rollback: " + backupFile);
            }
            String backupSha = sha256(backupFile);
            if (!expectedSha.equals(backupSha)) {
                throw new IOException("Backup checksum mismatch for " + rel);
            }
            Path target = options.worldDir().resolve(rel);
            if (target.getParent() != null) {
                Files.createDirectories(target.getParent());
            }
            Files.copy(backupFile, target, StandardCopyOption.REPLACE_EXISTING);
            if (!expectedSha.equals(sha256(target))) {
                throw new IOException("Restored file checksum mismatch for " + rel);
            }
            restored.add(rel);
        }

        return new MigrationReport(
                Mode.ROLLBACK,
                options.worldDir(),
                options.backupDir(),
                restored.size(),
                restored.size(),
                0, 0, 0, 0, 0, 0,
                Map.of(),
                List.of(),
                restored);
    }

    private record ChunkOutcome(boolean dirty,
                                int converted,
                                int preservedSpecial,
                                int tilesRemoved,
                                int tilesPreserved,
                                Map<String, Integer> categories) {
    }

    private static ChunkOutcome processChunk(NbtTag.CompoundTag root, boolean preserveSpecialTrees) {
        int converted = 0;
        int preservedSpecial = 0;
        int tilesRemoved = 0;
        int tilesPreserved = 0;
        Map<String, Integer> categories = new LinkedHashMap<>();

        NbtTag.ListTag sections = root.getList("sections");
        if (sections == null) {
            NbtTag.CompoundTag legacyLevel = root.getCompound("Level");
            if (legacyLevel != null) {
                sections = legacyLevel.getList("Sections");
            }
        }
        if (sections != null) {
            for (NbtTag sectionTag : sections.elements()) {
                if (!(sectionTag instanceof NbtTag.CompoundTag section)) {
                    continue;
                }
                NbtTag.CompoundTag blockStates = section.getCompound("block_states");
                if (blockStates == null) {
                    continue;
                }
                NbtTag.ListTag palette = blockStates.getList("palette");
                if (palette == null) {
                    continue;
                }
                List<NbtTag> entries = palette.elements();
                boolean sectionModified = false;
                for (int i = 0; i < entries.size(); i++) {
                    if (!(entries.get(i) instanceof NbtTag.CompoundTag state)) {
                        continue;
                    }
                    DynamicTreesBlockMapper.MappingOutcome outcome =
                            DynamicTreesBlockMapper.map(state, preserveSpecialTrees);
                    if (outcome.action() == DynamicTreesBlockMapper.Action.CONVERTED) {
                        entries.set(i, outcome.state());
                        converted++;
                        sectionModified = true;
                        categories.merge(outcome.category(), 1, Integer::sum);
                    } else if (outcome.action() == DynamicTreesBlockMapper.Action.PRESERVED_SPECIAL) {
                        preservedSpecial++;
                    }
                }
                if (sectionModified) {
                    deduplicateAndRepack(blockStates, palette);
                }
            }
        }

        NbtTag.ListTag tiles = root.getList("block_entities");
        if (tiles != null) {
            List<NbtTag> kept = new ArrayList<>(tiles.size());
            for (NbtTag tileTag : tiles.elements()) {
                if (tileTag instanceof NbtTag.CompoundTag tile
                        && DynamicTreesBlockMapper.isDynamicTreesId(tile.getString("id"))) {
                    tilesRemoved++;
                } else {
                    kept.add(tileTag);
                    tilesPreserved++;
                }
            }
            if (tilesRemoved > 0) {
                tiles.elements().clear();
                tiles.elements().addAll(kept);
            }
        }

        boolean dirty = converted > 0 || tilesRemoved > 0;
        return new ChunkOutcome(dirty, converted, preservedSpecial, tilesRemoved, tilesPreserved, categories);
    }

    /**
     * Deduplicates identical post-migration palette entries and repacks
     * {@code block_states.data} using Minecraft 1.20.1 {@code SimpleBitStorage}
     * (4096 entries, non-spanning per 64-bit long, minimum 4 bits when size &gt; 1).
     */
    private static void deduplicateAndRepack(NbtTag.CompoundTag blockStates, NbtTag.ListTag palette) {
        List<NbtTag> oldEntries = palette.elements();
        int oldSize = oldEntries.size();
        if (oldSize <= 1) {
            blockStates.entries().remove("data");
            return;
        }
        Map<NbtTag, Integer> indexMap = new LinkedHashMap<>();
        List<NbtTag> deduped = new ArrayList<>(oldSize);
        int[] remap = new int[oldSize];
        boolean hasDuplicates = false;
        for (int i = 0; i < oldSize; i++) {
            NbtTag entry = oldEntries.get(i);
            Integer existing = indexMap.get(entry);
            if (existing == null) {
                existing = deduped.size();
                indexMap.put(entry, existing);
                deduped.add(entry);
            } else {
                hasDuplicates = true;
            }
            remap[i] = existing;
        }
        if (!hasDuplicates) {
            return;
        }
        int oldBits = Math.max(4, ceilLog2(oldSize));
        int newSize = deduped.size();
        oldEntries.clear();
        oldEntries.addAll(deduped);

        if (newSize <= 1) {
            blockStates.entries().remove("data");
            return;
        }
        NbtTag rawData = blockStates.get("data");
        if (!(rawData instanceof NbtTag.LongArrayTag longArray)) {
            return;
        }
        long[] oldData = longArray.value();
        int newBits = Math.max(4, ceilLog2(newSize));
        int oldPerLong = 64 / oldBits;
        long oldMask = (1L << oldBits) - 1L;
        int newPerLong = 64 / newBits;
        int newLongCount = (4096 + newPerLong - 1) / newPerLong;
        long[] newData = new long[newLongCount];

        for (int k = 0; k < 4096; k++) {
            int oldLongIdx = k / oldPerLong;
            int oldBitOff = (k % oldPerLong) * oldBits;
            int oldIdx = oldLongIdx < oldData.length
                    ? (int) ((oldData[oldLongIdx] >>> oldBitOff) & oldMask)
                    : 0;
            int newIdx = oldIdx < remap.length ? remap[oldIdx] : 0;
            int newLongIdx = k / newPerLong;
            int newBitOff = (k % newPerLong) * newBits;
            newData[newLongIdx] |= ((long) newIdx) << newBitOff;
        }
        blockStates.put("data", new NbtTag.LongArrayTag(newData));
    }

    private static int ceilLog2(int value) {
        if (value <= 1) {
            return 0;
        }
        return 32 - Integer.numberOfLeadingZeros(value - 1);
    }

    public static List<Path> discoverRegionFiles(Path worldDir) throws IOException {
        if (!Files.isDirectory(worldDir)) {
            return List.of();
        }
        List<Path> results = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(worldDir)) {
            stream.filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(".mca"))
                    .filter(p -> p.getParent() != null && "region".equals(p.getParent().getFileName().toString()))
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(results::add);
        }
        return results;
    }

    public static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = Files.readAllBytes(file);
            byte[] hash = digest.digest(bytes);
            StringBuilder hex = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 unavailable", ex);
        }
    }
}
