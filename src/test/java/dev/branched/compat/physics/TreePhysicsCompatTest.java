package dev.branched.compat.physics;

import dev.branched.core.ResourceId;
import dev.branched.mixin.BranchedMixinPlugin;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.AABB;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TreePhysicsCompatTest {

    private record TestNode(ResourceId id, boolean branch, boolean log, int connMask) {
    }

    private static final class MapNodeView implements TreePhysicsCompat.WoodNodeView<TestNode> {
        private static final TestNode AIR = new TestNode(ResourceId.parse("minecraft:air"), false, false, 0);
        private final Map<BlockPos, TestNode> nodes;

        private MapNodeView(Map<BlockPos, TestNode> nodes) {
            this.nodes = nodes;
        }

        @Override
        public TestNode stateAt(BlockPos pos) {
            return nodes.getOrDefault(pos, AIR);
        }

        @Override
        public boolean isBranch(TestNode state) {
            return state != null && state.branch();
        }

        @Override
        public boolean isLogOrBranch(TestNode state) {
            return state != null && (state.branch() || state.log());
        }

        @Override
        public boolean matchesTreeWood(TestNode startWood, TestNode candidate) {
            if (startWood == null || candidate == null) {
                return false;
            }
            if (startWood.id().equals(candidate.id())) {
                return isLogOrBranch(candidate);
            }
            if (!candidate.branch()) {
                return false;
            }
            return TreePhysicsCompat.matchesWoodIds(startWood.id(), candidate.id());
        }

        @Override
        public boolean isConnectedBetween(TestNode fromState, BlockPos fromPos, TestNode toState, BlockPos toPos) {
            if (fromState == null || toState == null) {
                return false;
            }
            return TreePhysicsCompat.computeIsConnectedBetween(
                    fromState.branch(), isLogOrBranch(fromState), fromState.connMask(), fromPos,
                    toState.branch(), isLogOrBranch(toState), toState.connMask(), toPos);
        }
    }

    @Test
    void matchesWoodIdsCoversAllBranchBlocksAndMismatchesOtherWoodFamilies() throws Exception {
        List<String> allBranchNames;
        try (Stream<Path> stream = Files.list(Path.of("src/main/resources/assets/branched/blockstates"))) {
            allBranchNames = stream.filter(Files::isRegularFile)
                    .map(p -> p.getFileName().toString())
                    .filter(n -> n.endsWith("_branch.json"))
                    .map(n -> n.substring(0, n.length() - ".json".length()))
                    .sorted()
                    .toList();
        }
        assertEquals(83, allBranchNames.size());

        Map<String, String> trunkToBranch = new HashMap<>();
        trunkToBranch.put("minecraft:oak_log", "oak_branch");
        trunkToBranch.put("minecraft:spruce_log", "spruce_branch");
        trunkToBranch.put("minecraft:birch_log", "birch_branch");
        trunkToBranch.put("minecraft:jungle_log", "jungle_branch");
        trunkToBranch.put("minecraft:acacia_log", "acacia_branch");
        trunkToBranch.put("minecraft:dark_oak_log", "dark_oak_branch");
        trunkToBranch.put("minecraft:mangrove_log", "mangrove_branch");
        trunkToBranch.put("minecraft:cherry_log", "cherry_branch");
        trunkToBranch.put("minecraft:crimson_stem", "crimson_branch");
        trunkToBranch.put("minecraft:warped_stem", "warped_branch");
        trunkToBranch.put("minecraft:pale_oak_log", "pale_oak_branch");
        trunkToBranch.put("minecraft:poplar_log", "poplar_branch");
        trunkToBranch.put("regions_unexplored:pine_log", "pine_branch");
        trunkToBranch.put("regions_unexplored:larch_log", "larch_branch");
        trunkToBranch.put("regions_unexplored:maple_log", "maple_branch");
        trunkToBranch.put("regions_unexplored:magnolia_log", "magnolia_branch");
        trunkToBranch.put("regions_unexplored:blackwood_log", "blackwood_branch");
        trunkToBranch.put("regions_unexplored:redwood_log", "redwood_branch");
        trunkToBranch.put("regions_unexplored:kapok_log", "kapok_branch");
        trunkToBranch.put("regions_unexplored:eucalyptus_log", "eucalyptus_branch");
        trunkToBranch.put("regions_unexplored:baobab_log", "baobab_branch");
        trunkToBranch.put("regions_unexplored:cypress_log", "cypress_branch");
        trunkToBranch.put("regions_unexplored:palm_log", "palm_branch");
        trunkToBranch.put("regions_unexplored:willow_log", "willow_branch");
        trunkToBranch.put("regions_unexplored:silver_birch_log", "silver_birch_branch");
        trunkToBranch.put("regions_unexplored:ashen_log", "ashen_branch");
        trunkToBranch.put("regions_unexplored:dead_log", "dead_branch");
        trunkToBranch.put("regions_unexplored:mauve_log", "mauve_branch");
        trunkToBranch.put("regions_unexplored:socotra_log", "socotra_branch");
        trunkToBranch.put("regions_unexplored:joshua_log", "joshua_branch");
        trunkToBranch.put("regions_unexplored:brimwood_log", "brimwood_branch");
        trunkToBranch.put("regions_unexplored:cobalt_log", "cobalt_branch");
        trunkToBranch.put("regions_unexplored:alpha_log", "alpha_oak_branch");
        trunkToBranch.put("regions_unexplored:small_oak_log", "small_oak_branch");
        trunkToBranch.put("aether:skyroot_log", "skyroot_branch");
        trunkToBranch.put("aether:golden_oak_log", "golden_oak_branch");
        trunkToBranch.put("deep_aether:roseroot_log", "roseroot_branch");
        trunkToBranch.put("deep_aether:yagroot_log", "yagroot_branch");
        trunkToBranch.put("deep_aether:cruderoot_log", "cruderoot_branch");
        trunkToBranch.put("deep_aether:conberry_log", "conberry_branch");
        trunkToBranch.put("deep_aether:sunroot_log", "sunroot_branch");
        trunkToBranch.put("quark:blossom_log", "blossom_branch");
        trunkToBranch.put("quark:ancient_log", "ancient_branch");
        trunkToBranch.put("quark:azalea_log", "azalea_branch");
        trunkToBranch.put("fishofthieves:banana_stem", "banana_branch");
        trunkToBranch.put("fishofthieves:coconut_log", "fot_coconut_branch");
        trunkToBranch.put("twilightforest:twilight_oak_log", "twilight_oak_branch");
        trunkToBranch.put("twilightforest:canopy_log", "canopy_branch");
        trunkToBranch.put("twilightforest:dark_log", "darkwood_branch");
        trunkToBranch.put("twilightforest:mangrove_log", "twilight_mangrove_branch");
        trunkToBranch.put("twilightforest:mining_log", "mining_branch");
        trunkToBranch.put("twilightforest:time_log", "time_branch");
        trunkToBranch.put("twilightforest:transformation_log", "transformation_branch");
        trunkToBranch.put("twilightforest:sorting_log", "sorting_branch");
        trunkToBranch.put("biomemakeover:ancient_oak_log", "bm_ancient_oak_branch");
        trunkToBranch.put("biomemakeover:blighted_balsa_log", "bm_blighted_balsa_branch");
        trunkToBranch.put("biomemakeover:swamp_cypress_log", "bm_swamp_cypress_branch");
        trunkToBranch.put("biomemakeover:willow_log", "bm_willow_branch");
        trunkToBranch.put("ecologics:walnut_log", "walnut_branch");
        trunkToBranch.put("ecologics:coconut_log", "coconut_branch");
        trunkToBranch.put("ecologics:azalea_log", "ecologics_azalea_branch");
        trunkToBranch.put("upgrade_aquatic:river_log", "river_branch");
        trunkToBranch.put("baobabtree:baobab_log", "baobabtree_branch");
        trunkToBranch.put("deeperdarker:echo_log", "echo_branch");
        trunkToBranch.put("alexscaves:pewen_log", "pewen_branch");
        trunkToBranch.put("alexscaves:thornwood_log", "thornwood_branch");
        trunkToBranch.put("alexscaves:licoroot", "licoroot_branch");
        trunkToBranch.put("deep_aether:rotten_roseroot_log", "rotten_roseroot_branch");
        trunkToBranch.put("twilightdelight:ironwood_log", "ironwood_branch");
        trunkToBranch.put("betterend:dragon_tree_log", "dragon_tree_branch");
        trunkToBranch.put("betterend:helix_tree_log", "helix_tree_branch");
        trunkToBranch.put("betterend:lacugrove_log", "lacugrove_branch");
        trunkToBranch.put("betterend:lucernia_log", "lucernia_branch");
        trunkToBranch.put("betterend:pythadendron_log", "pythadendron_branch");
        trunkToBranch.put("betterend:tenanea_log", "tenanea_branch");
        trunkToBranch.put("betterend:umbrella_tree_log", "umbrella_tree_branch");
        trunkToBranch.put("betternether:anchor_tree_log", "anchor_tree_branch");
        trunkToBranch.put("betternether:gloomwood_log", "gloomwood_branch");
        trunkToBranch.put("betternether:gloomwood_dark_log", "gloomwood_dark_branch");
        trunkToBranch.put("betternether:nether_sakura_log", "nether_sakura_branch");
        trunkToBranch.put("betternether:rubeus_log", "rubeus_branch");
        trunkToBranch.put("betternether:willow_log", "bn_willow_branch");
        trunkToBranch.put("betternether:wart_log", "bn_wart_branch");
        assertEquals(allBranchNames.size(), trunkToBranch.size(), "every one of the 83 branches must be covered");

        for (Map.Entry<String, String> entry : trunkToBranch.entrySet()) {
            ResourceId trunkId = ResourceId.parse(entry.getKey());
            ResourceId branchId = ResourceId.parse("branched:" + entry.getValue());
            assertTrue(allBranchNames.contains(entry.getValue()), "unknown branch " + entry.getValue());
            assertTrue(TreePhysicsCompat.matchesWoodIds(trunkId, branchId),
                    entry.getKey() + " should match " + branchId);
        }
        assertTrue(TreePhysicsCompat.matchesWoodIds(
                ResourceId.parse("baobabtree:baobab_wood"),
                ResourceId.parse("branched:baobabtree_branch")));
        assertTrue(TreePhysicsCompat.matchesWoodIds(
                ResourceId.parse("biomemakeover:willow_wood"),
                ResourceId.parse("branched:bm_willow_branch")));

        // Cross-species and non-wood mismatch checks
        assertFalse(TreePhysicsCompat.matchesWoodIds(
                ResourceId.parse("minecraft:oak_log"), ResourceId.parse("branched:pine_branch")));
        assertFalse(TreePhysicsCompat.matchesWoodIds(
                ResourceId.parse("minecraft:oak_log"), ResourceId.parse("minecraft:stone")));
    }

    @Test
    void collisionBoundsHalfWidthMassAndAxisReflectThicknessAndConnections() {
        for (int thickness = 1; thickness <= 3; thickness++) {
            AABB coreBounds = TreePhysicsCompat.computeBranchCollisionBounds(
                    thickness, false, false, false, false, false, false);
            double halfPixels = (thickness == 3) ? 6.0D : (2.0D + thickness);
            double expectedMin = (8.0D - halfPixels) / 16.0D;
            double expectedMax = (8.0D + halfPixels) / 16.0D;
            assertEquals(expectedMin, coreBounds.minX, 1e-6);
            assertEquals(expectedMax, coreBounds.maxX, 1e-6);
            assertEquals(halfPixels / 16.0D, TreePhysicsCompat.computeBranchHalfWidth(thickness), 1e-6);
            double mass = TreePhysicsCompat.computeBranchMass(coreBounds);
            assertTrue(mass < 4.0D && mass > 0.0D);
        }

        AABB eastBounds = TreePhysicsCompat.computeBranchCollisionBounds(
                3, false, false, true, false, false, false);
        assertEquals(2.0D / 16.0D, eastBounds.minX, 1e-6);
        assertEquals(1.0D, eastBounds.maxX, 1e-6);
        assertEquals(Direction.Axis.X, TreePhysicsCompat.computePrimaryBranchAxis(
                false, false, true, false, false, false));
        assertEquals(Direction.Axis.Z, TreePhysicsCompat.computePrimaryBranchAxis(
                true, false, false, false, false, false));
        assertEquals(Direction.Axis.Y, TreePhysicsCompat.computePrimaryBranchAxis(
                false, false, false, false, true, false));

        int westMask = TreePhysicsCompat.connectionMask(false, false, false, true, false, false);
        int downMask = TreePhysicsCompat.connectionMask(false, false, false, false, false, true);
        assertTrue(TreePhysicsCompat.computeIsConnectedBetween(
                false, true, 0, new BlockPos(0, 66, 0),
                true, true, westMask, new BlockPos(1, 66, 0)));
        assertTrue(TreePhysicsCompat.computeIsConnectedBetween(
                true, true, westMask, new BlockPos(1, 66, 0),
                true, true, downMask, new BlockPos(2, 65, 0)));
    }

    @Test
    void expandWithConnectedBranchesIncludesDownwardDroopingTipsAndRejectsForeignBranches() {
        ResourceId oakLogId = ResourceId.parse("minecraft:oak_log");
        ResourceId oakBranchId = ResourceId.parse("branched:oak_branch");
        ResourceId pineBranchId = ResourceId.parse("branched:pine_branch");

        TestNode oakLogNode = new TestNode(oakLogId, false, true, 0);
        int westMask = TreePhysicsCompat.connectionMask(false, false, false, true, false, false);
        int downMask = TreePhysicsCompat.connectionMask(false, false, false, false, false, true);
        int upMask = TreePhysicsCompat.connectionMask(false, false, false, false, true, false);

        Map<BlockPos, TestNode> world = new HashMap<>();
        BlockPos trunkBase = new BlockPos(0, 64, 0);
        BlockPos trunkMid = new BlockPos(0, 65, 0);
        BlockPos trunkTop = new BlockPos(0, 66, 0);
        BlockPos armEast = new BlockPos(1, 66, 0);
        BlockPos droopDown = new BlockPos(2, 65, 0);
        BlockPos droopLeafEnd = new BlockPos(2, 64, 0);
        BlockPos foreignPine = new BlockPos(1, 67, 0);

        world.put(trunkBase, oakLogNode);
        world.put(trunkMid, oakLogNode);
        world.put(trunkTop, oakLogNode);
        world.put(armEast, new TestNode(oakBranchId, true, true, westMask));
        world.put(droopDown, new TestNode(oakBranchId, true, true, downMask));
        world.put(droopLeafEnd, new TestNode(oakBranchId, true, true, upMask));
        world.put(foreignPine, new TestNode(pineBranchId, true, true, downMask));

        MapNodeView view = new MapNodeView(world);
        Set<BlockPos> baseLogs = new LinkedHashSet<>(List.of(trunkBase, trunkMid, trunkTop));

        Set<BlockPos> expanded = TreePhysicsCompat.expandWithConnectedBranches(view, baseLogs, 64);
        assertTrue(expanded.contains(armEast));
        assertTrue(expanded.contains(droopDown), "diagonal downward branch tip (dy=-1) must be collected");
        assertTrue(expanded.contains(droopLeafEnd), "vertical downward branch continuation must be collected");
        assertFalse(expanded.contains(foreignPine), "unrelated pine branch must not merge into oak tree");
        assertEquals(6, expanded.size());
    }

    @Test
    void mixinPluginEnablesMixinsOnlyForPresentTargetClasses() {
        BranchedMixinPlugin plugin = new BranchedMixinPlugin();
        assertTrue(plugin.shouldApplyMixin(
                "dev.branched.compat.physics.TreePhysicsCompat",
                "dev.branched.mixin.Dummy"));
        assertFalse(plugin.shouldApplyMixin(
                "com.example.nonexistent.AbsentThirdPartyClass",
                "dev.branched.mixin.Dummy"));
        assertFalse(plugin.shouldApplyMixin(null, "dev.branched.mixin.Dummy"));
    }
}
