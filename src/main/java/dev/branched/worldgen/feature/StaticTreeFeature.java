package dev.branched.worldgen.feature;

import dev.branched.Branched;
import dev.branched.core.ResourceId;
import dev.branched.morphology.BlockPalette;
import dev.branched.morphology.MorphologyRequest;
import dev.branched.morphology.PerformancePreset;
import dev.branched.morphology.RasterizedTree;
import dev.branched.morphology.ReferenceBlockRasterizer;
import dev.branched.morphology.TreeSkeleton;
import dev.branched.species.SpeciesProfile;
import dev.branched.worldgen.BranchMaterials;
import dev.branched.worldgen.PlacementResult;
import dev.branched.worldgen.TreeFitValidator;
import dev.branched.worldgen.WorldGenPlacementContext;
import dev.branched.worldgen.WorldGenTreeWriter;
import dev.branched.worldgen.WorldgenGate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Worldgen entry point for a single static tree.
 *
 * <p>Everything is deterministic: the seed mixes world seed, dimension, exact
 * origin, species and generator version. Retry attempts only change the variant
 * salt, never the world RNG, so chunk generation order cannot alter the result.</p>
 *
 * <p>The feature creates no BlockEntity, schedules no tick and keeps no state
 * after the call returns.</p>
 */
public final class StaticTreeFeature extends Feature<StaticTreeConfiguration> {
    private static final Logger LOGGER = Branched.LOGGER;
    private static final AtomicLong PLACED = new AtomicLong();
    private static final AtomicLong REJECTED = new AtomicLong();
    private static final long FIRST_PLACEMENT_LOGS = 5L;

    private final ReferenceBlockRasterizer rasterizer = new ReferenceBlockRasterizer();

    public StaticTreeFeature() {
        super(StaticTreeConfiguration.CODEC);
    }

    @Override
    public boolean place(FeaturePlaceContext<StaticTreeConfiguration> context) {
        if (!WorldgenGate.enabled()) {
            return false;
        }
        ResourceId species = context.config().species();
        SpeciesProfile profile = WorldgenGate.replaceableProfile(species).orElse(null);
        if (profile == null) {
            reject("profile_missing_or_unresolved:" + species);
            return false;
        }

        WorldGenLevel level = context.level();
        BlockPos origin = context.origin();
        WorldGenPlacementContext world = new WorldGenPlacementContext(level);
        if (!world.isGround(origin.getX(), origin.getY() - 1, origin.getZ())) {
            reject("no_ground");
            return false;
        }

        BlockPalette palette = new BlockPalette(
                profile.log(),
                BranchMaterials.resolve(profile.log()),
                profile.leaves(),
                profile.sapling().orElse(null),
                null);
        int attempts = WorldgenGate.maxAttempts(context.config().attempts());
        PerformancePreset preset = WorldgenGate.performancePreset();

        for (int attempt = 0; attempt < attempts; attempt++) {
            MorphologyRequest request = new MorphologyRequest(
                    level.getSeed(),
                    world.dimension(),
                    origin.getX(),
                    origin.getY(),
                    origin.getZ(),
                    attempt,
                    profile);
            long attemptStart = System.nanoTime();
            TreeSkeleton skeleton = Branched.generator().generate(request);
            long rasterStart = System.nanoTime();
            RasterizedTree tree = rasterizer.rasterize(skeleton, profile, palette,
                    origin.getX(), origin.getY(), origin.getZ(), preset);
            Branched.metrics().recordRasterized(profile, tree,
                    System.nanoTime() - rasterStart);
            Branched.metrics().recordAttempt(System.nanoTime() - attemptStart);

            PlacementResult fit = TreeFitValidator.validate(tree, world);
            if (fit.outcome() != PlacementResult.Outcome.PLACED) {
                Branched.metrics().recordRejection(fit.reason());
                reject(fit.reason());
                continue;
            }

            int written = WorldGenTreeWriter.write(level, tree, profile, palette);
            if (written > 0) {
                long count = PLACED.incrementAndGet();
                if (count <= FIRST_PLACEMENT_LOGS || count % 250L == 0L) {
                    LOGGER.info("Branched placed {} at {} {} {}: {} blocks (attempt {}/{})",
                            profile.id(), origin.getX(), origin.getY(), origin.getZ(),
                            written, attempt + 1, attempts);
                }
                return true;
            }
            Branched.metrics().recordRejection("write_failed");
            reject("write_failed");
        }
        return false;
    }

    public static long placedCount() {
        return PLACED.get();
    }

    public static long rejectedCount() {
        return REJECTED.get();
    }

    private static void reject(String reason) {
        long count = REJECTED.incrementAndGet();
        if (count <= 3L || count % 500L == 0L) {
            LOGGER.info("Branched rejected a tree: {}", reason);
        }
    }
}
