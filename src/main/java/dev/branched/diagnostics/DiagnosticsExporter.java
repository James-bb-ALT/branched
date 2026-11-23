package dev.branched.diagnostics;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import dev.branched.Branched;
import dev.branched.compat.AdapterRegistry;
import dev.branched.compat.AdapterState;
import dev.branched.compat.ModAdapter;
import dev.branched.config.ArborealConfig;
import dev.branched.metrics.GenerationMetrics;
import dev.branched.metrics.MetricsRecorder;
import dev.branched.morphology.SemiproceduralMorphologyGenerator;
import dev.branched.platform.ForgeModPresence;
import dev.branched.platform.ModPresence;
import dev.branched.species.SpeciesProfile;
import dev.branched.species.SpeciesRegistry;
import dev.branched.worldgen.BranchMaterials;
import dev.branched.worldgen.feature.StaticTreeFeature;
import net.minecraftforge.fml.ModList;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * Exports a deterministic JSON snapshot of Branched configuration,
 * invariants, resolved compatibility adapters, validated species profiles, and
 * generation metrics to {@code logs/branched-diagnostics.json}.
 */
public final class DiagnosticsExporter {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    public static final String DEFAULT_RELATIVE_PATH = "logs/branched-diagnostics.json";

    private DiagnosticsExporter() {
    }

    /**
     * Pure-Java serialization entry point for tests and offline tools.
     */
    public static String toJson(SpeciesRegistry profiles,
                                AdapterRegistry adapters,
                                ModPresence presence,
                                MetricsRecorder metrics) {
        return toJson(profiles, adapters, presence, metrics, 0L, 0L, Branched.DEFAULT_VERSION);
    }

    /**
     * Full serialization entry point including feature placement counters and mod version.
     */
    public static String toJson(SpeciesRegistry profiles,
                                AdapterRegistry adapters,
                                ModPresence presence,
                                MetricsRecorder metrics,
                                long featurePlacedCount,
                                long featureRejectedCount,
                                String modVersion) {
        JsonObject root = new JsonObject();
        root.addProperty("modId", Branched.MOD_ID);
        root.addProperty("version", modVersion == null ? Branched.DEFAULT_VERSION : modVersion);
        root.addProperty("generatorVersion", SemiproceduralMorphologyGenerator.VERSION);

        root.add("config", configJson());
        root.add("invariants", invariantsJson(presence));
        root.add("adapters", adaptersJson(adapters, presence));
        root.add("profiles", profilesJson(profiles));
        root.add("metrics", metricsJson(metrics, featurePlacedCount, featureRejectedCount));
        return GSON.toJson(root) + "\n";
    }

    public static Path export(Path baseDir,
                              SpeciesRegistry profiles,
                              AdapterRegistry adapters,
                              ModPresence presence,
                              MetricsRecorder metrics) throws IOException {
        return export(baseDir, profiles, adapters, presence, metrics, 0L, 0L, Branched.DEFAULT_VERSION);
    }

    public static Path export(Path baseDir,
                              SpeciesRegistry profiles,
                              AdapterRegistry adapters,
                              ModPresence presence,
                              MetricsRecorder metrics,
                              long featurePlacedCount,
                              long featureRejectedCount,
                              String modVersion) throws IOException {
        Path out = baseDir.resolve(DEFAULT_RELATIVE_PATH);
        if (out.getParent() != null) {
            Files.createDirectories(out.getParent());
        }
        String payload = toJson(profiles, adapters, presence, metrics,
                featurePlacedCount, featureRejectedCount, modVersion);
        Files.writeString(out, payload, StandardCharsets.UTF_8);
        return out;
    }

    public static Path exportDefault(Path baseDir) throws IOException {
        String modVersion = ModList.get().getModContainerById(Branched.MOD_ID)
                .map(c -> c.getModInfo().getVersion().toString())
                .orElse(Branched.DEFAULT_VERSION);
        return export(baseDir,
                Branched.profiles(),
                Branched.adapters(),
                new ForgeModPresence(),
                Branched.metrics(),
                StaticTreeFeature.placedCount(),
                StaticTreeFeature.rejectedCount(),
                modVersion);
    }

    private static JsonObject configJson() {
        JsonObject config = new JsonObject();
        boolean loaded = ArborealConfig.SPEC.isLoaded();
        config.addProperty("loaded", loaded);
        config.addProperty("worldgenEnabled", !loaded || ArborealConfig.WORLDGEN_ENABLED.get());
        config.addProperty("replaceKnownFeatures", !loaded || ArborealConfig.REPLACE_KNOWN_FEATURES.get());
        config.addProperty("unknownFeaturePolicy",
                loaded ? ArborealConfig.UNKNOWN_FEATURE_POLICY.get() : "preserve_native");
        config.addProperty("replaceExistingChunks",
                loaded && ArborealConfig.REPLACE_EXISTING_CHUNKS.get());
        config.addProperty("renderPreset", loaded ? ArborealConfig.RENDER_PRESET.get() : "balanced");
        config.addProperty("useBlockEntities", loaded && ArborealConfig.USE_BLOCK_ENTITIES.get());
        config.addProperty("maxGenerationAttempts",
                loaded ? ArborealConfig.MAX_GENERATION_ATTEMPTS.get() : 3);
        config.addProperty("specialTreesPolicy",
                loaded ? ArborealConfig.SPECIAL_TREES.get() : "preserve_native");
        return config;
    }

