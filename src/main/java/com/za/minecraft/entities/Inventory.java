package com.za.minecraft.entities;

import com.za.minecraft.world.blocks.Blocks;
import com.za.minecraft.world.items.Item;
import com.za.minecraft.world.items.ItemRegistry;
import com.za.minecraft.world.items.ItemStack;
import com.za.minecraft.world.items.Items;
import com.za.minecraft.world.items.ToolItem;

public class Inventory {
    public static final int HOTBAR_SIZE = 9;
    public static final int MAIN_INVENTORY_SIZE = 27;
    public static final int TOTAL_SIZE = MAIN_INVENTORY_SIZE + HOTBAR_SIZE;
    
    private ItemStack[] slots; // 0-8 hotbar, 9-35 main inventory
    private int selectedSlot; // 0-8
    
    public Inventory() {
        this.slots = new ItemStack[TOTAL_SIZE];
        this.selectedSlot = 0;
        
        // Initial items in hotbar (slots 0-8)
        slots[0] = new ItemStack(Items.STONE_KNIFE);
        slots[1] = new ItemStack(Items.SCRAP_PICKAXE);
        slots[2] = new ItemStack(Items.CROWBAR);
        slots[3] = new ItemStack(ItemRegistry.getItem(Blocks.STONE.getId()));
        slots[4] = new ItemStack(ItemRegistry.getItem(Blocks.OAK_LOG.getId()));
        slots[5] = new ItemStack(ItemRegistry.getItem(Blocks.OAK_PLANKS.getId()));
        slots[6] = new ItemStack(ItemRegistry.getItem(Blocks.COBBLESTONE.getId()));
        slots[7] = new ItemStack(ItemRegistry.getItem(Blocks.RUSTY_METAL.getId()));
        slots[8] = new ItemStack(ItemRegistry.getItem(Blocks.ASPHALT.getId()));
    }
    
    public ItemStack getSelectedItemStack() {
        return slots[selectedSlot];
    }
    
    public Item getSelectedItem() {
        ItemStack stack = getSelectedItemStack();
        return stack != null ? stack.getItem() : null;
    }
    
    public int getSelectedSlot() {
        return selectedSlot;
    }
    
    public void setSelectedSlot(int slot) {
        if (slot >= 0 && slot < HOTBAR_SIZE) {
            this.selectedSlot = slot;
        }
    }

    public void consumeSelected(int amount) {
        ItemStack stack = getSelectedItemStack();
        if (stack != null) {
            stack.setCount(stack.getCount() - amount);
            if (stack.getCount() <= 0) {
                setStackInSlot(selectedSlot, null);
            }
        }
    }
    
    public ItemStack getStackInSlot(int slot) {
        if (slot >= 0 && slot < TOTAL_SIZE) {
            return slots[slot];
        }
        return null;
    }
    
    public void setStackInSlot(int slot, ItemStack stack) {
        if (slot >= 0 && slot < TOTAL_SIZE) {
            slots[slot] = stack;
        }
    }

    public boolean addItem(ItemStack stack) {
        if (stack == null) return false;
        
        // 1. Try to stack in hotbar (0-8)
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            if (slots[i] != null && slots[i].getItem().getId() == stack.getItem().getId()) {
                slots[i].setCount(slots[i].getCount() + stack.getCount());
                return true;
            }
        }
        
        // 2. Try to stack in main inventory (9-35)
        for (int i = HOTBAR_SIZE; i < TOTAL_SIZE; i++) {
            if (slots[i] != null && slots[i].getItem().getId() == stack.getItem().getId()) {
                slots[i].setCount(slots[i].getCount() + stack.getCount());
                return true;
            }
        }
        
        // 3. Find empty in hotbar
        for (int i = 0; i < HOTBAR_SIZE; i++) {
            if (slots[i] == null) {
                slots[i] = stack;
                return true;
            }
        }

        // 4. Find empty in main
        for (int i = HOTBAR_SIZE; i < TOTAL_SIZE; i++) {
            if (slots[i] == null) {
                slots[i] = stack;
                return true;
            }
        }
        return false;
    }
    
    public void nextSlot() {
        selectedSlot = (selectedSlot + 1) % HOTBAR_SIZE;
        logSelection();
    }
    
    public void previousSlot() {
        selectedSlot = (selectedSlot - 1 + HOTBAR_SIZE) % HOTBAR_SIZE;
        logSelection();
    }
    
    public void swapSlots(int slotA, int slotB) {
        if (slotA >= 0 && slotA < TOTAL_SIZE && slotB >= 0 && slotB < TOTAL_SIZE) {
            ItemStack temp = slots[slotA];
            slots[slotA] = slots[slotB];
            slots[slotB] = temp;
        }
    }

    public void quickMove(int slotIndex) {
        ItemStack stack = getStackInSlot(slotIndex);
        if (stack == null) return;

        int start, end;
        if (slotIndex < HOTBAR_SIZE) {
            // From hotbar to main inventory
            start = HOTBAR_SIZE;
            end = TOTAL_SIZE;
        } else {
            // From main inventory to hotbar
            start = 0;
            end = HOTBAR_SIZE;
        }

        // 1. Try to stack
        for (int i = start; i < end; i++) {
            if (slots[i] != null && slots[i].isStackableWith(stack)) {
                slots[i].setCount(slots[i].getCount() + stack.getCount());
                setStackInSlot(slotIndex, null);
                return;
            }
        }

        // 2. Try to find empty slot
        for (int i = start; i < end; i++) {
            if (slots[i] == null) {
                setStackInSlot(i, stack);
                setStackInSlot(slotIndex, null);
                return;
            }
        }
    }
    
    public void sortMainInventory() {
        for (int i = HOTBAR_SIZE; i < TOTAL_SIZE; i++) {
            if (slots[i] != null) {
                for (int j = i + 1; j < TOTAL_SIZE; j++) {
                    if (slots[j] != null && slots[i].isStackableWith(slots[j])) {
                        slots[i].setCount(slots[i].getCount() + slots[j].getCount());
                        slots[j] = null;
                    }
                }
            }
        }
        for (int i = HOTBAR_SIZE; i < TOTAL_SIZE; i++) {
            for (int j = i + 1; j < TOTAL_SIZE; j++) {
                boolean swap = false;
                if (slots[i] == null && slots[j] != null) swap = true;
                else if (slots[i] != null && slots[j] != null) {
                    if (slots[i].getItem().getId() > slots[j].getItem().getId()) swap = true;
                }
                if (swap) {
                    ItemStack temp = slots[i];
                    slots[i] = slots[j];
                    slots[j] = temp;
                }
            }
        }
    }
    
    private void logSelection() {
        Item current = getSelectedItem();
        String name = (current != null) ? current.getName() : "EMPTY";
        com.za.minecraft.utils.Logger.info("Selected slot %d: %s", selectedSlot, name);
    }
}
