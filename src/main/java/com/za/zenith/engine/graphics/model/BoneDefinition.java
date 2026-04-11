package com.za.zenith.engine.graphics.model;

import java.util.List;

/**
 * Definition of a bone in the viewmodel hierarchy.
 * Coordinates are in "voxels" (1/16 of a block).
 * Pivot is relative to the parent bone.
 */
public class BoneDefinition {
    public String name;
    public String parent;
    public float[] pivot; // [x, y, z] relative to parent
    public float[] rotation; // [x, y, z] degrees
    public List<CubeDefinition> cubes;

    public static class CubeDefinition {
        public float[] origin; // [x, y, z] relative to this bone's pivot
        public float[] size; // [x, y, z]
        public int[] uv; // [u, v]
    }
}
