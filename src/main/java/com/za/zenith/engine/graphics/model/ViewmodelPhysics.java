package com.za.zenith.engine.graphics.model;

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
    private float frequency = 14.0f; // Snappiness (slightly higher for better feel)
    private float damping = 0.7f;   // Bounce reduction
    
    // Scratch vectors to avoid garbage collection and ensure stability
    private final Vector3f scratchAcc = new Vector3f();
    private final Vector3f scratchVel = new Vector3f();
    
    /**
     * Updates the physical state towards a target.
     * @param dt Delta time
     * @param targetPos Where the animation wants the item to be
     * @param targetRot Desired rotation
     * @param mass Weight of the item (0.1 to 5.0)
     * @param externalForce Forces from collisions or movement
     */
    public void update(float dt, Vector3f targetPos, Quaternionf targetRot, float mass, Vector3f externalForce) {
        // Clamp dt to prevent physical explosions (NaN) during huge lag spikes or game initialization
        dt = Math.min(dt, 0.05f);
        if (dt <= 0) return;

        // Ensure mass is sane
        float m = Math.max(0.1f, mass);

        // Adjust frequency based on mass (heavy items are slower)
        float adjustedFreq = frequency / (float)Math.sqrt(m);
        
        // 1. Translation Spring (Hooke's Law: F = k*x - d*v)
        // acceleration = (frequency^2 * (target - current)) - (2 * frequency * damping * velocity)
        
        scratchAcc.set(targetPos).sub(currentPos).mul(adjustedFreq * adjustedFreq);
        
        // Add external forces (collisions, centrifugal force)
        scratchAcc.add(new Vector3f(externalForce).div(m));
        
        // Apply damping
        scratchAcc.sub(new Vector3f(currentVel).mul(2.0f * adjustedFreq * damping));
        
        // Integrate velocity and position (Semi-implicit Euler for better stability)
        currentVel.add(scratchAcc.mul(dt));
        
        // Sanity check for velocity to prevent NaN explosion
        if (!Float.isFinite(currentVel.x) || !Float.isFinite(currentVel.y) || !Float.isFinite(currentVel.z)) {
            currentVel.set(0, 0, 0);
        }

        currentPos.add(new Vector3f(currentVel).mul(dt));
        
        // Sanity check for position
        if (!Float.isFinite(currentPos.x) || !Float.isFinite(currentPos.y) || !Float.isFinite(currentPos.z)) {
            currentPos.set(targetPos);
            currentVel.set(0, 0, 0);
        }
        
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


