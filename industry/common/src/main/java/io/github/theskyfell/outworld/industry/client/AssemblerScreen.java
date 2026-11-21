package io.github.theskyfell.outworld.industry.client;

import io.github.theskyfell.outworld.core.client.ClientNetwork;
import io.github.theskyfell.outworld.core.client.MachineScreen;
import io.github.theskyfell.outworld.industry.machine.AssemblerBlockEntity;
import io.github.theskyfell.outworld.industry.machine.AssemblerMenu;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/**
 * The assembler's screen: a recipe machine's screen with a list of every recipe it can make on the
 * right, a page at a time, with a search box. Clicking a recipe chooses it; clicking the chosen one
 * again chooses none.
 */
public final class AssemblerScreen extends MachineScreen<AssemblerMenu> {
    private static final int LIST_X = MACHINE_WIDTH + 2;
    private static final int COLUMNS = 6;
    private static final int ROWS = 7;
    private static final int CELL = 18;
    private static final int PER_PAGE = COLUMNS * ROWS;
    private static final int SEARCH_TOP = 16;
    private static final int LIST_TOP = 32;
    private static final int PAGE_TOP = LIST_TOP + ROWS * CELL + 2;
    private static final int WIDTH = LIST_X + COLUMNS * CELL + 8;
    private static final int CHOSEN = 0xFF4CC2A0;
    private static final int KEY_ESCAPE = 256;
    /** Where the chosen recipe's result shows: above the progress arrow. */
    private static final int CHOSEN_X = 86;
    private static final int CHOSEN_Y = 16;

    private final List<Button> cells = new ArrayList<>();
    private EditBox search;
    private Button previous;
    private Button next;
    private List<AssemblerBlockEntity.Choice> shown = List.of();
    private int page;
    private int seenVersion = -1;

