package com.za.zenith.utils;

import java.util.List;

/**
 * Утилита для линейной интерполяции по набору точек (сплайну).
 */
public class SplineInterpolator {
    
    public static float interpolate(List<float[]> points, float input) {
        if (points == null || points.isEmpty()) return 0;
        if (points.size() == 1) return points.get(0)[1];

        // Сортировка по X (input) предполагается при загрузке JSON
        if (input <= points.get(0)[0]) return points.get(0)[1];
        if (input >= points.get(points.size() - 1)[0]) return points.get(points.size() - 1)[1];

        for (int i = 0; i < points.size() - 1; i++) {
            float[] p1 = points.get(i);
            float[] p2 = points.get(i + 1);

            if (input >= p1[0] && input <= p2[0]) {
                float t = (input - p1[0]) / (p2[0] - p1[0]);
                return p1[1] + t * (p2[1] - p1[1]);
            }
        }

        return points.get(points.size() - 1)[1];
    }
}
