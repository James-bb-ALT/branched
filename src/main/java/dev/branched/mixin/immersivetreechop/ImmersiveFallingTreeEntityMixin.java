package dev.branched.mixin.immersivetreechop;

import dev.litewer.immersivetreechop.entity.FallingTreeEntity;
import dev.litewer.immersivetreechop.entity.PhysicalWoodEntity;
import dev.litewer.immersivetreechop.physics.RigidBody;
import dev.branched.compat.physics.TreePhysicsCompat;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Refines rigid-body physics shapes, mass, and inertia on Immersive Tree Chop's
 * {@link FallingTreeEntity} so attached {@code StaticBranchBlock}s collide accurately
 * with terrain and do not merge horizontally when released into loose logs.
 */
@Mixin(value = FallingTreeEntity.class, remap = false)
public abstract class ImmersiveFallingTreeEntityMixin {
    @Shadow
    private List<FallingTreeEntity.Part> parts;

    @Shadow
    private Vec3 centerLocal;

    @Shadow
    private float cutHeight;

    @Shadow
    private double radius;

    @Inject(method = "configure", at = @At("HEAD"))
    private void branched$sanitizeBranchBaseFlags(CallbackInfo ci) {
        if (this.parts == null || this.parts.isEmpty()) {
            return;
        }
        for (int i = 0; i < this.parts.size(); i++) {
            FallingTreeEntity.Part part = this.parts.get(i);
            if (part.log() && part.base() && TreePhysicsCompat.isBranch(part.state())) {
                this.parts.set(i, new FallingTreeEntity.Part(
                        part.offset(), part.state(), true, false, part.drops(), null));
            }
        }
    }

    @Inject(method = "configure", at = @At("TAIL"))
    private void branched$rebuildBranchCollisionShapes(CallbackInfo ci) {
        if (this.parts == null || this.parts.isEmpty()) {
            return;
        }
        boolean hasBranch = false;
        List<FallingTreeEntity.Part> woodParts = new ArrayList<>();
        for (FallingTreeEntity.Part part : this.parts) {
            if (part.log()) {
                woodParts.add(part);
                if (TreePhysicsCompat.isBranch(part.state())) {
                    hasBranch = true;
                }
            }
        }
        if (!hasBranch || woodParts.isEmpty()) {
            return;
        }

        Vec3 weightedSum = Vec3.ZERO;
        double totalMass = 0.0D;
        for (FallingTreeEntity.Part part : woodParts) {
            double mass;
            Vec3 center;
            if (TreePhysicsCompat.isBranch(part.state())) {
                AABB bounds = TreePhysicsCompat.branchCollisionBounds(part.state());
                mass = TreePhysicsCompat.branchMass(part.state());
                center = new Vec3(
                        part.offset().getX() + (bounds.minX + bounds.maxX) * 0.5D,
                        part.offset().getY() + (bounds.minY + bounds.maxY) * 0.5D,
                        part.offset().getZ() + (bounds.minZ + bounds.maxZ) * 0.5D);
            } else {
                double height = part.base() ? (1.0F - this.cutHeight) : 1.0D;
                mass = 4.0D * height;
                double cy = part.offset().getY() + (part.base() ? (this.cutHeight + 1.0F) * 0.5D : 0.5D);
                center = new Vec3(part.offset().getX() + 0.5D, cy, part.offset().getZ() + 0.5D);
            }
            weightedSum = weightedSum.add(center.scale(mass));
            totalMass += mass;
        }

        if (totalMass <= 0.0D) {
            return;
        }

        RigidBody body = ((PhysicalWoodEntity) (Object) this).body();
        this.centerLocal = weightedSum.scale(1.0D / totalMass);
        body.mass = Math.max(1.0D, totalMass);
        body.shapes.clear();

        Vec3 inertia = Vec3.ZERO;
        for (FallingTreeEntity.Part part : woodParts) {
            double mass;
            Vec3 center;
            Vec3 half;
            if (TreePhysicsCompat.isBranch(part.state())) {
                AABB bounds = TreePhysicsCompat.branchCollisionBounds(part.state());
                mass = TreePhysicsCompat.branchMass(part.state());
                center = new Vec3(
                        part.offset().getX() + (bounds.minX + bounds.maxX) * 0.5D,
                        part.offset().getY() + (bounds.minY + bounds.maxY) * 0.5D,
                        part.offset().getZ() + (bounds.minZ + bounds.maxZ) * 0.5D);
                half = new Vec3(
                        (bounds.maxX - bounds.minX) * 0.5D,
                        (bounds.maxY - bounds.minY) * 0.5D,
                        (bounds.maxZ - bounds.minZ) * 0.5D);
            } else {
                double height = part.base() ? (1.0F - this.cutHeight) : 1.0D;
                mass = 4.0D * height;
                double cy = part.offset().getY() + (part.base() ? (this.cutHeight + 1.0F) * 0.5D : 0.5D);
                center = new Vec3(part.offset().getX() + 0.5D, cy, part.offset().getZ() + 0.5D);
                half = new Vec3(0.5D, height * 0.5D, 0.5D);
            }

            Vec3 rel = center.subtract(this.centerLocal);
            body.shapes.add(new RigidBody.Shape(rel, half));

            double ix = mass * ((half.y * half.y + half.z * half.z) / 3.0D + rel.y * rel.y + rel.z * rel.z);
            double iy = mass * ((half.x * half.x + half.z * half.z) / 3.0D + rel.x * rel.x + rel.z * rel.z);
            double iz = mass * ((half.x * half.x + half.y * half.y) / 3.0D + rel.x * rel.x + rel.y * rel.y);
            inertia = inertia.add(ix, iy, iz);
        }

        body.inverseInertia = new Vec3(
                1.0D / Math.max(0.05D, inertia.x),
                1.0D / Math.max(0.05D, inertia.y),
                1.0D / Math.max(0.05D, inertia.z));

        double maxDist = 5.0D;
        for (FallingTreeEntity.Part part : this.parts) {
            Vec3 p = new Vec3(part.offset().getX() + 0.5D, part.offset().getY() + 0.5D, part.offset().getZ() + 0.5D);
            double d = p.subtract(this.centerLocal).length();
            if (d > maxDist) {
                maxDist = d;
            }
        }
        this.radius = maxDist + 2.0D;
    }

    @Inject(method = "lambda$release$6", at = @At("HEAD"), cancellable = true)
    private static void branched$preventHorizontalBranchVerticalMerge(
            BlockPos nextPos, FallingTreeEntity.Part firstPart, FallingTreeEntity.Part candidate,
            CallbackInfoReturnable<Boolean> cir) {
        if (firstPart != null && TreePhysicsCompat.isBranch(firstPart.state())
                && TreePhysicsCompat.primaryBranchAxis(firstPart.state()) != Direction.Axis.Y) {
            cir.setReturnValue(false);
        }
    }
}
