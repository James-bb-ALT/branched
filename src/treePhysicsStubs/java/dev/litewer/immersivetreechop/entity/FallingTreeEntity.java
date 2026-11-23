package dev.litewer.immersivetreechop.entity;

import dev.litewer.immersivetreechop.server.TreeScanner;
import dev.litewer.immersivetreechop.wood.WoodVolume;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class FallingTreeEntity extends PhysicalWoodEntity {
    private final List<Part> parts = new ArrayList<>();
    private Vec3 centerLocal = Vec3.ZERO;
    private float cutHeight = 0.45F;
    private double radius = 10.0D;
    private UUID owner;

    public FallingTreeEntity(EntityType<? extends FallingTreeEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
    }

    private void configure() {
    }

    private void release() {
    }

    private static boolean lambda$release$6(BlockPos nextPos, Part firstPart, Part candidate) {
        return false;
    }

    private static void lambda$create$0(TreeScanner.Tree tree, ServerLevel level, float cutHeight,
                                        FallingTreeEntity entity, ServerPlayer player, ItemStack tool,
                                        BlockPos pos, BlockState state) {
    }

    private static boolean lambda$create$2(TreeScanner.Tree tree, BlockPos pos) {
        return false;
    }

    public record Part(BlockPos offset, BlockState state, boolean log, boolean base,
                       List<ItemStack> drops, WoodVolume volume) {
    }
}
