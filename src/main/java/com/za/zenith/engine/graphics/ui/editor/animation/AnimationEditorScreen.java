package com.za.zenith.engine.graphics.ui.editor.animation;

import com.za.zenith.engine.core.GameLoop;
import com.za.zenith.engine.graphics.DynamicTextureAtlas;
import com.za.zenith.engine.graphics.model.ModelRegistry;
import com.za.zenith.engine.graphics.model.Viewmodel;
import com.za.zenith.engine.graphics.model.ViewmodelDefinition;
import com.za.zenith.engine.graphics.ui.Screen;
import com.za.zenith.engine.graphics.ui.UIRenderer;
import com.za.zenith.utils.Identifier;
import com.za.zenith.utils.Logger;
import org.joml.Vector3f;

import java.util.Objects;

import static org.lwjgl.opengl.GL11.*;

/**
 * Specialized screen for editing viewmodel animations.
 * Orchestrates State, Renderer, UI, and Input components.
 */
public class AnimationEditorScreen implements Screen {
    
    private final AnimationEditorState state = new AnimationEditorState();
    private final AnimationEditorRenderer renderer = new AnimationEditorRenderer();
    private final EditorUI ui = new EditorUI();
    private final EditorInputHandler inputHandler = new EditorInputHandler();
    
    private Viewmodel viewmodel;
    private long lastFrameTime;
    private boolean initialized = false;

    @Override
    public void init(int screenWidth, int screenHeight) {
        if (!initialized) {
            ViewmodelDefinition handsDef = ModelRegistry.getViewmodel(Identifier.of("zenith:hands"));
            if (handsDef != null) {
                viewmodel = new Viewmodel(handsDef);
                // Attach camera to root so it gets global matrix updates
                viewmodel.root.children.add(state.cameraNode);
                
                state.tracks.clear();
                
                // Collect parts first
                collectParts(viewmodel.root);
                
                // Initial evaluation
                state.evaluateAll(getPartsList(), false);
            }
            lastFrameTime = System.currentTimeMillis();
            initialized = true;
            Logger.info("Animation Studio initialized (Modular)");
        }
    }

    private void collectParts(com.za.zenith.engine.graphics.model.ModelNode node) {
        if (node != null && node.name != null && !node.name.equals("root")) { 
            state.tracks.put(node.name, new AnimationEditorState.EditorTrack());
        }
        for (com.za.zenith.engine.graphics.model.ModelNode child : node.children) collectParts(child);
    }

    public Viewmodel getViewmodel() { return viewmodel; }

    @Override
    public boolean isScene() {
        return true;
    }

    @Override
    public void render(UIRenderer uiRenderer, int sw, int sh, DynamicTextureAtlas atlas) {
        if (viewmodel == null) return;

        // 1. Update logic
        long now = System.currentTimeMillis();
        float deltaTime = (now - lastFrameTime) / 1000.0f;
        lastFrameTime = now;

        if (state.isPlaying) {
            state.currentTime += deltaTime;
            if (state.currentTime > 1.0f) state.currentTime = 0.0f;
        }

        // Apply animations to bone transforms
        state.evaluateAll(getPartsList(), inputHandler.getCurrentMode() != TransformController.TransformMode.NONE);
        // 2. Render 3D
        glClearColor(0.1f, 0.1f, 0.12f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        renderer.render(state, viewmodel, atlas, (float)sw/sh, inputHandler.getTransformController());

        // 3. Render UI
        uiRenderer.setupUIProjection(sw, sh);
        ui.render(uiRenderer, state, sw, sh, atlas, getPartsList());
    }

    private com.za.zenith.engine.graphics.model.ModelNode findNode(String name) {
        return findNodeRecursive(viewmodel.root, name);
    }

    private com.za.zenith.engine.graphics.model.ModelNode findNodeRecursive(com.za.zenith.engine.graphics.model.ModelNode root, String name) {
        if (root.name.equals(name)) return root;
        for (com.za.zenith.engine.graphics.model.ModelNode child : root.children) {
            com.za.zenith.engine.graphics.model.ModelNode found = findNodeRecursive(child, name);
            if (found != null) return found;
        }
        return null;
    }

    private java.util.List<com.za.zenith.engine.graphics.model.ModelNode> getPartsList() {
        java.util.List<com.za.zenith.engine.graphics.model.ModelNode> list = new java.util.ArrayList<>();
        collectPartsToList(viewmodel.root, list);
        return list;
    }

    private void collectPartsToList(com.za.zenith.engine.graphics.model.ModelNode node, java.util.List<com.za.zenith.engine.graphics.model.ModelNode> list) {
        if (node != null && node.name != null && !node.name.equals("root") && !node.name.contains("attachment")) {
            list.add(node);
        }
        // Always add attachment for animation
        if (node != null && node.name.equals("item_attachment_r")) {
            list.add(node);
        }
        for (com.za.zenith.engine.graphics.model.ModelNode child : node.children) collectPartsToList(child, list);
    }
    @Override
    public boolean handleMouseClick(float mx, float my, int button) {
        if (viewmodel != null) viewmodel.updateHierarchy(new org.joml.Matrix4f().identity());
        inputHandler.handleMouseClick(mx, my, button, state, getPartsList(), renderer);
        return true;
    }

    @Override
    public void handleMouseRelease(int button) {
        if (viewmodel != null) viewmodel.updateHierarchy(new org.joml.Matrix4f().identity());
        inputHandler.handleMouseRelease(button, state, getPartsList());
        state.isScrubbing = false;
    }

    @Override
    public void handleMouseMove(float mx, float my) {
        if (viewmodel != null) viewmodel.updateHierarchy(new org.joml.Matrix4f().identity());
        inputHandler.handleMouseMove(mx, my, state, getPartsList());
    }

    @Override
    public boolean handleKeyPress(int key) {
        return inputHandler.handleKeyPress(key, state, getPartsList());
    }

    @Override
    public boolean handleScroll(double yoffset) {
        return inputHandler.handleScroll(yoffset);
    }

    public void cleanup() {
        renderer.cleanup();
        if (viewmodel != null) viewmodel.cleanup();
    }
}
