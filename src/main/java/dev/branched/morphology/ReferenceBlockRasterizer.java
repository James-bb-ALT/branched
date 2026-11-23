package dev.branched.morphology;

import dev.branched.core.ResourceId;
import dev.branched.species.SpeciesProfile;
import dev.branched.species.TreeArchetype;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Deterministic 6-connected voxel rasterizer with cohesive foliage pads,
 * guaranteed branch-tip canopy cover, and clean sub-canopy undersides.
 */
public final class ReferenceBlockRasterizer implements BlockRasterizer {
    private static final int[][] CARDINALS = {
            {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}
    };
    private static final int[][] FACE_NEIGHBORS = {
            {1, 0, 0}, {-1, 0, 0}, {0, 1, 0}, {0, -1, 0}, {0, 0, 1}, {0, 0, -1}
    };

    @Override
    public RasterizedTree rasterize(TreeSkeleton skeleton, SpeciesProfile profile, BlockPalette palette) {
        return rasterize(skeleton, profile, palette, 0, 0, 0);
    }

    @Override
    public RasterizedTree rasterize(TreeSkeleton skeleton, SpeciesProfile profile, BlockPalette palette,
                                    int originX, int originY, int originZ) {
        return rasterize(skeleton, profile, palette, originX, originY, originZ,
                PerformancePreset.BALANCED);
    }

    /**
     * Rasterizes with an explicit geometry preset. Every consecutive wood block
     * along a trunk or branch shares a full 2D face so 6-way multipart branches
     * render as seamless connected 3D limbs with zero diagonal gaps.
     */
    public RasterizedTree rasterize(TreeSkeleton skeleton, SpeciesProfile profile, BlockPalette palette,
                                    int originX, int originY, int originZ,
                                    PerformancePreset preset) {
        Set<Long> occupiedWood = new HashSet<>();
        Set<Long> trunkKeys = new HashSet<>();
        Set<Long> branchKeys = new HashSet<>();
        Map<Long, BlockPlacement> placed = new LinkedHashMap<>();

        int blockBudget = Math.max(16, profile.maxQuads() / 6);
        int leafBudget = preset.leafBudget(profile.maxLeaves());
        int logs = 0;

        // Track branch endpoints that do not fork further so we guarantee tip leaf pads
        Set<Long> branchStarts = new HashSet<>();
        List<int[]> rawBranchEnds = new ArrayList<>();
        int[] trunkApex = new int[]{originX, originY, originZ};

        boolean isDead = isDeadSpecies(profile);
        boolean isFlatUnderside = isFlatUndersideArchetype(profile);
        boolean isJungleParkour = isJungleSpecies(profile);
        boolean isPalm = isPalmSpecies(profile);
        boolean twoByTwoTrunk = !isPalm && skeleton.root().radius() >= 1.15D;
        Map<Long, int[]> actualNodeEnds = new HashMap<>();

        for (Segment segment : skeleton.segments()) {
            if (placed.size() >= blockBudget) {
                break;
            }
            boolean isTrunk = segment.depth() == 0;
            int sx = originX + (int) Math.round(segment.start().x());
            int sy = originY + (int) Math.round(segment.start().y());
            int sz = originZ + (int) Math.round(segment.start().z());
            int ex = originX + (int) Math.round(segment.end().x());
            int ey = originY + (int) Math.round(segment.end().y());
            int ez = originZ + (int) Math.round(segment.end().z());

            if (!isTrunk) {
                long rawStartKey = key(sx, sy, sz);
                int[] remappedStart = actualNodeEnds.get(rawStartKey);
                int startX = remappedStart != null ? remappedStart[0] : sx;
                int startY = remappedStart != null ? remappedStart[1] : sy;
                int startZ = remappedStart != null ? remappedStart[2] : sz;

                if (twoByTwoTrunk && startX >= originX && startX <= originX + 1
                        && startZ >= originZ && startZ <= originZ + 1) {
                    int cornerX = originX + (ex > originX ? 1 : 0);
                    int cornerZ = originZ + (ez > originZ ? 1 : 0);
                    if (trunkKeys.contains(key(cornerX, startY, cornerZ))) {
                        startX = cornerX;
                        startZ = cornerZ;
                    }
                }

                branchStarts.add(key(startX, startY, startZ));
                int[] actualEnd = walkAndPlaceBranch(
                        startX, startY, startZ, ex, ey, ez,
                        originY, blockBudget, palette.branch(),
                        placed, occupiedWood, trunkKeys, branchKeys);
                if (occupiedWood.contains(key(actualEnd[0], actualEnd[1], actualEnd[2]))) {
                    actualNodeEnds.put(key(ex, ey, ez), actualEnd);
                }
                rawBranchEnds.add(actualEnd);
                continue;
            }

            List<int[]> path = walk6Connected(sx, sy, sz, ex, ey, ez, false);
            int thickness = twoByTwoTrunk ? 2 : 1;
            for (int i = 0; i < path.size(); i++) {
                if (placed.size() >= blockBudget) {
                    break;
                }
                int[] step = path.get(i);
                for (int ox = 0; ox < thickness; ox++) {
                    for (int oz = 0; oz < thickness; oz++) {
                        int x = step[0] + ox;
                        int y = step[1];
                        int z = step[2] + oz;
                        if (y < originY) {
                            continue;
                        }
                        boolean offAxisUpperStem = !isPalm && thickness == 1
                                && y >= originY + 4 && (x != originX || z != originZ);
                        long k = key(x, y, z);
                        if (!offAxisUpperStem) {
                            trunkKeys.add(k);
                            if (y >= trunkApex[1]) {
                                trunkApex[0] = x;
                                trunkApex[1] = y;
                                trunkApex[2] = z;
                            }
                            // If a branch previously touched this trunk position, promote to LOG
                            branchKeys.remove(k);
                            BlockPlacement existing = placed.get(k);
                            if (existing == null || existing.role() != PlacementRole.LOG) {
                                if (existing == null && placed.size() >= blockBudget) {
                                    break;
                                }
                                if (existing == null) {
                                    logs++;
                                    occupiedWood.add(k);
                                }
                                placed.put(k, new BlockPlacement(x, y, z, palette.log(), PlacementRole.LOG));
                            }
                        } else if (!occupiedWood.contains(k)
                                && countNeighbors(occupiedWood, x, y, z) == 1
                                && placed.size() < blockBudget) {
                            occupiedWood.add(k);
                            branchKeys.add(k);
                            placed.put(k, new BlockPlacement(x, y, z, palette.branch(), PlacementRole.BRANCH));
                        }
                    }
                }
            }
        }

        List<int[]> terminalTips = new ArrayList<>();
        for (int[] tip : rawBranchEnds) {
            long tk = key(tip[0], tip[1], tip[2]);
            if (branchKeys.contains(tk) && !branchStarts.contains(tk)) {
                terminalTips.add(tip);
            }
        }

        Set<Long> leafKeys = new HashSet<>();
        int leaves = 0;
        boolean truncated = placed.size() >= blockBudget;

        // Monotonic preset scaling: PERFORMANCE (~0.84) < BALANCED (1.00) < VISUAL (~1.14)
        double outerReach = Math.pow(preset.leafDensityFactor(), 0.90D);
        double solidCoreLimit = 0.58D;
        double lowerCutoff = isFlatUnderside ? -0.25D : -0.52D;

        for (LeafCluster cluster : skeleton.leafClusters()) {
            if (truncated || placed.size() >= blockBudget || leaves >= leafBudget) {
                truncated = true;
                break;
            }
            int rx = (int) Math.ceil(cluster.radiusX() * outerReach);
            int ry = (int) Math.ceil(cluster.radiusY() * outerReach);
            int rz = (int) Math.ceil(cluster.radiusZ() * outerReach);
            int cx = originX + (int) Math.round(cluster.center().x());
            int cy = originY + (int) Math.round(cluster.center().y());
            int cz = originZ + (int) Math.round(cluster.center().z());

            for (int dx = -rx; dx <= rx; dx++) {
                for (int dy = -ry; dy <= ry; dy++) {
                    if (dy < lowerCutoff * cluster.radiusY()) {
                        continue;
                    }
                    for (int dz = -rz; dz <= rz; dz++) {
                        double normSq = (dx * dx) / (cluster.radiusX() * cluster.radiusX())
                                + (dy * dy) / (cluster.radiusY() * cluster.radiusY())
                                + (dz * dz) / (cluster.radiusZ() * cluster.radiusZ());
                        double norm = Math.sqrt(normSq);
                        if (norm > outerReach) {
                            continue;
                        }

                        int x = cx + dx;
                        int y = cy + dy;
                        int z = cz + dz;
                        if (y <= originY) {
                            continue;
                        }

                        if (norm > solidCoreLimit * Math.min(1.0D, outerReach)) {
                            double edgeFraction = 0.70D + 0.30D * voxelHash(x, y, z);
                            double cutoff = solidCoreLimit + (outerReach - solidCoreLimit) * edgeFraction;
                            if (norm > cutoff) {
                                continue;
                            }
                        }

                        long k = key(x, y, z);
                        if (occupiedWood.contains(k) || leafKeys.contains(k)) {
                            continue;
                        }
                        if (placed.size() >= blockBudget || leaves >= leafBudget) {
                            truncated = true;
                            break;
                        }
                        leafKeys.add(k);
                        placed.put(k, new BlockPlacement(x, y, z, palette.leaves(), PlacementRole.LEAVES));
                        leaves++;
                    }
                }
            }
        }

        // Clean inner sub-canopy underside: remove lone/thin leaf blocks directly below inner branches
        // or directly touching the lower trunk collar underneath an inner branch arm.
        int lowestCrownY = computeLowestCrownY(branchKeys, originY + 2, trunkApex[1]);
        leaves = cleanUndersideAndSingletons(placed, trunkKeys, branchKeys, leafKeys,
                originX, originZ, lowestCrownY, isDead);

        // Guaranteed top & tip cover on living trees: NO bare branch tip or crown branch pokes into empty sky!
        if (!isDead) {
            int maxGuarantee = Math.min(blockBudget, leafBudget + placed.size());
            leaves = enforceCanopyCover(placed, occupiedWood, trunkKeys, branchKeys, leafKeys,
                    terminalTips, trunkApex, preset, originY, maxGuarantee,
                    isJungleParkour, leaves);
        }

        List<BlockPlacement> blocks = new ArrayList<>(placed.values());
        int quads = exposedFaces(blocks);
        return new RasterizedTree(blocks, logs, leaves, quads, truncated);
    }

