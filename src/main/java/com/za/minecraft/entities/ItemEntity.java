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
    private boolean isBeingAttracted = false;
    private boolean isLockedOnPlayer = false; // "Мертвая хватка"
    
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
        
        Player player = world.getPlayer();
        
        if (canBePickedUp() && player != null) {
            Vector3f playerCenter = new Vector3f(player.getPosition());
            playerCenter.y += player.getHeight() * 0.5f;
            
            float distSq = position.distanceSquared(playerCenter);
            float attrRadius = com.za.minecraft.world.physics.PhysicsSettings.getInstance().itemAttractionRadius;
            
            // Если вошел в радиус - захватываем навсегда
            if (distSq < attrRadius * attrRadius || isLockedOnPlayer) {
                isBeingAttracted = true;
                isLockedOnPlayer = true;
                
                float distance = (float)Math.sqrt(distSq);
                Vector3f toPlayer = new Vector3f(playerCenter).sub(position);
                Vector3f direction = new Vector3f(toPlayer).normalize();
                
                // --- ГРАМОТНАЯ ФИЗИКА МАГНИТА ---
                // 1. Целевая скорость сближения (растет при приближении)
                float approachSpeed = 12.0f + (1.0f - Math.min(1.0f, distance / 4.0f)) * 10.0f;
                
                // 2. Идеальный вектор скорости: Скорость Игрока + Вектор к Игроку
                Vector3f targetVelocity = new Vector3f(player.getVelocity());
                targetVelocity.add(direction.mul(approachSpeed));
                
                // 3. Плавно, но быстро приводим текущую скорость к идеальной (Slerp-like)
                // Чем ближе предмет, тем жестче он следует за целевым вектором
                float followSharpness = 5.0f + (1.0f - Math.min(1.0f, distance / 2.0f)) * 15.0f;
                velocity.lerp(targetVelocity, deltaTime * followSharpness);
                
                onGround = false; 
            }
        }
        
        float gravityMultiplier = stack.getItem().getWeight();
        
        // Custom gravity, disabled during attraction
        if (!flying && !onGround && !isBeingAttracted) {
            velocity.y = Math.max(velocity.y + GRAVITY * gravityMultiplier * deltaTime, TERMINAL_VELOCITY);
        } else if (onGround && !isBeingAttracted) {
            velocity.y = 0;
        }
        
        // Движение с коллизиями
        move(world, velocity.x * deltaTime, velocity.y * deltaTime, velocity.z * deltaTime);

        age += deltaTime;
        if (pickupDelay > 0) {
            pickupDelay -= deltaTime;
        }
        
        // Трение (в режиме магнита трения нет, мы управляем вектором напрямую)
        float friction = isBeingAttracted ? 1.0f : (onGround ? 0.85f : 0.98f);
        velocity.mul(friction);
        
        if (onGround && !isBeingAttracted) {
            rotation.x = lerpAngle(rotation.x, 0, deltaTime * 5.0f);
            rotation.z = lerpAngle(rotation.z, 0, deltaTime * 5.0f);
            angularVelocity.mul(0.85f);
        } else if (isBeingAttracted) {
            // Визуальный "вихрь"
            angularVelocity.y += deltaTime * 20.0f;
            angularVelocity.x += deltaTime * 5.0f;
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
