package dev.branched.diagnostics;

import com.mojang.brigadier.CommandDispatcher;
import dev.branched.Branched;
import dev.branched.compat.AdapterState;
import dev.branched.compat.ModAdapter;
import dev.branched.platform.ForgeModPresence;
import dev.branched.platform.ModPresence;
import dev.branched.worldgen.WorldgenGate;
import dev.branched.worldgen.feature.StaticTreeFeature;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.event.RegisterCommandsEvent;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

/**
 * In-game diagnostics command ({@code /branched status} and
 * {@code /branched dump}) for verifying loaded profiles, adapter states
 * and runtime metrics without altering world state.
 */
public final class ArborealCommands {
    private ArborealCommands() {
    }

    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("branched")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("status").executes(ctx -> status(ctx.getSource())))
                .then(Commands.literal("dump").executes(ctx -> dump(ctx.getSource()))));
    }

    private static int status(CommandSourceStack source) {
        ModPresence presence = new ForgeModPresence();
        Map<String, AdapterState> states = Branched.adapters().states(presence);
        long ready = states.values().stream().filter(s -> s == AdapterState.READY).count();
        long replaceable = Branched.profiles().all().stream()
                .filter(p -> p.compatibility().mayReplace()).count();

        StringBuilder adapterSummary = new StringBuilder();
        for (ModAdapter adapter : Branched.adapters().all()) {
            if (adapterSummary.length() > 0) {
                adapterSummary.append(", ");
            }
            adapterSummary.append(adapter.modId()).append('=').append(states.get(adapter.modId()));
        }

        String summary = String.format(java.util.Locale.ROOT,
                "Branched: enabled=%s preset=%s profiles=%d (replaceable=%d) "
                        + "adapters=%d/%d [%s] placed=%d rejected=%d metrics=[%s]",
                WorldgenGate.enabled(),
                WorldgenGate.performancePreset().name().toLowerCase(java.util.Locale.ROOT),
                Branched.profiles().size(),
                replaceable,
                ready,
                Branched.adapters().size(),
                adapterSummary,
                StaticTreeFeature.placedCount(),
                StaticTreeFeature.rejectedCount(),
                Branched.metrics().snapshot().format());
        source.sendSuccess(() -> Component.literal(summary), false);
        return 1;
    }

    private static int dump(CommandSourceStack source) {
        try {
            Path baseDir = source.getServer().getServerDirectory().toPath();
            Path written = DiagnosticsExporter.exportDefault(baseDir);
            source.sendSuccess(() -> Component.literal(
                    "Branched diagnostics written to " + written.toAbsolutePath()), true);
            return 1;
        } catch (IOException ex) {
            source.sendFailure(Component.literal(
                    "Failed to write Branched diagnostics: " + ex.getMessage()));
            return 0;
        }
    }
}
