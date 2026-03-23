package com.za.minecraft.engine.graphics.ui;

import com.za.minecraft.utils.Identifier;
import java.util.HashMap;
import java.util.Map;

/**
 * Registry for GUI configurations loaded from JSON.
 */
public class GUIRegistry {
    private static final Map<Identifier, GUIConfig> GUIS = new HashMap<>();

    public static void register(Identifier id, GUIConfig config) {
        GUIS.put(id, config);
    }

    public static GUIConfig get(Identifier id) {
        return GUIS.get(id);
    }

    public static Map<Identifier, GUIConfig> getAll() {
        return GUIS;
    }
}
