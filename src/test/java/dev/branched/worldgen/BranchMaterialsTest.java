package dev.branched.worldgen;

import dev.branched.core.ResourceId;
import dev.branched.profile.ProfileParser;
import dev.branched.species.SpeciesProfile;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BranchMaterialsTest {
    @Test
    void vanillaAndModdedLogsResolveToDedicatedStaticBranchBlocks() {
        assertEquals(BranchMaterials.OAK_BRANCH,
                BranchMaterials.resolve(ResourceId.parse("minecraft:oak_log")));
        assertEquals(BranchMaterials.SPRUCE_BRANCH,
                BranchMaterials.resolve(ResourceId.parse("minecraft:spruce_log")));
        assertEquals(BranchMaterials.BIRCH_BRANCH,
                BranchMaterials.resolve(ResourceId.parse("minecraft:birch_log")));
        assertEquals(BranchMaterials.JUNGLE_BRANCH,
                BranchMaterials.resolve(ResourceId.parse("minecraft:jungle_log")));
        assertEquals(BranchMaterials.DARK_OAK_BRANCH,
                BranchMaterials.resolve(ResourceId.parse("minecraft:dark_oak_log")));
        assertEquals(BranchMaterials.PINE_BRANCH,
                BranchMaterials.resolve(ResourceId.parse("regions_unexplored:pine_log")));
        assertEquals(BranchMaterials.BAOBAB_BRANCH,
                BranchMaterials.resolve(ResourceId.parse("regions_unexplored:baobab_log")));
        assertEquals(BranchMaterials.WALNUT_BRANCH,
                BranchMaterials.resolve(ResourceId.parse("ecologics:walnut_log")));
        assertEquals(BranchMaterials.RIVER_BRANCH,
                BranchMaterials.resolve(ResourceId.parse("upgrade_aquatic:river_log")));
        assertEquals(BranchMaterials.ECHO_BRANCH,
                BranchMaterials.resolve(ResourceId.parse("deeperdarker:echo_log")));
        assertEquals(BranchMaterials.BAOBABTREE_BRANCH,
                BranchMaterials.resolve(ResourceId.parse("baobabtree:baobab_wood")));
        assertEquals(BranchMaterials.BM_ANCIENT_OAK_BRANCH,
                BranchMaterials.resolve(ResourceId.parse("biomemakeover:ancient_oak_log")));
        assertEquals(BranchMaterials.BM_BLIGHTED_BALSA_BRANCH,
                BranchMaterials.resolve(ResourceId.parse("biomemakeover:blighted_balsa_log")));
        assertEquals(BranchMaterials.BM_SWAMP_CYPRESS_BRANCH,
                BranchMaterials.resolve(ResourceId.parse("biomemakeover:swamp_cypress_log")));
        assertEquals(BranchMaterials.BM_WILLOW_BRANCH,
                BranchMaterials.resolve(ResourceId.parse("biomemakeover:willow_log")));
        assertEquals(BranchMaterials.TIME_BRANCH,
                BranchMaterials.resolve(ResourceId.parse("twilightforest:time_log")));
        assertEquals(BranchMaterials.TRANSFORMATION_BRANCH,
                BranchMaterials.resolve(ResourceId.parse("twilightforest:transformation_log")));
        assertEquals(BranchMaterials.SORTING_BRANCH,
                BranchMaterials.resolve(ResourceId.parse("twilightforest:sorting_log")));
    }

    @Test
    void woodAndStrippedVariantsResolveLikeTheirLog() {
        assertEquals(BranchMaterials.OAK_BRANCH,
                BranchMaterials.resolve(ResourceId.parse("minecraft:oak_wood")));
        assertEquals(BranchMaterials.SPRUCE_BRANCH,
                BranchMaterials.resolve(ResourceId.parse("minecraft:spruce_wood")));
        assertEquals(BranchMaterials.ACACIA_BRANCH,
                BranchMaterials.resolve(ResourceId.parse("minecraft:acacia_wood")));
        assertEquals(BranchMaterials.PINE_BRANCH,
                BranchMaterials.resolve(ResourceId.parse("regions_unexplored:pine_wood")));
        assertEquals(BranchMaterials.BM_WILLOW_BRANCH,
                BranchMaterials.resolve(ResourceId.parse("biomemakeover:willow_wood")));
        assertEquals(BranchMaterials.WARPED_BRANCH,
                BranchMaterials.resolve(ResourceId.parse("minecraft:stripped_warped_stem")));
        assertEquals(BranchMaterials.SPRUCE_BRANCH,
                BranchMaterials.resolve(ResourceId.parse("minecraft:stripped_spruce_log")));
        assertEquals(BranchMaterials.BM_WILLOW_BRANCH,
                BranchMaterials.resolve(ResourceId.parse("biomemakeover:stripped_willow_log")));

        assertTrue(BranchMaterials.hasExactMapping(ResourceId.parse("biomemakeover:willow_log")));
        assertTrue(BranchMaterials.hasExactMapping(ResourceId.parse("baobabtree:baobab_wood")));
        assertTrue(BranchMaterials.hasExactMapping(ResourceId.parse("minecraft:stripped_warped_stem")));
        assertFalse(BranchMaterials.hasExactMapping(ResourceId.parse("minecraft:dirt")));
        assertFalse(BranchMaterials.hasExactMapping(ResourceId.parse("betterend:dragon_tree_log")));
    }

    @Test
    void everyShippedSpeciesProfileResolvesToAStaticBranchBlockWithoutFullLogFallback() throws Exception {
        var parsed = new ProfileParser().parseDirectory(
                Path.of("src/main/resources/data/branched/arboreal/species"));
        assertTrue(parsed.profiles().size() >= 70);
        for (SpeciesProfile profile : parsed.profiles()) {
            ResourceId resolved = BranchMaterials.resolve(profile.log());
            assertEquals("branched", resolved.namespace(),
                    profile.id() + " log " + profile.log() + " must never fall back to full 16x16x16 log");
            assertTrue(resolved.path().endsWith("_branch"),
                    profile.id() + " must resolve to a *_branch block, got " + resolved);
            assertTrue(BranchMaterials.hasBranchMaterial(profile.log()));
        }
    }

    @Test
    void unknownModdedLogsFallbackByKeywordToClosestBranchMaterial() {
        assertEquals(BranchMaterials.PINE_BRANCH,
                BranchMaterials.resolve(ResourceId.parse("somemod:elder_pine_log")));
        assertEquals(BranchMaterials.BIRCH_BRANCH,
                BranchMaterials.resolve(ResourceId.parse("somemod:silver_aspen_log")));
        assertEquals(BranchMaterials.OAK_BRANCH,
                BranchMaterials.resolve(ResourceId.parse("somemod:mystery_timber_log")));
    }

    @Test
    void nullLogIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> BranchMaterials.resolve(null));
    }
}
