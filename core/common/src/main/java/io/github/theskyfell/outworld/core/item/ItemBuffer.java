package io.github.theskyfell.outworld.core.item;

import java.util.function.Predicate;
import java.util.stream.IntStream;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Item slots in an Outworld block. It is a vanilla {@link WorldlyContainer}, so each loader's own
 * transactional container wrapper exposes it to hoppers, pipes and other mods. Register it in
 * {@link io.github.theskyfell.outworld.core.module.OutworldModule#registerTransfers}.
 *
 * <p>Each slot has a {@link Role}: what the outside world may do with it. {@link #insert} and
 * {@link #extract} follow the roles, as other mods do. The block's own code can always read and change
 * any slot with {@link #getItem} and {@link #setItem}.
 */
public class ItemBuffer implements WorldlyContainer, ItemAccess {
    /** What the outside world (hoppers, pipes, other mods) may do with a slot. */
    public enum Role {
        /** Others may insert; only the block itself takes items out. */
        INPUT,
        /** Only the block itself puts items in; others may take them out. */
        OUTPUT,
        /** Others may insert and extract. */
        STORAGE
    }

    private final NonNullList<ItemStack> items;
    private final Role[] roles;
    private final int[] allSlots;
    private Predicate<ItemStack> inputFilter = stack -> true;
    private Runnable onChange = () -> { };

    public ItemBuffer(Role... roles) {
        this.roles = roles.clone();
        this.items = NonNullList.withSize(roles.length, ItemStack.EMPTY);
        this.allSlots = IntStream.range(0, roles.length).toArray();
    }

    /** Limits which items others may insert into INPUT and STORAGE slots. */
    public ItemBuffer setInputFilter(Predicate<ItemStack> filter) {
        this.inputFilter = filter;
        return this;
    }

    /**
     * Runs after every change. A change made inside another mod's transaction can notify before that
     * transaction commits, or even when it is rolled back, so keep the listener cheap and harmless to
     * repeat: mark the block entity changed, wake the machine.
     */
    public void onChange(Runnable listener) {
        this.onChange = listener;
    }

    public Role getRole(int slot) {
        return roles[slot];
    }

    public void save(ValueOutput output) {
        ContainerHelper.saveAllItems(output, items);
    }

    public void load(ValueInput input) {
        items.clear();
        ContainerHelper.loadAllItems(input, items);
    }

    // ItemAccess: the outside world's view, which follows the slot roles

    @Override
    public int insert(ItemStack stack, boolean simulate) {
        if (stack.isEmpty()) {
            return 0;
        }
        int remaining = stack.getCount();
        // Top up stacks of the same item first, then start new stacks in empty slots.
        for (int pass = 0; pass < 2 && remaining > 0; pass++) {
            boolean fillEmpty = pass == 1;
            for (int slot = 0; slot < items.size() && remaining > 0; slot++) {
                ItemStack current = items.get(slot);
                if (current.isEmpty() != fillEmpty || !canPlaceItem(slot, stack)) {
                    continue;
                }
                if (!fillEmpty && !ItemStack.isSameItemSameComponents(current, stack)) {
                    continue;
                }
                int moved = Math.min(remaining, getMaxStackSize(stack) - current.getCount());
                if (moved <= 0) {
                    continue;
                }
                if (!simulate) {
                    if (fillEmpty) {
                        items.set(slot, stack.copyWithCount(moved));
                    } else {
                        current.grow(moved);
                    }
                }
                remaining -= moved;
            }
        }
        int inserted = stack.getCount() - remaining;
        if (inserted > 0 && !simulate) {
            setChanged();
        }
        return inserted;
    }

    @Override
    public ItemStack extract(Predicate<ItemStack> filter, int maxCount, boolean simulate) {
        ItemStack result = ItemStack.EMPTY;
        int limit = maxCount;
        for (int slot = 0; slot < items.size() && result.getCount() < limit; slot++) {
            ItemStack current = items.get(slot);
            if (current.isEmpty() || roles[slot] == Role.INPUT) {
                continue;
            }
            if (result.isEmpty()) {
                if (!filter.test(current)) {
                    continue;
                }
                limit = Math.min(maxCount, current.getMaxStackSize());
            } else if (!ItemStack.isSameItemSameComponents(current, result)) {
                continue;
            }
            int moved = Math.min(limit - result.getCount(), current.getCount());
            if (moved <= 0) {
                continue;
            }
            if (result.isEmpty()) {
                result = current.copyWithCount(moved);
            } else {
                result.grow(moved);
            }
            if (!simulate) {
                current.shrink(moved);
                if (current.isEmpty()) {
                    items.set(slot, ItemStack.EMPTY);
                }
            }
        }
        if (!result.isEmpty() && !simulate) {
            setChanged();
        }
        return result;
    }

    // Container

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int count) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, count);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        stack.limitSize(getMaxStackSize(stack));
        setChanged();
    }

    @Override
    public void setChanged() {
        onChange.run();
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void clearContent() {
        items.clear();
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return roles[slot] != Role.OUTPUT && inputFilter.test(stack);
    }

    // WorldlyContainer: every slot is reachable from every side; the roles decide what is allowed.

    @Override
    public int[] getSlotsForFace(Direction side) {
        return allSlots;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return roles[slot] != Role.INPUT;
    }
}
