package dev.branched.worldgen;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CustomTreeFeatureConverterTest {

    @Test
    void allowlistCoversAllModpackCustomTreeFeatures() {
        List<String> expectedClasses = List.of(
                // Regions Unexplored
                "net.regions_unexplored.world.level.feature.tree.LargeJoshuaTreeFeature",
                "net.regions_unexplored.world.level.feature.tree.MediumJoshuaTreeFeature",
                "net.regions_unexplored.world.level.feature.tree.SmallJoshuaTreeFeature",
                "net.regions_unexplored.world.level.feature.tree.LargeSocotraTreeFeature",
                "net.regions_unexplored.world.level.feature.tree.SmallSocotraTreeFeature",
                "net.regions_unexplored.world.level.feature.tree.MapleTreeFeature",
                "net.regions_unexplored.world.level.feature.tree.nether.BrimWillowFeature",
                "net.regions_unexplored.world.level.feature.tree.nether.TallBrimWillowFeature",
                "net.regions_unexplored.world.level.feature.tree.CobaltShrubFeature",
                "net.regions_unexplored.world.level.feature.tree.MegaBaobabTreeFeature",
                "net.regions_unexplored.world.level.feature.tree.UltraRedwoodTreeFeature",

                // Twilight Forest + Twilight Delight
                "twilightforest.world.components.feature.trees.CanopyTreeFeature",
                "twilightforest.world.components.feature.trees.DarkCanopyTreeFeature",
                "twilightforest.world.components.feature.trees.HollowTreeFeature",
                "twilightforest.world.components.feature.trees.TimeTreeFeature",
                "twilightforest.world.components.feature.trees.MiningTreeFeature",

                // BetterEnd
                "org.betterx.betterend.world.features.trees.DragonTreeFeature",
                "org.betterx.betterend.world.features.trees.DragonHelixTreeFeature",
                "org.betterx.betterend.world.features.trees.HelixTreeFeature",
                "org.betterx.betterend.world.features.trees.LacugroveFeature",
                "org.betterx.betterend.world.features.trees.LucerniaFeature",
                "org.betterx.betterend.world.features.trees.PythadendronTreeFeature",
                "org.betterx.betterend.world.features.trees.TenaneaFeature",
                "org.betterx.betterend.world.features.trees.UmbrellaTreeFeature",

                // BetterNether
                "org.betterx.betternether.world.features.AnchorTreeFeature",
                "org.betterx.betternether.world.features.GloomwoodTreeFeature",
                "org.betterx.betternether.world.features.NetherSakuraFeature",
                "org.betterx.betternether.world.features.OldWillowTree",
                "org.betterx.betternether.world.features.RubeusTreeFeature",
                "org.betterx.betternether.world.features.WartTreeFeature",
                "org.betterx.betternether.world.features.WillowTreeFeature",

                // Alex's Caves + Upgrade Aquatic
                "com.github.alexmodguy.alexscaves.server.level.feature.AncientTreeFeature",
                "com.github.alexmodguy.alexscaves.server.level.feature.GiantAncientTreeFeature",
                "com.github.alexmodguy.alexscaves.server.level.feature.LicorootTreeFeature",
                "com.github.alexmodguy.alexscaves.server.level.feature.LicorootTreeWithSproutsFeature",
                "com.teamabnormals.upgrade_aquatic.common.levelgen.feature.RiverTreeFeature"
        );

        for (String fqcn : expectedClasses) {
            CustomTreeFeatureConverter.TreeScanProfile profile =
                    CustomTreeFeatureConverter.profileForClassName(fqcn);
            assertNotNull(profile, "missing custom tree scan profile for " + fqcn);
            assertTrue(profile.xzRadius() >= 16);
        }

        CustomTreeFeatureConverter.TreeScanProfile anchor = CustomTreeFeatureConverter.profileForClassName(
                "org.betterx.betternether.world.features.AnchorTreeFeature");
        assertTrue(anchor.allowCeilingAnchor());
        assertFalse(CustomTreeFeatureConverter.profileForClassName(
                "net.regions_unexplored.world.level.feature.tree.LargeJoshuaTreeFeature").allowCeilingAnchor());
        assertTrue(CustomTreeFeatureConverter.profileForClassName(
                "net.regions_unexplored.world.level.feature.tree.CobaltShrubFeature").allowNonTagFoliage());
    }

    @Test
    void allowlistExcludesVanillaBranchlessFungiFallenLogsAndNativeBranchedFeatures() {
        List<String> excludedClasses = List.of(
                "net.minecraft.world.level.levelgen.feature.TreeFeature",
                "dev.branched.worldgen.feature.StaticTreeFeature",
                "party.lemons.biomemakeover.level.feature.WaterTreeFeature",
                "net.regions_unexplored.world.level.feature.tree.PalmTreeFeature",
                "net.regions_unexplored.world.level.feature.tree.BambooTreeFeature",
                "net.regions_unexplored.world.level.feature.tree.SaguaroCactusFeature",
                "net.regions_unexplored.world.level.feature.tree.FallenBlobTreeFeature",
                "org.betterx.betterend.world.features.trees.GiganticAmaranitaFeature",
                "org.betterx.betterend.world.features.trees.JellyshroomFeature",
                "org.betterx.betterend.world.features.trees.MossyGlowshroomFeature",
                "com.github.alexmodguy.alexscaves.server.level.feature.PewenTreeFeature",
                "com.github.alexmodguy.alexscaves.server.level.feature.ThornwoodTreeFeature",
                "net.minecraft.world.level.levelgen.feature.OreFeature",
                "net.minecraft.world.level.levelgen.feature.SimpleRandomSelectorFeature"
        );

        for (String fqcn : excludedClasses) {
            assertNull(CustomTreeFeatureConverter.profileForClassName(fqcn),
                    "feature must stay excluded from post-place custom tree conversion: " + fqcn);
        }
    }
}
