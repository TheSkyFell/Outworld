package io.github.theskyfell.outworld.industry.machine;

import io.github.theskyfell.outworld.core.machine.MachineUpgrade;
import net.minecraft.world.item.Item;

/** A speed or efficiency upgrade, which goes in a recipe machine's upgrade slots. */
public final class UpgradeItem extends Item implements MachineUpgrade {
    private final Kind kind;

    public UpgradeItem(Kind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public Kind upgradeKind() {
        return kind;
    }
}
