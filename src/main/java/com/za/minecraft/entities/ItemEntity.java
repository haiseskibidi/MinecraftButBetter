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
    private final Vector3f angularVelocity = new Vector3f();
    
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
        float gravityMultiplier = stack.getItem().getWeight();
        
        if (!flying) {
            velocity.y = Math.max(velocity.y + GRAVITY * gravityMultiplier * deltaTime, TERMINAL_VELOCITY);
        }
        
        move(world, velocity.x * deltaTime, velocity.y * deltaTime, velocity.z * deltaTime);

        age += deltaTime;
        if (pickupDelay > 0) {
            pickupDelay -= deltaTime;
        }
        
        // Трение (Friction)
        float friction;
        if (onGround) {
            // Универсальная формула: чем тяжелее предмет, тем быстрее он останавливается (меньше коэффициент)
            // Вес 0.1 (волокно) -> 0.95 (долго скользит)
            // Вес 1.0 (обычный предмет) -> 0.80 (стандарт)
            // Вес 2.5 (тяжелый блок) -> 0.50 (быстро останавливается)
            float weightFactor = stack.getItem().getWeight();
            friction = 0.98f - (weightFactor * 0.2f);
            
            // Защита: трение не может быть меньше 0.1 (мгновенный стоп) и больше 0.98 (вечный полет)
            friction = Math.max(0.1f, Math.min(0.98f, friction));
            
            // Замедляем вращение на земле
            angularVelocity.mul(0.9f);
        } else {
            // В воздухе трение почти отсутствует
            friction = 0.98f;
        }
        
        velocity.x *= friction;
        velocity.z *= friction;
        
        // Rotation for visual tumbling effect
        rotation.add(
            angularVelocity.x * deltaTime,
            angularVelocity.y * deltaTime,
            angularVelocity.z * deltaTime
        );
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
