package dev.branched.worldgen;

import dev.branched.core.ResourceId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Minecraft-backed {@link PlacementContext} over a worldgen region. */
public final class WorldGenPlacementContext implements PlacementContext {
    private final WorldGenLevel level;
    private final ResourceId dimension;
    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

    public WorldGenPlacementContext(WorldGenLevel level) {
        this.level = level;
        ResourceLocation location = level.getLevel().dimension().location();
        this.dimension = ResourceId.of(location.getNamespace(), location.getPath());
    }

    @Override
    public long worldSeed() {
        return level.getSeed();
    }

    @Override
    public ResourceId dimension() {
        return dimension;
    }

    @Override
    public int minBuildHeight() {
        return level.getMinBuildHeight();
    }

    @Override
    public int maxBuildHeight() {
        return level.getMaxBuildHeight();
    }

    @Override
    public boolean isAir(int x, int y, int z) {
        return level.isEmptyBlock(cursor.set(x, y, z));
    }

    @Override
    public boolean isReplaceable(int x, int y, int z) {
        BlockState state = level.getBlockState(cursor.set(x, y, z));
        // Vanilla tree features may write over existing leaves; matching that
        // keeps dense taiga generation from rejecting every overlapping tree.
        return state.isAir()
                || state.canBeReplaced()
                || state.is(BlockTags.REPLACEABLE)
                || state.is(BlockTags.LEAVES);
    }

    @Override
    public boolean hasSolidGround(int x, int z) {
        for (int y = level.getMinBuildHeight(); y < level.getMaxBuildHeight(); y++) {
            if (!level.isEmptyBlock(cursor.set(x, y, z))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean canWrite(int x, int y, int z) {
        return level.ensureCanWrite(cursor.set(x, y, z));
    }

    @Override
    public boolean isGround(int x, int y, int z) {
        BlockState state = level.getBlockState(cursor.set(x, y, z));
        // Sand is included for the palm and beach families added in Phase 3.
        // The native features gate those trees behind a matching_blocks
        // predicate, which the mirrored placed feature keeps, so a sand-tolerant
        // ground check cannot leak trees into unrelated biomes.
        if (state.is(BlockTags.DIRT)
                || state.is(BlockTags.SAND)
                || state.is(Blocks.SNOW)
                || state.is(Blocks.SNOW_BLOCK)) {
            return true;
        }
        ResourceLocation id = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        String path = id.getPath();
        return path.contains("aether_grass")
                || path.contains("aether_dirt")
                || path.equals("echo_soil")
                || path.equals("sculk_grime");
    }

    @Override
    public ResourceId blockAt(int x, int y, int z) {
        BlockPos pos = cursor.set(x, y, z);
        if (!level.ensureCanWrite(pos)) {
            return null;
        }
        BlockState state = level.getBlockState(pos);
        ResourceLocation location = BuiltInRegistries.BLOCK.getKey(state.getBlock());
        return location == null
                ? null
                : ResourceId.of(location.getNamespace(), location.getPath());
    }
}
