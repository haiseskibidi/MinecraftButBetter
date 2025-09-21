package com.za.minecraft.engine.graphics;

import com.za.minecraft.utils.Logger;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class DebugRenderer {
    private Shader uiShader;
    private Mesh backgroundMesh;
    private Matrix4f projectionMatrix;
    private boolean initialized = false;
    
    public void init() {
        try {
            uiShader = new Shader("shaders/ui_vertex.glsl", "shaders/ui_fragment.glsl");
            projectionMatrix = new Matrix4f();
            
            // Создаем простой квад для фона
            float[] positions = {
                0.0f, 0.0f, 0.0f,   // bottom-left
                1.0f, 0.0f, 0.0f,   // bottom-right
                1.0f, 1.0f, 0.0f,   // top-right
                0.0f, 1.0f, 0.0f    // top-left
            };
            
            float[] texCoords = {
                0.0f, 1.0f,
                1.0f, 1.0f,
                1.0f, 0.0f,
                0.0f, 0.0f
            };
            
            float[] normals = {
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f,
                0.0f, 0.0f, 1.0f
            };
            
            int[] indices = {0, 1, 2, 2, 3, 0};
            
            backgroundMesh = new Mesh(positions, texCoords, normals, indices);
            initialized = true;
            Logger.info("DebugRenderer initialized");
        } catch (Exception e) {
            Logger.error("Failed to initialize DebugRenderer: " + e.getMessage(), e);
        }
    }
    
    public void renderFPS(float fps, int windowWidth, int windowHeight) {
        if (!initialized) return;
        
        // Настраиваем 2D проекцию
        projectionMatrix.setOrtho2D(0, windowWidth, windowHeight, 0);
        
        // Отключаем depth test и включаем blending для прозрачности
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        uiShader.use();
        uiShader.setUniform("projection", projectionMatrix);
        
        // Рисуем черную полупрозрачную подложку
        Matrix4f modelMatrix = new Matrix4f()
            .translate(5, 5, 0)  // позиция
            .scale(120, 20, 1);  // размер
        
        uiShader.setUniform("model", modelMatrix);
        uiShader.setUniform("color", 0.0f, 0.0f, 0.0f, 0.7f); // черный полупрозрачный
        
        backgroundMesh.render();
        
        // Включаем обратно depth test и отключаем blending
        glDisable(GL_BLEND);
        glEnable(GL_DEPTH_TEST);
        
        Shader.unbind();
    }
    
    public void cleanup() {
        if (uiShader != null) {
            uiShader.cleanup();
        }
        if (backgroundMesh != null) {
            backgroundMesh.cleanup();
        }
    }
}
