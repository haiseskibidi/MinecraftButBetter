package com.za.zenith.world.generation;

import com.za.zenith.utils.Identifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Глобальные настройки спавна ресурсов Foraging.
 */
public class ScavengeSettings {
    public record Entry(Identifier blockId, float chance) {}
    
    private static final List<Entry> ENTRIES = new ArrayList<>();

    public static void register(Identifier blockId, float chance) {
        ENTRIES.add(new Entry(blockId, chance));
    }

    public static List<Entry> getEntries() {
        return ENTRIES;
    }
}


