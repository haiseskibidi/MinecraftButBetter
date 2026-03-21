package com.za.minecraft.world.recipes;

import com.za.minecraft.world.items.ItemStack;
import com.za.minecraft.utils.Identifier;

/**
 * Базовый интерфейс для всех рецептов в игре.
 */
public interface IRecipe {
    Identifier getId();
    ItemStack getResult();
    String getType(); // "napping", "crafting", etc.
}
