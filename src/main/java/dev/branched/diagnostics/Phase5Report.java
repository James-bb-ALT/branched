package dev.branched.diagnostics;

import dev.branched.core.ResourceId;
import dev.branched.morphology.BlockPalette;
import dev.branched.morphology.BlockPlacement;
import dev.branched.morphology.CanonicalShape;
import dev.branched.morphology.MorphologyRequest;
import dev.branched.morphology.PerformancePreset;
import dev.branched.morphology.RasterizedTree;
import dev.branched.morphology.ReferenceBlockRasterizer;
import dev.branched.morphology.SemiproceduralMorphologyGenerator;
import dev.branched.morphology.TreeSkeleton;
import dev.branched.profile.ProfileParser;
import dev.branched.species.CompatibilityLevel;
import dev.branched.species.SpeciesProfile;
import dev.branched.worldgen.BranchMaterials;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Pure-Java Phase 5 measurement. Generates a fixed sample set per shipped
 * species and reports structural diversity plus geometry/latency numbers.
 *
 * <p>It never bootstraps Minecraft: it uses the same morphology and
 * rasterization classes as worldgen, with a synthetic world seed and positions.
 * Run with {@code gradlew phase5Report}; outputs
 * {@code docs/phase5-diversity.json} and {@code catalogs/phase5-diversity.csv}.</p>
 */
public final class Phase5Report {
    private static final Path DEFAULT_PROFILES =
            Path.of("src/main/resources/data/branched/arboreal/species");
    private static final Path DEFAULT_OUTPUT = Path.of("docs");
    private static final int DEFAULT_SAMPLES = 64;
    private static final long BASE_SEED = 0x5EED_1A5L;
    private static final ResourceId OVERWORLD = ResourceId.parse("minecraft:overworld");
    private static final List<String> PRESET_PROFILES = List.of(
            "minecraft:oak", "minecraft:spruce", "minecraft:jungle", "twilightforest:canopy");

    private Phase5Report() {
    }

    public static void main(String[] args) throws IOException {
        Path profilesRoot = Path.of(args.length > 0 ? args[0] : DEFAULT_PROFILES.toString());
        Path output = Path.of(args.length > 1 ? args[1] : DEFAULT_OUTPUT.toString());
        int samples = args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_SAMPLES;

        Map<String, SpeciesProfile> profiles = new LinkedHashMap<>();
        new ProfileParser().parseDirectory(profilesRoot).profiles()
                .forEach(profile -> profiles.put(profile.id().toString(), profile));

        List<Result> results = new ArrayList<>();
        for (SpeciesProfile profile : profiles.values()) {
            results.add(measure(profile, samples));
        }

        Files.createDirectories(output);
        Files.createDirectories(Path.of("catalogs"));
        Files.writeString(output.resolve("phase5-diversity.json"), json(results, profiles, samples),
                StandardCharsets.UTF_8);
        Files.writeString(Path.of("catalogs").resolve("phase5-diversity.csv"), csv(results),
                StandardCharsets.UTF_8);

        results.forEach(result -> System.out.println(result.summary()));
        presets(profiles).forEach(line -> System.out.println("phase5 preset " + line));
        System.out.printf(Locale.ROOT,
                "phase5: profiles=%d samples=%d replaced_profiles=%d%n",
                results.size(), samples,
                results.stream().filter(result -> result.profile.compatibility().mayReplace()).count());
    }

    /** Mean leaves/quads per preset for representative profiles, 16 seeds each. */
    private static List<String> presets(Map<String, SpeciesProfile> profiles) {
        List<String> lines = new ArrayList<>();
        for (String id : PRESET_PROFILES) {
            SpeciesProfile profile = profiles.get(id);
            if (profile == null) {
                continue;
            }
            StringBuilder sb = new StringBuilder(id);
            double[][] stats = presetStats(profile);
            for (int i = 0; i < PerformancePreset.values().length; i++) {
                sb.append(String.format(Locale.ROOT, " %s[leaves=%.1f,quads=%.1f]",
                        PerformancePreset.values()[i], stats[i][0], stats[i][1]));
            }
            lines.add(sb.toString());
        }
        return lines;
    }

