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
        String server = Services.PLATFORM.getPlatformName() + " server, game time " + sender.level().getGameTime()
                + ", work interval " + OutworldCore.config().machines.workIntervalTicks + " ticks";
        String machine = sender.level().getBlockEntity(ping.pos()) instanceof TestMachineBlockEntity testMachine
                ? "Machine " + (testMachine.isAwake() ? "awake" : "asleep") + ", " + testMachine.workDone() + " ticks of work done, "
                        + testMachine.energy().getAmount() + " FE stored"
                : "No machine here";
        Network.sendToPlayer(sender, new TestPong(ping.sentAtMillis(), server + "\n" + machine));
    }
}
