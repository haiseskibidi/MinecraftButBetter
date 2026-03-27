package com.za.minecraft.entities;

import com.za.minecraft.engine.core.PlayerMode;
import com.za.minecraft.world.World;
import com.za.minecraft.entities.parkour.animation.AnimationRegistry;
import com.za.minecraft.entities.parkour.animation.AnimationProfile;
import org.joml.Vector3f;

/**
 * AAA Player Entity with alternating parkour animations and heavy impulse landings.
 */
public class Player extends LivingEntity {
    private final Inventory inventory;
    private com.za.minecraft.engine.core.PlayerMode mode = com.za.minecraft.engine.core.PlayerMode.SURVIVAL;
    private final com.za.minecraft.entities.parkour.ParkourHandler parkourHandler = new com.za.minecraft.entities.parkour.ParkourHandler();
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
    private boolean wasOnGround = true;
    private boolean isFirstFrame = true;
    
    // Impulse States
    private float landingTimer = 1.0f;
    private float landingScale = 0.0f;
    private float landingSide = 1.0f;
    private float preUpdateVelocityY = 0.0f;
    private float fallingTimer = 0.0f;

    // Advanced Transitions
    private float parkourWeight = 0.0f; 
    private float moveLatchTimer = 0.0f;
    private static final float LATCH_DURATION = 0.15f; 

    private static final float MAX_HUNGER = 20.0f;
    
    public Player(Vector3f startPosition) {
        super(startPosition, 
              com.za.minecraft.world.physics.PhysicsSettings.getInstance().playerWidth, 
              com.za.minecraft.world.physics.PhysicsSettings.getInstance().standingHeight, 
              20.0f);
        this.inventory = new Inventory();
        
        com.za.minecraft.world.physics.PhysicsSettings settings = com.za.minecraft.world.physics.PhysicsSettings.getInstance();
        this.currentEyeHeight = settings.standingEyeHeight;
        this.wasOnGround = true; 
        this.landingTimer = 1.0f;
    }
    
    @Override
    public void update(float deltaTime, World world) {
        inventory.update(world, this, com.za.minecraft.engine.core.GameLoop.getInstance().getCamera());
        updateHunger(deltaTime);
        updateNoise(deltaTime);
        updateSneakState(world, deltaTime);
        parkourHandler.update(this, deltaTime, world);
        
        // --- 1. FIXED PHYSICAL LOCOMOTION (Update 170Hz) ---
        boolean isMovingPhysically = onGround && moving && velocity.lengthSquared() > 0.0001f;
        if (isMovingPhysically) moveLatchTimer = LATCH_DURATION;
        else moveLatchTimer = Math.max(0, moveLatchTimer - deltaTime);
        
        float alphaTarget = (moveLatchTimer > 0) ? 1.0f : 0.0f;
        movementAlpha += (alphaTarget - movementAlpha) * (sneaking ? 4.0f : 8.0f) * deltaTime;

        // Locomotion Timer
        String cN = sneaking ? "sneak" : (sprinting ? "sprint" : "walk");
        String ciN = sneaking ? "sneak_idle" : "idle";
        AnimationProfile cp = animationRegistry.get(cN);
        AnimationProfile cip = animationRegistry.get(ciN);
        float iDur = (cip != null) ? cip.getDuration() : 1.0f;
        float wDur = (cp != null) ? cp.getDuration() : 1.0f;
        
        float speedFactor = 1.0f;
        if (sneaking) {
            float horizontalSpeed = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
            float baseSneakSpeed = com.za.minecraft.world.physics.PhysicsSettings.getInstance().baseMoveSpeed * 0.35f;
            speedFactor = Math.max(0.2f, horizontalSpeed / baseSneakSpeed);
        }
        
        float currentDuration = iDur + ( (wDur / speedFactor) - iDur) * movementAlpha;
        locomotionTimer = (locomotionTimer + deltaTime / currentDuration) % 1.0f;
        
        // --- KEY FIX: Save vertical velocity BEFORE physics potentially clears it ---
        preUpdateVelocityY = velocity.y;
        super.update(deltaTime, world);
    }

