package com.za.zenith.world.blocks;

import com.za.zenith.utils.Identifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Реестр типов древесины для универсальных стадий срубания.
 */
public class WoodTypeRegistry {
    private static final List<Identifier> WOOD_TYPES = new ArrayList<>();

    public static void init(List<Identifier> types) {
        WOOD_TYPES.clear();
        WOOD_TYPES.addAll(types);
    }

    public static int register(Identifier logId) {
        if (!WOOD_TYPES.contains(logId)) {
            WOOD_TYPES.add(logId);
        }
        return WOOD_TYPES.indexOf(logId);
    }

    public static Identifier getLogId(int index) {
        if (index >= 0 && index < WOOD_TYPES.size()) {
            return WOOD_TYPES.get(index);
        }
        return WOOD_TYPES.get(0); // По умолчанию дуб
    }

    public static int getIndex(Identifier logId) {
        return WOOD_TYPES.indexOf(logId);
    }
    
    public static int size() {
        return WOOD_TYPES.size();
    }
    
    public static List<Identifier> getAllLogs() {
        return new ArrayList<>(WOOD_TYPES);
    }
}
