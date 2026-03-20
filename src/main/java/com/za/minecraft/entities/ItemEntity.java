package com.za.minecraft.entities;

import com.za.minecraft.world.World;
import com.za.minecraft.world.items.ItemStack;
import org.joml.Vector3f;

/**
 * Entity representing an item dropped in the world.
 */
public class ItemEntity extends Entity {
    private final ItemStack stack;
    private float age;
    private float pickupDelay = 1.0f; // 1 second before it can be picked up
    
    public ItemEntity(Vector3f position, ItemStack stack) {
        super(position, 0.25f, 0.25f);
        this.stack = stack;
        this.age = 0;
    }
    
    @Override
    public void update(float deltaTime, World world) {
        float gravityMultiplier = stack.getItem().getWeight();
        
        if (!flying) {
            velocity.y = Math.max(velocity.y + GRAVITY * gravityMultiplier * deltaTime, TERMINAL_VELOCITY);
        }
        
        move(world, velocity.x * deltaTime, velocity.y * deltaTime, velocity.z * deltaTime);

        age += deltaTime;
        if (pickupDelay > 0) {
            pickupDelay -= deltaTime;
        }
        
        // Air friction
        float friction = onGround ? 0.85f : 0.98f;
        
        // Heavy items have more momentum but also more ground friction
        if (onGround) {
            friction = 0.70f / stack.getItem().getWeight(); // Heavier = stops faster on ground
        }
        
        velocity.x *= friction;
        velocity.z *= friction;
        
        // Rotation for visual effect
        rotation.y += (4.0f / stack.getItem().getWeight()) * deltaTime; // Heavier = rotates slower
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
}
