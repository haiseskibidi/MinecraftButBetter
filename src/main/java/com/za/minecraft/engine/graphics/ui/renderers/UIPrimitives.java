package com.za.minecraft.engine.graphics.ui.renderers;

import com.za.minecraft.engine.graphics.Shader;
import com.za.minecraft.engine.graphics.Texture;
import com.za.minecraft.engine.graphics.ui.UIRenderer;
import com.za.minecraft.utils.Logger;

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
        
        float scaleX = (float)width / sw;
        float scaleY = (float)height / sh;
        float posX = (2.0f * x / sw) - 1.0f + scaleX;
        float posY = 1.0f - (2.0f * y / sh) - scaleY;

        uiShader.setUniform("scale", scaleX, scaleY, 0.0f, 0.0f);
        uiShader.setUniform("position_offset", posX, posY, 0.0f, 0.0f);
        uiShader.setUniform("tintColor", r, g, b, a);

        glBindVertexArray(renderer.getQuadVAO());
        glDrawElements(GL_TRIANGLES, renderer.getQuadIndicesLength(), GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    public void renderGroupBackground(int x, int y, int width, int height, com.za.minecraft.engine.graphics.ui.GUIConfig.BackgroundConfig bg) {
        int sw = com.za.minecraft.engine.core.GameLoop.getInstance().getWindow().getWidth();
        int sh = com.za.minecraft.engine.core.GameLoop.getInstance().getWindow().getHeight();
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
        uiShader.setUniform("tintColor", r, g, b, a);
        
        glBindTexture(GL_TEXTURE_2D, 0); 
        glBindVertexArray(renderer.getQuadVAO());
        glDrawElements(GL_TRIANGLES, renderer.getQuadIndicesLength(), GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
        uiShader.setInt("isSlot", 0);
    }

    public void renderDarkenedBackground() {
        Shader uiShader = renderer.getShader();
        uiShader.use();
        uiShader.setInt("useTexture", 0);
        uiShader.setInt("isSlot", 0);
        uiShader.setUniform("scale", 1.0f, 1.0f, 0.0f, 0.0f);
        uiShader.setUniform("position_offset", 0.0f, 0.0f, 0.0f, 0.0f);
        uiShader.setUniform("uvOffset", 0.0f, 0.0f, 0.0f, 0.0f);
        uiShader.setUniform("uvScale", 1.0f, 1.0f, 0.0f, 0.0f);
        uiShader.setUniform("tintColor", 0.0f, 0.0f, 0.0f, 0.6f);
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindVertexArray(renderer.getQuadVAO());
        glDrawElements(GL_TRIANGLES, renderer.getQuadIndicesLength(), GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
        uiShader.setUniform("tintColor", 1.0f, 1.0f, 1.0f, 1.0f);
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
