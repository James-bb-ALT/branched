package dev.branched.worldgen;

import dev.branched.core.ResourceId;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AutoCompatResolverTest {

    @Test
    void heuristicLogDetectorRecognizesTreeLogsAndExcludesCropsFungiAndPalms() {
        assertTrue(AutoCompatResolver.isHeuristicLogId(ResourceId.parse("somemod:elder_log")));
        assertTrue(AutoCompatResolver.isHeuristicLogId(ResourceId.parse("somemod:elder_wood")));
        assertTrue(AutoCompatResolver.isHeuristicLogId(ResourceId.parse("somemod:arcane_stem")));
        assertTrue(AutoCompatResolver.isHeuristicLogId(ResourceId.parse("somemod:ancient_trunk")));
        assertTrue(AutoCompatResolver.isHeuristicLogId(ResourceId.parse("somemod:charred_bark")));

        assertFalse(AutoCompatResolver.isHeuristicLogId(ResourceId.parse("natures_spirit:desert_turnip_stem")));
        assertFalse(AutoCompatResolver.isHeuristicLogId(ResourceId.parse("natures_spirit:lotus_stem")));
        assertFalse(AutoCompatResolver.isHeuristicLogId(ResourceId.parse("regions_unexplored:bamboo_log")));
        assertFalse(AutoCompatResolver.isHeuristicLogId(ResourceId.parse("enhanced_mushrooms:mushroom_stem")));
        assertFalse(AutoCompatResolver.isHeuristicLogId(ResourceId.parse("biomesoplenty:palm_log")));
        assertFalse(AutoCompatResolver.isHeuristicLogId(ResourceId.parse("timberfall:cut_log")));
    }

    @Test
    void unknownLogsMatchCuratedKeywordsAndMapColorRgb() {
        assertEquals(BranchMaterials.BOP_FIR_BRANCH,
                AutoCompatResolver.resolveBranchForUnknownLog(
                        ResourceId.parse("biomesoplenty:fir_log"), 0x4A3620));
        assertEquals(BranchMaterials.BOP_MAHOGANY_BRANCH,
                AutoCompatResolver.resolveBranchForUnknownLog(
                        ResourceId.parse("somemod:royal_mahogany_log"), 0x000000));
        assertEquals(BranchMaterials.NS_CEDAR_BRANCH,
                AutoCompatResolver.resolveBranchForUnknownLog(
                        ResourceId.parse("somemod:frost_cedar_log"), 0x000000));

        // Opaque unknown names resolved by MapColor RGB
        assertEquals(BranchMaterials.BIRCH_BRANCH,
                AutoCompatResolver.resolveBranchForUnknownLog(
                        ResourceId.parse("somemod:chalk_timber_log"), 0xDAD6CE));
        assertEquals(BranchMaterials.BLACKWOOD_BRANCH,
                AutoCompatResolver.resolveBranchForUnknownLog(
                        ResourceId.parse("somemod:void_timber_log"), 0x201E22));
        assertEquals(BranchMaterials.BOP_JACARANDA_BRANCH,
                AutoCompatResolver.resolveBranchForUnknownLog(
                        ResourceId.parse("somemod:amethyst_timber_log"), 0x614872));
        assertEquals(BranchMaterials.REDWOOD_BRANCH,
                AutoCompatResolver.resolveBranchForUnknownLog(
                        ResourceId.parse("somemod:crimson_timber_log".replace("crimson", "scarlet")), 0x7E382C));
    }
}
