package com.za.zenith.world.blocks;

import com.za.zenith.utils.Identifier;
import com.za.zenith.world.BlockPos;
import com.za.zenith.world.World;
import com.za.zenith.entities.Player;
import com.za.zenith.world.blocks.entity.BlockEntity;
import com.za.zenith.world.blocks.entity.ScavengerTableBlockEntity;
import com.za.zenith.world.blocks.entity.ICraftingSurface;
import com.za.zenith.world.items.ItemStack;
import com.za.zenith.world.recipes.RecipeRegistry;
import com.za.zenith.world.recipes.InWorldRecipe;
import java.util.ArrayList;
import java.util.List;

/**
 * Определение блока для Стола Мусорщика (Scavenger Table).
 * Использует универсальный In-World Crafting API.
 */
public class ScavengerTableBlockDefinition extends BlockDefinition {
    public ScavengerTableBlockDefinition(int id, Identifier identifier, String translationKey, boolean solid, boolean transparent) {
        super(id, identifier, translationKey, solid, transparent);
    }

    @Override
    public boolean hasOnUse() {
        return true;
    }

    @Override
    public boolean onUse(World world, BlockPos pos, Player player, ItemStack heldStack, float hitX, float hitY, float hitZ) {
        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof ScavengerTableBlockEntity)) {
            be = new ScavengerTableBlockEntity(pos);
            world.setBlockEntity(be);
        }
        ScavengerTableBlockEntity table = (ScavengerTableBlockEntity) be;

        // Разрешаем взаимодействие только с верхней ("рабочей") частью стола
        if (hitY <= 0.8f) return false;

        ICraftingSurface surface = table;
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
        if (!(be instanceof ScavengerTableBlockEntity table)) return false;
        
        ICraftingSurface surface = table;

        // Если бьем по верхней грани (hitY > 0.8) - попытка крафта
        if (hitY > 0.8f) {
            if (isNewClick) {
                if (tryCraft(surface, player, heldStack)) {
                    return true;
                }
            }
            return true; 
        }

        return false;
    }

    private boolean tryCraft(ICraftingSurface surface, Player player, ItemStack tool) {
        // Всегда визуализируем удар
        player.swing();
        player.performDiscreteAction(com.za.zenith.utils.Identifier.of("zenith:mine"));

        Identifier toolId = (tool != null) ? tool.getItem().getIdentifier() : com.za.zenith.world.items.Items.HAND.getIdentifier();
        
        List<ItemStack> currentItems = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ItemStack s = surface.getStackInSlot(i);
            if (s != null) currentItems.add(s);
        }

        List<com.za.zenith.world.recipes.IRecipe> recipes = RecipeRegistry.getRecipesByType("in_world_crafting");

        for (com.za.zenith.world.recipes.IRecipe r : recipes) {
            InWorldRecipe recipe = (InWorldRecipe) r;
            if (recipe.matches(currentItems, toolId, this.getIdentifier())) {
                surface.incrementProgress();

                if (surface.getCraftingProgress() >= recipe.getRequiredHits()) {
                    for (int i = 0; i < 9; i++) surface.setStackInSlot(i, null);
                    ItemStack result = recipe.getResult();
                    surface.setStackInSlot(4, result);
                    surface.resetProgress();
                }
                return true;
            }
        }
        return false;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos) {
        return new ScavengerTableBlockEntity(pos);
    }
}
