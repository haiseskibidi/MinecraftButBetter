package com.za.minecraft.world.inventory;

import com.za.minecraft.world.items.ItemStack;

/**
 * Common interface for anything that can store items.
 */
public interface IInventory {
    ItemStack getStack(int slot);
    void setStack(int slot, ItemStack stack);
    int size();
    boolean isItemValid(int slot, ItemStack stack);
    boolean isSlotActive(int slot);
    
    default void consume(int slot, int amount) {
        ItemStack stack = getStack(slot);
        if (stack != null) {
            stack.setCount(stack.getCount() - amount);
            if (stack.getCount() <= 0) {
                setStack(slot, null);
            }
        }
    }
}
