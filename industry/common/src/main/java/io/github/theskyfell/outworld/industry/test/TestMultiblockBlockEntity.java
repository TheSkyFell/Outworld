package io.github.theskyfell.outworld.industry.test;

import io.github.theskyfell.outworld.core.multiblock.MultiblockControllerBlockEntity;
import io.github.theskyfell.outworld.core.multiblock.MultiblockStructure;
import io.github.theskyfell.outworld.industry.IndustryContent;
import io.github.theskyfell.outworld.industry.OutworldIndustry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * Temporary test multiblock: forms {@link #STRUCTURE} (a 3x3x3 iron shell around an air block, with
 * the controller in the middle of its front face) and does nothing else. It counts how often the
 * structure has formed, so tests can read it with {@code /data get block}.
 */
public final class TestMultiblockBlockEntity extends MultiblockControllerBlockEntity {
    public static final ResourceKey<MultiblockStructure> STRUCTURE = ResourceKey.create(MultiblockStructure.REGISTRY,
            Identifier.fromNamespaceAndPath(OutworldIndustry.MOD_ID, "test_frame"));

    private int timesFormed;

    public TestMultiblockBlockEntity(BlockPos pos, BlockState state) {
        super(IndustryContent.TEST_MULTIBLOCK.get(), pos, state);
    }

    @Override
    public ResourceKey<MultiblockStructure> structureKey() {
        return STRUCTURE;
    }

    @Override
    protected void onFormed(MultiblockStructure.Match match) {
        timesFormed++;
        setChanged();
    }

    @Override
    protected boolean workFormed(ServerLevel level, int elapsedTicks) {
        return false;
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putInt("times_formed", timesFormed);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        timesFormed = input.getIntOr("times_formed", 0);
    }
}
