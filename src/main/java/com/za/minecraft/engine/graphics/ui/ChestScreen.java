package com.za.minecraft.engine.graphics.ui;

import com.za.minecraft.world.inventory.IInventory;
import com.za.minecraft.entities.inventory.Slot;

/**
 * Screen for a simple chest-like container (9x3).
 */
public class ChestScreen extends InventoryScreen {
    private final IInventory containerInventory;
    private final IInventory playerInventory;

    public ChestScreen(IInventory containerInventory, IInventory playerInventory) {
        super("Chest");
        this.containerInventory = containerInventory;
        this.playerInventory = playerInventory;
    }

    @Override
    public void init(int sw, int sh) {
        slots.clear();
        int size = getSlotSize();
        int spacing = getSpacing();

        int cols = 9;
        int rows = containerInventory.size() / cols;
        
        int totalWidth = cols * (size + spacing);
        int totalHeight = (rows + 4) * (size + spacing) + spacing * 4; // container + player (3 rows + hotbar)

        int startX = (sw - totalWidth) / 2;
        int startY = (sh - totalHeight) / 2;

        // Container slots
        for (int i = 0; i < containerInventory.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            slots.add(new SlotUI(new Slot(containerInventory, i, "any"), 
                                startX + col * (size + spacing), 
                                startY + row * (size + spacing)));
        }

        // Player inventory (3 rows: Pockets)
        int playerPocketsStart = startY + (rows + 1) * (size + spacing);
        for (int i = 0; i < 9; i++) { // Simple 9x1 pockets for now
            int col = i % 9;
            int row = i / 9;
            slots.add(new SlotUI(new Slot(playerInventory, 9 + i, "any"), 
                                startX + col * (size + spacing), 
                                playerPocketsStart + row * (size + spacing)));
        }

        // Player Hotbar
        int hotbarY = playerPocketsStart + 2 * (size + spacing);
        for (int i = 0; i < 9; i++) {
            slots.add(new SlotUI(new Slot(playerInventory, i, "any"), 
                                startX + i * (size + spacing), 
                                hotbarY));
        }
    }
}
