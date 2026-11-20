package io.github.theskyfell.outworld.core.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.theskyfell.outworld.core.OutworldCore;
import io.github.theskyfell.outworld.core.fluid.FluidAmount;
import io.github.theskyfell.outworld.core.fluid.FluidUnits;
import io.github.theskyfell.outworld.core.gas.Gas;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.registries.codec.RegistryCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;

/**
 * What a machine makes from what. Recipes are data, loaded from
 * {@code data/<namespace>/outworld/machine_recipe/<name>.json} when a world loads and sent to clients
 * (for screens and recipe viewers). Changing them needs the world to load again; {@code /reload} doesn't
 * reload them.
 *
 * <pre>{@code
 * {
 *   "machine": "outworld_industry:crusher",
 *   "items": [{ "item": "minecraft:raw_iron" }],
 *   "results": [{ "item": "outworld_industry:crushed_iron", "count": 2 }],
 *   "time": 100,
 *   "energy": 20
 * }
 * }</pre>
 *
 * <p>Inputs: {@code items} (an ingredient, or {@code #tag}, with a count), {@code fluids} (a fluid or
 * {@code #tag}, amount in millibuckets) and {@code gases} (a gas, amount in millibuckets). Outputs:
 * {@code results} (items, each with an optional {@code chance} from 0 to 1), {@code fluid_results} and
 * {@code gas_results}. {@code time} is in ticks and {@code energy} in FE per tick.
 *
 * @param machine the machine that uses this recipe, such as {@code outworld_industry:crusher}
 */
public record MachineRecipe(Identifier machine, List<ItemInput> items, List<FluidInput> fluids, List<GasAmount> gases,
        List<ItemResult> results, List<FluidResult> fluidResults, List<GasAmount> gasResults, int time, long energy) {

    public static final ResourceKey<Registry<MachineRecipe>> REGISTRY =
            ResourceKey.createRegistryKey(Identifier.fromNamespaceAndPath(OutworldCore.MOD_ID, "machine_recipe"));

    public static final Codec<MachineRecipe> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Identifier.CODEC.fieldOf("machine").forGetter(MachineRecipe::machine),
            ItemInput.CODEC.listOf().optionalFieldOf("items", List.of()).forGetter(MachineRecipe::items),
            FluidInput.CODEC.listOf().optionalFieldOf("fluids", List.of()).forGetter(MachineRecipe::fluids),
            GasAmount.CODEC.listOf().optionalFieldOf("gases", List.of()).forGetter(MachineRecipe::gases),
            ItemResult.CODEC.listOf().optionalFieldOf("results", List.of()).forGetter(MachineRecipe::results),
            FluidResult.CODEC.listOf().optionalFieldOf("fluid_results", List.of()).forGetter(MachineRecipe::fluidResults),
            GasAmount.CODEC.listOf().optionalFieldOf("gas_results", List.of()).forGetter(MachineRecipe::gasResults),
            Codec.intRange(1, Integer.MAX_VALUE).fieldOf("time").forGetter(MachineRecipe::time),
            Codec.LONG.optionalFieldOf("energy", 0L).forGetter(MachineRecipe::energy)
    ).apply(instance, MachineRecipe::new));

    /** An item input: anything the ingredient accepts, {@code count} of it. */
    public record ItemInput(Ingredient ingredient, int count) {
        public static final Codec<ItemInput> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Ingredient.CODEC.fieldOf("item").forGetter(ItemInput::ingredient),
                Codec.intRange(1, 99).optionalFieldOf("count", 1).forGetter(ItemInput::count)
        ).apply(instance, ItemInput::new));

        public boolean test(ItemStack stack) {
            return !stack.isEmpty() && ingredient.test(stack);
        }
    }

    /** An item made, with a chance from 0 to 1 of making it at all. */
    public record ItemResult(Holder<Item> item, int count, float chance) {
        public static final Codec<ItemResult> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.ITEM.holderByNameCodec().fieldOf("item").forGetter(ItemResult::item),
                Codec.intRange(1, 99).optionalFieldOf("count", 1).forGetter(ItemResult::count),
                Codec.floatRange(0, 1).optionalFieldOf("chance", 1.0F).forGetter(ItemResult::chance)
        ).apply(instance, ItemResult::new));

        public ItemStack create() {
            return new ItemStack(item, count);
        }
    }

    /** A fluid input: any fluid in the set, {@code amount} millibuckets of it. */
    public record FluidInput(HolderSet<Fluid> fluid, long amount) {
        public static final Codec<FluidInput> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                RegistryCodecs.holderSet(Registries.FLUID).fieldOf("fluid").forGetter(FluidInput::fluid),
                Codec.LONG.fieldOf("amount").forGetter(FluidInput::amount)
        ).apply(instance, FluidInput::new));

        /** The amount in droplets, the unit Outworld's fluid tanks use. */
        public long droplets() {
            return amount * FluidUnits.MILLIBUCKET;
        }

        public boolean test(FluidAmount contents) {
            return !contents.isEmpty() && contents.fluid().defaultFluidState().is(fluid) && contents.amount() >= droplets();
        }
    }

    /** A fluid made, {@code amount} millibuckets of it. */
    public record FluidResult(Holder<Fluid> fluid, long amount) {
        public static final Codec<FluidResult> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                BuiltInRegistries.FLUID.holderByNameCodec().fieldOf("fluid").forGetter(FluidResult::fluid),
                Codec.LONG.fieldOf("amount").forGetter(FluidResult::amount)
        ).apply(instance, FluidResult::new));

        public long droplets() {
            return amount * FluidUnits.MILLIBUCKET;
        }
    }

    /** An amount of a gas, in millibuckets, used or made. */
    public record GasAmount(ResourceKey<Gas> gas, long amount) {
        public static final Codec<GasAmount> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceKey.codec(Gas.REGISTRY).fieldOf("gas").forGetter(GasAmount::gas),
                Codec.LONG.fieldOf("amount").forGetter(GasAmount::amount)
        ).apply(instance, GasAmount::new));
    }
}