    private static int[] walkAndPlaceBranch(int startX, int startY, int startZ,
                                              int ex, int ey, int ez,
                                              int originY,
                                              int blockBudget,
                                              ResourceId branchId,
                                              Map<Long, BlockPlacement> placed,
                                              Set<Long> occupiedWood,
                                              Set<Long> trunkKeys,
                                              Set<Long> branchKeys) {
        if (!occupiedWood.contains(key(startX, startY, startZ))) {
            long startKey = key(startX, startY, startZ);
            if (startY > originY
                    && placed.size() < blockBudget
                    && countNeighbors(occupiedWood, startX, startY, startZ) == 1
                    && !createsVerticalInnerSandwich(trunkKeys, branchKeys, startX, startY, startZ)
                    && !trapsRequiredCoverCell(occupiedWood, branchKeys, startX, startY, startZ)) {
                occupiedWood.add(startKey);
                branchKeys.add(startKey);
                placed.put(startKey, new BlockPlacement(startX, startY, startZ, branchId, PlacementRole.BRANCH));
            } else {
                return new int[]{startX, startY, startZ};
            }
        }

        int curX = startX;
        int curY = startY;
        int curZ = startZ;
        int maxSteps = Math.abs(ex - startX) + Math.abs(ey - startY) + Math.abs(ez - startZ) + 2;
        for (int stepIdx = 0; stepIdx < maxSteps && (curX != ex || curY != ey || curZ != ez); stepIdx++) {
            if (placed.size() >= blockBudget) {
                break;
            }
            if (branchKeys.contains(key(curX, curY, curZ))
                    && countNeighbors(branchKeys, curX, curY, curZ) >= 3) {
                break;
            }
            int rx = Math.abs(ex - curX);
            int ry = Math.abs(ey - curY);
            int rz = Math.abs(ez - curZ);
            int dx = Integer.compare(ex, curX);
            int dy = Integer.compare(ey, curY);
            int dz = Integer.compare(ez, curZ);

            List<int[]> candidates = new ArrayList<>(3);
            if (dx != 0) candidates.add(new int[]{curX + dx, curY, curZ, rx});
            if (dz != 0) candidates.add(new int[]{curX, curY, curZ + dz, rz});
            if (dy != 0) candidates.add(new int[]{curX, curY + dy, curZ, ry});

            boolean fromTrunk = trunkKeys.contains(key(curX, curY, curZ));
            int stepY = curY;
            candidates.sort((a, b) -> {
                if (fromTrunk) {
                    boolean aHoriz = a[1] == stepY;
                    boolean bHoriz = b[1] == stepY;
                    if (aHoriz != bHoriz) {
                        return aHoriz ? -1 : 1;
                    }
                }
                return Integer.compare(b[3], a[3]);
            });

            int[] chosen = null;
            for (int[] cand : candidates) {
                int nx = cand[0];
                int ny = cand[1];
                int nz = cand[2];
                if (fromTrunk && ny > curY) {
                    continue; // Branches must originate laterally from the trunk, never vertically off its top
                }
                long nk = key(nx, ny, nz);
                if (ny <= originY || occupiedWood.contains(nk)) {
                    continue;
                }
                if (countNeighbors(occupiedWood, nx, ny, nz) != 1) {
                    continue;
                }
                if (createsVerticalInnerSandwich(trunkKeys, branchKeys, nx, ny, nz)) {
                    continue;
                }
                if (trapsRequiredCoverCell(occupiedWood, branchKeys, nx, ny, nz)) {
                    continue;
                }
                chosen = cand;
                break;
            }
            if (chosen == null) {
                break;
            }

            curX = chosen[0];
            curY = chosen[1];
            curZ = chosen[2];
            long ck = key(curX, curY, curZ);
            occupiedWood.add(ck);
            branchKeys.add(ck);
            placed.put(ck, new BlockPlacement(curX, curY, curZ, branchId, PlacementRole.BRANCH));
        }
        return new int[]{curX, curY, curZ};
    }

    private static boolean createsVerticalInnerSandwich(Set<Long> trunkKeys,
                                                        Set<Long> branchKeys,
                                                        int x, int y, int z) {
        if (!isAdjacentToTrunk(trunkKeys, x, y, z)) {
            return false;
        }
        return branchKeys.contains(key(x, y + 2, z)) || branchKeys.contains(key(x, y - 2, z));
    }

