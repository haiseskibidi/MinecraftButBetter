package com.za.zenith.world.items.stats;

import com.za.zenith.utils.Identifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Registry for data-driven item affixes.
 */
public class AffixRegistry {
    private static final Map<Identifier, AffixDefinition> AFFIXES = new HashMap<>();

    public static void register(AffixDefinition def) {
        AFFIXES.put(def.identifier(), def);
    }

    public static AffixDefinition get(Identifier id) {
        return AFFIXES.get(id);
    }

    public static Collection<AffixDefinition> getAll() {
        return AFFIXES.values();
    }

    public static List<AffixDefinition> getByRarity(Identifier rarityId) {
        List<AffixDefinition> result = new ArrayList<>();
        for (AffixDefinition def : AFFIXES.values()) {
            if (def.rarityId().equals(rarityId)) {
                result.add(def);
            }
        }
        return result;
    }
}
