package dev.litewer.immersivetreechop.block;

import dev.litewer.immersivetreechop.wood.WoodVolume;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public final class CutLogBlockEntity extends BlockEntity {
    public CutLogBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public BlockState original() {
        return Blocks.AIR.defaultBlockState();
    }

    public boolean stump() {
        return false;
    }

    public void initialize(BlockState state, Direction dir) {
    }

    public void makeStump(float cutHeight) {
    }

    public WoodVolume volume() {
        return new WoodVolume();
    }
}
