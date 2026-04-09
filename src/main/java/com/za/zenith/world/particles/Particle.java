package com.za.zenith.world.particles;

import org.joml.Vector3f;

/**
 * Базовый класс для всех частиц в Zenith.
 * Ориентирован на визуальную стабильность и производительность (без коллизий).
 */
public abstract class Particle {
    protected final Vector3f position = new Vector3f();
    protected final Vector3f velocity = new Vector3f();
    
    protected float lifeTime;
    protected float maxLifeTime;
    protected float scale = 1.0f;
    protected float roll = 0.0f;
    protected float rollVelocity = 0.0f;
    protected float alpha = 1.0f;
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

        // Движение по дуге (скорость + гравитация)
        position.add(new Vector3f(velocity).mul(deltaTime));
        
        // Вращение в плоскости экрана (roll)
        roll += rollVelocity * deltaTime;
        
        // Плавное затухание к концу жизни
        float ratio = lifeTime / maxLifeTime;
        alpha = Math.min(1.0f, ratio * 2.0f); // Начинает исчезать во второй половине жизни
    }

    public Vector3f getPosition() { return position; }
    public float getScale() { return scale; }
    public float getRoll() { return roll; }
    public float getAlpha() { return alpha; }
    public boolean isRemoved() { return removed; }
    public float getLifeRatio() { return lifeTime / maxLifeTime; }
}
