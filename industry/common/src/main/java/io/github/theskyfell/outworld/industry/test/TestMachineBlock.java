package io.github.theskyfell.outworld.industry.test;

import io.github.theskyfell.outworld.core.menu.Menus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/** Temporary test block: right-click opens the test machine screen. */
public final class TestMachineBlock extends Block {
    private static final Component TITLE = Component.translatable("container.outworld_industry.test_block");

    public TestMachineBlock(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer serverPlayer) {
            MenuProvider provider = new SimpleMenuProvider((containerId, inventory, opener) ->
                    new TestMachineMenu(containerId, inventory, pos), TITLE);
            Menus.open(serverPlayer, provider, BlockPos.STREAM_CODEC, pos);
        }
        return InteractionResult.SUCCESS;
    }
}
