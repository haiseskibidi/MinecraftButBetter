package com.za.zenith.engine.graphics.ui.crosshair;

import org.joml.Vector4f;
import java.util.List;

/**
 * Data-driven definition of a crosshair shape.
 * Matrix-based approach allows for flexible pixel-art shapes.
 * Supported animation parameters: bounce, recoil (pulse), and spread (progress-based).
 */
public class CrosshairDefinition {
    private String identifier;
    private int size;
    private float[] color; // Loaded as array [r, g, b, a]
    private List<String> matrix;
    private float scale = 1.0f;
    private boolean centered = true;
    
    // Animation settings
    private float bounceScale = 0.0f; // Extra scale on state entry
    private float bounceDuration = 0.3f; // Seconds
    
    private float recoilScale = 1.0f; // Power of the hit pulse (expansion on click)
    private float spreadScale = 0.0f; // How much corners move apart based on mining progress

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
    public float getBounceScale() { return bounceScale; }
    public float getBounceDuration() { return bounceDuration; }
    
    public float getRecoilScale() { return recoilScale; }
    public float getSpreadScale() { return spreadScale; }
}


