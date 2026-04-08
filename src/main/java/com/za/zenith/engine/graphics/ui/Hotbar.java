package com.za.zenith.engine.graphics.ui;

import com.za.zenith.entities.Player;
import com.za.zenith.world.items.ItemStack;

public class Hotbar {
    public static final int HOTBAR_SLOTS = 9;
    
    // Масштаб для увеличения хотбара (может использоваться для базового размера слота)
    public static final float HOTBAR_SCALE = 2.0f;
    public static final int HOTBAR_WIDTH = 182; // Константа для совместимости в других GUI
    
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

    // Для совместимости в HUD элементах, которые позиционируются относительно хотбара
    public int getScreenY(int windowHeight) {
        // Примерное положение, для HUD элементов типа голода/шума
        return (int) (windowHeight - 22 * HOTBAR_SCALE - 20);
    }
}
