package com.za.zenith.entities;

import org.joml.Vector3f;

/**
 * Base class for all living entities (Players, Mobs).
 * Adds health and combat mechanics.
 */
public abstract class LivingEntity extends Entity {
    protected float health;
    protected float maxHealth;
    protected final com.za.zenith.world.items.stats.StatContainer stats = new com.za.zenith.world.items.stats.StatContainer();

    public LivingEntity(Vector3f position, float width, float height, float maxHealth) {
        super(position, width, height);
        this.maxHealth = maxHealth;
        this.health = maxHealth;
    }

    public void takeDamage(float amount) {
        float defense = getDefense();
        // Formula: damage = base_damage * (100 / (100 + defense))
        // 100 defense = 50% reduction, 200 = 66% reduction
        float multiplier = 100.0f / (100.0f + Math.max(0, defense));
        float finalDamage = amount * multiplier;
        
        this.health = Math.max(0, this.health - finalDamage);
    }

    public float getDefense() {
        return stats.get(com.za.zenith.world.items.stats.StatRegistry.DEFENSE);
    }

    public float getStat(com.za.zenith.utils.Identifier statId) {
        return stats.get(statId);
    }

    public com.za.zenith.world.items.stats.StatContainer getStats() {
        return stats;
    }

    public void heal(float amount) {
        this.health = Math.min(this.maxHealth, this.health + amount);
    }

    public boolean isDead() {
        return health <= 0;
    }

    public float getHealth() { return health; }
    public float getMaxHealth() { return maxHealth; }

    @Override
    protected void onUpdate(float deltaTime, com.za.zenith.world.World world) {
        applyGravity(deltaTime);
        move(world, velocity.x * deltaTime, velocity.y * deltaTime, velocity.z * deltaTime);
    }
}


