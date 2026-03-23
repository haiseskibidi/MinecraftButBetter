package com.za.minecraft.engine.graphics.ui;

import com.za.minecraft.entities.inventory.Slot;

/**
 * Represents a logical slot at a specific screen position.
 */
public class SlotUI {
    private final Slot slot;
    private final int x, y;

    public SlotUI(Slot slot, int x, int y) {
        this.slot = slot;
        this.x = x;
        this.y = y;
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
