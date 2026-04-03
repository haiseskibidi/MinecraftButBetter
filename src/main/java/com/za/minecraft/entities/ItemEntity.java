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
        boolean attractedThisTick = false;
        
        if (canBePickedUp() && player != null) {
            Vector3f playerCenter = new Vector3f(player.getPosition());
            playerCenter.y += player.getHeight() * 0.5f; // Нацеливаемся в центр тела
            
            float distSq = position.distanceSquared(playerCenter);
            float attrRadius = com.za.minecraft.world.physics.PhysicsSettings.getInstance().itemAttractionRadius;
            
            if (distSq < attrRadius * attrRadius) {
                attractedThisTick = true;
                isBeingAttracted = true;
                
                // --- МАГНИТНОЕ ПРИТЯЖЕНИЕ (УСИЛЕННОЕ) ---
                Vector3f direction = new Vector3f(playerCenter).sub(position).normalize();
                
                // Ускорение зависит от расстояния (квадратичная зависимость для 'сочности')
                float distance = (float)Math.sqrt(distSq);
                float force = com.za.minecraft.world.physics.PhysicsSettings.getInstance().itemAttractionForce;
                
                // Базовая сила + добавка за близость (чем ближе, тем злее тянет)
                float t = 1.0f - (distance / attrRadius);
                float pullStrength = (t * t * 1.5f + 0.2f) * force; // Квадратичный разгон
                
                velocity.add(direction.mul(pullStrength * deltaTime));
                
                // Ограничиваем скорость полета
                float maxPullSpeed = 20.0f;
                if (velocity.length() > maxPullSpeed) {
                    velocity.normalize(maxPullSpeed);
                }
                
                onGround = false; 
            }
        }
        
        if (!attractedThisTick) isBeingAttracted = false;

        float gravityMultiplier = stack.getItem().getWeight();
        
        // Custom gravity for items (based on weight), disabled during attraction
        if (!flying && !onGround && !isBeingAttracted) {
            velocity.y = Math.max(velocity.y + GRAVITY * gravityMultiplier * deltaTime, TERMINAL_VELOCITY);
        } else if (onGround && !isBeingAttracted) {
            velocity.y = 0; // Ground lock
        }
        
        // Мы вызываем move напрямую, минуя Entity.update(), так как нам нужна кастомная гравитация
        move(world, velocity.x * deltaTime, velocity.y * deltaTime, velocity.z * deltaTime);

        // Snap very small residual velocities to zero (only if not flying to player)
        if (!isBeingAttracted) {
            if (Math.abs(velocity.x) < 0.005f) velocity.x = 0f;
            if (Math.abs(velocity.z) < 0.005f) velocity.z = 0f;
        }

        age += deltaTime;
        if (pickupDelay > 0) {
            pickupDelay -= deltaTime;
        }
        
        // Трение (Friction)
        float friction;
        if (onGround && !isBeingAttracted) {
            float weightFactor = stack.getItem().getWeight();
            friction = 0.98f - (weightFactor * 0.2f);
            friction = Math.max(0.1f, Math.min(0.98f, friction));
            angularVelocity.mul(0.9f);
        } else if (isBeingAttracted) {
            friction = 1.0f; // В вакууме магнита не тормозим (чтобы не было 'киселя')
        } else {
            friction = 0.98f;
        }
        
        velocity.x *= friction;
        velocity.y *= friction; 
        velocity.z *= friction;
        
        // Rotation for visual tumbling effect
        if (onGround && !isBeingAttracted) {
            // Плавно выравниваем предмет на земле (Pitch и Roll в 0)
            rotation.x = lerpAngle(rotation.x, 0, deltaTime * 5.0f);
            rotation.z = lerpAngle(rotation.z, 0, deltaTime * 5.0f);
            // Замедляем вращение вокруг Y
            angularVelocity.mul(0.85f);
        } else if (isBeingAttracted) {
            // В полете предмет начинает быстрее вращаться (эффект вихря)
            angularVelocity.y += deltaTime * 20.0f;
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
