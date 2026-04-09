package com.za.zenith.world.recipes;

import com.za.zenith.utils.Identifier;
import com.za.zenith.world.items.ItemStack;
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
    private final Identifier requiredSurface; // Опционально: ID блока (пень, стол и т.д.)
    private final int requiredHits;
    private final ItemStack result;

    public InWorldRecipe(Identifier id, List<Identifier> ingredients, Identifier toolId, int requiredHits, ItemStack result) {
        this(id, ingredients, toolId, null, requiredHits, result);
    }

    public InWorldRecipe(Identifier id, List<Identifier> ingredients, Identifier toolId, Identifier requiredSurface, int requiredHits, ItemStack result) {
        this.id = id;
        this.ingredients = ingredients;
        this.toolId = toolId;
        this.requiredSurface = requiredSurface;
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

    public Identifier getRequiredSurface() {
        return requiredSurface;
    }

    public int getRequiredHits() {
        return requiredHits;
    }

    /**
     * Проверяет, соответствует ли набор предметов на поверхности этому рецепту.
     * Не учитывает порядок, только состав.
     */
    public boolean matches(List<ItemStack> currentItems, Identifier tool, Identifier surfaceId) {
        // 1. Проверка поверхности (если задана)
        if (requiredSurface != null && !requiredSurface.equals(surfaceId)) {
            return false;
        }

        // 2. Проверка инструмента
        if (toolId != null && !toolId.equals(tool)) {
            return false;
        }

        // 3. Проверка состава ингредиентов
        List<Identifier> currentIds = new ArrayList<>();
        for (ItemStack s : currentItems) {
            if (s != null) currentIds.add(s.getItem().getIdentifier());
        }

        if (currentIds.size() != ingredients.size()) {
            return false;
        }

        List<Identifier> needed = new ArrayList<>(ingredients);
        for (Identifier id : currentIds) {
            if (!needed.remove(id)) {
                return false;
            }
        }

        return needed.isEmpty();
    }
}


