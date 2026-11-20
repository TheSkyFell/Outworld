package io.github.theskyfell.outworld.industry;

import io.github.theskyfell.outworld.core.registry.Registrar;
import io.github.theskyfell.outworld.core.registry.RegistryEntry;
import java.util.List;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Industry's blocks and items. */
public final class IndustryContent {
    public static final Registrar<Block> BLOCKS = Registrar.create(Registries.BLOCK, OutworldIndustry.MOD_ID);
    public static final Registrar<Item> ITEMS = Registrar.create(Registries.ITEM, OutworldIndustry.MOD_ID);

    /** Temporary: proves registration works on both loaders. Goes away once real machines exist. */
    public static final RegistryEntry<Block, Block> TEST_BLOCK = BLOCKS.register("test_block",
            key -> new Block(BlockBehaviour.Properties.of().setId(key).strength(3.0F).sound(SoundType.METAL)));

    public static final RegistryEntry<Item, BlockItem> TEST_BLOCK_ITEM = ITEMS.register("test_block",
            key -> new BlockItem(TEST_BLOCK.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));

    private IndustryContent() {
    }

    /** Blocks come first: a block item looks up its block when it is created. */
    static List<Registrar<?>> registrars() {
        return List.of(BLOCKS, ITEMS);
    }
}
