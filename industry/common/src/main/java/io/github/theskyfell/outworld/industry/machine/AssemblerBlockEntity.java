package io.github.theskyfell.outworld.industry.machine;

import io.github.theskyfell.outworld.core.machine.RecipeMachineBlockEntity;
import io.github.theskyfell.outworld.core.recipe.MachineRecipe;
import io.github.theskyfell.outworld.industry.IndustryContent;
import io.github.theskyfell.outworld.industry.OutworldIndustry;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * The assembler: makes whatever crafting recipe a player picks from the list in its screen, on
 * energy, a second a craft. It works with any shaped or shapeless crafting recipe, other mods'
 * included, but not the special ones (dyeing armour, cloning maps and the like).
 *
 * <p>Each different ingredient of the recipe has an input slot of its own, in the order the recipe
 * lists them, and the slot takes only that ingredient, so hoppers, pipes and inserters can't fill the
 * machine with one ingredient and starve it of the others. Choosing another recipe hands back
 * whatever the new recipe doesn't take.
 *
 * <p>Ingredients that leave something behind in a crafting table (the buckets of milk in a cake) are
 * used up whole.
 */
public final class AssemblerBlockEntity extends RecipeMachineBlockEntity {
    public static final Identifier MACHINE = Identifier.fromNamespaceAndPath(OutworldIndustry.MOD_ID, "assembler");
    /** Ticks a craft takes, before speed upgrades. */
    public static final int CRAFT_TICKS = 20;
    /** FE per tick while crafting: 200 FE a craft. */
    public static final long ENERGY_PER_TICK = 10;
    /** A crafting grid holds at most nine different ingredients. */
    public static final int INPUTS = 9;
    private static final Layout LAYOUT = Layout.items(MACHINE, INPUTS, 1, 20_000);

    /** A recipe the assembler can make, as its screen lists it. */
    public record Choice(Identifier recipe, ItemStack result) {
    }

    /** A crafting recipe as the assembler makes it, with what each input slot takes. */
    private record Converted(MachineRecipe recipe, List<Ingredient> slots, ItemStack result) {
    }

    private static WeakReference<RecipeManager> choicesFrom = new WeakReference<>(null);
    private static int choicesRecipeCount;
    private static List<Choice> choices = List.of();

    private ResourceKey<Recipe<?>> chosen;
    // The chosen recipe converted, and the recipe manager it came from (a new one after /reload).
    private RecipeManager convertedFrom;
    private Converted converted;

    public AssemblerBlockEntity(BlockPos pos, BlockState state) {
        super(IndustryContent.ASSEMBLER.get(), pos, state, LAYOUT);
        items().setSlotFilter(this::accepts);
    }

    @Override
    public void openMenu(ServerPlayer player, Component title) {
        AssemblerMenu.open(player, this, title);
    }

    /** The chosen recipe's id, or null. */
    public Identifier chosenRecipe() {
        return chosen == null ? null : chosen.identifier();
    }

    /**
     * Chooses the recipe to make, or none, and hands the player back whatever is in an input slot
     * that the new recipe doesn't take there.
     */
    public void choose(Identifier recipe, Player player) {
        ResourceKey<Recipe<?>> key = recipe == null ? null : ResourceKey.create(Registries.RECIPE, recipe);
        if (Objects.equals(key, chosen)) {
            return;
        }
        chosen = key;
        convertedFrom = null;
        converted = null;
        for (int slot = 0; slot < INPUTS; slot++) {
            ItemStack stack = items().getItem(slot);
            if (!stack.isEmpty() && !accepts(slot, stack)) {
                items().setItem(slot, ItemStack.EMPTY);
                if (!player.addItem(stack) && !stack.isEmpty() && level != null) {
                    Block.popResource(level, player.blockPosition(), stack);
                }
            }
        }
        resetRecipe();
    }

    /** Whether the assembler can make this recipe. */
    public static boolean canMake(MinecraftServer server, Identifier recipe) {
        return server.getRecipeManager().byKey(ResourceKey.create(Registries.RECIPE, recipe))
                .map(holder -> convert(holder.value()) != null)
                .orElse(false);
    }

