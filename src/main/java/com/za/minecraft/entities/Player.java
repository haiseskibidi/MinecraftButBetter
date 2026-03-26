package com.za.minecraft.entities;

import com.za.minecraft.engine.core.PlayerMode;
import com.za.minecraft.world.World;
import com.za.minecraft.entities.parkour.animation.AnimationRegistry;
import com.za.minecraft.entities.parkour.animation.AnimationProfile;
import org.joml.Vector3f;

/**
 * AAA-Grade Player Entity. 
 * Synchronized Lerp & Movement Latching to eliminate all micro-jitter.
 */
public class Player extends LivingEntity {
    private final Inventory inventory;
    private com.za.minecraft.engine.core.PlayerMode mode = com.za.minecraft.engine.core.PlayerMode.SURVIVAL;
    private final com.za.minecraft.entities.parkour.ParkourHandler parkourHandler = new com.za.minecraft.entities.parkour.ParkourHandler();
    
    // Cached Registry to prevent GC spikes
    private final AnimationRegistry animationRegistry = new AnimationRegistry();
    
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
    private float parkourCameraTilt = 0.0f;
    private float parkourCameraRoll = 0.0f;
    private float fovOffset = 0.0f;
    private float cameraOffsetX = 0.0f;
    private float cameraOffsetY = 0.0f;
    private float cameraOffsetZ = 0.0f;
    private float lastYaw = 0.0f;
    private float leanAmount = 0.0f;

    // Item Animation State
    private boolean swinging = false;
    private float itemSwingTimer = 0.0f;
    private float itemOffsetX = 0.0f;
    private float itemOffsetY = 0.0f;
    private float itemOffsetZ = 0.0f;
    private float itemPitchOffset = 0.0f;
    private float itemYawOffset = 0.0f;
    private float itemRollOffset = 0.0f;

    // Locomotion Engine
    private float locomotionTimer = 0.0f;
    private float movementAlpha = 0.0f;
    private float landingImpact = 0.0f;
    private boolean wasOnGround = true;
    
    // Jitter Mitigation
    private float moveLatchTimer = 0.0f; // NEW: Keeps "moving" state stable
    private static final float LATCH_DURATION = 0.15f; 

