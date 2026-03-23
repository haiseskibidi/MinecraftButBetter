package com.za.minecraft.world.blocks;

import com.za.minecraft.utils.Identifier;
import com.za.minecraft.world.BlockPos;
import com.za.minecraft.world.World;
import com.za.minecraft.entities.Player;
import com.za.minecraft.world.blocks.entity.BlockEntity;
import com.za.minecraft.world.blocks.entity.StumpBlockEntity;
import com.za.minecraft.world.blocks.entity.ICraftingSurface;
import com.za.minecraft.world.items.ItemStack;
import com.za.minecraft.world.recipes.RecipeRegistry;
import com.za.minecraft.world.recipes.InWorldRecipe;
import java.util.ArrayList;
import java.util.List;

/**
 * Определение блока для пня (Stump).
 * Использует универсальный In-World Crafting API для взаимодействия.
 */
public class StumpBlockDefinition extends BlockDefinition {
    public StumpBlockDefinition(int id, Identifier identifier, String translationKey, boolean solid, boolean transparent) {
        super(id, identifier, translationKey, solid, transparent);
    }

    @Override
    public boolean onUse(World world, BlockPos pos, Player player, ItemStack heldStack, float hitX, float hitY, float hitZ) {
        BlockEntity be = world.getBlockEntity(pos);
        ICraftingSurface surface;

        if (be instanceof ICraftingSurface s) {
            surface = s;
        } else {
            StumpBlockEntity stump = new StumpBlockEntity(pos);
            world.setBlockEntity(stump);
            surface = stump;
        }

        int slot = CraftingLayoutEngine.getSlotIndex(hitX, hitZ);
        ItemStack inSlot = surface.getStackInSlot(slot);

        // 1. Снятие предмета (Shift + ПКМ)
        if (player.isSneaking()) {
            if (inSlot != null) {
                player.getInventory().addItem(inSlot);
                surface.setStackInSlot(slot, null);
                surface.resetProgress();
                return true;
            }
            return false;
        }

        // 2. Размещение предмета (Обычный ПКМ)
        if (heldStack != null && inSlot == null) {
            surface.setStackInSlot(slot, heldStack.split(1));
            if (heldStack.getCount() <= 0) {
                player.getInventory().setStackInSlot(player.getInventory().getSelectedSlot(), null);
            }
            surface.resetProgress();
            return true;
        }

        return false;
    }

    @Override
    public boolean onLeftClick(World world, BlockPos pos, Player player, ItemStack heldStack, float hitX, float hitY, float hitZ, boolean isNewClick) {
        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof ICraftingSurface surface)) return false;

        // Если бьем по верхней грани (hitY > 0.8) - это попытка крафта
        if (hitY > 0.8f) {
            if (heldStack != null && isNewClick) {
                tryCraft(surface, player, heldStack);
            }
            return true; // Предотвращаем ломание блока, даже если это не "новый" клик
        }

        return false;
    }

    private boolean tryCraft(ICraftingSurface surface, Player player, ItemStack tool) {
        if (tool == null) return false;
        
        Identifier toolId = tool.getItem().getIdentifier();
        List<ItemStack> currentItems = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ItemStack s = surface.getStackInSlot(i);
            if (s != null) currentItems.add(s);
        }

        // com.za.minecraft.utils.Logger.info("Attempting craft with tool %s. Items on stump: %d", toolId, currentItems.size());

        List<com.za.minecraft.world.recipes.IRecipe> recipes = RecipeRegistry.getRecipesByType("in_world_crafting");
        if (recipes.isEmpty()) {
            // com.za.minecraft.utils.Logger.warn("No in_world_crafting recipes found in registry!");
        }

        for (com.za.minecraft.world.recipes.IRecipe r : recipes) {
            InWorldRecipe recipe = (InWorldRecipe) r;
            if (recipe.matches(currentItems, toolId)) {
                surface.incrementProgress();
                player.swing();
                player.addNoise(0.15f);
                
                com.za.minecraft.utils.Logger.info("Progress: %d/%d", surface.getCraftingProgress(), recipe.getRequiredHits());
                
                if (surface.getCraftingProgress() >= recipe.getRequiredHits()) {
                    for (int i = 0; i < 9; i++) surface.setStackInSlot(i, null);
                    ItemStack result = recipe.getResult();
                    surface.setStackInSlot(4, result);
                    surface.resetProgress();
                    com.za.minecraft.utils.Logger.info("Crafting successful: " + result.getItem().getName());
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos) {
        return new StumpBlockEntity(pos);
    }
}
