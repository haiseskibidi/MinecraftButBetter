package com.za.minecraft.engine.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {
    private static final float DEFAULT_FOV = (float) Math.toRadians(80.0f);
    private static final float Z_NEAR = 0.01f;
    private static final float Z_FAR = 1000.0f;
    
    private final Vector3f position;
    private final Vector3f rotation;
    private final Matrix4f viewMatrix;
    private final Matrix4f projectionMatrix;
    
    private float fov;
    private float aspectRatio;
    
    // Offset for bobbing/animations
    private float xOffset = 0, yOffset = 0, zOffset = 0;
    private float pitchOffset = 0;
    private float rollOffset = 0;
    private float fovOffset = 0;
    
    // No caching to avoid subtle jitter; compute per frame
    
    public Camera() {
        this.position = new Vector3f();
        this.rotation = new Vector3f();
        this.viewMatrix = new Matrix4f();
        this.projectionMatrix = new Matrix4f();
        this.fov = DEFAULT_FOV;
    }
    
    public Camera(Vector3f position) {
        this();
        this.position.set(position);
    }
    
    public Matrix4f getViewMatrix() {
        return viewMatrix.identity()
            .rotateX(-(rotation.x + pitchOffset))
            .rotateY(-rotation.y)
            .rotateZ(-(rotation.z + rollOffset))
            .translate(-position.x - xOffset, -position.y - yOffset, -position.z - zOffset);
    }

    public void setOffsets(float x, float y, float z) {
        this.xOffset = x;
        this.yOffset = y;
        this.zOffset = z;
    }

    public void setPitchOffset(float offset) {
        this.pitchOffset = offset;
    }

    public void setRollOffset(float offset) {
        this.rollOffset = offset;
    }

    public void setFovOffset(float offset) {
        this.fovOffset = offset;
    }
    
    public Matrix4f getProjectionMatrix() {
        return projectionMatrix.setPerspective(fov + fovOffset, aspectRatio, Z_NEAR, Z_FAR);
    }
    
    public void updateAspectRatio(float aspectRatio) {
        this.aspectRatio = aspectRatio;
    }

    public float getAspectRatio() {
        return aspectRatio;
    }
    
    public void movePosition(float offsetX, float offsetY, float offsetZ) {
        if (offsetZ != 0) {
            position.x += (float) Math.sin(rotation.y) * -1.0f * offsetZ;
            position.z += (float) Math.cos(rotation.y) * offsetZ;
        }
        if (offsetX != 0) {
            position.x += (float) Math.sin(rotation.y - Math.PI / 2f) * -1.0f * offsetX;
            position.z += (float) Math.cos(rotation.y - Math.PI / 2f) * offsetX;
        }
        position.y += offsetY;
    }
    
    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
    }
    
    public void moveRotation(float offsetX, float offsetY, float offsetZ) {
        rotation.add(offsetX, offsetY, offsetZ);
        
        // Clamp pitch to prevent gimbal lock
        if (rotation.x > (float) Math.toRadians(89.0)) rotation.x = (float) Math.toRadians(89.0);
        if (rotation.x < (float) Math.toRadians(-89.0)) rotation.x = (float) Math.toRadians(-89.0);
        
        // Normalize yaw to prevent precision issues
        while (rotation.y > (float) Math.PI) rotation.y -= (float) (2 * Math.PI);
        while (rotation.y < (float) -Math.PI) rotation.y += (float) (2 * Math.PI);
    }
    
    public void setRotation(float x, float y, float z) {
        rotation.set(x, y, z);
    }
    
    public Vector3f getPosition() {
        return position;
    }
    
    public Vector3f getRotation() {
        return rotation;
    }
    
    public float getFov() {
        return fov;
    }
    
    public void setFov(float fov) {
        this.fov = fov;
    }
}
