package com.za.zenith.world.items.stats;

import com.za.zenith.utils.Identifier;
import org.joml.Vector3f;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for data-driven item rarities.
 */
public class RarityRegistry {
    private static final Map<Identifier, RarityDefinition> RARITIES = new HashMap<>();

    public static void register(RarityDefinition def) {
        RARITIES.put(def.identifier(), def);
    }

    public static RarityDefinition get(Identifier id) {
        return RARITIES.get(id);
    }

    public static Collection<RarityDefinition> getAll() {
        return RARITIES.values();
    }

    // Default rarities for safety if JSON fails
    public static final Identifier COMMON = Identifier.of("zenith:common");
    
    public static void init() {
        // Fallback common rarity
        if (get(COMMON) == null) {
            register(new RarityDefinition(COMMON, "rarity.zenith.common", new Vector3f(1, 1, 1), "$f", 0, 100));
        }
    }
}
