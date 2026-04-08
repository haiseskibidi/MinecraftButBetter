package com.za.zenith.world.items.component;

public class BagComponent implements ItemComponent {
    private final int slots;
    private final boolean dropOnUnequip;

    public BagComponent(int slots) {
        this(slots, false);
    }

    public BagComponent(int slots, boolean dropOnUnequip) {
        this.slots = slots;
        this.dropOnUnequip = dropOnUnequip;
    }

    public int getSlots() {
        return slots;
    }

    public boolean isDropOnUnequip() {
        return dropOnUnequip;
    }
}
