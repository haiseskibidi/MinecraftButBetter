package com.za.zenith.engine.graphics.ui.blueprints;

import org.joml.Vector4f;
import java.util.List;
import java.util.Map;

/**
 * Универсальный графический чертеж для процедурной отрисовки UI элементов.
 * Поддерживает многослойные ASCII-матрицы и анимацию на основе триггеров.
 */
public class GraphicBlueprint {
    private String identifier;
    private int size = 32; 
    private java.util.List<Element> elements;

    public static class Element {
        public String type; // "ring", "radial_lines", "matrix", "rect"
        public float radius = 0.5f;
        public float thickness = 0.05f;
        public int count = 12; // For radial lines
        public float innerRadius = 0.2f;
        public float outerRadius = 0.8f;
        
        // Matrix specific
        public java.util.List<String> matrix;
        
        public float[] color = {1.0f, 1.0f, 1.0f, 1.0f};
        public Animation animation;

        public org.joml.Vector4f getVectorColor() {
            if (color == null || color.length < 4) return new org.joml.Vector4f(1, 1, 1, 1);
            return new org.joml.Vector4f(color[0], color[1], color[2], color[3]);
        }
    }

    public static class Animation {
        public String type; // "expand", "pulse", "rotate", "shake"
        public int triggerSlot = 0;
        public float intensity = 1.0f;
    }

    public String getIdentifier() { return identifier; }
    public int getSize() { return size; }
    public java.util.List<Element> getElements() { return elements; }
}
