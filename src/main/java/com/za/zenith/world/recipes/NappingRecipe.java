package com.za.zenith.world.recipes;

import com.za.zenith.utils.Identifier;
import com.za.zenith.world.items.ItemStack;
import java.util.List;

/**
 * Рецепт скалывания (Napping).
 * Поддерживает список допустимых входных предметов (например, кремень или камень).
 */
public class NappingRecipe implements IRecipe {
    private final Identifier id;
    private final List<Identifier> inputIds; 
    private final ItemStack result;
    private final boolean[] pattern; 

    public NappingRecipe(Identifier id, List<Identifier> inputIds, ItemStack result, boolean[] pattern) {
        this.id = id;
        this.inputIds = inputIds;
        this.result = result;
        this.pattern = pattern;
    }

    @Override
    public Identifier getId() { return id; }

    @Override
    public ItemStack getResult() { return result.copy(); }

    @Override
    public String getType() { return "napping"; }

    public List<Identifier> getInputIds() { return inputIds; }

    public boolean[] getPattern() { return pattern; }

    /**
     * Проверяет, подходит ли данный предмет для начала этого рецепта.
     */
    public boolean isInputValid(Identifier id) {
        return inputIds.contains(id);
    }
}
