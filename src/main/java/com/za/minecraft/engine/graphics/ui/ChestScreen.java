package com.za.minecraft.engine.graphics.ui;

import com.za.minecraft.world.inventory.IInventory;
import com.za.minecraft.entities.inventory.Slot;
import java.util.ArrayList;
import java.util.List;

/**
 * Screen for a simple chest-like container (9x3).
 */
public class ChestScreen extends InventoryScreen {
    private final com.za.minecraft.world.inventory.IInventory containerInventory;
    private final com.za.minecraft.world.inventory.IInventory playerInventory;
    private final List<GroupUI> groups = new ArrayList<>();
    private GroupUI globalBackground;
    private GUIConfig.BackgroundConfig backgroundConfig;

    public ChestScreen(com.za.minecraft.world.inventory.IInventory containerInventory, com.za.minecraft.world.inventory.IInventory playerInventory) {
        super("Chest");
        this.containerInventory = containerInventory;
        this.playerInventory = playerInventory;
    }

    @Override
    public void init(int sw, int sh) {
        slots.clear();
        groups.clear();
        int size = getSlotSize();
        int spacing = getSpacing();

        GUIConfig config = GUIRegistry.get(com.za.minecraft.utils.Identifier.of("minecraft:chest"));
        if (config == null) {
            // Fallback to old hardcoded logic if no config found
            int cols = 9;
            int rows = containerInventory.size() / cols;
            int totalWidth = cols * (size + spacing);
            int totalHeight = (rows + 4) * (size + spacing) + spacing * 4;
            int startX = (sw - totalWidth) / 2;
            int startY = (sh - totalHeight) / 2;

            for (int i = 0; i < containerInventory.size(); i++) {
                int col = i % cols;
                int row = i / cols;
                slots.add(new SlotUI(new com.za.minecraft.entities.inventory.Slot(containerInventory, i, "any"), 
                                    startX + col * (size + spacing), 
                                    startY + row * (size + spacing)));
            }
            int playerPocketsStart = startY + (rows + 1) * (size + spacing);
            for (int i = 0; i < 9; i++) {
                slots.add(new SlotUI(new com.za.minecraft.entities.inventory.Slot(playerInventory, 9 + i, "any"), 
                                    startX + i * (size + spacing), 
                                    playerPocketsStart));
            }
            int hotbarY = playerPocketsStart + 2 * (size + spacing);
            for (int i = 0; i < 9; i++) {
                slots.add(new SlotUI(new com.za.minecraft.entities.inventory.Slot(playerInventory, i, "any"), 
                                    startX + i * (size + spacing), 
                                    hotbarY));
            }
        } else {
            // Use flex layout system
            LayoutResult result = InventoryLayout.generateLayout(sw, sh, size, spacing, null, config);
            slots.addAll(result.slots);
            groups.addAll(result.groups);
            this.globalBackground = result.globalBackground;
            this.backgroundConfig = config.background;
        }
    }

    @Override
    public void render(UIRenderer renderer, int sw, int sh, com.za.minecraft.engine.graphics.DynamicTextureAtlas atlas) {
        if (globalBackground != null && backgroundConfig != null) {
            renderer.renderGroupBackground(globalBackground.getX(), globalBackground.getY(), globalBackground.getWidth(), globalBackground.getHeight(), backgroundConfig);
        }
        super.render(renderer, sw, sh, atlas);
    }
}
