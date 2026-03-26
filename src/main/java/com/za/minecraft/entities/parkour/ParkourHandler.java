package com.za.minecraft.entities.parkour;

import com.za.minecraft.entities.Player;
import com.za.minecraft.world.World;
import com.za.minecraft.world.physics.PhysicsSettings;
import com.za.minecraft.world.physics.AABB;
import org.joml.Vector3f;

/**
 * Advanced parkour handler with smooth curves and camera restrictions.
 */
public class ParkourHandler {
    public enum ParkourState {
        NONE,
        GRABBING,
        HANGING,
        CLIMBING
    }

    private ParkourState state = ParkourState.NONE;
    private float transitionTimer = 0.0f;
    private float baseYaw = 0.0f;
    private float climbSide = 1.0f; // 1.0 = right, -1.0 = left
    
    private final Vector3f startTransitionPosition = new Vector3f();
    private final Vector3f hangingPosition = new Vector3f();
    private final Vector3f targetClimbPosition = new Vector3f();
    
    private static final float GRAB_TRANSITION_TIME = 0.2f;

    public void update(Player player, float deltaTime, World world) {
        PhysicsSettings settings = PhysicsSettings.getInstance();
        com.za.minecraft.entities.parkour.animation.AnimationProfile grabAnim = com.za.minecraft.entities.parkour.animation.AnimationRegistry.get("grabbing");
        com.za.minecraft.entities.parkour.animation.AnimationProfile climbAnim = com.za.minecraft.entities.parkour.animation.AnimationRegistry.get("climbing");

        if (state == ParkourState.GRABBING) {
            transitionTimer += deltaTime;
            float duration = (grabAnim != null) ? grabAnim.getDuration() : GRAB_TRANSITION_TIME;
            float t = Math.min(1.0f, transitionTimer / duration);
            
            // Interpolation from JSON
            String interp = (grabAnim != null) ? grabAnim.getPathInterpolation() : "smoothstep";
            float easedT = interpolate(t, interp);
            
            player.getPosition().set(new Vector3f(startTransitionPosition).lerp(hangingPosition, easedT));
            player.getVelocity().set(0, 0, 0);
            player.setFlying(true);

            if (t >= 1.0f) {
                state = ParkourState.HANGING;
            }
        } else if (state == ParkourState.HANGING) {
            player.getVelocity().set(0, 0, 0);
            player.getPosition().set(hangingPosition);
            player.setFlying(true);
        } else if (state == ParkourState.CLIMBING) {
            transitionTimer += deltaTime;
            float duration = settings.climbDuration;
            if (climbAnim != null && climbAnim.getDurationKey() != null) {
                // Future: could use reflection to get from settings, for now use settings.climbDuration
                duration = settings.climbDuration;
            }
            float t = Math.min(1.0f, transitionTimer / duration);

            // Path interpolation from JSON
            String interp = (climbAnim != null) ? climbAnim.getPathInterpolation() : "smootherstep";
            float smoothT = interpolate(t, interp);

            // Quadratic Bezier Path
            float p0x = startTransitionPosition.x;
            float p0y = startTransitionPosition.y;
            float p0z = startTransitionPosition.z;

            float apexOffset = (climbAnim != null) ? climbAnim.getApexYOffset() : 0.25f;
            float p1x = startTransitionPosition.x;
            float p1y = targetClimbPosition.y + apexOffset; 
            float p1z = startTransitionPosition.z;

            float p2x = targetClimbPosition.x;
            float p2y = targetClimbPosition.y;
            float p2z = targetClimbPosition.z;

            float invT = 1.0f - smoothT;
            player.getPosition().x = invT * invT * p0x + 2 * invT * smoothT * p1x + smoothT * smoothT * p2x;
            player.getPosition().y = invT * invT * p0y + 2 * invT * smoothT * p1y + smoothT * smoothT * p2y;
            player.getPosition().z = invT * invT * p0z + 2 * invT * smoothT * p1z + smoothT * smoothT * p2z;

            player.getVelocity().set(0, 0, 0);
            player.setFlying(true);

            if (t >= 1.0f) {
                player.getPosition().set(targetClimbPosition);
                setState(player, ParkourState.NONE);
            }
        }
    }

    private float interpolate(float t, String type) {
        return switch (type.toLowerCase()) {
            case "smoothstep" -> t * t * (3 - 2 * t);
            case "smootherstep" -> t * t * t * (t * (t * 6 - 15) + 10);
            case "sine" -> (float) Math.sin(t * Math.PI / 2.0);
            case "quad_in" -> t * t;
            case "quad_out" -> 1.0f - (1.0f - t) * (1.0f - t);
            default -> t;
        };
    }

    /**
     * Standard Cubic Bezier calculation for a single dimension (0 to 1).
     */
    private float getBezier(float t, float p1, float p2) {
        float invT = 1.0f - t;
        return 3.0f * invT * invT * t * p1 + 3.0f * invT * t * t * p2 + t * t * t;
    }

