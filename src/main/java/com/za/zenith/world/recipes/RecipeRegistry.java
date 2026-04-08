package com.za.zenith.world.recipes;

import com.za.zenith.utils.Identifier;
import com.za.zenith.utils.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class RecipeRegistry {
    private static final Registry<IRecipe> REGISTRY = new Registry<>();

    public static void register(IRecipe recipe) {
        REGISTRY.register(recipe.getId(), recipe);
    }

    public static IRecipe get(Identifier id) {
        return REGISTRY.get(id);
    }

    public static List<IRecipe> getRecipesByType(String type) {
        return REGISTRY.values().stream()
                .filter(r -> r.getType().equalsIgnoreCase(type))
                .collect(Collectors.toList());
    }

    public static Registry<IRecipe> getRegistry() {
        return REGISTRY;
    }
}
