package com.za.minecraft.entities.inventory;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * A group of slots (e.g., Hotbar, Armor).
 * Can be dynamically activated/deactivated.
 */
public class SlotGroup {
    private final String id;
    private final List<Slot> slots;
    private Supplier<Boolean> activeSupplier;

    public SlotGroup(String id) {
        this.id = id;
        this.slots = new ArrayList<>();
        this.activeSupplier = () -> true; // Default: always active
    }

    public void addSlot(Slot slot) {
        slots.add(slot);
    }

    public SlotGroup withActiveSupplier(Supplier<Boolean> supplier) {
        this.activeSupplier = supplier;
        return this;
    }

    public String getId() {
        return id;
    }

    public List<Slot> getSlots() {
        return slots;
    }

    public boolean isActive() {
        return activeSupplier.get();
    }
    
    public int size() {
        return slots.size();
    }
}
