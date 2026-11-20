package io.github.theskyfell.outworld.core.command;

import com.mojang.brigadier.CommandDispatcher;
import io.github.theskyfell.outworld.core.machine.MachineScheduler;
import io.github.theskyfell.outworld.core.module.Modules;
import io.github.theskyfell.outworld.core.platform.Services;
import java.util.List;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/**
 * The {@code /outworld} command. Debug and admin tools hang off it as subcommands; those that show
 * server internals need the same permission as {@code /gamemode}.
 */
public final class OutworldCommand {
    private OutworldCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("outworld")
                .then(Commands.literal("version").executes(context -> {
                    String line = "Outworld on " + Services.PLATFORM.getPlatformName() + ": " + Modules.describe();
                    context.getSource().sendSuccess(() -> Component.literal(line), false);
                    return 1;
                }))
                .then(Commands.literal("machines")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> {
                            String text = describeMachines();
                            context.getSource().sendSuccess(() -> Component.literal(text), false);
                            return 1;
                        }))
                .then(Commands.literal("perf")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .executes(context -> {
                            String text = PerfReport.build();
                            context.getSource().sendSuccess(() -> Component.literal(text), false);
                            return 1;
                        })));
    }

    /** e.g. "Machines in minecraft:overworld: 12 loaded, 3 awake, 480 work batches since start". */
    private static String describeMachines() {
        List<MachineScheduler.Stats> stats = MachineScheduler.stats();
        if (stats.isEmpty()) {
            return "No machines are loaded.";
        }
        StringBuilder text = new StringBuilder();
        for (MachineScheduler.Stats level : stats) {
            if (!text.isEmpty()) {
                text.append('\n');
            }
            text.append("Machines in ").append(level.level().dimension().identifier())
                    .append(": ").append(level.loaded()).append(" loaded, ")
                    .append(level.awake()).append(" awake, ")
                    .append(level.batches()).append(" work batches since start");
        }
        return text.toString();
    }
}
