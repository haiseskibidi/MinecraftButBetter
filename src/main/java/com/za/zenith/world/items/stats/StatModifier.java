package com.za.zenith.world.items.stats;

import com.za.zenith.utils.Identifier;

/**
 * Modifies a base stat value.
 */
public record StatModifier(
    Identifier source,
    Operation operation,
    float value
) {
    public enum Operation {
        ADD,            // Add value to base
        MULTIPLY_BASE,  // Multiply base value (additive multiplier)
        MULTIPLY_TOTAL  // Multiply final sum (multiplicative multiplier)
    }
}
