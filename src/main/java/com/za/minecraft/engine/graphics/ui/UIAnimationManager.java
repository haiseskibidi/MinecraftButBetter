package com.za.minecraft.engine.graphics.ui;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages smooth animation states for UI elements.
 * Tracks hover progress and other dynamic properties.
 */
public class UIAnimationManager {
    private static final Map<String, Float> hoverStates = new HashMap<>();
    private static final float SPEED = 8.0f; // Speed of LERP

    /**
     * Updates and returns the hover progress (0.0 to 1.0) for a specific element.
     * @param id Unique ID of the element (e.g., "slot_hotbar_0")
     * @param isHovered Current hover state
     * @param delta Delta time
     * @return Smooth hover progress
     */
    public static float getHoverProgress(String id, boolean isHovered, float delta) {
        float current = hoverStates.getOrDefault(id, 0.0f);
        float target = isHovered ? 1.0f : 0.0f;
        
        if (Math.abs(current - target) < 0.001f) {
            current = target;
        } else {
            current += (target - current) * Math.min(delta * SPEED, 1.0f);
        }
        
        hoverStates.put(id, current);
        return current;
    }
    
    public static void clear() {
        hoverStates.clear();
    }
}