    private static boolean trapsRequiredCoverCell(Set<Long> occupiedWood,
                                                  Set<Long> branchKeys,
                                                  int nx, int ny, int nz) {
        for (int[] off : FACE_NEIGHBORS) {
            int ax = nx + off[0];
            int ay = ny + off[1];
            int az = nz + off[2];
            long ak = key(ax, ay, az);
            if (occupiedWood.contains(ak)) {
                continue;
            }
            boolean aboveBranch = branchKeys.contains(key(ax, ay - 1, az))
                    || (ax == nx && ay == ny + 1 && az == nz);
            if (aboveBranch && countNeighbors(occupiedWood, ax, ay, az) + 1 >= 4) {
                return true;
            }
        }
        return false;
    }

    /**
     * Walks a 6-connected face-adjacent path from {@code (x0,y0,z0)} to {@code (x1,y1,z1)}.
     * Advancing strictly one axis per step guarantees every consecutive voxel shares a 2D block face.
     */
    private static List<int[]> walk6Connected(int x0, int y0, int z0,
                                            int x1, int y1, int z1,
                                            boolean horizontalFirst) {
        int dx = Math.abs(x1 - x0);
        int dy = Math.abs(y1 - y0);
        int dz = Math.abs(z1 - z0);
        int sx = Integer.compare(x1, x0);
        int sy = Integer.compare(y1, y0);
        int sz = Integer.compare(z1, z0);

        int totalSteps = dx + dy + dz;
        List<int[]> path = new ArrayList<>(totalSteps + 1);
        int cx = x0;
        int cy = y0;
        int cz = z0;
        path.add(new int[]{cx, cy, cz});
        if (totalSteps == 0) {
            return path;
        }

        int rx = dx;
        int ry = dy;
        int rz = dz;
        double errX = rx > 0 ? 0.5D : 0.0D;
        double errY = ry > 0 ? 0.5D : 0.0D;
        double errZ = rz > 0 ? 0.5D : 0.0D;

        boolean firstStep = horizontalFirst && (rx > 0 || rz > 0);
        while (rx > 0 || ry > 0 || rz > 0) {
            char chosen;
            if (firstStep) {
                chosen = (rx >= rz) ? 'X' : 'Z';
                firstStep = false;
            } else {
                double bestScore = -1.0D;
                chosen = 'Y';
                if (rx > 0 && errX > bestScore) {
                    bestScore = errX;
                    chosen = 'X';
                }
                if (rz > 0 && errZ > bestScore) {
                    bestScore = errZ;
                    chosen = 'Z';
                }
                if (ry > 0 && errY > bestScore) {
                    chosen = 'Y';
                }
            }

            if (chosen == 'X' && rx > 0) {
                cx += sx;
                rx--;
                errX -= 1.0D;
            } else if (chosen == 'Z' && rz > 0) {
                cz += sz;
                rz--;
                errZ -= 1.0D;
            } else if (ry > 0) {
                cy += sy;
                ry--;
                errY -= 1.0D;
            }
            path.add(new int[]{cx, cy, cz});

            double stepInv = 1.0D / Math.max(1, totalSteps);
            errX += dx * stepInv;
            errY += dy * stepInv;
            errZ += dz * stepInv;
        }
        return path;
    }

    private static int enforceCanopyCover(Map<Long, BlockPlacement> placed,
                                          Set<Long> occupiedWood,
                                          Set<Long> trunkKeys,
                                          Set<Long> branchKeys,
                                          Set<Long> leafKeys,
                                          List<int[]> terminalTips,
                                          int[] trunkApex,
                                          PerformancePreset preset,
                                          int originY,
                                          int maxTotalBlocks,
                                          boolean isJungleParkour,
                                          int leaves) {
        // 1. Guarantee leaves directly above the trunk apex and its immediate ring
        ResourceId leafId = findLeafId(placed);
        if (leafId == null) {
            return leaves;
        }
        leaves = ensureLeaf(placed, occupiedWood, leafKeys, trunkApex[0], trunkApex[1] + 1, trunkApex[2],
                leafId, originY, maxTotalBlocks, leaves);
        for (Long tk : trunkKeys) {
            int ty = (int) ((tk >> 21) & 0x1FFFFFL);
            if ((ty & 0x100000) != 0) {
                ty |= ~0x1FFFFF;
            }
            if (ty == trunkApex[1]) {
                int tx = (int) ((tk >> 42) & 0x1FFFFFL);
                if ((tx & 0x100000) != 0) {
                    tx |= ~0x1FFFFF;
                }
                int tz = (int) (tk & 0x1FFFFFL);
                if ((tz & 0x100000) != 0) {
                    tz |= ~0x1FFFFF;
                }
                leaves = ensureLeaf(placed, occupiedWood, leafKeys, tx, ty + 1, tz,
                        leafId, originY, maxTotalBlocks, leaves);
            }
        }
        if (preset != PerformancePreset.PERFORMANCE) {
            for (int[] off : CARDINALS) {
                leaves = ensureLeaf(placed, occupiedWood, leafKeys,
                        trunkApex[0] + off[0], trunkApex[1] + 1, trunkApex[2] + off[2],
                        leafId, originY, maxTotalBlocks, leaves);
            }
        }

        // 2. Every terminal branch tip gets a guaranteed top cap + cardinal sleeve leaves
        for (int[] tip : terminalTips) {
            int tx = tip[0];
            int ty = tip[1];
            int tz = tip[2];
            leaves = ensureLeaf(placed, occupiedWood, leafKeys, tx, ty + 1, tz,
                    leafId, originY, maxTotalBlocks, leaves);
            for (int[] off : CARDINALS) {
                leaves = ensureLeaf(placed, occupiedWood, leafKeys, tx + off[0], ty, tz + off[2],
                        leafId, originY, maxTotalBlocks, leaves);
                if (preset == PerformancePreset.VISUAL) {
                    leaves = ensureLeaf(placed, occupiedWood, leafKeys, tx + off[0], ty + 1, tz + off[2],
                            leafId, originY, maxTotalBlocks, leaves);
                }
            }
        }

        // 3. Every branch block (and top of trunk) on a living tree must have wood or leaves directly above it (y + 1),
        // unless adding that leaf would be trapped underneath a parallel upper inner collar next to the trunk.
        List<BlockPlacement> currentBranches = new ArrayList<>();
        int maxTrunkY = Integer.MIN_VALUE;
        ResourceId fallbackBranchId = null;
        for (BlockPlacement block : placed.values()) {
            if (block.role() == PlacementRole.BRANCH) {
                currentBranches.add(block);
                if (fallbackBranchId == null) {
                    fallbackBranchId = block.block();
                }
            } else if (block.role() == PlacementRole.LOG) {
                if (block.y() > maxTrunkY) {
                    maxTrunkY = block.y();
                }
                if (fallbackBranchId == null) {
                    fallbackBranchId = block.block();
                }
            }
        }
        int mandatoryBudget = maxTotalBlocks + 96;
        for (BlockPlacement branch : currentBranches) {
            int bx = branch.x();
            int by = branch.y();
            int bz = branch.z();
            long aboveKey = key(bx, by + 1, bz);
            if (occupiedWood.contains(aboveKey) || leafKeys.contains(aboveKey)) {
                continue;
            }
            if (branchKeys.contains(key(bx, by + 2, bz)) && isAdjacentToTrunk(trunkKeys, bx, by + 2, bz)) {
                continue;
            }
            leaves = ensureLeaf(placed, occupiedWood, leafKeys, bx, by + 1, bz,
                    leafId, originY, mandatoryBudget, leaves);
        }
        for (BlockPlacement block : new ArrayList<>(placed.values())) {
            if (block.role() == PlacementRole.LOG && block.y() == maxTrunkY) {
                long aboveKey = key(block.x(), block.y() + 1, block.z());
                if (!occupiedWood.contains(aboveKey) && !leafKeys.contains(aboveKey)) {
                    leaves = ensureLeaf(placed, occupiedWood, leafKeys, block.x(), block.y() + 1, block.z(),
                            leafId, originY, mandatoryBudget, leaves);
                }
            }
        }

        // 4. Stabilize sparse branch caps before pruning so Step 5 does not cascade-delete expandable caps
        leaves = stabilizeAllSparseLeaves(placed, occupiedWood, trunkKeys, branchKeys, leafKeys,
                leafId, originY, mandatoryBudget, leaves);

        // 5. Prune dangling leaves or sparse leaves under inner trunk collars when not required as sole branch caps
        leaves = pruneLeafAnomalies(placed, occupiedWood, trunkKeys, branchKeys, leafKeys, leaves);

        // 6. Complete any remaining <2-neighbor wood-cap leaf and repair trapped chimneys / bare branch tops
        leaves = stabilizeAndSealCanopy(placed, occupiedWood, trunkKeys, branchKeys, leafKeys,
                leafId, fallbackBranchId, originY, mandatoryBudget, leaves, maxTrunkY);
        return leaves;
    }

