package com.za.zenith.world.blocks;

import org.joml.Vector3f;

/**
 * Утилитный класс для расчета координат сетки 4х4 (Carving).
 */
public class CarvingLayoutEngine {
    private static final float STEP = 0.25f;

    /**
     * Вычисляет индекс зоны (0-15) на основе хита.
     */
    public static int getZoneIndex(float hitX, float hitZ) {
        int x = (int) (hitX * 4);
        int z = (int) (hitZ * 4);
        
        x = Math.max(0, Math.min(3, x));
        z = Math.max(0, Math.min(3, z));
        
        return z * 4 + x;
    }

    /**
     * Возвращает смещение x, z для отрисовки зоны.
     */
    public static Vector3f getZoneOffset(int index) {
        int z = index / 4;
        int x = index % 4;
        return new Vector3f(x * STEP, 1.001f, z * STEP);
    }
}


