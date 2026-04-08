package com.za.zenith.engine.graphics.ui.renderers;

import com.za.zenith.engine.graphics.ui.PauseMenu;
import com.za.zenith.engine.graphics.ui.UIRenderer;
import com.za.zenith.utils.I18n;

import static org.lwjgl.opengl.GL11.*;

public class MenuRenderer {
    private final UIRenderer renderer;

    public MenuRenderer(UIRenderer renderer) {
        this.renderer = renderer;
    }

    public void renderPauseMenu(PauseMenu pauseMenu, int screenWidth, int screenHeight) {
        if (pauseMenu == null || !pauseMenu.isVisible()) return;
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        renderer.getPrimitivesRenderer().renderDarkenedBackground();
        renderMenuButtons(pauseMenu, screenWidth, screenHeight);
        
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }
    
    private void renderMenuButtons(PauseMenu pauseMenu, int screenWidth, int screenHeight) {
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        int buttonWidth = pauseMenu.getButtonWidth();
        int buttonHeight = pauseMenu.getButtonHeight();
        int spacing = pauseMenu.getButtonSpacing();
        
        renderButton(centerX, centerY - spacing, buttonWidth, buttonHeight, screenWidth, screenHeight, 
            I18n.get("menu.resume"), 0.4f, 0.7f, 1.0f);
        renderButton(centerX, centerY + spacing, buttonWidth, buttonHeight, screenWidth, screenHeight, 
            I18n.get("menu.exit"), 1.0f, 0.4f, 0.4f);
    }
    
    private void renderButton(int x, int y, int width, int height, int screenWidth, int screenHeight, String text, float r, float g, float b) {
        renderer.getPrimitivesRenderer().renderHighlight(x, y, width, screenWidth, screenHeight, r, g, b, 0.9f); // Just reuse highlight or rect logic for now. Wait, original renderButton used specific scale based on width/height.

        // Replicating original renderButton exact logic using primitives
        com.za.zenith.engine.graphics.Shader uiShader = renderer.getShader();
        uiShader.use();
        uiShader.setInt("useTexture", 0);
        uiShader.setInt("isSlot", 1); // Use SDF for buttons too
        float scaleX = (float)width / screenWidth;
        float scaleY = (float)height / screenHeight;
        float posX = (2.0f * x / screenWidth) - 1.0f;
        float posY = 1.0f - (2.0f * y / screenHeight);
        uiShader.setUniform("scale", scaleX, scaleY, 0.0f, 0.0f);
        uiShader.setUniform("position_offset", posX, posY, 0.0f, 0.0f);
        uiShader.setUniform("uvOffset", 0.0f, 0.0f, 0.0f, 0.0f);
        uiShader.setUniform("uvScale", 1.0f, 1.0f, 0.0f, 0.0f);
        uiShader.setUniform("tintColor", r, g, b, 0.9f);
        
        org.lwjgl.opengl.GL30.glBindVertexArray(renderer.getQuadVAO());
        glDrawElements(GL_TRIANGLES, renderer.getQuadIndicesLength(), GL_UNSIGNED_INT, 0);
        org.lwjgl.opengl.GL30.glBindVertexArray(0);
        uiShader.setInt("isSlot", 0);
        
        if (text != null && !text.isEmpty()) {
            int textSize = 16;
            int textWidth = renderer.getFontRenderer().getStringWidth(text, textSize);
            int textX = x - textWidth / 2;
            int textY = y - textSize / 2;
            renderer.getFontRenderer().drawString(text, textX, textY, textSize, screenWidth, screenHeight);
        }
    }
}
