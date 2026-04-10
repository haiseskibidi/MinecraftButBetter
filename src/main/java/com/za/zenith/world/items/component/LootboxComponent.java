package com.za.zenith.world.items.component;

import com.za.zenith.utils.Identifier;
import java.util.Map;

/**
 * Component that makes an item act as a loot container (case).
 */
public record LootboxComponent(
    Identifier lootTable,
    float openingTime,
    Map<Identifier, Integer> rarityWeights,
    Map<Identifier, Integer> affixWeights
) implements ItemComponent {
}
