package com.za.zenith.world.recipes;

import com.za.zenith.world.items.ItemStack;
import com.za.zenith.utils.Identifier;

/**
 * Базовый интерфейс для всех рецептов в игре.
 */
public interface IRecipe extends com.za.zenith.utils.LiveReloadable {
    Identifier getId();
    ItemStack getResult();
    String getType(); // "napping", "crafting", etc.
}


