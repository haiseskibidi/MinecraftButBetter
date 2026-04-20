package com.za.zenith.engine.graphics.ui.blueprints;

import com.za.zenith.engine.graphics.Shader;
import com.za.zenith.utils.Identifier;
import org.joml.Vector2f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class BlueprintRenderer {
    private static class BlueprintMesh {
        Map<String, LayerMesh> layerMeshes = new HashMap<>();
        void cleanup() {
            for (LayerMesh lm : layerMeshes.values()) {
                glDeleteVertexArrays(lm.vao);
                glDeleteBuffers(lm.vbo);
            }
        }
    }

    private static class LayerMesh {
        int vao, vbo, vertexCount;
    }

    private final Map<Identifier, BlueprintMesh> meshCache = new HashMap<>();
    private Shader blueprintShader;

    public void render(Identifier id, int x, int y, int size, int sw, int sh, float[] triggers) {
        if (blueprintShader == null) {
            blueprintShader = new Shader(
                "src/main/resources/shaders/blueprint_vertex.glsl",
                "src/main/resources/shaders/blueprint_fragment.glsl"
            );
        }

        GraphicBlueprint blueprint = BlueprintRegistry.get(id);
        if (blueprint == null) return;

        BlueprintMesh mesh = meshCache.computeIfAbsent(id, k -> createMesh(blueprint));
        if (mesh == null) return;

        blueprintShader.use();
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        for (int i = 0; i < 4; i++) {
            float val = (triggers != null && i < triggers.length) ? triggers[i] : 0.0f;
            blueprintShader.setFloat("uTrigger" + i, val);
        }
        blueprintShader.setFloat("uTime", (float)org.lwjgl.glfw.GLFW.glfwGetTime());

        float pixelScale = (float)size / (float)blueprint.getSize();
        float scaleX = pixelScale / sw;
        float scaleY = pixelScale / sh;
        
        float centerX = x + size / 2.0f;
        float centerY = y + size / 2.0f;
        float posX = (2.0f * centerX / sw) - 1.0f;
        float posY = 1.0f - (2.0f * centerY / sh);

        blueprintShader.setUniform("scale", scaleX, scaleY, 0.0f, 0.0f);
        blueprintShader.setUniform("position_offset", posX, posY, 0.0f, 0.0f);

        int elementIdx = 0;
        for (GraphicBlueprint.Element el : blueprint.getElements()) {
            Vector4f color = el.getVectorColor();
            blueprintShader.setUniform("tintColor", color.x, color.y, color.z, color.w);
            
            if (el.animation != null) {
                blueprintShader.setInt("uTriggerSlot", el.animation.triggerSlot);
                blueprintShader.setFloat("uIntensity", el.animation.intensity);
                blueprintShader.setInt("uAnimType", getAnimTypeInt(el.animation.type));
            } else {
                blueprintShader.setInt("uAnimType", 0);
            }

            int shapeType = getShapeType(el.type);
            blueprintShader.setInt("uType", shapeType);

            if (shapeType == 0) { // MATRIX
                LayerMesh lm = mesh.layerMeshes.get("el_" + elementIdx);
                if (lm != null) {
                    float matrixScaleX = ((float)size / blueprint.getSize()) / sw;
                    float matrixScaleY = ((float)size / blueprint.getSize()) / sh;
                    blueprintShader.setUniform("scale", matrixScaleX, matrixScaleY, 0.0f, 0.0f);
                    glBindVertexArray(lm.vao);
                    glDrawArrays(GL_TRIANGLES, 0, lm.vertexCount);
                    blueprintShader.setUniform("scale", scaleX, scaleY, 0.0f, 0.0f);
                }
            } else { // PROCEDURAL SHAPE
                if (shapeType == 1) { // Ring
                    blueprintShader.setFloat("uParam1", el.radius);
                    blueprintShader.setFloat("uParam2", el.thickness);
                    boolean isShaking = el.animation != null && "shake".equalsIgnoreCase(el.animation.type);
                    blueprintShader.setFloat("uParam4", isShaking ? 1.0f : 0.0f);
                } else if (shapeType == 2) { // Radial Lines
                    blueprintShader.setFloat("uParam1", el.innerRadius);
                    blueprintShader.setFloat("uParam2", el.outerRadius);
                    blueprintShader.setFloat("uParam3", (float)el.count);
                    blueprintShader.setFloat("uParam4", 0.0f);
                } else if (shapeType == 3) { // Rect
                    blueprintShader.setFloat("uParam1", 0.5f);
                    blueprintShader.setFloat("uParam2", 0.5f);
                    blueprintShader.setFloat("uParam4", 0.0f);
                } else if (shapeType == 4) { // Sonar
                    blueprintShader.setFloat("uParam1", el.radius); // Waves start from this radius
                    blueprintShader.setFloat("uParam4", 0.0f);
                } else if (shapeType == 5) { // Circle
                    blueprintShader.setFloat("uParam1", el.radius);
                    blueprintShader.setFloat("uParam2", el.thickness); // Use thickness for softness
                    blueprintShader.setFloat("uParam4", 0.0f);
                }

                int quadVAO = com.za.zenith.engine.core.GameLoop.getInstance().getRenderer().getUIRenderer().getQuadVAO();
                int quadCount = com.za.zenith.engine.core.GameLoop.getInstance().getRenderer().getUIRenderer().getQuadIndicesLength();
                float shapeScale = (float)blueprint.getSize();
                blueprintShader.setUniform("scale", shapeScale * scaleX, shapeScale * scaleY, 0.0f, 0.0f);
                glBindVertexArray(quadVAO);
                glDrawElements(GL_TRIANGLES, quadCount, GL_UNSIGNED_INT, 0);
                blueprintShader.setUniform("scale", scaleX, scaleY, 0.0f, 0.0f);
            }
            elementIdx++;
        }
        glBindVertexArray(0);
        glEnable(GL_DEPTH_TEST);
    }

    private int getShapeType(String type) {
        if ("ring".equalsIgnoreCase(type)) return 1;
        if ("radial_lines".equalsIgnoreCase(type)) return 2;
        if ("rect".equalsIgnoreCase(type)) return 3;
        if ("sonar".equalsIgnoreCase(type)) return 4;
        if ("circle".equalsIgnoreCase(type)) return 5;
        return 0; // Matrix
    }

    private int getAnimTypeInt(String type) {
        if ("expand".equalsIgnoreCase(type)) return 1;
        if ("pulse".equalsIgnoreCase(type)) return 2;
        if ("rotate".equalsIgnoreCase(type)) return 3;
        if ("shake".equalsIgnoreCase(type)) return 4;
        return 0;
    }

    private BlueprintMesh createMesh(GraphicBlueprint bp) {
        BlueprintMesh bm = new BlueprintMesh();
        int idx = 0;
        for (GraphicBlueprint.Element el : bp.getElements()) {
            if ("matrix".equalsIgnoreCase(el.type)) {
                LayerMesh lm = createLayerMesh(el);
                if (lm != null) bm.layerMeshes.put("el_" + idx, lm);
            }
            idx++;
        }
        return bm;
    }

    private LayerMesh createLayerMesh(GraphicBlueprint.Element layer) {
        java.util.List<String> matrix = layer.matrix;
        if (matrix == null || matrix.isEmpty()) return null;
        int rows = matrix.size();
        int maxCols = 0;
        for (String row : matrix) if (row != null) maxCols = Math.max(maxCols, row.length());
        if (maxCols == 0) return null;
        int activePixels = 0;
        for (String row : matrix) {
            if (row == null) continue;
            for (char c : row.toCharArray()) if (c != ' ') activePixels++;
        }
        if (activePixels == 0) return null;
        float[] vertices = new float[activePixels * 6 * 4]; 
        int vIdx = 0;
        float startX = -maxCols / 2.0f;
        float startY = rows / 2.0f;
        for (int r = 0; r < rows; r++) {
            String row = matrix.get(r);
            if (row == null) continue;
            for (int c = 0; c < row.length(); c++) {
                if (row.charAt(c) != ' ') {
                    float x = startX + c;
                    float y = startY - r;
                    Vector2f disp = new Vector2f(x + 0.5f, y - 0.5f);
                    if (disp.length() > 0.001f) disp.normalize();
                    addQuad(vertices, vIdx, x, y, disp);
                    vIdx += 24;
                }
            }
        }
        LayerMesh lm = new LayerMesh();
        lm.vertexCount = activePixels * 6;
        lm.vao = glGenVertexArrays();
        lm.vbo = glGenBuffers();
        glBindVertexArray(lm.vao);
        glBindBuffer(GL_ARRAY_BUFFER, lm.vbo);
        FloatBuffer buffer = MemoryUtil.memAllocFloat(vertices.length);
        buffer.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(buffer);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);
        glBindVertexArray(0);
        return lm;
    }

    private void addQuad(float[] v, int i, float x, float y, Vector2f d) {
        float[][] pts = {{x, y}, {x, y-1}, {x+1, y-1}, {x+1, y}};
        int[] ind = {0, 1, 2, 2, 3, 0};
        for (int j = 0; j < 6; j++) {
            v[i + j*4] = pts[ind[j]][0];
            v[i + j*4 + 1] = pts[ind[j]][1];
            v[i + j*4 + 2] = d.x;
            v[i + j*4 + 3] = d.y;
        }
    }

    public void cleanup() {
        if (blueprintShader != null) blueprintShader.cleanup();
        for (BlueprintMesh bm : meshCache.values()) bm.cleanup();
        meshCache.clear();
    }
}
