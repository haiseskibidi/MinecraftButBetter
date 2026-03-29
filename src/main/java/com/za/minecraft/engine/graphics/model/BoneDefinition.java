package com.za.minecraft.engine.graphics.model;

import java.util.List;

public class BoneDefinition {
    public String name;
    public String parent;
    public float[] pivot; // [x, y, z] in absolute space
    public float[] rotation; // [x, y, z] in degrees
    public List<CubeDefinition> cubes;

    public static class CubeDefinition {
        public float[] origin; // [x, y, z]
        public float[] size; // [x, y, z]
        public int[] uv; // [u, v]
    }
}
