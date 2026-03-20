package com.za.minecraft.entities;

import com.za.minecraft.entities.ai.AIState;
import com.za.minecraft.world.World;
import org.joml.Vector3f;
import java.util.Random;

/**
 * A basic infected scout. 
 * Reacts to player noise and chases them.
 */
public class ScoutEntity extends LivingEntity {
    private static final float SCOUT_WIDTH = 0.6f;
    private static final float SCOUT_HEIGHT = 1.8f;
    private static final float WANDER_SPEED = 2.0f;
    private static final float CHASE_SPEED = 4.5f;
    private static final float HEARING_RADIUS = 32.0f;
    private static final float DETECTION_THRESHOLD = 0.15f; // Player noise needed to detect at distance

    private AIState currentState = AIState.WANDER;
    private final Vector3f targetLocation = new Vector3f();
    private final Random random = new Random();
    private float stateTimer = 0;
    
    public ScoutEntity(Vector3f position) {
        super(position, SCOUT_WIDTH, SCOUT_HEIGHT, 15.0f);
    }

    @Override
    public void update(float deltaTime, World world) {
        // AI Logic
        updateAI(deltaTime, world);
        
        // Physics and Movement
        super.update(deltaTime, world);
    }

    private void updateAI(float deltaTime, World world) {
        Player player = world.getPlayer();
        if (player == null) return;

        float distToPlayer = position.distance(player.getPosition());
        stateTimer -= deltaTime;

        // 1. Perception: Hearing
        float perceivedNoise = world.getNoiseLevelAt(position);
        if (perceivedNoise > DETECTION_THRESHOLD) {
            if (currentState != AIState.CHASE) {
                currentState = AIState.SEARCH;
                // If player is making noise, head towards them. 
                // Otherwise, the entity will naturally be drawn to the area of noise.
                if (player.getNoiseLevel() > 0.1f) {
                    targetLocation.set(player.getPosition());
                } else {
                    // Head towards the general direction of noise if player is quiet but something else is noisy
                    // For now, let's just use the player as a target if they are within range,
                    // or we could iterate to find the actual noise source.
                    // To keep it simple: if player isn't noisy, scout heads towards a random point near its current position 
                    // that might be the source, or we can just stick to player if they are the most likely cause.
                    targetLocation.set(player.getPosition()); 
                }
                stateTimer = 5.0f; // Search for 5 seconds
            }
        }

        // 2. Perception: Visual (simple distance-based for now)
        float visibilityRange = player.isSneaking() ? 4.0f : 14.0f;
        if (distToPlayer < visibilityRange) {
            currentState = AIState.CHASE;
        }

        // 3. State Actions
        switch (currentState) {
            case WANDER:
                if (stateTimer <= 0) {
                    float rx = (random.nextFloat() - 0.5f) * 20.0f;
                    float rz = (random.nextFloat() - 0.5f) * 20.0f;
                    targetLocation.set(position.x + rx, position.y, position.z + rz);
                    stateTimer = 3.0f + random.nextFloat() * 5.0f;
                }
                moveToTarget(WANDER_SPEED);
                break;

            case SEARCH:
                moveToTarget(WANDER_SPEED);
                // Return to wander if searched long enough or reached location
                if (stateTimer <= 0) {
                    currentState = AIState.WANDER;
                    stateTimer = 2.0f;
                }
                break;

            case CHASE:
                targetLocation.set(player.getPosition());
                moveToTarget(CHASE_SPEED);
                
                // Lose interest if player is too far or hidden
                boolean tooFar = distToPlayer > HEARING_RADIUS;
                boolean lostSight = player.isSneaking() && distToPlayer > 6.0f;
                
                if (tooFar || lostSight) {
                    currentState = AIState.SEARCH;
                    stateTimer = 7.0f; // Look around for 7 seconds
                }
                break;
                
            case IDLE:
                velocity.x = 0;
                velocity.z = 0;
                if (stateTimer <= 0) currentState = AIState.WANDER;
                break;
        }
    }

    private void moveToTarget(float speed) {
        Vector3f dir = new Vector3f(targetLocation).sub(position);
        dir.y = 0; // Only horizontal movement
        if (dir.lengthSquared() > 0.01f) {
            dir.normalize().mul(speed);
            velocity.x = dir.x;
            velocity.z = dir.z;
            
            // Set rotation to face direction of movement
            rotation.y = (float) Math.atan2(dir.x, dir.z);
        } else {
            velocity.x = 0;
            velocity.z = 0;
        }
    }

    public AIState getCurrentState() {
        return currentState;
    }
}
