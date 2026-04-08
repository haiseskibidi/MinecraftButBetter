package com.za.zenith.world.recipes;

import com.za.zenith.utils.Identifier;
import com.za.zenith.world.items.ItemStack;

/**
 * Рецепт для крафта на пне (Stump Crafting).
 * Описывает входной предмет, требуемый инструмент, количество ударов и результат.
 */
public class StumpRecipe implements IRecipe {
    private final Identifier id;
    private final Identifier inputId;   // Предмет, который кладется на пень
    private final Identifier toolId;    // Инструмент, которым нужно бить (может быть null)
    private final int requiredHits;      // Количество необходимых ударов
    private final ItemStack result;      // Итоговый предмет

    public StumpRecipe(Identifier id, Identifier inputId, Identifier toolId, int requiredHits, ItemStack result) {
        this.id = id;
        this.inputId = inputId;
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
        return "stump_crafting";
    }

    public Identifier getInputId() {
        return inputId;
    }

    public Identifier getToolId() {
        return toolId;
    }

    public int getRequiredHits() {
        return requiredHits;
    }

    /**
     * Проверяет, подходит ли предмет и инструмент под этот рецепт.
     */
    public boolean matches(ItemStack input, Identifier tool) {
        if (input == null || !input.getItem().getIdentifier().equals(inputId)) {
            return false;
        }
        
        // Если в рецепте указан инструмент, он должен совпадать
        if (toolId != null) {
            return toolId.equals(tool);
        }
        
        return true;
    }
}


