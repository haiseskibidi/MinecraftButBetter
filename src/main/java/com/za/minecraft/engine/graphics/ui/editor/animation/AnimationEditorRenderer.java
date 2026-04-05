package com.za.minecraft.engine.graphics.ui.editor.animation;

import com.za.minecraft.engine.graphics.DynamicTextureAtlas;
import com.za.minecraft.engine.graphics.Shader;
import com.za.minecraft.engine.graphics.model.*;
import com.za.minecraft.world.items.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.opengl.GL11.*;

/**
 * Handles all 3D rendering for the Animation Studio.
 */
public class AnimationEditorRenderer {
    private final ViewmodelRenderer viewmodelRenderer = new ViewmodelRenderer();
    private final Shader shader;
    private com.za.minecraft.engine.graphics.Mesh gizmoMesh;
    private com.za.minecraft.engine.graphics.Mesh circleMesh;

    public AnimationEditorRenderer() {
        this.shader = new Shader("src/main/resources/shaders/viewmodel_vertex.glsl", "src/main/resources/shaders/viewmodel_fragment.glsl");
        initGizmoMesh();
    }

    private void initGizmoMesh() {
        // Line mesh
        float[] pos = { 0,0,0, 1,0,0 };
        float[] uv = { 0,0,0, 0,0,0 };
        float[] norm = { 0,1,0, 0,1,0 };
        float[] types = { 0, 0 };
        float[] neighbor = { 0, 0 };
        int[] ind = { 0, 1 };
        this.gizmoMesh = new com.za.minecraft.engine.graphics.Mesh(pos, uv, norm, types, neighbor, ind);

        // Circle mesh (32 segments)
        int segs = 32;
        float[] cPos = new float[(segs + 1) * 3];
        int[] cInd = new int[segs * 2];
        for (int i = 0; i <= segs; i++) {
            float ang = (float) (i * 2.0 * Math.PI / segs);
            cPos[i * 3] = 0;
            cPos[i * 3 + 1] = (float) Math.cos(ang);
            cPos[i * 3 + 2] = (float) Math.sin(ang);
            if (i < segs) {
                cInd[i * 2] = i;
                cInd[i * 2 + 1] = i + 1;
            }
        }
        float[] cUv = new float[cPos.length / 3 * 2];
        float[] cNorm = new float[cPos.length];
        float[] cTypes = new float[cPos.length / 3];
        float[] cNeighbors = new float[cPos.length / 3];
        this.circleMesh = new com.za.minecraft.engine.graphics.Mesh(cPos, cUv, cNorm, cTypes, cNeighbors, cInd);
    }

    public ViewmodelRenderer getViewmodelRenderer() {
        return viewmodelRenderer;
    }

    public void render(AnimationEditorState state, Viewmodel viewmodel, DynamicTextureAtlas atlas, float aspectRatio) {
        if (viewmodel == null) return;

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);
        glDisable(GL_CULL_FACE); 
        
        shader.use();
        atlas.bind();
        
        // Exact in-game viewmodel perspective
        shader.setMatrix4f("projection", new Matrix4f().setPerspective((float)Math.toRadians(70.0f), aspectRatio, 0.01f, 1000.0f));
        shader.setMatrix4f("view", new Matrix4f().identity()); 
        
        shader.setVector3f("lightDirection", new Vector3f(0.4f, -0.8f, 0.4f).normalize());
        shader.setVector3f("lightColor", new Vector3f(0.3f, 0.3f, 0.25f));
        shader.setVector3f("ambientLight", new Vector3f(0.8f, 0.8f, 0.85f));
        shader.setFloat("uTime", (float)glfwGetTime());
        shader.setVector3f("uCondition", new Vector3f(0, 0, 0)); // No blood/dirt in studio by default

        if (!viewmodel.root.children.isEmpty() && viewmodel.root.children.get(0).mesh == null) {
            viewmodel.initMeshes(atlas);
        }

