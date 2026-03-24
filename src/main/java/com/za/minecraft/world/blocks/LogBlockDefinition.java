package com.za.minecraft.world.blocks;

import com.za.minecraft.utils.Identifier;
import com.za.minecraft.world.BlockPos;
import com.za.minecraft.world.World;
import com.za.minecraft.entities.Player;
import com.za.minecraft.world.items.ItemStack;
import com.za.minecraft.world.items.Items;
import com.za.minecraft.world.blocks.entity.StumpBlockEntity;

public class LogBlockDefinition extends BlockDefinition {
    public LogBlockDefinition(int id, Identifier identifier, String translationKey, boolean solid, boolean transparent) {
        super(id, identifier, translationKey, solid, transparent);
    }

    @Override
    public boolean onUse(World world, BlockPos pos, Player player, ItemStack heldStack, float hitX, float hitY, float hitZ) {
        if (heldStack == null) return false;

        Identifier heldId = heldStack.getItem().getIdentifier();
        
        // Ищем рецепт обтёсывания для этого блока
        java.util.List<com.za.minecraft.world.recipes.IRecipe> recipes = com.za.minecraft.world.recipes.RecipeRegistry.getRecipesByType("carving");
        
        if (recipes.isEmpty()) {
            com.za.minecraft.utils.Logger.info("No carving recipes found in registry");
        }

        for (com.za.minecraft.world.recipes.IRecipe r : recipes) {
            com.za.minecraft.world.recipes.CarvingRecipe recipe = (com.za.minecraft.world.recipes.CarvingRecipe) r;
            
            boolean blockMatch = recipe.getInputBlock().equals(this.getIdentifier());
            boolean toolMatch = (recipe.getTool() == null || heldId.equals(recipe.getTool()));
            boolean sneaking = player.isSneaking();
            
            if (blockMatch) {
                com.za.minecraft.utils.Logger.info("Carving debug: toolMatch=%b (held:%s, req:%s), sneaking=%b, hitY=%.2f", 
                    toolMatch, heldId, recipe.getTool(), sneaking, hitY);
            }

            if (blockMatch && toolMatch && sneaking && hitY > 0.8f) {
                int intermediateId = BlockRegistry.getRegistry().getId(recipe.getIntermediateBlock());
                world.setBlock(pos, new Block(intermediateId));
                
                StumpBlockEntity be = (StumpBlockEntity) world.getBlockEntity(pos);
                if (be != null) {
                    be.setCarvingMask(0);
                }
                
                com.za.minecraft.utils.Logger.info("Started carving %s into %s", recipe.getInputBlock(), recipe.getIntermediateBlock());
                return true;
            }
        }

        // 1. Старая логика топора (оставляем как быстрый вариант, если нет рецепта)
        if (heldId.getPath().contains("axe")) {
            world.setBlock(pos, new Block(Blocks.STUMP.getId()));
            return true;
        }

        return false;
    }
}
