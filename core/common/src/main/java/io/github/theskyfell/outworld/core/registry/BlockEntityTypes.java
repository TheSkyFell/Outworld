package io.github.theskyfell.outworld.core.registry;

import io.github.theskyfell.outworld.core.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Creates block entity types. Vanilla's constructor isn't public, and each loader opens it differently.
 * Register the result through a {@link Registrar} for {@code Registries.BLOCK_ENTITY_TYPE}, after the
 * blocks it is for.
 */
public final class BlockEntityTypes {
    private BlockEntityTypes() {
    }

    public static <T extends BlockEntity> BlockEntityType<T> create(Factory<T> factory, Block... blocks) {
        return Services.PLATFORM.createBlockEntityType(factory, blocks);
    }

    /** Usually a block entity's constructor: {@code MyMachineBlockEntity::new}. */
    @FunctionalInterface
    public interface Factory<T extends BlockEntity> {
        T create(BlockPos pos, BlockState state);
    }
}
