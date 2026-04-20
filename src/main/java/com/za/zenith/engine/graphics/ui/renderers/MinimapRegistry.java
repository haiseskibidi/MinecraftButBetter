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
    private static final int[] colorArray = new int[4096]; // Максимальное кол-во блоков
    private static final boolean[] solidArray = new boolean[4096];
    private static final int DEFAULT_COLOR = 0xFF888888;

    static {
        java.util.Arrays.fill(colorArray, DEFAULT_COLOR);
    }

    public static void init() {
        // 1. Сначала регистрируем все твердые блоки из общего реестра
        for (Identifier id : BlockRegistry.getRegistry().getIds()) {
            int type = BlockRegistry.getRegistry().getId(id);
            com.za.zenith.world.blocks.BlockDefinition def = BlockRegistry.getBlock(type);
            
            // Если блок твердый - он должен быть на карте
            if (def.isSolid()) {
                solidArray[type] = true;
                
                // Назначаем дефолтные цвета по ключевым словам в ID
                String path = id.getPath();
                if (path.contains("log") || path.contains("wood")) colorArray[type] = 0xFF224466; // Brown/Wood
                else if (path.contains("leaf") || path.contains("leaves")) colorArray[type] = pack(ColorProvider.getFoliageColor());
                else if (path.contains("stone") || path.contains("cobble")) colorArray[type] = 0xFF888888;
                else if (path.contains("concrete")) colorArray[type] = 0xFF999999;
                else if (path.contains("planks")) colorArray[type] = 0xFF335577;
            }
        }

        // 2. Перекрываем специфическими цветами для красоты
        register(Identifier.of("zenith:grass_block"), pack(ColorProvider.getGrassColor()), true);
        register(Identifier.of("zenith:dirt"), 0xFF3B5B8E, true);
        register(Identifier.of("zenith:sand"), 0xFF8FD8E6, true);
        register(Identifier.of("zenith:water"), 0xFFFF9933, false);
        register(Identifier.of("zenith:bricks"), 0xFF3D3D99, true);
        register(Identifier.of("zenith:asphalt"), 0xFF333333, true);
        register(Identifier.of("zenith:glass"), 0x66FFFFFF, false); // Полупрозрачный
    }

    public static void register(Identifier id, int abgrColor, boolean isSolid) {
        int type = BlockRegistry.getRegistry().getId(id);
        if (type >= 0 && type < 4096) {
            colorArray[type] = abgrColor;
            solidArray[type] = isSolid;
        }
    }

    public static int getColor(int type) {
        return (type >= 0 && type < 4096) ? colorArray[type] : DEFAULT_COLOR;
    }

    public static boolean isSolid(int type) {
        return (type >= 0 && type < 4096) && solidArray[type];
    }

    private static int pack(Vector3f color) {
        int r = (int) (color.x * 255);
        int g = (int) (color.y * 255);
        int b = (int) (color.z * 255);
        return 0xFF000000 | (b << 16) | (g << 8) | r;
    }
}
