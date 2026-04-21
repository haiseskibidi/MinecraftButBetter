package com.za.zenith.world.actions;

import com.za.zenith.utils.Identifier;
import java.util.HashMap;
import java.util.Map;

public class ActionRegistry {
    private static final Map<Identifier, ActionDefinition> registry = new HashMap<>();

    public static void register(Identifier id, ActionDefinition def) {
        registry.put(id, def);
    }

    public static ActionDefinition get(Identifier id) {
        return registry.getOrDefault(id, new ActionDefinition());
    }

    public static java.util.Set<com.za.zenith.utils.Identifier> getKeys() {
        return registry.keySet();
    }

    public static void clear() {
        registry.clear();
    }
}


