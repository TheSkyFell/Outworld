package io.github.theskyfell.outworld.industry.pipe;

import io.github.theskyfell.outworld.core.fluid.FluidAmount;
import io.github.theskyfell.outworld.core.fluid.FluidConductor;
import io.github.theskyfell.outworld.core.fluid.FluidGrids;
import io.github.theskyfell.outworld.core.fluid.FluidTank;
import io.github.theskyfell.outworld.core.fluid.FluidUnits;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;

/**
 * A fluid tank: a block of fluid pipe that holds {@link #CAPACITY}. Tanks and pipes that touch share
 * one grid, so stacking tanks makes a bigger tank, and what it holds is saved with the world. Pour a
 * bucket in or fill an empty one by right-clicking it; with an empty hand it shows what it holds.
 */
public final class FluidTankBlock extends Block implements FluidConductor {
    /** What each tank block adds to its grid, in droplets (32 buckets). */
    public static final long CAPACITY = 32 * FluidUnits.BUCKET;

    public FluidTankBlock(Properties properties) {
        super(properties);
    }

    @Override
    public long fluidGridCapacity() {
        return CAPACITY;
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        Fluid poured = bucketFluid(stack);
        if (poured == null && !stack.is(Items.BUCKET)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }
        if (!(level instanceof ServerLevel serverLevel)) {
            return InteractionResult.SUCCESS;
        }
        FluidTank tank = FluidGrids.tankAt(serverLevel, pos);
        if (tank == null) {
            return InteractionResult.FAIL;
        }
        if (poured != null) {
            if (tank.insert(poured, DataComponentPatch.EMPTY, FluidUnits.BUCKET, true) < FluidUnits.BUCKET) {
                return InteractionResult.FAIL;
            }
            tank.insert(poured, DataComponentPatch.EMPTY, FluidUnits.BUCKET, false);
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
            return InteractionResult.SUCCESS;
        }
        FluidAmount contents = tank.getContents();
        Item filled = contents.isEmpty() ? Items.AIR : contents.fluid().getBucket();
        if (filled == Items.AIR || contents.amount() < FluidUnits.BUCKET || !contents.components().isEmpty()) {
            return InteractionResult.FAIL;
        }
        tank.extract(fluid -> fluid == contents.fluid(), FluidUnits.BUCKET, false);
        player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, new ItemStack(filled)));
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            serverPlayer.sendSystemMessage(GridInfo.fluid(FluidGrids.tankAt(serverLevel, pos)), true);
        }
        return InteractionResult.SUCCESS;
    }

    /** The source fluid this bucket holds, or null if it isn't a bucket of fluid. */
    private static Fluid bucketFluid(ItemStack stack) {
        if (stack.isEmpty() || stack.is(Items.BUCKET)) {
            return null;
        }
        for (Fluid fluid : BuiltInRegistries.FLUID) {
            if (fluid != Fluids.EMPTY && fluid.getBucket() == stack.getItem() && fluid.isSource(fluid.defaultFluidState())) {
                return fluid;
            }
        }
        return null;
    }
}
