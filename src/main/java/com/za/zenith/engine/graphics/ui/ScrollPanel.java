package com.za.zenith.engine.graphics.ui;

import org.lwjgl.opengl.GL11;

/**
 * Универсальный компонент прокрутки.
 * Инкапсулирует логику GL Scissor, управление смещением и отрисовку скроллбара.
 */
public class ScrollPanel {
    private int x, y, width, height;
    private float scrollOffset = 0;
    private float maxScroll = 0;
    private float contentHeight = 0;
    
    // Настройки внешнего вида (можно будет расширить)
    private float[] scrollbarColor = {0.0f, 0.6f, 1.0f, 0.6f};
    private float[] trackColor = {1.0f, 1.0f, 1.0f, 0.05f};

    public void setBounds(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Обновляет высоту контента и пересчитывает лимиты скролла.
     */
    public void updateContentHeight(float totalH) {
        this.contentHeight = totalH;
        this.maxScroll = Math.max(0, contentHeight - height);
        
        // Коррекция, если контент уменьшился
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
    }

    /**
     * Включает область отсечения (Scissor Test).
     * Все, что рисуется между begin() и end(), будет обрезано по границам панели.
     */
    public void begin(int sw, int sh) {
        GL11.glEnable(GL11.GL_SCISSOR_TEST);
        // OpenGL считает координаты от левого нижнего угла
        GL11.glScissor(x, sh - (y + height), width, height);
    }

    /**
     * Выключает область отсечения.
     */
    public void end() {
        GL11.glDisable(GL11.GL_SCISSOR_TEST);
    }

    public void handleScroll(double yoffset) {
        scrollOffset -= yoffset * 40;
        if (scrollOffset < 0) scrollOffset = 0;
        if (scrollOffset > maxScroll) scrollOffset = maxScroll;
    }

    public void reset() {
        scrollOffset = 0;
    }

    public float getOffset() {
        return scrollOffset;
    }

    public void setScrollY(float offset) {
        this.scrollOffset = offset;
    }

    public void renderScrollbar(UIRenderer renderer, int sw, int sh) {
        if (maxScroll <= 0) return;

        int sbWidth = 2;
        int sbX = x + width - 4;
        int sbY = y + 5;
        int sbHeight = height - 10;

        // Фон дорожки
        renderer.renderRect(sbX, sbY, sbWidth, sbHeight, sw, sh, trackColor[0], trackColor[1], trackColor[2], trackColor[3]);

        // Ползунок
        float viewRatio = (float)sbHeight / (sbHeight + maxScroll);
        int thumbH = Math.max(20, (int)(sbHeight * viewRatio));
        int thumbY = sbY + (int)((scrollOffset / maxScroll) * (sbHeight - thumbH));

        renderer.renderRect(sbX, thumbY, sbWidth, thumbH, sw, sh, scrollbarColor[0], scrollbarColor[1], scrollbarColor[2], scrollbarColor[3]);
    }

    public boolean isMouseOver(float mx, float my) {
        return mx >= x && mx <= x + width && my >= y && my <= y + height;
    }

    public int getX() { return x; }
    public int getY() { return y; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}


