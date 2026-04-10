package com.za.zenith.world.items.stats;

import com.za.zenith.utils.Identifier;

/**
 * Data-driven definition for a RPG statistic.
 */
public record StatDefinition(
    Identifier identifier,
    String translationKey,
    float defaultValue,
    float minValue,
    float maxValue,
    DisplayType displayType,
    Category category
) {
    public enum Category {
        GLOBAL,     // Core body/environmental stats (Mobility, Constitution, etc.)
        COMBAT      // Tactical/Weapon specific stats (Shock, Penetration, etc.)
    }

    public enum DisplayType {
        NUMBER,     // Standard number (e.g. 10)
        PERCENT,    // Percentage (e.g. 15%)
        TIME        // Time in seconds (e.g. 1.2s)
    }
}