    private static double[][] presetStats(SpeciesProfile profile) {
        PerformancePreset[] presets = PerformancePreset.values();
        double[][] stats = new double[presets.length][2];
        for (int p = 0; p < presets.length; p++) {
            double leaves = 0.0D;
            double quads = 0.0D;
            for (int i = 0; i < 16; i++) {
                RasterizedTree tree = rasterize(profile, presets[p], i);
                leaves += tree.leafBlocks();
                quads += tree.estimatedQuads();
            }
            stats[p][0] = leaves / 16.0D;
            stats[p][1] = quads / 16.0D;
        }
        return stats;
    }

    private static RasterizedTree rasterize(SpeciesProfile profile, PerformancePreset preset, int index) {
        SemiproceduralMorphologyGenerator generator = new SemiproceduralMorphologyGenerator();
        ReferenceBlockRasterizer rasterizer = new ReferenceBlockRasterizer();
        BlockPalette palette = new BlockPalette(
                profile.log(), BranchMaterials.resolve(profile.log()), profile.leaves(),
                profile.sapling().orElse(null), null);
        int x = ((index * 37) % 2048) - 1024;
        int z = ((index * 53) % 2048) - 1024;
        TreeSkeleton skeleton = generator.generate(new MorphologyRequest(
                BASE_SEED, OVERWORLD, x, 64, z, 0L, profile));
        return rasterizer.rasterize(skeleton, profile, palette, x, 64, z, preset);
    }

    private static Result measure(SpeciesProfile profile, int samples) {
        SemiproceduralMorphologyGenerator generator = new SemiproceduralMorphologyGenerator();
        ReferenceBlockRasterizer rasterizer = new ReferenceBlockRasterizer();
        BlockPalette palette = new BlockPalette(
                profile.log(), BranchMaterials.resolve(profile.log()), profile.leaves(),
                profile.sapling().orElse(null), null);

        // Warm up so the first sample does not dominate the percentile tail.
        for (int i = 0; i < 8; i++) {
            rasterize(generator, rasterizer, palette, profile, i);
        }

        Set<String> exact = new HashSet<>();
        Set<String> rotational = new HashSet<>();
        Set<String> scaled = new HashSet<>();
        long[] nanos = new long[samples];
        int heightSum = 0;
        int widthSum = 0;
        int logsSum = 0;
        int leavesSum = 0;
        int blocksSum = 0;
        int quadsSum = 0;
        int segmentsSum = 0;

        for (int i = 0; i < samples; i++) {
            int x = ((i * 37) % 2048) - 1024;
            int z = ((i * 53) % 2048) - 1024;
            MorphologyRequest request = new MorphologyRequest(
                    BASE_SEED, OVERWORLD, x, 64, z, 0L, profile);
            long start = System.nanoTime();
            TreeSkeleton skeleton = generator.generate(request);
            RasterizedTree tree = rasterizer.rasterize(skeleton, profile, palette,
                    x, 64, z);
            nanos[i] = System.nanoTime() - start;

            exact.add(CanonicalShape.exact(tree.blocks()));
            rotational.add(CanonicalShape.rotational(tree.blocks()));
            scaled.add(CanonicalShape.scaled(tree.blocks()));

            heightSum += extent(tree.blocks(), Axis.Y);
            widthSum += Math.max(extent(tree.blocks(), Axis.X), extent(tree.blocks(), Axis.Z));
            logsSum += tree.logBlocks();
            leavesSum += tree.leafBlocks();
            blocksSum += tree.totalBlocks();
            quadsSum += tree.estimatedQuads();
            segmentsSum += skeleton.segments().size();
        }

        long[] sorted = Arrays.copyOf(nanos, nanos.length);
        Arrays.sort(sorted);
        return new Result(profile, samples,
                exact.size(), rotational.size(), scaled.size(),
                heightSum / (double) samples, widthSum / (double) samples,
                logsSum / (double) samples, leavesSum / (double) samples,
                blocksSum / (double) samples, quadsSum / (double) samples,
                segmentsSum / (double) samples,
                percentile(sorted, 0.50D), percentile(sorted, 0.95D));
    }

