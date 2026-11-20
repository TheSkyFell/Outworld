package io.github.theskyfell.outworld.core.client;

import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;

/** Connects block entity types to their renderers. Given to {@link OutworldClientModule#registerBlockEntityRenderers}. */
public interface BlockEntityRendererRegistrar {
    <T extends BlockEntity, S extends BlockEntityRenderState> void register(BlockEntityType<? extends T> type,
            BlockEntityRendererProvider<T, S> provider);
}
