package dev.branched.gametest;

import dev.branched.Branched;
import dev.branched.block.BranchShape;
import dev.branched.block.StaticBranchBlock;
import dev.branched.core.ResourceId;
import dev.branched.diagnostics.DiagnosticsExporter;
import dev.branched.morphology.BlockPalette;
import dev.branched.morphology.CanonicalShape;
import dev.branched.morphology.MorphologyRequest;
import dev.branched.morphology.RasterizedTree;
import dev.branched.morphology.ReferenceBlockRasterizer;
import dev.branched.morphology.TreeSkeleton;
import dev.branched.registry.ModBlocks;
import dev.branched.species.SpeciesProfile;
import dev.branched.worldgen.BranchMaterials;
import dev.branched.worldgen.WorldgenGate;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Forge GameTests verifying runtime invariants under {@code forge.enabledGameTestNamespaces}.
 */
@GameTestHolder(Branched.MOD_ID)
@PrefixGameTestTemplate(false)
public final class ArborealGameTests {
    private ArborealGameTests() {
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void deterministicTreeTopology(GameTestHelper helper) {
        SpeciesProfile oak = Branched.profiles().get(ResourceId.parse("minecraft:oak"))
                .orElseThrow(() -> new AssertionError("minecraft:oak profile missing"));
        ReferenceBlockRasterizer rasterizer = new ReferenceBlockRasterizer();
        BlockPalette palette = new BlockPalette(
                oak.log(), BranchMaterials.resolve(oak.log()), oak.leaves(),
                oak.sapling().orElse(null), null);
        ResourceId overworld = ResourceId.parse("minecraft:overworld");

        MorphologyRequest reqA1 = new MorphologyRequest(123456789L, overworld, 16, 64, 16, 0L, oak);
        MorphologyRequest reqA2 = new MorphologyRequest(123456789L, overworld, 16, 64, 16, 0L, oak);
        MorphologyRequest reqB = new MorphologyRequest(123456789L, overworld, 48, 64, -32, 0L, oak);

        TreeSkeleton skelA1 = Branched.generator().generate(reqA1);
        TreeSkeleton skelA2 = Branched.generator().generate(reqA2);
        TreeSkeleton skelB = Branched.generator().generate(reqB);

        RasterizedTree treeA1 = rasterizer.rasterize(skelA1, oak, palette, 16, 64, 16);
        RasterizedTree treeA2 = rasterizer.rasterize(skelA2, oak, palette, 16, 64, 16);
        RasterizedTree treeB = rasterizer.rasterize(skelB, oak, palette, 48, 64, -32);

        helper.assertTrue(treeA1.blocks().equals(treeA2.blocks()),
                "Identical seed and position must produce identical blocks");
        helper.assertFalse(CanonicalShape.rotational(treeA1.blocks())
                        .equals(CanonicalShape.rotational(treeB.blocks())),
                "Distinct positions must produce distinct topologies");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void profilesAndAdaptersLoaded(GameTestHelper helper) {
        helper.assertTrue(Branched.profiles().size() == 70,
                "Expected 70 loaded profiles, got " + Branched.profiles().size());
        helper.assertTrue(Branched.adapters().size() == 15,
                "Expected 15 registered adapters, got " + Branched.adapters().size());
        helper.assertTrue(WorldgenGate.replaceableProfile(
                        ResourceId.parse("regions_unexplored:alpha")).isEmpty(),
                "SPECIAL_PRESERVE profile must never be offered for replacement");
        helper.assertTrue(WorldgenGate.replaceableProfile(
                        ResourceId.parse("twilightforest:mining")).isEmpty(),
                "Twilight magic profile must never be offered for replacement");
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void branchBlocksHaveNoBlockEntity(GameTestHelper helper) {
        for (var entry : ModBlocks.allBranches().values()) {
            StaticBranchBlock block = entry.get();
            for (BranchShape shape : BranchShape.values()) {
                BlockState state = StaticBranchBlock.withShape(block.defaultBlockState(), shape);
                helper.assertFalse(state.hasBlockEntity(),
                        "StaticBranchBlock must never own a BlockEntity: " + state);
                helper.assertFalse(state.getShape(helper.getLevel(), BlockPos.ZERO).isEmpty(),
                        "Cached VoxelShape must be non-empty for " + shape);
            }
            BlockState elbow = StaticBranchBlock.withConnections(
                    block.defaultBlockState(), true, false, true, false, true, false);
            helper.assertFalse(elbow.getShape(helper.getLevel(), BlockPos.ZERO).isEmpty(),
                    "6-way connected VoxelShape must be non-empty");
        }
        helper.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 40)
    public static void diagnosticsExportWritesValidJson(GameTestHelper helper) {
        try {
            Path baseDir = helper.getLevel().getServer().getServerDirectory().toPath();
            Path written = DiagnosticsExporter.exportDefault(baseDir);
            helper.assertTrue(Files.isRegularFile(written), "Missing diagnostics file " + written);
            String json = Files.readString(written);
            helper.assertTrue(json.contains("\"zeroBlockEntities\": true"),
                    "Diagnostics JSON missing invariant flag");
            helper.succeed();
        } catch (Exception ex) {
            throw new AssertionError("Diagnostics export failed", ex);
        }
    }
}
