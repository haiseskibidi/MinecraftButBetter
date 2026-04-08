package com.za.zenith.engine.graphics.ui;

import com.za.zenith.entities.inventory.Slot;

/**
 * Represents a logical slot at a specific screen position.
 */
public class SlotUI {
    private final Slot slot;
    private final int x, y;
    private final String groupId;
    private String placeholderTexture;

    public SlotUI(Slot slot, int x, int y, String groupId) {
        this.slot = slot;
        this.x = x;
        this.y = y;
        this.groupId = groupId;
    }

    public String getGroupId() {
        return groupId;
    }

    public SlotUI withPlaceholder(String texture) {
        this.placeholderTexture = texture;
        return this;
    }

    public String getPlaceholderTexture() {
        return placeholderTexture;
    }

    public Slot getSlot() {
        return slot;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public boolean isMouseOver(float mx, float my, int slotSize) {
        return mx >= x && mx <= x + slotSize && my >= y && my <= y + slotSize;
    }
}


