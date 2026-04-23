package com.za.zenith.world.items.component;

import com.za.zenith.world.items.ToolType;

public class ToolComponent implements ItemComponent {
    public ToolType type;
    public float efficiency;
    public int maxDurability;
    public boolean isEffectiveAgainstAll;
    public float attackInterval;

    public ToolComponent(ToolType type, float efficiency, int maxDurability) {
        this(type, efficiency, maxDurability, false, 0.5f);
    }

    public ToolComponent(ToolType type, float efficiency, int maxDurability, boolean isEffectiveAgainstAll) {
        this(type, efficiency, maxDurability, isEffectiveAgainstAll, 0.5f);
    }

    public ToolComponent(ToolType type, float efficiency, int maxDurability, boolean isEffectiveAgainstAll, float attackInterval) {
        this.type = type;
        this.efficiency = efficiency;
        this.maxDurability = maxDurability;
        this.isEffectiveAgainstAll = isEffectiveAgainstAll;
        this.attackInterval = attackInterval;
    }

    // Accessors to mimic record behavior for compatibility
    public ToolType type() { return type; }
    public float efficiency() { return efficiency; }
    public int maxDurability() { return maxDurability; }
    public boolean isEffectiveAgainstAll() { return isEffectiveAgainstAll; }
    public float attackInterval() { return attackInterval; }
}
