package com.za.zenith.world.generation.zones;

import com.za.zenith.utils.Identifier;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class ZoneRegistry {
    private static final Map<Identifier, ZoneDefinition> REGISTRY = new HashMap<>();

    public static void register(ZoneDefinition zone) {
        REGISTRY.put(zone.getId(), zone);
    }

    public static Collection<ZoneDefinition> getAll() {
        return REGISTRY.values();
    }
    
    public static ZoneDefinition get(Identifier id) {
        return REGISTRY.get(id);
    }
}
