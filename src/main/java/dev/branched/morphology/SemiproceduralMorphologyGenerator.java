package dev.branched.morphology;

import dev.branched.metrics.MetricsRecorder;
import dev.branched.species.SpeciesProfile;
import dev.branched.species.TreeArchetype;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Dynamic-Trees-inspired static morphology generator.
 *
 * <p>Uses species growth proportions (clear lower bole heights, horizontal-then-upward
 * primary limbs, secondary forks, tapered conifer whorls, long diagonal jungle
 * parkour boughs, savanna flat-top umbrellas and branch-tip-driven foliage clusters)
 * while producing a pure immutable {@link TreeSkeleton} once per placement with zero
 * runtime ticks, zero BlockEntities and zero Dynamic Trees dependencies.</p>
 */
public final class SemiproceduralMorphologyGenerator implements MorphologyGenerator {
    public static final int VERSION = 3;
    private static final double GOLDEN_ANGLE = Math.PI * (3.0D - Math.sqrt(5.0D));
    private static final double TWO_PI = Math.PI * 2.0D;

    private final MetricsRecorder metrics;

    public SemiproceduralMorphologyGenerator() {
        this(MetricsRecorder.NOOP);
    }

    public SemiproceduralMorphologyGenerator(MetricsRecorder metrics) {
        this.metrics = metrics == null ? MetricsRecorder.NOOP : metrics;
    }

    @Override
    public int version() {
        return VERSION;
    }

    @Override
    public TreeSkeleton generate(MorphologyRequest request) {
        long started = System.nanoTime();
        SpeciesProfile profile = request.profile();
        DeterministicRandom random = new DeterministicRandom(request.seed());

        double heightValue = Math.max(4.0D, profile.trunkHeight().sample(random));
        int height = (int) Math.round(heightValue);
        double baseRadius = Math.max(0.30D, profile.trunkRadius().sample(random));
        double bend = profile.trunkBend().sample(random);
        double asymmetry = profile.asymmetry().sample(random);
        double crownWidth = profile.crownWidthRatio().sample(random);
        double crownDensity = profile.crownDensity().sample(random);
        Style style = classify(profile);

        double[] lean = random.nextHorizontalUnitVector();
        double[] side = {-lean[1], lean[0]};

        BranchNode root = new BranchNode(new Point3(0.5D, 0.0D, 0.5D), baseRadius, 0);
        List<Segment> segments = new ArrayList<>();
        List<LeafCluster> clusters = new ArrayList<>();
        List<Point3> trunk = new ArrayList<>();
        List<BranchNode> trunkNodes = new ArrayList<>();
        trunk.add(new Point3(0.0D, 0.0D, 0.0D));
        trunkNodes.add(root);

        double effectiveBend = switch (style) {
            case PINE, CONIFER, BIRCH -> bend * 0.35D;
            case ACACIA -> Math.max(0.14D, bend * 1.15D);
            case PALM -> Math.max(0.12D, bend);
            default -> bend * 0.70D;
        };

        int maxSegments = profile.maxSegments();
        int maxLeaves = profile.maxLeaves();
        int maxDepth = Math.max(1, profile.branchDepth().sample(random));

        boolean isTwoByTwoBole = baseRadius >= 1.15D && style != Style.PALM;
        boolean bifurcateUpperTrunk = !isTwoByTwoBole
                && (style == Style.ACACIA || style == Style.BROADLEAF
                        || style == Style.DARK_CANOPY || style == Style.WILLOW_MANGROVE)
                && height >= 6;
        int trunkLogTopY;
        if (isTwoByTwoBole) {
            trunkLogTopY = (style == Style.CONIFER || style == Style.PINE || style == Style.JUNGLE)
                    ? Math.max(4, height - 1)
                    : Math.max(4, (int) Math.round(height * 0.72D));
        } else if (bifurcateUpperTrunk) {
            trunkLogTopY = Math.min(height - 1, Math.max(4, (int) Math.round(height * (style == Style.ACACIA ? 0.56D : 0.65D))));
        } else {
            trunkLogTopY = height;
        }

        Point3 previous = trunk.get(0);
        double previousRadius = baseRadius;
        double bendPhase = random.nextDouble() * TWO_PI;
        for (int y = 1; y <= height; y++) {
            double t = (double) y / (double) height;
            double radius = Math.max(0.25D, baseRadius * (1.0D - 0.58D * t));
            if (isTwoByTwoBole) {
                if (y <= trunkLogTopY) {
                    Point3 point = new Point3(0.0D, y, 0.0D);
                    double segStartR = Math.max(1.16D, previousRadius);
                    double segEndR = Math.max(1.16D, radius);
                    BranchNode node = new BranchNode(point, segEndR, 0);
                    trunkNodes.get(trunkNodes.size() - 1).addChild(node);
                    trunkNodes.add(node);
                    segments.add(new Segment(previous, point, segStartR, segEndR, 0));
                    trunk.add(point);
                    previous = point;
                    previousRadius = segEndR;
                } else {
                    trunk.add(trunk.get(trunk.size() - 1));
                    trunkNodes.add(trunkNodes.get(trunkNodes.size() - 1));
                }
                continue;
            }
            Point3 point;
            if (y <= trunkLogTopY && style != Style.PALM) {
                point = new Point3(0.0D, y, 0.0D);
            } else if (bifurcateUpperTrunk && y == trunkLogTopY + 1) {
                // Fork collar directly above the full-log bole so both thick stems sit cleanly on top
                point = new Point3(0.0D, y, 0.0D);
            } else {
                double curve = effectiveBend * height * Math.pow(t, 1.55D);
                double ripple = Math.sin(t * Math.PI * 1.25D + bendPhase) * effectiveBend * height * 0.16D;
                double asym = asymmetry * Math.pow(t, 1.85D) * (style == Style.ACACIA ? 1.5D : 0.75D);
                double x = lean[0] * curve + side[0] * (ripple + asym);
                double z = lean[1] * curve + side[1] * (ripple + asym);
                point = new Point3(x, y, z);
            }
            int segDepth = (style != Style.PALM && y > trunkLogTopY
                    && (bifurcateUpperTrunk || Math.round(point.x()) != 0 || Math.round(point.z()) != 0)) ? 1 : 0;
            double segStartR = (segDepth == 1) ? Math.max(0.42D, previousRadius) : previousRadius;
            double segEndR = (segDepth == 1) ? Math.max(0.30D, radius) : radius;
            BranchNode node = new BranchNode(point, segEndR, segDepth);
            trunkNodes.get(trunkNodes.size() - 1).addChild(node);
            trunkNodes.add(node);
            segments.add(new Segment(previous, point, segStartR, segEndR, segDepth));
            trunk.add(point);
            previous = point;
            previousRadius = radius;
        }

        if (isTwoByTwoBole) {
            double capRad = Math.max(2.15D, 1.65D + crownWidth * 0.35D);
            double capHeight = Math.max(1.45D, (height - trunkLogTopY) + 0.90D);
            addCluster(clusters, new Point3(0.5D, trunkLogTopY + 0.85D, 0.5D), capRad, capHeight, maxLeaves);
        }

        // Emit second thick bifurcating primary stem on crooked/forked canopies
        if (bifurcateUpperTrunk && trunkLogTopY + 1 < trunk.size() && segments.size() < maxSegments) {
            Point3 forkCollar = trunk.get(trunkLogTopY + 1);
            BranchNode collarNode = trunkNodes.get(trunkLogTopY + 1);
            double forkBaseAngle = Math.atan2(-lean[1], -lean[0]) + (random.nextDouble() - 0.5D) * 0.60D;
            double forkReach = Math.max(1.8D, Math.min(3.4D, height * (style == Style.ACACIA ? 0.34D : 0.25D)));
            double forkRise = Math.max(1.2D, Math.min(3.0D, (height - (trunkLogTopY + 1)) + random.nextDouble() * 0.8D));
            Point3 forkMid = forkCollar.add(
                    Math.cos(forkBaseAngle) * (forkReach * 0.52D),
                    forkRise * 0.48D,
                    Math.sin(forkBaseAngle) * (forkReach * 0.52D));
            Point3 forkTip = forkCollar.add(
                    Math.cos(forkBaseAngle) * forkReach,
                    forkRise,
                    Math.sin(forkBaseAngle) * forkReach);
            BranchNode midNode = new BranchNode(forkMid, 0.38D, 1);
            collarNode.addChild(midNode);
            segments.add(new Segment(forkCollar, forkMid, 0.46D, 0.38D, 1));
            midNode.addChild(new BranchNode(forkTip, 0.32D, 1));
            segments.add(new Segment(forkMid, forkTip, 0.38D, 0.32D, 1));
            double tipCrownRad = style == Style.ACACIA ? 2.15D : 1.75D;
            double tipCrownHeight = style == Style.ACACIA ? 1.00D : 1.25D;
            addCluster(clusters, forkTip.add(0.0D, 0.55D, 0.0D), tipCrownRad, tipCrownHeight, maxLeaves);
        }

        switch (style) {
            case PINE -> generatePine(profile, random, height, crownWidth, crownDensity,
                    trunk, trunkNodes, segments, clusters, maxSegments, maxLeaves);
            case CONIFER -> generateConifer(profile, random, height, crownWidth, crownDensity,
                    trunk, trunkNodes, segments, clusters, maxSegments, maxLeaves, maxDepth);
            case JUNGLE -> generateJungle(profile, random, height, crownWidth, crownDensity,
                    trunk, trunkNodes, segments, clusters, maxSegments, maxLeaves, maxDepth);
            case DARK_CANOPY -> generateDarkCanopy(profile, random, height, crownWidth, crownDensity,
                    trunk, trunkNodes, segments, clusters, maxSegments, maxLeaves, maxDepth);
            case ACACIA -> generateAcacia(profile, random, height, crownWidth, crownDensity,
                    trunk, trunkNodes, segments, clusters, maxSegments, maxLeaves, maxDepth);
            case BIRCH -> generateBirch(profile, random, height, crownWidth, crownDensity,
                    trunk, trunkNodes, segments, clusters, maxSegments, maxLeaves);
            case PALM -> generatePalm(profile, random, height, crownWidth,
                    trunk, trunkNodes, segments, clusters, maxSegments, maxLeaves);
            case WILLOW_MANGROVE -> generateWillowMangrove(profile, random, height, crownWidth, crownDensity,
                    trunk, trunkNodes, segments, clusters, maxSegments, maxLeaves, maxDepth);
            case BROADLEAF -> generateBroadleaf(profile, random, height, crownWidth, crownDensity,
                    trunk, trunkNodes, segments, clusters, maxSegments, maxLeaves, maxDepth);
        }

        TreeSkeleton skeleton = new TreeSkeleton(root, segments, clusters);
        metrics.recordGenerated(profile, skeleton, System.nanoTime() - started);
        return skeleton;
    }

