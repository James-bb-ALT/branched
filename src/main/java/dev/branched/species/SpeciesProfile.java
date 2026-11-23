package dev.branched.species;

import dev.branched.core.ResourceId;
import dev.branched.morphology.DoubleRange;
import dev.branched.morphology.IntRange;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Immutable, validated description of one tree species. This is the contract
 * between the JSON profile loader and the morphology generator; it contains no
 * Minecraft classes so it can be exercised in plain unit tests.
 */
public final class SpeciesProfile {
    private final ResourceId id;
    private final int schema;
    private final TreeArchetype archetype;
    private final ResourceId log;
    private final ResourceId wood;
    private final ResourceId leaves;
    private final ResourceId sapling;
    private final DoubleRange trunkHeight;
    private final DoubleRange trunkRadius;
    private final DoubleRange trunkBend;
    private final IntRange primaryBranches;
    private final IntRange branchDepth;
    private final DoubleRange branchAngleDegrees;
    private final DoubleRange branchLengthRatio;
    private final DoubleRange asymmetry;
    private final BranchPattern branchPattern;
    private final DoubleRange crownWidthRatio;
    private final DoubleRange crownDensity;
    private final IntRange crownClusters;
    private final int maxSegments;
    private final int maxLeaves;
    private final int maxQuads;
    private final CompatibilityLevel compatibility;
    private final List<ResourceId> biomes;
    private final String sourceFeature;
    private final boolean fruitBearing;
    private final int generatorVersion;

    private SpeciesProfile(Builder b) {
        this.id = b.id;
        this.schema = b.schema;
        this.archetype = b.archetype;
        this.log = b.log;
        this.wood = b.wood;
        this.leaves = b.leaves;
        this.sapling = b.sapling;
        this.trunkHeight = b.trunkHeight;
        this.trunkRadius = b.trunkRadius;
        this.trunkBend = b.trunkBend;
        this.primaryBranches = b.primaryBranches;
        this.branchDepth = b.branchDepth;
        this.branchAngleDegrees = b.branchAngleDegrees;
        this.branchLengthRatio = b.branchLengthRatio;
        this.asymmetry = b.asymmetry;
        this.branchPattern = b.branchPattern;
        this.crownWidthRatio = b.crownWidthRatio;
        this.crownDensity = b.crownDensity;
        this.crownClusters = b.crownClusters;
        this.maxSegments = b.maxSegments;
        this.maxLeaves = b.maxLeaves;
        this.maxQuads = b.maxQuads;
        this.compatibility = b.compatibility;
        this.biomes = List.copyOf(b.biomes);
        this.sourceFeature = b.sourceFeature == null ? "" : b.sourceFeature;
        this.fruitBearing = b.fruitBearing;
        this.generatorVersion = b.generatorVersion;
    }

    public static Builder builder(ResourceId id) {
        return new Builder(id);
    }

    public ResourceId id() { return id; }
    public int schema() { return schema; }
    public TreeArchetype archetype() { return archetype; }
    public ResourceId log() { return log; }
    public Optional<ResourceId> wood() { return Optional.ofNullable(wood); }
    public ResourceId leaves() { return leaves; }
    public Optional<ResourceId> sapling() { return Optional.ofNullable(sapling); }
    public DoubleRange trunkHeight() { return trunkHeight; }
    public DoubleRange trunkRadius() { return trunkRadius; }
    public DoubleRange trunkBend() { return trunkBend; }
    public IntRange primaryBranches() { return primaryBranches; }
    public IntRange branchDepth() { return branchDepth; }
    public DoubleRange branchAngleDegrees() { return branchAngleDegrees; }
    public DoubleRange branchLengthRatio() { return branchLengthRatio; }
    public DoubleRange asymmetry() { return asymmetry; }
    public BranchPattern branchPattern() { return branchPattern; }
    public DoubleRange crownWidthRatio() { return crownWidthRatio; }
    public DoubleRange crownDensity() { return crownDensity; }
    public IntRange crownClusters() { return crownClusters; }
    public int maxSegments() { return maxSegments; }
    public int maxLeaves() { return maxLeaves; }
    public int maxQuads() { return maxQuads; }
    public CompatibilityLevel compatibility() { return compatibility; }
    public List<ResourceId> biomes() { return biomes; }
    public String sourceFeature() { return sourceFeature; }
    public boolean fruitBearing() { return fruitBearing; }
    public int generatorVersion() { return generatorVersion; }

