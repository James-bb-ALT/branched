package dev.litewer.immersivetreechop.server;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

public final class PlacedLogs {
    public static PlacedLogs get(ServerLevel level) {
        return new PlacedLogs();
    }

    public boolean manual(BlockPos pos) {
        return false;
    }

    public boolean legacy(BlockPos pos) {
        return false;
    }
}
