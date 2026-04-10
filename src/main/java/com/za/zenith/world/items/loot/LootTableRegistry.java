package com.za.zenith.world.items.loot;

import com.za.zenith.utils.Identifier;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for data-driven loot tables.
 */
public class LootTableRegistry {
    private static final Map<Identifier, LootTable> TABLES = new HashMap<>();

    public static void register(LootTable table) {
        TABLES.put(table.identifier(), table);
    }

    public static LootTable get(Identifier id) {
        return TABLES.get(id);
    }

    public static Collection<LootTable> getAll() {
        return TABLES.values();
    }
}
