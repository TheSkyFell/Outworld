package io.github.theskyfell.outworld.industry;

import io.github.theskyfell.outworld.core.fluid.OutworldFluid;
import io.github.theskyfell.outworld.core.menu.Menus;
import io.github.theskyfell.outworld.core.registry.BlockEntityTypes;
import io.github.theskyfell.outworld.core.registry.CreativeTabs;
import io.github.theskyfell.outworld.core.registry.Registrar;
import io.github.theskyfell.outworld.core.registry.RegistryEntry;
import io.github.theskyfell.outworld.industry.logistics.BeltBlock;
import io.github.theskyfell.outworld.industry.logistics.BeltBlockEntity;
import io.github.theskyfell.outworld.industry.logistics.InserterBlock;
import io.github.theskyfell.outworld.industry.logistics.InserterBlockEntity;
import io.github.theskyfell.outworld.industry.machine.CrusherBlock;
import io.github.theskyfell.outworld.industry.machine.CrusherBlockEntity;
import io.github.theskyfell.outworld.industry.machine.ElectricFurnaceBlock;
import io.github.theskyfell.outworld.industry.machine.ElectricFurnaceBlockEntity;
import io.github.theskyfell.outworld.industry.machine.ElectrolyzerBlock;
import io.github.theskyfell.outworld.industry.machine.ElectrolyzerBlockEntity;
import io.github.theskyfell.outworld.industry.machine.GasLiquefierBlock;
import io.github.theskyfell.outworld.industry.machine.GasLiquefierBlockEntity;
import io.github.theskyfell.outworld.industry.machine.OreWasherBlock;
import io.github.theskyfell.outworld.industry.machine.OreWasherBlockEntity;
import io.github.theskyfell.outworld.industry.pipe.FluidPipeBlock;
import io.github.theskyfell.outworld.industry.pipe.FluidTankBlock;
import io.github.theskyfell.outworld.industry.pipe.GasPipeBlock;
import io.github.theskyfell.outworld.industry.pipe.GasTankBlock;
import io.github.theskyfell.outworld.industry.power.BatteryBlock;
import io.github.theskyfell.outworld.industry.power.BoilerBlock;
import io.github.theskyfell.outworld.industry.power.BoilerBlockEntity;
import io.github.theskyfell.outworld.industry.power.CableBlock;
import io.github.theskyfell.outworld.industry.power.CombustionGeneratorBlock;
import io.github.theskyfell.outworld.industry.power.CombustionGeneratorBlockEntity;
import io.github.theskyfell.outworld.industry.power.SolarPanelBlock;
import io.github.theskyfell.outworld.industry.power.SolarPanelBlockEntity;
import io.github.theskyfell.outworld.industry.power.SteamTurbineBlock;
import io.github.theskyfell.outworld.industry.power.SteamTurbineBlockEntity;
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
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;

/** Industry's blocks, items and menus. */
public final class IndustryContent {
    public static final Registrar<Fluid> FLUIDS = Registrar.create(Registries.FLUID, OutworldIndustry.MOD_ID);
    public static final Registrar<Block> BLOCKS = Registrar.create(Registries.BLOCK, OutworldIndustry.MOD_ID);
    public static final Registrar<Item> ITEMS = Registrar.create(Registries.ITEM, OutworldIndustry.MOD_ID);
    public static final Registrar<BlockEntityType<?>> BLOCK_ENTITIES = Registrar.create(Registries.BLOCK_ENTITY_TYPE, OutworldIndustry.MOD_ID);
    public static final Registrar<MenuType<?>> MENUS = Registrar.create(Registries.MENU, OutworldIndustry.MOD_ID);
    public static final Registrar<CreativeModeTab> TABS = Registrar.create(Registries.CREATIVE_MODE_TAB, OutworldIndustry.MOD_ID);

    // Machines

