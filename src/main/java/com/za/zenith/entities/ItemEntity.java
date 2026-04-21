package com.za.zenith.entities;

import com.za.zenith.world.World;
import com.za.zenith.world.items.ItemStack;
import com.za.zenith.entities.inventory.Slot;
import com.za.zenith.entities.inventory.SlotGroup;
import org.joml.Vector3f;

/**
 * Entity representing an item dropped in the world.
 */
public class ItemEntity extends Entity {
    private final ItemStack stack;
    private float age;
    private float pickupDelay = 1.0f; // 1 second before it can be picked up
    private final Vector3f angularVelocity = new Vector3f();
    private boolean isBeingAttracted = false;
    private boolean isLockedOnPlayer = false; // "Мертвая хватка"
    private boolean isSleeping = false;
    private float sleepTimer = 0;
    
    public ItemEntity(Vector3f position, ItemStack stack) {
        super(position, 0.25f, 0.25f);
        this.stack = stack;
        this.age = 0;
    }
    
    public void setAngularVelocity(Vector3f angVel) {
        this.angularVelocity.set(angVel);
    }
    
    @Override
    public void update(float deltaTime, World world) {
        // Сохраняем состояние начала тика
        prevPosition.set(position);
        prevRotation.set(rotation);
        
        age += deltaTime;
        if (pickupDelay > 0) pickupDelay -= deltaTime;

        Player player = world.getPlayer();
        
        // 1. MAGNETIC OPTIMIZATION
        if (canBePickedUp() && player != null && !player.getInventory().isFull()) {
            Vector3f playerCenter = new Vector3f(player.getPosition());
            playerCenter.y += player.getHeight() * 0.5f;
            float distSq = position.distanceSquared(playerCenter);

            if (isLockedOnPlayer || distSq < 100.0f) { // Check only within 10 blocks
                com.za.zenith.world.items.component.MagneticComponent magnet = player.getInventory().getActiveComponent(com.za.zenith.world.items.component.MagneticComponent.class);
                
                if (magnet != null && (distSq < magnet.attractionRadius * magnet.attractionRadius || isLockedOnPlayer)) {
                    isBeingAttracted = true;
                    isLockedOnPlayer = true;
                    isSleeping = false; 
                    
                    float distance = (float)Math.sqrt(distSq);
                    Vector3f direction = new Vector3f(playerCenter).sub(position).normalize();
                    float approachSpeed = 12.0f + (1.0f - Math.min(1.0f, distance / 4.0f)) * (magnet.attractionForce * 0.2f);
                    
                    velocity.set(player.getVelocity());
                    velocity.add(direction.mul(approachSpeed));
                    onGround = false; 
                } else {
                    isBeingAttracted = false;
                    isLockedOnPlayer = false;
                }
            }
        }

        // 2. PHYSICS SLEEPING
        if (onGround && velocity.lengthSquared() < 0.01f && !isBeingAttracted) {
            // Fast support check: if block below is air, wake up immediately
            if (world.getBlock((int)Math.floor(position.x), (int)Math.floor(position.y - 0.01f), (int)Math.floor(position.z)).isAir()) {
                isSleeping = false;
                onGround = false;
                sleepTimer = 0;
            } else {
                sleepTimer += deltaTime;
                if (sleepTimer > 1.0f) isSleeping = true;
            }
        } else {
            isSleeping = false;
            sleepTimer = 0;
        }

        if (isSleeping) {
            velocity.set(0, 0, 0);
            return;
        }

        float gravityMultiplier = stack.getItem().getWeight();
        if (!flying && !isBeingAttracted) {
            velocity.y = Math.max(velocity.y + GRAVITY * gravityMultiplier * deltaTime, TERMINAL_VELOCITY);
        }
        
        move(world, velocity.x * deltaTime, velocity.y * deltaTime, velocity.z * deltaTime);

        float friction = isBeingAttracted ? 1.0f : (onGround ? 0.85f : 0.98f);
        velocity.mul(friction);
        
        if (onGround && !isBeingAttracted) {
            rotation.x = lerpAngle(rotation.x, 0, deltaTime * 5.0f);
            rotation.z = lerpAngle(rotation.z, 0, deltaTime * 5.0f);
            angularVelocity.mul(0.85f);
        } else if (isBeingAttracted) {
            angularVelocity.y += deltaTime * 20.0f;
            angularVelocity.x += deltaTime * 5.0f;
        }
        
        rotation.add(angularVelocity.x * deltaTime, angularVelocity.y * deltaTime, angularVelocity.z * deltaTime);
    }

    private float lerpAngle(float start, float end, float t) {
        float diff = end - start;
        while (diff < -Math.PI) diff += Math.PI * 2;
        while (diff > Math.PI) diff -= Math.PI * 2;
        return start + diff * t;
    }
    
    public ItemStack getStack() {
        return stack;
    }
    
    public boolean canBePickedUp() {
        return pickupDelay <= 0;
    }
    
    public float getAge() {
        return age;
    }

    public boolean isBeingAttracted() {
        return isBeingAttracted;
    }
}


