package com.za.zenith.world.items.stats;

import com.za.zenith.utils.Identifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for data-driven affix rarities.
 */
public class AffixRarityRegistry {
    private static final Map<Identifier, AffixRarityDefinition> RARITIES = new HashMap<>();

    public static void register(AffixRarityDefinition def) {
        RARITIES.put(def.identifier(), def);
    }

    public static AffixRarityDefinition get(Identifier id) {
        return RARITIES.get(id);
    }

    public static Collection<AffixRarityDefinition> getAll() {
        return RARITIES.values();
    }
}
