package com.za.minecraft.engine.graphics.ui.editor.animation;

import com.za.minecraft.engine.graphics.DynamicTextureAtlas;
import com.za.minecraft.engine.graphics.Shader;
import com.za.minecraft.engine.graphics.model.*;
import com.za.minecraft.world.items.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.Map;

import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.opengl.GL11.*;

/**
 * Handles all 3D rendering for the Animation Studio.
 * Cleaned up: IK logic removed completely from the editor.
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
        float[] pos = { 0,0,0, 1,0,0 };
        float[] uv = { 0,0,0, 0,0,0 };
        float[] norm = { 0,1,0, 0,1,0 };
        float[] types = { 0, 0 };
        float[] neighbor = { 0, 0 };
        int[] ind = { 0, 1 };
        this.gizmoMesh = new com.za.minecraft.engine.graphics.Mesh(pos, uv, norm, types, neighbor, ind);

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

    public void render(AnimationEditorState state, Viewmodel viewmodel, DynamicTextureAtlas atlas, float aspectRatio, TransformController transform) {
        if (viewmodel == null) return;

        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);
        glDisable(GL_CULL_FACE); 
        
        shader.use();
        atlas.bind();
        
        float fovBase = (float)Math.toRadians(70.0f);
        float fovOffset = state.cameraNode != null ? state.cameraNode.animTranslation.z : 0;
        shader.setMatrix4f("projection", new Matrix4f().setPerspective(fovBase + fovOffset, aspectRatio, 0.01f, 1000.0f));
        
        Matrix4f viewMatrix = new Matrix4f().identity();
        if (state.cameraNode != null) {
            viewMatrix.rotateZ(-state.cameraNode.animRotation.z).rotateX(-state.cameraNode.animRotation.x).translate(-state.cameraNode.animTranslation.x, -state.cameraNode.animTranslation.y, 0);
        }
        shader.setMatrix4f("view", viewMatrix); 
        
        if (state.cameraNode != null) {
            state.cameraNode.globalMatrix.identity().translate(state.cameraNode.animTranslation.x, state.cameraNode.animTranslation.y, 0).rotateX(state.cameraNode.animRotation.x).rotateZ(state.cameraNode.animRotation.z).translate(0, -0.1f, -0.5f);
        }
        
        shader.setVector3f("lightDirection", new Vector3f(0.4f, -0.8f, 0.4f).normalize());
        shader.setVector3f("lightColor", new Vector3f(0.3f, 0.3f, 0.25f));
        shader.setVector3f("ambientLight", new Vector3f(0.8f, 0.8f, 0.85f));
        shader.setFloat("uTime", (float)glfwGetTime());
        shader.setVector3f("uCondition", new Vector3f(0, 0, 0));

        if (!viewmodel.root.children.isEmpty() && viewmodel.root.children.get(0).mesh == null) {
            viewmodel.initMeshes(atlas);
        }

        // Establish FK
        viewmodel.updateHierarchy(new Matrix4f().identity());
        
        // --- IK Update ---
        if (state.ikManager.isEnabled()) {
            state.ikManager.update(viewmodel.root);
        }

        if (state.onionSkinning) {
            renderGhosts(state, viewmodel, atlas);
        }

        renderRecursive(viewmodel.root, state, atlas);

        // --- IK Target Visualization ---
        if (state.ikManager.isEnabled()) {
            glDisable(GL_DEPTH_TEST);
            for (Map.Entry<String, com.za.minecraft.engine.graphics.model.ik.IKChain> entry : state.ikManager.getChains().entrySet()) {
                String name = entry.getKey();
                Vector3f tPos = state.ikManager.getTargetPos(name);
                Vector3f pPos = state.ikManager.getPolePos(name);

                // Draw Target (Yellow)
                shader.setMatrix4f("model", new Matrix4f().translate(tPos).scale(0.02f));
                shader.setBoolean("isHand", true); shader.setFloat("uHandPartWeight", 1.0f);
                shader.setVector3f("lightColor", new Vector3f(1, 1, 0)); 
                gizmoMesh.render(GL_LINES); 

                // Draw Pole (Blue)
                if (pPos != null) {
                    shader.setMatrix4f("model", new Matrix4f().translate(pPos).scale(0.015f));
                    shader.setVector3f("lightColor", new Vector3f(0.2f, 0.4f, 1.0f)); 
                    gizmoMesh.render(GL_LINES); 
                }
            }
            shader.setBoolean("isHand", false);
            glEnable(GL_DEPTH_TEST);
        }

        if (state.selectedPart != null && state.selectedPart != state.cameraNode) {
            renderGizmos(state.selectedPart);
        } else if (state.selectedPart == state.cameraNode) {
            shader.setMatrix4f("model", new Matrix4f(state.cameraNode.globalMatrix).scale(0.05f));
            shader.setBoolean("isHand", true); shader.setFloat("uHandPartWeight", 1.0f); shader.setFloat("uMiningHeat", 1.0f);
            gizmoMesh.render(GL_LINES);
            shader.setBoolean("isHand", false);
            renderGizmos(state.cameraNode);
        }

        glDisable(GL_DEPTH_TEST);
    }

    private void renderGhosts(AnimationEditorState state, Viewmodel viewmodel, DynamicTextureAtlas atlas) {
        glEnable(GL_BLEND); glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        float prevT = -1, nextT = -1;
        for (AnimationEditorState.EditorTrack track : state.tracks.values()) {
            for (AnimationEditorState.EditorKeyframe k : track.keyframes) {
                if (k.time() < state.currentTime - 0.001f) prevT = Math.max(prevT, k.time());
                if (k.time() > state.currentTime + 0.001f) nextT = (nextT == -1) ? k.time() : Math.min(nextT, k.time());
            }
        }
        Vector3f st = new Vector3f(), sr = new Vector3f();
        if (state.selectedPart != null) { st.set(state.selectedPart.animTranslation); sr.set(state.selectedPart.animRotation); }
        if (prevT != -1) renderGhostAt(state, viewmodel, atlas, prevT, 0.25f);
        if (nextT != -1) renderGhostAt(state, viewmodel, atlas, nextT, 0.25f);
        if (state.selectedPart != null) { state.selectedPart.animTranslation.set(st); state.selectedPart.animRotation.set(sr); }
        glDisable(GL_BLEND);
    }

    private void renderGhostAt(AnimationEditorState state, Viewmodel viewmodel, DynamicTextureAtlas atlas, float time, float alpha) {
        float realTime = state.currentTime; state.currentTime = time;
        state.evaluateAll(viewmodel.getAllNodes(), false);
        viewmodel.updateHierarchy(new Matrix4f().identity());
        shader.setFloat("uAlpha", alpha);
        renderRecursive(viewmodel.root, state, atlas);
        shader.setFloat("uAlpha", 1.0f);
        state.currentTime = realTime;
        state.evaluateAll(viewmodel.getAllNodes(), false);
    }

    private void renderGizmos(ModelNode node) {
        glDisable(GL_DEPTH_TEST); glLineWidth(2.5f);
        shader.setBoolean("isHand", true); shader.setFloat("uHandPartWeight", 1.0f);
        Matrix4f base = new Matrix4f(node.globalMatrix); float ringRadius = 0.2f;
        
        shader.setVector3f("lightColor", new Vector3f(1, 0.2f, 0.2f));
        shader.setMatrix4f("model", new Matrix4f(base).scale(0.22f, 1, 1)); gizmoMesh.render(GL_LINES);
        shader.setMatrix4f("model", new Matrix4f(base).rotateY(0).scale(ringRadius)); circleMesh.render(GL_LINES);
        
        shader.setVector3f("lightColor", new Vector3f(0.2f, 1, 0.2f));
        shader.setMatrix4f("model", new Matrix4f(base).rotateZ((float)Math.toRadians(90)).scale(0.22f, 1, 1)); gizmoMesh.render(GL_LINES);
        shader.setMatrix4f("model", new Matrix4f(base).rotateZ((float)Math.toRadians(90)).scale(ringRadius)); circleMesh.render(GL_LINES);
        
        shader.setVector3f("lightColor", new Vector3f(0.2f, 0.2f, 1));
        shader.setMatrix4f("model", new Matrix4f(base).rotateY((float)Math.toRadians(-90)).scale(0.22f, 1, 1)); gizmoMesh.render(GL_LINES);
        shader.setMatrix4f("model", new Matrix4f(base).rotateY((float)Math.toRadians(90)).scale(ringRadius)); circleMesh.render(GL_LINES);
        
        shader.setBoolean("isHand", false); glLineWidth(1.0f); glEnable(GL_DEPTH_TEST);
    }

    private void renderRecursive(ModelNode node, AnimationEditorState state, DynamicTextureAtlas atlas) {
        boolean isSelected = (node == state.selectedPart);
        if (node == state.cameraNode) {
            shader.setMatrix4f("model", new Matrix4f(node.globalMatrix).scale(0.05f));
            shader.setBoolean("isHand", true); shader.setFloat("uHandPartWeight", 1.0f); shader.setFloat("uMiningHeat", isSelected ? 1.0f : 0.0f);
            gizmoMesh.render(GL_LINES); shader.setBoolean("isHand", false);
        } else if (node.mesh != null) {
            shader.setMatrix4f("model", node.globalMatrix);
            float weight = node.name.contains("hand") ? 1.0f : (node.name.contains("forearm") ? 0.6f : 0.3f);
            shader.setBoolean("isHand", isSelected || weight > 0.01f);
            shader.setFloat("uHandPartWeight", isSelected ? 1.0f : weight);
            shader.setFloat("uMiningHeat", isSelected ? 1.0f : 0.0f);
            node.mesh.render();
            shader.setBoolean("isHand", false);
        }
        if (node.name.equals("item_attachment_r") && state.heldStack != null) {
            if (isSelected) { shader.setBoolean("isHand", true); shader.setFloat("uHandPartWeight", 1.0f); }
            viewmodelRenderer.getHeldItemRenderer().render(node.globalMatrix, state.heldStack, shader, atlas, true, isSelected ? 1.0f : 0.0f);
            shader.setBoolean("isHand", false);
        }
        for (ModelNode child : node.children) renderRecursive(child, state, atlas);
    }

    public void cleanup() { shader.cleanup(); viewmodelRenderer.cleanup(); }
}