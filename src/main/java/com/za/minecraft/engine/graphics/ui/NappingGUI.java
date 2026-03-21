package com.za.minecraft.engine.graphics.ui;

import com.za.minecraft.world.recipes.NappingSession;

/**
 * Отрисовка интерфейса скалывания камня (Napping).
 */
public class NappingGUI {
    public static final int GRID_SIZE = 5;
    public static final float SLOT_SIZE = 40.0f;
    public static final float SPACING = 5.0f;

    public static void render(UIRenderer renderer, int sw, int sh, NappingSession session) {
        if (session == null) return;

        // Явно подготавливаем шейдер для рисования GUI поверх всего
        renderer.setupUIProjection(sw, sh);

        float totalSize = GRID_SIZE * (SLOT_SIZE + SPACING);
        float startX = (sw - totalSize) / 2;
        float startY = (sh - totalSize) / 2;

        // --- Подложка ---
        renderer.renderHighlight((int)startX - 10, (int)startY - 10, (int)totalSize + 20, sw, sh, 0.1f, 0.1f, 0.1f, 0.7f);

        boolean[] grid = session.getGrid();
        for (int i = 0; i < 25; i++) {
            int row = i / GRID_SIZE;
            int col = i % GRID_SIZE;
            
            float x = startX + col * (SLOT_SIZE + SPACING);
            float y = startY + row * (SLOT_SIZE + SPACING);

            if (grid[i]) {
                // Камень (светло-серый)
                renderer.renderHighlight((int)x, (int)y, (int)SLOT_SIZE, sw, sh, 0.7f, 0.7f, 0.7f, 1.0f);
            } else {
                // Пустота (темно-серый)
                renderer.renderHighlight((int)x, (int)y, (int)SLOT_SIZE, sw, sh, 0.2f, 0.2f, 0.2f, 0.8f);
            }
        }
    }

    public static int getSlotIndexAt(float mx, float my, int sw, int sh) {
        float totalSize = GRID_SIZE * (SLOT_SIZE + SPACING);
        float startX = (sw - totalSize) / 2;
        float startY = (sh - totalSize) / 2;

        if (mx < startX || mx > startX + totalSize || my < startY || my > startY + totalSize) {
            return -1;
        }

        int col = (int)((mx - startX) / (SLOT_SIZE + SPACING));
        int row = (int)((my - startY) / (SLOT_SIZE + SPACING));
        
        if (col >= 0 && col < GRID_SIZE && row >= 0 && row < GRID_SIZE) {
            return row * GRID_SIZE + col;
        }
        return -1;
    }
}
