package com.za.zenith.world.items.stats;

import com.za.zenith.utils.Identifier;
import org.joml.Vector3f;

/**
 * Data-driven definition for affix rarity (Basic, Advanced, Elite).
 */
public record AffixRarityDefinition(
    Identifier identifier,
    String translationKey,
    Vector3f color,
    int weight
) {
}
