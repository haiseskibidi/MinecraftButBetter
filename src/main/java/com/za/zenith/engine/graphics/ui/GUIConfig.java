package com.za.zenith.engine.graphics.ui;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Configuration for a GUI screen loaded from JSON.
 */
public class GUIConfig {
    @SerializedName(value = "identifier", alternate = {"id"})
    public String identifier;
    public String title;
    public boolean hudVisible = false; // If true, this GUI can be rendered as part of the HUD
    public List<GroupConfig> groups;
    public List<ButtonConfig> buttons;
    public java.util.Map<String, HUDElementConfig> hudElements;
    public SelectionStyle selection = new SelectionStyle();
    public BackgroundConfig background = new BackgroundConfig();

    public static class ButtonConfig {
        public String id;
        public String text;
        public String action;
        public float[] color = {1.0f, 1.0f, 1.0f, 1.0f};
        public int x;
        public int y;
        public int width = 200;
        public int height = 40;
    }

    public static class HUDElementConfig {
        public String type = "text"; // text, bar
        public String texture; // Для типа image
        public String text; // Для текстовых элементов с поддержкой переменных
        public String condition; // Условие видимости (напр. "debug_mode")
        public String anchor = "center"; // top, bottom, left, right, center
        public String alignX = "center";
        public String alignY = "center";
        public Object x = 0;
        public Object y = 0;
        public int fontSize = 18;
        public int minFontSize = 12;
        public int maxWidth = 300;
        public int width = 100;
        public int height = 10;
        public int segments = 0; // 0 for continuous
        public int barsCount = 10; // For wave type
        public String blueprint; // For blueprint type
        public float[] color = {1.0f, 1.0f, 1.0f, 1.0f};
        public float[] backgroundColor = {0.0f, 0.0f, 0.0f, 0.5f};
        public boolean visible = true;
    }

    public static class SelectionStyle {
        public String type = "brackets"; // border, brackets, glow, none
        public float[] color = {1.0f, 1.0f, 1.0f, 0.8f};
        public int padding = 2;
        public float thickness = 2.0f; // In pixels
        public boolean pulse = true;   // Subtle glow animation
    }

    public static class GroupConfig {
        public String id;          // Group identifier (hotbar, pockets, etc)
        public String type = "grid"; // grid, vertical, horizontal
        public String anchor = "center"; // center, bottom, top, left, right
        public String alignX = "center"; // left, center, right
        public String alignY = "center"; // top, center, bottom
        public Object x = "0";     // Offset from anchor (e.g. 10, "-50%", "2s")
        public Object y = "0";
        public String relativeTo;   // ID of another group to position relative to
        public String relativeAlign; // left, right, top, bottom
        public String relativeAlignX; // left, center, right (secondary alignment)
        public String relativeAlignY; // top, center, bottom (secondary alignment)
        public Object fixedOffsetX = 0; // Offset in pixels (int) or slots (String "1s")
        public Object fixedOffsetY = 0;
        public boolean snapToGrid = false; // If true, aligns slots to the grid of the relativeTo group
        public boolean centerCombined = false; // If true, includes children in width/height calculation for alignment
        public int cols = 1;       // For grid type
        public int rows = 1;       // For grid type (can be auto if -1)
        public int spacing = 2;
        public String inventorySource = "player"; // "player" or "container"
        public int startIndex = 0;
        public int slotsCount = -1; // -1 means use all available slots starting from startIndex
        public java.util.List<String> quickMoveTo; // Target group IDs for Shift+Click
        public java.util.Map<Integer, String> placeholders; // Map slot index in group to placeholder texture
        public String condition;   // Optional condition (e.g., "has_pouch", "developer_mode")
        
        // Custom dimensions for special group types
        public int width = 0;
        public int height = 0;
        public int padding = 0;
        public int textSize = 14;
        public BackgroundConfig background;
    }

    public static class BackgroundConfig {
        public String type = "none"; // none, solid
        public float[] color = {0.0f, 0.0f, 0.0f, 0.5f};
        public int padding = 6;
        public int borderRadius = 4;
        public List<String> includeGroups; // List of group IDs to encompass with this background
    }
}
