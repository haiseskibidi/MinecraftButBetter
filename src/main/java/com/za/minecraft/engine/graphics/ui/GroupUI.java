package com.za.minecraft.engine.graphics.ui;

public class GroupUI {
    private int x, y, width, height;
    private GUIConfig.GroupConfig config;

    public GroupUI(int x, int y, int width, int height, GUIConfig.GroupConfig config) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        this.config = config;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
    public GUIConfig.GroupConfig getConfig() { return config; }
}
