package com.za.zenith.world.items.loot;

import com.za.zenith.utils.Identifier;
import java.util.List;

/**
 * Data-driven loot table structure.
 */
public record LootTable(
    Identifier identifier,
    List<Pool> pools
) {
    public record Pool(
        int rolls,
        List<Entry> entries
    ) {}

    public record Entry(
        Identifier item,
        int weight
    ) {}
}
