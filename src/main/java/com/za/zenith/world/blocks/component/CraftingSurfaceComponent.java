package com.za.zenith.world.blocks.component;

import com.google.gson.annotations.SerializedName;
import com.za.zenith.world.BlockPos;
import com.za.zenith.world.World;
import com.za.zenith.entities.Player;
import com.za.zenith.world.items.ItemStack;
import com.za.zenith.world.blocks.entity.ModularBlockEntity;
import com.za.zenith.world.recipes.RecipeRegistry;
import com.za.zenith.world.recipes.InWorldRecipe;
import com.za.zenith.world.blocks.InteractionZone;
import com.za.zenith.world.physics.AABB;
import java.util.Collections;
import java.util.List;

/**
 * Компонент для создания предметов на поверхности блока (как пень).
 * Логика: 
 * - RMB пустой рукой: забрать предмет.
 * - RMB с предметом в пустой слот: положить.
 * - RMB с предметом в тот же предмет: забрать.
 * - RMB с предметом в другой предмет: поменять местами.
 * - Shift + RMB: забрать ВСЁ.
 * - LMB: крафт.
 */
public class CraftingSurfaceComponent extends BlockComponent implements InventoryProvider {
    @SerializedName("recipe_type")
    private String recipeType = "in_world_crafting";
    @SerializedName("slots")
    private int slots = 9;
    @SerializedName("grid_size")
    private int gridSize = 3; 

    @Override
    public int getRequiredInventorySize() {
        return slots;
    }

    @Override
    public boolean hasOnUse() {
        return true;
    }

    @Override
    public List<InteractionZone> getInteractionZones(World world, BlockPos pos) {
        return Collections.singletonList(new InteractionZone(new AABB(-0.5f, 0.7f, -0.5f, 1.5f, 1.6f, 1.5f), null));
    }

    @Override
    public void addDynamicBoxes(World world, BlockPos pos, List<AABB> boxes) {
        var be = world.getBlockEntity(pos);
        if (!(be instanceof ModularBlockEntity modular)) return;

        int total = 0;
        for (int i = 0; i < modular.size(); i++) if (modular.getStack(i) != null) total++;
        if (total == 0) return;

        for (int i = 0; i < modular.size(); i++) {
            var stack = modular.getStack(i);
            if (stack == null) continue;

            var t = com.za.zenith.world.blocks.CraftingLayoutEngine.getSlotTransform(i, total, gridSize);
            float centerX = 0.5f + t.x;
            float centerZ = 0.5f + t.z;
            float scale = (stack.getItem().isBlock() ? 0.4f : stack.getItem().getDroppedScale() * 0.6f) * t.y;
            
            float hw = scale * 0.5f;
            float yMin = 1.0f;
            float yMax = 1.0f + (stack.getItem().isBlock() ? scale : 0.08f);
            
            boxes.add(new AABB(
                centerX - hw, yMin, centerZ - hw,
                centerX + hw, yMax, centerZ + hw
            ));
        }
    }

