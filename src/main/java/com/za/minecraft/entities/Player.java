package com.za.minecraft.entities;

import com.za.minecraft.world.BlockPos;
import com.za.minecraft.world.World;
import com.za.minecraft.world.blocks.Block;
import com.za.minecraft.world.physics.AABB;
import org.joml.Vector3f;

public class Player {
    private static final float PLAYER_WIDTH = 0.6f;
    private static final float PLAYER_HEIGHT = 1.8f;
    private static final float GRAVITY = -9.81f * 2.0f;
    private static final float JUMP_VELOCITY = 8.0f;
    private static final float TERMINAL_VELOCITY = -50.0f;
    
    private final Vector3f position;
    private final Vector3f velocity;
    private final AABB boundingBox;
    private final Inventory inventory;
    
    private boolean onGround = false;
    private boolean flying = false;
    private boolean sprinting = false;

    // Survival stats
    private float hunger = 20.0f;
    private float saturation = 5.0f;
    private float noiseLevel = 0.0f;
    private float continuousNoise = 0.0f;
    private boolean sneaking = false;
    private boolean moving = false;
    private static final float MAX_HUNGER = 20.0f;
    private static final float HUNGER_DEPletion_RATE = 0.005f; // Base loss per second
    private static final float NOISE_DECAY_RATE = 0.5f; // Fades from 100% to 0% in 2 seconds
    
    public Player(Vector3f startPosition) {
        this.position = new Vector3f(startPosition);
        this.velocity = new Vector3f();
        this.boundingBox = new AABB(
            -PLAYER_WIDTH / 2, 0, -PLAYER_WIDTH / 2,
            PLAYER_WIDTH / 2, PLAYER_HEIGHT, PLAYER_WIDTH / 2
        );
        this.inventory = new Inventory();
    }
    
    public void update(float deltaTime, World world) {
        if (!flying) {
            velocity.y = Math.max(velocity.y + GRAVITY * deltaTime, TERMINAL_VELOCITY);
        }
        
        updateHunger(deltaTime);
        updateNoise(deltaTime);
        move(world, velocity.x * deltaTime, velocity.y * deltaTime, velocity.z * deltaTime);

        // Snap very small residual velocities to zero to end micro-sliding
        if (Math.abs(velocity.x) < 0.005f) velocity.x = 0f;
        if (Math.abs(velocity.z) < 0.005f) velocity.z = 0f;
        if (onGround && Math.abs(velocity.y) < 0.005f) velocity.y = 0f;
    }

    private void updateNoise(float deltaTime) {
        // Calculate floor noise based on current movement state
        float floorNoise = 0.0f;
        if (!flying && moving) {
            if (sprinting) floorNoise = 0.35f;
            else if (sneaking) floorNoise = 0.02f;
            else floorNoise = 0.10f;
        }
        
        // Also consider other continuous sources (like breaking blocks)
        floorNoise = Math.max(floorNoise, continuousNoise);
        
        if (noiseLevel > floorNoise) {
            // Smoothly decay towards the floor
            noiseLevel = Math.max(floorNoise, noiseLevel - NOISE_DECAY_RATE * deltaTime);
        } else if (noiseLevel < floorNoise) {
            // Instantly rise to floor level
            noiseLevel = floorNoise;
        }
        
        // Reset continuous sources for the next frame
        continuousNoise = 0.0f;
    }