    public static final class Builder {
        private final ResourceId id;
        private int schema = 1;
        private TreeArchetype archetype = TreeArchetype.BROADLEAF_IRREGULAR;
        private ResourceId log;
        private ResourceId wood;
        private ResourceId leaves;
        private ResourceId sapling;
        private DoubleRange trunkHeight = new DoubleRange(5, 9);
        private DoubleRange trunkRadius = new DoubleRange(0.6, 0.9);
        private DoubleRange trunkBend = new DoubleRange(0.0, 0.12);
        private IntRange primaryBranches = new IntRange(4, 7);
        private IntRange branchDepth = new IntRange(1, 2);
        private DoubleRange branchAngleDegrees = new DoubleRange(25, 55);
        private DoubleRange branchLengthRatio = new DoubleRange(0.25, 0.45);
        private DoubleRange asymmetry = new DoubleRange(0.05, 0.3);
        private BranchPattern branchPattern = BranchPattern.ALTERNATE;
        private DoubleRange crownWidthRatio = new DoubleRange(0.8, 1.4);
        private DoubleRange crownDensity = new DoubleRange(0.4, 0.75);
        private IntRange crownClusters = new IntRange(6, 12);
        private int maxSegments = 96;
        private int maxLeaves = 320;
        private int maxQuads = 6000;
        private CompatibilityLevel compatibility = CompatibilityLevel.UNRESOLVED;
        private List<ResourceId> biomes = List.of();
        private String sourceFeature = "";
        private boolean fruitBearing;
        private int generatorVersion = 1;

        private Builder(ResourceId id) {
            this.id = Objects.requireNonNull(id, "id");
        }

        public Builder schema(int v) { this.schema = v; return this; }
        public Builder archetype(TreeArchetype v) { this.archetype = v; return this; }
        public Builder log(ResourceId v) { this.log = v; return this; }
        public Builder wood(ResourceId v) { this.wood = v; return this; }
        public Builder leaves(ResourceId v) { this.leaves = v; return this; }
        public Builder sapling(ResourceId v) { this.sapling = v; return this; }
        public Builder trunkHeight(DoubleRange v) { this.trunkHeight = v; return this; }
        public Builder trunkRadius(DoubleRange v) { this.trunkRadius = v; return this; }
        public Builder trunkBend(DoubleRange v) { this.trunkBend = v; return this; }
        public Builder primaryBranches(IntRange v) { this.primaryBranches = v; return this; }
        public Builder branchDepth(IntRange v) { this.branchDepth = v; return this; }
        public Builder branchAngleDegrees(DoubleRange v) { this.branchAngleDegrees = v; return this; }
        public Builder branchLengthRatio(DoubleRange v) { this.branchLengthRatio = v; return this; }
        public Builder asymmetry(DoubleRange v) { this.asymmetry = v; return this; }
        public Builder branchPattern(BranchPattern v) { this.branchPattern = v; return this; }
        public Builder crownWidthRatio(DoubleRange v) { this.crownWidthRatio = v; return this; }
        public Builder crownDensity(DoubleRange v) { this.crownDensity = v; return this; }
        public Builder crownClusters(IntRange v) { this.crownClusters = v; return this; }
        public Builder maxSegments(int v) { this.maxSegments = v; return this; }
        public Builder maxLeaves(int v) { this.maxLeaves = v; return this; }
        public Builder maxQuads(int v) { this.maxQuads = v; return this; }
        public Builder compatibility(CompatibilityLevel v) { this.compatibility = v; return this; }
        public Builder biomes(List<ResourceId> v) { this.biomes = v == null ? List.of() : v; return this; }
        public Builder sourceFeature(String v) { this.sourceFeature = v; return this; }
        public Builder fruitBearing(boolean v) { this.fruitBearing = v; return this; }
        public Builder generatorVersion(int v) { this.generatorVersion = v; return this; }

        public SpeciesProfile build() {
            Objects.requireNonNull(archetype, "archetype");
            Objects.requireNonNull(log, "log");
            Objects.requireNonNull(leaves, "leaves");
            Objects.requireNonNull(trunkHeight, "trunkHeight");
            Objects.requireNonNull(trunkRadius, "trunkRadius");
            Objects.requireNonNull(trunkBend, "trunkBend");
            Objects.requireNonNull(primaryBranches, "primaryBranches");
            Objects.requireNonNull(branchDepth, "branchDepth");
            Objects.requireNonNull(branchAngleDegrees, "branchAngleDegrees");
            Objects.requireNonNull(branchLengthRatio, "branchLengthRatio");
            Objects.requireNonNull(asymmetry, "asymmetry");
            Objects.requireNonNull(branchPattern, "branchPattern");
            Objects.requireNonNull(crownWidthRatio, "crownWidthRatio");
            Objects.requireNonNull(crownDensity, "crownDensity");
            Objects.requireNonNull(crownClusters, "crownClusters");
            Objects.requireNonNull(compatibility, "compatibility");
            if (schema < 1) {
                throw new IllegalArgumentException("schema must be >= 1");
            }
            if (maxSegments < 8 || maxLeaves < 8 || maxQuads < 64) {
                throw new IllegalArgumentException("budgets are too small for a tree");
            }
            if (generatorVersion < 1) {
                throw new IllegalArgumentException("generatorVersion must be >= 1");
            }
            return new SpeciesProfile(this);
        }
    }

    @Override
    public String toString() {
        return "SpeciesProfile[" + id + ", " + archetype + ", " + compatibility + "]";
    }
}
