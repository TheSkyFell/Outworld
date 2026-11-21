package io.github.theskyfell.outworld.core.client;

import io.github.theskyfell.outworld.core.machine.MachineMenu;
import io.github.theskyfell.outworld.core.machine.MachineUpgrades;
import io.github.theskyfell.outworld.core.machine.MachineValues;
import java.util.List;
import java.util.Locale;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;

/**
 * The screen of every recipe machine: input slots on the left, an arrow showing progress, output
 * slots on the right, the energy bar on the far left, tank gauges on the far right and the upgrade
 * slots below them. Drawn from flat colours, so it needs no textures. Machines with more to show
 * extend it, widening it to the right.
 */
public class MachineScreen<M extends MachineMenu> extends AbstractContainerScreen<M> {
    protected static final int PANEL = 0xFFC6C6C6;
    protected static final int PANEL_LIGHT = 0xFFFFFFFF;
    protected static final int PANEL_DARK = 0xFF555555;
    protected static final int SLOT = 0xFF8B8B8B;
    protected static final int SLOT_DARK = 0xFF373737;
    protected static final int TEXT = 0xFF404040;
    /** The width of the machine's part of the screen; wider screens put their extras to the right of it. */
    protected static final int MACHINE_WIDTH = 176;
    private static final int ENERGY = 0xFFD8402A;
    private static final int PROGRESS = 0xFF4CC2A0;
    private static final int GAUGE_BACK = 0xFF2B2B2B;

    private static final int ENERGY_X = 8;
    private static final int GAUGE_TOP = 17;
    private static final int GAUGE_HEIGHT = 52;
    private static final int GAUGE_WIDTH = 10;
    private static final int TANK_RIGHT = 168;

    public MachineScreen(M menu, Inventory inventory, Component title) {
        this(menu, inventory, title, MACHINE_WIDTH);
    }

    protected MachineScreen(M menu, Inventory inventory, Component title, int width) {
        super(menu, inventory, title, width, MachineMenu.HEIGHT);
    }

    /** Puts new readings on the open machine screen, if they are for it. */
    public static void onValues(MachineValues values) {
        if (Minecraft.getInstance().player != null
                && Minecraft.getInstance().player.containerMenu instanceof MachineMenu menu
                && menu.containerId == values.containerId()) {
            menu.setValues(values);
        }
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        extractBackground(graphics, mouseX, mouseY, delta);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        extractTooltip(graphics, mouseX, mouseY);
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        int left = leftPos;
        int top = topPos;
        int right = left + imageWidth;
        int bottom = top + imageHeight;
        graphics.fill(left, top, right, bottom, PANEL_DARK);
        graphics.fill(left, top, right - 1, bottom - 1, PANEL_LIGHT);
        graphics.fill(left + 1, top + 1, right - 1, bottom - 1, PANEL);

        for (Slot slot : menu.slots) {
            int x = left + slot.x - 1;
            int y = top + slot.y - 1;
            graphics.fill(x, y, x + 18, y + 18, PANEL_LIGHT);
            graphics.fill(x, y, x + 17, y + 17, SLOT_DARK);
            graphics.fill(x + 1, y + 1, x + 17, y + 17, SLOT);
        }

        MachineValues values = menu.values();
        gauge(graphics, left + ENERGY_X, top + GAUGE_TOP, ENERGY, fraction(values.energy(), values.energyCapacity()));

        // Progress arrow between the inputs and the outputs.
        int arrowLeft = left + MachineMenu.OUTPUT_X - 30;
        int arrowTop = top + MachineMenu.SLOT_Y + 4;
        int arrowWidth = 22;
        graphics.fill(arrowLeft, arrowTop, arrowLeft + arrowWidth, arrowTop + 8, GAUGE_BACK);
        int done = (int) Math.round(arrowWidth * fraction(values.progress(), values.recipeTime()));
        if (done > 0) {
            graphics.fill(arrowLeft, arrowTop, arrowLeft + done, arrowTop + 8, PROGRESS);
        }

        List<MachineValues.Tank> tanks = values.tanks();
        for (int i = 0; i < tanks.size(); i++) {
            MachineValues.Tank tank = tanks.get(i);
            int x = left + TANK_RIGHT - GAUGE_WIDTH - i * (GAUGE_WIDTH + 4);
            gauge(graphics, x, top + GAUGE_TOP, 0xFF000000 | tank.color(), fraction(tank.amount(), tank.capacity()));
        }
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        graphics.text(font, title, 8, 6, TEXT, false);
        graphics.text(font, playerInventoryTitle, 8, imageHeight - 94, TEXT, false);
        MachineValues values = menu.values();
        String energy = String.format(Locale.ROOT, "%,d / %,d FE", values.energy(), values.energyCapacity());
        graphics.text(font, Component.literal(energy), MachineMenu.INPUT_X, MachineMenu.energyTextY(menu.itemInputs()), TEXT, false);
        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        // Hovering an empty upgrade slot says what goes there.
        for (int i = 0; i < MachineUpgrades.SLOTS; i++) {
            Slot slot = menu.slots.get(menu.upgradeSlot(i));
            if (!slot.hasItem() && x >= slot.x && x < slot.x + 16 && y >= slot.y && y < slot.y + 16) {
                hoverLine(graphics, Component.translatable("container.outworld.upgrades"));
            }
        }
        // Hovering a gauge shows what it holds.
        if (y >= GAUGE_TOP && y < GAUGE_TOP + GAUGE_HEIGHT) {
            List<MachineValues.Tank> tanks = values.tanks();
            for (int i = 0; i < tanks.size(); i++) {
                int tankX = TANK_RIGHT - GAUGE_WIDTH - i * (GAUGE_WIDTH + 4);
                if (x >= tankX && x < tankX + GAUGE_WIDTH) {
                    MachineValues.Tank tank = tanks.get(i);
                    // Named by the translation key gas.<namespace>.<path> or fluid.<namespace>.<path>.
                    Component contents = tank.contents().isEmpty() ? Component.literal("Empty")
                            : Component.translatableWithFallback((tank.gas() ? "gas." : "fluid.") + tank.contents().replace(':', '.'), tank.contents());
                    String amounts = String.format(Locale.ROOT, ": %,d / %,d mB", tank.amount(), tank.capacity());
                    hoverLine(graphics, contents.copy().append(amounts));
                }
            }
        }
    }

    /** Shows a line about what the mouse is over, beside the inventory's title. Call from {@link #extractLabels}. */
    protected void hoverLine(GuiGraphicsExtractor graphics, Component line) {
        graphics.text(font, line, 70, imageHeight - 94, TEXT, false);
    }

    /** A vertical gauge, filled from the bottom. */
    private static void gauge(GuiGraphicsExtractor graphics, int x, int top, int color, double fraction) {
        graphics.fill(x - 1, top - 1, x + GAUGE_WIDTH + 1, top + GAUGE_HEIGHT + 1, PANEL_DARK);
        graphics.fill(x, top, x + GAUGE_WIDTH, top + GAUGE_HEIGHT, GAUGE_BACK);
        int filled = (int) Math.round(GAUGE_HEIGHT * fraction);
        if (filled > 0) {
            graphics.fill(x, top + GAUGE_HEIGHT - filled, x + GAUGE_WIDTH, top + GAUGE_HEIGHT, color);
        }
    }

    private static double fraction(long amount, long capacity) {
        return capacity <= 0 ? 0 : Math.clamp((double) amount / capacity, 0, 1);
    }
}