    private static int pruneLeafAnomalies(Map<Long, BlockPlacement> placed,
                                          Set<Long> occupiedWood,
                                          Set<Long> trunkKeys,
                                          Set<Long> branchKeys,
                                          Set<Long> leafKeys,
                                          int leaves) {
        for (int pass = 0; pass < 8; pass++) {
            boolean removedAny = false;
            for (BlockPlacement block : new ArrayList<>(placed.values())) {
                if (block.role() != PlacementRole.LEAVES) {
                    continue;
                }
                int x = block.x();
                int y = block.y();
                int z = block.z();
                long lk = key(x, y, z);
                if (!leafKeys.contains(lk)) {
                    continue;
                }
                int neighbors = countNeighbors(leafKeys, x, y, z);
                boolean aboveIsLeaf = leafKeys.contains(key(x, y + 1, z));
                boolean aboveUnderInnerBranch = aboveIsLeaf
                        && branchKeys.contains(key(x, y + 2, z))
                        && isAdjacentToTrunk(trunkKeys, x, y + 2, z);
                int aboveNeededAfterRemove = aboveUnderInnerBranch ? 3 : 2;
                boolean safeAboveLeaf = aboveIsLeaf
                        && (countNeighbors(leafKeys, x, y + 1, z) - 1) >= aboveNeededAfterRemove;
                boolean safeCoveredAbove = occupiedWood.contains(key(x, y + 1, z)) || safeAboveLeaf;
                boolean requiredCap = occupiedWood.contains(key(x, y - 1, z)) && !safeCoveredAbove;
                boolean underInnerBranch = branchKeys.contains(key(x, y + 1, z))
                        && isAdjacentToTrunk(trunkKeys, x, y + 1, z);
                if ((!requiredCap && neighbors <= 1) || (!requiredCap && underInnerBranch && neighbors < 3)) {
                    placed.remove(lk);
                    leafKeys.remove(lk);
                    leaves = Math.max(0, leaves - 1);
                    removedAny = true;
                }
            }
            if (!removedAny) {
                break;
            }
        }
        return leaves;
    }

    private static int ensureQuadPad(Map<Long, BlockPlacement> placed,
                                     Set<Long> occupiedWood,
                                     Set<Long> trunkKeys,
                                     Set<Long> branchKeys,
                                     Set<Long> leafKeys,
                                     int x, int y, int z,
                                     ResourceId leafId,
                                     int originY,
                                     int maxTotalBlocks,
                                     int leaves) {
        int[][] corners = {{1, 1}, {1, -1}, {-1, 1}, {-1, -1}};
        for (int[] c : corners) {
            int nx = x + c[0];
            int nz = z + c[1];
            int[][] quad = {{nx, y, z}, {x, y, nz}, {nx, y, nz}};
            if (isValidLeafQuad(quad, originY, occupiedWood, trunkKeys, branchKeys, true)) {
                return placeLeafCells(quad, placed, occupiedWood, leafKeys, leafId, originY, maxTotalBlocks, leaves);
            }
        }
        for (int sy : new int[]{1, -1}) {
            for (int s : new int[]{1, -1}) {
                int[][] xyQuad = {{x + s, y, z}, {x, y + sy, z}, {x + s, y + sy, z}};
                if (isValidLeafQuad(xyQuad, originY, occupiedWood, trunkKeys, branchKeys, true)) {
                    return placeLeafCells(xyQuad, placed, occupiedWood, leafKeys, leafId, originY, maxTotalBlocks, leaves);
                }
                int[][] zyQuad = {{x, y, z + s}, {x, y + sy, z}, {x, y + sy, z + s}};
                if (isValidLeafQuad(zyQuad, originY, occupiedWood, trunkKeys, branchKeys, true)) {
                    return placeLeafCells(zyQuad, placed, occupiedWood, leafKeys, leafId, originY, maxTotalBlocks, leaves);
                }
            }
        }
        // Fallback: bridge (x,y,z) via up to 2 steps through open non-wood cells to existing leaves
        for (int[] off : FACE_NEIGHBORS) {
            if (countNeighbors(leafKeys, x, y, z) >= 2) {
                break;
            }
            int ax = x + off[0];
            int ay = y + off[1];
            int az = z + off[2];
            long ak = key(ax, ay, az);
            if (ay <= originY || occupiedWood.contains(ak) || leafKeys.contains(ak)) {
                continue;
            }
            if (branchKeys.contains(key(ax, ay + 1, az)) && isAdjacentToTrunk(trunkKeys, ax, ay + 1, az)) {
                continue;
            }
            if (countNeighbors(leafKeys, ax, ay, az) >= 2) {
                leaves = ensureLeaf(placed, occupiedWood, leafKeys, ax, ay, az,
                        leafId, originY, maxTotalBlocks, leaves);
                continue;
            }
            for (int[] off2 : FACE_NEIGHBORS) {
                int bx = ax + off2[0];
                int by = ay + off2[1];
                int bz = az + off2[2];
                long bk = key(bx, by, bz);
                if (bk == key(x, y, z) || by <= originY || occupiedWood.contains(bk) || leafKeys.contains(bk)) {
                    continue;
                }
                if (branchKeys.contains(key(bx, by + 1, bz)) && isAdjacentToTrunk(trunkKeys, bx, by + 1, bz)) {
                    continue;
                }
                if (countNeighbors(leafKeys, bx, by, bz) >= 1) {
                    leaves = ensureLeaf(placed, occupiedWood, leafKeys, ax, ay, az,
                            leafId, originY, maxTotalBlocks, leaves);
                    leaves = ensureLeaf(placed, occupiedWood, leafKeys, bx, by, bz,
                            leafId, originY, maxTotalBlocks, leaves);
                    break;
                }
            }
        }
        return leaves;
    }

    private static boolean isValidLeafQuad(int[][] cells,
                                           int originY,
                                           Set<Long> occupiedWood,
                                           Set<Long> trunkKeys,
                                           Set<Long> branchKeys,
                                           boolean checkUnderBranch) {
        for (int[] cell : cells) {
            if (cell[1] <= originY || occupiedWood.contains(key(cell[0], cell[1], cell[2]))) {
                return false;
            }
            if (checkUnderBranch
                    && branchKeys.contains(key(cell[0], cell[1] + 1, cell[2]))
                    && isAdjacentToTrunk(trunkKeys, cell[0], cell[1] + 1, cell[2])) {
                return false;
            }
        }
        return true;
    }