    /**
     * Dynamic Trees {@code PineLogic}: tall bare lower trunk (58..68%),
     * short compact 1..2 block lateral arms in the upper third wrapped in narrow
     * pine needle tiers plus a tapered spire cap.
     */
    private static void generatePine(SpeciesProfile profile, DeterministicRandom random, int height,
                                     double crownWidth, double crownDensity, List<Point3> trunk,
                                     List<BranchNode> trunkNodes, List<Segment> segments,
                                     List<LeafCluster> clusters, int maxSegments, int maxLeaves) {
        boolean isDead = profile.id().path().equals("dead_pine") || profile.id().path().equals("dead");
        int crownBaseY = Math.max(4, (int) Math.round(height * (0.56D + random.nextDouble() * 0.10D)));
        if (height >= 6) {
            int lowerMax = Math.max(2, Math.min(crownBaseY - 2, (int) Math.round(height * 0.46D)));
            addLowerLateralBoughs(random, height, height <= 8 ? 2 : 3, lowerMax, height >= 12 ? 2 : 1,
                    1.90D, 2.50D, 0.38D, 0.28D, 1.08D, 0.65D, !isDead,
                    trunk, trunkNodes, segments, clusters, maxSegments, maxLeaves);
        }
        double baseAzimuth = random.nextDouble() * TWO_PI;
        double maxRadius = Math.max(1.15D, Math.min(2.0D,
                1.25D + crownWidth * 0.45D + (height >= 12 ? 0.35D : 0.0D)));

        int whorlStep = height >= 11 ? 2 : 1;
        int whorlIdx = 0;
        for (int y = crownBaseY; y <= height - 1 && segments.size() < maxSegments; y += whorlStep) {
            double progress = (double) (y - crownBaseY) / Math.max(1, height - crownBaseY);
            Point3 attach = pointAt(trunk, y);
            BranchNode parent = trunkNodes.get(Math.min(trunkNodes.size() - 1, y));

            // Compact reach: 1.0..1.9 blocks near crown base, 1.0 near apex with subtle azimuth variation
            double reach = Math.max(1.0D, Math.min(1.95D,
                    (1.65D - 0.60D * progress) * (0.78D + 0.44D * random.nextDouble())));
            int armCount = progress > 0.72D ? 2 : (whorlIdx % 2 == 0 ? 4 : 3);
            double offsetAngle = baseAzimuth + whorlIdx * (Math.PI / 4.0D);

            for (int a = 0; a < armCount && segments.size() < maxSegments; a++) {
                double angle = offsetAngle + a * (TWO_PI / armCount) + (random.nextDouble() - 0.5D) * 0.45D;
                double dx = Math.cos(angle) * reach;
                double dz = Math.sin(angle) * reach;
                double dy = progress > 0.60D ? 0.8D : 0.25D;
                Point3 end = attach.add(dx, dy, dz);

                BranchNode tip = new BranchNode(end, 0.22D, 1);
                parent.addChild(tip);
                segments.add(new Segment(attach, end, 0.28D, 0.18D, 1));

                double tipRad = Math.max(1.15D, maxRadius * (0.85D - 0.32D * progress)
                        * (0.88D + 0.14D * crownDensity));
                addCluster(clusters, end.add(0.0D, 0.35D, 0.0D), tipRad, Math.min(1.35D, tipRad * 0.78D), maxLeaves);
            }

            // Trunk-centered pine foliage collar bridging arms so there is no Swiss cheese or exposed branch inside
            double trunkRad = Math.max(1.15D, maxRadius * (0.95D - 0.42D * progress));
            addCluster(clusters, attach.add(0.0D, 0.25D, 0.0D), trunkRad, 1.15D, maxLeaves);
            whorlIdx++;
        }

        Point3 apex = trunk.get(trunk.size() - 1);
        addCluster(clusters, apex.add(0.0D, 0.55D, 0.0D), 1.35D, 1.15D, maxLeaves);
        addCluster(clusters, apex.add(0.0D, 1.45D, 0.0D), 0.90D, 0.95D, maxLeaves);
    }

