package com.za.zenith.world.generation.structures;

import com.za.zenith.utils.Identifier;
import com.za.zenith.utils.Registry;

public class StructureRegistry {
    private static final Registry<StructureTemplate> REGISTRY = new Registry<>();

    public static void register(Identifier id, StructureTemplate template) {
        REGISTRY.register(id, template);
    }

    public static StructureTemplate get(Identifier id) {
        return REGISTRY.get(id);
    }

    public static Registry<StructureTemplate> getRegistry() {
        return REGISTRY;
    }
}
