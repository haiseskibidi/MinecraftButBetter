package com.za.minecraft.entities;

import com.za.minecraft.utils.Identifier;
import org.joml.Vector3f;

/**
 * Универсальная декоративная сущность, параметры которой задаются через JSON (EntityDefinition).
 */
public class DecorationEntity extends Entity {
    private final EntityDefinition definition;

    public DecorationEntity(Vector3f position, Identifier definitionId, float rotationY) {
        super(position, 0.5f, 0.5f); // Дефолтный размер, переопределяется ниже
        this.definition = EntityRegistry.get(definitionId);
        
        if (this.definition != null) {
            this.rotation.y = rotationY;
            Vector3f hitbox = definition.hitboxSize();
            // Центрируем хитбокс относительно позиции
            this.boundingBox = new com.za.minecraft.world.physics.AABB(
                -hitbox.x / 2, 0, -hitbox.z / 2,
                hitbox.x / 2, hitbox.y, hitbox.z / 2
            );
        }
    }

    public EntityDefinition getDefinition() {
        return definition;
    }

    @Override
    public void update(float deltaTime, com.za.minecraft.world.World world) {
        // Декоративные сущности обычно статичны
    }
}