    public void tryLedgeGrab(Player player, World world, Vector3f lookDir) {
        if (state != ParkourState.NONE || player.isOnGround()) return;

        PhysicsSettings settings = PhysicsSettings.getInstance();
        Vector3f horizontalDir = new Vector3f(lookDir.x, 0, lookDir.z).normalize();
        Vector3f playerPos = player.getPosition();

        for (float h = settings.maxGrabHeight; h >= settings.minGrabHeight; h -= 0.1f) {
            Vector3f rayStart = new Vector3f(playerPos).add(0, h, 0);
            Vector3f rayEnd = new Vector3f(rayStart).add(new Vector3f(horizontalDir).mul(settings.grabDistance));

            int bx = (int) Math.floor(rayEnd.x);
            int by = (int) Math.floor(rayEnd.y);
            int bz = (int) Math.floor(rayEnd.z);

            if (world.getBlock(bx, by, bz).isAir() && world.getBlock(bx, by - 1, bz).isSolid()) {
                if (canClimbTo(player, world, rayEnd)) {
                    // Calculate correct distance to keep player hitbox outside the block
                    // blockCenter (0.5) + playerRadius (width/2) + small epsilon (0.05)
                    float offsetFromCenter = 0.5f + (settings.playerWidth / 2.0f) + 0.05f;

                    // Set up the hanging position
                    hangingPosition.set(playerPos.x, (float) by - 1.35f, playerPos.z);
                    
                    float blockCenterX = bx + 0.5f;
                    float blockCenterZ = bz + 0.5f;
                    
                    hangingPosition.x = blockCenterX - horizontalDir.x * offsetFromCenter;
                    hangingPosition.z = blockCenterZ - horizontalDir.z * offsetFromCenter;

                    startTransitionPosition.set(playerPos);
                    transitionTimer = 0;
                    state = ParkourState.GRABBING;
                    
                    // Store the yaw we are facing when grabbing to restrict camera later
                    baseYaw = (float) -Math.atan2(horizontalDir.x, -horizontalDir.z);
                    
                    com.za.minecraft.utils.Logger.info("Ledge found! Grabbing...");
                    return;
                }
            }
        }
    }

    private boolean canClimbTo(Player player, World world, Vector3f targetPos) {
        PhysicsSettings settings = PhysicsSettings.getInstance();
        float tx = (float) Math.floor(targetPos.x) + 0.5f;
        float ty = (float) Math.floor(targetPos.y);
        float tz = (float) Math.floor(targetPos.z) + 0.5f;

        AABB testBox = new AABB(
            -settings.playerWidth / 2, 0.05f, -settings.playerWidth / 2,
            settings.playerWidth / 2, settings.standingHeight, settings.playerWidth / 2
        ).offset(tx, ty, tz);

        int minX = (int) Math.floor(testBox.getMin().x);
        int maxX = (int) Math.floor(testBox.getMax().x);
        int minY = (int) Math.floor(testBox.getMin().y);
        int maxY = (int) Math.floor(testBox.getMax().y);
        int minZ = (int) Math.floor(testBox.getMin().z);
        int maxZ = (int) Math.floor(testBox.getMax().z);

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    if (world.getBlock(x, y, z).isSolid()) return false;
                }
            }
        }
        return true;
    }

    public void startClimb(Player player) {
        if (state != ParkourState.HANGING) return;

        PhysicsSettings settings = PhysicsSettings.getInstance();
        startTransitionPosition.set(player.getPosition());
        
        // Randomize leading hand for visual variety
        climbSide = Math.random() > 0.5 ? 1.0f : -1.0f;

        // Use the direction we are facing (the baseYaw) to move forward
        float forwardX = (float) Math.sin(-baseYaw);
        float forwardZ = -(float) Math.cos(-baseYaw);
        
        targetClimbPosition.set(
            (float) Math.floor(player.getPosition().x + forwardX * 0.8f) + 0.5f,
            (float) Math.floor(player.getPosition().y + 1.45f),
            (float) Math.floor(player.getPosition().z + forwardZ * 0.8f) + 0.5f
        );

        transitionTimer = 0;
        state = ParkourState.CLIMBING;
    }

    public float getClimbSide() {
        return climbSide;
    }

    public float getProgress() {
        com.za.minecraft.entities.parkour.animation.AnimationProfile grabAnim = com.za.minecraft.entities.parkour.animation.AnimationRegistry.get("grabbing");
        com.za.minecraft.entities.parkour.animation.AnimationProfile climbAnim = com.za.minecraft.entities.parkour.animation.AnimationRegistry.get("climbing");

        if (state == ParkourState.GRABBING) {
            float duration = (grabAnim != null) ? grabAnim.getDuration() : GRAB_TRANSITION_TIME;
            return Math.min(1.0f, transitionTimer / duration);
        }
        if (state == ParkourState.CLIMBING) {
            float duration = PhysicsSettings.getInstance().climbDuration;
            return Math.min(1.0f, transitionTimer / duration);
        }
        return 0.0f;
    }

    public boolean isRestrictingCamera() {
        return state == ParkourState.HANGING || state == ParkourState.GRABBING || state == ParkourState.CLIMBING;
    }

    public boolean isHanging() {
        return state == ParkourState.HANGING || state == ParkourState.GRABBING;
    }

    public boolean isClimbing() {
        return state == ParkourState.CLIMBING;
    }

    public boolean isInParkour() {
        return state != ParkourState.NONE;
    }

    public void cancel(Player player) {
        setState(player, ParkourState.NONE);
    }

    public float getBaseYaw() { return baseYaw; }

    public ParkourState getState() { return state; }

    public void setState(Player player, ParkourState newState) {
        this.state = newState;
        if (newState == ParkourState.NONE) {
            player.setFlying(player.getMode() == com.za.minecraft.engine.core.PlayerMode.DEVELOPER);
        }
    }
}
