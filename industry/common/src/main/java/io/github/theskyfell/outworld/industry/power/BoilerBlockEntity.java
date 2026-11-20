package io.github.theskyfell.outworld.industry.power;

import io.github.theskyfell.outworld.core.machine.RecipeMachineBlockEntity;
import io.github.theskyfell.outworld.industry.IndustryContent;
import io.github.theskyfell.outworld.industry.OutworldIndustry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The boiler: burns solid fuel to boil water into steam, which it pushes into gas pipes for steam
 * turbines. It needs no energy. With a turbine it gets more energy out of fuel than the combustion
 * generator, for more building. Its recipes name {@link #MACHINE} as their machine.
 */
public final class BoilerBlockEntity extends RecipeMachineBlockEntity {
    public static final Identifier MACHINE = Identifier.fromNamespaceAndPath(OutworldIndustry.MOD_ID, "boiler");
    private static final Layout LAYOUT = new Layout(MACHINE, 1, 0, 1, 0, 0, 1, 8, 0, false);

    public BoilerBlockEntity(BlockPos pos, BlockState state) {
        super(IndustryContent.BOILER.get(), pos, state, LAYOUT);
    }
}
