package com.za.zenith.entities;

import com.za.zenith.utils.Identifier;
import java.util.HashMap;
import java.util.Map;

/**
 * Реестр типов сущностей.
 */
public class EntityRegistry {
    private static final Map<Identifier, EntityDefinition> REGISTRY = new HashMap<>();

    public static void register(EntityDefinition definition) {
        REGISTRY.put(definition.identifier(), definition);
    }

    public static EntityDefinition get(Identifier identifier) {
        return REGISTRY.get(identifier);
    }

    public static Map<Identifier, EntityDefinition> getAll() {
        return new HashMap<>(REGISTRY);
    }
}
