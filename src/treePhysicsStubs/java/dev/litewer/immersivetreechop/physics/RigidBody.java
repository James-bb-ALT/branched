package dev.litewer.immersivetreechop.physics;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class RigidBody {
    public Vec3 position = Vec3.ZERO;
    public Vec3 velocity = Vec3.ZERO;
    public Vec3 angular = Vec3.ZERO;
    public Quaternionf orientation = new Quaternionf();
    public final List<Shape> shapes = new ArrayList<>();
    public final Set<BlockPos> ignored = new HashSet<>();
    public double mass;
    public double restitution;
    public double friction;
    public Vec3 inverseInertia = Vec3.ZERO;
    public Vec3 hingePivot;
    public Vec3 hingeOffset;
    public boolean contact;
    public boolean sleeping;
    public double impact;

    public Vec3 rotate(Vec3 local) {
        return local;
    }

    public Vec3 speedAt(Vec3 worldPos) {
        return Vec3.ZERO;
    }

    public void box(Vec3 half, double mass) {
    }

    public record Shape(Vec3 offset, Vec3 half) {
    }
}
