package com.za.zenith.world.blocks;

import com.za.zenith.utils.Identifier;
import com.za.zenith.world.BlockPos;
import com.za.zenith.world.World;
import com.za.zenith.entities.Player;
import com.za.zenith.world.blocks.entity.BlockEntity;
import com.za.zenith.world.blocks.entity.StumpBlockEntity;
import com.za.zenith.world.blocks.entity.ICraftingSurface;
import com.za.zenith.world.items.ItemStack;
import com.za.zenith.world.items.Items;
import com.za.zenith.world.recipes.RecipeRegistry;
import com.za.zenith.world.recipes.InWorldRecipe;
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
    public boolean hasOnUse() {
        return true;
    }

    @Override
    public java.util.List<InteractionZone> getInteractionZones(World world, BlockPos pos) {
        // Рабочая область пня - верхние 20% блока
        return java.util.List.of(new InteractionZone(
            new com.za.zenith.world.physics.AABB(0, 0.8f, 0, 1, 1, 1),
            null
        ));
    }

    @Override
    public boolean onUse(World world, BlockPos pos, Player player, ItemStack heldStack, float hitX, float hitY, float hitZ) {
        BlockEntity be = world.getBlockEntity(pos);
        if (!(be instanceof StumpBlockEntity)) {
            be = new StumpBlockEntity(pos);
            world.setBlockEntity(be);
        }
        StumpBlockEntity stump = (StumpBlockEntity) be;
        org.joml.Vector3f localHit = new org.joml.Vector3f(hitX, hitY, hitZ);

        // --- Логика обтёсывания (Carving) ---
        if (!stump.isFullyCarved()) {
            if (heldStack != null && heldStack.getItem().getIdentifier().getPath().contains("knife") && player.isSneaking() && isInteractableAt(world, pos, localHit)) {
                if (!stump.canCarve()) return true; 

                // Вычисляем индекс зоны 4x4 через движок
                int index = CarvingLayoutEngine.getZoneIndex(hitX, hitZ);
                stump.setCarvingBit(index);

                com.za.zenith.utils.Logger.info("Carving stump at zone %d, mask: %d", index, stump.getCarvingMask());

                // Если всё обтёсано - превращаем в финальный пень
                if (stump.isFullyCarved()) {
                    world.setBlock(pos, new Block(Blocks.STUMP.getId()));
                    StumpBlockEntity finalStump = (StumpBlockEntity) world.getBlockEntity(pos);
                    if (finalStump != null) {
                        finalStump.setCarvingMask(0xFFFF);
                        com.za.zenith.utils.Logger.info("Stump carving finished!");
                    }
                }
                return true;
            }
            return false;
        }

        // --- Обычная логика крафта ---
        if (!isInteractableAt(world, pos, localHit)) return false;

        ICraftingSurface surface = stump;
        int slot = CraftingLayoutEngine.getSlotIndex(hitX, hitZ);
        ItemStack inSlot = surface.getStackInSlot(slot);

        // 1. Снятие предмета (Shift + ПКМ)
        if (player.isSneaking()) {
            if (inSlot != null) {
                player.getInventory().addItem(inSlot, true);
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
        if (!(be instanceof StumpBlockEntity stump) || !stump.isFullyCarved()) return false;
        
        ICraftingSurface surface = stump;
        org.joml.Vector3f localHit = new org.joml.Vector3f(hitX, hitY, hitZ);

        if (isInteractableAt(world, pos, localHit)) {
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
        Identifier toolId = (tool != null) ? tool.getItem().getIdentifier() : com.za.zenith.world.items.Items.HAND.getIdentifier();
        
        List<ItemStack> currentItems = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ItemStack s = surface.getStackInSlot(i);
            if (s != null) currentItems.add(s);
        }

        List<com.za.zenith.world.recipes.IRecipe> recipes = RecipeRegistry.getRecipesByType("in_world_crafting");

        for (com.za.zenith.world.recipes.IRecipe r : recipes) {
            InWorldRecipe recipe = (InWorldRecipe) r;
            // Возвращаем простую проверку matches без ID поверхности (откат)
            if (recipe.matches(currentItems, toolId, this.getIdentifier())) {
                surface.incrementProgress();
                player.swing();
                player.performDiscreteAction(com.za.zenith.utils.Identifier.of("zenith:mine"));

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
        return new StumpBlockEntity(pos);
    }
}
