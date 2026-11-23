package dev.branched;

import com.mojang.logging.LogUtils;
import dev.branched.compat.AdapterRegistry;
import dev.branched.compat.AdapterState;
import dev.branched.compat.DefaultAdapters;
import dev.branched.compat.ModAdapter;
import dev.branched.config.ArborealConfig;
import dev.branched.diagnostics.ArborealCommands;
import dev.branched.diagnostics.WorldgenSelfTest;
import dev.branched.metrics.MetricsRecorder;
import dev.branched.morphology.MorphologyGenerator;
import dev.branched.morphology.SemiproceduralMorphologyGenerator;
import dev.branched.platform.ForgeModPresence;
import dev.branched.platform.ModPresence;
import dev.branched.profile.ProfileReloadListener;
import dev.branched.registry.ModBiomeModifiers;
import dev.branched.registry.ModBlocks;
import dev.branched.registry.ModFeatures;
import dev.branched.registry.ModItems;
import dev.branched.species.SpeciesProfile;
import dev.branched.species.SpeciesRegistry;
import dev.branched.worldgen.feature.StaticTreeFeature;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddReloadListenerEvent;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

import java.util.Map;

/**
 * Branched entry point.
 *
 * <p>Phase 0 wires configuration, optional-mod detection, the datapack profile
 * loader, the deterministic morphology generator and per-mod adapters. It does
 * not yet replace worldgen features: that gate opens only after a species is
 * verified end to end.</p>
 */
@Mod(Branched.MOD_ID)
public final class Branched {
    public static final String MOD_ID = "branched";
    public static final String DEFAULT_VERSION = "0.7.0";
    public static final Logger LOGGER = LogUtils.getLogger();

    private static final SpeciesRegistry PROFILES = new SpeciesRegistry();
    private static final AdapterRegistry ADAPTERS = new AdapterRegistry();
    private static final MetricsRecorder METRICS = new MetricsRecorder(true);
    private static final MorphologyGenerator GENERATOR = new SemiproceduralMorphologyGenerator(METRICS);

    public Branched() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ArborealConfig.SPEC);

        IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();
        modBus.addListener(this::commonSetup);
        modBus.addListener(this::onBuildCreativeTabs);

        ModBlocks.BLOCKS.register(modBus);
        ModItems.ITEMS.register(modBus);
        ModFeatures.FEATURES.register(modBus);
        ModBiomeModifiers.SERIALIZERS.register(modBus);

        MinecraftForge.EVENT_BUS.addListener(this::onAddReloadListeners);
        MinecraftForge.EVENT_BUS.addListener(ArborealCommands::onRegisterCommands);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStarted);
        MinecraftForge.EVENT_BUS.addListener(this::onServerStopped);
        LOGGER.info("Branched {} initialising", MOD_ID);
    }

    private void onBuildCreativeTabs(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.BUILDING_BLOCKS) {
            ModItems.allBranchItems().values().forEach(item -> event.accept(item));
        }
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        event.enqueueWork(() -> {
            ModPresence presence = new ForgeModPresence();
            ADAPTERS.replaceAll(DefaultAdapters.create());
            Map<String, AdapterState> states = ADAPTERS.states(presence);
            for (ModAdapter adapter : ADAPTERS.all()) {
                LOGGER.info("Adapter {} ({}): {} | {} replaceable, {} preserved",
                        adapter.modId(), adapter.displayName(), states.get(adapter.modId()),
                        adapter.replaceableFeatureCount(), adapter.preservedFeatureCount());
            }
            if (presence.isLoaded("dynamictrees") && ArborealConfig.WARN_IF_DYNAMICTREES_LOADED.get()) {
                LOGGER.warn("Dynamic Trees is loaded. Branched does not depend on it and will not "
                        + "replace its trees; existing Dynamic Trees blocks require the opt-in migration tool.");
            }
            LOGGER.info("Branched generator v{} ready; species profiles pending datapack reload",
                    GENERATOR.version());
        });
    }

    private void onAddReloadListeners(AddReloadListenerEvent event) {
        event.addListener(new ProfileReloadListener(PROFILES, METRICS));
    }

    private void onServerStarted(ServerStartedEvent event) {
        LOGGER.info("Branched worldgen active: {} profiles, {} adapters",
                PROFILES.size(), ADAPTERS.size());
        WorldgenSelfTest.onServerStarted(event);
    }

    private void onServerStopped(ServerStoppedEvent event) {
        LOGGER.info("Branched metrics: {}", METRICS.snapshot());
        LOGGER.info("Branched rejection reasons: {}", METRICS.rejectionsByReason());
        LOGGER.info("Branched feature results: placed={} rejected={}",
                StaticTreeFeature.placedCount(), StaticTreeFeature.rejectedCount());
    }

    public static SpeciesRegistry profiles() {
        return PROFILES;
    }

    public static AdapterRegistry adapters() {
        return ADAPTERS;
    }

    public static MetricsRecorder metrics() {
        return METRICS;
    }

    public static MorphologyGenerator generator() {
        return GENERATOR;
    }

    /** Diagnostics used by the Phase 0 self-check command and tests. */
    public static String describeProfiles() {
        StringBuilder sb = new StringBuilder();
        sb.append("profiles=").append(PROFILES.size()).append(' ');
        for (SpeciesProfile profile : PROFILES.all()) {
            sb.append(profile.id()).append('(').append(profile.compatibility()).append(") ");
        }
        return sb.toString().trim();
    }
}
