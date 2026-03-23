package com.za.minecraft.engine.graphics.ui;

import com.za.minecraft.entities.Inventory;
import com.za.minecraft.engine.core.PlayerMode;
import com.za.minecraft.entities.Player;
import com.za.minecraft.entities.inventory.Slot;
import com.za.minecraft.entities.inventory.SlotGroup;
import com.za.minecraft.world.inventory.IInventory;
import com.za.minecraft.world.inventory.RegistryInventory;
import com.za.minecraft.world.items.ItemStack;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class InventoryLayout {
    
    public static class SlotPos {
        public int x, y;
        public SlotPos(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }

    public static List<SlotUI> generateLayout(int sw, int sh, int slotSize, int spacing, Player player, GUIConfig config) {
        List<SlotUI> slots = new ArrayList<>();
        Inventory inv = player.getInventory();
        
        for (GUIConfig.GroupConfig groupCfg : config.groups) {
            // Check condition
            if (groupCfg.condition != null) {
                if (groupCfg.condition.equals("has_pouch")) {
                    ItemStack acc = inv.getStack(Inventory.SLOT_ACCESSORY);
                    if (acc == null || !acc.getItem().hasComponent(com.za.minecraft.world.items.component.BagComponent.class)) continue;
                } else if (groupCfg.condition.equals("developer_mode")) {
                    if (player.getMode() != PlayerMode.DEVELOPER) continue;
                }
            }

            SlotGroup group = inv.getGroup(groupCfg.id);
            List<Slot> targetSlots = (group != null) ? group.getSlots() : null;

            // Special case for developer_panel which uses RegistryInventory with scrolling
            if (groupCfg.id.equals("developer_panel")) {
                RegistryInventory regInv = new RegistryInventory();
                targetSlots = new ArrayList<>();
                int scroll = com.za.minecraft.engine.core.GameLoop.getInstance().getInputManager().getDevScroll();
                int maxDisplay = groupCfg.cols * groupCfg.rows;
                int startIdx = scroll * groupCfg.cols;
                
                for (int i = 0; i < maxDisplay && (startIdx + i) < regInv.size(); i++) {
                    targetSlots.add(new Slot(regInv, startIdx + i, "any"));
                }
            }

            if (targetSlots == null) continue;

            // Calculate group dimensions for centering
            int groupWidth = 0;
            int groupHeight = 0;
            if (groupCfg.type.equals("grid")) {
                int cols = groupCfg.cols;
                int rows = (targetSlots.size() + cols - 1) / cols;
                groupWidth = cols * slotSize + (cols - 1) * groupCfg.spacing;
                groupHeight = rows * slotSize + (rows - 1) * groupCfg.spacing;
            } else if (groupCfg.type.equals("vertical")) {
                groupWidth = slotSize;
                groupHeight = targetSlots.size() * slotSize + (targetSlots.size() - 1) * groupCfg.spacing;
            } else if (groupCfg.type.equals("horizontal")) {
                groupWidth = targetSlots.size() * slotSize + (targetSlots.size() - 1) * groupCfg.spacing;
                groupHeight = slotSize;
            }

            // Calculate base position from anchor
            int baseX = sw / 2;
            int baseY = sh / 2;

            if (groupCfg.anchor.contains("bottom")) baseY = sh;
            else if (groupCfg.anchor.contains("top")) baseY = 0;
            if (groupCfg.anchor.contains("left")) baseX = 0;
            else if (groupCfg.anchor.contains("right")) baseX = sw;

            // Apply alignX (default: center)
            if (groupCfg.alignX.equals("center")) baseX -= groupWidth / 2;
            else if (groupCfg.alignX.equals("right")) baseX -= groupWidth;

            // Apply alignY (default: center)
            if (groupCfg.alignY.equals("center")) baseY -= groupHeight / 2;
            else if (groupCfg.alignY.equals("bottom")) baseY -= groupHeight;

            int startX = baseX + calculateCoord(groupCfg.x, sw);
            int startY = baseY + calculateCoord(groupCfg.y, sh);

            for (int i = 0; i < targetSlots.size(); i++) {
                int col = 0, row = 0;
                if (groupCfg.type.equals("grid")) {
                    col = i % groupCfg.cols;
                    row = i / groupCfg.cols;
                } else if (groupCfg.type.equals("vertical")) {
                    row = i;
                } else if (groupCfg.type.equals("horizontal")) {
                    col = i;
                }
                
                int x = startX + col * (slotSize + groupCfg.spacing);
                int y = startY + row * (slotSize + groupCfg.spacing);
                slots.add(new SlotUI(targetSlots.get(i), x, y));
            }
        }

        return slots;
    }

    private static int calculateCoord(String value, int total) {
        if (value == null || value.isEmpty()) return 0;
        try {
            if (value.endsWith("%")) {
                float percent = Float.parseFloat(value.substring(0, value.length() - 1)) / 100.0f;
                return (int)(total * percent);
            }
            if (value.endsWith("px")) {
                return Integer.parseInt(value.substring(0, value.length() - 2));
            }
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }
}