    private static int placeLeafCells(int[][] cells,
                                      Map<Long, BlockPlacement> placed,
                                      Set<Long> occupiedWood,
                                      Set<Long> leafKeys,
                                      ResourceId leafId,
                                      int originY,
                                      int maxTotalBlocks,
                                      int leaves) {
        for (int[] cell : cells) {
            leaves = ensureLeaf(placed, occupiedWood, leafKeys, cell[0], cell[1], cell[2],
                    leafId, originY, maxTotalBlocks, leaves);
        }
        return leaves;
    }

    private static int cleanUndersideAndSingletons(Map<Long, BlockPlacement> placed,
                                                   Set<Long> trunkKeys,
                                                   Set<Long> branchKeys,
                                                   Set<Long> leafKeys,
                                                   int originX, int originZ,
                                                   int lowestCrownY,
                                                   boolean isDead) {
        List<Long> toRemove = new ArrayList<>();
        for (BlockPlacement block : placed.values()) {
            if (block.role() != PlacementRole.LEAVES) {
                continue;
            }
            int x = block.x();
            int y = block.y();
            int z = block.z();
            long k = key(x, y, z);

            // Never leave an isolated 1-block leaf floating or dangling under a stub
            int leafNeighbors = countNeighbors(leafKeys, x, y, z);
            if (!isDead && leafNeighbors <= 1) {
                toRemove.add(k);
                continue;
            }

            // Remove leaves hanging directly underneath inner branch blocks adjacent to the trunk
            long aboveKey = key(x, y + 1, z);
            if (branchKeys.contains(aboveKey) && isAdjacentToTrunk(trunkKeys, x, y + 1, z)) {
                if (leafNeighbors <= 2) {
                    toRemove.add(k);
                    continue;
                }
            }

            // Keep lower trunk clear of random stray leaves on tall trunks
            if (y < lowestCrownY && isAdjacentToTrunk(trunkKeys, x, y, z) && leafNeighbors <= 2) {
                toRemove.add(k);
            }
        }

        for (Long k : toRemove) {
            placed.remove(k);
            leafKeys.remove(k);
        }
        int remainingLeaves = 0;
        for (BlockPlacement block : placed.values()) {
            if (block.role() == PlacementRole.LEAVES) {
                remainingLeaves++;
            }
        }
        return remainingLeaves;
    }

    /**
     * Stabilizes sparse leaves (expanding into open 2x2 horizontal pads or short open-air bridges).
     */
    private static int stabilizeAllSparseLeaves(Map<Long, BlockPlacement> placed,
                                                Set<Long> occupiedWood,
                                                Set<Long> trunkKeys,
                                                Set<Long> branchKeys,
                                                Set<Long> leafKeys,
                                                ResourceId leafId,
                                                int originY,
                                                int maxTotalBlocks,
                                                int leaves) {
        for (int pass = 0; pass < 3; pass++) {
            boolean changed = false;
            for (BlockPlacement block : new ArrayList<>(placed.values())) {
                if (block.role() != PlacementRole.LEAVES) {
                    continue;
                }
                int x = block.x();
                int y = block.y();
                int z = block.z();
                int minRequired = minRequiredNeighbors(trunkKeys, branchKeys, x, y, z);
                if (countNeighbors(leafKeys, x, y, z) < minRequired) {
                    int before = leaves;
                    leaves = stabilizeSingleLeaf(placed, occupiedWood, trunkKeys, branchKeys, leafKeys,
                            x, y, z, minRequired, leafId, originY, maxTotalBlocks, leaves);
                    if (leaves != before) {
                        changed = true;
                    }
                }
            }
            if (!changed) {
                break;
            }
        }
        return leaves;
    }

