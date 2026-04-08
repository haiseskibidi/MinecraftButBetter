package com.za.zenith.world.recipes;

import com.za.zenith.utils.Identifier;
import com.za.zenith.world.items.ItemStack;

/**
 * Рецепт для процесса обтёсывания блока (например, бревно -> пень).
 */
public class CarvingRecipe implements IRecipe {
    private final Identifier id;
    private final Identifier inputBlock;
    private final Identifier tool;
    private final Identifier intermediateBlock;
    private final Identifier resultBlock;

    public CarvingRecipe(Identifier id, Identifier inputBlock, Identifier tool, Identifier intermediateBlock, Identifier resultBlock) {
        this.id = id;
        this.inputBlock = inputBlock;
        this.tool = tool;
        this.intermediateBlock = intermediateBlock;
        this.resultBlock = resultBlock;
    }

    @Override
    public Identifier getId() { return id; }
    
    @Override
    public String getType() { return "carving"; }

    public Identifier getInputBlock() { return inputBlock; }
    public Identifier getTool() { return tool; }
    public Identifier getIntermediateBlock() { return intermediateBlock; }
    public Identifier getResultBlock() { return resultBlock; }

    @Override
    public ItemStack getResult() { return null; } // Не используется для этого типа
}


