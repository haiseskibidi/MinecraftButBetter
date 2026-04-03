package com.za.minecraft.engine.graphics.ui.crosshair;

import com.za.minecraft.engine.graphics.Shader;
import org.joml.Vector4f;
import org.joml.Vector2f;
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
 * Supports smooth transitions and state entry animations.
 * Added per-quadrant displacement for rigid-group recoil animations.
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

    public void render(CrosshairManager manager, Shader uiShader, int sw, int sh, float alpha) {
        CrosshairDefinition current = CrosshairRegistry.get(manager.getCurrentId());
        float transition = manager.getTransitionFactor();
        float stateTimer = manager.getStateTimer();
        var input = com.za.minecraft.engine.core.GameLoop.getInstance().getInputManager();
        float progress = input.getBreakingProgress();
        float hitPulse = input.getMiningController().getHitPulse();

        uiShader.use();
        uiShader.setInt("useTexture", 0);
        uiShader.setInt("useArray", 0);
        uiShader.setInt("isSlot", 0);
        uiShader.setInt("isGrayscale", 0);
        uiShader.setUniform("uvOffset", 0.0f, 0.0f, 0.0f, 0.0f);
        uiShader.setUniform("uvScale", 1.0f, 1.0f, 0.0f, 0.0f);
        uiShader.setFloat("uHitPulse", hitPulse);

        glDisable(GL_CULL_FACE);

        // 1. Render Old Crosshair (Fading out)
        if (transition < 1.0f) {
            CrosshairDefinition last = CrosshairRegistry.get(manager.getLastId());
            if (last != null) {
                renderSingle(last, uiShader, sw, sh, alpha * (1.0f - transition), 0.0f, 1.0f - (transition * 0.5f), false);
            }
        }

        // 2. Render Current Crosshair (Fading in + Entry Animation)
        if (current != null) {
            float bounce = 0.0f;
            if (stateTimer < current.getBounceDuration()) {
                float t = stateTimer / current.getBounceDuration();
                bounce = (float)Math.sin(t * Math.PI) * current.getBounceScale() * (1.0f - t);
            }
            
            float scaleMod = transition; 
            if (transition >= 1.0f) scaleMod = 1.0f + bounce;
            
            boolean isMining = manager.getCurrentState() == CrosshairManager.State.MINING;
            renderSingle(current, uiShader, sw, sh, alpha * transition, progress, scaleMod, isMining);
        }

        // --- RESET ALL STATE TO PREVENT BREAKING UI ---
        uiShader.setInt("isCrosshair", 0);
        uiShader.setFloat("uProgress", 0.0f);
        uiShader.setFloat("uHitPulse", 0.0f);
        uiShader.setUniform("tintColor", 1.0f, 1.0f, 1.0f, 1.0f);
        glEnable(GL_CULL_FACE);
    }

    private void renderSingle(CrosshairDefinition def, Shader uiShader, int sw, int sh, float alpha, float progress, float scaleMod, boolean isMining) {
        CrosshairMesh mesh = meshCache.computeIfAbsent(def.getIdentifier(), k -> createMesh(def));
        if (mesh == null || mesh.vertexCount == 0) return;
        
        Vector4f color = def.getColor();
        uiShader.setUniform("tintColor", color.x, color.y, color.z, color.w * alpha);
        uiShader.setInt("isCrosshair", isMining ? 1 : 0);
        uiShader.setFloat("uProgress", progress);

        float basePixelSize = 4.0f; 
        float scaleX = (basePixelSize * def.getScale() * scaleMod) / sw;
        float scaleY = (basePixelSize * def.getScale() * scaleMod) / sh;

        uiShader.setUniform("scale", scaleX, scaleY, 0.0f, 0.0f);
        uiShader.setUniform("position_offset", 0.0f, 0.0f, 0.0f, 0.0f);

        glBindVertexArray(mesh.vao);
        glDrawArrays(GL_TRIANGLES, 0, mesh.vertexCount);
        glBindVertexArray(0);
    }

    private CrosshairMesh createMesh(CrosshairDefinition def) {
        List<String> matrix = def.getMatrix();
        if (matrix == null || matrix.isEmpty()) return null;

        int rows = matrix.size();
        int cols = matrix.get(0).length();
        
        int activePixels = 0;
        for (String row : matrix) {
            for (char c : row.toCharArray()) {
                if (c == '1') activePixels++;
            }
        }

        if (activePixels == 0) return null;

        float[] vertices = new float[activePixels * 6 * 4]; 
        int vIdx = 0;

        float startX = -cols / 2.0f;
        float startY = rows / 2.0f;

        for (int r = 0; r < rows; r++) {
            String row = matrix.get(r);
            for (int c = 0; row != null && c < row.length(); c++) {
                if (row.charAt(c) == '1') {
                    float x = (startX + c);
                    float y = (startY - r);
                    
                    // --- QUADRANT-BASED DISPLACEMENT ---
                    // Instead of normalizing each pixel's individual center, 
                    // we normalize the SIGN of its quadrant. This forces all pixels 
                    // in a corner/quadrant to move together as a single rigid unit.
                    float pxCenter = x + 0.5f;
                    float pyCenter = y - 0.5f;
                    
                    Vector2f disp = new Vector2f();
                    // Determine horizontal direction
                    if (pxCenter < -0.1f) disp.x = -1.0f;
                    else if (pxCenter > 0.1f) disp.x = 1.0f;
                    
                    // Determine vertical direction
                    if (pyCenter < -0.1f) disp.y = -1.0f;
                    else if (pyCenter > 0.1f) disp.y = 1.0f;
                    
                    if (disp.length() > 0.001f) {
                        disp.normalize();
                    }

                    // Vertex 1-6 (Two triangles)
                    addVertex(vertices, vIdx, x, y, disp); vIdx += 4;
                    addVertex(vertices, vIdx, x, y - 1, disp); vIdx += 4;
                    addVertex(vertices, vIdx, x + 1, y - 1, disp); vIdx += 4;
                    
                    addVertex(vertices, vIdx, x + 1, y - 1, disp); vIdx += 4;
                    addVertex(vertices, vIdx, x + 1, y, disp); vIdx += 4;
                    addVertex(vertices, vIdx, x, y, disp); vIdx += 4;
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

        // Attribute 0: Position (vec2)
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        
        // Attribute 1: Displacement (vec2)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
        return cm;
    }

    private void addVertex(float[] vertices, int offset, float x, float y, Vector2f disp) {
        vertices[offset] = x;
        vertices[offset + 1] = y;
        vertices[offset + 2] = disp.x;
        vertices[offset + 3] = disp.y;
    }

    public void cleanup() {
        for (CrosshairMesh mesh : meshCache.values()) {
            mesh.cleanup();
        }
        meshCache.clear();
    }
}
