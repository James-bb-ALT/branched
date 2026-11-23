package dev.branched.migration;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.InflaterInputStream;

/**
 * Pure-Java binary NBT codec compatible with Minecraft Java 1.20.1 Anvil chunks
 * and structure templates.
 */
public final class NbtIo {
    private static final int MAX_DEPTH = 512;

    public record NamedCompound(String name, NbtTag.CompoundTag root) {
    }

    private NbtIo() {
    }

    public static NamedCompound readUncompressed(byte[] bytes) throws IOException {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            return readNamedCompound(in);
        }
    }

    public static NamedCompound readZlib(byte[] bytes) throws IOException {
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new InflaterInputStream(new ByteArrayInputStream(bytes))))) {
            return readNamedCompound(in);
        }
    }

    public static NamedCompound readGzip(byte[] bytes) throws IOException {
        try (DataInputStream in = new DataInputStream(
                new BufferedInputStream(new GZIPInputStream(new ByteArrayInputStream(bytes))))) {
            return readNamedCompound(in);
        }
    }

    public static NamedCompound readCompressed(Path path) throws IOException {
        try (InputStream raw = Files.newInputStream(path);
             DataInputStream in = new DataInputStream(new BufferedInputStream(new GZIPInputStream(raw)))) {
            return readNamedCompound(in);
        }
    }

    public static byte[] writeUncompressed(String name, NbtTag.CompoundTag root) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(baos)) {
            writeNamedCompound(out, name, root);
        }
        return baos.toByteArray();
    }

    public static byte[] writeZlib(String name, NbtTag.CompoundTag root) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new DeflaterOutputStream(baos)))) {
            writeNamedCompound(out, name, root);
        }
        return baos.toByteArray();
    }

    public static byte[] writeGzip(String name, NbtTag.CompoundTag root) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(
                new BufferedOutputStream(new GZIPOutputStream(baos)))) {
            writeNamedCompound(out, name, root);
        }
        return baos.toByteArray();
    }

    public static void writeCompressed(Path path, String name, NbtTag.CompoundTag root) throws IOException {
        if (path.getParent() != null) {
            Files.createDirectories(path.getParent());
        }
        try (OutputStream raw = Files.newOutputStream(path);
             DataOutputStream out = new DataOutputStream(new BufferedOutputStream(new GZIPOutputStream(raw)))) {
            writeNamedCompound(out, name, root);
        }
    }

    public static NamedCompound readNamedCompound(DataInput in) throws IOException {
        byte type = in.readByte();
        if (type == NbtTag.TAG_END) {
            return new NamedCompound("", new NbtTag.CompoundTag());
        }
        if (type != NbtTag.TAG_COMPOUND) {
            throw new IOException("Expected root TAG_Compound (10), got " + type);
        }
        String name = in.readUTF();
        NbtTag.CompoundTag root = (NbtTag.CompoundTag) readPayload(NbtTag.TAG_COMPOUND, in, 0);
        return new NamedCompound(name, root);
    }

    public static void writeNamedCompound(DataOutput out, String name, NbtTag.CompoundTag root) throws IOException {
        out.writeByte(NbtTag.TAG_COMPOUND);
        out.writeUTF(name == null ? "" : name);
        writePayload(root, out, 0);
    }

    private static NbtTag readPayload(byte type, DataInput in, int depth) throws IOException {
        if (depth > MAX_DEPTH) {
            throw new IOException("NBT nesting depth exceeded " + MAX_DEPTH);
        }
        return switch (type) {
            case NbtTag.TAG_END -> NbtTag.EndTag.INSTANCE;
            case NbtTag.TAG_BYTE -> new NbtTag.ByteTag(in.readByte());
            case NbtTag.TAG_SHORT -> new NbtTag.ShortTag(in.readShort());
            case NbtTag.TAG_INT -> new NbtTag.IntTag(in.readInt());
            case NbtTag.TAG_LONG -> new NbtTag.LongTag(in.readLong());
            case NbtTag.TAG_FLOAT -> new NbtTag.FloatTag(in.readFloat());
            case NbtTag.TAG_DOUBLE -> new NbtTag.DoubleTag(in.readDouble());
            case NbtTag.TAG_BYTE_ARRAY -> {
                int len = in.readInt();
                if (len < 0) throw new IOException("Negative byte array length: " + len);
                byte[] arr = new byte[len];
                in.readFully(arr);
                yield new NbtTag.ByteArrayTag(arr);
            }
            case NbtTag.TAG_STRING -> new NbtTag.StringTag(in.readUTF());
            case NbtTag.TAG_LIST -> {
                byte elemType = in.readByte();
                int len = in.readInt();
                if (len < 0) throw new IOException("Negative list length: " + len);
                if (elemType == NbtTag.TAG_END && len > 0) {
                    throw new IOException("TAG_List with TAG_End elements cannot have length " + len);
                }
                List<NbtTag> elements = new ArrayList<>(len);
                for (int i = 0; i < len; i++) {
                    elements.add(readPayload(elemType, in, depth + 1));
                }
                yield new NbtTag.ListTag(elemType, elements);
            }
            case NbtTag.TAG_COMPOUND -> {
                NbtTag.CompoundTag compound = new NbtTag.CompoundTag();
                while (true) {
                    byte childType = in.readByte();
                    if (childType == NbtTag.TAG_END) {
                        break;
                    }
                    String key = in.readUTF();
                    compound.put(key, readPayload(childType, in, depth + 1));
                }
                yield compound;
            }
            case NbtTag.TAG_INT_ARRAY -> {
                int len = in.readInt();
                if (len < 0) throw new IOException("Negative int array length: " + len);
                int[] arr = new int[len];
                for (int i = 0; i < len; i++) {
                    arr[i] = in.readInt();
                }
                yield new NbtTag.IntArrayTag(arr);
            }
            case NbtTag.TAG_LONG_ARRAY -> {
                int len = in.readInt();
                if (len < 0) throw new IOException("Negative long array length: " + len);
                long[] arr = new long[len];
                for (int i = 0; i < len; i++) {
                    arr[i] = in.readLong();
                }
                yield new NbtTag.LongArrayTag(arr);
            }
            default -> throw new IOException("Unknown NBT tag type: " + type);
        };
    }

    private static void writePayload(NbtTag tag, DataOutput out, int depth) throws IOException {
        if (depth > MAX_DEPTH) {
            throw new IOException("NBT nesting depth exceeded " + MAX_DEPTH);
        }
        if (tag instanceof NbtTag.EndTag) {
            return;
        }
        if (tag instanceof NbtTag.ByteTag b) {
            out.writeByte(b.value());
        } else if (tag instanceof NbtTag.ShortTag s) {
            out.writeShort(s.value());
        } else if (tag instanceof NbtTag.IntTag i) {
            out.writeInt(i.value());
        } else if (tag instanceof NbtTag.LongTag l) {
            out.writeLong(l.value());
        } else if (tag instanceof NbtTag.FloatTag f) {
            out.writeFloat(f.value());
        } else if (tag instanceof NbtTag.DoubleTag d) {
            out.writeDouble(d.value());
        } else if (tag instanceof NbtTag.ByteArrayTag ba) {
            byte[] arr = ba.value();
            out.writeInt(arr.length);
            out.write(arr);
        } else if (tag instanceof NbtTag.StringTag st) {
            out.writeUTF(st.value());
        } else if (tag instanceof NbtTag.ListTag list) {
            byte elemType = list.elementType();
            out.writeByte(elemType);
            out.writeInt(list.size());
            for (NbtTag element : list.elements()) {
                writePayload(element, out, depth + 1);
            }
        } else if (tag instanceof NbtTag.CompoundTag compound) {
            for (Map.Entry<String, NbtTag> entry : compound.entries().entrySet()) {
                out.writeByte(entry.getValue().typeId());
                out.writeUTF(entry.getKey());
                writePayload(entry.getValue(), out, depth + 1);
            }
            out.writeByte(NbtTag.TAG_END);
        } else if (tag instanceof NbtTag.IntArrayTag ia) {
            int[] arr = ia.value();
            out.writeInt(arr.length);
            for (int value : arr) {
                out.writeInt(value);
            }
        } else if (tag instanceof NbtTag.LongArrayTag la) {
            long[] arr = la.value();
            out.writeInt(arr.length);
            for (long value : arr) {
                out.writeLong(value);
            }
        }
    }
}
