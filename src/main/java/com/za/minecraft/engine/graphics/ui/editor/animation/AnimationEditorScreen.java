package com.za.minecraft.engine.graphics.ui.editor.animation;

import com.za.minecraft.engine.graphics.Camera;
import com.za.minecraft.engine.graphics.DynamicTextureAtlas;
import com.za.minecraft.engine.graphics.Shader;
import com.za.minecraft.engine.graphics.model.ModelRegistry;
import com.za.minecraft.engine.graphics.model.Viewmodel;
import com.za.minecraft.engine.graphics.model.ViewmodelDefinition;
import com.za.minecraft.engine.graphics.model.ViewmodelRenderer;
import com.za.minecraft.engine.graphics.ui.Screen;
import com.za.minecraft.engine.graphics.ui.ScreenManager;
import com.za.minecraft.engine.graphics.ui.UIRenderer;
import com.za.minecraft.engine.core.GameLoop;
import com.za.minecraft.utils.Identifier;
import com.za.minecraft.utils.Logger;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import static org.lwjgl.glfw.GLFW.glfwGetTime;
import static org.lwjgl.opengl.GL11.*;

/**
 * Specialized screen for editing viewmodel animations.
 */
public class AnimationEditorScreen implements Screen {
    private Viewmodel viewmodel;
    private final ViewmodelRenderer viewmodelRenderer = new ViewmodelRenderer();
    private Shader shader;
    private final Camera camera = new Camera();
    
    private boolean initialized = false;

    @Override
    public void init(int screenWidth, int screenHeight) {
        if (!initialized) {
            // Load hands model
            ViewmodelDefinition handsDef = ModelRegistry.getViewmodel(Identifier.of("minecraft:hands"));
            if (handsDef != null) {
                viewmodel = new Viewmodel(handsDef);
            } else {
                Logger.error("AnimationEditor: Failed to find 'minecraft:hands' model!");
            }

            // Setup dedicated shader
            shader = new Shader(
                "src/main/resources/shaders/viewmodel_vertex.glsl",
                "src/main/resources/shaders/viewmodel_fragment.glsl"
            );

            // Setup static camera
            camera.getPosition().set(0, 0, 0);
            camera.getRotation().set(0, 0, 0);
            
            initialized = true;
            Logger.info("Animation Editor initialized");
        }
        
        camera.updateAspectRatio((float) screenWidth / screenHeight);
    }

    @Override
    public boolean isScene() {
        return true;
    }

    @Override
    public void render(UIRenderer renderer, int sw, int sh, DynamicTextureAtlas atlas) {
        if (viewmodel == null) return;

        // 0. Clear screen with studio background
        glClearColor(0.12f, 0.12f, 0.12f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // 1. Render 3D Viewport
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);
        
        shader.use();
        atlas.bind();

        // Standard perspective for viewmodel
        Matrix4f projection = new Matrix4f().setPerspective((float)Math.toRadians(70.0f), camera.getAspectRatio(), 0.01f, 1000.0f);
        shader.setMatrix4f("projection", projection);
        shader.setMatrix4f("view", new Matrix4f().identity()); // Fixed view for now
        
        shader.setVector3f("lightDirection", new Vector3f(0.4f, -0.8f, 0.4f).normalize());
        shader.setVector3f("lightColor", new Vector3f(0.3f, 0.3f, 0.25f));
        shader.setVector3f("ambientLight", new Vector3f(0.85f, 0.85f, 0.9f));
        shader.setFloat("uTime", (float)glfwGetTime());
        shader.setVector3f("uCondition", new Vector3f(0, 0, 0)); // Clean hands

        // Ensure meshes are initialized
        if (!viewmodel.root.children.isEmpty() && viewmodel.root.children.get(0).mesh == null) {
            viewmodel.initMeshes(atlas);
        }

        // Update hierarchy (static for now, no animation playing yet)
        viewmodel.updateHierarchy(new Matrix4f().identity());

        // Render the model
        viewmodelRenderer.render(viewmodel, shader, atlas, null, null, null, 0, 0);

        glDisable(GL_DEPTH_TEST);

        // 2. Render UI Overlay
        renderer.setupUIProjection(sw, sh);
        renderUI(renderer, sw, sh);
    }

    private void renderUI(UIRenderer renderer, int sw, int sh) {
        // Placeholder for future UI components
        renderer.getFontRenderer().drawString("ANIMATION EDITOR (MVP)", 20, 20, 24, sw, sh, 1.0f, 0.6f, 0.0f, 1.0f);
        renderer.getFontRenderer().drawString("Model: minecraft:hands", 20, 50, 16, sw, sh);
        
        // Render a basic panel background on the left for bone list (Step 1.4)
        renderer.renderRect(10, 80, 200, sh - 200, sw, sh, 0.1f, 0.1f, 0.1f, 0.8f);
        renderer.getFontRenderer().drawString("BONES", 20, 90, 18, sw, sh, 0.4f, 0.7f, 1.0f, 1.0f);
        
        // Timeline placeholder (Step 1.2)
        renderer.renderRect(10, sh - 100, sw - 20, 80, sw, sh, 0.1f, 0.1f, 0.1f, 0.8f);
        renderer.getFontRenderer().drawString("TIMELINE", 20, sh - 90, 18, sw, sh, 0.4f, 0.7f, 1.0f, 1.0f);
    }

    @Override
    public boolean handleKeyPress(int key) {
        // Exit editor on ESC
        if (key == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            GameLoop.getInstance().toggleAnimationEditor();
            return true;
        }
        return false;
    }

    public void cleanup() {
        if (shader != null) shader.cleanup();
        if (viewmodel != null) viewmodel.cleanup();
    }
}