    public AssemblerScreen(AssemblerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, WIDTH);
    }

    @Override
    protected void init() {
        super.init();
        cells.clear();
        String query = search != null ? search.getValue() : "";
        search = new EditBox(font, leftPos + LIST_X, topPos + SEARCH_TOP, COLUMNS * CELL, 12,
                Component.translatable("container.outworld_industry.assembler.search"));
        search.setMaxLength(50);
        search.setValue(query);
        search.setResponder(text -> {
            page = 0;
            refresh();
        });
        addRenderableWidget(search);
        for (int i = 0; i < PER_PAGE; i++) {
            int cell = i;
            cells.add(addRenderableWidget(Button.builder(Component.empty(), button -> pick(cell))
                    .bounds(leftPos + LIST_X + i % COLUMNS * CELL, topPos + LIST_TOP + i / COLUMNS * CELL, CELL, CELL)
                    .build()));
        }
        previous = addRenderableWidget(Button.builder(Component.literal("<"), button -> {
            page--;
            refresh();
        }).bounds(leftPos + LIST_X, topPos + PAGE_TOP, 20, 14).build());
        next = addRenderableWidget(Button.builder(Component.literal(">"), button -> {
            page++;
            refresh();
        }).bounds(leftPos + LIST_X + COLUMNS * CELL - 20, topPos + PAGE_TOP, 20, 14).build());
        refresh();
    }

    /** Shows the recipes that match the search, on the current page. */
    private void refresh() {
        seenVersion = menu.version();
        String query = search == null ? "" : search.getValue().trim().toLowerCase(Locale.ROOT);
        List<AssemblerBlockEntity.Choice> matching = new ArrayList<>();
        for (AssemblerBlockEntity.Choice choice : menu.choices()) {
            if (query.isEmpty() || choice.result().getHoverName().getString().toLowerCase(Locale.ROOT).contains(query)
                    || choice.recipe().toString().contains(query)) {
                matching.add(choice);
            }
        }
        shown = matching;
        int pages = Math.max(1, (shown.size() + PER_PAGE - 1) / PER_PAGE);
        page = Math.clamp(page, 0, pages - 1);
        for (int i = 0; i < cells.size(); i++) {
            cells.get(i).visible = page * PER_PAGE + i < shown.size();
        }
        if (previous != null) {
            previous.active = page > 0;
            next.active = page < pages - 1;
        }
    }

    private AssemblerBlockEntity.Choice choiceAt(int cell) {
        int index = page * PER_PAGE + cell;
        return index < shown.size() ? shown.get(index) : null;
    }

    private void pick(int cell) {
        AssemblerBlockEntity.Choice choice = choiceAt(cell);
        if (choice == null) {
            return;
        }
        Identifier recipe = choice.recipe().equals(menu.chosen()) ? null : choice.recipe();
        menu.pick(recipe);
        ClientNetwork.sendToServer(new AssemblerMenu.Choose(menu.containerId, recipe == null ? "" : recipe.toString()));
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        if (menu.version() != seenVersion) {
            refresh();
        }
        super.extractRenderState(graphics, mouseX, mouseY, delta);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractLabels(graphics, mouseX, mouseY);
        // Drawn with the labels, after the buttons, so the icons sit on top of them.
        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        for (int cell = 0; cell < PER_PAGE; cell++) {
            AssemblerBlockEntity.Choice choice = choiceAt(cell);
            if (choice == null) {
                break;
            }
            int cellX = LIST_X + cell % COLUMNS * CELL;
            int cellY = LIST_TOP + cell / COLUMNS * CELL;
            if (choice.recipe().equals(menu.chosen())) {
                outline(graphics, cellX, cellY, CHOSEN);
            }
            graphics.item(choice.result(), cellX + 1, cellY + 1);
            if (x >= cellX && x < cellX + CELL && y >= cellY && y < cellY + CELL) {
                hoverLine(graphics, describe(choice));
            }
        }
        int pages = Math.max(1, (shown.size() + PER_PAGE - 1) / PER_PAGE);
        Component pageLabel = Component.literal((page + 1) + " / " + pages);
        graphics.text(font, pageLabel, LIST_X + (COLUMNS * CELL - font.width(pageLabel)) / 2, PAGE_TOP + 3, TEXT, false);
        // What the assembler is making, above the progress arrow.
        AssemblerBlockEntity.Choice chosen = chosenChoice();
        if (chosen == null) {
            graphics.text(font, Component.translatable("container.outworld_industry.assembler.none"), CHOSEN_X, CHOSEN_Y + 4, TEXT, false);
        } else {
            graphics.item(chosen.result(), CHOSEN_X + 3, CHOSEN_Y);
            if (x >= CHOSEN_X + 3 && x < CHOSEN_X + 19 && y >= CHOSEN_Y && y < CHOSEN_Y + 16) {
                hoverLine(graphics, describe(chosen));
            }
        }
    }

    private AssemblerBlockEntity.Choice chosenChoice() {
        for (AssemblerBlockEntity.Choice choice : menu.choices()) {
            if (Objects.equals(choice.recipe(), menu.chosen())) {
                return choice;
            }
        }
        return null;
    }

    private static Component describe(AssemblerBlockEntity.Choice choice) {
        Component name = choice.result().getHoverName();
        return choice.result().getCount() > 1 ? name.copy().append(" ×" + choice.result().getCount()) : name;
    }

    private static void outline(GuiGraphicsExtractor graphics, int x, int y, int color) {
        graphics.fill(x, y, x + CELL, y + 1, color);
        graphics.fill(x, y + CELL - 1, x + CELL, y + CELL, color);
        graphics.fill(x, y, x + 1, y + CELL, color);
        graphics.fill(x + CELL - 1, y, x + CELL, y + CELL, color);
    }

    /** Typing in the search box mustn't close the screen when a letter is the inventory key. */
    @Override
    public boolean keyPressed(KeyEvent event) {
        if (search != null && search.isFocused() && event.key() != KEY_ESCAPE) {
            return search.keyPressed(event) || search.canConsumeInput() || super.keyPressed(event);
        }
        return super.keyPressed(event);
    }
}
