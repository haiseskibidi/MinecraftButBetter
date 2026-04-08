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

    public static LayoutResult generateLayout(int sw, int sh, int slotSize, int spacing, Player player, GUIConfig config, Map<String, IInventory> inventories) {
        LayoutResult result = new LayoutResult();
        Map<String, GroupBounds> basicBoundsMap = new HashMap<>();
        Map<String, GroupBounds> finalBoundsMap = new HashMap<>();
        
        // Cache for resolved slots per group to avoid re-calculation
        Map<String, List<Slot>> resolvedSlots = new HashMap<>();

        // Pass 1: Resolve slots and calculate basic dimensions
        for (GUIConfig.GroupConfig groupCfg : config.groups) {
            if (groupCfg.condition != null) {
                Inventory pinv = player.getInventory();
                if (groupCfg.condition.equals("has_pouch")) {
                    ItemStack acc = pinv.getStack(Inventory.SLOT_ACCESSORY);
                    if (acc == null || !acc.getItem().hasComponent(com.za.minecraft.world.items.component.BagComponent.class)) continue;
                } else if (groupCfg.condition.equals("developer_mode")) {
                    if (player.getMode() != PlayerMode.DEVELOPER) continue;
                }
            }

            IInventory sourceInv = inventories.getOrDefault(groupCfg.inventorySource, player.getInventory());
            List<Slot> targetSlots = new ArrayList<>();

            // If it's a named group from Player Inventory (legacy compatibility + power)
            if (groupCfg.inventorySource.equals("player") && sourceInv instanceof Inventory pInv) {
                SlotGroup group = pInv.getGroup(groupCfg.id);
                if (group != null) targetSlots.addAll(group.getSlots());
            }

            // Fallback: use index range from the source inventory
            // Only do this if slotsCount is explicitly set, OR if it's a container (where we want to see everything by default)
            if (targetSlots.isEmpty() && (groupCfg.slotsCount != -1 || !groupCfg.inventorySource.equals("player"))) {
                int start = groupCfg.startIndex;
                int count = groupCfg.slotsCount;
                if (count == -1) count = sourceInv.size() - start;
                
                for (int i = 0; i < count && (start + i) < sourceInv.size(); i++) {
                    targetSlots.add(new Slot(sourceInv, start + i, "any"));
                }
            }

            if (targetSlots.isEmpty()) continue;
            resolvedSlots.put(groupCfg.id, targetSlots);

            int groupWidth = 0, groupHeight = 0;
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
            basicBoundsMap.put(groupCfg.id, new GroupBounds(0, 0, groupWidth, groupHeight));
        }

        // Pass 2 & 3: Positioning (Same logic as before)
        Map<String, GroupBounds> combinedBoundsMap = new HashMap<>();
        for (GUIConfig.GroupConfig groupCfg : config.groups) {
            if (!basicBoundsMap.containsKey(groupCfg.id)) continue;
            if (groupCfg.centerCombined) {
                combinedBoundsMap.put(groupCfg.id, calculateCombinedBounds(groupCfg.id, config, basicBoundsMap, slotSize, spacing));
            } else {
                combinedBoundsMap.put(groupCfg.id, basicBoundsMap.get(groupCfg.id));
            }
        }

        java.util.Set<String> resolving = new java.util.HashSet<>();
        for (GUIConfig.GroupConfig groupCfg : config.groups) {
            resolveGroupBounds(groupCfg.id, config, basicBoundsMap, combinedBoundsMap, finalBoundsMap, resolving, sw, sh, slotSize, spacing);
        }

        // Final Pass: Create objects
        for (GUIConfig.GroupConfig groupCfg : config.groups) {
            GroupBounds bounds = finalBoundsMap.get(groupCfg.id);
            if (bounds == null) continue;

            result.groups.add(new GroupUI(bounds.x, bounds.y, bounds.width, bounds.height, groupCfg));
            
            List<Slot> targetSlots = resolvedSlots.get(groupCfg.id);
            for (int i = 0; i < targetSlots.size(); i++) {
                int col = 0, row = 0;
                if (groupCfg.type.equals("grid")) { col = i % groupCfg.cols; row = i / groupCfg.cols; }
                else if (groupCfg.type.equals("vertical")) row = i; else if (groupCfg.type.equals("horizontal")) col = i;
                
                Slot slot = targetSlots.get(i);
                SlotUI slotUI = new SlotUI(slot, bounds.x + col * (slotSize + groupCfg.spacing), bounds.y + row * (slotSize + groupCfg.spacing));
                if (groupCfg.placeholders != null) {
                    String placeholder = groupCfg.placeholders.get(i);
                    if (placeholder != null) { slot.withPlaceholder(placeholder); slotUI.withPlaceholder(placeholder); }
                }
                result.slots.add(slotUI);
            }
        }

        // Global Background
        if (config.background != null && "solid".equals(config.background.type) && config.background.includeGroups != null) {
            int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE, maxY = Integer.MIN_VALUE;
            boolean found = false;
            for (GroupUI gui : result.groups) if (config.background.includeGroups.contains(gui.getConfig().id)) {
                minX = Math.min(minX, gui.getX()); minY = Math.min(minY, gui.getY());
                maxX = Math.max(maxX, gui.getX() + gui.getWidth()); maxY = Math.max(maxY, gui.getY() + gui.getHeight());
                found = true;
            }
            if (found) result.globalBackground = new GroupUI(minX, minY, maxX - minX, maxY - minY, null);
        }
        return result;
    }

    private static GroupBounds resolveGroupBounds(String id, GUIConfig config, Map<String, GroupBounds> basic, Map<String, GroupBounds> combined, Map<String, GroupBounds> finalized, java.util.Set<String> resolving, int sw, int sh, int slotSize, int spacing) {
        if (finalized.containsKey(id)) return finalized.get(id);
        if (resolving.contains(id)) return null; // Circular dependency
        resolving.add(id);

        GUIConfig.GroupConfig groupCfg = null;
        for (GUIConfig.GroupConfig g : config.groups) if (g.id.equals(id)) { groupCfg = g; break; }
        if (groupCfg == null || !basic.containsKey(id)) { resolving.remove(id); return null; }

        GroupBounds baseBounds = basic.get(id);
        GroupBounds comb = combined.get(id);
        int groupWidth = groupCfg.centerCombined ? comb.width : baseBounds.width;
        int groupHeight = groupCfg.centerCombined ? comb.height : baseBounds.height;

        int startX, startY;
        if (groupCfg.relativeTo != null) {
            GroupBounds rel = resolveGroupBounds(groupCfg.relativeTo, config, basic, combined, finalized, resolving, sw, sh, slotSize, spacing);
            if (rel == null) { // Fallback if parent missing
                startX = sw/2; startY = sh/2;
            } else {
                GUIConfig.GroupConfig relCfg = null;
                for (GUIConfig.GroupConfig g : config.groups) if (g.id.equals(groupCfg.relativeTo)) { relCfg = g; break; }
                int gridSpacing = (relCfg != null) ? relCfg.spacing : spacing;
                int effectiveFixedX = calculateCoord(groupCfg.fixedOffsetX, sw, slotSize, gridSpacing);
                int effectiveFixedY = calculateCoord(groupCfg.fixedOffsetY, sh, slotSize, gridSpacing);

                if (groupCfg.snapToGrid) {
                    if ("right".equals(groupCfg.relativeAlign)) effectiveFixedX = gridSpacing;
                    else if ("left".equals(groupCfg.relativeAlign)) effectiveFixedX = -gridSpacing;
                    if ("bottom".equals(groupCfg.relativeAlign)) effectiveFixedY = gridSpacing;
                    else if ("top".equals(groupCfg.relativeAlign)) effectiveFixedY = -gridSpacing;
                }
                
                startX = rel.x; startY = rel.y;
                if ("right".equals(groupCfg.relativeAlign)) startX = rel.x + rel.width + effectiveFixedX;
                else if ("left".equals(groupCfg.relativeAlign)) startX = rel.x - baseBounds.width + effectiveFixedX;
                else if ("bottom".equals(groupCfg.relativeAlign)) startY = rel.y + rel.height + effectiveFixedY;
                else if ("top".equals(groupCfg.relativeAlign)) startY = rel.y - baseBounds.height + effectiveFixedY;

                if ("center".equals(groupCfg.relativeAlignY)) startY = rel.y + (rel.height / 2 - groupHeight / 2) + effectiveFixedY;
                else if ("top".equals(groupCfg.relativeAlignY)) startY = rel.y + effectiveFixedY;
                else if ("bottom".equals(groupCfg.relativeAlignY)) startY = rel.y + rel.height - groupHeight + effectiveFixedY;

                if ("center".equals(groupCfg.relativeAlignX)) startX = rel.x + (rel.width / 2 - groupWidth / 2) + effectiveFixedX;
                else if ("left".equals(groupCfg.relativeAlignX)) startX = rel.x + effectiveFixedX;
                else if ("right".equals(groupCfg.relativeAlignX)) startX = rel.x + rel.width - groupWidth + effectiveFixedX;
            }
        } else {
            int baseX = sw / 2, baseY = sh / 2;
            if (groupCfg.anchor.contains("bottom")) baseY = sh; else if (groupCfg.anchor.contains("top")) baseY = 0;
            if (groupCfg.anchor.contains("left")) baseX = 0; else if (groupCfg.anchor.contains("right")) baseX = sw;
            if (groupCfg.alignX.equals("center")) baseX -= groupWidth / 2; else if (groupCfg.alignX.equals("right")) baseX -= groupWidth;
            if (groupCfg.alignY.equals("center")) baseY -= groupHeight / 2; else if (groupCfg.alignY.equals("bottom")) baseY -= groupHeight;
            startX = baseX + calculateCoord(groupCfg.x, sw, slotSize, groupCfg.spacing) + calculateCoord(groupCfg.fixedOffsetX, sw, slotSize, groupCfg.spacing);
            startY = baseY + calculateCoord(groupCfg.y, sh, slotSize, groupCfg.spacing) + calculateCoord(groupCfg.fixedOffsetY, sh, slotSize, groupCfg.spacing);
        }

        if (groupCfg.centerCombined) {
            startX -= comb.x; startY -= comb.y;
        }

        GroupBounds result = new GroupBounds(startX, startY, baseBounds.width, baseBounds.height);
        finalized.put(id, result);
        resolving.remove(id);
        return result;
    }

    private static GroupBounds calculateCombinedBounds(String rootId, GUIConfig config, Map<String, GroupBounds> basicBounds, int slotSize, int spacing) {
        int minX = 0, minY = 0, maxX = basicBounds.get(rootId).width, maxY = basicBounds.get(rootId).height;
        
        // Simple recursive search for children (only 1 level for now as it's enough for current UI)
        for (GUIConfig.GroupConfig cfg : config.groups) {
            if (rootId.equals(cfg.relativeTo) && basicBounds.containsKey(cfg.id)) {
                GroupBounds b = basicBounds.get(cfg.id);
                int ox = 0, oy = 0;
                int gridSpacing = cfg.spacing;
                
                if ("left".equals(cfg.relativeAlign)) ox = -b.width - gridSpacing;
                else if ("right".equals(cfg.relativeAlign)) ox = maxX + gridSpacing;
                else if ("top".equals(cfg.relativeAlign)) oy = -b.height - gridSpacing;
                else if ("bottom".equals(cfg.relativeAlign)) oy = maxY + gridSpacing;
                
                minX = Math.min(minX, ox); minY = Math.min(minY, oy);
                maxX = Math.max(maxX, ox + b.width); maxY = Math.max(maxY, oy + b.height);
            }
        }
        return new GroupBounds(minX, minY, maxX - minX, maxY - minY);
    }

    private static int calculateCoord(Object valueObj, int total) {
        return calculateCoord(valueObj, total, 0, 0);
    }

    private static int calculateCoord(Object valueObj, int total, int slotSize, int spacing) {
        if (valueObj == null) return 0;
        
        // Support direct numeric values from JSON (old way)
        if (valueObj instanceof Number num) {
            return num.intValue();
        }
        
        String value = valueObj.toString();
        if (value.isEmpty()) return 0;
        
        try {
            if (value.endsWith("%")) {
                float percent = Float.parseFloat(value.substring(0, value.length() - 1)) / 100.0f;
                return (int)(total * percent);
            }
            if (value.endsWith("s")) {
                float slots = Float.parseFloat(value.substring(0, value.length() - 1));
                return (int)(slots * (slotSize + spacing));
            }
            if (value.endsWith("px")) {
                return Integer.parseInt(value.substring(0, value.length() - 2));
            }
            // Fallback: try to parse as regular integer string
            return Integer.parseInt(value);
        } catch (Exception e) {
            return 0;
        }
    }
}
