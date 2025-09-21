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
    private int quadVAO;
    private int quadVBO;
    private int quadEBO;
    
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
        
        crosshairTexture = new Texture("src/main/resources/textures/crosshair.png");
        
        createQuad();
        
        uiShader.use();
        uiShader.setInt("textureSampler", 0);
        
        Logger.info("UI Renderer initialized");
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
        
        // Размер прицела (в пикселях)
        float crosshairSize = 16.0f;
        float scaleX = crosshairSize / screenWidth;
        float scaleY = crosshairSize / screenHeight;
        
        uiShader.setUniform("scale", scaleX, scaleY, 0.0f, 0.0f);
        uiShader.setUniform("position_offset", 0.0f, 0.0f, 0.0f, 0.0f);
        
        // UV координаты для отдельного файла прицела (используем всю текстуру)
        float u = 0.0f;        // X позиция в текстуре
        float v = 0.0f;        // Y позиция в текстуре
        float uvSize = 1.0f;   // Используем всю текстуру (16x16)
        
        uiShader.setUniform("uvOffset", u, v, 0.0f, 0.0f);
        uiShader.setUniform("uvScale", uvSize, uvSize, 0.0f, 0.0f);
        
        crosshairTexture.bind();
        
        glBindVertexArray(quadVAO);
        glDrawElements(GL_TRIANGLES, QUAD_INDICES.length, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
        
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }
    
    public void cleanup() {
        if (uiShader != null) {
            uiShader.cleanup();
        }
        if (crosshairTexture != null) {
            crosshairTexture.cleanup();
        }
        glDeleteVertexArrays(quadVAO);
        glDeleteBuffers(quadVBO);
        glDeleteBuffers(quadEBO);
    }
}
