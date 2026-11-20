package io.github.theskyfell.outworld.industry.test;

import io.github.theskyfell.outworld.core.energy.EnergyBuffer;
import io.github.theskyfell.outworld.industry.IndustryContent;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Temporary test battery: holds {@link #CAPACITY} FE and starts full. It is exposed through each
 * loader's energy API, so the test machine (and any other mod) reaches it the standard way. A
 * {@link #setCreative creative} battery refills itself whenever energy is taken, so it never runs out.
 */
public final class TestBatteryBlockEntity extends BlockEntity {
    public static final long CAPACITY = 10_000;

    private final EnergyBuffer energy = new EnergyBuffer(CAPACITY, 1_000, 1_000);
    private boolean creative;

    public TestBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(IndustryContent.TEST_BATTERY.get(), pos, state);
        energy.setAmount(CAPACITY);
        energy.onChange(() -> {
            setChanged();
            if (creative && energy.getAmount() < CAPACITY) {
                energy.setAmount(CAPACITY); // changes it, so this runs once more and stops
            }
        });
    }

    /** Whether the battery refills itself whenever energy is taken. */
    public void setCreative(boolean creative) {
        this.creative = creative;
        setChanged();
        if (creative) {
            energy.setAmount(CAPACITY);
        }
    }

    public EnergyBuffer energy() {
        return energy;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energy.save(output, "energy");
        output.putBoolean("creative", creative);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.load(input, "energy");
        creative = input.getBooleanOr("creative", false);
    }
}
