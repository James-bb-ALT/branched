package dev.branched.block;

import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;

import java.util.EnumMap;
import java.util.Map;

/**
 * Baked branch geometry. Every value is one blockstate variant with a cached
 * {@link net.minecraft.world.phys.shapes.VoxelShape}; no neighbour lookups and
 * no BlockEntity are involved at runtime.
 *
 * <p>{@code CORE} is a centred joint. Every other value adds a beam from the
 * joint towards that face so adjacent branch blocks visually connect.</p>
 */
public enum BranchShape implements StringRepresentable {
    CORE("core", null),
    UP("up", Direction.UP),
    DOWN("down", Direction.DOWN),
    NORTH("north", Direction.NORTH),
    SOUTH("south", Direction.SOUTH),
    EAST("east", Direction.EAST),
    WEST("west", Direction.WEST);

    private static final Map<Direction, BranchShape> BY_DIRECTION = new EnumMap<>(Direction.class);

    static {
        for (BranchShape shape : values()) {
            if (shape.direction != null) {
                BY_DIRECTION.put(shape.direction, shape);
            }
        }
    }

    private final String name;
    private final Direction direction;

    BranchShape(String name, Direction direction) {
        this.name = name;
        this.direction = direction;
    }

    public Direction direction() {
        return direction;
    }

    public static BranchShape forDirection(Direction direction) {
        return direction == null ? CORE : BY_DIRECTION.getOrDefault(direction, CORE);
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
