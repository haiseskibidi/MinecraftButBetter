package com.za.zenith.world.items.stats;

import com.za.zenith.utils.Identifier;
import java.util.List;
import java.util.Map;

/**
 * Data-driven definition for an item affix (modifier).
 */
public record AffixDefinition(
    Identifier identifier,
    String translationKey,
    Identifier rarityId,
    Type type,
    Map<Identifier, Float> stats,
    List<String> applicableTo
) {
    public enum Type {
        PREFIX,
        SUFFIX
    }
}
