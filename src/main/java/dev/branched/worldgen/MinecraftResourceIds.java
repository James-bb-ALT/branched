package dev.branched.worldgen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import dev.branched.core.ResourceId;
import net.minecraft.resources.ResourceLocation;

/**
 * Bridge between the Minecraft-free {@link ResourceId} and {@link ResourceLocation}.
 *
 * <p>{@link #CODEC} never throws inside a datapack load: an id that Minecraft's
 * parser accepts but {@code ResourceId} rejects (for example an empty path)
 * becomes a normal {@link DataResult} error instead of a crash.</p>
 */
public final class MinecraftResourceIds {
    public static final Codec<ResourceId> CODEC = ResourceLocation.CODEC.flatXmap(
            MinecraftResourceIds::decode,
            id -> DataResult.success(toLocation(id)));

    private MinecraftResourceIds() {
    }

    public static ResourceLocation toLocation(ResourceId id) {
        return ResourceLocation.fromNamespaceAndPath(id.namespace(), id.path());
    }

    public static DataResult<ResourceId> decode(ResourceLocation location) {
        ResourceId id = ResourceId.tryParse(location.toString());
        return id == null
                ? DataResult.error(() -> "Invalid resource id: '" + location + "'")
                : DataResult.success(id);
    }

    /** Returns {@code null} instead of throwing, for datapack lookups. */
    public static ResourceId fromLocationOrNull(ResourceLocation location) {
        return decode(location).result().orElse(null);
    }
}
