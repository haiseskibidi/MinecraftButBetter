package com.za.zenith.engine.graphics.ui.renderers;

import com.za.zenith.utils.Identifier;
import com.za.zenith.world.blocks.BlockRegistry;
import com.za.zenith.engine.graphics.ColorProvider;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

/**
 * Реестр цветов для миникарты. Сопоставляет типы блоков их визуальному представлению на радаре.
 */
public class MinimapRegistry {
    private static final Map<Integer, Integer> colorCache = new HashMap<>();
    private static final int DEFAULT_COLOR = 0xFF888888; // Grey

    static {
        // Базовые цвета (будут дополнены из JSON в будущем)
        register(Identifier.of("zenith:grass_block"), pack(ColorProvider.getGrassColor()));
        register(Identifier.of("zenith:dirt"), 0xFF3B5B8E); // Dirt/Brown (ABGR)
        register(Identifier.of("zenith:stone"), 0xFF888888);
        register(Identifier.of("zenith:sand"), 0xFF8FD8E6);
        register(Identifier.of("zenith:water"), 0xFFFF9933); // Blue
        register(Identifier.of("zenith:clay"), 0xFF9494A1);
        register(Identifier.of("zenith:gravel"), 0xFF737373);
        register(Identifier.of("zenith:asphalt"), 0xFF333333);
        register(Identifier.of("zenith:bricks"), 0xFF3D3D99);
        register(Identifier.of("zenith:leaves"), pack(ColorProvider.getFoliageColor()));
        register(Identifier.of("zenith:oak_leaves"), pack(ColorProvider.getFoliageColor()));
        register(Identifier.of("zenith:birch_leaves"), pack(ColorProvider.getFoliageColor()));
        register(Identifier.of("zenith:jungle_leaves"), pack(ColorProvider.getFoliageColor()));
        register(Identifier.of("zenith:acacia_leaves"), pack(ColorProvider.getFoliageColor()));
        register(Identifier.of("zenith:dark_oak_leaves"), pack(ColorProvider.getFoliageColor()));
        register(Identifier.of("zenith:cobblestone"), 0xFF777777);
    }

    public static void register(Identifier id, int abgrColor) {
        int type = BlockRegistry.getRegistry().getId(id);
        if (type != -1) {
            colorCache.put(type, abgrColor);
        }
    }

    public static int getColor(int type) {
        return colorCache.getOrDefault(type, DEFAULT_COLOR);
    }

    private static int pack(Vector3f color) {
        int r = (int) (color.x * 255);
        int g = (int) (color.y * 255);
        int b = (int) (color.z * 255);
        return 0xFF000000 | (b << 16) | (g << 8) | r;
    }
}
