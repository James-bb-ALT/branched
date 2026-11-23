package dev.litewer.immersivetreechop.entity;

import dev.litewer.immersivetreechop.physics.RigidBody;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;

public abstract class PhysicalWoodEntity extends Entity {
    protected final RigidBody body = new RigidBody();

    protected PhysicalWoodEntity(EntityType<?> type, Level level) {
        super(type, level);
    }

    public RigidBody body() {
        return body;
    }

    public void publish() {
    }
}
