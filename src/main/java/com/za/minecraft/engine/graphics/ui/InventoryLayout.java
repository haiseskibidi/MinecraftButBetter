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

    public static class GroupBounds {
        public int x, y, width, height;
        public GroupBounds(int x, int y, int w, int h) {
            this.x = x; this.y = y; this.width = w; this.height = h;
        }
    }

    public static List<SlotUI> generateLayout(int sw, int sh, int slotSize, int spacing, Player player, GUIConfig config) {
        List<SlotUI> slots = new ArrayList<>();
        Inventory inv = player.getInventory();
        Map<String, GroupBounds> groupBoundsMap = new HashMap<>();
        
        // Pass 1: Calculate absolute positions and dimensions for all groups
        for (GUIConfig.GroupConfig groupCfg : config.groups) {
            // Check condition (same logic as before)
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

            if (targetSlots == null) continue;

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

            int baseX = sw / 2;
            int baseY = sh / 2;
            if (groupCfg.anchor.contains("bottom")) baseY = sh;
            else if (groupCfg.anchor.contains("top")) baseY = 0;
            if (groupCfg.anchor.contains("left")) baseX = 0;
            else if (groupCfg.anchor.contains("right")) baseX = sw;

            if (groupCfg.alignX.equals("center")) baseX -= groupWidth / 2;
            else if (groupCfg.alignX.equals("right")) baseX -= groupWidth;
            if (groupCfg.alignY.equals("center")) baseY -= groupHeight / 2;
            else if (groupCfg.alignY.equals("bottom")) baseY -= groupHeight;

            int startX = baseX + calculateCoord(groupCfg.x, sw) + groupCfg.fixedOffsetX;
            int startY = baseY + calculateCoord(groupCfg.y, sh) + groupCfg.fixedOffsetY;

            groupBoundsMap.put(groupCfg.id, new GroupBounds(startX, startY, groupWidth, groupHeight));
        }

        // Pass 2: Apply relative positioning and create SlotUI objects
        for (GUIConfig.GroupConfig groupCfg : config.groups) {
            GroupBounds bounds = groupBoundsMap.get(groupCfg.id);
            if (bounds == null) continue;

            int startX = bounds.x;
            int startY = bounds.y;

            if (groupCfg.relativeTo != null && groupBoundsMap.containsKey(groupCfg.relativeTo)) {
                GroupBounds rel = groupBoundsMap.get(groupCfg.relativeTo);
                
                // Handle Horizontal Relative Positioning
                if ("right".equals(groupCfg.relativeAlign)) {
                    startX = rel.x + rel.width + groupCfg.fixedOffsetX;
                } else if ("left".equals(groupCfg.relativeAlign)) {
                    startX = rel.x - bounds.width + groupCfg.fixedOffsetX;
                }
                
                // Handle Vertical Relative Positioning
                if ("bottom".equals(groupCfg.relativeAlign)) {
                    startY = rel.y + rel.height + groupCfg.fixedOffsetY;
                } else if ("top".equals(groupCfg.relativeAlign)) {
                    startY = rel.y - bounds.height + groupCfg.fixedOffsetY;
                }

                // Handle Secondary Alignment (e.g. center against parent)
                if ("center".equals(groupCfg.relativeAlignY)) {
                    startY = rel.y + (rel.height / 2 - bounds.height / 2) + groupCfg.fixedOffsetY;
                } else if ("top".equals(groupCfg.relativeAlignY)) {
                    startY = rel.y + groupCfg.fixedOffsetY;
                } else if ("bottom".equals(groupCfg.relativeAlignY)) {
                    startY = rel.y + rel.height - bounds.height + groupCfg.fixedOffsetY;
                }

                if ("center".equals(groupCfg.relativeAlignX)) {
                    startX = rel.x + (rel.width / 2 - bounds.width / 2) + groupCfg.fixedOffsetX;
                } else if ("left".equals(groupCfg.relativeAlignX)) {
                    startX = rel.x + groupCfg.fixedOffsetX;
                } else if ("right".equals(groupCfg.relativeAlignX)) {
                    startX = rel.x + rel.width - bounds.width + groupCfg.fixedOffsetX;
                }
            }

            SlotGroup group = inv.getGroup(groupCfg.id);
            List<Slot> targetSlots = (group != null) ? group.getSlots() : null;

            if (targetSlots == null) continue;

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
                
                Slot slot = targetSlots.get(i);
                SlotUI slotUI = new SlotUI(slot, x, y);
                
                // FIXED: Use loop index 'i' as Integer to match GUIConfig.GroupConfig.placeholders Map type
                if (groupCfg.placeholders != null) {
                    String placeholder = groupCfg.placeholders.get(i);
                    if (placeholder != null) {
                        slot.withPlaceholder(placeholder);
                        slotUI.withPlaceholder(placeholder);
                    }
                }
                
                slots.add(slotUI);
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
