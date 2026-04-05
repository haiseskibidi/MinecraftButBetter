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

    public AnimationEditorRenderer() {
        this.shader = new Shader("src/main/resources/shaders/viewmodel_vertex.glsl", "src/main/resources/shaders/viewmodel_fragment.glsl");
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

        glDisable(GL_DEPTH_TEST);
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
