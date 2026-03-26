package com.za.minecraft.entities;

import com.za.minecraft.engine.core.PlayerMode;
import com.za.minecraft.world.World;
import org.joml.Vector3f;

/**
 * The human-controlled entity.
 */
public class Player extends LivingEntity {
    private final Inventory inventory;
    private com.za.minecraft.engine.core.PlayerMode mode = com.za.minecraft.engine.core.PlayerMode.SURVIVAL;
    private final com.za.minecraft.entities.parkour.ParkourHandler parkourHandler = new com.za.minecraft.entities.parkour.ParkourHandler();
    
    // Survival stats
    private float hunger = 20.0f;
    private float saturation = 5.0f;
    private float stamina = 1.0f;
    private float noiseLevel = 0.0f;
    private float continuousNoise = 0.0f;
    private boolean sneaking = false;
    private boolean moving = false;
    private boolean sprinting = false;
    
    // Animation and State
    private float currentEyeHeight;
    private float walkBobTimer = 0.0f;
    private float bobIntensity = 0.0f;
    private float swingProgress = 0.0f;
    private boolean swinging = false;
    private float parkourCameraTilt = 0.0f;
    private float parkourCameraRoll = 0.0f;
    private float fovOffset = 0.0f;

    private static final float MAX_HUNGER = 20.0f;
    private static final float HUNGER_DEPletion_RATE = 0.1f; // Increased 20x for testing (was 0.005f)
    private static final float NOISE_DECAY_RATE = 0.5f;
    
    public Player(Vector3f startPosition) {
        super(startPosition, 
              com.za.minecraft.world.physics.PhysicsSettings.getInstance().playerWidth, 
              com.za.minecraft.world.physics.PhysicsSettings.getInstance().standingHeight, 
              20.0f);
        this.inventory = new Inventory();
        this.currentEyeHeight = com.za.minecraft.world.physics.PhysicsSettings.getInstance().standingEyeHeight;
    }
    
    @Override
    public void update(float deltaTime, World world) {
        inventory.update(world, this, com.za.minecraft.engine.core.GameLoop.getInstance().getCamera());
        updateHunger(deltaTime);
        updateNoise(deltaTime);
        updateAnimations(deltaTime);
        updateSneakState(world, deltaTime);
        parkourHandler.update(this, deltaTime, world);
        
        // Base physics and movement from Entity
        super.update(deltaTime, world);
    }

    public com.za.minecraft.entities.parkour.ParkourHandler getParkourHandler() {
        return parkourHandler;
    }

    public float getStamina() {
        return stamina;
    }

    public void setStamina(float stamina) {
        this.stamina = stamina;
    }

    private void updateSneakState(World world, float deltaTime) {
        com.za.minecraft.world.physics.PhysicsSettings settings = com.za.minecraft.world.physics.PhysicsSettings.getInstance();
        float targetHeight = sneaking ? settings.sneakingHeight : settings.standingHeight;
        float targetEyeHeight = sneaking ? settings.sneakingEyeHeight : settings.standingEyeHeight;

        // If trying to stand up, check if there's enough space
        if (!sneaking && boundingBox.getMax().y < settings.standingHeight) {
            if (canStandUp(world)) {
                setBoundingBox(settings.playerWidth, settings.standingHeight);
            } else {
                // Force sneaking if blocked
                targetHeight = settings.sneakingHeight;
                targetEyeHeight = settings.sneakingEyeHeight;
            }
        } else if (sneaking && boundingBox.getMax().y > settings.sneakingHeight) {
            setBoundingBox(settings.playerWidth, settings.sneakingHeight);
        }

        // Smooth eye height transition
        float lerpSpeed = 10.0f;
        currentEyeHeight += (targetEyeHeight - currentEyeHeight) * lerpSpeed * deltaTime;
    }

