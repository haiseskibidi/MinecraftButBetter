package com.za.minecraft.world.items.component;

/**
 * Component for thermal properties of an item.
 */
public record ThermalComponent(
    float initialTemperature,
    float specificHeatCapacity,
    float burnThreshold
) implements ItemComponent {
    public static final float DEFAULT_BURN_THRESHOLD = 55.0f;
    public static final float DEFAULT_SPECIFIC_HEAT = 1.0f;
}
