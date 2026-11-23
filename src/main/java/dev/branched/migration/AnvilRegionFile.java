package dev.branched.migration;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Pure-Java reader and writer for Minecraft Anvil ({@code .mca}) region files.
 *
 * <p>Each file starts with a 4 KiB location table (1024 32-bit entries) and a
 * 4 KiB timestamp table, followed by 4 KiB-aligned compressed chunk records.</p>
 */
public final class AnvilRegionFile {
    public static final int SECTOR_BYTES = 4096;
    public static final int HEADER_SLOTS = 1024;
    public static final int HEADER_BYTES = SECTOR_BYTES * 2;
    public static final byte COMPRESSION_GZIP = 1;
    public static final byte COMPRESSION_ZLIB = 2;
    public static final byte COMPRESSION_NONE = 3;

    public record ChunkRecord(int localX,
                              int localZ,
                              int timestamp,
                              byte compressionType,
                              String rootName,
                              NbtTag.CompoundTag nbt) {
        public int slotIndex() {
            return (localX & 31) + (localZ & 31) * 32;
        }
    }

    private AnvilRegionFile() {
    }

    public static List<ChunkRecord> read(Path mcaPath) throws IOException {
        byte[] raw = Files.readAllBytes(mcaPath);
        if (raw.length == 0) {
            return List.of();
        }
        if (raw.length < HEADER_BYTES) {
            throw new IOException("Truncated .mca header (" + raw.length + " bytes): " + mcaPath);
        }
        ByteBuffer buf = ByteBuffer.wrap(raw);
        int[] locations = new int[HEADER_SLOTS];
        int[] timestamps = new int[HEADER_SLOTS];
        for (int i = 0; i < HEADER_SLOTS; i++) {
            locations[i] = buf.getInt(i * 4);
            timestamps[i] = buf.getInt(SECTOR_BYTES + i * 4);
        }

        List<ChunkRecord> chunks = new ArrayList<>();
        for (int idx = 0; idx < HEADER_SLOTS; idx++) {
            int loc = locations[idx];
            if (loc == 0) {
                continue;
            }
            int sectorOffset = loc >>> 8;
            int sectorCount = loc & 0xFF;
            if (sectorOffset < 2 || sectorCount == 0) {
                continue;
            }
            int byteOffset = sectorOffset * SECTOR_BYTES;
            if (byteOffset + 5 > raw.length) {
                throw new IOException("Chunk offset out of bounds in " + mcaPath + " slot " + idx);
            }
            int length = buf.getInt(byteOffset);
            if (length <= 1 || byteOffset + 4 + length > raw.length) {
                throw new IOException("Invalid chunk length " + length + " in " + mcaPath + " slot " + idx);
            }
            byte compression = raw[byteOffset + 4];
            if ((compression & 0x80) != 0) {
                throw new IOException("External .mcc chunk payload not supported in " + mcaPath + " slot " + idx);
            }
            byte[] payload = new byte[length - 1];
            System.arraycopy(raw, byteOffset + 5, payload, 0, payload.length);
            NbtIo.NamedCompound named = decodePayload(compression, payload);
            int localX = idx & 31;
            int localZ = idx >> 5;
            chunks.add(new ChunkRecord(localX, localZ, timestamps[idx], compression, named.name(), named.root()));
        }
        return chunks;
    }

    public static void write(Path mcaPath, List<ChunkRecord> chunks) throws IOException {
        if (mcaPath.getParent() != null) {
            Files.createDirectories(mcaPath.getParent());
        }
        int[] locations = new int[HEADER_SLOTS];
        int[] timestamps = new int[HEADER_SLOTS];
        ByteArrayOutputStream sectorStream = new ByteArrayOutputStream();
        int nextSector = 2;

        for (ChunkRecord chunk : chunks) {
            byte compression = chunk.compressionType() == 0 ? COMPRESSION_ZLIB : chunk.compressionType();
            byte[] compressed = encodePayload(compression, chunk.rootName(), chunk.nbt());
            int totalBytes = 4 + 1 + compressed.length;
            int sectors = (totalBytes + SECTOR_BYTES - 1) / SECTOR_BYTES;
            if (sectors > 255) {
                throw new IOException("Chunk exceeds 255-sector Anvil limit at ("
                        + chunk.localX() + "," + chunk.localZ() + "): " + totalBytes + " bytes");
            }
            int slot = chunk.slotIndex();
            locations[slot] = (nextSector << 8) | sectors;
            timestamps[slot] = chunk.timestamp();

            ByteArrayOutputStream record = new ByteArrayOutputStream(sectors * SECTOR_BYTES);
            try (DataOutputStream out = new DataOutputStream(record)) {
                out.writeInt(1 + compressed.length);
                out.writeByte(compression);
                out.write(compressed);
                int remainder = sectors * SECTOR_BYTES - totalBytes;
                if (remainder > 0) {
                    out.write(new byte[remainder]);
                }
            }
            sectorStream.write(record.toByteArray());
            nextSector += sectors;
        }

        ByteBuffer header = ByteBuffer.allocate(HEADER_BYTES);
        for (int i = 0; i < HEADER_SLOTS; i++) {
            header.putInt(i * 4, locations[i]);
            header.putInt(SECTOR_BYTES + i * 4, timestamps[i]);
        }
        byte[] sectorBytes = sectorStream.toByteArray();
        byte[] full = new byte[HEADER_BYTES + sectorBytes.length];
        System.arraycopy(header.array(), 0, full, 0, HEADER_BYTES);
        System.arraycopy(sectorBytes, 0, full, HEADER_BYTES, sectorBytes.length);
        Files.write(mcaPath, full);
    }

    private static NbtIo.NamedCompound decodePayload(byte compression, byte[] payload) throws IOException {
        return switch (compression) {
            case COMPRESSION_GZIP -> NbtIo.readGzip(payload);
            case COMPRESSION_ZLIB -> NbtIo.readZlib(payload);
            case COMPRESSION_NONE -> NbtIo.readUncompressed(payload);
            default -> throw new IOException("Unsupported Anvil compression type: " + compression);
        };
    }

    private static byte[] encodePayload(byte compression, String rootName, NbtTag.CompoundTag root)
            throws IOException {
        return switch (compression) {
            case COMPRESSION_GZIP -> NbtIo.writeGzip(rootName, root);
            case COMPRESSION_ZLIB -> NbtIo.writeZlib(rootName, root);
            case COMPRESSION_NONE -> NbtIo.writeUncompressed(rootName, root);
            default -> throw new IOException("Unsupported Anvil compression type: " + compression);
        };
    }
}
