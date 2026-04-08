package com.za.zenith.utils;

import com.za.zenith.world.physics.AABB;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.stb.STBImage.*;

/**
 * Утилита для автоматической генерации AABB на основе прозрачности текстуры.
 * Позволяет создавать точные хитбоксы для предметов и декоративных блоков.
 */
public class TextureAABBGenerator {
    private static final Map<String, AABB> CACHE = new HashMap<>();
    private static final int ALPHA_THRESHOLD = 30; // Порог прозрачности (0-255)

    /**
     * Генерирует AABB на основе непрозрачных пикселей текстуры.
     * @param texturePath Путь к ресурсу текстуры (например, "minecraft/textures/item/stick.png")
     * @return AABB в диапазоне [0.0, 1.0] или null при ошибке.
     */
    public static AABB generateAABB(String texturePath) {
        if (texturePath == null || texturePath.isEmpty()) return null;
        if (CACHE.containsKey(texturePath)) return CACHE.get(texturePath);

        // Путь должен быть относительным к ресурсам (без префикса src/main/resources/)
        String resourcePath = texturePath.replace("src/main/resources/", "");
        
        try (var is = TextureAABBGenerator.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) {
                Logger.warn("Texture not found for AABB generation: " + resourcePath);
                return null;
            }

            byte[] data = is.readAllBytes();
            ByteBuffer buffer = MemoryUtil.memAlloc(data.length);
            buffer.put(data).flip();

            int[] w = new int[1];
            int[] h = new int[1];
            int[] c = new int[1];
            
            // Загружаем текстуру (переворот не важен для поиска границ, но сделаем для консистентности)
            stbi_set_flip_vertically_on_load(true);
            ByteBuffer image = stbi_load_from_memory(buffer, w, h, c, 4);
            MemoryUtil.memFree(buffer);

            if (image == null) {
                Logger.error("Failed to load image for AABB generation: " + resourcePath);
                return null;
            }

            int width = w[0];
            int height = h[0];

            int minX = width, minY = height;
            int maxX = -1, maxY = -1;
            boolean found = false;

            // Сканируем все пиксели
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int alpha = image.get((y * width + x) * 4 + 3) & 0xFF;
                    if (alpha > ALPHA_THRESHOLD) {
                        if (x < minX) minX = x;
                        if (x > maxX) maxX = x;
                        if (y < minY) minY = y;
                        if (y > maxY) maxY = y;
                        found = true;
                    }
                }
            }

            stbi_image_free(image);

            if (!found) {
                CACHE.put(texturePath, null);
                return null;
            }

            // Преобразуем в нормализованные координаты [0.0, 1.0]
            // Помним, что STB загружает с (0,0) в левом верхнем углу (или нижнем при flip)
            float fx0 = (float) minX / width;
            float fy0 = (float) minY / height;
            float fx1 = (float) (maxX + 1) / width;
            float fy1 = (float) (maxY + 1) / height;

            AABB aabb = new AABB(fx0, fy0, 0.0f, fx1, fy1, 1.0f);
            CACHE.put(texturePath, aabb);
            return aabb;

        } catch (Exception e) {
            Logger.error("Error generating AABB for " + texturePath + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Очищает кеш сгенерированных AABB.
     */
    public static void clearCache() {
        CACHE.clear();
    }
}
