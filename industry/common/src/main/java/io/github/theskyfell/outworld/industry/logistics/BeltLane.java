package io.github.theskyfell.outworld.industry.logistics;

import java.util.Arrays;
import net.minecraft.world.item.ItemStack;

/**
 * One lane of a belt: up to {@link #CAPACITY} single items, each at a position from 0 (where items come
 * on) to {@link #LENGTH} (the end, where they leave). Items keep at least {@link #SPACING} apart and
 * never pass each other. Index 0 is the item furthest along.
 *
 * <p>Plain data with no level access, so the server and the client (which draws items between updates
 * by running the same rules) share it.
 */
public final class BeltLane {
    /** Positions along a lane, in 1/256 of a block. */
    public static final int LENGTH = 256;
    /** The closest two items get: four fit on a lane. */
    public static final int SPACING = 64;
    public static final int CAPACITY = LENGTH / SPACING;

    private final ItemStack[] items = new ItemStack[CAPACITY];
    private final int[] positions = new int[CAPACITY];
    private int size;

    /** Takes an item off the end of a lane that reached it, or refuses. */
    @FunctionalInterface
    public interface Exit {
        /**
         * @param overflow how far past the end the item would have travelled, to place it on the next belt
         * @return true if the item was taken
         */
        boolean accept(ItemStack item, int overflow);
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public ItemStack item(int index) {
        return items[index];
    }

    public int position(int index) {
        return positions[index];
    }

    /** Whether an item fits at {@code position} without coming closer than {@link #SPACING} to another. */
    public boolean canInsert(int position) {
        return size < CAPACITY && position >= 0 && position <= LENGTH && slotFor(position) >= 0;
    }

    /** Puts a single item on the lane at {@code position}; false if it doesn't fit there. */
    public boolean insert(ItemStack item, int position) {
        if (size >= CAPACITY || position < 0 || position > LENGTH) {
            return false;
        }
        int index = slotFor(position);
        if (index < 0) {
            return false;
        }
        System.arraycopy(items, index, items, index + 1, size - index);
        System.arraycopy(positions, index, positions, index + 1, size - index);
        items[index] = item.copyWithCount(1);
        positions[index] = position;
        size++;
        return true;
    }

    /** Takes the item at {@code index} off the lane. */
    public ItemStack remove(int index) {
        ItemStack item = items[index];
        System.arraycopy(items, index + 1, items, index, size - index - 1);
        System.arraycopy(positions, index + 1, positions, index, size - index - 1);
        size--;
        items[size] = null;
        return item;
    }

    public void clear() {
        Arrays.fill(items, null);
        size = 0;
    }

    /**
     * Moves every item {@code distance} along, as far as the items ahead allow. An item that reaches the
     * end is offered to {@code exit} (null: nothing takes it) and stops at the end if refused.
     *
     * @return true if any item moved or left
     */
    public boolean advance(int distance, Exit exit) {
        boolean moved = false;
        while (size > 0 && exit != null && positions[0] + distance >= LENGTH) {
            int overflow = Math.max(0, positions[0] + distance - LENGTH);
            if (!exit.accept(items[0], overflow)) {
                break;
            }
            remove(0);
            moved = true;
        }
        for (int i = 0; i < size; i++) {
            int limit = i == 0 ? LENGTH : positions[i - 1] - SPACING;
            int target = Math.min(positions[i] + distance, limit);
            if (target > positions[i]) {
                positions[i] = target;
                moved = true;
            }
        }
        return moved;
    }

    /**
     * Where each item would be after {@code distance} more travel with nothing leaving: what the client
     * draws between updates. Writes into {@code out}, which must have room for {@link #size} entries.
     */
    public void project(float distance, float[] out) {
        for (int i = 0; i < size; i++) {
            float limit = i == 0 ? LENGTH : out[i - 1] - SPACING;
            out[i] = Math.max(positions[i], Math.min(positions[i] + distance, limit));
        }
    }

    /** Copies another lane's contents into this one. */
    public void copyFrom(BeltLane other) {
        clear();
        for (int i = 0; i < other.size; i++) {
            items[i] = other.items[i];
            positions[i] = other.positions[i];
        }
        size = other.size;
    }

    /** Where an item at {@code position} would go in the arrays, or -1 if it would be too close to one. */
    private int slotFor(int position) {
        int index = 0;
        while (index < size && positions[index] >= position) {
            index++;
        }
        if (index > 0 && positions[index - 1] - position < SPACING) {
            return -1;
        }
        if (index < size && position - positions[index] < SPACING) {
            return -1;
        }
        return index;
    }
}
