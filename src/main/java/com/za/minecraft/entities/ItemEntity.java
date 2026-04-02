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
        // Сохраняем состояние начала тика
        prevPosition.set(position);
        prevRotation.set(rotation);
        
        float gravityMultiplier = stack.getItem().getWeight();
        
        // Custom gravity for items (based on weight)
        if (!flying && !onGround) {
            velocity.y = Math.max(velocity.y + GRAVITY * gravityMultiplier * deltaTime, TERMINAL_VELOCITY);
        } else if (onGround) {
            velocity.y = 0; // Ground lock
        }
        
        // Мы вызываем move напрямую, минуя Entity.update(), так как нам нужна кастомная гравитация
        move(world, velocity.x * deltaTime, velocity.y * deltaTime, velocity.z * deltaTime);

        // Snap very small residual velocities to zero
        if (Math.abs(velocity.x) < 0.005f) velocity.x = 0f;
        if (Math.abs(velocity.z) < 0.005f) velocity.z = 0f;

        age += deltaTime;
        if (pickupDelay > 0) {
            pickupDelay -= deltaTime;
        }
        
        // Трение (Friction)
        float friction;
        if (onGround) {
            float weightFactor = stack.getItem().getWeight();
            friction = 0.98f - (weightFactor * 0.2f);
            friction = Math.max(0.1f, Math.min(0.98f, friction));
            angularVelocity.mul(0.9f);
        } else {
            friction = 0.98f;
        }
        
        velocity.x *= friction;
        velocity.z *= friction;
        
        // Rotation for visual tumbling effect
        if (onGround) {
            // Плавно выравниваем предмет на земле (Pitch и Roll в 0)
            rotation.x = lerpAngle(rotation.x, 0, deltaTime * 5.0f);
            rotation.z = lerpAngle(rotation.z, 0, deltaTime * 5.0f);
            // Замедляем вращение вокруг Y
            angularVelocity.mul(0.85f);
        }
        
        rotation.add(
            angularVelocity.x * deltaTime,
            angularVelocity.y * deltaTime,
            angularVelocity.z * deltaTime
        );
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
}
