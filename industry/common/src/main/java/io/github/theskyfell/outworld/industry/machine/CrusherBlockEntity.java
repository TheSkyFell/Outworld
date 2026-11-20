package io.github.theskyfell.outworld.industry.machine;

import io.github.theskyfell.outworld.core.machine.RecipeMachineBlockEntity;
import io.github.theskyfell.outworld.industry.IndustryContent;
import io.github.theskyfell.outworld.industry.OutworldIndustry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The crusher: grinds raw ore into crushed ore, two for one, which then smelts into ingots. The first
 * step of every ore processing chain. Its recipes name {@link #MACHINE} as their machine.
 */
public final class CrusherBlockEntity extends RecipeMachineBlockEntity {
    public static final Identifier MACHINE = Identifier.fromNamespaceAndPath(OutworldIndustry.MOD_ID, "crusher");
    private static final Layout LAYOUT = Layout.items(MACHINE, 1, 2, 10_000);

    public CrusherBlockEntity(BlockPos pos, BlockState state) {
        super(IndustryContent.CRUSHER.get(), pos, state, LAYOUT);
    }
}
