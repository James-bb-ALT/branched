package dev.litewer.immersivetreechop.entity;

import dev.litewer.immersivetreechop.wood.WoodVolume;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.List;
import java.util.UUID;

public final class LooseLogEntity extends PhysicalWoodEntity {
    private BlockState wood = Blocks.OAK_LOG.defaultBlockState();
    private float length = 1.0F;

    public LooseLogEntity(EntityType<? extends LooseLogEntity> type, Level level) {
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

    public double halfWidth(float partialTick) {
        return 0.46D;
    }

    public static LooseLogEntity fromTree(Level level, BlockState state, List<ItemStack> drops,
                                          Vec3 pos, Quaternionf orientation, Vec3 velocity,
                                          Vec3 angular, float length, UUID owner, float cutHeight,
                                          WoodVolume volume) {
        return null;
    }
}
