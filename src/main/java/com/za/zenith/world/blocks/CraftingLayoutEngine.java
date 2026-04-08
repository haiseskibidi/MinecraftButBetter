package com.za.zenith.world.blocks;

import org.joml.Vector3f;

/**
 * Утилита для управления лейаутом предметов на поверхности блоков (пни, верстаки).
 * Поддерживает адаптивное масштабирование и позиционирование.
 */
public class CraftingLayoutEngine {

    /**
     * Вычисляет индекс слота (0-8) на основе координат клика по верхней грани блока.
     */
    public static int getSlotIndex(float hitX, float hitZ) {
        // Ограничиваем входные значения, чтобы избежать ошибок округления
        float x = Math.max(0, Math.min(0.999f, hitX));
        float z = Math.max(0, Math.min(0.999f, hitZ));
        
        int col = (int) (x * 3);
        int row = (int) (z * 3);
        
        return row * 3 + col;
    }

    /**
     * Возвращает параметры трансформации для конкретного слота.
     * @param slotIndex Индекс слота (0-8)
     * @param totalItems Общее количество предметов на поверхности
     * @return Vector3f, где x, z - смещения от центра (0,0), y - масштаб (scale)
     */
    public static Vector3f getSlotTransform(int slotIndex, int totalItems) {
        float scale;
        
        // Масштаб зависит от общего кол-ва предметов
        if (totalItems <= 1) scale = 0.7f;
        else if (totalItems <= 4) scale = 0.45f;
        else scale = 0.33f;

        // Позиция ВСЕГДА фиксирована по сетке 3х3, чтобы избежать наложений
        int col = slotIndex % 3;
        int row = slotIndex / 3;
        
        float offsetX = (col - 1) * 0.3f;
        float offsetZ = (row - 1) * 0.3f;

        // Если предмет всего один и он в центре, можно сделать его побольше
        if (totalItems == 1 && slotIndex == 4) {
            scale = 0.75f;
        }

        return new Vector3f(offsetX, scale, offsetZ);
    }
}
