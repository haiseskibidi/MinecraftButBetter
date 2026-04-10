package com.za.zenith.engine.graphics;

import org.joml.Vector3f;

/**
 * Единая точка управления цветами биома (трава, листва).
 * Позволяет динамически менять палитру мира и гарантирует
 * синхронизацию между блоками, предметами и частицами.
 */
public class ColorProvider {
    // Базовый цвет травы и листвы (Zenith Green)
    private static final Vector3f GRASS_COLOR = new Vector3f(0.486f, 0.784f, 0.314f);
    private static final Vector3f FOLIAGE_COLOR = new Vector3f(0.380f, 0.690f, 0.220f);

    public static Vector3f getGrassColor() {
        return GRASS_COLOR;
    }

    public static Vector3f getFoliageColor() {
        return FOLIAGE_COLOR;
    }
}
