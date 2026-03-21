package com.za.minecraft.world.items.component;

import com.za.minecraft.world.items.ToolItem;

public record ToolComponent(ToolItem.ToolType type, float efficiency, int maxDurability, boolean isEffectiveAgainstAll) implements ItemComponent {
    public ToolComponent(ToolItem.ToolType type, float efficiency, int maxDurability) {
        this(type, efficiency, maxDurability, false);
    }
}
