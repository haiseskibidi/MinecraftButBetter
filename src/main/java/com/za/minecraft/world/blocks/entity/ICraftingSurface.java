package com.za.minecraft.world.blocks.entity;

import com.za.minecraft.world.items.ItemStack;

/**
 * Интерфейс для сущностей блоков, поверхность которых можно использовать для крафта.
 */
public interface ICraftingSurface {
    ItemStack getStackInSlot(int slot);
    void setStackInSlot(int slot, ItemStack stack);
    int getCraftingProgress();
    void incrementProgress();
    void resetProgress();
    
    default int getActiveSlotsCount() {
        int count = 0;
        for (int i = 0; i < 9; i++) {
            if (getStackInSlot(i) != null) count++;
        }
        return count;
    }
}
