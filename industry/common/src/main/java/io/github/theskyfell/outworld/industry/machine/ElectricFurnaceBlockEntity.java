package io.github.theskyfell.outworld.industry.machine;

import io.github.theskyfell.outworld.core.machine.RecipeMachineBlockEntity;
import io.github.theskyfell.outworld.core.recipe.MachineRecipe;
import io.github.theskyfell.outworld.industry.IndustryContent;
import io.github.theskyfell.outworld.industry.OutworldIndustry;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The electric furnace: smelts anything a furnace smelts, twice as fast, on energy instead of fuel.
 * Recipes for {@link #MACHINE} in data come first; otherwise it uses the vanilla smelting recipe for
 * what's in its input slot, so other mods' smelting recipes work too. Smelting experience is not kept.
 */
public final class ElectricFurnaceBlockEntity extends RecipeMachineBlockEntity {
    public static final Identifier MACHINE = Identifier.fromNamespaceAndPath(OutworldIndustry.MOD_ID, "electric_furnace");
    /** FE per tick while smelting: a standard 10-second smelt takes 100 ticks and 2,000 FE. */
    public static final long ENERGY_PER_TICK = 20;
    private static final Layout LAYOUT = Layout.items(MACHINE, 1, 1, 10_000);

    private final RecipeManager.CachedCheck<SingleRecipeInput, SmeltingRecipe> smelting = RecipeManager.createCheck(RecipeType.SMELTING);

    public ElectricFurnaceBlockEntity(BlockPos pos, BlockState state) {
        super(IndustryContent.ELECTRIC_FURNACE.get(), pos, state, LAYOUT);
    }

    @Override
    protected MachineRecipe findRecipe(ServerLevel level) {
        MachineRecipe fromData = super.findRecipe(level);
        if (fromData != null) {
            return fromData;
        }
        ItemStack input = items().getItem(0);
        if (input.isEmpty()) {
            return null;
        }
        SingleRecipeInput recipeInput = new SingleRecipeInput(input);
        return smelting.getRecipeFor(recipeInput, level)
                .map(holder -> fromSmelting(input, holder.value(), recipeInput))
                .orElse(null);
    }

    /** One of this item in, the smelting result out, in half the time. */
    private static MachineRecipe fromSmelting(ItemStack input, SmeltingRecipe smelting, SingleRecipeInput recipeInput) {
        ItemStack result = smelting.assemble(recipeInput);
        if (result.isEmpty()) {
            return null;
        }
        return new MachineRecipe(MACHINE,
                List.of(new MachineRecipe.ItemInput(Ingredient.of(input.getItem()), 1)), List.of(), List.of(),
                List.of(new MachineRecipe.ItemResult(result.getItem().builtInRegistryHolder(), result.getCount(), 1.0F)),
                List.of(), List.of(),
                Math.max(1, smelting.cookingTime() / 2), ENERGY_PER_TICK);
    }
}
