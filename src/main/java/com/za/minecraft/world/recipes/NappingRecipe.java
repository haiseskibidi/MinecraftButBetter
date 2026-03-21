package com.za.minecraft.world.recipes;

import com.za.minecraft.utils.Identifier;
import com.za.minecraft.world.items.ItemStack;

public class NappingRecipe implements IRecipe {
    private final Identifier id;
    private final Identifier inputId; // Какой камень нужен (например, flint)
    private final ItemStack result;
    private final boolean[] pattern; // 5x5 паттерн (true = блок должен быть отколот)

    public NappingRecipe(Identifier id, Identifier inputId, ItemStack result, boolean[] pattern) {
        this.id = id;
        this.inputId = inputId;
        this.result = result;
        this.pattern = pattern;
    }

    @Override
    public Identifier getId() { return id; }

    @Override
    public ItemStack getResult() { return result.copy(); }

    @Override
    public String getType() { return "napping"; }

    public Identifier getInputId() { return inputId; }

    public boolean[] getPattern() { return pattern; }
}
