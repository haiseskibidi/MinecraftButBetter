package com.za.minecraft.world.items.component;

import java.util.Map;

/**
 * Component that holds animation overrides for an item.
 * Maps base animation keys (e.g., "item_walk") to custom profile names (e.g., "axe_walk").
 */
public record AnimationComponent(Map<String, String> overrides) implements ItemComponent {
    public String getOverride(String baseKey) {
        return overrides.get(baseKey);
    }
}
