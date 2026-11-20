package io.github.theskyfell.outworld.core.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import io.github.theskyfell.outworld.core.stress.StressScenarios;
import io.github.theskyfell.outworld.core.stress.StressTest;
import java.util.function.Supplier;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/**
 * {@code /outworld stress}: builds a stress scenario and measures what Outworld costs under load and
 * when idle. Needs the same permission as {@code /ban}, since it builds in the world.
 */
final class StressCommand {
    private StressCommand() {
    }

    static LiteralArgumentBuilder<CommandSourceStack> node() {
        return Commands.literal("stress")
                .requires(Commands.hasPermission(Commands.LEVEL_ADMINS))
                .then(Commands.literal("list").executes(context -> reply(context, StressTest::list)))
                .then(Commands.literal("status").executes(context -> reply(context, StressTest::status)))
                .then(Commands.literal("stop").executes(context -> reply(context, StressTest::stop)))
                .then(Commands.literal("clear").executes(context -> reply(context, StressTest::clear)))
                .then(Commands.literal("start")
                        .then(Commands.argument("scenario", StringArgumentType.word())
                                .suggests((context, builder) -> SharedSuggestionProvider.suggest(StressScenarios.all().keySet(), builder))
                                .executes(context -> start(context, null, null))
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 10_000))
                                        .executes(context -> start(context, IntegerArgumentType.getInteger(context, "count"), null))
                                        .then(Commands.argument("origin", BlockPosArgument.blockPos())
                                                .executes(context -> start(context, IntegerArgumentType.getInteger(context, "count"),
                                                        BlockPosArgument.getBlockPos(context, "origin")))))));
    }

    private static int start(CommandContext<CommandSourceStack> context, Integer count, BlockPos origin) {
        CommandSourceStack source = context.getSource();
        String scenario = StringArgumentType.getString(context, "scenario");
        BlockPos at = origin != null ? origin : BlockPos.containing(source.getPosition());
        try {
            String message = StressTest.start(source.getLevel(), scenario, count, at);
            source.sendSuccess(() -> Component.literal(message), true);
            return 1;
        } catch (IllegalStateException e) {
            source.sendFailure(Component.literal(e.getMessage()));
            return 0;
        }
    }

    private static int reply(CommandContext<CommandSourceStack> context, Supplier<String> action) {
        String message = action.get();
        context.getSource().sendSuccess(() -> Component.literal(message), false);
        return 1;
    }
}