    private static final float MAX_HUNGER = 20.0f;
    private static final float HUNGER_DEPletion_RATE = 0.1f;
    
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
        super.update(deltaTime, world);
    }

    public com.za.minecraft.entities.parkour.ParkourHandler getParkourHandler() {
        return parkourHandler;
    }

    private void updateSneakState(World world, float deltaTime) {
        com.za.minecraft.world.physics.PhysicsSettings settings = com.za.minecraft.world.physics.PhysicsSettings.getInstance();
        float targetEyeHeight = sneaking ? settings.sneakingEyeHeight : settings.standingEyeHeight;
        
        if (!sneaking && boundingBox.getMax().y < settings.standingHeight) {
            if (canStandUp(world)) {
                setBoundingBox(settings.playerWidth, settings.standingHeight);
            } else {
                targetEyeHeight = settings.sneakingEyeHeight;
            }
        } else if (sneaking && boundingBox.getMax().y > settings.sneakingHeight) {
            setBoundingBox(settings.playerWidth, settings.sneakingHeight);
        }
        currentEyeHeight += (targetEyeHeight - currentEyeHeight) * 10.0f * deltaTime;
    }

    private boolean canStandUp(World world) {
        com.za.minecraft.world.physics.PhysicsSettings settings = com.za.minecraft.world.physics.PhysicsSettings.getInstance();
        com.za.minecraft.world.physics.AABB standingBox = new com.za.minecraft.world.physics.AABB(
            -settings.playerWidth / 2, 0, -settings.playerWidth / 2,
            settings.playerWidth / 2, settings.standingHeight, settings.playerWidth / 2
        ).offset(position);
        
        for (int x = (int)Math.floor(standingBox.getMin().x); x <= (int)Math.floor(standingBox.getMax().x); x++) {
            for (int y = (int)Math.floor(standingBox.getMin().y); y <= (int)Math.floor(standingBox.getMax().y); y++) {
                for (int z = (int)Math.floor(standingBox.getMin().z); z <= (int)Math.floor(standingBox.getMax().z); z++) {
                    com.za.minecraft.world.blocks.Block block = world.getBlock(x, y, z);
                    if (!block.isAir() && block.isSolid()) return false;
                }
            }
        }
        return true;
    }

    public float getEyeHeight() { return currentEyeHeight; }

    private void updateAnimations(float deltaTime) {
        // 1. Inputs & Physics Feedback
        float currentYaw = getRotation().y;
        float yawDelta = currentYaw - lastYaw;
        while (yawDelta < -Math.PI) yawDelta += Math.PI * 2;
        while (yawDelta > Math.PI) yawDelta -= Math.PI * 2;
        
        // Filter out micro-mouse movements
        if (Math.abs(yawDelta) < 0.0001f) yawDelta = 0;
        
        float leanTarget = -yawDelta * 0.8f; 
        if (sprinting) leanTarget *= 1.2f;
        leanAmount += (leanTarget - leanAmount) * (sneaking ? 3.0f : 7.0f) * deltaTime;
        lastYaw = currentYaw;

        if (onGround && !wasOnGround && velocity.y < -4.0f) {
            landingImpact = Math.min(0.15f, Math.abs(velocity.y) * 0.015f);
        }
        wasOnGround = onGround;
        landingImpact += (0.0f - landingImpact) * 10.0f * deltaTime;

        // 2. Movement Latching (Anti-Jitter)
        boolean isMovingPhysically = onGround && moving && velocity.lengthSquared() > 0.0001f;
        if (isMovingPhysically) moveLatchTimer = LATCH_DURATION;
        else moveLatchTimer = Math.max(0, moveLatchTimer - deltaTime);
        
        boolean stableMoving = moveLatchTimer > 0;
        float alphaTarget = stableMoving ? 1.0f : 0.0f;
        movementAlpha += (alphaTarget - movementAlpha) * (sneaking ? 4.0f : 8.0f) * deltaTime;

        // 3. Profiles & Timing
        String cN = sneaking ? "sneak" : (sprinting ? "sprint" : "walk");
        String iN = sneaking ? "item_sneak" : (sprinting ? "item_sprint" : "item_walk");
        String ciN = sneaking ? "sneak_idle" : "idle";
        String iiN = sneaking ? "item_sneak_idle" : "item_idle";

        AnimationProfile cp = animationRegistry.get(cN);
        AnimationProfile ip = animationRegistry.get(iN);
        AnimationProfile cip = animationRegistry.get(ciN);
        AnimationProfile iip = animationRegistry.get(iiN);

        float iDur = (cip != null) ? cip.getDuration() : 1.0f;
        float wDur = (cp != null) ? cp.getDuration() : 1.0f;
        float currentDuration = iDur + (wDur - iDur) * movementAlpha;
        locomotionTimer += deltaTime / currentDuration;

        // 4. Evaluation and Blending
        float targetTilt = 0, targetRoll = 0, targetFov = 0, tCamY = 0;
        float tItX = 0, tItY = 0, tItZ = 0, tItP = 0, tItYw = 0, tItR = 0;

        com.za.minecraft.entities.parkour.ParkourHandler.ParkourState pState = parkourHandler.getState();
        if (pState != com.za.minecraft.entities.parkour.ParkourHandler.ParkourState.NONE) {
            AnimationProfile anim = animationRegistry.get((pState == com.za.minecraft.entities.parkour.ParkourHandler.ParkourState.CLIMBING) ? "climbing" : "grabbing");
            if (anim != null) {
                targetTilt = anim.evaluate("camera_tilt", parkourHandler.getProgress(), parkourHandler.getClimbSide());
                targetRoll = anim.evaluate("camera_roll", parkourHandler.getProgress(), parkourHandler.getClimbSide());
            }
        } else {
            // Evaluated values
            float wTilt = (cp != null) ? cp.evaluate("camera_tilt", locomotionTimer, 1.0f) : 0;
            float wRoll = (cp != null) ? cp.evaluate("camera_roll", locomotionTimer, 1.0f) : 0;
            float wFov = (cp != null) ? cp.evaluate("fov_offset", locomotionTimer, 1.0f) : 0;
            float wCamY = (cp != null) ? cp.evaluate("camera_y", locomotionTimer, 1.0f) : 0;

            float wItX = (ip != null) ? ip.evaluate("item_x", locomotionTimer, 1.0f) : 0;
            float wItY = (ip != null) ? ip.evaluate("item_y", locomotionTimer, 1.0f) : 0;
            float wItZ = (ip != null) ? ip.evaluate("item_z", locomotionTimer, 1.0f) : 0;
            float wItP = (ip != null) ? ip.evaluate("item_pitch", locomotionTimer, 1.0f) : 0;
            float wItR = (ip != null) ? ip.evaluate("item_roll", locomotionTimer, 1.0f) : 0;

            float iTilt = (cip != null) ? cip.evaluate("camera_tilt", locomotionTimer, 1.0f) : 0;
            float iRoll = (cip != null) ? cip.evaluate("camera_roll", locomotionTimer, 1.0f) : 0;
            float iCamY = (cip != null) ? cip.evaluate("camera_y", locomotionTimer, 1.0f) : 0;

            float iItX = (iip != null) ? iip.evaluate("item_x", locomotionTimer, 1.0f) : 0;
            float iItY = (iip != null) ? iip.evaluate("item_y", locomotionTimer, 1.0f) : 0;
            float iItZ = (iip != null) ? iip.evaluate("item_z", locomotionTimer, 1.0f) : 0;
            float iItP = (iip != null) ? iip.evaluate("item_pitch", locomotionTimer, 1.0f) : 0;
            float iItR = (iip != null) ? iip.evaluate("item_roll", locomotionTimer, 1.0f) : 0;

            // Blending
            targetTilt = iTilt + (wTilt - iTilt) * movementAlpha;
            targetRoll = (iRoll + (wRoll - iRoll) * movementAlpha) + leanAmount;
            targetFov = wFov * movementAlpha;
            tCamY = iCamY + (wCamY - iCamY) * movementAlpha;
            
            tItX = iItX + (wItX - iItX) * movementAlpha;
            tItY = iItY + (wItY - iItY) * movementAlpha;
            tItZ = iItZ + (wItZ - iItZ) * movementAlpha;
            tItP = iItP + (wItP - iItP) * movementAlpha;
            tItR = iItR + (wItR - iItR) * movementAlpha;
        }

        // 5. Final Synchronized Lerp (THE JITTER KILLER)
        // Camera and Item MUST use same speed to prevent relative vibration
        float syncLerp = (sneaking) ? 5.0f : 8.0f;
        
        parkourCameraTilt += (targetTilt + landingImpact - parkourCameraTilt) * syncLerp * deltaTime;
        parkourCameraRoll += (targetRoll - parkourCameraRoll) * syncLerp * deltaTime;
        fovOffset += (targetFov - fovOffset) * 4.0f * deltaTime;
        cameraOffsetY += (tCamY - cameraOffsetY) * syncLerp * deltaTime;

        itemOffsetX += (tItX + (leanAmount * 0.1f) - itemOffsetX) * syncLerp * deltaTime;
        itemOffsetY += (tItY - itemOffsetY) * syncLerp * deltaTime;
        itemOffsetZ += (tItZ - itemOffsetZ) * syncLerp * deltaTime;
        itemPitchOffset += (tItP - itemPitchOffset) * syncLerp * deltaTime;
        itemYawOffset += ((leanAmount * 0.2f) - itemYawOffset) * syncLerp * deltaTime;
        itemRollOffset += (tItR + (leanAmount * 0.5f) - itemRollOffset) * syncLerp * deltaTime;

        // 6. Action: Swing
        if (swinging) {
            AnimationProfile swingAnim = animationRegistry.get("item_swing");
            if (swingAnim != null) {
                itemSwingTimer += deltaTime / swingAnim.getDuration();
                if (itemSwingTimer >= 1.0f) { swinging = false; itemSwingTimer = 0; }
                else {
                    itemOffsetX += swingAnim.evaluate("item_x", itemSwingTimer, 1.0f);
                    itemOffsetY += swingAnim.evaluate("item_y", itemSwingTimer, 1.0f);
                    itemPitchOffset += swingAnim.evaluate("item_pitch", itemSwingTimer, 1.0f);
                }
            } else swinging = false;
        }
    }

    public float getCameraPitchOffset() { return parkourCameraTilt; }
    public float getCameraRollOffset() { return parkourCameraRoll; }
    public float getFovOffset() { return fovOffset; }
    public float getCameraOffsetX() { return cameraOffsetX; }
    public float getCameraOffsetY() { return cameraOffsetY; }
    public float getCameraOffsetZ() { return cameraOffsetZ; }
    public float getItemOffsetX() { return itemOffsetX; }
    public float getItemOffsetY() { return itemOffsetY; }
    public float getItemOffsetZ() { return itemOffsetZ; }
    public float getItemPitchOffset() { return itemPitchOffset; }
    public float getItemYawOffset() { return itemYawOffset; }
    public float getItemRollOffset() { return itemRollOffset; }

    public void swing() { if (!swinging) { swinging = true; itemSwingTimer = 0; } }
    public boolean isMoving() { return moving; }
    private void updateNoise(float deltaTime) {
        float floorNoise = 0.0f;
        if (!flying && moving) {
            if (sprinting) floorNoise = 0.35f;
            else if (sneaking) floorNoise = 0.02f;
            else floorNoise = 0.10f;
        }
        noiseLevel = Math.max(floorNoise, Math.max(continuousNoise, noiseLevel - 0.5f * deltaTime));
        continuousNoise = 0.0f;
    }
    public void addNoise(float amount) { this.noiseLevel = Math.min(1.0f, this.noiseLevel + amount); }
    public void setContinuousNoise(float level) { this.continuousNoise = Math.max(this.continuousNoise, level); }
    public float getNoiseLevel() { return noiseLevel; }
    public void setSneaking(boolean sneaking) { this.sneaking = sneaking; }
    public boolean isSneaking() { return sneaking; }
    public boolean isPhysicallySneaking() { return boundingBox.getMax().y < com.za.minecraft.world.physics.PhysicsSettings.getInstance().standingHeight - 0.01f; }
    public void setMoving(boolean moving) { this.moving = moving; }
    public void setSprinting(boolean sprinting) { this.sprinting = sprinting; }
    public float getStamina() { return stamina; }
    public void setStamina(float stamina) { this.stamina = stamina; }

    private void updateHunger(float deltaTime) {
        float mult = sprinting ? 3.0f : (!onGround && !flying ? 2.0f : 1.0f);
        if (saturation > 0) saturation -= 0.1f * mult * deltaTime;
        else hunger = Math.max(0, hunger - 0.1f * mult * deltaTime);
    }
    public void eat(com.za.minecraft.world.items.Item item) {
        com.za.minecraft.world.items.component.FoodComponent food = item.getComponent(com.za.minecraft.world.items.component.FoodComponent.class);
        if (food != null && hunger < 20.0f) {
            hunger = Math.min(20.0f, hunger + food.nutrition());
            saturation = Math.min(20.0f, saturation + food.saturationBonus());
        }
    }
    public float getHunger() { return hunger; }
    public void jump() { if (onGround || flying) { velocity.y = com.za.minecraft.world.physics.PhysicsSettings.getInstance().jumpVelocity; onGround = false; } }
    public void addVelocity(float vx, float vy, float vz) { velocity.add(vx, vy, vz); }
    public void applyHorizontalAcceleration(float ax, float az, float maxSpeed) {
        velocity.x += ax; velocity.z += az;
        float speed = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        if (speed > maxSpeed && speed > 0.0001f) { float scale = maxSpeed / speed; velocity.x *= scale; velocity.z *= scale; }
    }
    public void setHorizontalVelocity(float vx, float vz) { velocity.x = vx; velocity.z = vz; }
    public Inventory getInventory() { return inventory; }
    public PlayerMode getMode() { return mode; }
    public void setMode(PlayerMode mode) { this.mode = mode; }
}
