package com.za.zenith.world.items.component;

import com.za.zenith.world.items.ToolType;

public record ToolComponent(ToolType type, float efficiency, int maxDurability, boolean isEffectiveAgainstAll, float attackInterval) implements ItemComponent {
    public ToolComponent(ToolType type, float efficiency, int maxDurability) {
        this(type, efficiency, maxDurability, false, 0.5f);
    }

    // Compatibility constructor for DataLoader
    public ToolComponent(ToolType type, float efficiency, int maxDurability, boolean isEffectiveAgainstAll) {
        this(type, efficiency, maxDurability, isEffectiveAgainstAll, 0.5f);
    }}


