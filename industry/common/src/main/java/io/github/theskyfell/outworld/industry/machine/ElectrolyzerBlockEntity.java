package io.github.theskyfell.outworld.industry.machine;

import io.github.theskyfell.outworld.core.machine.RecipeMachineBlockEntity;
import io.github.theskyfell.outworld.industry.IndustryContent;
import io.github.theskyfell.outworld.industry.OutworldIndustry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The electrolyzer: splits water into hydrogen and oxygen gas with a lot of energy. The start of the
 * rocket fuel chain. Water goes in through any mod's fluid storage; the two gases come out of their
 * own tanks, on every side, to gas pipes and tanks. Its recipes name {@link #MACHINE} as their machine.
 */
public final class ElectrolyzerBlockEntity extends RecipeMachineBlockEntity {
    public static final Identifier MACHINE = Identifier.fromNamespaceAndPath(OutworldIndustry.MOD_ID, "electrolyzer");
    private static final Layout LAYOUT = new Layout(MACHINE, 0, 0, 1, 0, 0, 2, 8, 40_000, false);

    public ElectrolyzerBlockEntity(BlockPos pos, BlockState state) {
        super(IndustryContent.ELECTROLYZER.get(), pos, state, LAYOUT);
    }
}
