package com.za.zenith.world.items.stats;

import com.za.zenith.utils.Identifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for data-driven stats.
 */
public class StatRegistry {
    private static final Map<Identifier, StatDefinition> STATS = new HashMap<>();

    public static void register(StatDefinition def) {
        STATS.put(def.identifier(), def);
    }

    public static StatDefinition get(Identifier id) {
        return STATS.get(id);
    }

    public static Collection<StatDefinition> getAll() {
        return STATS.values();
    }

    // Common stat identifiers
    public static final Identifier IMPACT = Identifier.of("zenith:impact");
    public static final Identifier MOBILITY = Identifier.of("zenith:mobility");
    public static final Identifier CONSTITUTION = Identifier.of("zenith:constitution");
    public static final Identifier HANDLING = Identifier.of("zenith:handling");
    public static final Identifier DEFENSE = Identifier.of("zenith:defense");
    public static final Identifier SHOCK = Identifier.of("zenith:shock");
    public static final Identifier PENETRATION = Identifier.of("zenith:penetration");
}
