package dev.branched.migration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Minimal, Minecraft-free Named Binary Tag (NBT) representation preserving
 * insertion order on {@link CompoundTag} entries for lossless Anvil chunk
 * roundtrips.
 */
public sealed interface NbtTag permits
        NbtTag.EndTag,
        NbtTag.ByteTag,
        NbtTag.ShortTag,
        NbtTag.IntTag,
        NbtTag.LongTag,
        NbtTag.FloatTag,
        NbtTag.DoubleTag,
        NbtTag.ByteArrayTag,
        NbtTag.StringTag,
        NbtTag.ListTag,
        NbtTag.CompoundTag,
        NbtTag.IntArrayTag,
        NbtTag.LongArrayTag {

    byte TAG_END = 0;
    byte TAG_BYTE = 1;
    byte TAG_SHORT = 2;
    byte TAG_INT = 3;
    byte TAG_LONG = 4;
    byte TAG_FLOAT = 5;
    byte TAG_DOUBLE = 6;
    byte TAG_BYTE_ARRAY = 7;
    byte TAG_STRING = 8;
    byte TAG_LIST = 9;
    byte TAG_COMPOUND = 10;
    byte TAG_INT_ARRAY = 11;
    byte TAG_LONG_ARRAY = 12;

    byte typeId();

    NbtTag copy();

    record EndTag() implements NbtTag {
        public static final EndTag INSTANCE = new EndTag();

        @Override
        public byte typeId() {
            return TAG_END;
        }

        @Override
        public NbtTag copy() {
            return this;
        }
    }

    record ByteTag(byte value) implements NbtTag {
        @Override
        public byte typeId() {
            return TAG_BYTE;
        }

        @Override
        public NbtTag copy() {
            return this;
        }
    }

    record ShortTag(short value) implements NbtTag {
        @Override
        public byte typeId() {
            return TAG_SHORT;
        }

        @Override
        public NbtTag copy() {
            return this;
        }
    }

    record IntTag(int value) implements NbtTag {
        @Override
        public byte typeId() {
            return TAG_INT;
        }

        @Override
        public NbtTag copy() {
            return this;
        }
    }

    record LongTag(long value) implements NbtTag {
        @Override
        public byte typeId() {
            return TAG_LONG;
        }

        @Override
        public NbtTag copy() {
            return this;
        }
    }

    record FloatTag(float value) implements NbtTag {
        @Override
        public byte typeId() {
            return TAG_FLOAT;
        }

        @Override
        public NbtTag copy() {
            return this;
        }
    }

    record DoubleTag(double value) implements NbtTag {
        @Override
        public byte typeId() {
            return TAG_DOUBLE;
        }

        @Override
        public NbtTag copy() {
            return this;
        }
    }

    record ByteArrayTag(byte[] value) implements NbtTag {
        public ByteArrayTag {
            value = value == null ? new byte[0] : value.clone();
        }

        @Override
        public byte typeId() {
            return TAG_BYTE_ARRAY;
        }

        @Override
        public NbtTag copy() {
            return new ByteArrayTag(value);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof ByteArrayTag other && Arrays.equals(value, other.value);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(value);
        }
    }

    record StringTag(String value) implements NbtTag {
        public StringTag {
            value = value == null ? "" : value;
        }

        @Override
        public byte typeId() {
            return TAG_STRING;
        }

        @Override
        public NbtTag copy() {
            return this;
        }
    }

    final class ListTag implements NbtTag {
        private final byte elementType;
        private final List<NbtTag> elements;

        public ListTag(byte elementType, List<NbtTag> elements) {
            this.elementType = elementType;
            this.elements = new ArrayList<>(Objects.requireNonNull(elements, "elements"));
        }

        public ListTag() {
            this(TAG_END, List.of());
        }

        public byte elementType() {
            return elements.isEmpty() ? TAG_END : elementType;
        }

        public List<NbtTag> elements() {
            return elements;
        }

        public int size() {
            return elements.size();
        }

        public void add(NbtTag tag) {
            elements.add(tag);
        }

        @Override
        public byte typeId() {
            return TAG_LIST;
        }

        @Override
        public ListTag copy() {
            List<NbtTag> copy = new ArrayList<>(elements.size());
            for (NbtTag element : elements) {
                copy.add(element.copy());
            }
            return new ListTag(elementType, copy);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof ListTag other
                    && elementType() == other.elementType()
                    && elements.equals(other.elements);
        }

        @Override
        public int hashCode() {
            return Objects.hash(elementType(), elements);
        }
    }

    final class CompoundTag implements NbtTag {
        private final Map<String, NbtTag> entries;

        public CompoundTag() {
            this.entries = new LinkedHashMap<>();
        }

        public CompoundTag(Map<String, NbtTag> entries) {
            this.entries = new LinkedHashMap<>(Objects.requireNonNull(entries, "entries"));
        }

        public Map<String, NbtTag> entries() {
            return entries;
        }

        public boolean contains(String key) {
            return entries.containsKey(key);
        }

        public NbtTag get(String key) {
            return entries.get(key);
        }

        public String getString(String key) {
            NbtTag tag = entries.get(key);
            return tag instanceof StringTag s ? s.value() : "";
        }

        public int getInt(String key, int fallback) {
            NbtTag tag = entries.get(key);
            if (tag instanceof IntTag i) return i.value();
            if (tag instanceof ByteTag b) return b.value();
            if (tag instanceof ShortTag s) return s.value();
            return fallback;
        }

        public CompoundTag getCompound(String key) {
            NbtTag tag = entries.get(key);
            return tag instanceof CompoundTag c ? c : null;
        }

        public ListTag getList(String key) {
            NbtTag tag = entries.get(key);
            return tag instanceof ListTag l ? l : null;
        }

        public void put(String key, NbtTag value) {
            entries.put(Objects.requireNonNull(key, "key"), Objects.requireNonNull(value, "value"));
        }

        public void putString(String key, String value) {
            put(key, new StringTag(value));
        }

        public void putInt(String key, int value) {
            put(key, new IntTag(value));
        }

        public void putByte(String key, byte value) {
            put(key, new ByteTag(value));
        }

        @Override
        public byte typeId() {
            return TAG_COMPOUND;
        }

        @Override
        public CompoundTag copy() {
            CompoundTag clone = new CompoundTag();
            for (Map.Entry<String, NbtTag> entry : entries.entrySet()) {
                clone.put(entry.getKey(), entry.getValue().copy());
            }
            return clone;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof CompoundTag other && entries.equals(other.entries);
        }

        @Override
        public int hashCode() {
            return entries.hashCode();
        }
    }

    record IntArrayTag(int[] value) implements NbtTag {
        public IntArrayTag {
            value = value == null ? new int[0] : value.clone();
        }

        @Override
        public byte typeId() {
            return TAG_INT_ARRAY;
        }

        @Override
        public NbtTag copy() {
            return new IntArrayTag(value);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof IntArrayTag other && Arrays.equals(value, other.value);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(value);
        }
    }

    record LongArrayTag(long[] value) implements NbtTag {
        public LongArrayTag {
            value = value == null ? new long[0] : value.clone();
        }

        @Override
        public byte typeId() {
            return TAG_LONG_ARRAY;
        }

        @Override
        public NbtTag copy() {
            return new LongArrayTag(value);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof LongArrayTag other && Arrays.equals(value, other.value);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(value);
        }
    }
}
