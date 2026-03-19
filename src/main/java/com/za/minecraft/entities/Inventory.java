package com.za.minecraft.entities;

import com.za.minecraft.world.blocks.BlockType;
import com.za.minecraft.world.items.Item;
import com.za.minecraft.world.items.ItemRegistry;
import com.za.minecraft.world.items.ItemStack;
import com.za.minecraft.world.items.ItemType;
import com.za.minecraft.world.items.ToolItem;

public class Inventory {
    public static final int HOTBAR_SIZE = 9;
    
    private ItemStack[] hotbarSlots;
    private int selectedSlot;
    
    public Inventory() {
        this.hotbarSlots = new ItemStack[HOTBAR_SIZE];
        this.selectedSlot = 0;
        
        // Добавляем инструменты в начало хотбара
        hotbarSlots[0] = new ItemStack(ItemRegistry.getItem(ItemType.STONE_KNIFE));
        hotbarSlots[1] = new ItemStack(ItemRegistry.getItem(ItemType.SCRAP_PICKAXE));
        hotbarSlots[2] = new ItemStack(ItemRegistry.getItem(ItemType.CROWBAR));
        
        // Заполняем остальные слоты базовыми блоками
        hotbarSlots[3] = new ItemStack(ItemRegistry.getItem(BlockType.STONE));
        hotbarSlots[4] = new ItemStack(ItemRegistry.getItem(BlockType.WOOD));
        hotbarSlots[5] = new ItemStack(ItemRegistry.getItem(BlockType.OAK_PLANKS));
        hotbarSlots[6] = new ItemStack(ItemRegistry.getItem(BlockType.COBBLESTONE));
        hotbarSlots[7] = new ItemStack(ItemRegistry.getItem(BlockType.RUSTY_METAL));
        hotbarSlots[8] = new ItemStack(ItemRegistry.getItem(BlockType.ASPHALT));
    }
    
    public ItemStack getSelectedItemStack() {
        return hotbarSlots[selectedSlot];
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
    
    public ItemStack getStackInSlot(int slot) {
        if (slot >= 0 && slot < HOTBAR_SIZE) {
            return hotbarSlots[slot];
        }
        return null;
    }
    
    public void setStackInSlot(int slot, ItemStack stack) {
        if (slot >= 0 && slot < HOTBAR_SIZE) {
            hotbarSlots[slot] = stack;
        }
    }
    
    public void nextSlot() {
        selectedSlot = (selectedSlot + 1) % HOTBAR_SIZE;
        logSelection();
    }
    
    public void previousSlot() {
        selectedSlot = (selectedSlot - 1 + HOTBAR_SIZE) % HOTBAR_SIZE;
        logSelection();
    }
    
    private void logSelection() {
        Item current = getSelectedItem();
        String name = (current != null) ? current.getName() : "EMPTY";
        com.za.minecraft.utils.Logger.info("Selected slot %d: %s", selectedSlot, name);
    }
}