    private static RasterizedTree rasterize(SemiproceduralMorphologyGenerator generator,
                                            ReferenceBlockRasterizer rasterizer,
                                            BlockPalette palette, SpeciesProfile profile, int index) {
        int x = ((index * 37) % 2048) - 1024;
        int z = ((index * 53) % 2048) - 1024;
        TreeSkeleton skeleton = generator.generate(new MorphologyRequest(
                BASE_SEED, OVERWORLD, x, 64, z, 0L, profile));
        return rasterizer.rasterize(skeleton, profile, palette, x, 64, z);
    }

    private enum Axis { X, Y, Z }

    private static int extent(List<BlockPlacement> blocks, Axis axis) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        for (BlockPlacement block : blocks) {
            int value = switch (axis) {
                case X -> block.x();
                case Y -> block.y();
                case Z -> block.z();
            };
            min = Math.min(min, value);
            max = Math.max(max, value);
        }
        return blocks.isEmpty() ? 0 : max - min + 1;
    }

    private static double percentile(long[] sorted, double quantile) {
        if (sorted.length == 0) {
            return 0.0D;
        }
        int index = (int) Math.ceil(quantile * sorted.length) - 1;
        return sorted[Math.max(0, Math.min(sorted.length - 1, index))] / 1000.0D;
    }

    private static double dupRate(int distinct, int samples) {
        return samples == 0 ? 0.0D : 1.0D - (double) distinct / (double) samples;
    }

    private static String csv(List<Result> results) {
        StringBuilder sb = new StringBuilder();
        sb.append("species,compatibility,samples,exact_distinct,exact_dup,rot_distinct,rot_dup,")
                .append("scaled_distinct,scaled_dup,mean_height,mean_width,mean_logs,mean_leaves,")
                .append("mean_blocks,mean_quads,quads_per_block,mean_segments,gen_p50_us,gen_p95_us\n");
        for (Result result : results) {
            sb.append(result.profile.id()).append(',')
                    .append(result.profile.compatibility()).append(',')
                    .append(result.samples).append(',')
                    .append(result.exactDistinct).append(',')
                    .append(fixed(dupRate(result.exactDistinct, result.samples))).append(',')
                    .append(result.rotationalDistinct).append(',')
                    .append(fixed(dupRate(result.rotationalDistinct, result.samples))).append(',')
                    .append(result.scaledDistinct).append(',')
                    .append(fixed(dupRate(result.scaledDistinct, result.samples))).append(',')
                    .append(fixed(result.meanHeight)).append(',')
                    .append(fixed(result.meanWidth)).append(',')
                    .append(fixed(result.meanLogs)).append(',')
                    .append(fixed(result.meanLeaves)).append(',')
                    .append(fixed(result.meanBlocks)).append(',')
                    .append(fixed(result.meanQuads)).append(',')
                    .append(fixed(result.quadsPerBlock())).append(',')
                    .append(fixed(result.meanSegments)).append(',')
                    .append(fixed(result.p50Micros)).append(',')
                    .append(fixed(result.p95Micros)).append('\n');
        }
        return sb.toString();
    }

    private static String fixed(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static String json(List<Result> results, Map<String, SpeciesProfile> profiles, int samples) {
        StringBuilder sb = new StringBuilder(8192);
        sb.append("{\n  \"phase\": 5,\n")
                .append("  \"method\": \"morphology+reference-rasterizer, synthetic seeds and positions\",\n")
                .append("  \"samples_per_profile\": ").append(samples).append(",\n")
                .append("  \"scale_bins\": ").append(CanonicalShape.SCALE_BINS).append(",\n")
                .append("  \"profiles\": {\n");
        for (int i = 0; i < results.size(); i++) {
            Result result = results.get(i);
            sb.append("    \"").append(result.profile.id()).append("\": {")
                    .append("\"compatibility\": \"").append(result.profile.compatibility()).append("\", ")
                    .append("\"exact_dup\": ").append(fixed(dupRate(result.exactDistinct, result.samples))).append(", ")
                    .append("\"rotational_dup\": ").append(fixed(dupRate(result.rotationalDistinct, result.samples))).append(", ")
                    .append("\"scaled_dup\": ").append(fixed(dupRate(result.scaledDistinct, result.samples))).append(", ")
                    .append("\"mean_height\": ").append(fixed(result.meanHeight)).append(", ")
                    .append("\"mean_width\": ").append(fixed(result.meanWidth)).append(", ")
                    .append("\"mean_logs\": ").append(fixed(result.meanLogs)).append(", ")
                    .append("\"mean_leaves\": ").append(fixed(result.meanLeaves)).append(", ")
                    .append("\"mean_quads\": ").append(fixed(result.meanQuads)).append(", ")
                    .append("\"quads_per_block\": ").append(fixed(result.quadsPerBlock())).append(", ")
                    .append("\"mean_segments\": ").append(fixed(result.meanSegments)).append(", ")
                    .append("\"gen_p50_us\": ").append(fixed(result.p50Micros)).append(", ")
                    .append("\"gen_p95_us\": ").append(fixed(result.p95Micros))
                    .append('}');
            sb.append(i + 1 < results.size() ? ",\n" : "\n");
        }
        sb.append("  },\n  \"presets\": {\n");
        for (int p = 0; p < PRESET_PROFILES.size(); p++) {
            String id = PRESET_PROFILES.get(p);
            SpeciesProfile profile = profiles.get(id);
            if (profile == null) {
                continue;
            }
            double[][] stats = presetStats(profile);
            sb.append("    \"").append(id).append("\": {");
            for (int i = 0; i < PerformancePreset.values().length; i++) {
                PerformancePreset preset = PerformancePreset.values()[i];
                sb.append("\"").append(preset.name().toLowerCase(Locale.ROOT)).append("\": {")
                        .append("\"leaves\": ").append(fixed(stats[i][0])).append(", ")
                        .append("\"quads\": ").append(fixed(stats[i][1])).append('}')
                        .append(i + 1 < PerformancePreset.values().length ? ", " : "");
            }
            sb.append('}').append(p + 1 < PRESET_PROFILES.size() ? ",\n" : "\n");
        }
        sb.append("  },\n  \"totals\": {\n");
        sb.append("    \"profiles\": ").append(results.size()).append(",\n");
        sb.append("    \"replaceable_profiles\": ")
                .append(results.stream().filter(result -> result.profile.compatibility().mayReplace()).count())
                .append(",\n");
        sb.append("    \"mean_scaled_dup_replaceable\": ")
                .append(fixed(mean(results, true))).append(",\n");
        sb.append("    \"max_scaled_dup_replaceable\": ")
                .append(fixed(max(results, true))).append("\n");
        sb.append("  }\n}\n");
        return sb.toString();
    }

    private static double mean(List<Result> results, boolean replaceableOnly) {
        return results.stream()
                .filter(result -> !replaceableOnly || result.profile.compatibility().mayReplace())
                .mapToDouble(result -> dupRate(result.scaledDistinct, result.samples))
                .average().orElse(0.0D);
    }

    private static double max(List<Result> results, boolean replaceableOnly) {
        return results.stream()
                .filter(result -> !replaceableOnly || result.profile.compatibility().mayReplace())
                .mapToDouble(result -> dupRate(result.scaledDistinct, result.samples))
                .max().orElse(0.0D);
    }

    private record Result(SpeciesProfile profile, int samples,
                          int exactDistinct, int rotationalDistinct, int scaledDistinct,
                          double meanHeight, double meanWidth, double meanLogs, double meanLeaves,
                          double meanBlocks, double meanQuads, double meanSegments,
                          double p50Micros, double p95Micros) {

        private double quadsPerBlock() {
            return meanBlocks <= 0.0D ? 0.0D : meanQuads / meanBlocks;
        }

        private String summary() {
            return String.format(Locale.ROOT,
                    "phase5 %-42s %-14s exact_dup=%.3f rot_dup=%.3f scaled_dup=%.3f "
                            + "h=%.1f w=%.1f logs=%.1f leaves=%.1f quads=%.1f q/b=%.1f "
                            + "seg=%.1f p50=%.0fus p95=%.0fus",
                    profile.id(), profile.compatibility(),
                    dupRate(exactDistinct, samples), dupRate(rotationalDistinct, samples),
                    dupRate(scaledDistinct, samples),
                    meanHeight, meanWidth, meanLogs, meanLeaves, meanQuads, quadsPerBlock(),
                    meanSegments, p50Micros, p95Micros);
        }
    }
}
