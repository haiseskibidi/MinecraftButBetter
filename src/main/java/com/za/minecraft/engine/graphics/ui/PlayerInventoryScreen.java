package com.za.minecraft.engine.graphics.ui;

import com.za.minecraft.entities.Player;
import com.za.minecraft.engine.core.PlayerMode;
import com.za.minecraft.entities.inventory.Slot;
import com.za.minecraft.entities.inventory.SlotGroup;
import java.util.Map;

/**
 * Main player inventory screen.
 */
public class PlayerInventoryScreen extends InventoryScreen {
    private final Player player;

    public PlayerInventoryScreen(Player player) {
        super("Player Inventory");
        this.player = player;
    }

    @Override
    public void init(int sw, int sh) {
        slots.clear();
        int slotSize = getSlotSize();
        int spacing = getSpacing();

        GUIConfig config = GUIRegistry.get(com.za.minecraft.utils.Identifier.of("minecraft:player_inventory"));
        if (config != null) {
            slots.addAll(InventoryLayout.generateLayout(sw, sh, slotSize, spacing, player, config));
        }
    }
}