    public void addNoise(float amount) {
        // Additive spikes (jumps, landings, block breaks)
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

    public boolean isMoving() {
        return moving;
    }

    private void updateHunger(float deltaTime) {
        float depletionMultiplier = 1.0f;
        if (sprinting) depletionMultiplier = 3.0f;
        if (!onGround && !flying) depletionMultiplier = 2.0f; // Jumping/falling

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

    public void setSprinting(boolean sprinting) {
        this.sprinting = sprinting;
    }
    
    private void move(World world, float dx, float dy, float dz) {
        float originalDx = dx;
        float originalDy = dy;
        float originalDz = dz;
        
        AABB currentBox = boundingBox.offset(position);
        
        if (dy != 0) {
            for (int x = (int) Math.floor(currentBox.getMin().x); x <= Math.floor(currentBox.getMax().x); x++) {
                for (int z = (int) Math.floor(currentBox.getMin().z); z <= Math.floor(currentBox.getMax().z); z++) {
                    for (int y = (int) Math.floor(currentBox.getMin().y + dy); y <= Math.floor(currentBox.getMax().y + dy); y++) {
                        Block block = world.getBlock(x, y, z);
                        if (!block.isAir()) {
                            AABB blockBox = new AABB(x, y, z, x + 1, y + 1, z + 1);
                            AABB testBox = currentBox.offset(0, dy, 0);
                            
                            if (testBox.intersects(blockBox)) {
                                if (dy > 0) {
                                    dy = blockBox.getMin().y - currentBox.getMax().y - 0.001f;
                                } else {
                                    dy = blockBox.getMax().y - currentBox.getMin().y + 0.001f;
                                    onGround = true;
                                }
                                velocity.y = 0;
                                break;
                            }
                        }
                    }
                }
            }
            position.y += dy;
            currentBox = boundingBox.offset(position);
        }
        
        if (dx != 0) {
            for (int x = (int) Math.floor(currentBox.getMin().x + dx); x <= Math.floor(currentBox.getMax().x + dx); x++) {
                for (int z = (int) Math.floor(currentBox.getMin().z); z <= Math.floor(currentBox.getMax().z); z++) {
                    for (int y = (int) Math.floor(currentBox.getMin().y); y <= Math.floor(currentBox.getMax().y); y++) {
                        Block block = world.getBlock(x, y, z);
                        if (!block.isAir()) {
                            AABB blockBox = new AABB(x, y, z, x + 1, y + 1, z + 1);
                            AABB testBox = currentBox.offset(dx, 0, 0);
                            
                            if (testBox.intersects(blockBox)) {
                                if (dx > 0) {
                                    dx = blockBox.getMin().x - currentBox.getMax().x - 0.001f;
                                } else {
                                    dx = blockBox.getMax().x - currentBox.getMin().x + 0.001f;
                                }
                                velocity.x = 0;
                                break;
                            }
                        }
                    }
                }
            }
            position.x += dx;
            currentBox = boundingBox.offset(position);
        }
        
        if (dz != 0) {
            for (int x = (int) Math.floor(currentBox.getMin().x); x <= Math.floor(currentBox.getMax().x); x++) {
                for (int z = (int) Math.floor(currentBox.getMin().z + dz); z <= Math.floor(currentBox.getMax().z + dz); z++) {
                    for (int y = (int) Math.floor(currentBox.getMin().y); y <= Math.floor(currentBox.getMax().y); y++) {
                        Block block = world.getBlock(x, y, z);
                        if (!block.isAir()) {
                            AABB blockBox = new AABB(x, y, z, x + 1, y + 1, z + 1);
                            AABB testBox = currentBox.offset(0, 0, dz);
                            
                            if (testBox.intersects(blockBox)) {
                                if (dz > 0) {
                                    dz = blockBox.getMin().z - currentBox.getMax().z - 0.001f;
                                } else {
                                    dz = blockBox.getMax().z - currentBox.getMin().z + 0.001f;
                                }
                                velocity.z = 0;
                                break;
                            }
                        }
                    }
                }
            }
            position.z += dz;
        }
        
        if (Math.abs(originalDy) > 0.001f && Math.abs(dy) < 0.001f && originalDy < 0) {
            onGround = true;
        } else if (Math.abs(originalDy) > 0.001f) {
            onGround = false;
        }
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
    
    public Vector3f getPosition() {
        return new Vector3f(position);
    }
    
    public void setPosition(float x, float y, float z) {
        position.set(x, y, z);
    }
    
    public Vector3f getVelocity() {
        return new Vector3f(velocity);
    }
    
    public boolean isOnGround() {
        return onGround;
    }
    
    public boolean isFlying() {
        return flying;
    }
    
    public void setFlying(boolean flying) {
        this.flying = flying;
        if (flying) {
            velocity.y = 0;
        }
    }
    
    public AABB getBoundingBox() {
        return boundingBox.offset(position);
    }
    
    public Inventory getInventory() {
        return inventory;
    }
}
