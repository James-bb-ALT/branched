package dev.branched.profile;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import dev.branched.core.ResourceId;
import dev.branched.species.SpeciesProfile;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Pure (Minecraft-free) profile parser shared by the runtime reload listener
 * and unit tests. It never throws for a single malformed profile: it reports
 * issues and keeps the remaining profiles usable.
 */
public final class ProfileParser {
    public static final String DIRECTORY = "arboreal/species";
    private static final Gson GSON = new Gson();

    private final ProfileValidator validator;

    public ProfileParser() {
        this(new ProfileValidator());
    }

    public ProfileParser(ProfileValidator validator) {
        this.validator = validator;
    }

    public ParseResult parse(Map<ResourceId, JsonObject> files) {
        List<SpeciesProfile> accepted = new ArrayList<>();
        List<ValidationResult> results = new ArrayList<>();
        Map<ResourceId, ResourceId> speciesSource = new LinkedHashMap<>();

        for (Map.Entry<ResourceId, JsonObject> entry : files.entrySet()) {
            ValidationResult result = validator.validate(entry.getKey(), entry.getValue());
            results.add(result);
            if (result.profile().isEmpty()) {
                continue;
            }
            SpeciesProfile profile = result.profile().get();
            ResourceId previous = speciesSource.putIfAbsent(profile.id(), entry.getKey());
            if (previous != null) {
                results.add(new ValidationResult(entry.getKey(), null, List.of(
                        ValidationIssue.error("duplicate_species", "$.species",
                                "species " + profile.id() + " already declared by " + previous))));
                continue;
            }
            accepted.add(profile);
        }
        accepted.sort(java.util.Comparator.comparing(SpeciesProfile::id));
        return new ParseResult(accepted, results);
    }

    /** Loads profiles from a filesystem directory; used by tests and tooling. */
    public ParseResult parseDirectory(Path root) throws IOException {
        Map<ResourceId, JsonObject> files = new LinkedHashMap<>();
        if (!Files.isDirectory(root)) {
            return new ParseResult(List.of(), List.of());
        }
        try (Stream<Path> stream = Files.walk(root)) {
            List<Path> paths = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".json"))
                    .sorted()
                    .toList();
            for (Path path : paths) {
                String relative = root.relativize(path).toString().replace('\\', '/');
                String pathId = relative.substring(0, relative.length() - ".json".length());
                ResourceId fileId = new ResourceId("branched", pathId);
                try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                    JsonElement element = GSON.fromJson(reader, JsonElement.class);
                    if (element == null || !element.isJsonObject()) {
                        files.put(fileId, new JsonObject());
                    } else {
                        files.put(fileId, element.getAsJsonObject());
                    }
                } catch (JsonParseException ex) {
                    files.put(fileId, new JsonObject());
                }
            }
        }
        return parse(files);
    }

    /** Builds a registry from a parse result, throwing if a duplicate slipped through. */
    public static void apply(ParseResult result, dev.branched.species.SpeciesRegistry registry) {
        registry.replaceAll(result.profiles());
    }

    public record ParseResult(List<SpeciesProfile> profiles, List<ValidationResult> results) {
        public long errorCount() {
            return results.stream().flatMap(r -> r.issues().stream())
                    .filter(i -> i.severity() == ValidationIssue.Severity.ERROR)
                    .count();
        }

        public long warningCount() {
            return results.stream().flatMap(r -> r.issues().stream())
                    .filter(i -> i.severity() == ValidationIssue.Severity.WARNING)
                    .count();
        }

        public List<String> errorMessages() {
            List<String> messages = new ArrayList<>();
            for (ValidationResult result : results) {
                for (ValidationIssue issue : result.errors()) {
                    messages.add(result.source() + " " + issue);
                }
            }
            return messages;
        }
    }
}
