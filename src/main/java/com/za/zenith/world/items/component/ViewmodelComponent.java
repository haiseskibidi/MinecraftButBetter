package com.za.zenith.world.items.component;

import com.za.zenith.engine.graphics.model.GripDefinition;

/**
 * Allows customizing how an item is held in the hand.
 */
public record ViewmodelComponent(
    String socket,      // socket_palm_r, socket_wrist_l, etc.
    float[] translation, // [x, y, z] in voxels
    float[] rotation,    // [x, y, z] in degrees
    float scale,
    GripDefinition grip  // Настройки сгиба пальцев (может быть null)
) implements ItemComponent {
    
    public ViewmodelComponent() {
        this(null, new float[]{0, 0, 0}, new float[]{0, 0, 0}, 1.0f, null);
    }
}
