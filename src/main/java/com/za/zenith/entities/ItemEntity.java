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
        boolean hasMagnet = false;
        
        if (player != null) {
            // Проверяем наличие MagneticComponent в любом активном слоте оборудования (аксессуары, offhand и т.д.)
            if (player.getInventory().hasActiveComponent(com.za.zenith.world.items.component.MagneticComponent.class)) {
                hasMagnet = true;
            }
        }
        
        if (canBePickedUp() && player != null) {
            Vector3f playerCenter = new Vector3f(player.getPosition());
            playerCenter.y += player.getHeight() * 0.5f;
            
            float distSq = position.distanceSquared(playerCenter);
            
            // Ищем магнит в инвентаре (в активном оборудовании)
            com.za.zenith.world.items.component.MagneticComponent magnet = null;
            SlotGroup equipment = player.getInventory().getGroup("equipment");
            if (equipment != null) {
                for (Slot slot : equipment.getSlots()) {
                    ItemStack stack = slot.getStack();
                    if (stack != null) {
                        com.za.zenith.world.items.component.MagneticComponent m = stack.getItem().getComponent(com.za.zenith.world.items.component.MagneticComponent.class);
                        if (m != null) {
                            magnet = m;
                            break;
                        }
                    }
                }
            }

            // Если вошел в радиус - захватываем навсегда
            if (magnet != null && (distSq < magnet.attractionRadius * magnet.attractionRadius || isLockedOnPlayer)) {
                isBeingAttracted = true;
                isLockedOnPlayer = true;
                
                float distance = (float)Math.sqrt(distSq);
                Vector3f toPlayer = new Vector3f(playerCenter).sub(position);
                Vector3f direction = new Vector3f(toPlayer).normalize();
                
                // --- ГРАМОТНАЯ ФИЗИКА МАГНИТА ---
                // Параметры теперь из компонента
                float approachSpeed = 12.0f + (1.0f - Math.min(1.0f, distance / 4.0f)) * (magnet.attractionForce * 0.2f);
                
                Vector3f targetVelocity = new Vector3f(player.getVelocity());
                targetVelocity.add(direction.mul(approachSpeed));
                
                float followSharpness = 5.0f + (1.0f - Math.min(1.0f, distance / 2.0f)) * 15.0f;
                velocity.lerp(targetVelocity, deltaTime * followSharpness);
                
                onGround = false; 
            }
        } else {
            isBeingAttracted = false;
            isLockedOnPlayer = false;
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
