package io.github.theskyfell.outworld.industry;

import io.github.theskyfell.outworld.core.menu.Menus;
import io.github.theskyfell.outworld.core.registry.BlockEntityTypes;
import io.github.theskyfell.outworld.core.registry.Registrar;
import io.github.theskyfell.outworld.core.registry.RegistryEntry;
import io.github.theskyfell.outworld.industry.machine.CrusherBlock;
import io.github.theskyfell.outworld.industry.machine.CrusherBlockEntity;
import io.github.theskyfell.outworld.industry.test.TestBatteryBlock;
import io.github.theskyfell.outworld.industry.test.TestBatteryBlockEntity;
import io.github.theskyfell.outworld.industry.test.TestCrateBlock;
import io.github.theskyfell.outworld.industry.test.TestCrateBlockEntity;
import io.github.theskyfell.outworld.industry.test.TestGasTankBlock;
import io.github.theskyfell.outworld.industry.test.TestGasTankBlockEntity;
import io.github.theskyfell.outworld.industry.test.TestMachineBlock;
import io.github.theskyfell.outworld.industry.test.TestMachineBlockEntity;
import io.github.theskyfell.outworld.industry.test.TestMachineMenu;
import io.github.theskyfell.outworld.industry.test.TestMultiblockBlock;
import io.github.theskyfell.outworld.industry.test.TestMultiblockBlockEntity;
import io.github.theskyfell.outworld.industry.test.TestTankBlock;
import io.github.theskyfell.outworld.industry.test.TestTankBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/** Industry's blocks, items and menus. */
public final class IndustryContent {
    public static final Registrar<Block> BLOCKS = Registrar.create(Registries.BLOCK, OutworldIndustry.MOD_ID);
    public static final Registrar<Item> ITEMS = Registrar.create(Registries.ITEM, OutworldIndustry.MOD_ID);
    public static final Registrar<BlockEntityType<?>> BLOCK_ENTITIES = Registrar.create(Registries.BLOCK_ENTITY_TYPE, OutworldIndustry.MOD_ID);
    public static final Registrar<MenuType<?>> MENUS = Registrar.create(Registries.MENU, OutworldIndustry.MOD_ID);

    // Machines

