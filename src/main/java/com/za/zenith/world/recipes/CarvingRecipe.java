package com.za.zenith.world.recipes;

import com.za.zenith.utils.Identifier;
import com.za.zenith.world.items.ItemStack;

/**
 * Рецепт для процесса обтёсывания блока (например, бревно -> пень).
 */
public class CarvingRecipe implements IRecipe {
    private final Identifier id;
    private final Identifier input;
    private final Identifier tool;
    private final Identifier intermediate;
    private final Identifier result;
    private String sourcePath;

    @Override
    public String getSourcePath() { return sourcePath; }

    @Override
    public void setSourcePath(String path) { this.sourcePath = path; }

    public CarvingRecipe(Identifier id, Identifier input, Identifier tool, Identifier intermediate, Identifier result) {
        this.id = id;
        this.input = input;
        this.tool = tool;
        this.intermediate = intermediate;
        this.result = result;
    }

    @Override
    public Identifier getId() { return id; }
    
    @Override
    public String getType() { return "carving"; }

    public Identifier getInputBlock() { return input; }
    public Identifier getTool() { return tool; }
    public Identifier getIntermediateBlock() { return intermediate; }
    public Identifier getResultBlock() { return result; }

    @Override
    public ItemStack getResult() { return null; } // Не используется для этого типа
}
