package com.za.zenith.engine.graphics.model;

import com.za.zenith.utils.Identifier;
import java.util.HashMap;
import java.util.Map;

/**
 * Реестр для хранения пресетов хвата (Grip Presets).
 * Заполняется при загрузке игры из папки src/main/resources/&lt;namespace&gt;/grips/
 */
public class GripRegistry {
    private static final Map<Identifier, GripDefinition> GRIPS = new HashMap<>();

    public static void register(Identifier id, GripDefinition def) {
        GRIPS.put(id, def);
    }

    public static GripDefinition get(Identifier id) {
        return GRIPS.get(id);
    }

    public static GripDefinition get(String idString) {
        if (idString == null || idString.isEmpty()) return null;
        try {
            return GRIPS.get(Identifier.of(idString));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static void clear() {
        GRIPS.clear();
    }
}