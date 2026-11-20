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
 * loader's energy API, so the test machine (and any other mod) reaches it the standard way.
 */
public final class TestBatteryBlockEntity extends BlockEntity {
    public static final long CAPACITY = 10_000;

    private final EnergyBuffer energy = new EnergyBuffer(CAPACITY, 1_000, 1_000);

    public TestBatteryBlockEntity(BlockPos pos, BlockState state) {
        super(IndustryContent.TEST_BATTERY.get(), pos, state);
        energy.setAmount(CAPACITY);
        energy.onChange(this::setChanged);
    }

    public EnergyBuffer energy() {
        return energy;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energy.save(output, "energy");
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.load(input, "energy");
    }
}
