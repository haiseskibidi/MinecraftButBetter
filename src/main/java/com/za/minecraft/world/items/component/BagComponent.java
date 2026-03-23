package com.za.minecraft.world.items.component;

public class BagComponent implements ItemComponent {
    private final int slots;

    public BagComponent(int slots) {
        this.slots = slots;
    }

    public int getSlots() {
        return slots;
    }
}
