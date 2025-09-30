package com.za.minecraft.engine.graphics.ui;

import com.za.minecraft.engine.graphics.Shader;
import com.za.minecraft.engine.graphics.Texture;
import com.za.minecraft.utils.Logger;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class UIRenderer {
    private Shader uiShader;
    private Texture crosshairTexture;
    private Texture hotbarTexture;
    private Texture hotbarSelectionTexture;
    private int quadVAO;
    private int quadVBO;
    private int quadEBO;
    private Hotbar hotbar;
    private PauseMenu pauseMenu;
    private FontRenderer fontRenderer;
    
    private static final float[] QUAD_VERTICES = {
        // Позиции    // UV координаты
        -1.0f, -1.0f, 0.0f, 0.0f,
         1.0f, -1.0f, 1.0f, 0.0f,
         1.0f,  1.0f, 1.0f, 1.0f,
        -1.0f,  1.0f, 0.0f, 1.0f
    };
    
    private static final int[] QUAD_INDICES = {
        0, 1, 2,
        2, 3, 0
    };
    
    public void init() {
        uiShader = new Shader(
            "src/main/resources/shaders/ui_vertex.glsl",
            "src/main/resources/shaders/ui_fragment.glsl"
        );
        
        crosshairTexture = new Texture("src/main/resources/textures/crosshair.png", false, false);
        hotbarTexture = new Texture("src/main/resources/textures/gui/hotbar_slots.png", false, false);
        hotbarSelectionTexture = new Texture("src/main/resources/textures/gui/hotbar_selection.png", false, false);
        
        createQuad();
        
        fontRenderer = new FontRenderer();
        fontRenderer.init(uiShader);
        
        uiShader.use();
        uiShader.setInt("textureSampler", 0);
        
        Logger.info("UI Renderer initialized");
    }
    
    public void setHotbar(Hotbar hotbar) {
        this.hotbar = hotbar;
    }
    
    public void setPauseMenu(PauseMenu pauseMenu) {
        this.pauseMenu = pauseMenu;
    }
    
    private void createQuad() {
        quadVAO = glGenVertexArrays();
        quadVBO = glGenBuffers();
        quadEBO = glGenBuffers();
        
        glBindVertexArray(quadVAO);
        
        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(QUAD_VERTICES.length);
        vertexBuffer.put(QUAD_VERTICES).flip();
        
        glBindBuffer(GL_ARRAY_BUFFER, quadVBO);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
        
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);
        
        IntBuffer indexBuffer = MemoryUtil.memAllocInt(QUAD_INDICES.length);
        indexBuffer.put(QUAD_INDICES).flip();
        
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, quadEBO);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);
        
        glBindVertexArray(0);
        
        MemoryUtil.memFree(vertexBuffer);
        MemoryUtil.memFree(indexBuffer);
    }
    
    public void renderCrosshair(int screenWidth, int screenHeight) {
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        uiShader.use();
        uiShader.setInt("useTexture", 1);
        uiShader.setUniform("tintColor", 1.0f, 1.0f, 1.0f, 1.0f);
        
        float crosshairSize = 16.0f;
        float scaleX = crosshairSize / screenWidth;
        float scaleY = crosshairSize / screenHeight;
        
        uiShader.setUniform("scale", scaleX, scaleY, 0.0f, 0.0f);
        uiShader.setUniform("position_offset", 0.0f, 0.0f, 0.0f, 0.0f);
        
        float u = 0.0f;
        float v = 0.0f;
        float uvSize = 1.0f;
        
        uiShader.setUniform("uvOffset", u, v, 0.0f, 0.0f);
        uiShader.setUniform("uvScale", uvSize, uvSize, 0.0f, 0.0f);
        
        crosshairTexture.bind();
        
        glBindVertexArray(quadVAO);
        glDrawElements(GL_TRIANGLES, QUAD_INDICES.length, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
        
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }
    
    public void renderHotbar(int screenWidth, int screenHeight) {
        if (hotbar == null) return;
        
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        uiShader.use();
        uiShader.setInt("useTexture", 1);
        uiShader.setUniform("tintColor", 1.0f, 1.0f, 1.0f, 1.0f);
        
        renderHotbarBackground(screenWidth, screenHeight);
        
        renderHotbarSelection(screenWidth, screenHeight);
        
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }
    
    public void renderPauseMenu(int screenWidth, int screenHeight) {
        if (pauseMenu == null || !pauseMenu.isVisible()) return;
        
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        renderDarkenedBackground();
        
        renderMenuButtons(screenWidth, screenHeight);
        
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }
    
    private void renderDarkenedBackground() {
        uiShader.use();
        
        uiShader.setInt("useTexture", 0);
        uiShader.setUniform("scale", 1.0f, 1.0f, 0.0f, 0.0f);
        uiShader.setUniform("position_offset", 0.0f, 0.0f, 0.0f, 0.0f);
        uiShader.setUniform("uvOffset", 0.0f, 0.0f, 0.0f, 0.0f);
        uiShader.setUniform("uvScale", 1.0f, 1.0f, 0.0f, 0.0f);
        uiShader.setUniform("tintColor", 0.0f, 0.0f, 0.0f, 0.6f);
        
        glBindTexture(GL_TEXTURE_2D, 0);
        
        glBindVertexArray(quadVAO);
        glDrawElements(GL_TRIANGLES, QUAD_INDICES.length, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
        
        uiShader.setUniform("tintColor", 1.0f, 1.0f, 1.0f, 1.0f);
    }
    
    private void renderMenuButtons(int screenWidth, int screenHeight) {
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        
        int buttonWidth = pauseMenu.getButtonWidth();
        int buttonHeight = pauseMenu.getButtonHeight();
        int spacing = pauseMenu.getButtonSpacing();
        
        renderButton(centerX, centerY - spacing, buttonWidth, buttonHeight, screenWidth, screenHeight, null, 0.4f, 0.7f, 1.0f);
        renderButton(centerX, centerY + spacing, buttonWidth, buttonHeight, screenWidth, screenHeight, null, 1.0f, 0.4f, 0.4f);
    }
    
    private void renderButton(int x, int y, int width, int height, int screenWidth, int screenHeight, String text, float r, float g, float b) {
        uiShader.use();
        
        uiShader.setInt("useTexture", 0);
        float scaleX = (float)width / screenWidth;
        float scaleY = (float)height / screenHeight;
        
        float posX = (2.0f * x / screenWidth) - 1.0f;
        float posY = 1.0f - (2.0f * y / screenHeight);
        
        uiShader.setUniform("scale", scaleX, scaleY, 0.0f, 0.0f);
        uiShader.setUniform("position_offset", posX, posY, 0.0f, 0.0f);
        uiShader.setUniform("uvOffset", 0.0f, 0.0f, 0.0f, 0.0f);
        uiShader.setUniform("uvScale", 1.0f, 1.0f, 0.0f, 0.0f);
        uiShader.setUniform("tintColor", r, g, b, 0.9f);
        
        glBindTexture(GL_TEXTURE_2D, 0);
        
        glBindVertexArray(quadVAO);
        glDrawElements(GL_TRIANGLES, QUAD_INDICES.length, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
        
        if (text != null && !text.isEmpty()) {
            int textSize = 16;
            int textWidth = fontRenderer.getStringWidth(text, textSize);
            int textX = x - textWidth / 2;
            int textY = y - textSize / 2;
            
            fontRenderer.drawString(text, textX, textY, textSize, screenWidth, screenHeight);
        }
    }
    
    private void renderHotbarBackground(int screenWidth, int screenHeight) {
        hotbarTexture.bind();
        
        // Позиция хотбара на экране
        int hotbarX = hotbar.getScreenX(screenWidth);
        int hotbarY = hotbar.getScreenY(screenHeight);
        
        // Размеры хотбара с учетом масштабирования
        float hotbarWidth = Hotbar.HOTBAR_WIDTH * Hotbar.HOTBAR_SCALE;
        float hotbarHeight = Hotbar.HOTBAR_HEIGHT * Hotbar.HOTBAR_SCALE;
        
        // Преобразование в нормализованные координаты экрана (-1 to 1)
        float scaleX = hotbarWidth / screenWidth;
        float scaleY = hotbarHeight / screenHeight;
        
        // Позиция в нормализованных координатах
        float posX = (2.0f * hotbarX / screenWidth) - 1.0f + scaleX;
        float posY = 1.0f - (2.0f * hotbarY / screenHeight) - scaleY;
        
        uiShader.setUniform("scale", scaleX, scaleY, 0.0f, 0.0f);
        uiShader.setUniform("position_offset", posX, posY, 0.0f, 0.0f);
        
        // Используем всю текстуру (отдельный файл 182x22)
        uiShader.setUniform("uvOffset", 0.0f, 0.0f, 0.0f, 0.0f);
        uiShader.setUniform("uvScale", 1.0f, 1.0f, 0.0f, 0.0f);
        
        glBindVertexArray(quadVAO);
        glDrawElements(GL_TRIANGLES, QUAD_INDICES.length, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }
    
    private void renderHotbarSelection(int screenWidth, int screenHeight) {
        hotbarSelectionTexture.bind();
        
        // Позиция выделения на экране
        int selectionX = hotbar.getSelectionScreenX(screenWidth);
        int selectionY = hotbar.getSelectionScreenY(screenHeight);
        
        // Размеры выделения с учетом масштабирования
        float selectionWidth = Hotbar.HOTBAR_SELECTION_WIDTH * Hotbar.HOTBAR_SCALE;
        float selectionHeight = Hotbar.HOTBAR_SELECTION_HEIGHT * Hotbar.HOTBAR_SCALE;
        
        // Преобразование в нормализованные координаты экрана
        float scaleX = selectionWidth / screenWidth;
        float scaleY = selectionHeight / screenHeight;
        
        float posX = (2.0f * selectionX / screenWidth) - 1.0f + scaleX;
        float posY = 1.0f - (2.0f * selectionY / screenHeight) - scaleY;
        
        uiShader.setUniform("scale", scaleX, scaleY, 0.0f, 0.0f);
        uiShader.setUniform("position_offset", posX, posY, 0.0f, 0.0f);
        
        // Используем всю текстуру (отдельный файл 24x24)
        uiShader.setUniform("uvOffset", 0.0f, 0.0f, 0.0f, 0.0f);
        uiShader.setUniform("uvScale", 1.0f, 1.0f, 0.0f, 0.0f);
        
        glBindVertexArray(quadVAO);
        glDrawElements(GL_TRIANGLES, QUAD_INDICES.length, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }
    
    public void cleanup() {
        if (uiShader != null) {
            uiShader.cleanup();
        }
        if (crosshairTexture != null) {
            crosshairTexture.cleanup();
        }
        if (hotbarTexture != null) {
            hotbarTexture.cleanup();
        }
        if (hotbarSelectionTexture != null) {
            hotbarSelectionTexture.cleanup();
        }
        if (fontRenderer != null) {
            fontRenderer.cleanup();
        }
        glDeleteVertexArrays(quadVAO);
        glDeleteBuffers(quadVBO);
        glDeleteBuffers(quadEBO);
    }
}
