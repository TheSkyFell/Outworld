package io.github.theskyfell.outworld.industry.power;

import io.github.theskyfell.outworld.core.energy.Energy;
import io.github.theskyfell.outworld.core.energy.EnergyBuffer;
import io.github.theskyfell.outworld.core.machine.MachineBlockEntity;
import io.github.theskyfell.outworld.industry.IndustryContent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A solar panel: makes up to {@link #PEAK} FE per tick under open sky, less as the sky darkens and
 * nothing at night, and pushes it into neighbouring storage (cables, batteries, machines). The sun
 * moves slowly, so it only works once a second.
 */
public final class SolarPanelBlockEntity extends MachineBlockEntity {
    public static final long PEAK = 16;
    private static final int INTERVAL_TICKS = 20;

    private final EnergyBuffer energy = new EnergyBuffer(4_000, 0, 4_000);

    public SolarPanelBlockEntity(BlockPos pos, BlockState state) {
        super(IndustryContent.SOLAR_PANEL.get(), pos, state);
        energy.onChange(this::setChanged);
    }

    public EnergyBuffer energy() {
        return energy;
    }

    /** FE per tick right now: full in daylight, nothing at night or under a roof. */
    public long output(ServerLevel level) {
        if (!level.canSeeSky(worldPosition.above())) {
            return 0;
        }
        int darkness = level.getSkyDarken(); // 0 at noon, 11 at midnight
        return Math.max(0, PEAK * (11 - darkness) / 11);
    }

    @Override
    protected boolean work(ServerLevel level, int elapsedTicks) {
        long made = elapsedTicks * output(level);
        if (made > 0) {
            energy.setAmount(Math.min(energy.getCapacity(), energy.getAmount() + made));
        }
        if (energy.getAmount() > 0) {
            Energy.pushToNeighbours(level, worldPosition, energy, energy.getAmount());
        }
        retryIn(INTERVAL_TICKS);
        return false;
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
