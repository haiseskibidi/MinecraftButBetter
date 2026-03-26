package com.za.minecraft.engine.graphics;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Camera {
    private static final float DEFAULT_FOV = (float) Math.toRadians(80.0f);
    private static final float Z_NEAR = 0.01f;
    private static final float Z_FAR = 1000.0f;
    
    private final Vector3f position = new Vector3f();
    private final Vector3f prevPosition = new Vector3f(); // For interpolation
    private final Vector3f rotation = new Vector3f();
    private final Matrix4f viewMatrix = new Matrix4f();
    private final Matrix4f projectionMatrix = new Matrix4f();
    
    private float fov;
    private float aspectRatio;
    
    // Offset for bobbing/animations
    private float xOffset = 0, yOffset = 0, zOffset = 0;
    private float pitchOffset = 0;
    private float rollOffset = 0;
    private float fovOffset = 0;
    
    public Camera() {
        this.fov = DEFAULT_FOV;
    }
    
    public Camera(Vector3f position) {
        this();
        this.position.set(position);
        this.prevPosition.set(position);
    }
    
    /**
     * @param alpha [0..1] interpolation factor between physics ticks
     */
    public Matrix4f getViewMatrix(float alpha) {
        float interpX = prevPosition.x + (position.x - prevPosition.x) * alpha;
        float interpY = prevPosition.y + (position.y - prevPosition.y) * alpha;
        float interpZ = prevPosition.z + (position.z - prevPosition.z) * alpha;

        return viewMatrix.identity()
            .rotateX(-(rotation.x + pitchOffset))
            .rotateY(-rotation.y)
            .rotateZ(-(rotation.z + rollOffset))
            .translate(-interpX - xOffset, -interpY - yOffset, -interpZ - zOffset);
    }

    public void setOffsets(float x, float y, float z) {
        this.xOffset = x;
        this.yOffset = y;
        this.zOffset = z;
    }

    public void setPitchOffset(float offset) { this.pitchOffset = offset; }
    public void setRollOffset(float offset) { this.rollOffset = offset; }
    public void setFovOffset(float offset) { this.fovOffset = offset; }
    
    public Matrix4f getProjectionMatrix() {
        return projectionMatrix.setPerspective(fov + fovOffset, aspectRatio, Z_NEAR, Z_FAR);
    }
    
    public void updateAspectRatio(float aspectRatio) { this.aspectRatio = aspectRatio; }
    public float getAspectRatio() { return aspectRatio; }
    
    public void setPosition(float x, float y, float z) {
        prevPosition.set(position); // Store old before updating to new
        position.set(x, y, z);
    }
    
    public void moveRotation(float offsetX, float offsetY, float offsetZ) {
        rotation.add(offsetX, offsetY, offsetZ);
        if (rotation.x > (float) Math.toRadians(89.0)) rotation.x = (float) Math.toRadians(89.0);
        if (rotation.x < (float) Math.toRadians(-89.0)) rotation.x = (float) Math.toRadians(-89.0);
        while (rotation.y > (float) Math.PI) rotation.y -= (float) (2 * Math.PI);
        while (rotation.y < (float) -Math.PI) rotation.y += (float) (2 * Math.PI);
    }
    
    public void setRotation(float x, float y, float z) { rotation.set(x, y, z); }
    public Vector3f getPosition() { return position; }
    public Vector3f getRotation() { return rotation; }
    public float getFov() { return fov; }
    public void setFov(float fov) { this.fov = fov; }
}
