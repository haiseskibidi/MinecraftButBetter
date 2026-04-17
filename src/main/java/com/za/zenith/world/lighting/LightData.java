package com.za.zenith.world.lighting;

import org.joml.Vector3f;

public class LightData {
    public enum Type {
        NONE, DIRECTIONAL, POINT, SPOT
    }

    public Type type = Type.POINT;
    public Vector3f color = new Vector3f(1.0f, 0.85f, 0.6f);
    public float intensity = 15.0f;
    public float radius = 10.0f;
    public float spotAngle = 35.0f;
    public boolean flicker = false;
    public boolean dynamic = true;
    
    // Additional property to help with parsing string types
    public static Type parseType(String typeStr) {
        if (typeStr == null) return Type.NONE;
        switch (typeStr.toLowerCase()) {
            case "directional": return Type.DIRECTIONAL;
            case "point": return Type.POINT;
            case "spot": return Type.SPOT;
            default: return Type.NONE;
        }
    }
}