package io.github.theskyfell.outworld.industry.power;

import io.github.theskyfell.outworld.core.machine.RecipeMachineBlockEntity;
import io.github.theskyfell.outworld.industry.IndustryContent;
import io.github.theskyfell.outworld.industry.OutworldIndustry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The steam turbine: a generator that runs on steam from boilers, drawn from the gas pipes and tanks
 * next to it, and pushes its energy into cables, batteries and machines. Its recipes name
 * {@link #MACHINE} as their machine.
 */
public final class SteamTurbineBlockEntity extends RecipeMachineBlockEntity {
    public static final Identifier MACHINE = Identifier.fromNamespaceAndPath(OutworldIndustry.MOD_ID, "steam_turbine");
    private static final Layout LAYOUT = new Layout(MACHINE, 0, 0, 0, 0, 1, 0, 8, 40_000, true);

    public SteamTurbineBlockEntity(BlockPos pos, BlockState state) {
        super(IndustryContent.STEAM_TURBINE.get(), pos, state, LAYOUT);
    }
}
