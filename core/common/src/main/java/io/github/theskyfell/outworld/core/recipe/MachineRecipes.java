package io.github.theskyfell.outworld.core.recipe;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.Registry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;

/** Looks up machine recipes by machine. Works on both sides. */
public final class MachineRecipes {
    /** Recipes grouped by machine, per loaded registry (the server's and each client's). */
    private static final Map<Registry<MachineRecipe>, Map<Identifier, List<MachineRecipe>>> BY_MACHINE =
            Collections.synchronizedMap(new WeakHashMap<>());

    private MachineRecipes() {
    }

    /** Every recipe for {@code machine}, in no particular order. */
    public static List<MachineRecipe> forMachine(Level level, Identifier machine) {
        Registry<MachineRecipe> registry = level.registryAccess().lookupOrThrow(MachineRecipe.REGISTRY);
        return BY_MACHINE.computeIfAbsent(registry, MachineRecipes::group).getOrDefault(machine, List.of());
    }

    private static Map<Identifier, List<MachineRecipe>> group(Registry<MachineRecipe> registry) {
        Map<Identifier, List<MachineRecipe>> grouped = new HashMap<>();
        for (MachineRecipe recipe : registry) {
            grouped.computeIfAbsent(recipe.machine(), machine -> new ArrayList<>()).add(recipe);
        }
        grouped.replaceAll((machine, recipes) -> List.copyOf(recipes));
        return grouped;
    }
}
