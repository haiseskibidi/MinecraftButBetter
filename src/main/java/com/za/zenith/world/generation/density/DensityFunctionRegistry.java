package com.za.zenith.world.generation.density;

import com.google.gson.JsonElement;
import com.za.zenith.utils.Identifier;

import java.util.HashMap;
import java.util.Map;

public class DensityFunctionRegistry {
    private static final Map<Identifier, JsonElement> REGISTRY = new HashMap<>();

    public static void register(Identifier id, JsonElement functionJson) {
        REGISTRY.put(id, functionJson);
    }

    public static JsonElement get(Identifier id) {
        return REGISTRY.get(id);
    }

    public static void clear() {
        REGISTRY.clear();
    }
}
