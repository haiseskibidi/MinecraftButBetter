package com.za.zenith.engine.graphics.ui.renderers;

import com.za.zenith.engine.graphics.Shader;
import com.za.zenith.engine.graphics.Texture;
import com.za.zenith.engine.graphics.ui.UIRenderer;
import com.za.zenith.utils.Logger;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

public class UIPrimitives {
    private final UIRenderer renderer;
    private final Map<String, Texture> externalTextures = new HashMap<>();

    public UIPrimitives(UIRenderer renderer) {
        this.renderer = renderer;
    }

    public void renderRect(int x, int y, int width, int height, int sw, int sh, float r, float g, float b, float a) {
        Shader uiShader = renderer.getShader();
        uiShader.use();
        uiShader.setInt("useTexture", 0);
        uiShader.setInt("isSlot", 0);
        uiShader.setInt("isGradient", 0);
        
        float scaleX = (float)width / sw;
        float scaleY = (float)height / sh;
        float posX = (2.0f * x / sw) - 1.0f + scaleX;
        float posY = 1.0f - (2.0f * y / sh) - scaleY;

        uiShader.setUniform("scale", scaleX, scaleY, 0.0f, 0.0f);
        uiShader.setUniform("position_offset", posX, posY, 0.0f, 0.0f);
        uiShader.setUniform("uvOffset", 0.0f, 0.0f, 0.0f, 0.0f);
        uiShader.setUniform("uvScale", 1.0f, 1.0f, 0.0f, 0.0f);
        uiShader.setUniform("tintColor", r, g, b, a);

        glBindVertexArray(renderer.getQuadVAO());
        glDrawElements(GL_TRIANGLES, renderer.getQuadIndicesLength(), GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    public void renderGradientRect(int x, int y, int width, int height, int sw, int sh, float[] c1, float[] c2) {
        Shader uiShader = renderer.getShader();
        uiShader.use();
        uiShader.setInt("useTexture", 0);
        uiShader.setInt("isSlot", 0);
        uiShader.setInt("isGradient", 1);
        
        float scaleX = (float)width / sw;
        float scaleY = (float)height / sh;
        float posX = (2.0f * x / sw) - 1.0f + scaleX;
        float posY = 1.0f - (2.0f * y / sh) - scaleY;

        uiShader.setUniform("scale", scaleX, scaleY, 0.0f, 0.0f);
        uiShader.setUniform("position_offset", posX, posY, 0.0f, 0.0f);
        uiShader.setUniform("tintColor", c1[0], c1[1], c1[2], c1[3]);
        uiShader.setUniform("tintColor2", c2[0], c2[1], c2[2], c2[3]);

        glBindVertexArray(renderer.getQuadVAO());
        glDrawElements(GL_TRIANGLES, renderer.getQuadIndicesLength(), GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
        uiShader.setInt("isGradient", 0);
    }

    public void renderTextWithShadow(String text, int x, int y, int fontSize, int sw, int sh, float r, float g, float b, float a) {
        // Single crisp shadow layer, but darker (85% opacity instead of 60%), ignoring embedded color codes
        renderer.getFontRenderer().drawString(text, x + 1, y + 1, fontSize, sw, sh, 0.0f, 0.0f, 0.0f, a * 0.85f, true);
        // Main text
        renderer.getFontRenderer().drawString(text, x, y, fontSize, sw, sh, r, g, b, a, false);
    }

    public void renderGroupBackground(int x, int y, int width, int height, com.za.zenith.engine.graphics.ui.GUIConfig.BackgroundConfig bg) {
        int sw = com.za.zenith.engine.core.GameLoop.getInstance().getWindow().getWidth();
        int sh = com.za.zenith.engine.core.GameLoop.getInstance().getWindow().getHeight();
        int p = bg.padding;
        renderRect(x - p, y - p, width + p * 2, height + p * 2, sw, sh, bg.color[0], bg.color[1], bg.color[2], bg.color[3]);
    }

    public void renderHighlight(int x, int y, int size, int sw, int sh, float r, float g, float b, float a) {
        Shader uiShader = renderer.getShader();
        uiShader.use();
        uiShader.setInt("useTexture", 0);
        uiShader.setInt("isSlot", 1); // Use SDF for highlights too!
        uiShader.setUniform("uvOffset", 0.0f, 0.0f, 0.0f, 0.0f);
        uiShader.setUniform("uvScale", 1.0f, 1.0f, 0.0f, 0.0f);
        
        float scaleX = (float)size / sw;
        float scaleY = (float)size / sh;
        float posX = (2.0f * x / sw) - 1.0f + scaleX;
        float posY = 1.0f - (2.0f * y / sh) - scaleY;
        
        uiShader.setUniform("scale", scaleX, scaleY, 0.0f, 0.0f);
        uiShader.setUniform("position_offset", posX, posY, 0.0f, 0.0f);
        uiShader.setUniform("uvOffset", 0.0f, 0.0f, 0.0f, 0.0f);
        uiShader.setUniform("uvScale", 1.0f, 1.0f, 0.0f, 0.0f);
        uiShader.setUniform("tintColor", r, g, b, a);
        
        glBindTexture(GL_TEXTURE_2D, 0); 
        glBindVertexArray(renderer.getQuadVAO());
        glDrawElements(GL_TRIANGLES, renderer.getQuadIndicesLength(), GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
        uiShader.setInt("isSlot", 0);
    }

    public void renderDarkenedBackground() {
        int sw = com.za.zenith.engine.core.GameLoop.getInstance().getWindow().getWidth();
        int sh = com.za.zenith.engine.core.GameLoop.getInstance().getWindow().getHeight();
        renderRect(0, 0, sw, sh, sw, sh, 0.0f, 0.0f, 0.0f, 0.6f);
    }

    public void renderExternalImage(String path, int x, int y, int width, int height, int sw, int sh) {
        Texture tex = externalTextures.get(path);
        if (tex == null) {
            try {
                tex = new Texture("src/main/resources/" + path, false, false);
                externalTextures.put(path, tex);
            } catch (Exception e) {
                Logger.error("Failed to load external image: " + path);
                return;
            }
        }
        
        if (tex != null) {
            Shader uiShader = renderer.getShader();
            tex.bind();
            uiShader.use();
            uiShader.setInt("useTexture", 1);
            uiShader.setInt("useArray", 0);
            uiShader.setInt("isGrayscale", 0);
            uiShader.setInt("isSlot", 0);
            
            float scaleX = (float)width / sw;
            float scaleY = (float)height / sh;
            float posX = (2.0f * x / sw) - 1.0f + scaleX;
            float posY = 1.0f - (2.0f * y / sw) - scaleY;
            
            uiShader.setUniform("scale", scaleX, scaleY, 0.0f, 0.0f);
            uiShader.setUniform("position_offset", posX, posY, 0.0f, 0.0f);
            uiShader.setUniform("uvOffset", 0.0f, 0.0f, 0.0f, 0.0f);
            uiShader.setUniform("uvScale", 1.0f, 1.0f, 0.0f, 0.0f);
            uiShader.setUniform("tintColor", 1.0f, 1.0f, 1.0f, 1.0f);
            
            glBindVertexArray(renderer.getQuadVAO());
            glDrawElements(GL_TRIANGLES, renderer.getQuadIndicesLength(), GL_UNSIGNED_INT, 0);
            glBindVertexArray(0);
        }
    }
}