    private boolean canStandUp(World world) {
        com.za.minecraft.world.physics.PhysicsSettings settings = com.za.minecraft.world.physics.PhysicsSettings.getInstance();
        // Check standing height from current feet position
        com.za.minecraft.world.physics.AABB standingBox = new com.za.minecraft.world.physics.AABB(
            -settings.playerWidth / 2, 0, -settings.playerWidth / 2,
            settings.playerWidth / 2, settings.standingHeight, settings.playerWidth / 2
        ).offset(position);

        int minX = (int) Math.floor(standingBox.getMin().x);
        int maxX = (int) Math.floor(standingBox.getMax().x);
        int minY = (int) Math.floor(standingBox.getMin().y);
        int maxY = (int) Math.floor(standingBox.getMax().y);
        int minZ = (int) Math.floor(standingBox.getMin().z);
        int maxZ = (int) Math.floor(standingBox.getMax().z);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    com.za.minecraft.world.blocks.Block block = world.getBlock(x, y, z);
                    if (!block.isAir() && block.isSolid()) {
                        com.za.minecraft.world.physics.VoxelShape shape = com.za.minecraft.world.blocks.BlockRegistry.getBlock(block.getType()).getShape(block.getMetadata());
                        if (shape != null) {
                            for (com.za.minecraft.world.physics.AABB box : shape.getBoxes()) {
                                if (standingBox.intersects(box.offset(x, y, z))) {
                                    return false;
                                }
                            }
                        }
                    }
                }
            }
        }
        return true;
    }

    public float getEyeHeight() {
        return currentEyeHeight;
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

        // Physically-Based Parkour Camera Logic
        float targetTilt = 0.0f;
        float targetRoll = 0.0f;
        float targetFovOffset = 0.0f;
        com.za.minecraft.entities.parkour.ParkourHandler.ParkourState pState = parkourHandler.getState();
        float pProgress = parkourHandler.getProgress();
        
        com.za.minecraft.entities.parkour.animation.ParkourAnimation currentAnim = null;
        if (pState == com.za.minecraft.entities.parkour.ParkourHandler.ParkourState.CLIMBING) {
            currentAnim = com.za.minecraft.entities.parkour.animation.AnimationRegistry.get("climbing");
        } else if (pState == com.za.minecraft.entities.parkour.ParkourHandler.ParkourState.GRABBING) {
            currentAnim = com.za.minecraft.entities.parkour.animation.AnimationRegistry.get("grabbing");
        }

        if (currentAnim != null) {
            float t = pProgress;
            float side = parkourHandler.getClimbSide();
            
            targetTilt = currentAnim.evaluate("camera_tilt", t, side);
            targetRoll = currentAnim.evaluate("camera_roll", t, side);
            targetFovOffset = currentAnim.evaluate("fov_offset", t, side);
            
            // Effort-based Jitter (Shaking) from JSON
            if (currentAnim.isJitterEnabled() && t > currentAnim.getJitterStart() && t < currentAnim.getJitterEnd()) {
                float intensityMult = (float) Math.sin((t - currentAnim.getJitterStart()) / (currentAnim.getJitterEnd() - currentAnim.getJitterStart()) * Math.PI);
                float intensity = currentAnim.getJitterIntensity() * intensityMult;
                targetTilt += (float) (Math.sin(System.currentTimeMillis() / 15.0) * intensity);
                targetRoll += (float) (Math.cos(System.currentTimeMillis() / 12.0) * intensity);
            }
        } else if (pState == com.za.minecraft.entities.parkour.ParkourHandler.ParkourState.HANGING) {
            targetTilt = 0.04f;
            targetRoll = (float) (Math.sin(System.currentTimeMillis() / 800.0) * 0.012f);
        }
        
        // Multi-rate Lerp for organic feel
        float tiltLerp = (pState == com.za.minecraft.entities.parkour.ParkourHandler.ParkourState.NONE) ? 4.0f : 12.0f;
        float rollLerp = (pState == com.za.minecraft.entities.parkour.ParkourHandler.ParkourState.NONE) ? 3.0f : 10.0f;
        float fovLerp = (pState == com.za.minecraft.entities.parkour.ParkourHandler.ParkourState.NONE) ? 2.0f : 8.0f;

        parkourCameraTilt += (targetTilt - parkourCameraTilt) * tiltLerp * deltaTime;
        parkourCameraRoll += (targetRoll - parkourCameraRoll) * rollLerp * deltaTime;
        fovOffset += (targetFovOffset - fovOffset) * fovLerp * deltaTime;
    }

    public float getCameraPitchOffset() {
        return parkourCameraTilt;
    }

    public float getCameraRollOffset() {
        return parkourCameraRoll;
    }

    public float getFovOffset() {
        return fovOffset;
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

    public boolean isPhysicallySneaking() {
        com.za.minecraft.world.physics.PhysicsSettings settings = com.za.minecraft.world.physics.PhysicsSettings.getInstance();
        return boundingBox.getMax().y < settings.standingHeight - 0.01f;
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
            velocity.y = com.za.minecraft.world.physics.PhysicsSettings.getInstance().jumpVelocity;
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
