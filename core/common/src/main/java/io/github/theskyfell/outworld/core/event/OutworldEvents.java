package io.github.theskyfell.outworld.core.event;

import com.mojang.brigadier.CommandDispatcher;
import java.util.function.Consumer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * Server-side game events that common code can listen to. Core's loader layer forwards each loader's
 * own event here. Client events are in
 * {@link io.github.theskyfell.outworld.core.client.OutworldClientEvents}.
 *
 * <p>Tick listeners run every tick, so they must stay cheap: do the real work in batches or only when
 * something changed.
 */
public final class OutworldEvents {
    public static final Event<Consumer<MinecraftServer>> SERVER_STARTING = new Event<>();
    public static final Event<Consumer<MinecraftServer>> SERVER_STARTED = new Event<>();
    public static final Event<Consumer<MinecraftServer>> SERVER_STOPPING = new Event<>();

    /** After the server has ticked every level. */
    public static final Event<Consumer<MinecraftServer>> SERVER_TICK_END = new Event<>();

    /** After one level has ticked. */
    public static final Event<Consumer<ServerLevel>> LEVEL_TICK_END = new Event<>();

    public static final Event<CommandRegistration> REGISTER_COMMANDS = new Event<>();

    private OutworldEvents() {
    }

    @FunctionalInterface
    public interface CommandRegistration {
        void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context,
                Commands.CommandSelection selection);
    }
}
