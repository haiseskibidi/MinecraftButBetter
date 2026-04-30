package com.za.zenith.world.blocks;

import org.joml.Vector3f;

/**
 * Утилита для управления лейаутом предметов на поверхности блоков (пни, верстаки).
 * Поддерживает адаптивное масштабирование и позиционирование.
 */
public class CraftingLayoutEngine {

    /**
     * Вычисляет индекс слота на основе координат клика и размера сетки.
     */
    public static int getSlotIndex(float hitX, float hitZ, int gridSize) {
        float x = Math.max(0, Math.min(0.999f, hitX));
        float z = Math.max(0, Math.min(0.999f, hitZ));
        
        int col = (int) (x * gridSize);
        int row = (int) (z * gridSize);
        
        return row * gridSize + col;
    }

    /**
     * Устаревший метод для совместимости (3х3).
     */
    public static int getSlotIndex(float hitX, float hitZ) {
        return getSlotIndex(hitX, hitZ, 3);
    }

    /**
     * Возвращает параметры трансформации для конкретного слота.
     */
    public static Vector3f getSlotTransform(int slotIndex, int totalItems, int gridSize) {
        float scale;
        
        if (gridSize == 2) {
            scale = totalItems <= 1 ? 0.6f : 0.4f;
            int col = slotIndex % 2;
            int row = slotIndex / 2;
            // Центры слотов: 0.25 и 0.75. Смещение от центра блока (0.5): -0.25 и 0.25
            float offsetX = (col - 0.5f) * 0.5f;
            float offsetZ = (row - 0.5f) * 0.5f;
            return new Vector3f(offsetX, scale, offsetZ);
        } else {
            // 3x3 Logic
            if (totalItems <= 1) scale = 0.7f;
            else if (totalItems <= 4) scale = 0.45f;
            else scale = 0.33f;

            int col = slotIndex % 3;
            int row = slotIndex / 3;
            // Центры слотов: 0.166, 0.5, 0.833. Смещение от центра блока (0.5): -0.333, 0, 0.333
            float offsetX = (col - 1) * 0.3333f;
            float offsetZ = (row - 1) * 0.3333f;

            if (totalItems == 1 && slotIndex == 4) scale = 0.75f;
            return new Vector3f(offsetX, scale, offsetZ);
        }
    }

    public static Vector3f getSlotTransform(int slotIndex, int totalItems) {
        return getSlotTransform(slotIndex, totalItems, 3);
    }
}


