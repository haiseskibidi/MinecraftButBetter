package com.za.minecraft.world.blocks;

import com.za.minecraft.utils.Identifier;
import java.util.ArrayList;
import java.util.List;

/**
 * Реестр типов древесины для универсальных стадий срубания.
 */
public class WoodTypeRegistry {
    private static final List<Identifier> WOOD_TYPES = new ArrayList<>();

    static {
        // Регистрация ванильных типов (порядок важен для метаданных)
        register(Identifier.of("minecraft:oak_log"));
        register(Identifier.of("minecraft:birch_log"));
        register(Identifier.of("minecraft:spruce_log"));
        register(Identifier.of("minecraft:jungle_log"));
        register(Identifier.of("minecraft:acacia_log"));
        register(Identifier.of("minecraft:dark_oak_log"));
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
}
