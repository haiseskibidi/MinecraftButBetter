package com.za.minecraft.engine.math;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Transform {
    private final Vector3f position;
    private final Vector3f rotation;
    private final Vector3f scale;
    private final Matrix4f transformMatrix;
    
    public Transform() {
        this.position = new Vector3f();
        this.rotation = new Vector3f();
        this.scale = new Vector3f(1.0f);
        this.transformMatrix = new Matrix4f();
    }
    
    public Transform(Vector3f position) {
        this();
        this.position.set(position);
    }
    
    public Matrix4f getTransformMatrix() {
        transformMatrix.identity()
            .translate(position)
            .rotateXYZ(rotation)
            .scale(scale);
        return transformMatrix;
    }
    
    public Vector3f getPosition() {
        return position;
    }
    
    public Vector3f getRotation() {
        return rotation;
    }
    
    public Vector3f getScale() {
        return scale;
    }
    
    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
    }
    
    public void setRotation(float x, float y, float z) {
        rotation.set(x, y, z);
    }
    
    public void setScale(float x, float y, float z) {
        scale.set(x, y, z);
    }
}
