package com.za.minecraft.world.blocks;

import com.za.minecraft.utils.Identifier;
import com.za.minecraft.world.BlockPos;
import com.za.minecraft.world.World;
import com.za.minecraft.entities.Player;
import com.za.minecraft.world.blocks.entity.BlockEntity;
import com.za.minecraft.world.blocks.entity.StumpBlockEntity;
import com.za.minecraft.world.items.ItemStack;
import com.za.minecraft.world.recipes.RecipeRegistry;
import com.za.minecraft.world.recipes.StumpRecipe;

/**
 * Определение блока для пня (Stump).
 * Управляет логикой взаимодействия: размещение предметов, обработка инструментами.
 */
public class StumpBlockDefinition extends BlockDefinition {
    public StumpBlockDefinition(int id, Identifier identifier, String translationKey, boolean solid, boolean transparent) {
        super(id, identifier, translationKey, solid, transparent);
    }

    @Override
    public boolean onUse(World world, BlockPos pos, Player player, ItemStack heldStack) {
        BlockEntity be = world.getBlockEntity(pos);
        StumpBlockEntity stump;
        
        if (!(be instanceof StumpBlockEntity)) {
            // Если BE почему-то нет, создаем его лениво
            stump = new StumpBlockEntity(pos);
            world.setBlockEntity(stump);
        } else {
            stump = (StumpBlockEntity) be;
        }

        // 1. Снятие предмета (Shift+ПКМ или ПКМ пустой рукой)
        if (player.isSneaking() || heldStack == null) {
            if (stump.hasItem()) {
                player.getInventory().addItem(stump.getHeldStack());
                stump.setHeldStack(null);
                return true;
            }
            return false;
        }

        // 2. Если на пне пусто - кладем предмет (по 1 штуке)
        if (!stump.hasItem()) {
            // Проверяем, есть ли вообще рецепты для этого предмета, чтобы не класть мусор
            if (hasRecipesFor(heldStack.getItem().getIdentifier())) {
                stump.setHeldStack(heldStack.split(1));
                if (heldStack.getCount() <= 0) {
                    player.getInventory().setStackInSlot(player.getInventory().getSelectedSlot(), null);
                }
                return true;
            }
            return false;
        }

        // 3. Если на пне что-то есть - пробуем обработать инструментом
        ItemStack onStump = stump.getHeldStack();
        Identifier toolId = heldStack.getItem().getIdentifier();
        
        java.util.List<com.za.minecraft.world.recipes.IRecipe> recipes = RecipeRegistry.getRecipesByType("stump_crafting");
        for (com.za.minecraft.world.recipes.IRecipe r : recipes) {
            StumpRecipe recipe = (StumpRecipe) r;
            if (recipe.matches(onStump, toolId)) {
                stump.incrementProgress(toolId);
                player.swing();
                
                // Эффекты
                player.addNoise(0.1f); 
                
                if (stump.getProgress() >= recipe.getRequiredHits()) {
                    stump.setHeldStack(recipe.getResult());
                    com.za.minecraft.utils.Logger.info("Crafting on stump complete: " + recipe.getResult().getItem().getName());
                }
                return true;
            }
        }

        return false;
    }

    private boolean hasRecipesFor(Identifier inputId) {
        java.util.List<com.za.minecraft.world.recipes.IRecipe> recipes = RecipeRegistry.getRecipesByType("stump_crafting");
        for (com.za.minecraft.world.recipes.IRecipe r : recipes) {
            if (((StumpRecipe)r).getInputId().equals(inputId)) return true;
        }
        return false;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos) {
        return new StumpBlockEntity(pos);
    }
}