    /**
     * Dynamic Trees {@code ConiferLogic}: clear lower trunk (32..40%), radial tiered whorls
     * tapering linearly in horizontal reach towards a pointed conifer spire.
     */
    private static void generateConifer(SpeciesProfile profile, DeterministicRandom random, int height,
                                        double crownWidth, double crownDensity, List<Point3> trunk,
                                        List<BranchNode> trunkNodes, List<Segment> segments,
                                        List<LeafCluster> clusters, int maxSegments, int maxLeaves,
                                        int maxDepth) {
        boolean isCypress = profile.archetype() == TreeArchetype.CYPRESS
                || profile.id().path().contains("cypress");
        boolean isTallRedwood = height >= 16;

        int crownBaseY = height <= 5
                ? 4
                : Math.max(4, (int) Math.round(height
                * ((isCypress ? 0.34D : 0.39D) + random.nextDouble() * 0.08D)));
        if (height >= 5) {
            int lowerMax = Math.max(2, Math.min(crownBaseY - 2, (int) Math.round(height * 0.42D)));
            int lowerCount = isTallRedwood ? 3 : (height >= 8 ? 2 : 1);
            addLowerLateralBoughs(random, height, 2, lowerMax, lowerCount,
                    isCypress ? 1.55D : 2.05D, isCypress ? 2.15D : 2.75D, 0.30D,
                    0.36D, isCypress ? 1.05D : 1.20D, 0.66D, true,
                    trunk, trunkNodes, segments, clusters, maxSegments, maxLeaves);
        }
        double baseAzimuth = random.nextDouble() * TWO_PI;
        double maxReach = isCypress
                ? Math.max(1.2D, Math.min(2.1D, height * 0.18D))
                : Math.max(1.5D, Math.min(3.5D, height * (isTallRedwood ? 0.18D : 0.29D) * (0.8D + crownWidth * 0.35D)));
        double maxClusterRad = isCypress
                ? Math.max(1.2D, Math.min(1.9D, maxReach * 0.95D))
                : Math.max(1.35D, Math.min(2.6D, (isTallRedwood ? 2.4D : 1.75D) * (0.8D + 0.3D * crownDensity)));

        int step = isTallRedwood ? 3 : 2;
        int whorlIdx = 0;
        for (int y = crownBaseY; y <= height - 1 && segments.size() < maxSegments; y += step) {
            double progress = (double) (y - crownBaseY) / Math.max(1, height - 1 - crownBaseY);
            Point3 attach = pointAt(trunk, y);
            BranchNode parent = trunkNodes.get(Math.min(trunkNodes.size() - 1, y));

            // Keep spruce/conifer whorls bounded while varying arm lengths enough that consecutive seeds differ cleanly
            double reach = Math.max(1.0D, maxReach * (1.0D - 0.65D * progress));
            int armCount = progress > 0.78D ? 3 : 4;
            double whorlAngle = baseAzimuth + whorlIdx * (Math.PI / 4.0D) + (random.nextDouble() - 0.5D) * 0.45D;

            for (int a = 0; a < armCount && segments.size() < maxSegments; a++) {
                double angle = whorlAngle + a * (TWO_PI / armCount) + (random.nextDouble() - 0.5D) * 0.40D;
                double armReach = reach * (0.72D + 0.56D * random.nextDouble());
                double dx = Math.cos(angle) * armReach;
                double dz = Math.sin(angle) * armReach;
                double rise = progress > 0.60D ? (0.65D + random.nextDouble() * 0.65D) : (random.nextDouble() * 0.5D);
                Point3 elbow = attach.add(Math.cos(angle) * Math.min(1.2D, armReach * 0.6D), 0.0D,
                        Math.sin(angle) * Math.min(1.2D, armReach * 0.6D));
                Point3 end = attach.add(dx, rise, dz);

                BranchNode bNode = new BranchNode(elbow, 0.26D, 1);
                parent.addChild(bNode);
                if (elbow.distanceTo(end) >= 0.9D) {
                    segments.add(new Segment(attach, elbow, 0.32D, 0.24D, 1));
                    BranchNode tNode = new BranchNode(end, 0.18D, 1);
                    bNode.addChild(tNode);
                    segments.add(new Segment(elbow, end, 0.24D, 0.16D, 1));
                } else {
                    segments.add(new Segment(attach, end, 0.30D, 0.18D, 1));
                }

                // Tall redwoods/conifers add a subtle lateral twig fork on lower boughs
                if (maxDepth >= 2 && isTallRedwood && armReach >= 2.4D && segments.size() < maxSegments) {
                    double sideAngle = angle + (random.nextBoolean(0.5D) ? 1.0D : -1.0D) * 0.55D;
                    Point3 fork = elbow.add(Math.cos(sideAngle) * 1.4D, 0.4D, Math.sin(sideAngle) * 1.4D);
                    segments.add(new Segment(elbow, fork, 0.18D, 0.14D, 2));
                    addCluster(clusters, fork.add(0.0D, 0.35D, 0.0D),
                            Math.max(1.2D, maxClusterRad * (0.65D - 0.25D * progress)), 1.05D, maxLeaves);
                }

                double tipRad = Math.max(1.15D, maxClusterRad * (0.95D - 0.52D * progress));
                addCluster(clusters, end.add(0.0D, 0.35D, 0.0D), tipRad, 1.05D, maxLeaves);
            }

            double skirtRad = Math.max(1.2D, maxClusterRad * (1.02D - 0.58D * progress));
            addCluster(clusters, attach.add(0.0D, 0.25D, 0.0D), skirtRad, 1.05D, maxLeaves);
            whorlIdx++;
        }

        Point3 apex = trunk.get(trunk.size() - 1);
        addCluster(clusters, apex.add(0.0D, 0.65D, 0.0D), 1.35D, 1.15D, maxLeaves);
        addCluster(clusters, apex.add(0.0D, 1.55D, 0.0D), 0.90D, 0.90D, maxLeaves);
    }

