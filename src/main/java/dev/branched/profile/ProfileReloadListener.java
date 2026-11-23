package dev.branched.profile;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.logging.LogUtils;
import dev.branched.core.ResourceId;
import dev.branched.metrics.MetricsRecorder;
import dev.branched.species.SpeciesProfile;
import dev.branched.species.SpeciesRegistry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Datapack-aware profile reload. Reading happens off-thread during preparation;
 * the registry is swapped only in the apply stage, so worldgen never observes a
 * half-loaded profile set.
 */
public final class ProfileReloadListener implements PreparableReloadListener {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Gson GSON = new Gson();

    private final SpeciesRegistry registry;
    private final ProfileParser parser = new ProfileParser();
    private final MetricsRecorder metrics;

    public ProfileReloadListener(SpeciesRegistry registry, MetricsRecorder metrics) {
        this.registry = registry;
        this.metrics = metrics;
    }

    @Override
    public CompletableFuture<Void> reload(PreparationBarrier barrier,
                                          ResourceManager resourceManager,
                                          ProfilerFiller preparationsProfiler,
                                          ProfilerFiller reloadProfiler,
                                          Executor backgroundExecutor,
                                          Executor gameExecutor) {
        Map<ResourceId, JsonObject> files = readProfiles(resourceManager);
        return CompletableFuture.supplyAsync(() -> parser.parse(files), backgroundExecutor)
                .thenCompose(barrier::wait)
                .thenAcceptAsync(result -> apply(result), gameExecutor);
    }

    private Map<ResourceId, JsonObject> readProfiles(ResourceManager resourceManager) {
        Map<ResourceId, JsonObject> files = new LinkedHashMap<>();
        Map<ResourceLocation, Resource> found = resourceManager.listResources(
                ProfileParser.DIRECTORY, location -> location.getPath().endsWith(".json"));
        for (Map.Entry<ResourceLocation, Resource> entry : found.entrySet()) {
            ResourceLocation location = entry.getKey();
            ResourceId id = new ResourceId(location.getNamespace(),
                    location.getPath().substring(ProfileParser.DIRECTORY.length() + 1,
                            location.getPath().length() - ".json".length()));
            try (InputStreamReader reader = new InputStreamReader(
                    entry.getValue().open(), StandardCharsets.UTF_8)) {
                JsonElement element = GSON.fromJson(reader, JsonElement.class);
                if (element != null && element.isJsonObject()) {
                    files.put(id, element.getAsJsonObject());
                } else {
                    LOGGER.error("Profile {} is not a JSON object", location);
                }
            } catch (Exception ex) {
                LOGGER.error("Failed to read profile {}", location, ex);
            }
        }
        LOGGER.info("Branched: read {} species profile file(s)", files.size());
        return files;
    }

    private void apply(ProfileParser.ParseResult result) {
        if (result.profiles().isEmpty() && result.errorCount() > 0) {
            LOGGER.error("Branched: profile reload rejected; keeping previous registry ({} profiles)",
                    registry.size());
        } else {
            registry.replaceAll(result.profiles());
            LOGGER.info("Branched: loaded {} species profiles ({} errors, {} warnings)",
                    result.profiles().size(), result.errorCount(), result.warningCount());
        }
        for (String message : result.errorMessages()) {
            LOGGER.error("Profile error: {}", message);
        }
        if (metrics.enabled()) {
            LOGGER.info("Branched metrics: {}", metrics.snapshot().format());
        }
    }
}
