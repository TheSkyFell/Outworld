package io.github.theskyfell.outworld.industry.client;

import io.github.theskyfell.outworld.core.client.ClientNetwork;
import io.github.theskyfell.outworld.industry.test.TestMachineMenu;
import io.github.theskyfell.outworld.industry.test.TestPing;
import io.github.theskyfell.outworld.industry.test.TestPong;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Temporary test screen: shows where the machine is and pings the server. */
public final class TestMachineScreen extends AbstractContainerScreen<TestMachineMenu> {
    private static final int PANEL = 0xFFC6C6C6;
    private static final int PANEL_LIGHT = 0xFFFFFFFF;
    private static final int PANEL_DARK = 0xFF555555;
    private static final int TEXT = 0xFF404040;

    private Component status = Component.translatable("screen.outworld_industry.test_block.idle");
    private Component reply = Component.empty();

    public TestMachineScreen(TestMachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 240;
        this.imageHeight = 96;
    }

    /** Shows the server's reply if this screen is still open. */
    public static void onPong(TestPong pong) {
        if (Minecraft.getInstance().screen instanceof TestMachineScreen screen) {
            long roundTripMillis = System.currentTimeMillis() - pong.sentAtMillis();
            screen.status = Component.translatable("screen.outworld_industry.test_block.reply", roundTripMillis);
            screen.reply = Component.literal(pong.message());
        }
    }

    @Override
    protected void init() {
        super.init();
        addRenderableWidget(Button.builder(Component.translatable("screen.outworld_industry.test_block.ping"), button -> {
            status = Component.translatable("screen.outworld_industry.test_block.waiting");
            reply = Component.empty();
            ClientNetwork.sendToServer(new TestPing(menu.pos(), System.currentTimeMillis()));
        }).bounds(leftPos + 8, topPos + imageHeight - 28, 100, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        extractBackground(graphics, mouseX, mouseY, delta);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        extractTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int right = leftPos + imageWidth;
        int bottom = topPos + imageHeight;
        graphics.fill(leftPos, topPos, right, bottom, PANEL_DARK);
        graphics.fill(leftPos, topPos, right - 1, bottom - 1, PANEL_LIGHT);
        graphics.fill(leftPos + 1, topPos + 1, right - 1, bottom - 1, PANEL);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, 8, 6, TEXT, false);
        graphics.text(font, Component.translatable("screen.outworld_industry.test_block.position", menu.pos().toShortString()),
                8, 22, TEXT, false);
        graphics.text(font, status, 8, 38, TEXT, false);
        graphics.text(font, reply, 8, 50, TEXT, false);
    }
}