        viewmodel.updateHierarchy(new Matrix4f().identity());
        renderRecursive(viewmodel.root, state, atlas);

        if (state.selectedPart != null) {
            renderGizmos(state.selectedPart);
        }

        glDisable(GL_DEPTH_TEST);
    }

    private void renderGizmos(ModelNode node) {
        glDisable(GL_DEPTH_TEST);
        glLineWidth(2.5f);
        
        shader.setBoolean("isHand", true);
        shader.setFloat("uHandPartWeight", 1.0f);
        
        Matrix4f base = new Matrix4f(node.globalMatrix);
        float ringRadius = 0.2f;
        
        // X - Red (Pitch)
        shader.setVector3f("lightColor", new Vector3f(1, 0.2f, 0.2f));
        shader.setMatrix4f("model", new Matrix4f(base).scale(0.22f, 1, 1));
        gizmoMesh.render(GL_LINES);
        shader.setMatrix4f("model", new Matrix4f(base).rotateY((float)Math.toRadians(0)).scale(ringRadius));
        circleMesh.render(GL_LINES);
        
        // Y - Green (Yaw)
        shader.setVector3f("lightColor", new Vector3f(0.2f, 1, 0.2f));
        shader.setMatrix4f("model", new Matrix4f(base).rotateZ((float)Math.toRadians(90)).scale(0.22f, 1, 1));
        gizmoMesh.render(GL_LINES);
        shader.setMatrix4f("model", new Matrix4f(base).rotateZ((float)Math.toRadians(90)).scale(ringRadius));
        circleMesh.render(GL_LINES);
        
        // Z - Blue (Roll)
        shader.setVector3f("lightColor", new Vector3f(0.2f, 0.2f, 1));
        shader.setMatrix4f("model", new Matrix4f(base).rotateY((float)Math.toRadians(-90)).scale(0.22f, 1, 1));
        gizmoMesh.render(GL_LINES);
        shader.setMatrix4f("model", new Matrix4f(base).rotateY((float)Math.toRadians(90)).scale(ringRadius));
        circleMesh.render(GL_LINES);
        
        shader.setBoolean("isHand", false);
        glLineWidth(1.0f);
        glEnable(GL_DEPTH_TEST);
    }

    private void renderRecursive(ModelNode node, AnimationEditorState state, DynamicTextureAtlas atlas) {
        boolean isSelected = (node == state.selectedPart);
        
        if (node.mesh != null) {
            shader.setMatrix4f("model", node.globalMatrix);
            float weight = node.name.contains("hand") ? 1.0f : (node.name.contains("forearm") ? 0.6f : 0.3f);
            
            // Selection Override for bones
            if (isSelected) {
                shader.setBoolean("isHand", true);
                shader.setFloat("uHandPartWeight", 1.0f);
                shader.setFloat("uMiningHeat", 1.0f);
            } else {
                shader.setBoolean("isHand", weight > 0.01f);
                shader.setFloat("uHandPartWeight", weight);
                shader.setFloat("uMiningHeat", 0.0f);
            }
            node.mesh.render();
            shader.setBoolean("isHand", false);
        }

        // Render held item at attachment point
        if (node.name.equals("item_attachment_r") && state.heldStack != null) {
            float heat = isSelected ? 1.0f : 0.0f;
            // Force whole item to glow if attachment node is selected
            if (isSelected) {
                shader.setBoolean("isHand", true);
                shader.setFloat("uHandPartWeight", 1.0f);
            } else {
                shader.setBoolean("isHand", false);
            }
            viewmodelRenderer.getHeldItemRenderer().render(node.globalMatrix, state.heldStack, shader, atlas, true, heat);
            shader.setBoolean("isHand", false);
        }

        for (ModelNode child : node.children) {
            renderRecursive(child, state, atlas);
        }
    }

    public void cleanup() {
        shader.cleanup();
        viewmodelRenderer.cleanup();
    }
}