    /**
     * Iteratively guarantees every branch top (and trunk top) is covered and no sparse or trapped-singleton leaves remain.
     */
    private static int stabilizeAndSealCanopy(Map<Long, BlockPlacement> placed,
                                              Set<Long> occupiedWood,
                                              Set<Long> trunkKeys,
                                              Set<Long> branchKeys,
                                              Set<Long> leafKeys,
                                              ResourceId leafId,
                                              ResourceId branchId,
                                              int originY,
                                              int maxTotalBlocks,
                                              int leaves,
                                              int maxTrunkY) {
        for (int pass = 0; pass < 12; pass++) {
            boolean changed = false;

            // 1. Ensure every living branch (and top of trunk) is capped at y+1 or y+2
            for (BlockPlacement block : new ArrayList<>(placed.values())) {
                boolean isBranch = block.role() == PlacementRole.BRANCH;
                boolean isTrunkTop = block.role() == PlacementRole.LOG && block.y() == maxTrunkY;
                if (!isBranch && !isTrunkTop) {
                    continue;
                }
                int bx = block.x();
                int by = block.y();
                int bz = block.z();
                if (isOccupied(occupiedWood, leafKeys, bx, by + 1, bz)
                        || isOccupied(occupiedWood, leafKeys, bx, by + 2, bz)) {
                    continue;
                }
                changed = true;
                if (!isUnderInnerBranch(trunkKeys, branchKeys, bx, by + 1, bz)) {
                    leaves = ensureLeaf(placed, occupiedWood, leafKeys, bx, by + 1, bz,
                            leafId, originY, maxTotalBlocks, leaves);
                    leaves = stabilizeSingleLeaf(placed, occupiedWood, trunkKeys, branchKeys, leafKeys,
                            bx, by + 1, bz, 2, leafId, originY, maxTotalBlocks, leaves);
                }
                if (leafKeys.contains(key(bx, by + 1, bz))
                        && countNeighbors(leafKeys, bx, by + 1, bz) >= minRequiredNeighbors(trunkKeys, branchKeys, bx, by + 1, bz)) {
                    continue;
                }
                // Try placing/stabilizing at y+2 instead (or bridge out if trapped inside a stacked wood collar)
                if (!occupiedWood.contains(key(bx, by + 2, bz))) {
                    leaves = ensureLeaf(placed, occupiedWood, leafKeys, bx, by + 2, bz,
                            leafId, originY, maxTotalBlocks, leaves);
                    int minY2 = minRequiredNeighbors(trunkKeys, branchKeys, bx, by + 2, bz);
                    leaves = stabilizeSingleLeaf(placed, occupiedWood, trunkKeys, branchKeys, leafKeys,
                            bx, by + 2, bz, minY2, leafId, originY, maxTotalBlocks, leaves);
                    if (leafKeys.contains(key(bx, by + 2, bz))
                            && countNeighbors(leafKeys, bx, by + 2, bz) >= minY2) {
                        continue;
                    }
                }
                if (isBranch) {
                    if (countNeighbors(occupiedWood, bx, by, bz) == 1) {
                        long bk = key(bx, by, bz);
                        branchKeys.remove(bk);
                        occupiedWood.remove(bk);
                        placed.remove(bk);
                        leaves = ensureLeaf(placed, occupiedWood, leafKeys, bx, by, bz,
                                leafId, originY, maxTotalBlocks, leaves);
                        leaves = stabilizeSingleLeaf(placed, occupiedWood, trunkKeys, branchKeys, leafKeys,
                                bx, by, bz, 2, leafId, originY, maxTotalBlocks, leaves);
                    } else if (!occupiedWood.contains(key(bx, by + 1, bz))) {
                        leaves = ensureLeaf(placed, occupiedWood, leafKeys, bx, by + 1, bz,
                                leafId, originY, maxTotalBlocks, leaves);
                        for (int[] off : CARDINALS) {
                            if (!isOccupied(occupiedWood, leafKeys, bx + off[0], by + 1, bz + off[2])) {
                                leaves = ensureLeaf(placed, occupiedWood, leafKeys,
                                        bx + off[0], by + 1, bz + off[2], leafId, originY, maxTotalBlocks, leaves);
                                leaves = stabilizeSingleLeaf(placed, occupiedWood, trunkKeys, branchKeys, leafKeys,
                                        bx + off[0], by + 1, bz + off[2], 2, leafId, originY, maxTotalBlocks, leaves);
                                break;
                            }
                        }
                        leaves = stabilizeSingleLeaf(placed, occupiedWood, trunkKeys, branchKeys, leafKeys,
                                bx, by + 1, bz, 2, leafId, originY, maxTotalBlocks, leaves);
                    }
                }
            }

            // 2. Stabilize or safely resolve any remaining leaf with < minRequired neighbors
            leaves = stabilizeAllSparseLeaves(placed, occupiedWood, trunkKeys, branchKeys, leafKeys,
                    leafId, originY, maxTotalBlocks, leaves);

            for (BlockPlacement block : new ArrayList<>(placed.values())) {
                if (block.role() != PlacementRole.LEAVES) {
                    continue;
                }
                int x = block.x();
                int y = block.y();
                int z = block.z();
                int minRequired = minRequiredNeighbors(trunkKeys, branchKeys, x, y, z);
                if (countNeighbors(leafKeys, x, y, z) >= minRequired) {
                    continue;
                }

                // If directly above required wood and y+1 is a leaf, try strengthening y+1 (without counting this leaf)
                boolean requiredWoodBase = isRequiredWoodBase(trunkKeys, branchKeys, x, y - 1, z, maxTrunkY);
                if (requiredWoodBase && leafKeys.contains(key(x, y + 1, z))) {
                    int y1Min = minRequiredNeighbors(trunkKeys, branchKeys, x, y + 1, z);
                    leaves = stabilizeSingleLeaf(placed, occupiedWood, trunkKeys, branchKeys, leafKeys,
                            x, y + 1, z, y1Min + 1, leafId, originY, maxTotalBlocks, leaves);
                }

                if (canSafelyRemoveLeaf(trunkKeys, branchKeys, occupiedWood, leafKeys, x, y, z, maxTrunkY)) {
                    leaves = removeLeafAt(placed, leafKeys, x, y, z, leaves);
                    changed = true;
                } else {
                    List<int[]> leafCandidates = new ArrayList<>();
                    for (int[] off : new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0, 1, 0}}) {
                        int nx = x + off[0];
                        int ny = y + off[1];
                        int nz = z + off[2];
                        long nk = key(nx, ny, nz);
                        if (ny > originY && !occupiedWood.contains(nk) && !leafKeys.contains(nk)
                                && !isUnderInnerBranch(trunkKeys, branchKeys, nx, ny, nz)) {
                            leafCandidates.add(new int[]{nx, ny, nz, countNeighbors(leafKeys, nx, ny, nz)});
                        }
                    }
                    leafCandidates.sort((a, b) -> Integer.compare(b[3], a[3]));
                    for (int[] cand : leafCandidates) {
                        if (countNeighbors(leafKeys, x, y, z) >= minRequired) {
                            break;
                        }
                        int prevLeaves = leaves;
                        leaves = tryAddAndStabilizeLeaf(placed, occupiedWood, trunkKeys, branchKeys, leafKeys,
                                cand[0], cand[1], cand[2], leafId, originY, maxTotalBlocks, maxTrunkY, leaves);
                        if (leaves != prevLeaves) {
                            changed = true;
                        }
                    }

                    if (countNeighbors(leafKeys, x, y, z) < minRequired) {
                        for (int[] off : new int[][]{{0, 1, 0}, {1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}}) {
                            if (countNeighbors(leafKeys, x, y, z) >= minRequired) {
                                break;
                            }
                            int nx = x + off[0];
                            int ny = y + off[1];
                            int nz = z + off[2];
                            int prevLeaves = leaves;
                            leaves = tryAddAndStabilizeLeaf(placed, occupiedWood, trunkKeys, branchKeys, leafKeys,
                                    nx, ny, nz, leafId, originY, maxTotalBlocks, maxTrunkY, leaves);
                            if (leaves != prevLeaves) {
                                changed = true;
                            }
                        }
                    }

                    boolean removedTrappedPocket = false;
                    if (countNeighbors(leafKeys, x, y, z) < 2 && requiredWoodBase) {
                        if (!isOccupied(occupiedWood, leafKeys, x, y + 1, z)) {
                            int before = leaves;
                            leaves = ensureLeaf(placed, occupiedWood, leafKeys, x, y + 1, z,
                                    leafId, originY, maxTotalBlocks, leaves);
                            if (leaves > before) {
                                leaves = stabilizeSingleLeaf(placed, occupiedWood, trunkKeys, branchKeys, leafKeys,
                                        x, y + 1, z, 3, leafId, originY, maxTotalBlocks, leaves);
                                changed = true;
                            }
                        }
                        if (leafKeys.contains(key(x, y + 1, z))) {
                            int neededAbove = minRequiredNeighbors(trunkKeys, branchKeys, x, y + 1, z) + 1;
                            for (int[] off : new int[][]{{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0, 1, 0}}) {
                                if (countNeighbors(leafKeys, x, y + 1, z) >= neededAbove) {
                                    break;
                                }
                                int ax = x + off[0];
                                int ay = y + 1 + off[1];
                                int az = z + off[2];
                                int prevLeaves = leaves;
                                leaves = tryAddAndStabilizeLeaf(placed, occupiedWood, trunkKeys, branchKeys, leafKeys,
                                        ax, ay, az, leafId, originY, maxTotalBlocks, maxTrunkY, leaves);
                                if (leaves != prevLeaves) {
                                    changed = true;
                                }
                            }
                        }
                        if (canSafelyRemoveLeaf(trunkKeys, branchKeys, occupiedWood, leafKeys, x, y, z, maxTrunkY)
                                || (!isUnderInnerBranch(trunkKeys, branchKeys, x, y + 1, z)
                                        && countNeighbors(leafKeys, x, y, z) < 2
                                        && isOccupied(occupiedWood, leafKeys, x, y + 1, z)
                                        && (!leafKeys.contains(key(x, y + 1, z))
                                                || countNeighbors(leafKeys, x, y + 1, z) - 1 >= 2))) {
                            leaves = removeLeafAt(placed, leafKeys, x, y, z, leaves);
                            changed = true;
                            removedTrappedPocket = true;
                        }
                    }

                    if (!removedTrappedPocket
                            && countNeighbors(leafKeys, x, y, z) < minRequired
                            && branchKeys.contains(key(x, y - 1, z))
                            && countNeighbors(occupiedWood, x, y - 1, z) == 1
                            && !isOccupied(occupiedWood, leafKeys, x, y + 1, z)) {
                        leaves = removeLeafAt(placed, leafKeys, x, y, z, leaves);
                        long tipKey = key(x, y - 1, z);
                        branchKeys.remove(tipKey);
                        occupiedWood.remove(tipKey);
                        placed.remove(tipKey);
                        changed = true;
                    }
                }
            }

