package io.github.theskyfell.outworld.core.client;

import io.github.theskyfell.outworld.core.event.Event;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;

/** Client-side game events. Core's client loader layer forwards each loader's own event here. */
public final class OutworldClientEvents {
    /** After the client has ticked. Runs every tick, so listeners must stay cheap. */
    public static final Event<Consumer<Minecraft>> CLIENT_TICK_END = new Event<>();

    private OutworldClientEvents() {
    }
}
