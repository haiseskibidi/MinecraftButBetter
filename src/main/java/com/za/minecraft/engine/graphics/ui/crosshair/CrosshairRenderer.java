package com.za.minecraft.engine.graphics.ui.crosshair;

import com.za.minecraft.engine.graphics.Shader;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

/**
 * Procedural renderer for matrix-based crosshairs.
 * Generates and caches meshes for each crosshair definition.
 */
public class CrosshairRenderer {
    private static class CrosshairMesh {
        int vao, vbo, vertexCount;
        void cleanup() {
            glDeleteVertexArrays(vao);
            glDeleteBuffers(vbo);
        }
    }

    private final Map<String, CrosshairMesh> meshCache = new HashMap<>();

    public void render(CrosshairDefinition def, Shader uiShader, int sw, int sh, float alpha, float progress, CrosshairManager.State state) {
        uiShader.use();
        uiShader.setInt("useTexture", 0);
        uiShader.setInt("useArray", 0);
        uiShader.setInt("isSlot", 0);
        uiShader.setInt("isGrayscale", 0);
        
        // Effects only for MINING state
        boolean isMining = (state == CrosshairManager.State.MINING);
        uiShader.setInt("isCrosshair", isMining ? 1 : 0);
        uiShader.setFloat("uProgress", progress);
        
        uiShader.setUniform("uvOffset", 0.0f, 0.0f, 0.0f, 0.0f);
        uiShader.setUniform("uvScale", 1.0f, 1.0f, 0.0f, 0.0f);

        if (def == null) return;

        CrosshairMesh mesh = meshCache.computeIfAbsent(def.getIdentifier(), k -> createMesh(def));
        if (mesh == null || mesh.vertexCount == 0) return;
        
        Vector4f color = def.getColor();
        uiShader.setUniform("tintColor", color.x, color.y, color.z, color.w * alpha);

        // One "matrix pixel" = 4 actual screen pixels (2x2 pixels if scale is 1.0)
        float basePixelSize = 4.0f; 
        float scaleX = (basePixelSize * def.getScale()) / sw;
        float scaleY = (basePixelSize * def.getScale()) / sh;

        uiShader.setUniform("scale", scaleX, scaleY, 0.0f, 0.0f);
        uiShader.setUniform("position_offset", 0.0f, 0.0f, 0.0f, 0.0f);

        glDisable(GL_CULL_FACE);
        glBindVertexArray(mesh.vao);
        glDrawArrays(GL_TRIANGLES, 0, mesh.vertexCount);
        glBindVertexArray(0);
        glEnable(GL_CULL_FACE);
        
        uiShader.setInt("isCrosshair", 0);
        uiShader.setFloat("uProgress", 0.0f);
    }

    private CrosshairMesh createMesh(CrosshairDefinition def) {
        List<String> matrix = def.getMatrix();
        if (matrix == null || matrix.isEmpty()) return null;

        int rows = matrix.size();
        int cols = matrix.get(0).length();
        
        // Count active pixels
        int activePixels = 0;
        for (String row : matrix) {
            for (char c : row.toCharArray()) {
                if (c == '1') activePixels++;
            }
        }

        if (activePixels == 0) return null;

        float[] vertices = new float[activePixels * 6 * 2]; 
        int vIdx = 0;

        float startX = -cols / 2.0f;
        float startY = rows / 2.0f;

        for (int r = 0; r < rows; r++) {
            String row = matrix.get(r);
            for (int c = 0; row != null && c < row.length(); c++) {
                if (row.charAt(c) == '1') {
                    float x = (startX + c);
                    float y = (startY - r);
                    
                    // CCW (Counter-Clockwise) order for triangles
                    // Quad: (x, y) [TL], (x, y-1) [BL], (x+1, y-1) [BR], (x+1, y) [TR]
                    
                    // Triangle 1: TL -> BL -> BR
                    vertices[vIdx++] = x;     vertices[vIdx++] = y;
                    vertices[vIdx++] = x;     vertices[vIdx++] = y - 1;
                    vertices[vIdx++] = x + 1; vertices[vIdx++] = y - 1;
                    
                    // Triangle 2: BR -> TR -> TL
                    vertices[vIdx++] = x + 1; vertices[vIdx++] = y - 1;
                    vertices[vIdx++] = x + 1; vertices[vIdx++] = y;
                    vertices[vIdx++] = x;     vertices[vIdx++] = y;
                }
            }
        }

        CrosshairMesh cm = new CrosshairMesh();
        cm.vertexCount = activePixels * 6;
        cm.vao = glGenVertexArrays();
        cm.vbo = glGenBuffers();

        glBindVertexArray(cm.vao);
        glBindBuffer(GL_ARRAY_BUFFER, cm.vbo);
        
        FloatBuffer buffer = MemoryUtil.memAllocFloat(vertices.length);
        buffer.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(buffer);

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);

        glBindVertexArray(0);
        return cm;
    }

    public void cleanup() {
        for (CrosshairMesh mesh : meshCache.values()) {
            mesh.cleanup();
        }
        meshCache.clear();
    }
}
