package io.github.theskyfell.outworld.industry.power;

import io.github.theskyfell.outworld.core.machine.RecipeMachineBlockEntity;
import io.github.theskyfell.outworld.industry.IndustryContent;
import io.github.theskyfell.outworld.industry.OutworldIndustry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The combustion generator: early power from solid fuel (coal, charcoal, blaze rods) or liquid fuel
 * (lava) in its tank. Its fuels are recipes naming {@link #MACHINE}, whose {@code energy} is what it
 * makes per tick of burning. It pushes its energy into neighbouring cables, batteries and machines.
 */
public final class CombustionGeneratorBlockEntity extends RecipeMachineBlockEntity {
    public static final Identifier MACHINE = Identifier.fromNamespaceAndPath(OutworldIndustry.MOD_ID, "combustion_generator");
    private static final Layout LAYOUT = new Layout(MACHINE, 1, 0, 1, 0, 0, 0, 4, 20_000, true);

    public CombustionGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(IndustryContent.COMBUSTION_GENERATOR.get(), pos, state, LAYOUT);
    }
}
