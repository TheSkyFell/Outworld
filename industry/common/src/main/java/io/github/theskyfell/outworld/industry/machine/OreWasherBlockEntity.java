package io.github.theskyfell.outworld.industry.machine;

import io.github.theskyfell.outworld.core.machine.RecipeMachineBlockEntity;
import io.github.theskyfell.outworld.industry.IndustryContent;
import io.github.theskyfell.outworld.industry.OutworldIndustry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The ore washer: rinses crushed ore with water into purified ore, three for every two, which smelts
 * into ingots. The second step of the ore chain after the crusher. Water goes in through pipes,
 * buckets or any mod's fluid storage. Its recipes name {@link #MACHINE} as their machine.
 */
public final class OreWasherBlockEntity extends RecipeMachineBlockEntity {
    public static final Identifier MACHINE = Identifier.fromNamespaceAndPath(OutworldIndustry.MOD_ID, "ore_washer");
    private static final Layout LAYOUT = new Layout(MACHINE, 1, 1, 1, 0, 0, 0, 4, 10_000, false);

    public OreWasherBlockEntity(BlockPos pos, BlockState state) {
        super(IndustryContent.ORE_WASHER.get(), pos, state, LAYOUT);
    }
}
