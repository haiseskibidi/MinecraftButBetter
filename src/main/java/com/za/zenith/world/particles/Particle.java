package com.za.zenith.world.particles;

import org.joml.Vector3f;
import org.joml.Quaternionf;

/**
 * Базовый класс для всех частиц в Zenith.
 */
public abstract class Particle {
    protected final Vector3f position = new Vector3f();
    protected final Vector3f velocity = new Vector3f();
    protected final Quaternionf rotation = new Quaternionf();
    protected final Vector3f angularVelocity = new Vector3f();
    
    protected float lifeTime;
    protected float maxLifeTime;
    protected float scale = 1.0f;
    protected boolean removed = false;

    public Particle(Vector3f pos, Vector3f vel, float life) {
        this.position.set(pos);
        this.velocity.set(vel);
        this.maxLifeTime = life;
        this.lifeTime = life;
    }

    public void update(float deltaTime) {
        lifeTime -= deltaTime;
        if (lifeTime <= 0) {
            removed = true;
            return;
        }

        // Базовая физика
        position.add(new Vector3f(velocity).mul(deltaTime));
        
        // Вращение
        rotation.rotateXYZ(angularVelocity.x * deltaTime, angularVelocity.y * deltaTime, angularVelocity.z * deltaTime);
    }

    public Vector3f getPosition() { return position; }
    public Quaternionf getRotation() { return rotation; }
    public float getScale() { return scale; }
    public float getLifeRatio() { return lifeTime / maxLifeTime; }
    public boolean isRemoved() { return removed; }
}
