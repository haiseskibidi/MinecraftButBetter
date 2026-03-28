package com.za.minecraft.engine.graphics.ui;

import com.za.minecraft.entities.Player;
import com.za.minecraft.engine.core.PlayerMode;
import com.za.minecraft.entities.inventory.Slot;
import com.za.minecraft.entities.inventory.SlotGroup;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

/**
 * Main player inventory screen.
 */
public class PlayerInventoryScreen extends InventoryScreen {
    private final Player player;
    private final List<GroupUI> groups = new ArrayList<>();
    private GroupUI globalBackground;
    private GUIConfig.BackgroundConfig backgroundConfig;

    private String lastLayoutKey = "";

    public PlayerInventoryScreen(Player player) {
        super("Player Inventory");
        this.player = player;
    }

    private String calculateLayoutKey(int sw, int sh) {
        StringBuilder key = new StringBuilder();
        key.append(sw).append("x").append(sh).append("|");
        key.append(player.getMode().name()).append("|");
        
        // Add activity status and size of all groups
        for (SlotGroup group : player.getInventory().getGroups()) {
            key.append(group.getId()).append(":")
               .append(group.isActive()).append(":")
               .append(group.getSlots().size()).append(",");
        }
        return key.toString();
    }

    @Override
    public void init(int sw, int sh) {
        slots.clear();
        groups.clear();
        int slotSize = getSlotSize();
        int spacing = getSpacing();

        GUIConfig config = GUIRegistry.get(com.za.minecraft.utils.Identifier.of("minecraft:player_inventory"));
        if (config != null) {
            LayoutResult result = InventoryLayout.generateLayout(sw, sh, slotSize, spacing, player, config);
            slots.addAll(result.slots);
            groups.addAll(result.groups);
            this.globalBackground = result.globalBackground;
            this.backgroundConfig = config.background;
            this.lastLayoutKey = calculateLayoutKey(sw, sh);
        }
    }

    @Override
    public void render(UIRenderer renderer, int sw, int sh, com.za.minecraft.engine.graphics.DynamicTextureAtlas atlas) {
        // Universal dynamic update: check if any layout-affecting state changed
        String currentKey = calculateLayoutKey(sw, sh);
        if (!currentKey.equals(lastLayoutKey)) {
            init(sw, sh);
        }

        // Draw global background if configured
        if (globalBackground != null && backgroundConfig != null) {
            renderer.renderGroupBackground(globalBackground.getX(), globalBackground.getY(), globalBackground.getWidth(), globalBackground.getHeight(), backgroundConfig);
        }

        // Draw individual group backgrounds if they are NOT part of global background
        for (GroupUI group : groups) {
            if (backgroundConfig != null && backgroundConfig.includeGroups != null && backgroundConfig.includeGroups.contains(group.getConfig().id)) {
                continue; // Already covered by global background
            }
            // Add check for group-specific background if we decide to keep them in GroupConfig (but we moved it to root)
        }
        
        super.render(renderer, sw, sh, atlas);
    }
}
