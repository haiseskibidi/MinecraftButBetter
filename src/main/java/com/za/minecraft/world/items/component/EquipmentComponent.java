package com.za.minecraft.world.items.component;

public class EquipmentComponent implements ItemComponent {
    private final String slotType;
    private final boolean strict;

    public EquipmentComponent(String slotType) {
        this(slotType, false);
    }

    public EquipmentComponent(String slotType, boolean strict) {
        this.slotType = slotType;
        this.strict = strict;
    }

    public String getSlotType() {
        return slotType;
    }

    public boolean isStrict() {
        return strict;
    }
}