    /**
     * Dynamic Trees {@code JungleLogic}: tall rainforest trunk with multiple long
     * diagonal lateral parkour boughs extending out along the trunk sides (each topped
     * with a walkable leaf platform at its outer end) plus wide spreading upper limbs.
     */
    private static void generateJungle(SpeciesProfile profile, DeterministicRandom random, int height,
                                       double crownWidth, double crownDensity, List<Point3> trunk,
                                       List<BranchNode> trunkNodes, List<Segment> segments,
                                       List<LeafCluster> clusters, int maxSegments, int maxLeaves,
                                       int maxDepth) {
        int parkourStart = Math.max(2, (int) Math.round(height * 0.20D));
        int parkourEnd = Math.max(parkourStart + 2, (int) Math.round(height * 0.66D));
        int parkourStep = Math.max(2, (parkourEnd - parkourStart) / (height >= 16 ? 4 : 3));
        double angle = random.nextDouble() * TWO_PI;

        // Lateral diagonal parkour boughs along the lower/mid trunk (disconnected below the high upper canopy)
        for (int y = parkourStart; y <= parkourEnd && segments.size() < maxSegments; y += parkourStep) {
            Point3 attach = pointAt(trunk, y);
            BranchNode parent = trunkNodes.get(Math.min(trunkNodes.size() - 1, y));

            double altProgress = (double) (y - parkourStart) / Math.max(1, parkourEnd - parkourStart);
            double reach = Math.max(3.0D, Math.min(5.2D,
                    (height * 0.24D + 1.4D) * (0.85D + 0.30D * random.nextDouble()) * (1.06D - 0.16D * altProgress)));
            double rise = 0.8D + random.nextDouble() * 1.1D;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);

            double startR = 0.45D - 0.11D * altProgress;
            double elbowR = 0.30D - 0.08D * altProgress;
            double tipR = 0.18D - 0.04D * altProgress;

            // Emerge horizontally 2 blocks out from trunk, then climb diagonally outward & upward
            Point3 elbow = attach.add(cos * 1.9D, 0.0D, sin * 1.9D);
            Point3 tip = attach.add(cos * reach, rise, sin * reach);

            BranchNode elbowNode = new BranchNode(elbow, elbowR, 1);
            parent.addChild(elbowNode);
            segments.add(new Segment(attach, elbow, startR, elbowR, 1));
            BranchNode tipNode = new BranchNode(tip, tipR, 1);
            elbowNode.addChild(tipNode);
            segments.add(new Segment(elbow, tip, elbowR, tipR, 1));

            // Walkable flat foliage pad only on outer tip so lateral boughs stay isolated from main canopy
            double padRad = Math.max(1.45D, Math.min(2.05D, 1.45D + crownWidth * 0.25D));
            addCluster(clusters, tip.add(0.0D, 0.30D, 0.0D), padRad, 0.78D, maxLeaves);

            // Occasional secondary fork at outer end of tall jungle limbs
            if (maxDepth >= 2 && segments.size() < maxSegments && reach >= 3.4D) {
                double forkAngle = angle + (random.nextBoolean(0.5D) ? 0.65D : -0.65D);
                Point3 forkTip = elbow.add(Math.cos(forkAngle) * (reach * 0.55D), rise * 0.85D,
                        Math.sin(forkAngle) * (reach * 0.55D));
                BranchNode forkNode = new BranchNode(forkTip, Math.max(0.14D, tipR - 0.02D), 2);
                elbowNode.addChild(forkNode);
                segments.add(new Segment(elbow, forkTip, Math.max(0.18D, elbowR * 0.8D), Math.max(0.14D, tipR - 0.02D), 2));
                addCluster(clusters, forkTip.add(0.0D, 0.30D, 0.0D), padRad * 0.85D, 0.76D, maxLeaves);
            }

            // Golden / alternate spiral ensures branches jut toward all sides for canopy parkour
            angle += Math.toRadians(115.0D + random.nextDouble() * 45.0D);
        }

        // Upper main jungle canopy crown (78..98% trunk height), cleanly gapped above lateral boughs
        int upperStart = Math.max(parkourEnd + 3, (int) Math.round(height * 0.78D));
        int upperLimbs = Math.max(4, Math.min(6, profile.primaryBranches().sample(random)));
        double crownBaseAngle = random.nextDouble() * TWO_PI;
        for (int i = 0; i < upperLimbs && segments.size() < maxSegments; i++) {
            double t = (double) i / Math.max(1, upperLimbs - 1);
            int y = Math.min(height - 1, Math.max(1, upperStart + (int) Math.round(t * Math.max(1, height - 1 - upperStart))));
            Point3 attach = pointAt(trunk, y);
            BranchNode parent = trunkNodes.get(Math.min(trunkNodes.size() - 1, y));

            double limbAngle = crownBaseAngle + i * (TWO_PI / upperLimbs) + (random.nextDouble() - 0.5D) * 0.35D;
            double reach = Math.max(3.0D, Math.min(5.6D, height * 0.26D + 1.3D + random.nextDouble() * 1.1D));
            double rise = 1.4D + random.nextDouble() * 1.4D;
            double cos = Math.cos(limbAngle);
            double sin = Math.sin(limbAngle);

            Point3 elbow = attach.add(cos * (reach * 0.45D), 0.3D, sin * (reach * 0.45D));
            Point3 end = attach.add(cos * reach, rise, sin * reach);
            BranchNode elbowNode = new BranchNode(elbow, 0.30D, 1);
            parent.addChild(elbowNode);
            segments.add(new Segment(attach, elbow, 0.38D, 0.28D, 1));
            BranchNode endNode = new BranchNode(end, 0.20D, 1);
            elbowNode.addChild(endNode);
            segments.add(new Segment(elbow, end, 0.28D, 0.18D, 1));

            double tipRad = Math.max(2.0D, Math.min(3.1D, (2.1D + crownWidth * 0.4D) * (0.85D + 0.2D * crownDensity)));
            addCluster(clusters, elbow.add(0.0D, 0.9D, 0.0D), tipRad * 0.78D, 1.15D, maxLeaves);
            addCluster(clusters, end.add(0.0D, 0.5D, 0.0D), tipRad, 1.35D, maxLeaves);

            if (maxDepth >= 2 && segments.size() < maxSegments) {
                double forkAngle = limbAngle + (random.nextBoolean(0.5D) ? 0.58D : -0.58D);
                Point3 forkEnd = elbow.add(Math.cos(forkAngle) * (reach * 0.52D), rise * 0.9D,
                        Math.sin(forkAngle) * (reach * 0.52D));
                elbowNode.addChild(new BranchNode(forkEnd, 0.16D, 2));
                segments.add(new Segment(elbow, forkEnd, 0.22D, 0.15D, 2));
                addCluster(clusters, forkEnd.add(0.0D, 0.5D, 0.0D), tipRad * 0.9D, 1.25D, maxLeaves);
            }
        }

