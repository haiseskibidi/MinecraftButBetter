package com.za.zenith.engine.graphics.model;

import java.util.List;

public class BoneDefinition {
    public String name;
    public String parent;
    
    // Пивот сустава в вокселях (1/16 блока)
    // X: right(+)/left(-), Y: up(+)/down(-), Z: forward(+)/backward(-)
    public float x = 0, y = 0, z = 0;
    
    public float[] rotation; // [x, y, z] degrees
    public List<CubeDefinition> cubes;

    public static class CubeDefinition {
        // Начало куба ОТНОСИТЕЛЬНО пивота кости
        public float x = 0, y = 0, z = 0;
        public float[] size; // [width, height, length]
        public int[] uv;
    }
}
