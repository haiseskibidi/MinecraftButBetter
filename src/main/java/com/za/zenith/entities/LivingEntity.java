package com.za.zenith.entities;

import org.joml.Vector3f;

/**
 * Base class for all living entities (Players, Mobs).
 * Adds health and combat mechanics.
 */
public abstract class LivingEntity extends Entity {
    protected float health;
    protected float maxHealth;

    public LivingEntity(Vector3f position, float width, float height, float maxHealth) {
        super(position, width, height);
        this.maxHealth = maxHealth;
        this.health = maxHealth;
    }

    public void takeDamage(float amount) {
        this.health = Math.max(0, this.health - amount);
    }

    public void heal(float amount) {
        this.health = Math.min(this.maxHealth, this.health + amount);
    }

    public boolean isDead() {
        return health <= 0;
    }

    public float getHealth() { return health; }
    public float getMaxHealth() { return maxHealth; }
}