    public static final RegistryEntry<Block, CrusherBlock> CRUSHER_BLOCK = BLOCKS.register("crusher",
            key -> new CrusherBlock(BlockBehaviour.Properties.of().setId(key).strength(3.5F).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    public static final RegistryEntry<Item, BlockItem> CRUSHER_ITEM = ITEMS.register("crusher",
            key -> new BlockItem(CRUSHER_BLOCK.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));

    public static final RegistryEntry<BlockEntityType<?>, BlockEntityType<CrusherBlockEntity>> CRUSHER = BLOCK_ENTITIES.register("crusher",
            key -> BlockEntityTypes.create(CrusherBlockEntity::new, CRUSHER_BLOCK.get()));

    public static final RegistryEntry<Block, ElectricFurnaceBlock> ELECTRIC_FURNACE_BLOCK = BLOCKS.register("electric_furnace",
            key -> new ElectricFurnaceBlock(BlockBehaviour.Properties.of().setId(key).strength(3.5F).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    public static final RegistryEntry<Item, BlockItem> ELECTRIC_FURNACE_ITEM = ITEMS.register("electric_furnace",
            key -> new BlockItem(ELECTRIC_FURNACE_BLOCK.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));

    public static final RegistryEntry<BlockEntityType<?>, BlockEntityType<ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE = BLOCK_ENTITIES.register("electric_furnace",
            key -> BlockEntityTypes.create(ElectricFurnaceBlockEntity::new, ELECTRIC_FURNACE_BLOCK.get()));

    public static final RegistryEntry<Block, OreWasherBlock> ORE_WASHER_BLOCK = BLOCKS.register("ore_washer",
            key -> new OreWasherBlock(BlockBehaviour.Properties.of().setId(key).strength(3.5F).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    public static final RegistryEntry<Item, BlockItem> ORE_WASHER_ITEM = ITEMS.register("ore_washer",
            key -> new BlockItem(ORE_WASHER_BLOCK.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));

    public static final RegistryEntry<BlockEntityType<?>, BlockEntityType<OreWasherBlockEntity>> ORE_WASHER = BLOCK_ENTITIES.register("ore_washer",
            key -> BlockEntityTypes.create(OreWasherBlockEntity::new, ORE_WASHER_BLOCK.get()));

    public static final RegistryEntry<Block, ElectrolyzerBlock> ELECTROLYZER_BLOCK = BLOCKS.register("electrolyzer",
            key -> new ElectrolyzerBlock(BlockBehaviour.Properties.of().setId(key).strength(3.5F).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    public static final RegistryEntry<Item, BlockItem> ELECTROLYZER_ITEM = ITEMS.register("electrolyzer",
            key -> new BlockItem(ELECTROLYZER_BLOCK.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));

    public static final RegistryEntry<BlockEntityType<?>, BlockEntityType<ElectrolyzerBlockEntity>> ELECTROLYZER = BLOCK_ENTITIES.register("electrolyzer",
            key -> BlockEntityTypes.create(ElectrolyzerBlockEntity::new, ELECTROLYZER_BLOCK.get()));

    // Power

    public static final RegistryEntry<Block, CombustionGeneratorBlock> COMBUSTION_GENERATOR_BLOCK = BLOCKS.register("combustion_generator",
            key -> new CombustionGeneratorBlock(BlockBehaviour.Properties.of().setId(key).strength(3.5F).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    public static final RegistryEntry<Item, BlockItem> COMBUSTION_GENERATOR_ITEM = ITEMS.register("combustion_generator",
            key -> new BlockItem(COMBUSTION_GENERATOR_BLOCK.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));

    public static final RegistryEntry<BlockEntityType<?>, BlockEntityType<CombustionGeneratorBlockEntity>> COMBUSTION_GENERATOR =
            BLOCK_ENTITIES.register("combustion_generator",
                    key -> BlockEntityTypes.create(CombustionGeneratorBlockEntity::new, COMBUSTION_GENERATOR_BLOCK.get()));

    public static final RegistryEntry<Block, BoilerBlock> BOILER_BLOCK = BLOCKS.register("boiler",
            key -> new BoilerBlock(BlockBehaviour.Properties.of().setId(key).strength(3.5F).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    public static final RegistryEntry<Item, BlockItem> BOILER_ITEM = ITEMS.register("boiler",
            key -> new BlockItem(BOILER_BLOCK.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));

    public static final RegistryEntry<BlockEntityType<?>, BlockEntityType<BoilerBlockEntity>> BOILER = BLOCK_ENTITIES.register("boiler",
            key -> BlockEntityTypes.create(BoilerBlockEntity::new, BOILER_BLOCK.get()));

    public static final RegistryEntry<Block, SteamTurbineBlock> STEAM_TURBINE_BLOCK = BLOCKS.register("steam_turbine",
            key -> new SteamTurbineBlock(BlockBehaviour.Properties.of().setId(key).strength(3.5F).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    public static final RegistryEntry<Item, BlockItem> STEAM_TURBINE_ITEM = ITEMS.register("steam_turbine",
            key -> new BlockItem(STEAM_TURBINE_BLOCK.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));

    public static final RegistryEntry<BlockEntityType<?>, BlockEntityType<SteamTurbineBlockEntity>> STEAM_TURBINE = BLOCK_ENTITIES.register("steam_turbine",
            key -> BlockEntityTypes.create(SteamTurbineBlockEntity::new, STEAM_TURBINE_BLOCK.get()));

    public static final RegistryEntry<Block, SolarPanelBlock> SOLAR_PANEL_BLOCK = BLOCKS.register("solar_panel",
            key -> new SolarPanelBlock(BlockBehaviour.Properties.of().setId(key).strength(2.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    public static final RegistryEntry<Item, BlockItem> SOLAR_PANEL_ITEM = ITEMS.register("solar_panel",
            key -> new BlockItem(SOLAR_PANEL_BLOCK.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));

    public static final RegistryEntry<BlockEntityType<?>, BlockEntityType<SolarPanelBlockEntity>> SOLAR_PANEL = BLOCK_ENTITIES.register("solar_panel",
            key -> BlockEntityTypes.create(SolarPanelBlockEntity::new, SOLAR_PANEL_BLOCK.get()));

    public static final RegistryEntry<Block, BatteryBlock> BATTERY_BLOCK = BLOCKS.register("battery",
            key -> new BatteryBlock(BlockBehaviour.Properties.of().setId(key).strength(3.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    public static final RegistryEntry<Item, BlockItem> BATTERY_ITEM = ITEMS.register("battery",
            key -> new BlockItem(BATTERY_BLOCK.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));

    public static final RegistryEntry<Block, CableBlock> CABLE_BLOCK = BLOCKS.register("cable",
            key -> new CableBlock(BlockBehaviour.Properties.of().setId(key).strength(0.5F).sound(SoundType.WOOL).noOcclusion()));

    public static final RegistryEntry<Item, BlockItem> CABLE_ITEM = ITEMS.register("cable",
            key -> new BlockItem(CABLE_BLOCK.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));

    public static final RegistryEntry<Block, GasLiquefierBlock> GAS_LIQUEFIER_BLOCK = BLOCKS.register("gas_liquefier",
            key -> new GasLiquefierBlock(BlockBehaviour.Properties.of().setId(key).strength(3.5F).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    public static final RegistryEntry<Item, BlockItem> GAS_LIQUEFIER_ITEM = ITEMS.register("gas_liquefier",
            key -> new BlockItem(GAS_LIQUEFIER_BLOCK.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));

    public static final RegistryEntry<BlockEntityType<?>, BlockEntityType<GasLiquefierBlockEntity>> GAS_LIQUEFIER = BLOCK_ENTITIES.register("gas_liquefier",
            key -> BlockEntityTypes.create(GasLiquefierBlockEntity::new, GAS_LIQUEFIER_BLOCK.get()));

    // Rocket propellants: liquids that only live in tanks and pipes

    public static final RegistryEntry<Fluid, Fluid> LIQUID_HYDROGEN = FLUIDS.register("liquid_hydrogen",
            key -> OutworldFluid.create(key, 0xc8ecff));

    public static final RegistryEntry<Fluid, Fluid> LIQUID_OXYGEN = FLUIDS.register("liquid_oxygen",
            key -> OutworldFluid.create(key, 0x8fb8ff));

    // Logistics

    public static final RegistryEntry<Block, BeltBlock> BELT_BLOCK = BLOCKS.register("belt",
            key -> new BeltBlock(BlockBehaviour.Properties.of().setId(key).strength(1.5F).sound(SoundType.METAL).noOcclusion()));

    public static final RegistryEntry<Item, BlockItem> BELT_ITEM = ITEMS.register("belt",
            key -> new BlockItem(BELT_BLOCK.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));

    public static final RegistryEntry<BlockEntityType<?>, BlockEntityType<BeltBlockEntity>> BELT = BLOCK_ENTITIES.register("belt",
            key -> BlockEntityTypes.create(BeltBlockEntity::new, BELT_BLOCK.get()));

    public static final RegistryEntry<Block, InserterBlock> INSERTER_BLOCK = BLOCKS.register("inserter",
            key -> new InserterBlock(BlockBehaviour.Properties.of().setId(key).strength(2.0F).sound(SoundType.METAL).noOcclusion()));

    public static final RegistryEntry<Item, BlockItem> INSERTER_ITEM = ITEMS.register("inserter",
            key -> new BlockItem(INSERTER_BLOCK.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));

    public static final RegistryEntry<BlockEntityType<?>, BlockEntityType<InserterBlockEntity>> INSERTER = BLOCK_ENTITIES.register("inserter",
            key -> BlockEntityTypes.create(InserterBlockEntity::new, INSERTER_BLOCK.get()));

    // Pipes and tanks

    public static final RegistryEntry<Block, FluidPipeBlock> FLUID_PIPE_BLOCK = BLOCKS.register("fluid_pipe",
            key -> new FluidPipeBlock(BlockBehaviour.Properties.of().setId(key).strength(1.0F).sound(SoundType.COPPER).noOcclusion()));

    public static final RegistryEntry<Item, BlockItem> FLUID_PIPE_ITEM = ITEMS.register("fluid_pipe",
            key -> new BlockItem(FLUID_PIPE_BLOCK.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));

    public static final RegistryEntry<Block, GasPipeBlock> GAS_PIPE_BLOCK = BLOCKS.register("gas_pipe",
            key -> new GasPipeBlock(BlockBehaviour.Properties.of().setId(key).strength(1.0F).sound(SoundType.METAL).noOcclusion()));

    public static final RegistryEntry<Item, BlockItem> GAS_PIPE_ITEM = ITEMS.register("gas_pipe",
            key -> new BlockItem(GAS_PIPE_BLOCK.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));

    public static final RegistryEntry<Block, FluidTankBlock> FLUID_TANK_BLOCK = BLOCKS.register("fluid_tank",
            key -> new FluidTankBlock(BlockBehaviour.Properties.of().setId(key).strength(3.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    public static final RegistryEntry<Item, BlockItem> FLUID_TANK_ITEM = ITEMS.register("fluid_tank",
            key -> new BlockItem(FLUID_TANK_BLOCK.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));

    public static final RegistryEntry<Block, GasTankBlock> GAS_TANK_BLOCK = BLOCKS.register("gas_tank",
            key -> new GasTankBlock(BlockBehaviour.Properties.of().setId(key).strength(3.0F).sound(SoundType.METAL).requiresCorrectToolForDrops()));

    public static final RegistryEntry<Item, BlockItem> GAS_TANK_ITEM = ITEMS.register("gas_tank",
            key -> new BlockItem(GAS_TANK_BLOCK.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));

    // Materials

    public static final RegistryEntry<Item, Item> CRUSHED_IRON = ITEMS.register("crushed_iron",
            key -> new Item(new Item.Properties().setId(key)));
    public static final RegistryEntry<Item, Item> CRUSHED_COPPER = ITEMS.register("crushed_copper",
            key -> new Item(new Item.Properties().setId(key)));
    public static final RegistryEntry<Item, Item> CRUSHED_GOLD = ITEMS.register("crushed_gold",
            key -> new Item(new Item.Properties().setId(key)));
    public static final RegistryEntry<Item, Item> PURIFIED_IRON = ITEMS.register("purified_iron",
            key -> new Item(new Item.Properties().setId(key)));
    public static final RegistryEntry<Item, Item> PURIFIED_COPPER = ITEMS.register("purified_copper",
            key -> new Item(new Item.Properties().setId(key)));
    public static final RegistryEntry<Item, Item> PURIFIED_GOLD = ITEMS.register("purified_gold",
            key -> new Item(new Item.Properties().setId(key)));

    // Aluminium and titanium: ores, raw ore, the ore chain's crushed and purified forms, ingots and blocks.
    // Aluminium only smelts in the electric furnace; titanium will need the arc furnace.

    public static final RegistryEntry<Block, Block> BAUXITE_ORE = BLOCKS.register("bauxite_ore",
            key -> new Block(BlockBehaviour.Properties.of().setId(key).mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(3.0F, 3.0F)));

    public static final RegistryEntry<Item, BlockItem> BAUXITE_ORE_ITEM = ITEMS.register("bauxite_ore",
            key -> new BlockItem(BAUXITE_ORE.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));

    public static final RegistryEntry<Block, Block> TITANIUM_ORE = BLOCKS.register("titanium_ore",
            key -> new Block(BlockBehaviour.Properties.of().setId(key).mapColor(MapColor.STONE).requiresCorrectToolForDrops().strength(3.0F, 3.0F)));

    public static final RegistryEntry<Item, BlockItem> TITANIUM_ORE_ITEM = ITEMS.register("titanium_ore",
            key -> new BlockItem(TITANIUM_ORE.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));

    public static final RegistryEntry<Block, Block> DEEPSLATE_TITANIUM_ORE = BLOCKS.register("deepslate_titanium_ore",
            key -> new Block(BlockBehaviour.Properties.of().setId(key).mapColor(MapColor.DEEPSLATE).requiresCorrectToolForDrops().strength(4.5F, 3.0F).sound(SoundType.DEEPSLATE)));

    public static final RegistryEntry<Item, BlockItem> DEEPSLATE_TITANIUM_ORE_ITEM = ITEMS.register("deepslate_titanium_ore",
            key -> new BlockItem(DEEPSLATE_TITANIUM_ORE.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));

    public static final RegistryEntry<Block, Block> ALUMINIUM_BLOCK = BLOCKS.register("aluminium_block",
            key -> new Block(BlockBehaviour.Properties.of().setId(key).mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.METAL)));

    public static final RegistryEntry<Item, BlockItem> ALUMINIUM_BLOCK_ITEM = ITEMS.register("aluminium_block",
            key -> new BlockItem(ALUMINIUM_BLOCK.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));

    public static final RegistryEntry<Block, Block> TITANIUM_BLOCK = BLOCKS.register("titanium_block",
            key -> new Block(BlockBehaviour.Properties.of().setId(key).mapColor(MapColor.METAL).requiresCorrectToolForDrops().strength(5.0F, 6.0F).sound(SoundType.METAL)));

    public static final RegistryEntry<Item, BlockItem> TITANIUM_BLOCK_ITEM = ITEMS.register("titanium_block",
            key -> new BlockItem(TITANIUM_BLOCK.get(), new Item.Properties().setId(key).useBlockDescriptionPrefix()));

    public static final RegistryEntry<Item, Item> RAW_BAUXITE = ITEMS.register("raw_bauxite",
            key -> new Item(new Item.Properties().setId(key)));
    public static final RegistryEntry<Item, Item> CRUSHED_BAUXITE = ITEMS.register("crushed_bauxite",
            key -> new Item(new Item.Properties().setId(key)));
    public static final RegistryEntry<Item, Item> PURIFIED_BAUXITE = ITEMS.register("purified_bauxite",
            key -> new Item(new Item.Properties().setId(key)));
    public static final RegistryEntry<Item, Item> ALUMINIUM_INGOT = ITEMS.register("aluminium_ingot",
            key -> new Item(new Item.Properties().setId(key)));
    public static final RegistryEntry<Item, Item> RAW_TITANIUM = ITEMS.register("raw_titanium",
            key -> new Item(new Item.Properties().setId(key)));
    public static final RegistryEntry<Item, Item> CRUSHED_TITANIUM = ITEMS.register("crushed_titanium",
            key -> new Item(new Item.Properties().setId(key)));
    public static final RegistryEntry<Item, Item> PURIFIED_TITANIUM = ITEMS.register("purified_titanium",
            key -> new Item(new Item.Properties().setId(key)));
    public static final RegistryEntry<Item, Item> TITANIUM_INGOT = ITEMS.register("titanium_ingot",
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

    /** Every Industry item, test blocks last. */
    public static final RegistryEntry<CreativeModeTab, CreativeModeTab> TAB = TABS.register("industry",
            key -> CreativeTabs.listing(ITEMS, () -> new ItemStack(CRUSHER_ITEM.get())));

    private IndustryContent() {
    }

    /** Blocks come first: block items and block entity types look up their blocks when they are created. */
    static List<Registrar<?>> registrars() {
        return List.of(FLUIDS, BLOCKS, ITEMS, BLOCK_ENTITIES, MENUS, TABS);
    }
}
