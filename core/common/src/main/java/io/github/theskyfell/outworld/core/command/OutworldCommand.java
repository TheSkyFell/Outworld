package io.github.theskyfell.outworld.core.command;

import com.mojang.brigadier.CommandDispatcher;
import io.github.theskyfell.outworld.core.module.Modules;
import io.github.theskyfell.outworld.core.platform.Services;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;

/** The {@code /outworld} command. Debug and admin tools hang off it as subcommands. */
public final class OutworldCommand {
    private OutworldCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("outworld")
                .then(Commands.literal("version").executes(context -> {
                    String line = "Outworld on " + Services.PLATFORM.getPlatformName() + ": " + Modules.describe();
                    context.getSource().sendSuccess(() -> Component.literal(line), false);
                    return 1;
                })));
    }
}
