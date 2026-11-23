package ht.treechop.common.chop;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.stream.Stream;

public class LazyTreeData {
    private Level level;

    public Stream<BlockPos> streamLogs() {
        return Stream.empty();
    }
}
