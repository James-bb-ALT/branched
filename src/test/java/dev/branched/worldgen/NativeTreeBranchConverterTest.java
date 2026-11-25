package dev.branched.worldgen;

import dev.branched.core.ResourceId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NativeTreeBranchConverterTest {

    @Test
    void excludesPalmsCoconutsBananasFungiAndOasisPalms() {
        assertTrue(NativeTreeBranchConverter.isNonBranchingSpecies(
                ResourceId.parse("fishofthieves:coconut_log"),
                ResourceId.parse("fishofthieves:coconut_fronds")));
        assertTrue(NativeTreeBranchConverter.isNonBranchingSpecies(
                ResourceId.parse("fishofthieves:banana_stem"),
                ResourceId.parse("fishofthieves:banana_leaves")));
        assertTrue(NativeTreeBranchConverter.isNonBranchingSpecies(
                ResourceId.parse("ecologics:coconut_log"),
                ResourceId.parse("ecologics:coconut_leaves")));
        assertTrue(NativeTreeBranchConverter.isNonBranchingSpecies(
                ResourceId.parse("regions_unexplored:palm_log"),
                ResourceId.parse("regions_unexplored:palm_leaves")));
        assertTrue(NativeTreeBranchConverter.isNonBranchingSpecies(
                ResourceId.parse("minecraft:jungle_log"),
                ResourceId.parse("minecraft:azalea_leaves")));

        assertFalse(NativeTreeBranchConverter.isNonBranchingSpecies(
                ResourceId.parse("quark:blossom_log"),
                ResourceId.parse("quark:blue_blossom_leaves")));
        assertFalse(NativeTreeBranchConverter.isNonBranchingSpecies(
                ResourceId.parse("minecraft:oak_log"),
                ResourceId.parse("minecraft:oak_leaves")));
        assertFalse(NativeTreeBranchConverter.isNonBranchingSpecies(
                ResourceId.parse("minecraft:acacia_log"),
                ResourceId.parse("minecraft:acacia_leaves")));
        assertFalse(NativeTreeBranchConverter.isNonBranchingSpecies(
                ResourceId.parse("minecraft:oak_log"),
                ResourceId.parse("fishofthieves:mango_leaves")));
    }

    @Test
    void convertsInternalAndDiagonalBlossomLimbsIntoConnectedBranches() {
        BlockPos origin = new BlockPos(0, 64, 0);
        Set<BlockPos> rawLogs = new LinkedHashSet<>();
        for (int y = 64; y <= 73; y++) {
            rawLogs.add(new BlockPos(0, y, 0));
        }
        // Simulated FancyTrunkPlacer / Quark Blossom diagonal limb climbing outward & upward
        rawLogs.add(new BlockPos(1, 69, 0));
        rawLogs.add(new BlockPos(2, 70, 1)); // diagonal step from (1,69,0)
        rawLogs.add(new BlockPos(3, 71, 1));

        Set<BlockPos> rawLeaves = fillSphereLeaves(0, 72, 0, 4);

        NativeTreeBranchConverter.ConversionPlan plan = NativeTreeBranchConverter.planConversion(
                origin, rawLogs, rawLeaves, Set.of(), 12345L);

        assertFalse(plan.branchPositions().isEmpty());
        assertTrue(plan.branchPositions().contains(new BlockPos(1, 69, 0)));
        assertTrue(plan.branchPositions().contains(new BlockPos(2, 70, 1)));
        assertTrue(plan.branchPositions().contains(new BlockPos(3, 71, 1)));
        assertTrue(plan.keptTrunkLogs().contains(new BlockPos(0, 64, 0)));
        // Main trunk continues as full log all the way to the top (y=71..73), never replaced by a vertical branch
        assertTrue(plan.keptTrunkLogs().contains(new BlockPos(0, 73, 0)));
        assertFalse(plan.branchPositions().contains(new BlockPos(0, 73, 0)));
        assertEquals(3, plan.branchThicknesses().get(new BlockPos(1, 69, 0)));
        assertSixConnected(plan.keptTrunkLogs(), plan.branchPositions());
    }

    @Test
    void convertsForkingCrookedTrunkStemsIntoThickBranches() {
        BlockPos origin = new BlockPos(10, 64, -10);
        Set<BlockPos> rawLogs = new LinkedHashSet<>();
        // Vertical lower trunk (3 blocks) splitting diagonally into two forked stems
        rawLogs.add(new BlockPos(10, 64, -10));
        rawLogs.add(new BlockPos(10, 65, -10));
        rawLogs.add(new BlockPos(10, 66, -10));
        // Fork A
        rawLogs.add(new BlockPos(11, 67, -10));
        rawLogs.add(new BlockPos(12, 68, -10));
        // Fork B
        rawLogs.add(new BlockPos(9, 67, -11));
        rawLogs.add(new BlockPos(8, 68, -11));

        Set<BlockPos> rawLeaves = new LinkedHashSet<>();
        rawLeaves.addAll(fillSphereLeaves(12, 69, -10, 2));
        rawLeaves.addAll(fillSphereLeaves(8, 69, -11, 2));

        NativeTreeBranchConverter.ConversionPlan plan = NativeTreeBranchConverter.planConversion(
                origin, rawLogs, rawLeaves, Set.of(), 999L);

        assertTrue(plan.keptTrunkLogs().contains(new BlockPos(10, 64, -10)));
        assertTrue(plan.keptTrunkLogs().contains(new BlockPos(10, 65, -10)));
        assertTrue(plan.keptTrunkLogs().contains(new BlockPos(10, 66, -10)));
        assertFalse(plan.branchPositions().contains(new BlockPos(10, 67, -10)),
                "forked limbs must step horizontally off the main trunk top, never vertically onto (10,67,-10)");
        assertTrue(plan.branchPositions().contains(new BlockPos(11, 66, -10)));
        assertTrue(plan.branchPositions().contains(new BlockPos(11, 67, -10)));
        assertTrue(plan.branchPositions().contains(new BlockPos(12, 68, -10)));
        assertTrue(plan.branchPositions().contains(new BlockPos(9, 67, -11)));
        assertTrue(plan.branchPositions().contains(new BlockPos(8, 68, -11)));
        assertEquals(3, plan.branchThicknesses().get(new BlockPos(11, 66, -10)));
        assertTrue(plan.branchThicknesses().get(new BlockPos(11, 67, -10)) >= 2,
                "middle branch segments should taper gradually instead of dropping straight to thin");
        assertSixConnected(plan.keptTrunkLogs(), plan.branchPositions());
    }

    @Test
    void sproutsCanopyCoveredBranchesOnStraightUnbranchedNativeTree() {
        BlockPos origin = new BlockPos(0, 64, 0);
        Set<BlockPos> rawLogs = new LinkedHashSet<>();
        for (int y = 64; y <= 69; y++) {
            rawLogs.add(new BlockPos(0, y, 0));
        }
        Set<BlockPos> rawLeaves = fillSphereLeaves(0, 68, 0, 3);
        rawLeaves.removeAll(rawLogs);

        NativeTreeBranchConverter.ConversionPlan plan = NativeTreeBranchConverter.planConversion(
                origin, rawLogs, rawLeaves, Set.of(), 42L);

        assertFalse(plan.branchPositions().isEmpty(),
                "straight unbranched native tree must sprout lateral canopy branches");
        for (BlockPos branch : plan.branchPositions()) {
            assertTrue(rawLeaves.contains(branch.above()),
                    "sprouted branch must stay covered by canopy leaves at " + branch);
        }
        assertSixConnected(plan.keptTrunkLogs(), plan.branchPositions());
    }

    @Test
    void prunesClumpedCanopyBaseLogsWithoutFormingBranchMesh() {
        BlockPos origin = new BlockPos(0, 64, 0);
        Set<BlockPos> rawLogs = new LinkedHashSet<>();
        for (int y = 64; y <= 70; y++) {
            rawLogs.add(new BlockPos(0, y, 0));
        }
        // Dense 2x2x2 crown-base clump plus diagonal outer limbs
        rawLogs.add(new BlockPos(1, 68, 0));
        rawLogs.add(new BlockPos(0, 68, 1));
        rawLogs.add(new BlockPos(1, 68, 1));
        rawLogs.add(new BlockPos(1, 69, 0));
        rawLogs.add(new BlockPos(0, 69, 1));
        rawLogs.add(new BlockPos(1, 69, 1));
        rawLogs.add(new BlockPos(2, 69, 0));
        rawLogs.add(new BlockPos(0, 69, 2));

        Set<BlockPos> rawLeaves = fillSphereLeaves(0, 69, 0, 4);
        rawLeaves.removeAll(rawLogs);

        NativeTreeBranchConverter.ConversionPlan plan = NativeTreeBranchConverter.planConversion(
                origin, rawLogs, rawLeaves, Set.of(), 777L);

        assertFalse(plan.branchPositions().isEmpty());
        assertFalse(plan.prunedClutterLogs().isEmpty());

        Set<BlockPos> wood = new LinkedHashSet<>(plan.keptTrunkLogs());
        wood.addAll(plan.branchPositions());
        int[] signs = {-1, 1};
        for (BlockPos b : plan.branchPositions()) {
            for (int dx : signs) {
                for (int dz : signs) {
                    assertFalse(wood.contains(b.offset(dx, 0, 0))
                                    && wood.contains(b.offset(0, 0, dz))
                                    && wood.contains(b.offset(dx, 0, dz)),
                            "XZ 2x2 wood square must not contain branch " + b);
                }
            }
            for (int dx : signs) {
                for (int dy : signs) {
                    assertFalse(wood.contains(b.offset(dx, 0, 0))
                                    && wood.contains(b.offset(0, dy, 0))
                                    && wood.contains(b.offset(dx, dy, 0)),
                            "XY 2x2 wood square must not contain branch " + b);
                }
            }
            for (int dy : signs) {
                for (int dz : signs) {
                    assertFalse(wood.contains(b.offset(0, dy, 0))
                                    && wood.contains(b.offset(0, 0, dz))
                                    && wood.contains(b.offset(0, dy, dz)),
                            "YZ 2x2 wood square must not contain branch " + b);
                }
            }
        }
        assertSixConnected(plan.keptTrunkLogs(), plan.branchPositions());
    }

    @Test
    void dominantBranchSelectionIgnoresDirtFromTrunkPlacersAndRejectsUnmappedLogs() {
        ResourceId willowBranch = NativeTreeBranchConverter.selectDominantBranch(
                List.of(
                        ResourceId.parse("minecraft:dirt"),
                        ResourceId.parse("biomemakeover:willow_log"),
                        ResourceId.parse("biomemakeover:willow_log"),
                        ResourceId.parse("biomemakeover:willow_log")),
                id -> !"minecraft:dirt".equals(id.toString()));
        assertEquals(BranchMaterials.BM_WILLOW_BRANCH, willowBranch);

        ResourceId alien = NativeTreeBranchConverter.selectDominantBranch(
                List.of(ResourceId.parse("unknownmod:alien_timber_log"), ResourceId.parse("unknownmod:alien_timber_log")),
                id -> id.path().endsWith("_log"));
        assertNull(alien, "unmapped alien tree logs must not fall back to mismatched oak branches");
    }

    @Test
    void joshuaAndBetternetherWartAreBranchingWhileCrimsonWartStaysExcluded() {
        assertFalse(NativeTreeBranchConverter.isNonBranchingSpecies(
                ResourceId.parse("regions_unexplored:joshua_log"),
                ResourceId.parse("regions_unexplored:joshua_leaves")));
        assertFalse(NativeTreeBranchConverter.isNonBranchingSpecies(
                ResourceId.parse("betternether:wart_log"),
                ResourceId.parse("minecraft:nether_wart_block")));
        assertTrue(NativeTreeBranchConverter.isNonBranchingSpecies(
                ResourceId.parse("minecraft:crimson_stem"),
                ResourceId.parse("minecraft:nether_wart_block")));

        BlockPos origin = new BlockPos(0, 64, 0);
        Set<BlockPos> rawLogs = new LinkedHashSet<>();
        for (int y = 64; y <= 68; y++) {
            rawLogs.add(new BlockPos(0, y, 0));
        }
        // Forked Joshua desert limbs ascending outward
        rawLogs.add(new BlockPos(1, 68, 0));
        rawLogs.add(new BlockPos(2, 69, 0));
        rawLogs.add(new BlockPos(2, 70, 0));
        rawLogs.add(new BlockPos(-1, 68, 0));
        rawLogs.add(new BlockPos(-2, 69, 1));

        Set<BlockPos> leaves = new LinkedHashSet<>();
        leaves.add(new BlockPos(2, 71, 0));
        leaves.add(new BlockPos(-2, 70, 1));

        NativeTreeBranchConverter.ConversionPlan plan = NativeTreeBranchConverter.planConversion(
                origin, rawLogs, leaves, Set.of(), 2026L);

        assertEquals(BranchMaterials.JOSHUA_BRANCH,
                BranchMaterials.resolve(ResourceId.parse("regions_unexplored:joshua_log")));
        assertTrue(plan.branchPositions().contains(new BlockPos(1, 68, 0)));
        assertTrue(plan.branchPositions().contains(new BlockPos(2, 70, 0)));
        assertTrue(plan.branchPositions().contains(new BlockPos(-2, 69, 1)));
        assertSixConnected(plan.keptTrunkLogs(), plan.branchPositions());
    }

    @Test
    void socotraUmbrellaLimbsConvertToSixConnectedTaperedBranches() {
        BlockPos origin = new BlockPos(0, 64, 0);
        Set<BlockPos> rawLogs = new LinkedHashSet<>();
        for (int y = 64; y <= 71; y++) {
            rawLogs.add(new BlockPos(0, y, 0));
        }
        rawLogs.add(new BlockPos(1, 70, 0));
        rawLogs.add(new BlockPos(2, 71, 0));
        rawLogs.add(new BlockPos(-1, 70, 0));
        rawLogs.add(new BlockPos(-2, 71, 0));
        rawLogs.add(new BlockPos(0, 70, 1));
        rawLogs.add(new BlockPos(0, 71, 2));
        rawLogs.add(new BlockPos(0, 70, -1));
        rawLogs.add(new BlockPos(0, 71, -2));

        Set<BlockPos> leaves = new LinkedHashSet<>();
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                leaves.add(new BlockPos(dx, 72, dz));
            }
        }
        leaves.removeAll(rawLogs);

        NativeTreeBranchConverter.ConversionPlan plan = NativeTreeBranchConverter.planConversion(
                origin, rawLogs, leaves, Set.of(), 77L);

        assertEquals(BranchMaterials.SOCOTRA_BRANCH,
                BranchMaterials.resolve(ResourceId.parse("regions_unexplored:socotra_log")));
        assertTrue(plan.branchPositions().contains(new BlockPos(2, 71, 0)));
        assertTrue(plan.branchPositions().contains(new BlockPos(-2, 71, 0)));
        assertTrue(plan.branchPositions().contains(new BlockPos(0, 71, 2)));
        assertTrue(plan.branchPositions().contains(new BlockPos(0, 71, -2)));
        assertSixConnected(plan.keptTrunkLogs(), plan.branchPositions());
    }

    @Test
    void compactStraightMauveTreesSproutBalancedCanopyCoveredBranches() {
        for (int height : List.of(4, 5, 6)) {
            BlockPos origin = new BlockPos(0, 64, 0);
            Set<BlockPos> rawLogs = new LinkedHashSet<>();
            for (int i = 0; i < height; i++) {
                rawLogs.add(new BlockPos(0, 64 + i, 0));
            }
            Set<BlockPos> leaves = new LinkedHashSet<>();
            int topY = 64 + height - 1;
            for (int y = topY - 2; y <= topY; y++) {
                for (int dx = -2; dx <= 2; dx++) {
                    for (int dz = -2; dz <= 2; dz++) {
                        if (Math.abs(dx) + Math.abs(dz) <= 3) {
                            leaves.add(new BlockPos(dx, y, dz));
                        }
                    }
                }
            }
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    leaves.add(new BlockPos(dx, topY + 1, dz));
                }
            }
            leaves.removeAll(rawLogs);

            NativeTreeBranchConverter.ConversionPlan plan = NativeTreeBranchConverter.planConversion(
                    origin, rawLogs, leaves, Set.of(), 300L + height);

            int minExpected = height <= 4 ? 2 : 3;
            assertTrue(plan.branchPositions().size() >= minExpected,
                    "height " + height + " compact tree should sprout at least " + minExpected
                            + " branch blocks, got " + plan.branchPositions().size());
            Set<Direction> dirs = new LinkedHashSet<>();
            for (BlockPos b : plan.branchPositions()) {
                assertTrue(leaves.contains(b.above()),
                        "sprouted branch must stay covered by canopy leaves at " + b);
                int sx = Integer.compare(b.getX(), 0);
                int sz = Integer.compare(b.getZ(), 0);
                if (sx > 0) dirs.add(Direction.EAST);
                if (sx < 0) dirs.add(Direction.WEST);
                if (sz > 0) dirs.add(Direction.SOUTH);
                if (sz < 0) dirs.add(Direction.NORTH);
            }
            assertTrue(dirs.size() >= 2, "sprouted branches should radiate in multiple directions: " + dirs);
            assertSixConnected(plan.keptTrunkLogs(), plan.branchPositions());
        }
    }

    @Test
    void wideHollowGiantTrunkPreservesGroundedRingWhileConvertingLimbs() {
        BlockPos origin = new BlockPos(0, 64, 0);
        Set<BlockPos> rawLogs = new LinkedHashSet<>();
        for (int y = 64; y <= 78; y++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (Math.abs(dx) == 2 || Math.abs(dz) == 2) {
                        rawLogs.add(new BlockPos(dx, y, dz));
                    }
                }
            }
        }
        // Boughs extending outward from the hollow ring
        rawLogs.add(new BlockPos(3, 75, 0));
        rawLogs.add(new BlockPos(4, 76, 1));
        rawLogs.add(new BlockPos(-3, 75, 0));
        rawLogs.add(new BlockPos(-4, 76, -1));

        Set<BlockPos> leaves = new LinkedHashSet<>();
        leaves.addAll(fillSphereLeaves(4, 77, 1, 2));
        leaves.addAll(fillSphereLeaves(-4, 77, -1, 2));
        leaves.removeAll(rawLogs);

        NativeTreeBranchConverter.ConversionPlan plan = NativeTreeBranchConverter.planConversion(
                origin, rawLogs, leaves, Set.of(), 808L);

        assertTrue(plan.keptTrunkLogs().contains(new BlockPos(2, 64, 2)));
        assertTrue(plan.keptTrunkLogs().contains(new BlockPos(-2, 64, -2)));
        assertTrue(plan.keptTrunkLogs().contains(new BlockPos(2, 78, -2)));
        assertTrue(plan.branchPositions().contains(new BlockPos(3, 75, 0)));
        assertTrue(plan.branchPositions().contains(new BlockPos(4, 76, 1)));
        assertTrue(plan.branchPositions().contains(new BlockPos(-3, 75, 0)));
        assertTrue(plan.branchPositions().contains(new BlockPos(-4, 76, -1)));
        assertSixConnected(plan.keptTrunkLogs(), plan.branchPositions());
    }

    @Test
    void ceilingAnchoredTreeKeepsUpperTrunkAndConvertsDownwardLimbs() {
        BlockPos origin = new BlockPos(0, 90, 0);
        Set<BlockPos> rawLogs = new LinkedHashSet<>();
        for (int y = 90; y >= 81; y--) {
            rawLogs.add(new BlockPos(0, y, 0));
        }
        rawLogs.add(new BlockPos(1, 83, 0));
        rawLogs.add(new BlockPos(2, 82, 1));
        rawLogs.add(new BlockPos(-1, 82, 0));
        rawLogs.add(new BlockPos(-2, 81, -1));

        Set<BlockPos> leaves = fillSphereLeaves(0, 81, 0, 3);
        leaves.removeAll(rawLogs);

        NativeTreeBranchConverter.ConversionPlan plan = NativeTreeBranchConverter.planCeilingConversion(
                origin, rawLogs, leaves, Set.of(), 909L);

        assertTrue(plan.keptTrunkLogs().contains(new BlockPos(0, 90, 0)));
        assertTrue(plan.keptTrunkLogs().contains(new BlockPos(0, 89, 0)));
        assertTrue(plan.branchPositions().contains(new BlockPos(1, 83, 0)));
        assertTrue(plan.branchPositions().contains(new BlockPos(2, 82, 1)));
        assertTrue(plan.branchPositions().contains(new BlockPos(-1, 82, 0)));
        assertTrue(plan.branchPositions().contains(new BlockPos(-2, 81, -1)));
        assertSixConnected(plan.keptTrunkLogs(), plan.branchPositions());
    }

    @Test
    void explicitBaobabConversionPreservesWideCircularTrunkAndPreventsBranchMeshes() {
        Set<BlockPos> trunk = new LinkedHashSet<>();
        for (int y = 64; y <= 76; y++) {
            for (int dx = -2; dx <= 2; dx++) {
                for (int dz = -2; dz <= 2; dz++) {
                    if (dx * dx + dz * dz <= 5) {
                        trunk.add(new BlockPos(dx, y, dz));
                    }
                }
            }
        }
        // Lateral bough starting in circular trunk notch at (2, 74, 2) plus diagonal steps & fork
        Set<BlockPos> candidateBranches = new LinkedHashSet<>();
        candidateBranches.add(new BlockPos(2, 74, 2)); // touches trunk at (2,74,1) and (1,74,2)
        candidateBranches.add(new BlockPos(3, 74, 2));
        candidateBranches.add(new BlockPos(3, 74, 3)); // flank neighbor forming 2x2 with trunk? Check!
        candidateBranches.add(new BlockPos(4, 75, 2));
        candidateBranches.add(new BlockPos(5, 76, 3));
        candidateBranches.add(new BlockPos(4, 76, 1));

        Set<BlockPos> leaves = fillSphereLeaves(5, 77, 3, 3);
        leaves.removeAll(trunk);

        NativeTreeBranchConverter.ConversionPlan plan =
                NativeTreeBranchConverter.planExplicitBranchConversion(trunk, candidateBranches, leaves);

        assertEquals(trunk.size(), plan.keptTrunkLogs().size(), "entire baobab trunk must remain intact");
        assertTrue(plan.keptTrunkLogs().containsAll(trunk));
        assertTrue(plan.branchPositions().contains(new BlockPos(2, 74, 2)),
                "branch root socket on circular trunk notch must convert cleanly");
        assertTrue(plan.branchPositions().contains(new BlockPos(5, 76, 3)));
        assertEquals(3, plan.branchThicknesses().get(new BlockPos(2, 74, 2)));
        assertSixConnected(plan.keptTrunkLogs(), plan.branchPositions());

        Set<BlockPos> wood = new LinkedHashSet<>(plan.keptTrunkLogs());
        wood.addAll(plan.branchPositions());
        int[] signs = {-1, 1};
        for (BlockPos b : plan.branchPositions()) {
            for (int dx : signs) {
                for (int dz : signs) {
                    boolean allFour = wood.contains(b.offset(dx, 0, 0))
                            && wood.contains(b.offset(0, 0, dz))
                            && wood.contains(b.offset(dx, 0, dz));
                    if (allFour) {
                        assertFalse(plan.branchPositions().contains(b.offset(dx, 0, 0))
                                        || plan.branchPositions().contains(b.offset(0, 0, dz))
                                        || plan.branchPositions().contains(b.offset(dx, 0, dz)),
                                "2x2 square must never contain multiple branch blocks at " + b);
                    }
                }
            }
        }
    }

    @Test
    void submergedOrFlaredLowerRootLogsStayFullTrunkLogs() {
        BlockPos origin = new BlockPos(0, 64, 0);
        Set<BlockPos> rawLogs = new LinkedHashSet<>();
        // Submerged/flared root collar below main trunk base (y=62..63)
        rawLogs.add(new BlockPos(0, 62, 0));
        rawLogs.add(new BlockPos(1, 63, 0));
        rawLogs.add(new BlockPos(-1, 63, 0));
        for (int y = 63; y <= 75; y++) {
            rawLogs.add(new BlockPos(0, y, 0));
        }
        // Upper limb at y=73..74
        rawLogs.add(new BlockPos(1, 73, 0));
        rawLogs.add(new BlockPos(2, 74, 0));

        Set<BlockPos> leaves = fillSphereLeaves(0, 74, 0, 3);
        leaves.removeAll(rawLogs);

        NativeTreeBranchConverter.ConversionPlan plan = NativeTreeBranchConverter.planConversion(
                origin, rawLogs, leaves, Set.of(), 555L);

        assertTrue(plan.keptTrunkLogs().contains(new BlockPos(0, 62, 0)));
        assertTrue(plan.keptTrunkLogs().contains(new BlockPos(1, 63, 0)));
        assertTrue(plan.keptTrunkLogs().contains(new BlockPos(-1, 63, 0)));
        assertTrue(plan.branchPositions().contains(new BlockPos(1, 73, 0)));
        assertTrue(plan.branchPositions().contains(new BlockPos(2, 74, 0)));
        assertSixConnected(plan.keptTrunkLogs(), plan.branchPositions());
    }

    @Test
    void redwoodSlopedRootsAndAdjacentTrunksStayIntact() {
        BlockPos origin = new BlockPos(0, 64, 0);
        Set<BlockPos> rawLogs = new LinkedHashSet<>();
        Set<BlockPos> horizontalLogs = new LinkedHashSet<>();

        // Downhill 3-tall root buttress starting 3 blocks below origin (y=61..63)
        for (int y = 61; y <= 63; y++) {
            rawLogs.add(new BlockPos(1, y, 0));
        }
        // Main 1x1 central trunk starting at origin (y=64..82)
        for (int y = 64; y <= 82; y++) {
            rawLogs.add(new BlockPos(0, y, 0));
        }
        // Bare-trunk 1-block horizontal stub at y=68 (below canopy)
        BlockPos bareStub = new BlockPos(-1, 68, 0);
        rawLogs.add(bareStub);
        horizontalLogs.add(bareStub);

        // Adjacent secondary vertical trunk >= 5 blocks tall swept into component
        for (int y = 64; y <= 76; y++) {
            rawLogs.add(new BlockPos(4, y, 0));
        }
        // Canopy limb connecting to leaves at y=79..80
        rawLogs.add(new BlockPos(1, 79, 0));
        rawLogs.add(new BlockPos(2, 80, 0));
        horizontalLogs.add(new BlockPos(1, 79, 0));
        horizontalLogs.add(new BlockPos(2, 80, 0));

        Set<BlockPos> leaves = fillSphereLeaves(0, 80, 0, 4);
        leaves.removeAll(rawLogs);

        NativeTreeBranchConverter.ConversionPlan plan = NativeTreeBranchConverter.planConversion(
                origin, rawLogs, leaves, horizontalLogs, 777L);

        for (int y = 64; y <= 82; y++) {
            assertTrue(plan.keptTrunkLogs().contains(new BlockPos(0, y, 0)),
                    "main redwood trunk log must stay intact at y=" + y);
        }
        for (int y = 64; y <= 76; y++) {
            assertTrue(plan.keptTrunkLogs().contains(new BlockPos(4, y, 0)),
                    "adjacent vertical trunk log must stay intact at y=" + y);
        }
        assertTrue(plan.keptTrunkLogs().contains(bareStub),
                "isolated 1-block leafless trunk stub should not become a branch nub");
        assertTrue(plan.branchPositions().contains(new BlockPos(1, 79, 0)));
        assertTrue(plan.branchPositions().contains(new BlockPos(2, 80, 0)));
        assertSixConnected(plan.keptTrunkLogs(), plan.branchPositions());
    }

    private static Set<BlockPos> fillSphereLeaves(int cx, int cy, int cz, int radius) {
        Set<BlockPos> leaves = new LinkedHashSet<>();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx * dx + dy * dy + dz * dz <= radius * radius + 1) {
                        leaves.add(new BlockPos(cx + dx, cy + dy, cz + dz));
                    }
                }
            }
        }
        return leaves;
    }

    private static void assertSixConnected(Set<BlockPos> trunk, Set<BlockPos> branches) {
        Set<BlockPos> reached = new LinkedHashSet<>(trunk);
        boolean added = true;
        while (added) {
            added = false;
            for (BlockPos b : branches) {
                if (reached.contains(b)) {
                    continue;
                }
                for (Direction dir : Direction.values()) {
                    if (reached.contains(b.relative(dir))) {
                        reached.add(b);
                        added = true;
                        break;
                    }
                }
            }
        }
        for (BlockPos b : branches) {
            assertTrue(reached.contains(b), "branch block must be 6-connected to trunk: " + b);
        }
    }
}
