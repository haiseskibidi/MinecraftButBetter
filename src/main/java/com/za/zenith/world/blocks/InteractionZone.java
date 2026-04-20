package com.za.zenith.world.blocks;

import com.za.zenith.world.physics.AABB;
import org.joml.Vector3f;

/**
 * Описывает интерактивную область внутри блока.
 */
public record InteractionZone(
    AABB bounds,           // Область в локальных координатах блока (0.0 - 1.0)
    String requiredAction  // null или специфическое действие (например, "carve")
) {
    public boolean contains(Vector3f localHit) {
        return bounds.contains(localHit);
    }
}