            if (!changed) {
                break;
            }
        }
        return leaves;
    }

    private static int tryAddAndStabilizeLeaf(Map<Long, BlockPlacement> placed,
                                              Set<Long> occupiedWood,
                                              Set<Long> trunkKeys,
                                              Set<Long> branchKeys,
                                              Set<Long> leafKeys,
                                              int x, int y, int z,
                                              ResourceId leafId,
                                              int originY,
                                              int maxTotalBlocks,
                                              int maxTrunkY,
                                              int leaves) {
        if (y <= originY || occupiedWood.contains(key(x, y, z)) || leafKeys.contains(key(x, y, z))) {
            return leaves;
        }
        int before = leaves;
        leaves = ensureLeaf(placed, occupiedWood, leafKeys, x, y, z, leafId, originY, maxTotalBlocks, leaves);
        if (leaves == before) {
            return leaves;
        }
        int minReq = minRequiredNeighbors(trunkKeys, branchKeys, x, y, z);
        leaves = stabilizeSingleLeaf(placed, occupiedWood, trunkKeys, branchKeys, leafKeys,
                x, y, z, minReq, leafId, originY, maxTotalBlocks, leaves);
        if (leafKeys.contains(key(x, y, z))
                && countNeighbors(leafKeys, x, y, z) < minReq
                && canSafelyRemoveLeaf(trunkKeys, branchKeys, occupiedWood, leafKeys, x, y, z, maxTrunkY)) {
            leaves = removeLeafAt(placed, leafKeys, x, y, z, leaves);
        }
        return leaves;
    }

    private static int stabilizeSingleLeaf(Map<Long, BlockPlacement> placed,
                                           Set<Long> occupiedWood,
                                           Set<Long> trunkKeys,
                                           Set<Long> branchKeys,
                                           Set<Long> leafKeys,
                                           int x, int y, int z,
                                           int targetNeighbors,
                                           ResourceId leafId,
                                           int originY,
                                           int maxTotalBlocks,
                                           int leaves) {
        if (!leafKeys.contains(key(x, y, z))) {
            return leaves;
        }
        if (!isUnderInnerBranch(trunkKeys, branchKeys, x, y, z) && countNeighbors(leafKeys, x, y, z) < targetNeighbors) {
            int before = leaves;
            leaves = ensureQuadPad(placed, occupiedWood, trunkKeys, branchKeys, leafKeys,
                    x, y, z, leafId, originY, maxTotalBlocks, leaves);
            if (leaves != before && countNeighbors(leafKeys, x, y, z) >= targetNeighbors) {
                return leaves;
            }
        }
        int attempts = 0;
        while (countNeighbors(leafKeys, x, y, z) < targetNeighbors && attempts++ < 3) {
            int before = leaves;
            leaves = bridgeLeafToCluster(placed, occupiedWood, trunkKeys, branchKeys, leafKeys,
                    x, y, z, leafId, originY, maxTotalBlocks, leaves);
            if (leaves == before) {
                break;
            }
        }
        return leaves;
    }

    private record BfsNode(int x, int y, int z, int depth, int parentIdx) {}

    private static int bridgeLeafToCluster(Map<Long, BlockPlacement> placed,
                                           Set<Long> occupiedWood,
                                           Set<Long> trunkKeys,
                                           Set<Long> branchKeys,
                                           Set<Long> leafKeys,
                                           int startX, int startY, int startZ,
                                           ResourceId leafId,
                                           int originY,
                                           int maxTotalBlocks,
                                           int leaves) {
        List<BfsNode> nodes = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        visited.add(key(startX, startY, startZ));

        // Prefer horizontal steps before vertical so pads grow laterally first
        int[][] stepOrder = {{1, 0, 0}, {-1, 0, 0}, {0, 0, 1}, {0, 0, -1}, {0, 1, 0}, {0, -1, 0}};
        int head = 0;
        for (int[] off : stepOrder) {
            int nx = startX + off[0];
            int ny = startY + off[1];
            int nz = startZ + off[2];
            long nk = key(nx, ny, nz);
            if (ny > originY && !occupiedWood.contains(nk) && !leafKeys.contains(nk)
                    && !isUnderInnerBranch(trunkKeys, branchKeys, nx, ny, nz) && visited.add(nk)) {
                nodes.add(new BfsNode(nx, ny, nz, 1, -1));
            }
        }

        while (head < nodes.size()) {
            int curIdx = head++;
            BfsNode cur = nodes.get(curIdx);

            // Goal A: Placing a valid 2x2 horizontal pad containing cur stabilizes the entire bridge
            int[][] quads = findFirstValidQuadFor(cur.x(), cur.y(), cur.z(), originY, occupiedWood, trunkKeys, branchKeys);
            if (quads != null) {
                leaves = placeBridgePath(nodes, curIdx, placed, occupiedWood, leafKeys, leafId, originY, maxTotalBlocks, leaves);
                return placeLeafCells(quads, placed, occupiedWood, leafKeys, leafId, originY, maxTotalBlocks, leaves);
            }

            // Goal B: Connecting directly to a stable existing leaf (non-singleton, excluding start)
            boolean touchesStableLeaf = false;
            for (int[] off : FACE_NEIGHBORS) {
                int lx = cur.x() + off[0];
                int ly = cur.y() + off[1];
                int lz = cur.z() + off[2];
                if (lx == startX && ly == startY && lz == startZ) {
                    continue;
                }
                if (leafKeys.contains(key(lx, ly, lz))
                        && countNeighbors(leafKeys, lx, ly, lz) >= minRequiredNeighbors(trunkKeys, branchKeys, lx, ly, lz)) {
                    touchesStableLeaf = true;
                    break;
                }
            }
            if (touchesStableLeaf) {
                return placeBridgePath(nodes, curIdx, placed, occupiedWood, leafKeys, leafId, originY, maxTotalBlocks, leaves);
            }

            if (cur.depth() < 3) {
                for (int[] off : stepOrder) {
                    int nx = cur.x() + off[0];
                    int ny = cur.y() + off[1];
                    int nz = cur.z() + off[2];
                    long nk = key(nx, ny, nz);
                    if (ny > originY && !occupiedWood.contains(nk) && !leafKeys.contains(nk)
                            && !isUnderInnerBranch(trunkKeys, branchKeys, nx, ny, nz) && visited.add(nk)) {
                        nodes.add(new BfsNode(nx, ny, nz, cur.depth() + 1, curIdx));
                    }
                }
            }
        }
        return leaves;
    }

    private static int placeBridgePath(List<BfsNode> nodes,
                                       int idx,
                                       Map<Long, BlockPlacement> placed,
                                       Set<Long> occupiedWood,
                                       Set<Long> leafKeys,
                                       ResourceId leafId,
                                       int originY,
                                       int maxTotalBlocks,
                                       int leaves) {
        for (int cursor = idx; cursor >= 0; cursor = nodes.get(cursor).parentIdx()) {
            BfsNode node = nodes.get(cursor);
            leaves = ensureLeaf(placed, occupiedWood, leafKeys, node.x(), node.y(), node.z(),
                    leafId, originY, maxTotalBlocks, leaves);
        }
        return leaves;
    }

    private static int[][] findFirstValidQuadFor(int cx, int cy, int cz,
                                                 int originY,
                                                 Set<Long> occupiedWood,
                                                 Set<Long> trunkKeys,
                                                 Set<Long> branchKeys) {
        for (int dx : new int[]{0, -1}) {
            for (int dz : new int[]{0, -1}) {
                int x0 = cx + dx;
                int z0 = cz + dz;
                int[][] cells = {
                        {x0, cy, z0},
                        {x0 + 1, cy, z0},
                        {x0, cy, z0 + 1},
                        {x0 + 1, cy, z0 + 1}
                };
                if (isValidLeafQuad(cells, originY, occupiedWood, trunkKeys, branchKeys, true)) {
                    return cells;
                }
            }
        }
        return null;
    }

    private static boolean isOccupied(Set<Long> occupiedWood, Set<Long> leafKeys, int x, int y, int z) {
        long k = key(x, y, z);
        return occupiedWood.contains(k) || leafKeys.contains(k);
    }

    private static boolean isCardinallyAdjacentToTrunkAtY(Set<Long> trunkKeys, int x, int y, int z) {
        for (int[] off : CARDINALS) {
            if (trunkKeys.contains(key(x + off[0], y, z + off[2]))) {
                return true;
            }
        }
        return false;
    }

    private static boolean isUnderInnerBranch(Set<Long> trunkKeys, Set<Long> branchKeys, int x, int y, int z) {
        return branchKeys.contains(key(x, y + 1, z)) && isCardinallyAdjacentToTrunkAtY(trunkKeys, x, y + 1, z);
    }

    private static int minRequiredNeighbors(Set<Long> trunkKeys, Set<Long> branchKeys, int x, int y, int z) {
        return isUnderInnerBranch(trunkKeys, branchKeys, x, y, z) ? 3 : 2;
    }

    private static boolean isRequiredWoodBase(Set<Long> trunkKeys, Set<Long> branchKeys,
                                              int x, int y, int z, int maxTrunkY) {
        long k = key(x, y, z);
        return branchKeys.contains(k) || (trunkKeys.contains(k) && y == maxTrunkY);
    }

    private static boolean canSafelyRemoveLeaf(Set<Long> trunkKeys,
                                               Set<Long> branchKeys,
                                               Set<Long> occupiedWood,
                                               Set<Long> leafKeys,
                                               int x, int y, int z,
                                               int maxTrunkY) {
        if (isRequiredWoodBase(trunkKeys, branchKeys, x, y - 1, z, maxTrunkY)) {
            if (!isOccupied(occupiedWood, leafKeys, x, y + 1, z)) {
                return false;
            }
            if (!occupiedWood.contains(key(x, y + 1, z))
                    && countNeighbors(leafKeys, x, y + 1, z) - 1 < minRequiredNeighbors(trunkKeys, branchKeys, x, y + 1, z)) {
                return false;
            }
        }
        return true;
    }

    private static int removeLeafAt(Map<Long, BlockPlacement> placed,
                                    Set<Long> leafKeys,
                                    int x, int y, int z,
                                    int leaves) {
        long k = key(x, y, z);
        if (leafKeys.remove(k)) {
            placed.remove(k);
            return Math.max(0, leaves - 1);
        }
        return leaves;
    }

    private static int ensureLeaf(Map<Long, BlockPlacement> placed,
                                  Set<Long> occupiedWood,
                                  Set<Long> leafKeys,
                                  int x, int y, int z,
                                  ResourceId leafId,
                                  int originY,
                                  int maxTotalBlocks,
                                  int leaves) {
        if (y <= originY || placed.size() >= maxTotalBlocks) {
            return leaves;
        }
        long k = key(x, y, z);
        if (occupiedWood.contains(k) || leafKeys.contains(k)) {
            return leaves;
        }
        leafKeys.add(k);
        placed.put(k, new BlockPlacement(x, y, z, leafId, PlacementRole.LEAVES));
        return leaves + 1;
    }

    private static ResourceId findLeafId(Map<Long, BlockPlacement> placed) {
        for (BlockPlacement b : placed.values()) {
            if (b.role() == PlacementRole.LEAVES) {
                return b.block();
            }
        }
        return null;
    }

    private static boolean hasLeafNeighbor(Set<Long> leafKeys, int x, int y, int z) {
        return countNeighbors(leafKeys, x, y, z) > 0;
    }

    private static int countNeighbors(Set<Long> keys, int x, int y, int z) {
        int count = 0;
        for (int[] off : FACE_NEIGHBORS) {
            if (keys.contains(key(x + off[0], y + off[1], z + off[2]))) {
                count++;
            }
        }
        return count;
    }

    private static boolean isAdjacentToTrunk(Set<Long> trunkKeys, int x, int y, int z) {
        for (int[] off : CARDINALS) {
            if (trunkKeys.contains(key(x + off[0], y, z + off[2]))
                    || trunkKeys.contains(key(x + off[0], y + 1, z + off[2]))
                    || trunkKeys.contains(key(x + off[0], y - 1, z + off[2]))) {
                return true;
            }
        }
        return false;
    }

    private static int computeLowestCrownY(Set<Long> branchKeys, int fallbackMin, int trunkTopY) {
        if (branchKeys.isEmpty()) {
            return Math.max(fallbackMin, trunkTopY - 2);
        }
        int minY = Integer.MAX_VALUE;
        for (Long k : branchKeys) {
            int y = (int) ((k >> 21) & 0x1FFFFFL);
            // Sign-extend 21 bits
            if ((y & 0x100000) != 0) {
                y |= ~0x1FFFFF;
            }
            if (y < minY) {
                minY = y;
            }
        }
        return minY == Integer.MAX_VALUE ? fallbackMin : minY;
    }

    private static double voxelHash(int x, int y, int z) {
        return (SeedMixer.mix(x * 341873128712L + y * 132897987541L + z * 42317861L) >>> 11) * 0x1.0p-53;
    }

    private static boolean isDeadSpecies(SpeciesProfile profile) {
        String path = profile.id().path().toLowerCase(Locale.ROOT);
        return path.equals("dead") || path.equals("dead_pine");
    }

    private static boolean isFlatUndersideArchetype(SpeciesProfile profile) {
        TreeArchetype arch = profile.archetype();
        return arch == TreeArchetype.ACACIA
                || arch == TreeArchetype.PALM
                || arch == TreeArchetype.CONIFER
                || arch == TreeArchetype.COLUMNAR
                || arch == TreeArchetype.CYPRESS
                || profile.id().path().contains("pine");
    }

    private static boolean isJungleSpecies(SpeciesProfile profile) {
        String path = profile.id().path().toLowerCase(Locale.ROOT);
        return path.contains("jungle") || path.contains("kapok") || path.contains("eucalyptus");
    }

    private static boolean isPalmSpecies(SpeciesProfile profile) {
        String path = profile.id().path().toLowerCase(Locale.ROOT);
        return profile.archetype() == TreeArchetype.PALM
                || path.contains("palm") || path.contains("coconut") || path.contains("banana");
    }

    private static int exposedFaces(List<BlockPlacement> blocks) {
        Set<Long> occupied = new HashSet<>(blocks.size() * 2);
        for (BlockPlacement block : blocks) {
            occupied.add(key(block.x(), block.y(), block.z()));
        }
        int faces = 0;
        for (BlockPlacement block : blocks) {
            int x = block.x();
            int y = block.y();
            int z = block.z();
            if (!occupied.contains(key(x + 1, y, z))) faces++;
            if (!occupied.contains(key(x - 1, y, z))) faces++;
            if (!occupied.contains(key(x, y + 1, z))) faces++;
            if (!occupied.contains(key(x, y - 1, z))) faces++;
            if (!occupied.contains(key(x, y, z + 1))) faces++;
            if (!occupied.contains(key(x, y, z - 1))) faces++;
        }
        return faces;
    }

    private static long key(int x, int y, int z) {
        return ((long) (x & 0x1FFFFF) << 42) | ((long) (y & 0x1FFFFF) << 21) | (z & 0x1FFFFF);
    }
}