    /** Every recipe the assembler can make, by id. Worked out once for each set of recipes. */
    public static synchronized List<Choice> choices(MinecraftServer server) {
        RecipeManager recipes = server.getRecipeManager();
        int count = recipes.getRecipes().size();
        if (choicesFrom.get() != recipes || choicesRecipeCount != count) {
            List<Choice> found = new ArrayList<>();
            for (RecipeHolder<?> holder : recipes.getRecipes()) {
                Converted recipe = convert(holder.value());
                if (recipe != null) {
                    found.add(new Choice(holder.id().identifier(), recipe.result()));
                }
            }
            found.sort(Comparator.comparing(choice -> choice.recipe().toString()));
            choices = List.copyOf(found);
            choicesFrom = new WeakReference<>(recipes);
            choicesRecipeCount = count;
        }
        return choices;
    }

    @Override
    protected MachineRecipe findRecipe(ServerLevel level) {
        Converted recipe = converted();
        return recipe != null && matches(recipe.recipe()) ? recipe.recipe() : null;
    }

    /** Input slot {@code slot} takes only the chosen recipe's ingredient for it. */
    private boolean accepts(int slot, ItemStack stack) {
        Converted recipe = converted();
        return recipe != null && slot < recipe.slots().size() && recipe.slots().get(slot).test(stack);
    }

    private Converted converted() {
        if (chosen == null || !(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        RecipeManager recipes = serverLevel.getServer().getRecipeManager();
        if (recipes != convertedFrom) {
            convertedFrom = recipes;
            Optional<RecipeHolder<?>> holder = recipes.byKey(chosen);
            converted = holder.map(found -> convert(found.value())).orElse(null);
        }
        return converted;
    }

    /**
     * A crafting recipe as a machine recipe: each different ingredient once, with how many of it a
     * craft takes. Null for anything but an ordinary crafting recipe.
     */
    private static Converted convert(Recipe<?> recipe) {
        if (!(recipe instanceof CraftingRecipe crafting) || crafting.isSpecial()) {
            return null;
        }
        ItemStack result;
        try {
            result = crafting.assemble(CraftingInput.EMPTY);
        } catch (RuntimeException e) {
            return null; // a recipe whose result depends on what goes in
        }
        List<Ingredient> ingredients = crafting.placementInfo().ingredients();
        if (result.isEmpty() || ingredients.isEmpty()) {
            return null;
        }
        // Ingredients that take the same items are one ingredient (the three planks of a shapeless
        // recipe are three separate but equal ingredients).
        List<List<Holder<Item>>> seen = new ArrayList<>();
        List<Ingredient> slots = new ArrayList<>();
        List<Integer> counts = new ArrayList<>();
        for (Ingredient ingredient : ingredients) {
            List<Holder<Item>> items = new ArrayList<>();
            ingredient.items().forEach(items::add);
            int index = seen.indexOf(items);
            if (index < 0) {
                seen.add(items);
                slots.add(ingredient);
                counts.add(1);
            } else {
                counts.set(index, counts.get(index) + 1);
            }
        }
        if (slots.size() > INPUTS) {
            return null;
        }
        List<MachineRecipe.ItemInput> inputs = new ArrayList<>();
        for (int i = 0; i < slots.size(); i++) {
            inputs.add(new MachineRecipe.ItemInput(slots.get(i), counts.get(i)));
        }
        MachineRecipe machineRecipe = new MachineRecipe(MACHINE, List.copyOf(inputs), List.of(), List.of(),
                List.of(new MachineRecipe.ItemResult(result.getItem().builtInRegistryHolder(), result.getCount(), 1.0F)),
                List.of(), List.of(), CRAFT_TICKS, ENERGY_PER_TICK);
        return new Converted(machineRecipe, List.copyOf(slots), result);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        if (chosen != null) {
            output.putString("recipe", chosen.identifier().toString());
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        String saved = input.getStringOr("recipe", "");
        Identifier recipe = saved.isEmpty() ? null : Identifier.tryParse(saved);
        chosen = recipe == null ? null : ResourceKey.create(Registries.RECIPE, recipe);
        convertedFrom = null;
        converted = null;
    }
}
