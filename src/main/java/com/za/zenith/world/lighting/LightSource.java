package com.za.zenith.world.lighting;

import org.joml.Vector3f;

public class LightSource {
    public LightData data;
    public Vector3f position = new Vector3f();
    public Vector3f direction = new Vector3f(0, -1, 0); // For directional and spot
    
    public LightSource(LightData data) {
        this.data = data;
    }
    
    public LightSource(LightData data, Vector3f position) {
        this.data = data;
        this.position.set(position);
    }
}
