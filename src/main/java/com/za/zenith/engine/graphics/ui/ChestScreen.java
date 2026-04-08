package com.za.zenith.engine.graphics.ui;

import com.za.zenith.world.inventory.IInventory;
import com.za.zenith.entities.inventory.Slot;
import java.util.ArrayList;
import java.util.List;

/**
 * Screen for a simple chest-like container (9x3).
 */
public class ChestScreen extends InventoryScreen {
    private final com.za.zenith.world.inventory.IInventory containerInventory;
    private final com.za.zenith.entities.Player player;
    private final com.za.zenith.utils.Identifier guiId;
    private final List<GroupUI> groups = new ArrayList<>();
    private GroupUI globalBackground;
    private GUIConfig.BackgroundConfig backgroundConfig;

    public ChestScreen(com.za.zenith.world.inventory.IInventory containerInventory, com.za.zenith.entities.Player player, com.za.zenith.utils.Identifier guiId) {
        super("Container");
        this.containerInventory = containerInventory;
        this.player = player;
        this.guiId = guiId;
    }

    @Override
    public void init(int sw, int sh) {
        slots.clear();
        groups.clear();
        int size = getSlotSize();
        int spacing = getSpacing();

        GUIConfig config = GUIRegistry.get(guiId);
        if (config != null) {
            java.util.Map<String, com.za.zenith.world.inventory.IInventory> inventories = new java.util.HashMap<>();
            inventories.put("player", player.getInventory());
            inventories.put("container", containerInventory);
            
            LayoutResult result = InventoryLayout.generateLayout(sw, sh, size, spacing, player, config, inventories);
            slots.addAll(result.slots);
            groups.addAll(result.groups);
            this.globalBackground = result.globalBackground;
            this.backgroundConfig = config.background;
        } else {
            // Absolute fallback if JSON missing (9x3 chest)
            int cols = 9;
            int rows = containerInventory.size() / cols;
            int startX = (sw - cols * (size + spacing)) / 2;
            int startY = (sh - (rows + 4) * (size + spacing)) / 2;
            for (int i = 0; i < containerInventory.size(); i++) {
                slots.add(new SlotUI(new com.za.zenith.entities.inventory.Slot(containerInventory, i, "any"), 
                                    startX + (i % cols) * (size + spacing), startY + (i / cols) * (size + spacing), "container"));
            }
        }
    }

    @Override
    protected com.za.zenith.utils.Identifier getScreenIdentifier() {
        return guiId;
    }

    @Override
    public void render(UIRenderer renderer, int sw, int sh, com.za.zenith.engine.graphics.DynamicTextureAtlas atlas) {
        if (globalBackground != null && backgroundConfig != null) {
            renderer.renderGroupBackground(globalBackground.getX(), globalBackground.getY(), globalBackground.getWidth(), globalBackground.getHeight(), backgroundConfig);
        }
        super.render(renderer, sw, sh, atlas);
    }
}


