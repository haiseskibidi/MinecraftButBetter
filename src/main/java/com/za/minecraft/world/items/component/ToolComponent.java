package com.za.minecraft.world.items.component;

import com.za.minecraft.world.items.ToolType;

public record ToolComponent(ToolType type, float efficiency, int maxDurability, boolean isEffectiveAgainstAll) implements ItemComponent {
    public ToolComponent(ToolType type, float efficiency, int maxDurability) {
        this(type, efficiency, maxDurability, false);
    }
}
