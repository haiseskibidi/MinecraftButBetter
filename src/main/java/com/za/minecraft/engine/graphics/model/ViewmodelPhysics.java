package com.za.minecraft.engine.graphics.model;

import org.joml.Vector3f;
import org.joml.Quaternionf;

/**
 * High-fidelity 2nd order spring-mass-damper simulator for physical items.
 * Simulates inertia, mass, and external forces (collisions).
 */
public class ViewmodelPhysics {
    // Physical State
    public final Vector3f currentPos = new Vector3f();
    public final Vector3f currentVel = new Vector3f();
    public final Quaternionf currentRot = new Quaternionf();
    private final Vector3f angularVel = new Vector3f();
    
    // Spring Settings
    private float frequency = 12.0f; // Snappiness
    private float damping = 0.65f;   // Bounce reduction (0.5-0.8 is best)
    
    /**
     * Updates the physical state towards a target.
     * @param dt Delta time
     * @param targetPos Where the animation wants the item to be
     * @param targetRot Desired rotation
     * @param mass Weight of the item (0.1 to 5.0)
     * @param externalForce Forces from collisions or movement
     */
    public void update(float dt, Vector3f targetPos, Quaternionf targetRot, float mass, Vector3f externalForce) {
        // Adjust frequency based on mass (heavy items are slower)
        float adjustedFreq = frequency / (float)Math.sqrt(mass);
        
        // 1. Translation Spring
        // Force = k * (target - current) - d * velocity
        Vector3f posDiff = new Vector3f(targetPos).sub(currentPos);
        Vector3f acceleration = posDiff.mul(adjustedFreq * adjustedFreq);
        
        // Add external forces (collisions, centrifugal force)
        acceleration.add(externalForce.div(mass));
        
        // Apply damping
        acceleration.sub(new Vector3f(currentVel).mul(2.0f * adjustedFreq * damping));
        
        // Integrate velocity and position
        currentVel.add(acceleration.mul(dt));
        currentPos.add(new Vector3f(currentVel).mul(dt));
        
        // 2. Rotation Slerp (Simplified rotation lag)
        float rotFollowSpeed = adjustedFreq * 1.5f;
        currentRot.slerp(targetRot, Math.min(1.0f, dt * rotFollowSpeed));
    }
    
    public void reset(Vector3f pos, Quaternionf rot) {
        currentPos.set(pos);
        currentVel.set(0, 0, 0);
        currentRot.set(rot);
        angularVel.set(0, 0, 0);
    }
}
