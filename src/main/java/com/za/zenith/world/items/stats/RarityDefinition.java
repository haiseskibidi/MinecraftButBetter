package com.za.zenith.world.items.stats;

import com.za.zenith.utils.Identifier;
import org.joml.Vector3f;

/**
 * Data-driven definition for item rarity.
 */
public record RarityDefinition(
    Identifier identifier,
    String translationKey,
    Vector3f color,
    String colorCode,
    int affixSlots,
    int weight
) {
}
