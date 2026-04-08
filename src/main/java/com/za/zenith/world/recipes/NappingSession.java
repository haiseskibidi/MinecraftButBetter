package com.za.zenith.world.recipes;

import com.za.zenith.world.items.Item;
import java.util.Arrays;
import java.util.List;

/**
 * Управляет состоянием одной сессии скалывания камня (Napping).
 */
public class NappingSession {
    private final Item inputItem;
    private final boolean[] grid; // 5x5, true = кусок камня есть, false = отколот

    public NappingSession(Item inputItem) {
        this.inputItem = inputItem;
        this.grid = new boolean[25];
        Arrays.fill(this.grid, true); // Изначально целый кусок
    }

    /**
     * Удаляет кусочек камня по индексу (0-24).
     */
    public void removePiece(int index) {
        if (index >= 0 && index < 25) {
            grid[index] = false;
        }
    }

    public boolean[] getGrid() {
        return grid;
    }

    public Item getInputItem() {
        return inputItem;
    }

    /**
     * Проверяет, соответствует ли текущая сетка какому-либо рецепту скалывания.
     */
    public NappingRecipe checkMatch() {
        List<IRecipe> recipes = RecipeRegistry.getRecipesByType("napping");
        for (IRecipe recipe : recipes) {
            if (recipe instanceof NappingRecipe nappingRecipe) {
                // Совпадает ли материал?
                if (nappingRecipe.isInputValid(inputItem.getIdentifier())) {
                    // Совпадает ли паттерн?
                    if (Arrays.equals(grid, nappingRecipe.getPattern())) {
                        return nappingRecipe;
                    }
                }
            }
        }
        return null;
    }
}


