package com.za.minecraft.engine.graphics;

import com.za.minecraft.utils.Logger;
import org.joml.Vector2f;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.*;

public class PostProcessor {
    private Shader fxaaShader;
    private Shader passthroughShader;
    private int quadVAO;
    private int quadVBO;
    private int quadEBO;
    
    private static final float[] QUAD_VERTICES = {
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
        fxaaShader = new Shader(
            "src/main/resources/shaders/fxaa_vertex.glsl",
            "src/main/resources/shaders/fxaa_fragment.glsl"
        );
        
        passthroughShader = new Shader(
            "src/main/resources/shaders/fxaa_vertex.glsl",
            "src/main/resources/shaders/passthrough_fragment.glsl"
        );
        
        createQuad();
        
        fxaaShader.use();
        fxaaShader.setInt("screenTexture", 0);
        
        passthroughShader.use();
        passthroughShader.setInt("screenTexture", 0);
        
        Logger.info("PostProcessor initialized with FXAA and passthrough shaders");
    }
    
    private void createQuad() {
        quadVAO = glGenVertexArrays();
        quadVBO = glGenBuffers();
        quadEBO = glGenBuffers();
        
        glBindVertexArray(quadVAO);
        
        FloatBuffer vertexBuffer = memAllocFloat(QUAD_VERTICES.length);
        vertexBuffer.put(QUAD_VERTICES).flip();
        
        glBindBuffer(GL_ARRAY_BUFFER, quadVBO);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
        
        IntBuffer indexBuffer = memAllocInt(QUAD_INDICES.length);
        indexBuffer.put(QUAD_INDICES).flip();
        
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, quadEBO);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);
        
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);
        
        glBindVertexArray(0);
        
        memFree(vertexBuffer);
        memFree(indexBuffer);
    }
    
    public void processFXAA(int colorTexture, int screenWidth, int screenHeight) {
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        
        fxaaShader.use();
        fxaaShader.setVector2f("screenSize", new Vector2f(screenWidth, screenHeight));
        
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, colorTexture);
        
        glBindVertexArray(quadVAO);
        glDrawElements(GL_TRIANGLES, QUAD_INDICES.length, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
        
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
    }
    
    public void processPassthrough(int colorTexture) {
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        
        passthroughShader.use();
        
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, colorTexture);
        
        glBindVertexArray(quadVAO);
        glDrawElements(GL_TRIANGLES, QUAD_INDICES.length, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
        
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
    }
    
    public void cleanup() {
        if (fxaaShader != null) {
            fxaaShader.cleanup();
        }
        if (passthroughShader != null) {
            passthroughShader.cleanup();
        }
        glDeleteVertexArrays(quadVAO);
        glDeleteBuffers(quadVBO);
        glDeleteBuffers(quadEBO);
    }
}
