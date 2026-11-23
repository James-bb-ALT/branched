package dev.branched.morphology;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Rotation-, translation- and scale-invariant signatures of a rasterized tree.
 *
 * <p>Phase 5 measures how often two trees of the same species collapse to the
 * same signature. A low duplicate rate is evidence that the generator varies
 * topology, not just a rotation or a height multiplier.</p>
 *
 * <ul>
 *   <li>{@link #exact} — translation only: literal voxel layout.</li>
 *   <li>{@link #rotational} — translation + four 90° Y-rotations.</li>
 *   <li>{@link #scaled} — translation + rotation + per-axis 10-bin
 *       normalization, i.e. the "same silhouette at a different size" test.</li>
 * </ul>
 */
public final class CanonicalShape {
    /** Bins per axis used by {@link #scaled}. */
    public static final int SCALE_BINS = 10;

    private CanonicalShape() {
    }

    public static String exact(List<BlockPlacement> blocks) {
        return serialize(normalizeTranslation(cells(blocks)));
    }

    public static String rotational(List<BlockPlacement> blocks) {
        List<Cell> cells = cells(blocks);
        String best = null;
        for (int rotation = 0; rotation < 4; rotation++) {
            String candidate = serialize(normalizeTranslation(rotate(cells, rotation)));
            if (best == null || candidate.compareTo(best) < 0) {
                best = candidate;
            }
        }
        return best == null ? "" : best;
    }

    public static String scaled(List<BlockPlacement> blocks) {
        List<Cell> cells = cells(blocks);
        String best = null;
        for (int rotation = 0; rotation < 4; rotation++) {
            String candidate = serialize(scale(rotate(cells, rotation)));
            if (best == null || candidate.compareTo(best) < 0) {
                best = candidate;
            }
        }
        return best == null ? "" : best;
    }

    private static List<Cell> cells(List<BlockPlacement> blocks) {
        List<Cell> cells = new ArrayList<>(blocks.size());
        for (BlockPlacement block : blocks) {
            cells.add(new Cell(block.x(), block.y(), block.z(), role(block.role())));
        }
        return cells;
    }

    private static char role(PlacementRole role) {
        return switch (role) {
            case LOG -> 'L';
            case BRANCH -> 'B';
            case LEAVES -> 'F';
            default -> 'X';
        };
    }

    private static List<Cell> rotate(List<Cell> cells, int quarterTurns) {
        List<Cell> rotated = new ArrayList<>(cells.size());
        for (Cell cell : cells) {
            int x = cell.x();
            int z = cell.z();
            for (int turn = 0; turn < quarterTurns; turn++) {
                int next = x;
                x = z;
                z = -next;
            }
            rotated.add(new Cell(x, cell.y(), z, cell.role()));
        }
        return rotated;
    }

    private static List<Cell> normalizeTranslation(List<Cell> cells) {
        if (cells.isEmpty()) {
            return List.of();
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        for (Cell cell : cells) {
            minX = Math.min(minX, cell.x());
            minY = Math.min(minY, cell.y());
            minZ = Math.min(minZ, cell.z());
        }
        List<Cell> normalized = new ArrayList<>(cells.size());
        for (Cell cell : cells) {
            normalized.add(new Cell(cell.x() - minX, cell.y() - minY, cell.z() - minZ, cell.role()));
        }
        return normalized;
    }

    private static List<Cell> scale(List<Cell> cells) {
        if (cells.isEmpty()) {
            return List.of();
        }
        int minX = Integer.MAX_VALUE;
        int minY = Integer.MAX_VALUE;
        int minZ = Integer.MAX_VALUE;
        int maxX = Integer.MIN_VALUE;
        int maxY = Integer.MIN_VALUE;
        int maxZ = Integer.MIN_VALUE;
        for (Cell cell : cells) {
            minX = Math.min(minX, cell.x());
            minY = Math.min(minY, cell.y());
            minZ = Math.min(minZ, cell.z());
            maxX = Math.max(maxX, cell.x());
            maxY = Math.max(maxY, cell.y());
            maxZ = Math.max(maxZ, cell.z());
        }
        List<Cell> scaled = new ArrayList<>(cells.size());
        for (Cell cell : cells) {
            scaled.add(new Cell(
                    bin(cell.x(), minX, maxX),
                    bin(cell.y(), minY, maxY),
                    bin(cell.z(), minZ, maxZ),
                    cell.role()));
        }
        return scaled;
    }

    private static int bin(int value, int min, int max) {
        int span = max - min + 1;
        int index = (int) Math.floor((value - min) * (double) SCALE_BINS / (double) span);
        return Math.max(0, Math.min(SCALE_BINS - 1, index));
    }

    private static String serialize(List<Cell> cells) {
        List<Cell> sorted = new ArrayList<>(cells);
        Collections.sort(sorted);
        StringBuilder sb = new StringBuilder(sorted.size() * 12);
        for (Cell cell : sorted) {
            sb.append(cell.x()).append(',').append(cell.y()).append(',').append(cell.z())
                    .append(cell.role()).append(';');
        }
        return sb.toString();
    }

    private record Cell(int x, int y, int z, char role) implements Comparable<Cell> {
        @Override
        public int compareTo(Cell other) {
            int byX = Integer.compare(x, other.x);
            if (byX != 0) {
                return byX;
            }
            int byY = Integer.compare(y, other.y);
            if (byY != 0) {
                return byY;
            }
            int byZ = Integer.compare(z, other.z);
            if (byZ != 0) {
                return byZ;
            }
            return Character.compare(role, other.role);
        }
    }
}
