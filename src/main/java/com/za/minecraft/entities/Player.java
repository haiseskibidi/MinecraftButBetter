package com.za.minecraft.entities;

import com.za.minecraft.engine.core.PlayerMode;
import com.za.minecraft.world.World;
import com.za.minecraft.entities.parkour.animation.AnimationRegistry;
import com.za.minecraft.entities.parkour.animation.AnimationProfile;
import com.za.minecraft.world.items.ItemStack;
import org.joml.Vector3f;

/**
 * AAA Player Entity.
 * Optimized version with physical viewmodel and clean animation logic.
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

    // Body Condition (Hand State)
    private float dirt = 0.0f;
    private float blood = 0.0f;
    private float wetness = 0.0f;
    private boolean hasParasites = false;
    private float parasitesTimer = 0.0f;
    
    // Animation State
    private float currentEyeHeight;
    private float parkourCameraTilt = 0.0f;
    private float parkourCameraRoll = 0.0f;
    private float fovOffset = 0.0f;
    private float cameraOffsetX = 0.0f;
    private float cameraOffsetY = 0.0f;
    private float lastYaw = 0.0f;
    private float lastPitch = 0.0f;
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
    private float itemSwingDuration = 0.5f;
    
    // Viewmodel Physics
    private com.za.minecraft.engine.graphics.model.Viewmodel viewmodel;
    private final com.za.minecraft.engine.graphics.model.ViewmodelController viewmodelController = new com.za.minecraft.engine.graphics.model.ViewmodelController();
    private final com.za.minecraft.engine.graphics.model.ViewmodelPhysics mainHandPhys = new com.za.minecraft.engine.graphics.model.ViewmodelPhysics();
    private float lerpedWeight = 0.2f;
    private boolean physicsInitialized = false;

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

    // Transitions
    private float parkourWeight = 0.0f; 
    private float moveLatchTimer = 0.0f;
    private static final float LATCH_DURATION = 0.15f; 

    public Player(Vector3f startPosition) {
        super(startPosition, 
              com.za.minecraft.world.physics.PhysicsSettings.getInstance().playerWidth, 
              com.za.minecraft.world.physics.PhysicsSettings.getInstance().standingHeight, 
              20.0f);
        this.inventory = new Inventory();
        
        com.za.minecraft.world.physics.PhysicsSettings settings = com.za.minecraft.world.physics.PhysicsSettings.getInstance();
        this.currentEyeHeight = settings.standingEyeHeight;
        this.wasOnGround = true; 

        com.za.minecraft.engine.graphics.model.ViewmodelDefinition handsDef = 
            com.za.minecraft.engine.graphics.model.ModelRegistry.getViewmodel(com.za.minecraft.utils.Identifier.of("minecraft:hands"));
        if (handsDef != null) {
            this.viewmodel = new com.za.minecraft.engine.graphics.model.Viewmodel(handsDef);
        }
    }
    
    public com.za.minecraft.engine.graphics.model.Viewmodel getViewmodel() { return viewmodel; }
    
    @Override
    public void update(float deltaTime, World world) {
        inventory.update(world, this, com.za.minecraft.engine.core.GameLoop.getInstance().getCamera());
        updateHunger(deltaTime);
        updateNoise(deltaTime);
        updateSneakState(world, deltaTime);
        parkourHandler.update(this, deltaTime, world);
        updateThermalAndConditions(deltaTime, world);
        
        boolean isMovingPhysically = onGround && moving && velocity.lengthSquared() > 0.0001f;
        if (isMovingPhysically) moveLatchTimer = LATCH_DURATION;
        else moveLatchTimer = Math.max(0, moveLatchTimer - deltaTime);
        
        float alphaTarget = (moveLatchTimer > 0) ? 1.0f : 0.0f;
        movementAlpha += (alphaTarget - movementAlpha) * (sneaking ? 4.0f : 8.0f) * deltaTime;

        String cN = sneaking ? "sneak" : (sprinting ? "sprint" : "walk");
        AnimationProfile cp = animationRegistry.get(cN);
        AnimationProfile cip = animationRegistry.get(sneaking ? "sneak_idle" : "idle");
        
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
        
        preUpdateVelocityY = velocity.y;
        super.update(deltaTime, world);
    }

    public void updateAnimations(float deltaTime, World world) {
        if (isFirstFrame) {
            lastYaw = getRotation().y;
            lastPitch = getRotation().x;
            wasOnGround = onGround;
            isFirstFrame = false;
            return;
        }

        // 1. Mouse Delta & Leaning
        float currentYaw = getRotation().y;
        float yawDelta = currentYaw - lastYaw;
        while (yawDelta < -Math.PI) yawDelta += Math.PI * 2;
        while (yawDelta > Math.PI) yawDelta -= Math.PI * 2;
        if (Math.abs(yawDelta) < 0.0001f) yawDelta = 0;
        float leanTarget = -yawDelta * 0.8f; 
        if (sprinting) leanTarget *= 1.2f;
        leanAmount += (leanTarget - leanAmount) * (sneaking ? 3.0f : 7.0f) * deltaTime;
        lastYaw = currentYaw;

        // 2. Impulse Trigger: Landing
        if (onGround && !wasOnGround && preUpdateVelocityY < -2.0f) {
            landingTimer = 0.0f;
            landingSide = Math.random() > 0.5 ? 1.0f : -1.0f;
            float v = Math.abs(preUpdateVelocityY);
            landingScale = Math.min(2.2f, (v * v) / 250.0f + (v * 0.02f)); 
        }
        wasOnGround = onGround;

        // 3. Profiles & Evaluation
        com.za.minecraft.world.items.Item heldItem = inventory.getSelectedItem();
        String cN = sneaking ? "sneak" : (sprinting ? "sprint" : "walk");
        String iN = heldItem != null ? heldItem.getAnimation(sneaking ? "item_sneak" : (sprinting ? "item_sprint" : "item_walk")) : (sneaking ? "item_sneak" : (sprinting ? "item_sprint" : "item_walk"));
        String iiN = heldItem != null ? heldItem.getAnimation("item_idle") : "item_idle";

        AnimationProfile cp = animationRegistry.get(cN);
        AnimationProfile ip = animationRegistry.get(iN);
        AnimationProfile cip = animationRegistry.get(sneaking ? "sneak_idle" : "idle");
        AnimationProfile iip = animationRegistry.get(iiN);

        float horizontalSpeed = (float) Math.sqrt(velocity.x * velocity.x + velocity.z * velocity.z);
        float maxSneakSpeed = com.za.minecraft.world.physics.PhysicsSettings.getInstance().baseMoveSpeed * 0.35f;
        float speedIntensity = Math.min(1.0f, horizontalSpeed / maxSneakSpeed);

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
        float iCamX = (cip != null) ? cip.evaluate("camera_x", locomotionTimer, 1.0f) : 0;
        float iItX = (iip != null) ? iip.evaluate("item_x", sneaking ? 0.0f : locomotionTimer, 1.0f) : 0;
        float iItY = (iip != null) ? iip.evaluate("item_y", locomotionTimer, 1.0f) : 0;
        float iItZ = (iip != null) ? iip.evaluate("item_z", locomotionTimer, 1.0f) : 0;
        float iItP = (iip != null) ? iip.evaluate("item_pitch", locomotionTimer, 1.0f) : 0;
        float iItR = (iip != null) ? iip.evaluate("item_roll", sneaking ? 0.0f : locomotionTimer, 1.0f) : 0;

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
            tItYw = (leanAmount * 0.2f);
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

        // 4. Parkour blending
        float parkourTarget = parkourHandler.isInParkour() ? 1.0f : 0.0f;
        parkourWeight += (parkourTarget - parkourWeight) * 18.0f * deltaTime;
        if (parkourWeight > 0.001f) {
            String pN = (parkourHandler.getState() == com.za.minecraft.entities.parkour.ParkourHandler.ParkourState.CLIMBING) ? "climbing" : "grabbing";
            AnimationProfile pp = animationRegistry.get(pN);
            if (pp != null) {
                float pT = parkourHandler.getProgress();
                float pSide = parkourHandler.getClimbSide();
                targetTilt = targetTilt + (pp.evaluate("camera_tilt", pT, pSide) - targetTilt) * parkourWeight;
                targetRoll = targetRoll + (pp.evaluate("camera_roll", pT, pSide) - targetRoll) * parkourWeight;
                tCamY = tCamY + (pp.evaluate("camera_y", pT, pSide) - tCamY) * parkourWeight;
                tCamX = tCamX + (pp.evaluate("camera_x", pT, pSide) - tCamX) * parkourWeight;
                targetFov = targetFov + (pp.evaluate("fov_offset", pT, pSide) - targetFov) * parkourWeight;
                tItX = tItX + (pp.evaluate("item_x", pT, pSide) - tItX) * parkourWeight;
                tItY = tItY + (pp.evaluate("item_y", pT, pSide) - tItY) * parkourWeight;
                tItZ = tItZ + (pp.evaluate("item_z", pT, pSide) - tItZ) * parkourWeight;
                tItP = tItP + (pp.evaluate("item_pitch", pT, pSide) - tItP) * parkourWeight;
                tItYw = tItYw + (pp.evaluate("item_yaw", pT, pSide) - tItYw) * parkourWeight;
                tItR = tItR + (pp.evaluate("item_roll", pT, pSide) - tItR) * parkourWeight;
            }
        }

        // 5. Impulse Apply
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

        float swingPosX = 0f, swingPosY = 0f, swingPosZ = 0f;
        float swingPitch = 0f, swingYaw = 0f, swingRoll = 0f;

        if (swinging) {
            String sN = heldItem != null ? heldItem.getAnimation("item_swing") : "hand_swing";
            AnimationProfile swingAnim = animationRegistry.get(sN);
            if (swingAnim != null) {
                itemSwingTimer += deltaTime / itemSwingDuration; 
                if (itemSwingTimer >= 1.0f) { swinging = false; itemSwingTimer = 0; }
                else {
                    // Positions in JSON are in voxels (16 per block), convert to meters
                    swingPosX = swingAnim.evaluate("item_x", itemSwingTimer, 1.0f) / 16.0f;
                    swingPosY = swingAnim.evaluate("item_y", itemSwingTimer, 1.0f) / 16.0f;
                    swingPosZ = swingAnim.evaluate("item_z", itemSwingTimer, 1.0f) / 16.0f;
                    
                    // Rotations in JSON are in degrees, convert to radians
                    swingPitch = (float)Math.toRadians(swingAnim.evaluate("item_pitch", itemSwingTimer, 1.0f));
                    swingYaw = (float)Math.toRadians(swingAnim.evaluate("item_yaw", itemSwingTimer, 1.0f));
                    swingRoll = (float)Math.toRadians(swingAnim.evaluate("item_roll", itemSwingTimer, 1.0f));
                    
                    // Add camera impact from swing
                    targetTilt += swingAnim.evaluate("camera_tilt", itemSwingTimer, 1.0f);
                    targetRoll += swingAnim.evaluate("camera_roll", itemSwingTimer, 1.0f);
                }
            } else swinging = false;
        }

        // 6. Final Sync
        float syncLerp = 12.0f;
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

        // 7. Physical Simulation
        if (viewmodel != null) {
            viewmodelController.resetAnimation(viewmodel);
            viewmodelController.applyAnimation(viewmodel, cip, sneaking ? 0.0f : locomotionTimer, (1.0f - movementAlpha));
            viewmodelController.applyAnimation(viewmodel, cp, locomotionTimer, movementAlpha);
            viewmodelController.applyAnimation(viewmodel, iip, locomotionTimer, (1.0f - movementAlpha));
            viewmodelController.applyAnimation(viewmodel, ip, locomotionTimer, movementAlpha);
            
            if (parkourWeight > 0.001f) {
                String pN = (parkourHandler.getState() == com.za.minecraft.entities.parkour.ParkourHandler.ParkourState.CLIMBING) ? "climbing" : "grabbing";
                AnimationProfile pp = animationRegistry.get(pN);
                if (pp != null) viewmodelController.applyAnimation(viewmodel, pp, parkourHandler.getProgress(), parkourWeight);
            }
            if (landingTimer < 1.0f) {
                AnimationProfile lp = animationRegistry.get("landing");
                if (lp != null) viewmodelController.applyAnimation(viewmodel, lp, Math.min(1.0f, landingTimer), landingScale);
            }
            if (swinging) {
                String sN = heldItem != null ? heldItem.getAnimation("item_swing") : "item_swing";
                AnimationProfile swingAnim = animationRegistry.get(sN);
                if (swingAnim != null) viewmodelController.applyAnimation(viewmodel, swingAnim, itemSwingTimer, 1.0f);
            }

            com.za.minecraft.engine.graphics.model.ModelNode sh = viewmodel.getNode("shoulder_r");
            com.za.minecraft.engine.graphics.model.ModelNode fo = viewmodel.getNode("forearm_r");
            com.za.minecraft.engine.graphics.model.ModelNode hand = viewmodel.getNode("hand_r");
            
            if (sh != null && fo != null && hand != null) {
                Vector3f camForward = com.za.minecraft.engine.core.GameLoop.getInstance().getCamera().getDirection();
                if (!physicsInitialized) {
                    mainHandPhys.reset(new Vector3f(itemOffsetX, itemOffsetY, itemOffsetZ), 
                                       new org.joml.Quaternionf().rotateX(itemPitchOffset).rotateY(itemYawOffset).rotateZ(itemRollOffset));
                    physicsInitialized = true;
                }

                float currentPitch = com.za.minecraft.engine.core.GameLoop.getInstance().getCamera().getRotation().x;
                float pDelta = currentPitch - lastPitch; lastPitch = currentPitch;
                Vector3f extF = new Vector3f(-yawDelta * 60.0f, pDelta * 60.0f, 0);

                Vector3f tPos = new Vector3f(itemOffsetX, itemOffsetY, itemOffsetZ);

                org.joml.Quaternionf tRot = new org.joml.Quaternionf().rotateX(itemPitchOffset).rotateY(itemYawOffset).rotateZ(itemRollOffset);

                // --- 7.1 WORLD COLLISIONS (Tarkov-style) ---
                float reach = 0.6f; 
                if (heldItem != null) reach += heldItem.getVisualScale() * 0.4f;
                Vector3f eyePos = new Vector3f(position).add(0, getEyeHeight(), 0);
                com.za.minecraft.world.physics.RaycastResult hit = com.za.minecraft.world.physics.Raycast.raycast(world, eyePos, camForward);
                Vector3f probePoint = new Vector3f(eyePos).fma(reach * 0.8f, camForward);
                com.za.minecraft.world.blocks.Block probedBlock = world.getBlock((int)Math.floor(probePoint.x), (int)Math.floor(probePoint.y), (int)Math.floor(probePoint.z));
                boolean colliding = (hit != null && hit.isHit() && hit.getDistance() < reach) || !probedBlock.isAir();
                
                if (colliding) {
                    float dist = (hit != null && hit.isHit()) ? hit.getDistance() : reach * 0.5f;
                    float factor = (reach - dist) / reach;
                    float pf = Math.clamp(factor, 0.0f, 1.0f);
                    tPos.z += 0.8f * pf; tPos.x += 0.15f * pf;
                    tRot.rotateX((float)Math.toRadians(-75.0f * pf));
                    tRot.rotateZ((float)Math.toRadians(15.0f * pf));
                    extF.z += 60.0f * pf;
                }

                float tW = (heldItem != null) ? heldItem.getWeight() : 0.2f;
                lerpedWeight += (tW - lerpedWeight) * 5.0f * deltaTime;
                sh.animTranslation.y -= lerpedWeight * 0.05f; 
                
                mainHandPhys.update(deltaTime, tPos, tRot, lerpedWeight, extF);
                
                Vector3f a = mainHandPhys.currentRot.getEulerAnglesXYZ(new Vector3f());
                sh.animRotation.set(a.x * 0.1f, a.y * 0.1f, a.z * 0.1f);
                fo.animRotation.set(a.x * 0.2f, a.y * 0.2f, a.z * 0.2f);
                hand.animRotation.set(a.x * 0.7f, a.y * 0.7f, a.z * 0.7f);
                sh.animRotation.x += lerpedWeight * 0.05f;

                // Apply swing rotations dynamically across the arm joints (more shoulder/forearm for weight)
                sh.animRotation.add(swingPitch * 0.25f, swingYaw * 0.25f, swingRoll * 0.25f);
                fo.animRotation.add(swingPitch * 0.35f, swingYaw * 0.35f, swingRoll * 0.35f);
                hand.animRotation.add(swingPitch * 0.4f, swingYaw * 0.4f, swingRoll * 0.4f);

                Vector3f finalPos = new Vector3f(mainHandPhys.currentPos).add(swingPosX, swingPosY, swingPosZ);
                viewmodel.updateHierarchy(new org.joml.Matrix4f().identity().translate(finalPos));
            }
        }
    }

    public com.za.minecraft.entities.parkour.ParkourHandler getParkourHandler() { return parkourHandler; }

    private void updateSneakState(World world, float deltaTime) {
        com.za.minecraft.world.physics.PhysicsSettings settings = com.za.minecraft.world.physics.PhysicsSettings.getInstance();
        float targetEyeHeight = sneaking ? settings.sneakingEyeHeight : settings.standingEyeHeight;
        if (!sneaking && boundingBox.getMax().y < settings.standingHeight) {
            if (canStandUp(world)) setBoundingBox(settings.playerWidth, settings.standingHeight);
            else targetEyeHeight = settings.sneakingEyeHeight;
        } else if (sneaking && boundingBox.getMax().y > settings.sneakingHeight) {
            setBoundingBox(settings.playerWidth, settings.sneakingHeight);
        }
        currentEyeHeight += (targetEyeHeight - currentEyeHeight) * 10.0f * deltaTime;
    }

    private boolean canStandUp(World world) {
        com.za.minecraft.world.physics.PhysicsSettings settings = com.za.minecraft.world.physics.PhysicsSettings.getInstance();
        com.za.minecraft.world.physics.AABB standingBox = new com.za.minecraft.world.physics.AABB(-settings.playerWidth / 2, 0, -settings.playerWidth / 2, settings.playerWidth / 2, settings.standingHeight, settings.playerWidth / 2).offset(position);
        for (int x = (int)Math.floor(standingBox.getMin().x); x <= (int)Math.floor(standingBox.getMax().x); x++) {
            for (int y = (int)Math.floor(standingBox.getMin().y); y <= (int)Math.floor(standingBox.getMax().y); y++) {
                for (int z = (int)Math.floor(standingBox.getMin().z); z <= (int)Math.floor(standingBox.getMax().z); z++) {
                    com.za.minecraft.world.blocks.Block b = world.getBlock(x, y, z);
                    if (!b.isAir() && b.isSolid()) return false;
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
    public float getCameraOffsetZ() { return 0.0f; } 
    public float getItemOffsetX() { return itemOffsetX; }
    public float getItemOffsetY() { return itemOffsetY; }
    public float getItemOffsetZ() { return itemOffsetZ; }
    public float getItemPitchOffset() { return itemPitchOffset; }
    public float getItemYawOffset() { return itemYawOffset; }
    public float getItemRollOffset() { return itemRollOffset; }
    
    public void swing() { swing(0.35f); }
    public void swing(float duration) { if (!swinging) { swinging = true; itemSwingTimer = 0; itemSwingDuration = duration; } }
    public boolean isMoving() { return moving; }

    public boolean isInWater() {
        com.za.minecraft.world.blocks.Block b = com.za.minecraft.engine.core.GameLoop.getInstance().getWorld().getBlock((int)Math.floor(position.x), (int)Math.floor(position.y + 0.5f), (int)Math.floor(position.z));
        return com.za.minecraft.world.blocks.BlockRegistry.getBlock(b.getType()).getIdentifier().getPath().contains("water");
    }

    public boolean isInRain() {
        return false;
    }

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
    public float getMiningSpeedMultiplier() { return 1.0f; }
    private void updateThermalAndConditions(float deltaTime, World world) {
        ItemStack held = inventory.getSelectedItemStack();
        if (held != null) {
            float ambient = 20.0f; 
            if (isInWater()) ambient = 15.0f;
            held.updateTemperature(ambient, deltaTime);
            com.za.minecraft.world.items.component.ThermalComponent thermal = held.getItem().getComponent(com.za.minecraft.world.items.component.ThermalComponent.class);
            float threshold = (thermal != null) ? thermal.burnThreshold() : 55.0f;
            if (held.getTemperature() > threshold) {
                takeDamage(0.5f * deltaTime);
                if (Math.random() < 0.05f * deltaTime) {
                    inventory.dropSelected(this, world, com.za.minecraft.engine.core.GameLoop.getInstance().getCamera(), true);
                }
            }
        }
        if (isInWater()) {
            wetness = 1.0f;
            dirt = Math.max(0, dirt - 2.0f * deltaTime);
            blood = Math.max(0, blood - 1.0f * deltaTime);
        } else {
            wetness = Math.max(0, wetness - 0.1f * deltaTime);
        }
        if (parasitesTimer > 0) {
            parasitesTimer -= deltaTime;
            if (parasitesTimer <= 0) hasParasites = false;
        }
    }

    public void addDirt(float amount) { this.dirt = Math.min(1.0f, this.dirt + amount); }
    public void addBlood(float amount) { this.blood = Math.min(1.0f, this.blood + amount); }
    public void washHands() { this.dirt = 0; this.blood = 0; this.wetness = 1.0f; }
    public float getDirt() { return dirt; }
    public float getBlood() { return blood; }
    public float getWetness() { return wetness; }
    public float getScentLevel() { return blood * 2.0f; }

    public void updateHunger(float deltaTime) {
        float mult = sprinting ? 3.0f : (!onGround && !flying ? 2.0f : 1.0f);
        if (hasParasites) mult *= 2.0f;
        if (saturation > 0) saturation -= 0.1f * mult * deltaTime;
        else hunger = Math.max(0, hunger - 0.1f * mult * deltaTime);
    }

    public void eat(com.za.minecraft.world.items.Item item) {
        com.za.minecraft.world.items.component.FoodComponent food = item.getComponent(com.za.minecraft.world.items.component.FoodComponent.class);
        if (food != null && hunger < 20.0f) {
            hunger = Math.min(20.0f, hunger + food.nutrition());
            saturation = Math.min(20.0f, saturation + food.saturationBonus());
            if (dirt > 0.5f && Math.random() < 0.3f) {
                hasParasites = true;
                parasitesTimer = 600.0f; 
            }
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
    public com.za.minecraft.engine.core.PlayerMode getMode() { return mode; }
    public void setMode(com.za.minecraft.engine.core.PlayerMode mode) { this.mode = mode; }
}
