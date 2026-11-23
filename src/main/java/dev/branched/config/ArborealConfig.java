package dev.branched.config;

import net.minecraftforge.common.ForgeConfigSpec;

/** Common config. Defaults follow the v2 plan: static generation, no simulation. */
public final class ArborealConfig {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    public static final ForgeConfigSpec.BooleanValue WORLDGEN_ENABLED = BUILDER
            .comment("Enable Branched worldgen replacement.")
            .define("worldgen.enabled", true);

    public static final ForgeConfigSpec.BooleanValue REPLACE_KNOWN_FEATURES = BUILDER
            .comment("Replace only features present in the audited integration catalog.")
            .define("worldgen.replace_known_features", true);

    public static final ForgeConfigSpec.ConfigValue<String> UNKNOWN_FEATURE_POLICY = BUILDER
            .comment("preserve_native | static_if_profile | disable")
            .define("worldgen.unknown_feature_policy", "preserve_native");

    public static final ForgeConfigSpec.BooleanValue REPLACE_EXISTING_CHUNKS = BUILDER
            .comment("Never rewrite already generated chunks during Phase 0.")
            .define("worldgen.replace_existing_chunks", false);

    public static final ForgeConfigSpec.BooleanValue PROGRESSIVE_GROWTH = BUILDER
            .comment("Forbidden in the static architecture; kept for explicit diagnostics.")
            .define("simulation.progressive_growth", false);

    public static final ForgeConfigSpec.BooleanValue ROOT_SIMULATION = BUILDER
            .define("simulation.root_simulation", false);

    public static final ForgeConfigSpec.BooleanValue CUSTOM_LEAF_HYDRATION = BUILDER
            .define("simulation.custom_leaf_hydration", false);

    public static final ForgeConfigSpec.BooleanValue WHOLE_TREE_FELLING = BUILDER
            .define("simulation.whole_tree_felling", false);

    public static final ForgeConfigSpec.ConfigValue<String> RENDER_PRESET = BUILDER
            .comment("performance | balanced | visual")
            .define("rendering.preset", "balanced");

    public static final ForgeConfigSpec.BooleanValue USE_BLOCK_ENTITIES = BUILDER
            .comment("Must remain false: no BlockEntity is created per tree.")
            .define("rendering.use_block_entities", false);

    public static final ForgeConfigSpec.BooleanValue CACHE_BAKED_GEOMETRY = BUILDER
            .define("rendering.cache_baked_geometry", true);

    public static final ForgeConfigSpec.ConfigValue<String> MORPHOLOGY_MODE = BUILDER
            .define("morphology.mode", "semiprocedural_once");

    public static final ForgeConfigSpec.BooleanValue DETERMINISTIC_BY_WORLD_SEED = BUILDER
            .define("morphology.deterministic_by_world_seed", true);

    public static final ForgeConfigSpec.BooleanValue RANDOMIZE_BRANCH_TOPOLOGY = BUILDER
            .define("morphology.randomize_branch_topology", true);

    public static final ForgeConfigSpec.BooleanValue RANDOMIZE_CROWN_TOPOLOGY = BUILDER
            .define("morphology.randomize_crown_topology", true);

    public static final ForgeConfigSpec.BooleanValue USE_FULL_TREE_TEMPLATES = BUILDER
            .comment("Full NBT templates must not be the primary generation mechanism.")
            .define("morphology.use_full_tree_templates_as_primary", false);

    public static final ForgeConfigSpec.IntValue MAX_GENERATION_ATTEMPTS = BUILDER
            .defineInRange("morphology.max_generation_attempts", 3, 1, 8);

    public static final ForgeConfigSpec.ConfigValue<String> SPECIAL_TREES = BUILDER
            .define("compatibility.special_trees", "preserve_native");

    public static final ForgeConfigSpec.BooleanValue LOG_UNRESOLVED_FEATURES = BUILDER
            .define("compatibility.log_unresolved_features", true);

    public static final ForgeConfigSpec.BooleanValue WARN_IF_DYNAMICTREES_LOADED = BUILDER
            .define("compatibility.warn_if_dynamictrees_loaded", true);

    public static final ForgeConfigSpec.BooleanValue METRICS_ENABLED = BUILDER
            .define("diagnostics.metrics_enabled", true);

    public static final ForgeConfigSpec.BooleanValue LOG_PROFILE_SUMMARY = BUILDER
            .define("diagnostics.log_profile_summary", true);

    public static final ForgeConfigSpec SPEC = BUILDER.build();

    private ArborealConfig() {
    }
}
