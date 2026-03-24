package com.za.minecraft.engine.graphics.ui;

import java.util.List;

/**
 * Configuration for a GUI screen loaded from JSON.
 */
public class GUIConfig {
    public String id;
    public String title;
    public List<GroupConfig> groups;

    public static class GroupConfig {
        public String id;          // Group identifier (hotbar, pockets, etc)
        public String type = "grid"; // grid, vertical, horizontal
        public String anchor = "center"; // center, bottom, top, left, right
        public String alignX = "center"; // left, center, right
        public String alignY = "center"; // top, center, bottom
        public String x = "0";     // Offset from anchor (e.g. "10", "-50%", "20px")
        public String y = "0";
        public String relativeTo;   // ID of another group to position relative to
        public String relativeAlign; // left, right, top, bottom
        public String relativeAlignX; // left, center, right (secondary alignment)
        public String relativeAlignY; // top, center, bottom (secondary alignment)
        public int fixedOffsetX = 0; // Fixed pixel offset added after all calculations
        public int fixedOffsetY = 0;
        public int cols = 1;       // For grid type
        public int rows = 1;       // For grid type (can be auto if -1)
        public int spacing = 2;
        public java.util.Map<Integer, String> placeholders; // Map slot index in group to placeholder texture
        public String condition;   // Optional condition (e.g., "has_pouch", "developer_mode")
    }
}
