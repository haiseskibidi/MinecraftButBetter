package com.za.minecraft.entities;

import com.za.minecraft.engine.core.PlayerMode;
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
    private PlayerMode mode = PlayerMode.SURVIVAL;
    
    // Survival stats
    private float hunger = 20.0f;
    private float saturation = 5.0f;
    private float noiseLevel = 0.0f;
    private float continuousNoise = 0.0f;
    private boolean sneaking = false;
    private boolean moving = false;
    private boolean sprinting = false;
    
    // Animation states
    private float walkBobTimer = 0.0f;
    private float bobIntensity = 0.0f;
    private float swingProgress = 0.0f;
    private boolean swinging = false;
    
    private static final float MAX_HUNGER = 20.0f;
    private static final float HUNGER_DEPletion_RATE = 0.1f; // Increased 20x for testing (was 0.005f)
    private static final float NOISE_DECAY_RATE = 0.5f;
    
    public Player(Vector3f startPosition) {
        super(startPosition, PLAYER_WIDTH, PLAYER_HEIGHT, 20.0f);
        this.inventory = new Inventory();
    }
    
    @Override
    public void update(float deltaTime, World world) {
        inventory.update(world, this, com.za.minecraft.engine.core.GameLoop.getInstance().getCamera());
        updateHunger(deltaTime);
        updateNoise(deltaTime);
        updateAnimations(deltaTime);
        
        // Base physics and movement from Entity
        super.update(deltaTime, world);
    }

    private void updateAnimations(float deltaTime) {
        // Walk bobbing
        boolean isWalking = onGround && moving && velocity.lengthSquared() > 0.01f;
        
        if (isWalking) {
            float speedMult = sprinting ? 1.5f : (sneaking ? 0.5f : 1.0f);
            walkBobTimer += 10.0f * speedMult * deltaTime;
            bobIntensity = Math.min(1.0f, bobIntensity + 5.0f * deltaTime);
        } else {
            bobIntensity = Math.max(0.0f, bobIntensity - 5.0f * deltaTime);
            if (bobIntensity > 0) {
                walkBobTimer += 5.0f * deltaTime; // Keep timer moving while intensity fades
            } else {
                walkBobTimer = 0;
            }
        }

        // Swing animation
        if (swinging) {
            swingProgress += 5.0f * deltaTime; // Quick swing
            if (swingProgress >= 1.0f) {
                swingProgress = 0;
                swinging = false;
            }
        }
    }

    public void swing() {
        if (!swinging) {
            swinging = true;
            swingProgress = 0;
        }
    }

    public float getWalkBobTimer() { return walkBobTimer; }
    public float getBobIntensity() { return bobIntensity; }
    public float getSwingProgress() { return swingProgress; }
    public boolean isMoving() { return moving; }

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

    public void eat(com.za.minecraft.world.items.Item item) {
        if (hunger < MAX_HUNGER) {
            com.za.minecraft.world.items.component.FoodComponent food = item.getComponent(com.za.minecraft.world.items.component.FoodComponent.class);
            if (food != null) {
                hunger = Math.min(MAX_HUNGER, hunger + food.nutrition());
                saturation = Math.min(MAX_HUNGER, saturation + food.saturationBonus());
                com.za.minecraft.utils.Logger.info("Ate %s. Hunger: %.1f", item.getName(), hunger);
            }
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

    public PlayerMode getMode() {
        return mode;
    }

    public void setMode(PlayerMode mode) {
        this.mode = mode;
    }
}
