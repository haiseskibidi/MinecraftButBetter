package com.za.zenith.world.generation;

import com.za.zenith.utils.Identifier;
import com.za.zenith.utils.Registry;

import java.util.Collection;

public class BiomeRegistry {
    private static final Registry<BiomeDefinition> REGISTRY = new Registry<>();
    
    public static void register(BiomeDefinition biome) {
        REGISTRY.register(biome.getId(), biome);
    }
    
    public static BiomeDefinition get(Identifier id) {
        return REGISTRY.get(id);
    }
    
    public static Collection<BiomeDefinition> getAll() {
        return REGISTRY.values();
    }
}