    public void updateAnimations(float deltaTime) {
        if (isFirstFrame) {
            lastYaw = getRotation().y;
            wasOnGround = onGround;
            isFirstFrame = false;
            return;
        }

        // --- 2. VISUAL EVALUATION ---
        float currentYaw = getRotation().y;
        float yawDelta = currentYaw - lastYaw;
        while (yawDelta < -Math.PI) yawDelta += Math.PI * 2;
        while (yawDelta > Math.PI) yawDelta -= Math.PI * 2;
        if (Math.abs(yawDelta) < 0.0001f) yawDelta = 0;
        float leanTarget = -yawDelta * 0.8f; 
        if (sprinting) leanTarget *= 1.2f;
        leanAmount += (leanTarget - leanAmount) * (sneaking ? 3.0f : 7.0f) * deltaTime;
        lastYaw = currentYaw;

        // --- 3. IMPULSE TRIGGER: LANDING ---
        if (onGround && !wasOnGround && preUpdateVelocityY < -2.0f) {
            landingTimer = 0.0f;
            landingSide = Math.random() > 0.5 ? 1.0f : -1.0f;
            // Retuned Scaling: less sensitive for normal jumps (v~8), heavy for high falls (v>15)
            float v = Math.abs(preUpdateVelocityY);
            landingScale = Math.min(2.2f, (v * v) / 250.0f + (v * 0.02f)); 
        }
        wasOnGround = onGround;

        // Profiles
        com.za.minecraft.world.items.Item heldItem = inventory.getSelectedItem();
        
        String cN = sneaking ? "sneak" : (sprinting ? "sprint" : "walk");
        String iN = sneaking ? "item_sneak" : (sprinting ? "item_sprint" : "item_walk");
        if (heldItem != null) iN = heldItem.getAnimation(iN);
        
        String ciN = sneaking ? "sneak_idle" : "idle"; 
        String iiN = sneaking ? "item_sneak" : "item_idle"; 
        if (heldItem != null) iiN = heldItem.getAnimation(iiN);

        AnimationProfile cp = animationRegistry.get(cN);
        AnimationProfile ip = animationRegistry.get(iN);
        AnimationProfile cip = animationRegistry.get(ciN);
        AnimationProfile iip = animationRegistry.get(iiN);

        // Evaluations
        float wTilt = (cp != null) ? cp.evaluate("camera_tilt", locomotionTimer, 1.0f) : 0;
        float wRoll = (cp != null) ? cp.evaluate("camera_roll", locomotionTimer, 1.0f) : 0;
        float wFov = (cp != null) ? cp.evaluate("fov_offset", locomotionTimer, 1.0f) : 0;
        float wCamY = (cp != null) ? cp.evaluate("camera_y", locomotionTimer, 1.0f) : 0;

        float wItX = (ip != null) ? ip.evaluate("item_x", locomotionTimer, 1.0f) : 0;
        float wItY = (ip != null) ? ip.evaluate("item_y", locomotionTimer, 1.0f) : 0;
        float wItZ = (ip != null) ? ip.evaluate("item_z", locomotionTimer, 1.0f) : 0;
        float wItP = (ip != null) ? ip.evaluate("item_pitch", locomotionTimer, 1.0f) : 0;
        float wItR = (ip != null) ? ip.evaluate("item_roll", locomotionTimer, 1.0f) : 0;

        // Base evaluations
        float iTilt = (cip != null) ? cip.evaluate("camera_tilt", locomotionTimer, 1.0f) : 0;
        float iRoll = (cip != null) ? cip.evaluate("camera_roll", locomotionTimer, 1.0f) : 0;
        float iCamY = (cip != null) ? cip.evaluate("camera_y", locomotionTimer, 1.0f) : 0;
        float iCamX = (cip != null) ? cip.evaluate("camera_x", locomotionTimer, 1.0f) : 0;

        float iItX = (iip != null) ? iip.evaluate("item_x", sneaking ? 0.0f : locomotionTimer, 1.0f) : 0;
        float iItY = (iip != null) ? iip.evaluate("item_y", locomotionTimer, 1.0f) : 0;
        float iItZ = (iip != null) ? iip.evaluate("item_z", locomotionTimer, 1.0f) : 0;
        float iItP = (iip != null) ? iip.evaluate("item_pitch", locomotionTimer, 1.0f) : 0;
        float iItR = (iip != null) ? iip.evaluate("item_roll", sneaking ? 0.0f : locomotionTimer, 1.0f) : 0;

        // 4. Blending Logic
        float horizontalSpeed = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        float maxSneakSpeed = com.za.minecraft.world.physics.PhysicsSettings.getInstance().baseMoveSpeed * 0.35f;
        float speedIntensity = Math.min(1.0f, horizontalSpeed / maxSneakSpeed);

        float targetTilt, targetRoll, tCamY, tCamX, targetFov;
        float tItX, tItY, tItZ, tItP, tItYw, tItR;

        targetFov = wFov * movementAlpha;

        if (sneaking) {
            float breathWeight = 0.4f + (speedIntensity * 0.6f);
            float walkWeight = speedIntensity; 
            targetTilt = iTilt + (wTilt - iTilt) * breathWeight;
            targetRoll = (iRoll + (wRoll - iRoll) * breathWeight) + leanAmount;
            tCamY = iCamY + (wCamY - iCamY) * breathWeight;
            tCamX = iCamX * walkWeight; 
            tItX = iItX + (wItX - iItX) * walkWeight;
            tItY = iItY + (wItY - iItY) * breathWeight;
            tItZ = iItZ + (wItZ - iItZ) * breathWeight;
            tItP = iItP + (wItP - iItP) * breathWeight;
            tItYw = (leanAmount * 0.2f); // Base yaw from lean
            tItR = iItR + (wItR - iItR) * walkWeight;
        } else {
            targetTilt = iTilt + (wTilt - iTilt) * movementAlpha;
            targetRoll = (iRoll + (wRoll - iRoll) * movementAlpha) + leanAmount;
            tCamY = iCamY + (wCamY - iCamY) * movementAlpha;
            tCamX = 0; 
            tItX = iItX + (wItX - iItX) * movementAlpha;
            tItY = iItY + (wItY - iItY) * movementAlpha;
            tItZ = iItZ + (wItZ - iItZ) * movementAlpha;
            tItP = iItP + (wItP - iItP) * movementAlpha;
            tItYw = (leanAmount * 0.2f);
            tItR = iItR + (wItR - iItR) * movementAlpha;
        }

        // --- 4.5 PARKOUR BLENDING ---
        float parkourTarget = parkourHandler.isInParkour() ? 1.0f : 0.0f;
        parkourWeight += (parkourTarget - parkourWeight) * 18.0f * deltaTime;

        if (parkourWeight > 0.001f) {
            String pN = (parkourHandler.getState() == com.za.minecraft.entities.parkour.ParkourHandler.ParkourState.CLIMBING) ? "climbing" : "grabbing";
            AnimationProfile pp = animationRegistry.get(pN);
            if (pp != null) {
                float pT = parkourHandler.getProgress();
                float pSide = parkourHandler.getClimbSide();

                float pTilt = pp.evaluate("camera_tilt", pT, pSide);
                float pRoll = pp.evaluate("camera_roll", pT, pSide);
                float pCamY = pp.evaluate("camera_y", pT, pSide);
                float pCamX = pp.evaluate("camera_x", pT, pSide);
                float pFov = pp.evaluate("fov_offset", pT, pSide);

                float pItX = pp.evaluate("item_x", pT, pSide);
                float pItY = pp.evaluate("item_y", pT, pSide);
                float pItZ = pp.evaluate("item_z", pT, pSide);
                float pItP = pp.evaluate("item_pitch", pT, pSide);
                float pItYw = pp.evaluate("item_yaw", pT, pSide);
                float pItR = pp.evaluate("item_roll", pT, pSide);

                // Blend locomotion with parkour
                targetTilt = targetTilt + (pTilt - targetTilt) * parkourWeight;
                targetRoll = targetRoll + (pRoll - targetRoll) * parkourWeight;
                tCamY = tCamY + (pCamY - tCamY) * parkourWeight;
                tCamX = tCamX + (pCamX - tCamX) * parkourWeight;
                targetFov = targetFov + (pFov - targetFov) * parkourWeight;

                tItX = tItX + (pItX - tItX) * parkourWeight;
                tItY = tItY + (pItY - tItY) * parkourWeight;
                tItZ = tItZ + (pItZ - tItZ) * parkourWeight;
                tItP = tItP + (pItP - tItP) * parkourWeight;
                tItYw = tItYw + (pItYw - tItYw) * parkourWeight;
                tItR = tItR + (pItR - tItR) * parkourWeight;
            }
        }

        // 5. Landing Impulse Apply
        if (landingTimer < 1.0f) {
            AnimationProfile lp = animationRegistry.get("landing");
            if (lp != null) {
                landingTimer += deltaTime / lp.getDuration();
                float t = Math.min(1.0f, landingTimer);
                targetTilt += lp.evaluate("camera_tilt", t, landingSide) * landingScale;
                targetRoll += lp.evaluate("camera_roll", t, landingSide) * landingScale;
                tCamY += lp.evaluate("camera_y", t, landingSide) * landingScale;
                tCamX += lp.evaluate("camera_x", t, landingSide) * landingScale;
                targetFov += lp.evaluate("fov_offset", t, landingSide) * landingScale;
                tItY += lp.evaluate("item_y", t, landingSide) * landingScale;
                tItP += lp.evaluate("item_pitch", t, landingSide) * landingScale;
                tItR += lp.evaluate("item_roll", t, landingSide) * landingScale;
            } else landingTimer = 1.0f;
        }

        // 5.1 Falling Tension (Item Resistance)
        if (!onGround && velocity.y < -3.0f) {
            fallingTimer += deltaTime;
            float fallIntensity = Math.min(1.0f, (Math.abs(velocity.y) - 3.0f) / 25.0f);
            AnimationProfile fp = animationRegistry.get("falling");
            if (fp != null) {
                float normTime = (fallingTimer / fp.getDuration()) % 1.0f;
                tItY += fp.evaluate("item_y", normTime, 1.0f) * fallIntensity;
                tItZ += fp.evaluate("item_z", normTime, 1.0f) * fallIntensity;
                tItP += fp.evaluate("item_pitch", normTime, 1.0f) * fallIntensity;
                
                // Procedural Item-Only Shake (Wind on hands)
                float shakeFreq = 50.0f;
                float shake = (float) Math.sin(fallingTimer * shakeFreq) * 0.015f * fallIntensity;
                tItY += shake;
            }
        } else {
            fallingTimer = 0.0f;
        }

        // 5.2 Item Swing (Mining/Punching)
        if (swinging) {
            String sN = "item_swing";
            if (heldItem != null) sN = heldItem.getAnimation(sN);
            AnimationProfile swingAnim = animationRegistry.get(sN);
            if (swingAnim != null) {
                itemSwingTimer += deltaTime / swingAnim.getDuration();
                if (itemSwingTimer >= 1.0f) {
                    swinging = false;
                    itemSwingTimer = 0;
                } else {
                    tItX += swingAnim.evaluate("item_x", itemSwingTimer, 1.0f);
                    tItY += swingAnim.evaluate("item_y", itemSwingTimer, 1.0f);
                    tItZ += swingAnim.evaluate("item_z", itemSwingTimer, 1.0f);
                    tItP += swingAnim.evaluate("item_pitch", itemSwingTimer, 1.0f);
                    tItYw += swingAnim.evaluate("item_yaw", itemSwingTimer, 1.0f);
                }
            } else {
                swinging = false;
            }
        }

        // 6. Final Sync Apply
        float syncLerp = 12.0f; // Faster sync for sharper landings
        parkourCameraTilt += (targetTilt - parkourCameraTilt) * syncLerp * deltaTime;
        parkourCameraRoll += (targetRoll - parkourCameraRoll) * syncLerp * deltaTime;
        fovOffset += (targetFov - fovOffset) * 4.0f * deltaTime;
        cameraOffsetY += (tCamY - cameraOffsetY) * syncLerp * deltaTime;
        cameraOffsetX += (tCamX - cameraOffsetX) * syncLerp * deltaTime;

        itemOffsetX += (tItX + (leanAmount * 0.1f) - itemOffsetX) * syncLerp * deltaTime;
        itemOffsetY += (tItY - itemOffsetY) * syncLerp * deltaTime;
        itemOffsetZ += (tItZ - itemOffsetZ) * syncLerp * deltaTime;
        itemPitchOffset += (tItP - itemPitchOffset) * syncLerp * deltaTime;
        itemYawOffset += (tItYw - itemYawOffset) * syncLerp * deltaTime;
        itemRollOffset += (tItR + (leanAmount * 0.5f) - itemRollOffset) * syncLerp * deltaTime;
    }

    public com.za.minecraft.entities.parkour.ParkourHandler getParkourHandler() { return parkourHandler; }

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