    @Override
    public boolean onUse(World world, BlockPos pos, Player player, ItemStack heldStack, float hitX, float hitY, float hitZ) {
        if (hitY < 0.7f) return false;

        var be = world.getBlockEntity(pos);
        if (!(be instanceof ModularBlockEntity modular)) return false;

        // 1. Shift + ПКМ: Забрать ВСЁ
        if (player.isSneaking()) {
            boolean takenAny = false;
            for (int i = 0; i < modular.size(); i++) {
                ItemStack stack = modular.getStack(i);
                if (stack != null) {
                    if (player.getInventory().addItem(stack)) {
                        modular.setStack(i, null);
                        takenAny = true;
                    }
                }
            }
            if (takenAny) {
                modular.setFloat(ModularBlockEntity.PROP_CRAFT_HITS, 0);
                modular.setFloat(ModularBlockEntity.PROP_CRAFT_PROGRESS, 0);
            }
            return takenAny;
        }

        int slot = com.za.zenith.world.blocks.CraftingLayoutEngine.getSlotIndex(hitX, hitZ, gridSize);
        if (slot >= modular.size()) slot = modular.size() - 1;
        ItemStack existing = modular.getStack(slot);

        // 2. ПКМ пустой рукой: Забрать предмет
        if (heldStack == null) {
            if (existing != null) {
                if (player.getInventory().addItem(existing)) {
                    modular.setStack(slot, null);
                    modular.setFloat(ModularBlockEntity.PROP_CRAFT_HITS, 0);
                    modular.setFloat(ModularBlockEntity.PROP_CRAFT_PROGRESS, 0);
                    return true;
                }
            }
            return false;
        }

        // 3. ПКМ с предметом
        if (existing == null) {
            // В пустой слот - кладём 1 шт
            ItemStack toPlace = heldStack.copy();
            toPlace.setCount(1);
            modular.setStack(slot, toPlace);
            heldStack.setCount(heldStack.getCount() - 1);
            if (heldStack.getCount() <= 0) {
                player.getInventory().setStackInSlot(player.getInventory().getSelectedSlot(), null);
            }
            modular.setFloat(ModularBlockEntity.PROP_CRAFT_HITS, 0);
            modular.setFloat(ModularBlockEntity.PROP_CRAFT_PROGRESS, 0);
            return true;
        } else {
            // В слот с предметом
            if (existing.getItem().getId() == heldStack.getItem().getId()) {
                // Тот же предмет - забираем
                if (player.getInventory().addItem(existing)) {
                    modular.setStack(slot, null);
                    modular.setFloat(ModularBlockEntity.PROP_CRAFT_HITS, 0);
                    modular.setFloat(ModularBlockEntity.PROP_CRAFT_PROGRESS, 0);
                    return true;
                }
            } else {
                // Другой предмет - меняем местами (только если heldStack = 1 шт, иначе неудобно)
                if (heldStack.getCount() == 1) {
                    ItemStack fromSurface = existing.copy();
                    ItemStack fromHand = heldStack.copy();
                    modular.setStack(slot, fromHand);
                    player.getInventory().setStackInSlot(player.getInventory().getSelectedSlot(), fromSurface);
                    modular.setFloat(ModularBlockEntity.PROP_CRAFT_HITS, 0);
                    modular.setFloat(ModularBlockEntity.PROP_CRAFT_PROGRESS, 0);
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean onLeftClick(World world, BlockPos pos, Player player, ItemStack heldStack, float hitX, float hitY, float hitZ, boolean isNewClick) {
        if (hitY < 0.7f) return false;
        
        if (isNewClick) {
            if (tryCraft(world, pos, player, heldStack)) return true;
        }
        
        var be = world.getBlockEntity(pos);
        if (be instanceof ModularBlockEntity modular) {
             for (int i = 0; i < modular.size(); i++) {
                 if (modular.getStack(i) != null) return true;
             }
        }

        return false; 
    }

    private boolean tryCraft(World world, BlockPos pos, Player player, ItemStack tool) {
        var be = (ModularBlockEntity) world.getBlockEntity(pos);
        var recipes = RecipeRegistry.getRecipesByType(recipeType);
        
        java.util.List<ItemStack> inputs = new java.util.ArrayList<>();
        for (int i = 0; i < be.size(); i++) {
            if (be.getStack(i) != null) inputs.add(be.getStack(i));
        }

        if (inputs.isEmpty()) return false;

        player.swing();

        for (var r : recipes) {
            if (!(r instanceof InWorldRecipe recipe)) continue;
            
            var surfaceDef = com.za.zenith.world.blocks.BlockRegistry.getBlock(world.getBlock(pos).getType());
            if (surfaceDef == null) continue;

            if (recipe.matches(inputs, tool != null ? tool.getItem().getIdentifier() : com.za.zenith.world.items.Items.HAND.getIdentifier(), surfaceDef.getIdentifier())) {
                float currentHits = be.getFloat(ModularBlockEntity.PROP_CRAFT_HITS, 0) + 1.0f;
                
                if (currentHits >= recipe.getRequiredHits()) {
                    be.setFloat(ModularBlockEntity.PROP_CRAFT_HITS, 0);
                    be.setFloat(ModularBlockEntity.PROP_CRAFT_PROGRESS, 0);
                    for (int i = 0; i < be.size(); i++) be.setStack(i, null);
                    
                    ItemStack result = recipe.getResult().copy();
                    if (!player.getInventory().addItem(result)) {
                        world.spawnItem(result, pos.x() + 0.5f, pos.y() + 1.1f, pos.z() + 0.5f);
                    }
                } else {
                    be.setFloat(ModularBlockEntity.PROP_CRAFT_HITS, currentHits);
                    be.setFloat(ModularBlockEntity.PROP_CRAFT_PROGRESS, currentHits / (float)recipe.getRequiredHits());
                }
                return true;
            }
        }
        return false;
    }
    
    public int getGridSize() {
        return gridSize;
    }
}
