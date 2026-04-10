package com.za.zenith.world.items;

import com.za.zenith.engine.graphics.ColorProvider;
import org.joml.Vector3f;

public enum Rarity {
    COMMON(new Vector3f(1.0f, 1.0f, 1.0f)),      // White
    UNCOMMON(new Vector3f(0.1f, 0.8f, 0.1f)),    // Green
    RARE(new Vector3f(0.2f, 0.4f, 1.0f)),        // Blue
    EPIC(new Vector3f(0.7f, 0.2f, 1.0f)),        // Purple
    LEGENDARY(new Vector3f(1.0f, 0.6f, 0.0f));   // Orange

    private final Vector3f color;

    Rarity(Vector3f color) {
        this.color = color;
    }

    public Vector3f getColor() {
        return color;
    }
}
