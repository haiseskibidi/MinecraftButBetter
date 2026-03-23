package com.za.minecraft.engine.graphics.ui;

import com.za.minecraft.entities.Player;
import com.za.minecraft.world.items.ItemStack;

public class Hotbar {
    public static final int HOTBAR_SLOTS = 9;
    public static final int HOTBAR_WIDTH = 182;
    public static final int HOTBAR_HEIGHT = 22;
    public static final int SLOT_SIZE = 20;
    public static final int SLOT_SPACING = 20;
    
    // Масштаб для увеличения хотбара
    public static final float HOTBAR_SCALE = 2.0f;
    
    // Размеры элементов хотбара (теперь используем отдельные файлы)
    public static final int HOTBAR_SELECTION_WIDTH = 24;
    public static final int HOTBAR_SELECTION_HEIGHT = 24;
    
    private Player player;
    
    public Hotbar(Player player) {
        this.player = player;
    }
    
    public Player getPlayer() {
        return player;
    }
    
    public void setSelectedSlot(int slot) {
        if (slot >= 0 && slot < HOTBAR_SLOTS) {
            player.getInventory().setSelectedSlot(slot);
        }
    }
    
    public int getSelectedSlot() {
        return player.getInventory().getSelectedSlot();
    }
    
    public void selectNext() {
        player.getInventory().nextSlot();
    }
    
    public void selectPrevious() {
        player.getInventory().previousSlot();
    }
    
    public ItemStack getSelectedItemStack() {
        return player.getInventory().getSelectedItemStack();
    }
    
    public ItemStack getStackInSlot(int slot) {
        return player.getInventory().getStackInSlot(slot);
    }
    
    // Позиция хотбара на экране (центр нижней части)
    public int getScreenX(int windowWidth) {
        return (int) ((windowWidth - HOTBAR_WIDTH * HOTBAR_SCALE) / 2);
    }
    
    public int getScreenY(int windowHeight) {
        return (int) (windowHeight - HOTBAR_HEIGHT * HOTBAR_SCALE - 20); // 20 пикселей от края
    }
    
    // Позиция конкретного слота
    public int getSlotScreenX(int windowWidth, int slot) {
        int hotbarX = getScreenX(windowWidth);
        return (int) (hotbarX + 3 * HOTBAR_SCALE + (slot * SLOT_SPACING * HOTBAR_SCALE));
    }
    
    public int getSlotScreenY(int windowHeight) {
        return (int) (getScreenY(windowHeight) + 3 * HOTBAR_SCALE);
    }
    
    // Позиция выделения выбранного слота
    public int getSelectionScreenX(int windowWidth) {
        int currentSlot = player.getInventory().getSelectedSlot();
        int slotX = getSlotScreenX(windowWidth, currentSlot);
        return (int) (slotX - 4 * HOTBAR_SCALE);
    }
    
    public int getSelectionScreenY(int windowHeight) {
        int slotY = getSlotScreenY(windowHeight);
        return (int) (slotY - 4 * HOTBAR_SCALE);
    }
}
