package dev.branched.compat;

import dev.branched.compat.adapter.AetherAdapter;
import dev.branched.compat.adapter.AlexsCavesAdapter;
import dev.branched.compat.adapter.BaobabAdapter;
import dev.branched.compat.adapter.BaobabTreeAdapter;
import dev.branched.compat.adapter.BetterEndAdapter;
import dev.branched.compat.adapter.BetterNetherAdapter;
import dev.branched.compat.adapter.BiomeMakeoverAdapter;
import dev.branched.compat.adapter.DeepAetherAdapter;
import dev.branched.compat.adapter.DeeperDarkerAdapter;
import dev.branched.compat.adapter.EcologicsAdapter;
import dev.branched.compat.adapter.FishOfThievesAdapter;
import dev.branched.compat.adapter.QuarkAdapter;
import dev.branched.compat.adapter.RegionsUnexploredAdapter;
import dev.branched.compat.adapter.TerralithAdapter;
import dev.branched.compat.adapter.TwilightDelightAdapter;
import dev.branched.compat.adapter.TwilightForestAdapter;
import dev.branched.compat.adapter.UpgradeAquaticAdapter;
import dev.branched.compat.adapter.VanillaAdapter;

import java.util.List;

/** Central list of isolated adapters. Adding a mod never touches the others. */
public final class DefaultAdapters {
    private DefaultAdapters() {
    }

    public static List<ModAdapter> create() {
        return List.of(
                new VanillaAdapter(),
                new RegionsUnexploredAdapter(),
                new TerralithAdapter(),
                new AetherAdapter(),
                new DeepAetherAdapter(),
                new TwilightForestAdapter(),
                new QuarkAdapter(),
                new FishOfThievesAdapter(),
                new EcologicsAdapter(),
                new UpgradeAquaticAdapter(),
                new DeeperDarkerAdapter(),
                new AlexsCavesAdapter(),
                new BaobabTreeAdapter(),
                new BiomeMakeoverAdapter(),
                new BaobabAdapter(),
                new BetterEndAdapter(),
                new BetterNetherAdapter(),
                new TwilightDelightAdapter());
    }
}
