package io.github.theskyfell.outworld.industry.machine;

import io.github.theskyfell.outworld.core.machine.RecipeMachineBlockEntity;
import io.github.theskyfell.outworld.industry.IndustryContent;
import io.github.theskyfell.outworld.industry.OutworldIndustry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The gas liquefier: chills and compresses a gas into its liquid, ten to one, using a lot of energy.
 * Hydrogen and oxygen from the electrolyzer become liquid hydrogen and liquid oxygen, the rocket
 * propellants. It draws gas from gas pipes and tanks next to it and pushes the liquid into fluid pipes
 * and tanks. Its recipes name {@link #MACHINE} as their machine.
 */
public final class GasLiquefierBlockEntity extends RecipeMachineBlockEntity {
    public static final Identifier MACHINE = Identifier.fromNamespaceAndPath(OutworldIndustry.MOD_ID, "gas_liquefier");
    private static final Layout LAYOUT = new Layout(MACHINE, 0, 0, 0, 1, 1, 0, 8, 40_000, false);

    public GasLiquefierBlockEntity(BlockPos pos, BlockState state) {
        super(IndustryContent.GAS_LIQUEFIER.get(), pos, state, LAYOUT);
    }
}