    public static final RegistryEntry<Block, CrusherBlock> CRUSHER_BLOCK = BLOCKS.register("crusher",
            key -> new CrusherBlock(BlockBehaviour.Properties.of().setId(key).strength(3.5F).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    public static final RegistryEntry<Item, BlockItem> CRUSHER_ITEM = ITEMS.register("crusher",
            key -> new BlockItem(CRUSHER_BLOCK.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));

    public static final RegistryEntry<BlockEntityType<?>, BlockEntityType<CrusherBlockEntity>> CRUSHER = BLOCK_ENTITIES.register("crusher",
            key -> BlockEntityTypes.create(CrusherBlockEntity::new, CRUSHER_BLOCK.get()));

    // Materials

    public static final RegistryEntry<Item, Item> CRUSHED_IRON = ITEMS.register("crushed_iron",
            key -> new Item(new Item.Properties().setId(key)));
    public static final RegistryEntry<Item, Item> CRUSHED_COPPER = ITEMS.register("crushed_copper",
            key -> new Item(new Item.Properties().setId(key)));
    public static final RegistryEntry<Item, Item> CRUSHED_GOLD = ITEMS.register("crushed_gold",
            key -> new Item(new Item.Properties().setId(key)));

    // Temporary test blocks

    /**
     * Temporary: opens a menu that pings the server, which tests registration, menus, screens and
     * networking on both loaders. Goes away once real machines exist.
     */
    public static final RegistryEntry<Block, TestMachineBlock> TEST_BLOCK = BLOCKS.register("test_block",
            key -> new TestMachineBlock(BlockBehaviour.Properties.of().setId(key).strength(3.0F).sound(SoundType.METAL)));

    public static final RegistryEntry<Item, BlockItem> TEST_BLOCK_ITEM = ITEMS.register("test_block",
            key -> new BlockItem(TEST_BLOCK.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));

    /** Temporary: starts with 10,000 FE and lends it to the test machine through the loaders' energy APIs. */
    public static final RegistryEntry<Block, TestBatteryBlock> TEST_BATTERY_BLOCK = BLOCKS.register("test_battery",
            key -> new TestBatteryBlock(BlockBehaviour.Properties.of().setId(key).strength(3.0F).sound(SoundType.METAL)));

    public static final RegistryEntry<Item, BlockItem> TEST_BATTERY_ITEM = ITEMS.register("test_battery",
            key -> new BlockItem(TEST_BATTERY_BLOCK.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));

    /** Temporary: three item slots that move items downwards, for testing the loaders' item APIs. */
    public static final RegistryEntry<Block, TestCrateBlock> TEST_CRATE_BLOCK = BLOCKS.register("test_crate",
            key -> new TestCrateBlock(BlockBehaviour.Properties.of().setId(key).strength(2.0F).sound(SoundType.WOOD)));

    public static final RegistryEntry<Item, BlockItem> TEST_CRATE_ITEM = ITEMS.register("test_crate",
            key -> new BlockItem(TEST_CRATE_BLOCK.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));

    /** Temporary: a four-bucket tank that moves fluid upwards, for testing the loaders' fluid APIs. */
    public static final RegistryEntry<Block, TestTankBlock> TEST_TANK_BLOCK = BLOCKS.register("test_tank",
            key -> new TestTankBlock(BlockBehaviour.Properties.of().setId(key).strength(3.0F).sound(SoundType.METAL)));

    public static final RegistryEntry<Item, BlockItem> TEST_TANK_ITEM = ITEMS.register("test_tank",
            key -> new BlockItem(TEST_TANK_BLOCK.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));

    /** Temporary: a multiblock controller that forms a 3x3x3 iron frame, for testing the multiblock framework. */
    public static final RegistryEntry<Block, TestMultiblockBlock> TEST_MULTIBLOCK_BLOCK = BLOCKS.register("test_multiblock",
            key -> new TestMultiblockBlock(BlockBehaviour.Properties.of().setId(key).strength(3.0F).sound(SoundType.METAL)));

    public static final RegistryEntry<Item, BlockItem> TEST_MULTIBLOCK_ITEM = ITEMS.register("test_multiblock",
            key -> new BlockItem(TEST_MULTIBLOCK_BLOCK.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));

    /** Temporary: an eight-bucket gas tank that moves gas upwards, for testing Core's gas storage. */
    public static final RegistryEntry<Block, TestGasTankBlock> TEST_GAS_TANK_BLOCK = BLOCKS.register("test_gas_tank",
            key -> new TestGasTankBlock(BlockBehaviour.Properties.of().setId(key).strength(3.0F).sound(SoundType.METAL)));

    public static final RegistryEntry<Item, BlockItem> TEST_GAS_TANK_ITEM = ITEMS.register("test_gas_tank",
            key -> new BlockItem(TEST_GAS_TANK_BLOCK.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));

    public static final RegistryEntry<BlockEntityType<?>, BlockEntityType<TestMachineBlockEntity>> TEST_MACHINE = BLOCK_ENTITIES.register("test_block",
            key -> BlockEntityTypes.create(TestMachineBlockEntity::new, TEST_BLOCK.get()));

    public static final RegistryEntry<BlockEntityType<?>, BlockEntityType<TestBatteryBlockEntity>> TEST_BATTERY = BLOCK_ENTITIES.register("test_battery",
            key -> BlockEntityTypes.create(TestBatteryBlockEntity::new, TEST_BATTERY_BLOCK.get()));

    public static final RegistryEntry<BlockEntityType<?>, BlockEntityType<TestCrateBlockEntity>> TEST_CRATE = BLOCK_ENTITIES.register("test_crate",
            key -> BlockEntityTypes.create(TestCrateBlockEntity::new, TEST_CRATE_BLOCK.get()));

    public static final RegistryEntry<BlockEntityType<?>, BlockEntityType<TestTankBlockEntity>> TEST_TANK = BLOCK_ENTITIES.register("test_tank",
            key -> BlockEntityTypes.create(TestTankBlockEntity::new, TEST_TANK_BLOCK.get()));

    public static final RegistryEntry<BlockEntityType<?>, BlockEntityType<TestMultiblockBlockEntity>> TEST_MULTIBLOCK = BLOCK_ENTITIES.register("test_multiblock",
            key -> BlockEntityTypes.create(TestMultiblockBlockEntity::new, TEST_MULTIBLOCK_BLOCK.get()));

    public static final RegistryEntry<BlockEntityType<?>, BlockEntityType<TestGasTankBlockEntity>> TEST_GAS_TANK = BLOCK_ENTITIES.register("test_gas_tank",
            key -> BlockEntityTypes.create(TestGasTankBlockEntity::new, TEST_GAS_TANK_BLOCK.get()));

    public static final RegistryEntry<MenuType<?>, MenuType<TestMachineMenu>> TEST_MACHINE_MENU = MENUS.register("test_machine",
            key -> Menus.createType(TestMachineMenu::new, BlockPos.STREAM_CODEC));

    private IndustryContent() {
    }

    /** Blocks come first: block items and block entity types look up their blocks when they are created. */
    static List<Registrar<?>> registrars() {
        return List.of(BLOCKS, ITEMS, BLOCK_ENTITIES, MENUS);
    }
}
