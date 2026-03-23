package com.za.minecraft.world.items.component;

public class EquipmentComponent implements ItemComponent {
    private final String slotType;

    public EquipmentComponent(String slotType) {
        this.slotType = slotType;
    }

    public String getSlotType() {
        return slotType;
    }
}
