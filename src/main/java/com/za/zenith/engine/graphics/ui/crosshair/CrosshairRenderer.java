package com.za.zenith.engine.graphics.ui.crosshair;

import com.za.zenith.engine.graphics.Shader;
import com.za.zenith.engine.core.GameLoop;
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
 * Now supports data-driven animations: recoil (hits) and spread (progress).
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
    private Shader crosshairShader;

    public void render(CrosshairManager manager, Shader fallbackShader, int sw, int sh, float alpha) {
        if (crosshairShader == null) {
            crosshairShader = new Shader(
                "src/main/resources/shaders/crosshair_vertex.glsl",
                "src/main/resources/shaders/crosshair_fragment.glsl"
            );
        }

        CrosshairDefinition current = CrosshairRegistry.get(manager.getCurrentId());
        float transition = manager.getTransitionFactor();
        float stateTimer = manager.getStateTimer();
        
        var game = GameLoop.getInstance();
        var mining = game.getInputManager().getMiningController();
        float hitPulse = mining.getHitPulse();
        float progress = mining.getBreakingProgress();

        crosshairShader.use();
        crosshairShader.setFloat("uHitPulse", hitPulse);
        crosshairShader.setFloat("uProgress", progress);

        glDisable(GL_CULL_FACE);

        if (transition < 1.0f) {
            CrosshairDefinition last = CrosshairRegistry.get(manager.getLastId());
            if (last != null) {
                renderSingle(last, crosshairShader, sw, sh, alpha * (1.0f - transition), 1.0f - (transition * 0.5f));
            }
        }

        if (current != null) {
            float bounce = 0.0f;
            if (stateTimer < current.getBounceDuration()) {
                float t = stateTimer / current.getBounceDuration();
                bounce = (float)Math.sin(t * Math.PI) * current.getBounceScale() * (1.0f - t);
            }
            
            float scaleMod = transition; 
            if (transition >= 1.0f) scaleMod = 1.0f + bounce;
            
            renderSingle(current, crosshairShader, sw, sh, alpha * transition, scaleMod);
        }

        glEnable(GL_CULL_FACE);
    }

    private void renderSingle(CrosshairDefinition def, Shader shader, int sw, int sh, float alpha, float scaleMod) {
        CrosshairMesh mesh = meshCache.computeIfAbsent(def.getIdentifier(), k -> createMesh(def));
        if (mesh == null || mesh.vertexCount == 0) return;
        
        Vector4f color = def.getColor();
        shader.setUniform("tintColor", color.x, color.y, color.z, color.w * alpha);
        
        // --- DATA-DRIVEN ANIMATION UNIFORMS ---
        shader.setFloat("uRecoilScale", def.getRecoilScale());
        shader.setFloat("uSpreadScale", def.getSpreadScale());

        float basePixelSize = 4.0f; 
        float scaleX = (basePixelSize * def.getScale() * scaleMod) / sw;
        float scaleY = (basePixelSize * def.getScale() * scaleMod) / sh;

        shader.setUniform("scale", scaleX, scaleY, 0.0f, 0.0f);
        shader.setUniform("position_offset", 0.0f, 0.0f, 0.0f, 0.0f);

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
                    
                    float pxCenter = x + 0.5f;
                    float pyCenter = y - 0.5f;
                    
                    Vector2f disp = calculateDisplacement(pxCenter, pyCenter);

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

        // Location 0: position (vec2)
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        
        // Location 1: displacement (vec2)
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

    private Vector2f calculateDisplacement(float pxCenter, float pyCenter) {
        Vector2f disp = new Vector2f();
        // Determine quadrant
        if (pxCenter < -0.1f) disp.x = -1.0f;
        else if (pxCenter > 0.1f) disp.x = 1.0f;
        
        if (pyCenter < -0.1f) disp.y = -1.0f;
        else if (pyCenter > 0.1f) disp.y = 1.0f;
        
        if (disp.length() > 0.001f) {
            disp.normalize();
        }
        return disp;
    }

    public void cleanup() {
        if (crosshairShader != null) crosshairShader.cleanup();
        for (CrosshairMesh mesh : meshCache.values()) {
            mesh.cleanup();
        }
        meshCache.clear();
    }
}
