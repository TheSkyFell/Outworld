package io.github.theskyfell.outworld.industry.test;

import io.github.theskyfell.outworld.core.OutworldCore;
import io.github.theskyfell.outworld.core.network.Network;
import io.github.theskyfell.outworld.core.platform.Services;
import net.minecraft.server.level.ServerPlayer;

/** Server side of the test machine. */
public final class TestMachine {
    private TestMachine() {
    }

    /** Answers a ping, but only from a player who really has that test machine open. */
    public static void handlePing(TestPing ping, ServerPlayer sender) {
        if (!(sender.containerMenu instanceof TestMachineMenu menu) || !menu.pos().equals(ping.pos())) {
            return;
        }
        String message = Services.PLATFORM.getPlatformName() + " server, game time " + sender.level().getGameTime()
                + ", machine interval " + OutworldCore.config().machines.workIntervalTicks + " ticks";
        Network.sendToPlayer(sender, new TestPong(ping.sentAtMillis(), message));
    }
}
