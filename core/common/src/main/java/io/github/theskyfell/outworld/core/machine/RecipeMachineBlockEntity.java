package io.github.theskyfell.outworld.core.machine;

import io.github.theskyfell.outworld.core.energy.Energy;
import io.github.theskyfell.outworld.core.energy.EnergyBuffer;
import io.github.theskyfell.outworld.core.fluid.FluidAccess;
import io.github.theskyfell.outworld.core.fluid.FluidAmount;
import io.github.theskyfell.outworld.core.fluid.FluidTank;
import io.github.theskyfell.outworld.core.fluid.FluidTransfer;
import io.github.theskyfell.outworld.core.fluid.FluidUnits;
import io.github.theskyfell.outworld.core.gas.Gas;
import io.github.theskyfell.outworld.core.gas.GasAccess;
import io.github.theskyfell.outworld.core.gas.GasHolder;
import io.github.theskyfell.outworld.core.gas.GasStack;
import io.github.theskyfell.outworld.core.gas.GasTank;
import io.github.theskyfell.outworld.core.gas.GasTransfer;
import io.github.theskyfell.outworld.core.gas.Gases;
import io.github.theskyfell.outworld.core.item.ItemBuffer;
import io.github.theskyfell.outworld.core.recipe.MachineRecipe;
import io.github.theskyfell.outworld.core.recipe.MachineRecipes;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A machine that runs {@link MachineRecipe}s: it takes items, fluids and gases from its input slots
 * and tanks, uses energy every tick of work, and puts the results in its output slots and tanks.
 * Hoppers, pipes and inserters fill the inputs and empty the item outputs; the machine itself never
 * pulls items, but it does draw energy from neighbouring energy storage (cables, batteries) when it
 * runs short, and pushes the fluid and gas it makes into neighbouring storage.
 *
 * <p>A {@link Layout#generator() generator} runs its recipes the other way round for energy: a
 * recipe's {@code energy} is made every tick instead of used, and the machine pushes its energy into
 * neighbouring storage.
 *
 * <p>It works in batches like every machine, and may finish several recipes in one batch. It sleeps
 * when it has no recipe to run or nowhere to put the results, and wakes when its contents change.
 * Short of energy (or, for a generator, with nowhere to put it) it checks again every second.
 */
public abstract class RecipeMachineBlockEntity extends MachineBlockEntity implements GasHolder {
    /**
     * The shape of a machine.
     *
     * @param machine the id recipes name in their {@code machine} field
     * @param tankBuckets how much each fluid and gas tank holds, in buckets
     * @param energyCapacity how much energy the machine stores, in FE
     * @param generator true if recipes make energy instead of using it
     */
    public record Layout(Identifier machine, int itemInputs, int itemOutputs, int fluidInputs, int fluidOutputs,
            int gasInputs, int gasOutputs, int tankBuckets, long energyCapacity, boolean generator) {
        /** A machine with only item slots. */
        public static Layout items(Identifier machine, int inputs, int outputs, long energyCapacity) {
            return new Layout(machine, inputs, outputs, 0, 0, 0, 0, 0, energyCapacity, false);
        }
    }

    /** How often a machine short of energy, or a generator with nowhere to put it, checks again. */
    private static final int ENERGY_RETRY_TICKS = 20;
    /** How often a machine holding fluid or gas its neighbours wouldn't take offers it again. */
    private static final int OUTPUT_RETRY_TICKS = 20;

    private final Layout layout;
    private final EnergyBuffer energy;
    private final ItemBuffer items;
    private final List<FluidTank> fluidInputs = new ArrayList<>();
    private final List<FluidTank> fluidOutputs = new ArrayList<>();
    private final List<GasTank> gasInputs = new ArrayList<>();
    private final List<GasTank> gasOutputs = new ArrayList<>();
    private final GasAccess gasAccess = new MachineGasAccess();
    private MachineRecipe recipe;
    private boolean recipeCheckNeeded = true;
    private int progress;

    protected RecipeMachineBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, Layout layout) {
        super(type, pos, state);
        this.layout = layout;
        // Machines take energy in; generators let it out.
        this.energy = layout.generator()
                ? new EnergyBuffer(layout.energyCapacity(), 0, layout.energyCapacity())
                : new EnergyBuffer(layout.energyCapacity(), layout.energyCapacity(), 0);
        ItemBuffer.Role[] roles = new ItemBuffer.Role[layout.itemInputs() + layout.itemOutputs()];
        for (int slot = 0; slot < roles.length; slot++) {
            roles[slot] = slot < layout.itemInputs() ? ItemBuffer.Role.INPUT : ItemBuffer.Role.OUTPUT;
        }
        this.items = new ItemBuffer(roles);
        long fluidCapacity = layout.tankBuckets() * FluidUnits.BUCKET;
        long gasCapacity = layout.tankBuckets() * Gases.BUCKET;
        for (int i = 0; i < layout.fluidInputs(); i++) {
            fluidInputs.add(new FluidTank(fluidCapacity, true, false, fluid -> true));
        }
        for (int i = 0; i < layout.fluidOutputs(); i++) {
            fluidOutputs.add(new FluidTank(fluidCapacity, false, true, fluid -> true));
        }
        for (int i = 0; i < layout.gasInputs(); i++) {
            gasInputs.add(new GasTank(gasCapacity, true, false, gas -> true));
        }
        for (int i = 0; i < layout.gasOutputs(); i++) {
            gasOutputs.add(new GasTank(gasCapacity, false, true, gas -> true));
        }

        Runnable contentsChanged = () -> {
            recipeCheckNeeded = true;
            setChanged();
            wake();
        };
        energy.onChange(() -> {
            setChanged();
            wake();
        });
        items.onChange(contentsChanged);
        fluidInputs.forEach(tank -> tank.onChange(contentsChanged));
        fluidOutputs.forEach(tank -> tank.onChange(contentsChanged));
        gasInputs.forEach(tank -> tank.onChange(contentsChanged));
        gasOutputs.forEach(tank -> tank.onChange(contentsChanged));
    }

    public Layout layout() {
        return layout;
    }

    public EnergyBuffer energy() {
        return energy;
    }

    public ItemBuffer items() {
        return items;
    }

    public List<FluidTank> fluidInputs() {
        return fluidInputs;
    }

    public List<FluidTank> fluidOutputs() {
        return fluidOutputs;
    }

    public List<GasTank> gasInputs() {
        return gasInputs;
    }

    public List<GasTank> gasOutputs() {
        return gasOutputs;
    }

    /** Ticks of work done on the current recipe. */
    public int progress() {
        return progress;
    }

    /** How many ticks the current recipe takes, or 0 if there is none. */
    public int recipeTime() {
        return recipe == null ? 0 : recipe.time();
    }

    /**
     * The fluid tank other mods reach from {@code side}: by default the first input tank, or the
     * first output tank if there are no inputs, or null. Override to choose per side.
     */
    public FluidTank exposedFluidTank(Direction side) {
        if (!fluidInputs.isEmpty()) {
            return fluidInputs.getFirst();
        }
        return fluidOutputs.isEmpty() ? null : fluidOutputs.getFirst();
    }

    /** Gas goes into the input tanks and comes out of the output tanks, from every side. */
    @Override
    public GasAccess gasAccess(Direction side) {
        return gasInputs.isEmpty() && gasOutputs.isEmpty() ? null : gasAccess;
    }

    @Override
    protected boolean work(ServerLevel level, int elapsedTicks) {
        boolean busy = runRecipes(level, elapsedTicks);
        if (pushOutputs(level) && !busy) {
            // Holding fluid or gas that a neighbour couldn't take yet: offer it again later.
            retryIn(OUTPUT_RETRY_TICKS);
        }
        if (layout.generator() && energy.getAmount() > 0) {
            long pushed = Energy.pushToNeighbours(level, worldPosition, energy, energy.getAmount());
            if (!busy && energy.getAmount() > 0) {
                // Holding energy: keep pushing while it goes somewhere, otherwise look again later.
                if (pushed > 0) {
                    return true;
                }
                retryIn(ENERGY_RETRY_TICKS);
            }
        }
        return busy;
    }

    /**
     * Pushes the output tanks' fluid and gas into neighbouring storage (pipes, tanks, other machines'
     * inputs). Items stay in the output slots for hoppers and inserters to take.
     *
     * @return true if something is left in an output tank and a neighbour has storage that might take
     *     it later
     */
    private boolean pushOutputs(ServerLevel level) {
        boolean anyFluid = fluidOutputs.stream().anyMatch(tank -> !tank.getContents().isEmpty());
        boolean anyGas = gasOutputs.stream().anyMatch(tank -> !tank.getContents().isEmpty());
        if (!anyFluid && !anyGas) {
            return false;
        }
        boolean waiting = false;
        for (Direction direction : Direction.values()) {
            BlockPos neighbour = worldPosition.relative(direction);
            if (anyFluid) {
                FluidAccess target = FluidTransfer.find(level, neighbour, direction.getOpposite());
                if (target != null) {
                    for (FluidTank tank : fluidOutputs) {
                        if (!tank.getContents().isEmpty()) {
                            FluidTransfer.move(tank, target, fluid -> true, tank.getContents().amount());
                            waiting |= !tank.getContents().isEmpty();
                        }
                    }
                }
            }
            if (anyGas) {
                GasAccess target = GasTransfer.find(level, neighbour, direction.getOpposite());
                if (target != null) {
                    for (GasTank tank : gasOutputs) {
                        if (!tank.getContents().isEmpty()) {
                            GasTransfer.move(tank, target, gas -> true, tank.getAmount());
                            waiting |= !tank.getContents().isEmpty();
                        }
                    }
                }
            }
        }
        return waiting;
    }

    private boolean runRecipes(ServerLevel level, int elapsedTicks) {
        int ticks = elapsedTicks;
        while (ticks > 0) {
            if (recipeCheckNeeded || recipe == null) {
                recipeCheckNeeded = false;
                MachineRecipe found = recipe != null && matches(recipe) ? recipe : findRecipe(level);
                if (found != recipe) {
                    // A recipe interrupted by its inputs going starts again; after loading, the saved
                    // progress carries on with the recipe found.
                    if (recipe != null || found == null) {
                        progress = 0;
                    }
                    recipe = found;
                    if (recipe != null) {
                        progress = Math.min(progress, recipe.time() - 1);
                    }
                    setChanged();
                }
            }
            if (recipe == null || !outputsFit(recipe)) {
                return false; // sleep until the contents change
            }
            int step = Math.min(ticks, recipe.time() - progress);
            long perTick = recipe.energy();
            if (perTick > 0 && layout.generator()) {
                long room = energy.getCapacity() - energy.getAmount();
                if (room < step * perTick) {
                    Energy.pushToNeighbours(level, worldPosition, energy, energy.getAmount());
                    room = energy.getCapacity() - energy.getAmount();
                }
                step = (int) Math.min(step, room / perTick);
                if (step <= 0) {
                    retryIn(ENERGY_RETRY_TICKS); // full, and nothing takes the energy
                    return false;
                }
                energy.setAmount(energy.getAmount() + step * perTick);
            } else if (perTick > 0) {
                long needed = step * perTick;
                if (energy.getAmount() < needed) {
                    Energy.pullFromNeighbours(level, worldPosition, energy, needed - energy.getAmount());
                }
                step = (int) Math.min(step, energy.getAmount() / perTick);
                if (step <= 0) {
                    retryIn(ENERGY_RETRY_TICKS); // energy arriving through a cable doesn't wake us
                    return false;
                }
                energy.setAmount(energy.getAmount() - step * perTick);
            }
            progress += step;
            ticks -= step;
            setChanged();
            if (progress >= recipe.time()) {
                complete(level, recipe);
                progress = 0;
                recipeCheckNeeded = true;
            }
        }
        return true;
    }

    /**
     * The recipe to run for the current contents, or null. By default the first data recipe for this
     * machine that matches; machines can add recipes from elsewhere (the electric furnace adds vanilla
     * smelting). Called only when the contents changed and the current recipe no longer matches.
     */
    protected MachineRecipe findRecipe(ServerLevel level) {
        for (MachineRecipe candidate : MachineRecipes.forMachine(level, layout.machine())) {
            if (matches(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean matches(MachineRecipe candidate) {
        if (candidate.items().isEmpty() && candidate.fluids().isEmpty() && candidate.gases().isEmpty()) {
            return false;
        }
        for (MachineRecipe.ItemInput input : candidate.items()) {
            int found = 0;
            for (int slot = 0; slot < layout.itemInputs(); slot++) {
                ItemStack stack = items.getItem(slot);
                if (input.test(stack)) {
                    found += stack.getCount();
                }
            }
            if (found < input.count()) {
                return false;
            }
        }
        for (MachineRecipe.FluidInput input : candidate.fluids()) {
            if (fluidInputs.stream().noneMatch(tank -> input.test(tank.getContents()))) {
                return false;
            }
        }
        for (MachineRecipe.GasAmount input : candidate.gases()) {
            if (gasInputs.stream().noneMatch(tank -> tank.getContents().is(input.gas()) && tank.getAmount() >= input.amount())) {
                return false;
            }
        }
        return true;
    }

    /** Whether every result would fit, assuming every chance comes up. */
    private boolean outputsFit(MachineRecipe candidate) {
        List<ItemStack> slots = new ArrayList<>();
        for (int slot = layout.itemInputs(); slot < items.getContainerSize(); slot++) {
            slots.add(items.getItem(slot).copy());
        }
        for (MachineRecipe.ItemResult result : candidate.results()) {
            if (!addToStacks(slots, result.create())) {
                return false;
            }
        }
        List<FluidAmount> fluids = new ArrayList<>(fluidOutputs.stream().map(FluidTank::getContents).toList());
        for (MachineRecipe.FluidResult result : candidate.fluidResults()) {
            int tank = fluidTankFor(fluids, result.fluid().value(), result.droplets());
            if (tank < 0) {
                return false;
            }
            fluids.set(tank, new FluidAmount(result.fluid().value(), DataComponentPatch.EMPTY, fluids.get(tank).amount() + result.droplets()));
        }
        List<GasStack> gases = new ArrayList<>(gasOutputs.stream().map(GasTank::getContents).toList());
        for (MachineRecipe.GasAmount result : candidate.gasResults()) {
            int tank = gasTankFor(gases, result.gas(), result.amount());
            if (tank < 0) {
                return false;
            }
            gases.set(tank, GasStack.of(result.gas(), gases.get(tank).amount() + result.amount()));
        }
        return true;
    }

    private void complete(ServerLevel level, MachineRecipe done) {
        for (MachineRecipe.ItemInput input : done.items()) {
            int remaining = input.count();
            for (int slot = 0; slot < layout.itemInputs() && remaining > 0; slot++) {
                ItemStack stack = items.getItem(slot);
                if (input.test(stack)) {
                    int taken = Math.min(remaining, stack.getCount());
                    items.removeItem(slot, taken);
                    remaining -= taken;
                }
            }
        }
        for (MachineRecipe.FluidInput input : done.fluids()) {
            for (FluidTank tank : fluidInputs) {
                if (input.test(tank.getContents())) {
                    FluidAmount contents = tank.getContents();
                    tank.setContents(new FluidAmount(contents.fluid(), contents.components(), contents.amount() - input.droplets()));
                    break;
                }
            }
        }
        for (MachineRecipe.GasAmount input : done.gases()) {
            for (GasTank tank : gasInputs) {
                if (tank.getContents().is(input.gas()) && tank.getAmount() >= input.amount()) {
                    tank.setContents(tank.getContents().withAmount(tank.getAmount() - input.amount()));
                    break;
                }
            }
        }

        for (MachineRecipe.ItemResult result : done.results()) {
            if (result.chance() < 1 && level.getRandom().nextFloat() >= result.chance()) {
                continue;
            }
            addToOutputSlots(result.create());
        }
        List<FluidAmount> fluids = new ArrayList<>(fluidOutputs.stream().map(FluidTank::getContents).toList());
        for (MachineRecipe.FluidResult result : done.fluidResults()) {
            int tank = fluidTankFor(fluids, result.fluid().value(), result.droplets());
            if (tank >= 0) {
                FluidAmount filled = new FluidAmount(result.fluid().value(), DataComponentPatch.EMPTY, fluids.get(tank).amount() + result.droplets());
                fluids.set(tank, filled);
                fluidOutputs.get(tank).setContents(filled);
            }
        }
        List<GasStack> gases = new ArrayList<>(gasOutputs.stream().map(GasTank::getContents).toList());
        for (MachineRecipe.GasAmount result : done.gasResults()) {
            int tank = gasTankFor(gases, result.gas(), result.amount());
            if (tank >= 0) {
                GasStack filled = GasStack.of(result.gas(), gases.get(tank).amount() + result.amount());
                gases.set(tank, filled);
                gasOutputs.get(tank).setContents(filled);
            }
        }
    }

    /** The first output tank that holds this fluid (or nothing) and has room, given these contents. */
    private int fluidTankFor(List<FluidAmount> contents, Fluid fluid, long droplets) {
        for (int i = 0; i < contents.size(); i++) {
            FluidAmount held = contents.get(i);
            boolean same = held.isEmpty() || held.isSameFluid(fluid, DataComponentPatch.EMPTY);
            if (same && fluidOutputs.get(i).getCapacity() - held.amount() >= droplets) {
                return i;
            }
        }
        return -1;
    }

    private int gasTankFor(List<GasStack> contents, ResourceKey<Gas> gas, long amount) {
        for (int i = 0; i < contents.size(); i++) {
            GasStack held = contents.get(i);
            if ((held.isEmpty() || held.is(gas)) && gasOutputs.get(i).getCapacity() - held.amount() >= amount) {
                return i;
            }
        }
        return -1;
    }

    /** Adds a stack to these stacks (topping up, then empty slots); false if it doesn't all fit. */
    private boolean addToStacks(List<ItemStack> stacks, ItemStack stack) {
        int remaining = stack.getCount();
        for (int pass = 0; pass < 2 && remaining > 0; pass++) {
            for (int i = 0; i < stacks.size() && remaining > 0; i++) {
                ItemStack held = stacks.get(i);
                if (pass == 0 && !held.isEmpty() && ItemStack.isSameItemSameComponents(held, stack)) {
                    int moved = Math.min(remaining, held.getMaxStackSize() - held.getCount());
                    if (moved > 0) {
                        held.grow(moved);
                        remaining -= moved;
                    }
                } else if (pass == 1 && held.isEmpty()) {
                    int moved = Math.min(remaining, stack.getMaxStackSize());
                    stacks.set(i, stack.copyWithCount(moved));
                    remaining -= moved;
                }
            }
        }
        return remaining == 0;
    }

    private void addToOutputSlots(ItemStack stack) {
        List<ItemStack> outputs = new ArrayList<>();
        for (int slot = layout.itemInputs(); slot < items.getContainerSize(); slot++) {
            outputs.add(items.getItem(slot).copy());
        }
        addToStacks(outputs, stack);
        for (int i = 0; i < outputs.size(); i++) {
            int slot = layout.itemInputs() + i;
            if (!ItemStack.matches(items.getItem(slot), outputs.get(i))) {
                items.setItem(slot, outputs.get(i));
            }
        }
    }

    /** Drops the machine's items when it is broken. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) {
            Containers.dropContents(level, pos, items);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        energy.save(output, "energy");
        items.save(output);
        output.putInt("progress", progress);
        for (int i = 0; i < fluidInputs.size(); i++) {
            fluidInputs.get(i).save(output, "fluid_in_" + i);
        }
        for (int i = 0; i < fluidOutputs.size(); i++) {
            fluidOutputs.get(i).save(output, "fluid_out_" + i);
        }
        for (int i = 0; i < gasInputs.size(); i++) {
            gasInputs.get(i).save(output, "gas_in_" + i);
        }
        for (int i = 0; i < gasOutputs.size(); i++) {
            gasOutputs.get(i).save(output, "gas_out_" + i);
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        energy.load(input, "energy");
        items.load(input);
        progress = input.getIntOr("progress", 0);
        for (int i = 0; i < fluidInputs.size(); i++) {
            fluidInputs.get(i).load(input, "fluid_in_" + i);
        }
        for (int i = 0; i < fluidOutputs.size(); i++) {
            fluidOutputs.get(i).load(input, "fluid_out_" + i);
        }
        for (int i = 0; i < gasInputs.size(); i++) {
            gasInputs.get(i).load(input, "gas_in_" + i);
        }
        for (int i = 0; i < gasOutputs.size(); i++) {
            gasOutputs.get(i).load(input, "gas_out_" + i);
        }
        recipe = null;
        recipeCheckNeeded = true;
    }

    /** Gas goes into the first input tank that takes it, and comes out of the output tanks. */
    private final class MachineGasAccess implements GasAccess {
        @Override
        public long insert(ResourceKey<Gas> gas, long maxAmount, boolean simulate) {
            for (GasTank tank : gasInputs) {
                long accepted = tank.insert(gas, maxAmount, simulate);
                if (accepted > 0) {
                    return accepted;
                }
            }
            return 0;
        }

        @Override
        public GasStack extract(Predicate<ResourceKey<Gas>> filter, long maxAmount, boolean simulate) {
            for (GasTank tank : gasOutputs) {
                GasStack taken = tank.extract(filter, maxAmount, simulate);
                if (!taken.isEmpty()) {
                    return taken;
                }
            }
            return GasStack.EMPTY;
        }
    }
}
