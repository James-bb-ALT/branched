package dev.branched;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Source-level proof of the Phase 1 gate: a static tree owns no BlockEntity,
 * schedules no tick and never reads the chunk-generation RNG.
 *
 * <p>Comments and javadoc are stripped before matching, so documentation that
 * explains the prohibition does not trip the checks.</p>
 */
class StaticInvariantsTest {
    private static final Path SOURCES = Path.of("src/main/java");
    private static final Pattern BLOCK_COMMENT = Pattern.compile("(?s)/\\*.*?\\*/");
    private static final Pattern LINE_COMMENT = Pattern.compile("//[^\\n]*");
    private static final Pattern RANDOM_TICK = Pattern.compile("\\brandomTick\\s*\\(");
    private static final Pattern SET_BLOCK_ENTITY = Pattern.compile("\\bsetBlockEntity\\s*\\(");
    private static final Pattern WORLD_RNG = Pattern.compile(
            "\\b(context|ctx)\\.random\\s*\\(\\s*\\)");

    @Test
    void noMainSourceDeclaresABlockEntityOrRandomTick() throws IOException {
        for (Path source : mainSources()) {
            String code = stripComments(Files.readString(source));
            assertFalse(code.contains("extends BlockEntity"),
                    "BlockEntity subclass in " + source);
            assertFalse(code.contains("BlockEntityType"),
                    "BlockEntityType usage in " + source);
            assertFalse(code.contains("new BlockEntity"),
                    "BlockEntity construction in " + source);
            assertFalse(code.contains("net.minecraft.world.level.block.entity"),
                    "BlockEntity import in " + source);
            assertFalse(RANDOM_TICK.matcher(code).find(),
                    "randomTick override in " + source);
            assertFalse(code.contains("scheduleTick"),
                    "scheduled tick in " + source);
            assertFalse(SET_BLOCK_ENTITY.matcher(code).find(),
                    "setBlockEntity in " + source);
        }
    }

    @Test
    void treeGenerationNeverConsumesTheWorldRng() throws IOException {
        for (Path source : mainSources()) {
            String code = stripComments(Files.readString(source));
            assertFalse(WORLD_RNG.matcher(code).find(),
                    "world RNG use in " + source + "; determinism requires our own seed");
        }
    }

    @Test
    void branchGeometryIsBakedOnce() throws IOException {
        String code = stripComments(Files.readString(SOURCES.resolve(
                "dev/branched/block/StaticBranchBlock.java")));
        assertTrue(code.contains("private static final List<VoxelShape> SHAPES = buildShapes()"),
                "branch shapes must be cached in a static list");
        assertTrue(code.contains("List.of(table)"),
                "cached branch shapes must be immutable");
    }

    @Test
    void leavesAlwaysReceiveAnExplicitDistance() throws IOException {
        String code = stripComments(Files.readString(SOURCES.resolve(
                "dev/branched/worldgen/WorldGenTreeWriter.java")));
        assertTrue(code.contains("LeafDistance.distances"),
                "writer must compute leaf distances before writing");
        assertTrue(code.contains("LeavesBlock.DISTANCE"),
                "writer must set the vanilla leaf distance property");
    }

    private static List<Path> mainSources() throws IOException {
        try (Stream<Path> stream = Files.walk(SOURCES)) {
            List<Path> sources = stream
                    .filter(path -> path.toString().endsWith(".java"))
                    .toList();
            assertFalse(sources.isEmpty());
            return sources;
        }
    }

    private static String stripComments(String text) {
        return LINE_COMMENT.matcher(BLOCK_COMMENT.matcher(text).replaceAll(" "))
                .replaceAll(" ");
    }
}
