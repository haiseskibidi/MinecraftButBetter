package com.za.minecraft.engine.graphics.ui.crosshair;

import org.joml.Vector4f;
import java.util.List;

/**
 * Data-driven definition of a crosshair shape.
 * Matrix-based approach allows for flexible pixel-art shapes.
 */
public class CrosshairDefinition {
    private String identifier;
    private int size;
    private float[] color; // Loaded as array [r, g, b, a]
    private List<String> matrix;
    private float scale = 1.0f;
    private boolean centered = true;

    public CrosshairDefinition() {}

    public String getIdentifier() { return identifier; }
    public int getSize() { return size; }
    public Vector4f getColor() { 
        if (color == null || color.length < 4) return new Vector4f(1, 1, 1, 1);
        return new Vector4f(color[0], color[1], color[2], color[3]); 
    }
    public List<String> getMatrix() { return matrix; }
    public float getScale() { return scale; }
    public boolean isCentered() { return centered; }
}
