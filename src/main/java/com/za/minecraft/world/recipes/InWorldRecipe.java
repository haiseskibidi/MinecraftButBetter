package com.za.minecraft.world.recipes;

import com.za.minecraft.utils.Identifier;
import com.za.minecraft.world.items.ItemStack;
import java.util.List;
import java.util.ArrayList;

/**
 * Универсальный рецепт для In-World крафта (на пне, верстаке и т.д.).
 * Поддерживает несколько ингредиентов и требует инструмент.
 */
public class InWorldRecipe implements IRecipe {
    private final Identifier id;
    private final List<Identifier> ingredients;
    private final Identifier toolId;
    private final int requiredHits;
    private final ItemStack result;

    public InWorldRecipe(Identifier id, List<Identifier> ingredients, Identifier toolId, int requiredHits, ItemStack result) {
        this.id = id;
        this.ingredients = ingredients;
        this.toolId = toolId;
        this.requiredHits = requiredHits;
        this.result = result;
    }

    @Override
    public Identifier getId() {
        return id;
    }

    @Override
    public ItemStack getResult() {
        return result.copy();
    }

    @Override
    public String getType() {
        return "in_world_crafting";
    }

    public List<Identifier> getIngredients() {
        return ingredients;
    }

    public Identifier getToolId() {
        return toolId;
    }

    public int getRequiredHits() {
        return requiredHits;
    }

    /**
     * Проверяет, соответствует ли набор предметов на поверхности этому рецепту.
     * Не учитывает порядок, только состав.
     */
    public boolean matches(List<ItemStack> currentItems, Identifier tool) {
        if (toolId != null && !toolId.equals(tool)) {
            // com.za.minecraft.utils.Logger.info("Tool mismatch: expected %s, got %s", toolId, tool);
            return false;
        }

        List<Identifier> currentIds = new ArrayList<>();
        for (ItemStack s : currentItems) {
            if (s != null) currentIds.add(s.getItem().getIdentifier());
        }

        if (currentIds.size() != ingredients.size()) {
            // com.za.minecraft.utils.Logger.info("Size mismatch: expected %d, got %d", ingredients.size(), currentIds.size());
            return false;
        }

        List<Identifier> needed = new ArrayList<>(ingredients);
        for (Identifier id : currentIds) {
            if (!needed.remove(id)) {
                // com.za.minecraft.utils.Logger.info("Ingredient mismatch: %s not in recipe", id);
                return false;
            }
        }

        return needed.isEmpty();
    }
}
