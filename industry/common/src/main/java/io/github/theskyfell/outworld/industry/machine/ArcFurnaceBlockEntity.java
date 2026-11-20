package io.github.theskyfell.outworld.industry.machine;

import io.github.theskyfell.outworld.core.machine.MultiblockRecipeMachineBlockEntity;
import io.github.theskyfell.outworld.core.multiblock.MultiblockStructure;
import io.github.theskyfell.outworld.industry.IndustryContent;
import io.github.theskyfell.outworld.industry.OutworldIndustry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The arc furnace: a 3x3x3 multiblock of refractory bricks around a hollow chamber, with an iron block
 * electrode in the middle of the roof and this controller in the middle of the front. It melts what an
 * ordinary furnace can't: iron and coal into steel, purified titanium into titanium ingots. It uses a
 * lot of energy, through the controller like its items. Its recipes name {@link #MACHINE} as their
 * machine; its shape is {@code data/outworld_industry/outworld/multiblock/arc_furnace.json}.
 */
public final class ArcFurnaceBlockEntity extends MultiblockRecipeMachineBlockEntity {
    public static final Identifier MACHINE = Identifier.fromNamespaceAndPath(OutworldIndustry.MOD_ID, "arc_furnace");
    public static final ResourceKey<MultiblockStructure> STRUCTURE = ResourceKey.create(MultiblockStructure.REGISTRY, MACHINE);
    private static final Layout LAYOUT = Layout.items(MACHINE, 2, 1, 100_000);

    public ArcFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(IndustryContent.ARC_FURNACE.get(), pos, state, LAYOUT);
    }

    @Override
    public ResourceKey<MultiblockStructure> structureKey() {
        return STRUCTURE;
    }
}
