package com.za.zenith.world.inventory;

import com.za.zenith.world.items.ItemStack;

/**
 * Simple implementation of IInventory for any storage.
 */
public class SimpleInventory implements IInventory {
    private final ItemStack[] slots;

    public SimpleInventory(int size) {
        this.slots = new ItemStack[size];
    }

    @Override
    public ItemStack getStack(int slot) {
        if (slot >= 0 && slot < slots.length) return slots[slot];
        return null;
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        if (slot >= 0 && slot < slots.length) slots[slot] = stack;
    }

    @Override
    public int size() {
        return slots.length;
    }

    @Override
    public boolean isItemValid(int slot, ItemStack stack) {
        return true;
    }

    @Override
    public boolean isSlotActive(int slot) {
        return true;
    }
}


