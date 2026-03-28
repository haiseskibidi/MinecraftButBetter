package com.za.minecraft.engine.graphics.ui;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;
import com.za.minecraft.engine.graphics.Shader;

/**
 * Specialized renderer for UI effects like selection brackets, glows, and animations.
 * Decoupled from UIRenderer to keep it clean.
 */
public class UIEffectsRenderer {

    public static void renderSelection(UIRenderer renderer, Shader uiShader, int quadVAO, int x, int y, int size, int sw, int sh, GUIConfig.SelectionStyle style) {
        if (style == null || style.type.equals("none")) return;

        int padding = style.padding;
        int frameX = x - padding;
        int frameY = y - padding;
        int frameSize = size + padding * 2;
        
        float alpha = style.color[3];
        if (style.pulse) {
            float time = (float)(System.currentTimeMillis() % 2000) / 2000.0f;
            alpha *= 0.6f + 0.4f * (float)Math.sin(time * Math.PI * 2.0f);
        }

        uiShader.use();
        uiShader.setInt("useTexture", 0);
        uiShader.setInt("useArray", 0);
        uiShader.setUniform("tintColor", style.color[0], style.color[1], style.color[2], alpha);
        
        glBindVertexArray(quadVAO);

        if (style.type.equals("brackets")) {
            renderBrackets(uiShader, frameX, frameY, frameSize, sw, sh, style.thickness);
        } else if (style.type.equals("border")) {
            renderBorder(uiShader, frameX, frameY, frameSize, sw, sh, style.thickness);
        }

        glBindVertexArray(0);
    }

    private static void renderBrackets(Shader shader, int x, int y, int size, int sw, int sh, float thickness) {
        int armLength = (int)(size * 0.25f); // 1/4 of size for brackets
        
        // Top-Left Corner
        drawRect(shader, x, y, armLength, (int)thickness, sw, sh); // horizontal
        drawRect(shader, x, y, (int)thickness, armLength, sw, sh); // vertical
        
        // Top-Right Corner
        drawRect(shader, x + size - armLength, y, armLength, (int)thickness, sw, sh); 
        drawRect(shader, x + size - (int)thickness, y, (int)thickness, armLength, sw, sh);
        
        // Bottom-Left Corner
        drawRect(shader, x, y + size - (int)thickness, armLength, (int)thickness, sw, sh);
        drawRect(shader, x, y + size - armLength, (int)thickness, armLength, sw, sh);
        
        // Bottom-Right Corner
        drawRect(shader, x + size - armLength, y + size - (int)thickness, armLength, (int)thickness, sw, sh);
        drawRect(shader, x + size - (int)thickness, y + size - armLength, (int)thickness, armLength, sw, sh);
    }

    private static void renderBorder(Shader shader, int x, int y, int size, int sw, int sh, float thickness) {
        drawRect(shader, x, y, size, (int)thickness, sw, sh); // Top
        drawRect(shader, x, y + size - (int)thickness, size, (int)thickness, sw, sh); // Bottom
        drawRect(shader, x, y, (int)thickness, size, sw, sh); // Left
        drawRect(shader, x + size - (int)thickness, y, (int)thickness, size, sw, sh); // Right
    }

    private static void drawRect(Shader shader, int x, int y, int w, int h, int sw, int sh) {
        float scaleX = (float)w / sw;
        float scaleY = (float)h / sh;
        float posX = (2.0f * x / sw) - 1.0f + scaleX;
        float posY = 1.0f - (2.0f * y / sh) - scaleY;

        shader.setUniform("scale", scaleX, scaleY, 0.0f, 0.0f);
        shader.setUniform("position_offset", posX, posY, 0.0f, 0.0f);
        glDrawElements(GL_TRIANGLES, 6, GL_UNSIGNED_INT, 0);
    }
}
