package party.lemons.biomemakeover.level.feature;

import com.mojang.serialization.Codec;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.TreeConfiguration;

public class WaterTreeFeature extends Feature<TreeConfiguration> {
    public WaterTreeFeature(Codec<TreeConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<TreeConfiguration> context) {
        return false;
    }
}
