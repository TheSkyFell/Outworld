package io.github.theskyfell.outworld.core.fluid;

import io.github.theskyfell.outworld.core.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A fluid that only lives in tanks and pipes: it can't be placed in the world and has no bucket, like
 * liquid hydrogen. Create one with {@link #create} inside a {@code Registrar} for
 * {@code Registries.FLUID}; each loader adds what it needs (NeoForge a fluid type). Its name is the
 * translation key {@code fluid.<namespace>.<path>}.
 */
public class OutworldFluid extends Fluid {
    private final int color;

    /** Use {@link #create}; the loader layer may hand out a subclass. */
    protected OutworldFluid(int color) {
        this.color = color;
    }

    /** A new fluid for this registry key, coloured {@code color} (0xRRGGBB) in tank gauges. */
    public static Fluid create(ResourceKey<Fluid> key, int color) {
        return Services.PLATFORM.createFluid(key, color);
    }

    /** For loader layers only: the common fluid, for loaders that need nothing more. */
    public static OutworldFluid plain(int color) {
        return new OutworldFluid(color);
    }

    /** The colour shown in tank gauges, 0xRRGGBB. */
    public int color() {
        return color;
    }

    @Override
    public Item getBucket() {
        return Items.AIR;
    }

    @Override
    protected boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid other, Direction direction) {
        return true;
    }

    @Override
    protected Vec3 getFlow(BlockGetter level, BlockPos pos, FluidState fluidState) {
        return Vec3.ZERO;
    }

    @Override
    public int getTickDelay(LevelReader level) {
        return 0;
    }

    @Override
    protected float getExplosionResistance() {
        return 0;
    }

    @Override
    public float getHeight(FluidState fluidState, BlockGetter level, BlockPos pos) {
        return 0;
    }

    @Override
    public float getOwnHeight(FluidState fluidState) {
        return 0;
    }

    @Override
    protected BlockState createLegacyBlock(FluidState fluidState) {
        return Blocks.AIR.defaultBlockState();
    }

    @Override
    public boolean isSource(FluidState fluidState) {
        return true;
    }

    @Override
    public int getAmount(FluidState fluidState) {
        return 8;
    }

    @Override
    public VoxelShape getShape(FluidState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }
}
