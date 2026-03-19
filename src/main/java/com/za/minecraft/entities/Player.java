package com.za.minecraft.entities;

import com.za.minecraft.world.World;
import org.joml.Vector3f;

/**
 * The human-controlled entity.
 */
public class Player extends LivingEntity {
    private static final float PLAYER_WIDTH = 0.6f;
    private static final float PLAYER_HEIGHT = 1.8f;
    private static final float JUMP_VELOCITY = 8.0f;
    
    private final Inventory inventory;
    
    // Survival stats
    private float hunger = 20.0f;
    private float saturation = 5.0f;
    private float noiseLevel = 0.0f;
    private float continuousNoise = 0.0f;
    private boolean sneaking = false;
    private boolean moving = false;
    private boolean sprinting = false;
    
    private static final float MAX_HUNGER = 20.0f;
    private static final float HUNGER_DEPletion_RATE = 0.1f; // Increased 20x for testing (was 0.005f)
    private static final float NOISE_DECAY_RATE = 0.5f;
    
    public Player(Vector3f startPosition) {
        super(startPosition, PLAYER_WIDTH, PLAYER_HEIGHT, 20.0f);
        this.inventory = new Inventory();
    }
    
    @Override
    public void update(float deltaTime, World world) {
        updateHunger(deltaTime);
        updateNoise(deltaTime);
        
        // Base physics and movement from Entity
        super.update(deltaTime, world);
    }

    private void updateNoise(float deltaTime) {
        float floorNoise = 0.0f;
        if (!flying && moving) {
            if (sprinting) floorNoise = 0.35f;
            else if (sneaking) floorNoise = 0.02f;
            else floorNoise = 0.10f;
        }
        
        floorNoise = Math.max(floorNoise, continuousNoise);
        
        if (noiseLevel > floorNoise) {
            noiseLevel = Math.max(floorNoise, noiseLevel - NOISE_DECAY_RATE * deltaTime);
        } else if (noiseLevel < floorNoise) {
            noiseLevel = floorNoise;
        }
        
        continuousNoise = 0.0f;
    }

    public void addNoise(float amount) {
        this.noiseLevel = Math.min(1.0f, this.noiseLevel + amount);
    }

    public void setContinuousNoise(float level) {
        this.continuousNoise = Math.max(this.continuousNoise, level);
    }

    public float getNoiseLevel() {
        return noiseLevel;
    }

    public void setSneaking(boolean sneaking) {
        this.sneaking = sneaking;
    }

    public boolean isSneaking() {
        return sneaking;
    }

    public void setMoving(boolean moving) {
        this.moving = moving;
    }

    public void setSprinting(boolean sprinting) {
        this.sprinting = sprinting;
    }

    private void updateHunger(float deltaTime) {
        float depletionMultiplier = 1.0f;
        if (sprinting) depletionMultiplier = 3.0f;
        if (!onGround && !flying) depletionMultiplier = 2.0f;

        if (saturation > 0) {
            saturation -= HUNGER_DEPletion_RATE * depletionMultiplier * deltaTime;
        } else {
            hunger = Math.max(0, hunger - HUNGER_DEPletion_RATE * depletionMultiplier * deltaTime);
        }
    }

    public void eat(com.za.minecraft.world.items.FoodItem food) {
        if (hunger < MAX_HUNGER) {
            hunger = Math.min(MAX_HUNGER, hunger + food.getNutrition());
            saturation = Math.min(MAX_HUNGER, saturation + food.getSaturationBonus());
            com.za.minecraft.utils.Logger.info("Ate %s. Hunger: %.1f", food.getName(), hunger);
        }
    }

    public float getHunger() {
        return hunger;
    }
    
    public void jump() {
        if (onGround || flying) {
            velocity.y = JUMP_VELOCITY;
            onGround = false;
        }
    }
    
    public void addVelocity(float vx, float vy, float vz) {
        velocity.add(vx, vy, vz);
    }
    
    public void applyHorizontalAcceleration(float ax, float az, float maxSpeed) {
        velocity.x += ax;
        velocity.z += az;
        float speed = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (speed > maxSpeed && speed > 0.0001f) {
            float scale = maxSpeed / speed;
            velocity.x *= scale;
            velocity.z *= scale;
        }
    }

    public void setHorizontalVelocity(float vx, float vz) {
        velocity.x = vx;
        velocity.z = vz;
    }
    
    public Inventory getInventory() {
        return inventory;
    }
}