    private static JsonObject invariantsJson(ModPresence presence) {
        JsonObject invariants = new JsonObject();
        invariants.addProperty("zeroBlockEntities", true);
        invariants.addProperty("zeroPeriodicSimulation", true);
        invariants.addProperty("noDynamicTreesDependency", true);
        invariants.addProperty("dynamicTreesLoaded", presence != null && presence.isLoaded("dynamictrees"));
        return invariants;
    }

    private static JsonObject adaptersJson(AdapterRegistry adapters, ModPresence presence) {
        JsonObject section = new JsonObject();
        Map<String, AdapterState> states = adapters.states(presence);
        section.addProperty("total", adapters.size());
        long readyCount = states.values().stream().filter(s -> s == AdapterState.READY).count();
        section.addProperty("readyCount", readyCount);
        JsonArray list = new JsonArray();
        for (ModAdapter adapter : adapters.all()) {
            JsonObject entry = new JsonObject();
            entry.addProperty("modId", adapter.modId());
            entry.addProperty("displayName", adapter.displayName());
            entry.addProperty("state", states.get(adapter.modId()).name());
            entry.addProperty("minVersion", adapter.minimumVersion());
            entry.addProperty("detectedVersion", presence.version(adapter.modId()).orElse("absent"));
            entry.addProperty("replaceableCount", adapter.replaceableFeatureCount());
            entry.addProperty("preservedCount", adapter.preservedFeatureCount());
            list.add(entry);
        }
        section.add("entries", list);
        return section;
    }

    private static JsonObject profilesJson(SpeciesRegistry profiles) {
        JsonObject section = new JsonObject();
        List<SpeciesProfile> all = profiles.all();
        long replaceable = all.stream().filter(p -> p.compatibility().mayReplace()).count();
        long preserved = all.size() - replaceable;
        section.addProperty("total", all.size());
        section.addProperty("replaceable", replaceable);
        section.addProperty("preserved", preserved);
        JsonArray entries = new JsonArray();
        for (SpeciesProfile profile : all) {
            JsonObject item = new JsonObject();
            item.addProperty("id", profile.id().toString());
            item.addProperty("archetype", profile.archetype().name());
            item.addProperty("compatibility", profile.compatibility().name());
            item.addProperty("log", profile.log().toString());
            item.addProperty("branch", BranchMaterials.resolve(profile.log()).toString());
            item.addProperty("leaves", profile.leaves().toString());
            entries.add(item);
        }
        section.add("entries", entries);
        return section;
    }

    private static JsonObject metricsJson(MetricsRecorder recorder,
                                          long featurePlacedCount,
                                          long featureRejectedCount) {
        GenerationMetrics snapshot = recorder.snapshot();
        JsonObject section = new JsonObject();
        section.addProperty("enabled", recorder.enabled());
        section.addProperty("generatedTrees", snapshot.generatedTrees());
        section.addProperty("rejectedPlacements", snapshot.rejectedPlacements());
        section.addProperty("PlacedCount", featurePlacedCount);
        section.addProperty("RejectedCount", featureRejectedCount);
        section.addProperty("segments", snapshot.segments());
        section.addProperty("leafClusters", snapshot.leafClusters());
        section.addProperty("blocks", snapshot.blocks());
        section.addProperty("estimatedQuads", snapshot.estimatedQuads());
        section.addProperty("avgMs", round3(snapshot.averageNanos() / 1_000_000.0D));
        section.addProperty("maxMs", round3(snapshot.maxNanos() / 1_000_000.0D));
        section.addProperty("p50Ms", round3(snapshot.p50Nanos() / 1_000_000.0D));
        section.addProperty("p95Ms", round3(snapshot.p95Nanos() / 1_000_000.0D));
        section.addProperty("p99Ms", round3(snapshot.p99Nanos() / 1_000_000.0D));
        section.addProperty("latencySamples", snapshot.latencySamples());
        section.addProperty("trackedProfiles", snapshot.trackedProfiles());
        JsonObject reasons = new JsonObject();
        recorder.rejectionsByReason().forEach(reasons::addProperty);
        section.add("rejectionsByReason", reasons);
        return section;
    }

    private static double round3(double value) {
        return Math.round(value * 1000.0D) / 1000.0D;
    }
}