        Point3 apex = trunk.get(trunk.size() - 1);
        addCluster(clusters, apex.add(0.0D, 0.6D, 0.0D), Math.min(3.2D, 2.3D + crownWidth * 0.35D), 1.45D, maxLeaves);
    }

    /** Dark oak / canopy / baobab: vaulted thick lower bole splitting into wide upper scaffold limbs. */
    private static void generateDarkCanopy(SpeciesProfile profile, DeterministicRandom random, int height,
                                           double crownWidth, double crownDensity, List<Point3> trunk,
                                           List<BranchNode> trunkNodes, List<Segment> segments,
                                           List<LeafCluster> clusters, int maxSegments, int maxLeaves,
                                           int maxDepth) {
        int splitY = Math.max(5, (int) Math.round(height * 0.58D));
        boolean isSocotra = profile.id().path().contains("socotra");
        if (!isSocotra && height >= 7) {
            int lowerMax = Math.min(splitY - 2, (int) Math.round(height * 0.44D));
            addLowerLateralBoughs(random, height, 2, lowerMax, height >= 12 ? 3 : 2,
                    2.00D, 3.40D, 0.55D, 0.42D, 1.35D, 0.70D, true,
                    trunk, trunkNodes, segments, clusters, maxSegments, maxLeaves);
        }
        int maxAttachY = Math.min(height - 1, (int) Math.round(trunk.get(trunk.size() - 1).y()));
        int rawLimbs = Math.max(3, Math.min(5, profile.primaryBranches().sample(random)));
        int neededLevels = (rawLimbs + 1) / 2;
        int minAttachY = Math.max(3, Math.min(splitY, maxAttachY - neededLevels + 1));
        int limbCount = Math.min(rawLimbs, Math.max(2, (maxAttachY - minAttachY + 1) * 2));
        double baseAzimuth = random.nextDouble() * TWO_PI;

        for (int i = 0; i < limbCount && segments.size() < maxSegments; i++) {
            int levelIdx = i / 2;
            int slotInLevel = i % 2;
            int y = Math.min(maxAttachY, minAttachY + levelIdx);
            Point3 attach = pointAt(trunk, y);
            BranchNode parent = trunkNodes.get(Math.min(trunkNodes.size() - 1, y));

            double angle = baseAzimuth + levelIdx * GOLDEN_ANGLE + slotInLevel * Math.PI + (random.nextDouble() - 0.5D) * 0.28D;
            double reach = Math.max(2.5D, Math.min(4.8D, height * 0.36D * (0.85D + 0.30D * random.nextDouble())));
            double rise = 1.4D + random.nextDouble() * 1.4D + Math.max(0, height - 1 - maxAttachY) * 0.65D;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);

            double elbowDy = 0.50D + 0.20D * slotInLevel;
            Point3 elbow = attach.add(cos * Math.min(1.6D, reach * 0.45D), elbowDy, sin * Math.min(1.6D, reach * 0.45D));
            Point3 end = attach.add(cos * reach, rise, sin * reach);

            BranchNode elbowNode = new BranchNode(elbow, 0.30D, 1);
            parent.addChild(elbowNode);
            segments.add(new Segment(attach, elbow, 0.38D, 0.28D, 1));
            BranchNode endNode = new BranchNode(end, 0.20D, 1);
            elbowNode.addChild(endNode);
            segments.add(new Segment(elbow, end, 0.28D, 0.18D, 1));

            double tipRad = Math.max(1.85D, Math.min(2.85D, (1.75D + crownWidth * 0.42D) * (0.85D + 0.2D * crownDensity)));
            addCluster(clusters, end.add(0.0D, 0.55D, 0.0D), tipRad, tipRad * 0.68D, maxLeaves);

            if (maxDepth >= 2 && segments.size() < maxSegments) {
                double forkAngle = angle + (random.nextBoolean(0.5D) ? 0.62D : -0.62D);
                Point3 fork = elbow.add(Math.cos(forkAngle) * (reach * 0.55D), rise * 0.92D,
                        Math.sin(forkAngle) * (reach * 0.55D));
                elbowNode.addChild(new BranchNode(fork, 0.16D, 2));
                segments.add(new Segment(elbow, fork, 0.22D, 0.14D, 2));
                addCluster(clusters, fork.add(0.0D, 0.55D, 0.0D), tipRad * 0.88D, tipRad * 0.65D, maxLeaves);
            }
        }

        Point3 apex = trunk.get(trunk.size() - 1);
        double domeRad = Math.max(2.0D, Math.min(3.0D, 1.8D + crownWidth * 0.45D));
        addCluster(clusters, apex.add(0.0D, 0.65D, 0.0D), domeRad, 1.50D, maxLeaves);
    }

    /** Savanna acacia: high asymmetric diagonal limbs topped with crisp flat umbrella leaf tables. */
    private static void generateAcacia(SpeciesProfile profile, DeterministicRandom random, int height,
                                       double crownWidth, double crownDensity, List<Point3> trunk,
                                       List<BranchNode> trunkNodes, List<Segment> segments,
                                       List<LeafCluster> clusters, int maxSegments, int maxLeaves,
                                       int maxDepth) {
        int splitY = Math.max(3, (int) Math.round(height * 0.64D));
        int boughs = Math.max(2, Math.min(4, profile.primaryBranches().sample(random)));
        double baseAzimuth = random.nextDouble() * TWO_PI;

        for (int i = 0; i < boughs && segments.size() < maxSegments; i++) {
            double t = (double) i / Math.max(1, boughs - 1);
            int y = Math.min(height, splitY + (int) Math.round(t * Math.max(1, height - splitY)));
            Point3 attach = pointAt(trunk, y);
            BranchNode parent = trunkNodes.get(Math.min(trunkNodes.size() - 1, y));

            double angle = baseAzimuth + i * (TWO_PI / boughs) + (random.nextDouble() - 0.5D) * 0.45D;
            double reach = Math.max(2.4D, Math.min(4.6D, height * 0.44D * (0.82D + 0.35D * random.nextDouble())));
            double rise = 1.6D + random.nextDouble() * 1.1D;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);

            Point3 mid = attach.add(cos * (reach * 0.48D), rise * 0.55D, sin * (reach * 0.48D));
            Point3 end = attach.add(cos * reach, rise, sin * reach);
            BranchNode midNode = new BranchNode(mid, 0.25D, 1);
            parent.addChild(midNode);
            segments.add(new Segment(attach, mid, 0.30D, 0.24D, 1));
            midNode.addChild(new BranchNode(end, 0.18D, 1));
            segments.add(new Segment(mid, end, 0.24D, 0.16D, 1));

            double tableRad = Math.max(2.2D, Math.min(3.3D, (1.9D + crownWidth * 0.55D) * (0.85D + 0.2D * crownDensity)));
            addCluster(clusters, end.add(0.0D, 0.50D, 0.0D), tableRad, 1.00D, maxLeaves);

            if (maxDepth >= 2 && i == 0 && segments.size() < maxSegments) {
                double forkAngle = angle + (random.nextBoolean(0.5D) ? 0.72D : -0.72D);
                Point3 fork = mid.add(Math.cos(forkAngle) * (reach * 0.48D), rise * 0.5D,
                        Math.sin(forkAngle) * (reach * 0.48D));
                segments.add(new Segment(mid, fork, 0.20D, 0.15D, 2));
                addCluster(clusters, fork.add(0.0D, 0.50D, 0.0D), tableRad * 0.85D, 1.00D, maxLeaves);
            }
        }

        Point3 apex = trunk.get(trunk.size() - 1);
        addCluster(clusters, apex.add(0.0D, 0.85D, 0.0D), Math.min(2.8D, 2.0D + crownWidth * 0.4D), 1.00D, maxLeaves);
    }

    /** Slender birch / silver birch / poplar: tall clear white bark with compact upward crown branches. */
    private static void generateBirch(SpeciesProfile profile, DeterministicRandom random, int height,
                                      double crownWidth, double crownDensity, List<Point3> trunk,
                                      List<BranchNode> trunkNodes, List<Segment> segments,
                                      List<LeafCluster> clusters, int maxSegments, int maxLeaves) {
        int crownStart = Math.max(4, (int) Math.round(height * 0.58D));
        if (height >= 7) {
            int lowerMax = Math.min(crownStart - 2, (int) Math.round(height * 0.45D));
            addLowerLateralBoughs(random, height, 2, lowerMax, height >= 11 ? 2 : 1,
                    1.30D, 2.00D, 0.35D, 0.24D, 1.05D, 0.65D, true,
                    trunk, trunkNodes, segments, clusters, maxSegments, maxLeaves);
        }
        int count = Math.max(3, Math.min(5, profile.primaryBranches().sample(random)));
        double baseAzimuth = random.nextDouble() * TWO_PI;

        for (int i = 0; i < count && segments.size() < maxSegments; i++) {
            double t = (double) i / Math.max(1, count - 1);
            int y = Math.min(height - 1, crownStart + (int) Math.round(t * Math.max(1, height - 1 - crownStart)));
            Point3 attach = pointAt(trunk, y);
            BranchNode parent = trunkNodes.get(Math.min(trunkNodes.size() - 1, y));

            double angle = baseAzimuth + i * GOLDEN_ANGLE + (random.nextDouble() - 0.5D) * 0.35D;
            double reach = Math.max(1.1D, Math.min(2.1D, height * 0.21D * (0.85D + 0.30D * random.nextDouble())));
            double rise = 1.0D + random.nextDouble() * 0.9D;
            Point3 end = attach.add(Math.cos(angle) * reach, rise, Math.sin(angle) * reach);

            parent.addChild(new BranchNode(end, 0.16D, 1));
            segments.add(new Segment(attach, end, 0.22D, 0.15D, 1));

            double rad = Math.max(1.25D, Math.min(1.95D, (1.35D + crownWidth * 0.35D) * (0.85D + 0.2D * crownDensity)));
            addCluster(clusters, end.add(0.0D, 0.45D, 0.0D), rad, rad * 0.85D, maxLeaves);
        }

        Point3 apex = trunk.get(trunk.size() - 1);
        addCluster(clusters, apex.add(0.0D, 0.55D, 0.0D), 1.65D, 1.50D, maxLeaves);
    }

    /** Palm / coconut / banana: gently curved branchless stem topped with arching frond foliage. */
    private static void generatePalm(SpeciesProfile profile, DeterministicRandom random, int height,
                                     double crownWidth, List<Point3> trunk, List<BranchNode> trunkNodes,
                                     List<Segment> segments, List<LeafCluster> clusters,
                                     int maxSegments, int maxLeaves) {
        Point3 apex = trunk.get(trunk.size() - 1);
        int fronds = Math.max(5, Math.min(7, profile.primaryBranches().sample(random)));
        double baseAzimuth = random.nextDouble() * TWO_PI;

        for (int i = 0; i < fronds; i++) {
            double angle = baseAzimuth + i * (TWO_PI / fronds) + (random.nextDouble() - 0.5D) * 0.28D;
            double reach = Math.max(1.4D, Math.min(2.3D,
                    (1.55D + crownWidth * 0.35D) * (0.82D + 0.36D * random.nextDouble())));
            double archY = 0.25D + random.nextDouble() * 0.55D;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            addCluster(clusters, apex.add(cos * (reach * 0.52D), archY, sin * (reach * 0.52D)),
                    1.35D, 0.95D, maxLeaves);
            addCluster(clusters, apex.add(cos * reach, archY - 0.35D, sin * reach),
                    1.45D, 0.90D, maxLeaves);
        }
        addCluster(clusters, apex.add(0.0D, 0.60D, 0.0D), 1.55D, 1.05D, maxLeaves);
    }

    /** Mangrove & willow: wide spreading boughs with outer-perimeter draping canopy tips. */
    private static void generateWillowMangrove(SpeciesProfile profile, DeterministicRandom random, int height,
                                               double crownWidth, double crownDensity, List<Point3> trunk,
                                               List<BranchNode> trunkNodes, List<Segment> segments,
                                               List<LeafCluster> clusters, int maxSegments, int maxLeaves,
                                               int maxDepth) {
        int crownStart = Math.max(4, (int) Math.round(height * 0.52D));
        if (height >= 6) {
            int lowerMax = Math.max(2, Math.min(crownStart - 2, (int) Math.round(height * 0.42D)));
            addLowerLateralBoughs(random, height, 2, lowerMax, height >= 10 ? 2 : 1,
                    1.95D, 2.90D, 0.35D, 0.34D, 1.25D, 0.70D, true,
                    trunk, trunkNodes, segments, clusters, maxSegments, maxLeaves);
        }
        int maxAttachY = Math.min(height - 1, (int) Math.round(trunk.get(trunk.size() - 1).y()));
        int rawLimbs = Math.max(3, Math.min(5, profile.primaryBranches().sample(random)));
        int neededLevels = (rawLimbs + 1) / 2;
        int minAttachY = Math.max(3, Math.min(crownStart, maxAttachY - neededLevels + 1));
        int limbs = Math.min(rawLimbs, Math.max(2, (maxAttachY - minAttachY + 1) * 2));
        double baseAzimuth = random.nextDouble() * TWO_PI;

        for (int i = 0; i < limbs && segments.size() < maxSegments; i++) {
            int levelIdx = i / 2;
            int slotInLevel = i % 2;
            int y = Math.min(maxAttachY, minAttachY + levelIdx);
            Point3 attach = pointAt(trunk, y);
            BranchNode parent = trunkNodes.get(Math.min(trunkNodes.size() - 1, y));

            double angle = baseAzimuth + levelIdx * GOLDEN_ANGLE + slotInLevel * Math.PI + (random.nextDouble() - 0.5D) * 0.28D;
            double reach = Math.max(2.4D, Math.min(4.2D, height * 0.38D * (0.85D + 0.30D * random.nextDouble())));
            double rise = 1.0D + random.nextDouble() * 1.1D + Math.max(0, height - 1 - maxAttachY) * 0.60D;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);

            double elbowDy = 0.45D + 0.20D * slotInLevel;
            Point3 elbow = attach.add(cos * Math.min(1.6D, reach * 0.48D), elbowDy, sin * Math.min(1.6D, reach * 0.48D));
            Point3 end = attach.add(cos * reach, rise, sin * reach);
            BranchNode elbowNode = new BranchNode(elbow, 0.25D, 1);
            parent.addChild(elbowNode);
            segments.add(new Segment(attach, elbow, 0.32D, 0.24D, 1));
            elbowNode.addChild(new BranchNode(end, 0.18D, 1));
            segments.add(new Segment(elbow, end, 0.24D, 0.16D, 1));

            double rad = Math.max(1.75D, Math.min(2.6D, (1.65D + crownWidth * 0.38D) * (0.85D + 0.2D * crownDensity)));
            addCluster(clusters, end.add(0.0D, 0.45D, 0.0D), rad, 1.45D, maxLeaves);
            // Outer-rim hanging drape strictly outside the inner trunk radius (never near trunk collar!)
            addCluster(clusters, end.add(cos * 0.6D, -0.45D, sin * 0.6D), Math.max(1.05D, rad * 0.62D), 1.35D, maxLeaves);

            if (maxDepth >= 2 && segments.size() < maxSegments) {
                double forkAngle = angle + (random.nextBoolean(0.5D) ? 0.55D : -0.55D);
                Point3 fork = elbow.add(Math.cos(forkAngle) * (reach * 0.5D), rise * 0.85D,
                        Math.sin(forkAngle) * (reach * 0.5D));
                segments.add(new Segment(elbow, fork, 0.20D, 0.14D, 2));
                addCluster(clusters, fork.add(0.0D, 0.45D, 0.0D), rad * 0.85D, 1.35D, maxLeaves);
            }
        }

        Point3 apex = trunk.get(trunk.size() - 1);
        addCluster(clusters, apex.add(0.0D, 0.60D, 0.0D), Math.min(2.6D, 1.8D + crownWidth * 0.35D), 1.40D, maxLeaves);
    }

    /**
     * Dynamic Trees deciduous broadleaf (oak, fancy_oak, maple, magnolia, cherry, walnut, skyroot, echo):
     * clean lower trunk, 4..7 radial limbs emerging horizontally first then stepping diagonally up & out
     * with secondary forks, each ending inside a rounded leaf cluster above the limb.
     */
    private static void generateBroadleaf(SpeciesProfile profile, DeterministicRandom random, int height,
                                          double crownWidth, double crownDensity, List<Point3> trunk,
                                          List<BranchNode> trunkNodes, List<Segment> segments,
                                          List<LeafCluster> clusters, int maxSegments, int maxLeaves,
                                          int maxDepth) {
        boolean isDead = profile.id().path().equals("dead") || profile.id().path().equals("dead_pine");
        int crownStart = Math.max(4, (int) Math.round(height * 0.56D));
        if (height >= 5) {
            int lowerMax = Math.max(2, Math.min(crownStart - 2, (int) Math.round(height * 0.48D)));
            int lowerCount = height >= 11 ? 3 : (height >= 8 ? 2 : 1);
            addLowerLateralBoughs(random, height, 2, lowerMax, lowerCount,
                    2.10D, height >= 10 ? 3.10D : 2.55D, 0.45D,
                    0.36D, 1.18D, 0.68D, !isDead,
                    trunk, trunkNodes, segments, clusters, maxSegments, maxLeaves);
        }
        int maxAttachY = Math.min(height - 1, (int) Math.round(trunk.get(trunk.size() - 1).y()));
        int rawLimbs = Math.max(3, Math.min(5, profile.primaryBranches().sample(random)));
        int neededLevels = (rawLimbs + 1) / 2;
        int minAttachY = Math.max(3, Math.min(crownStart, maxAttachY - neededLevels + 1));
        int limbCount = Math.min(rawLimbs, Math.max(2, (maxAttachY - minAttachY + 1) * 2));
        double baseAzimuth = random.nextDouble() * TWO_PI;

        for (int i = 0; i < limbCount && segments.size() < maxSegments; i++) {
            double t = (double) i / Math.max(1, limbCount - 1);
            int levelIdx = i / 2;
            int slotInLevel = i % 2;
            int y = Math.min(maxAttachY, minAttachY + levelIdx);
            Point3 attach = pointAt(trunk, y);
            BranchNode parent = trunkNodes.get(Math.min(trunkNodes.size() - 1, y));

            double angle = baseAzimuth + levelIdx * GOLDEN_ANGLE + slotInLevel * Math.PI + (random.nextDouble() - 0.5D) * 0.28D;
            double reachRatio = Math.max(0.24D, Math.min(0.45D, profile.branchLengthRatio().sample(random)));
            double reach = Math.max(1.8D, Math.min(3.8D, height * reachRatio * (0.90D + 0.30D * (1.0D - t))));
            double rise = Math.max(0.9D, Math.min(2.3D, reach * (0.42D + random.nextDouble() * 0.25D)))
                    + Math.max(0, height - 1 - maxAttachY) * 0.65D;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);

            // Step horizontally 1..1.5 blocks out from trunk first, then climb diagonally to tip
            double elbowReach = Math.min(1.5D, reach * 0.48D);
            double elbowDy = 0.45D + 0.20D * slotInLevel;
            Point3 elbow = attach.add(cos * elbowReach, elbowDy, sin * elbowReach);
            Point3 end = attach.add(cos * reach, rise, sin * reach);

            BranchNode elbowNode = new BranchNode(elbow, 0.24D, 1);
            parent.addChild(elbowNode);
            segments.add(new Segment(attach, elbow, 0.32D, 0.22D, 1));
            elbowNode.addChild(new BranchNode(end, 0.18D, 1));
            segments.add(new Segment(elbow, end, 0.22D, 0.16D, 1));

            double baseTipRad = isDead ? 1.05D : Math.max(1.65D, Math.min(2.55D,
                    (1.55D + crownWidth * 0.42D) * (0.85D + 0.22D * crownDensity)));
            addCluster(clusters, end.add(0.0D, 0.45D, 0.0D), baseTipRad, isDead ? 0.85D : baseTipRad * 0.78D, maxLeaves);

            if (!isDead && maxDepth >= 2 && reach >= 2.1D && segments.size() < maxSegments) {
                double forkAngle = angle + (random.nextBoolean(0.5D) ? 0.62D : -0.62D);
                double forkReach = reach * (0.48D + random.nextDouble() * 0.18D);
                Point3 forkEnd = elbow.add(Math.cos(forkAngle) * forkReach, rise * 0.85D,
                        Math.sin(forkAngle) * forkReach);
                elbowNode.addChild(new BranchNode(forkEnd, 0.15D, 2));
                segments.add(new Segment(elbow, forkEnd, 0.20D, 0.14D, 2));
                addCluster(clusters, forkEnd.add(0.0D, 0.45D, 0.0D), baseTipRad * 0.86D, baseTipRad * 0.72D, maxLeaves);
            }
        }

        Point3 apex = trunk.get(trunk.size() - 1);
        if (isDead) {
            addCluster(clusters, apex.add(0.0D, 0.45D, 0.0D), 1.10D, 0.85D, maxLeaves);
        } else {
            double apexRad = Math.max(1.80D, Math.min(2.65D, (1.65D + crownWidth * 0.40D) * (0.88D + 0.18D * crownDensity)));
            addCluster(clusters, apex.add(0.0D, 0.55D, 0.0D), apexRad, apexRad * 0.75D, maxLeaves);
        }
    }

    /**
     * Generates sub-canopy lateral branches below and vertically disconnected from the upper crown,
     * with thickness and reach scaled by altitude on the trunk.
     */
    private static void addLowerLateralBoughs(DeterministicRandom random,
                                              int height,
                                              int minY,
                                              int maxY,
                                              int count,
                                              double minReach,
                                              double maxReach,
                                              double maxRise,
                                              double baseStartRadius,
                                              double leafPadRadius,
                                              double leafPadHeight,
                                              boolean addLeafPad,
                                              List<Point3> trunk,
                                              List<BranchNode> trunkNodes,
                                              List<Segment> segments,
                                              List<LeafCluster> clusters,
                                              int maxSegments,
                                              int maxLeaves) {
        if (maxY < minY || count <= 0 || segments.size() >= maxSegments) {
            return;
        }
        int span = maxY - minY;
        int actualCount = Math.min(count, Math.max(1, (span / 2) + 1));
        double baseAzimuth = random.nextDouble() * TWO_PI;

        for (int i = 0; i < actualCount && segments.size() < maxSegments; i++) {
            int y = actualCount == 1
                    ? (span >= 3 ? minY + random.nextInt(2) : minY)
                    : Math.min(maxY, minY + (int) Math.round(i * (double) span / (actualCount - 1)));
            Point3 attach = pointAt(trunk, y);
            BranchNode parent = trunkNodes.get(Math.min(trunkNodes.size() - 1, y));

            double altFrac = (double) y / Math.max(1, height);
            double taper = Math.max(0.55D, 1.15D - altFrac * 0.90D);
            double startR = Math.max(0.18D, baseStartRadius * taper);
            double midR = Math.max(0.15D, startR * 0.68D);
            double tipR = Math.max(0.12D, startR * 0.44D);

            double angle = baseAzimuth + i * GOLDEN_ANGLE + (random.nextDouble() - 0.5D) * 0.45D;
            double reach = Math.max(minReach,
                    (minReach + (maxReach - minReach) * (0.45D + 0.55D * random.nextDouble()))
                    * Math.max(0.82D, 1.04D - altFrac * 0.35D));
            double rise = random.nextDouble() * maxRise;
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);
            Point3 tip = attach.add(cos * reach, rise, sin * reach);

            if (reach >= 2.0D && segments.size() + 2 <= maxSegments) {
                double elbowReach = Math.max(1.0D, Math.min(1.6D, reach * 0.52D));
                Point3 elbow = attach.add(cos * elbowReach, 0.0D, sin * elbowReach);
                BranchNode elbowNode = new BranchNode(elbow, midR, 1);
                parent.addChild(elbowNode);
                segments.add(new Segment(attach, elbow, startR, midR, 1));
                elbowNode.addChild(new BranchNode(tip, tipR, 1));
                segments.add(new Segment(elbow, tip, midR, tipR, 1));
            } else {
                parent.addChild(new BranchNode(tip, tipR, 1));
                segments.add(new Segment(attach, tip, startR, tipR, 1));
            }

            if (addLeafPad) {
                addCluster(clusters, tip.add(0.0D, 0.25D, 0.0D), leafPadRadius, leafPadHeight, maxLeaves);
            }
        }
    }

    private static Point3 pointAt(List<Point3> trunk, int y) {
        return trunk.get(Math.max(0, Math.min(trunk.size() - 1, y)));
    }

    private static void addCluster(List<LeafCluster> clusters, Point3 center,
                                   double horzRadius, double vertRadius, int maxLeaves) {
        if (clusters.size() >= maxLeaves) {
            return;
        }
        double h = Math.max(0.6D, horzRadius);
        double v = Math.max(0.6D, vertRadius);
        clusters.add(new LeafCluster(center, h, v, h, 1));
    }

    private static Style classify(SpeciesProfile profile) {
        String path = profile.id().path().toLowerCase(Locale.ROOT);
        TreeArchetype arch = profile.archetype();
        if (arch == TreeArchetype.COLUMNAR || path.contains("pine")) {
            return Style.PINE;
        }
        if (path.contains("jungle") || path.contains("kapok") || path.contains("eucalyptus")) {
            return Style.JUNGLE;
        }
        if (arch == TreeArchetype.CONIFER || arch == TreeArchetype.CYPRESS
                || path.contains("spruce") || path.contains("larch")
                || path.contains("redwood") || path.contains("pewen")) {
            return Style.CONIFER;
        }
        if (arch == TreeArchetype.BAOBAB || path.contains("dark_oak")
                || path.contains("darkwood") || path.equals("dark")
                || path.contains("canopy") || path.contains("ancient")
                || path.contains("baobab") || path.contains("socotra")) {
            return Style.DARK_CANOPY;
        }
        if (arch == TreeArchetype.ACACIA || path.contains("acacia") || path.contains("thornwood")) {
            return Style.ACACIA;
        }
        if (arch == TreeArchetype.BIRCH || path.contains("birch")
                || path.contains("poplar") || path.contains("conberry")) {
            return Style.BIRCH;
        }
        if (arch == TreeArchetype.PALM || path.contains("palm")
                || path.contains("coconut") || path.contains("banana")) {
            return Style.PALM;
        }
        if (arch == TreeArchetype.MANGROVE || path.contains("willow")
                || path.contains("mangrove") || path.contains("yagroot") || path.contains("sunroot")) {
            return Style.WILLOW_MANGROVE;
        }
        return Style.BROADLEAF;
    }

    private enum Style {
        PINE,
        CONIFER,
        JUNGLE,
        DARK_CANOPY,
        ACACIA,
        BIRCH,
        PALM,
        WILLOW_MANGROVE,
        BROADLEAF
    }
}
