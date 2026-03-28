package com.za.minecraft.world.inventory;

import com.za.minecraft.world.items.ItemStack;

public class ItemInventory implements IInventory {
    private final ItemStack[] slots;

    public ItemInventory(int size) {
        this.slots = new ItemStack[size];
    }

    @Override
    public int size() {
        return slots.length;
    }

    @Override
    public ItemStack getStack(int index) {
        if (index >= 0 && index < slots.length) {
            return slots[index];
        }
        return null;
    }

    @Override
    public void setStack(int index, ItemStack stack) {
        if (index >= 0 && index < slots.length) {
            slots[index] = stack;
        }
    }

    @Override
    public boolean isItemValid(int index, ItemStack stack) {
        // Can't put a bag into a bag (simple check to prevent inception and infinite recursion)
        if (stack != null && stack.getItem().hasComponent(com.za.minecraft.world.items.component.BagComponent.class)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isSlotActive(int slotIndex) {
        return true;
    }

    public ItemStack[] getSlots() {
        return slots;
    }
    
    public ItemInventory copy() {
        ItemInventory newInv = new ItemInventory(slots.length);
        for (int i = 0; i < slots.length; i++) {
            if (slots[i] != null) {
                newInv.slots[i] = slots[i].copy();
            }
        }
        return newInv;
    }
}