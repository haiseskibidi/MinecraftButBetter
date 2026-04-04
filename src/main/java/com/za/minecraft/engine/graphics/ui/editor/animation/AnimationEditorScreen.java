package com.za.minecraft.engine.graphics.ui.editor.animation;

import com.za.minecraft.engine.graphics.Camera;
import com.za.minecraft.engine.graphics.DynamicTextureAtlas;
import com.za.minecraft.engine.graphics.Shader;
import com.za.minecraft.engine.graphics.model.*;
import com.za.minecraft.engine.graphics.ui.Screen;
import com.za.minecraft.engine.graphics.ui.UIRenderer;
import com.za.minecraft.engine.core.GameLoop;
import com.za.minecraft.utils.Identifier;
import com.za.minecraft.utils.Logger;
import org.joml.Intersectionf;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Specialized screen for editing viewmodel animations.
 */
public class AnimationEditorScreen implements Screen {
    private Viewmodel viewmodel;
    private final ViewmodelRenderer viewmodelRenderer = new ViewmodelRenderer();
    private Shader shader;
    private final Camera camera = new Camera();
    
    // Bone Hierarchy
    private final List<ModelNode> allParts = new ArrayList<>();
    private ModelNode selectedPart;
    private int hoveredPartIndex = -1;

    // Timeline state
    private float currentTime = 0.0f; // 0.0 to 1.0
    private boolean isPlaying = false;
    private long lastFrameTime;
    private boolean isScrubbing = false;

    // Interaction Modes
    private enum TransformMode { NONE, GRAB, ROTATE }
    private TransformMode currentMode = TransformMode.NONE;
    private char activeAxis = ' '; // 'X', 'Y', 'Z' or ' ' for all
    
    private Vector2f lastMousePos = new Vector2f();
    private Vector3f originalValue = new Vector3f();
    private Vector3f startAnimTranslation = new Vector3f();
    private Vector3f startAnimRotation = new Vector3f();

    private boolean initialized = false;

    @Override
    public void init(int screenWidth, int screenHeight) {
        if (!initialized) {
            // Load hands model
            ViewmodelDefinition handsDef = ModelRegistry.getViewmodel(Identifier.of("minecraft:hands"));
            if (handsDef != null) {
                viewmodel = new Viewmodel(handsDef);
                allParts.clear();
                collectParts(viewmodel.root);
                Logger.info("AnimationEditor: Found " + allParts.size() + " parts in model.");
            } else {
                Logger.error("AnimationEditor: Failed to find 'minecraft:hands' model!");
            }

            // Setup dedicated shader
            shader = new Shader(
                "src/main/resources/shaders/viewmodel_vertex.glsl",
                "src/main/resources/shaders/viewmodel_fragment.glsl"
            );

            // Setup static camera (match in-game viewmodel perspective)
            camera.getPosition().set(0, 0, 0);
            camera.getRotation().set(0, 0, 0);
            
            lastFrameTime = System.currentTimeMillis();
            initialized = true;
            Logger.info("Animation Editor initialized");
        }
        
        camera.updateAspectRatio((float) screenWidth / screenHeight);
    }

    private void collectParts(ModelNode node) {
        if (node != null && node.name != null && !node.name.equals("root") && !node.name.contains("attachment")) { 
            allParts.add(node);
        }
        for (ModelNode child : node.children) {
            collectParts(child);
        }
    }

    @Override
    public boolean isScene() {
        return true;
    }

    private void update() {
        long now = System.currentTimeMillis();
        float deltaTime = (now - lastFrameTime) / 1000.0f;
        lastFrameTime = now;

        if (isPlaying) {
            currentTime += deltaTime;
            if (currentTime > 1.0f) currentTime = 0.0f;
        }
    }

    @Override
    public void render(UIRenderer renderer, int sw, int sh, DynamicTextureAtlas atlas) {
        if (viewmodel == null) {
            renderer.getFontRenderer().drawString("ERROR: MODEL NOT LOADED", sw/2 - 100, sh/2, 20, sw, sh, 1, 0, 0, 1);
            return;
        }
        update();

        // 0. Clear screen
        glClearColor(0.12f, 0.12f, 0.12f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // 1. Render 3D Viewport
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);
        glDisable(GL_CULL_FACE); 
        
        shader.use();
        atlas.bind();

        Matrix4f projection = new Matrix4f().setPerspective((float)Math.toRadians(70.0f), (float)sw/sh, 0.01f, 1000.0f);
        Matrix4f view = new Matrix4f().identity();
        
        shader.setMatrix4f("projection", projection);
        shader.setMatrix4f("view", view); 
        
        shader.setVector3f("lightDirection", new Vector3f(0.4f, -0.8f, 0.4f).normalize());
        shader.setVector3f("lightColor", new Vector3f(0.3f, 0.3f, 0.25f));
        shader.setVector3f("ambientLight", new Vector3f(0.85f, 0.85f, 0.9f));
        shader.setFloat("uTime", (float)glfwGetTime());
        shader.setVector3f("uCondition", new Vector3f(0, 0, 0)); 

        if (!viewmodel.root.children.isEmpty() && viewmodel.root.children.get(0).mesh == null) {
            viewmodel.initMeshes(atlas);
        }

        viewmodel.updateHierarchy(new Matrix4f().identity());
        renderModelWithHighlight(viewmodel.root);

        glEnable(GL_CULL_FACE);
        glDisable(GL_DEPTH_TEST);

        // 2. Render UI Overlay
        renderer.setupUIProjection(sw, sh);
        renderUI(renderer, sw, sh);
    }

    private void renderModelWithHighlight(ModelNode node) {
        if (node.mesh != null) {
            shader.setMatrix4f("model", node.globalMatrix);
            
            float partWeight = 0.0f;
            if (node.name.contains("hand") || node.name.contains("finger")) partWeight = 1.0f;
            else if (node.name.contains("forearm")) partWeight = 0.6f;
            else if (node.name.contains("shoulder")) partWeight = 0.3f;

            boolean isSelected = (node == selectedPart);
            if (isSelected) {
                shader.setBoolean("isHand", true);
                shader.setFloat("uHandPartWeight", 1.0f);
                shader.setFloat("uMiningHeat", 1.0f);
            } else {
                shader.setBoolean("isHand", partWeight > 0.01f);
                shader.setFloat("uHandPartWeight", partWeight);
                shader.setFloat("uMiningHeat", 0.0f);
            }
            
            node.mesh.render();
            shader.setBoolean("isHand", false);
        }
        for (ModelNode child : node.children) {
            renderModelWithHighlight(child);
        }
    }

    private void renderUI(UIRenderer renderer, int sw, int sh) {
        // Header
        renderer.getFontRenderer().drawString("ANIMATION EDITOR", 20, 20, 24, sw, sh, 1.0f, 0.6f, 0.0f, 1.0f);
        renderer.getFontRenderer().drawString("F8 - Close | Model: minecraft:hands", 20, 50, 14, sw, sh, 0.7f, 0.7f, 0.7f, 1.0f);
        
        // Mode Hint
        if (currentMode != TransformMode.NONE) {
            String modeStr = currentMode.name() + (activeAxis != ' ' ? " (" + activeAxis + ")" : "");
            renderer.getFontRenderer().drawString(modeStr, sw/2 - 50, 20, 20, sw, sh, 1.0f, 1.0f, 0.0f, 1.0f);
            renderer.getFontRenderer().drawString("Confirm: Left Click | Cancel: Right Click/ESC", sw/2 - 120, 45, 12, sw, sh);
        }

        // Parts List Panel (Left)
        int panelX = 10;
        int panelY = 80;
        int panelW = 220;
        int panelH = sh - 200;
        renderer.renderRect(panelX, panelY, panelW, panelH, sw, sh, 0.08f, 0.08f, 0.08f, 0.9f);
        renderer.getFontRenderer().drawString("BODY PARTS (" + allParts.size() + ")", panelX + 10, panelY + 10, 18, sw, sh, 0.4f, 0.7f, 1.0f, 1.0f);
        
        int itemY = panelY + 40;
        for (int i = 0; i < allParts.size(); i++) {
            ModelNode part = allParts.get(i);
            boolean isSelected = (part == selectedPart);
            boolean isHovered = (i == hoveredPartIndex);
            
            if (isSelected) {
                renderer.renderRect(panelX + 5, itemY - 2, panelW - 10, 20, sw, sh, 0.2f, 0.4f, 0.6f, 1.0f);
            } else if (isHovered) {
                renderer.renderRect(panelX + 5, itemY - 2, panelW - 10, 20, sw, sh, 0.15f, 0.15f, 0.15f, 1.0f);
            }
            
            float r = isSelected ? 1 : (isHovered ? 0.9f : 0.6f);
            renderer.getFontRenderer().drawString(part.name, panelX + 15, itemY, 14, sw, sh, r, r, r, 1.0f);
            itemY += 22;
        }

        // Property Inspector (Right)
        if (selectedPart != null) {
            int propX = sw - 230;
            int propY = 80;
            int propW = 220;
            renderer.renderRect(propX, propY, propW, sh - 200, sw, sh, 0.08f, 0.08f, 0.08f, 0.9f);
            renderer.getFontRenderer().drawString("PROPERTIES", propX + 10, propY + 10, 18, sw, sh, 1.0f, 0.8f, 0.2f, 1.0f);
            renderer.getFontRenderer().drawString(selectedPart.name, propX + 10, propY + 35, 12, sw, sh, 0.7f, 0.7f, 0.7f, 1.0f);
            
            int infoY = propY + 70;
            drawTransformInfo(renderer, "Anim X", selectedPart.animTranslation.x, propX + 15, infoY, sw, sh);
            drawTransformInfo(renderer, "Anim Y", selectedPart.animTranslation.y, propX + 15, infoY + 25, sw, sh);
            drawTransformInfo(renderer, "Anim Z", selectedPart.animTranslation.z, propX + 15, infoY + 50, sw, sh);
            
            drawTransformInfo(renderer, "Pitch", (float)Math.toDegrees(selectedPart.animRotation.x), propX + 15, infoY + 90, sw, sh);
            drawTransformInfo(renderer, "Yaw  ", (float)Math.toDegrees(selectedPart.animRotation.y), propX + 15, infoY + 115, sw, sh);
            drawTransformInfo(renderer, "Roll ", (float)Math.toDegrees(selectedPart.animRotation.z), propX + 15, infoY + 140, sw, sh);

            renderer.getFontRenderer().drawString("[G] Grab | [R] Rotate", propX + 15, sh - 150, 12, sw, sh, 0.5f, 0.5f, 0.5f, 1.0f);
        }
        
        // Timeline Panel
        int tlX = 10;
        int tlY = sh - 100;
        int tlW = sw - 20;
        int tlH = 80;
        renderer.renderRect(tlX, tlY, tlW, tlH, sw, sh, 0.08f, 0.08f, 0.08f, 0.9f);
        renderer.getFontRenderer().drawString("TIMELINE", tlX + 10, tlY + 10, 16, sw, sh, 0.4f, 0.7f, 1.0f, 1.0f);
        
        int barX = tlX + 50;
        int barY = tlY + 45;
        int barW = tlW - 100;
        int barH = 10;
        renderer.renderRect(barX, barY, barW, barH, sw, sh, 0.2f, 0.2f, 0.2f, 1.0f);
        int phX = barX + (int)(currentTime * barW);
        renderer.renderRect(phX - 2, barY - 15, 4, 40, sw, sh, 1.0f, 0.2f, 0.2f, 1.0f);
        
        renderer.getFontRenderer().drawString("0.0", barX - 30, barY - 2, 12, sw, sh);
        renderer.getFontRenderer().drawString("1.0", barX + barW + 10, barY - 2, 12, sw, sh);
        renderer.getFontRenderer().drawString(String.format("%.2f", currentTime), phX - 10, barY + 30, 12, sw, sh, 1.0f, 0.4f, 0.4f, 1.0f);

        String status = isPlaying ? "PLAYING" : "PAUSED";
        renderer.getFontRenderer().drawString(status, sw - 120, 20, 16, sw, sh, isPlaying ? 0.2f : 1.0f, isPlaying ? 1.0f : 0.2f, 0.2f, 1.0f);
        renderer.getFontRenderer().drawString("[Space] to Toggle", sw - 140, 45, 12, sw, sh);
    }

    private void drawTransformInfo(UIRenderer renderer, String label, float value, int x, int y, int sw, int sh) {
        renderer.getFontRenderer().drawString(label + ":", x, y, 12, sw, sh, 0.6f, 0.6f, 0.6f, 1.0f);
        renderer.getFontRenderer().drawString(String.format("%.3f", value), x + 60, y, 12, sw, sh, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    @Override
    public boolean handleMouseClick(float mx, float my, int button) {
        if (currentMode != TransformMode.NONE) {
            if (button == 0) { // Confirm
                currentMode = TransformMode.NONE;
                activeAxis = ' ';
                Logger.info("Transform confirmed.");
            } else if (button == 1) { // Cancel
                if (selectedPart != null) {
                    selectedPart.animTranslation.set(startAnimTranslation);
                    selectedPart.animRotation.set(startAnimRotation);
                }
                currentMode = TransformMode.NONE;
                activeAxis = ' ';
                Logger.info("Transform cancelled.");
            }
            return true;
        }

        // Timeline Seek/Scrub
        int sw = GameLoop.getInstance().getWindow().getWidth();
        int sh = GameLoop.getInstance().getWindow().getHeight();
        int tlY = sh - 100;
        int barX = 60;
        int barW = sw - 120;
        if (my >= tlY + 20 && my <= tlY + 80) {
            isScrubbing = true;
            currentTime = (mx - barX) / (float)barW;
            currentTime = Math.max(0, Math.min(1.0f, currentTime));
            return true;
        }

        if (pickPart3D(mx, my)) return true;

        // Parts List
        int panelX = 10;
        int panelY = 80;
        int panelW = 220;
        if (mx >= panelX && mx <= panelX + panelW) {
            int itemY = panelY + 40;
            for (int i = 0; i < allParts.size(); i++) {
                if (my >= itemY - 5 && my <= itemY + 20) {
                    selectedPart = allParts.get(i);
                    return true;
                }
                itemY += 22;
            }
        }

        return false;
    }

    @Override
    public void handleMouseRelease(int button) {
        if (button == 0) {
            isScrubbing = false;
        }
    }

    @Override
    public void handleMouseMove(float mx, float my) {
        if (isScrubbing) {
            int sw = GameLoop.getInstance().getWindow().getWidth();
            int barX = 60;
            int barW = sw - 120;
            currentTime = (mx - barX) / (float)barW;
            currentTime = Math.max(0, Math.min(1.0f, currentTime));
            return;
        }

        if (currentMode != TransformMode.NONE && selectedPart != null) {
            float dx = (mx - lastMousePos.x) * 0.01f;
            float dy = (my - lastMousePos.y) * 0.01f;

            if (currentMode == TransformMode.GRAB) {
                if (activeAxis == 'X' || activeAxis == ' ') selectedPart.animTranslation.x += dx;
                if (activeAxis == 'Y' || activeAxis == ' ') selectedPart.animTranslation.y -= dy;
                if (activeAxis == 'Z') selectedPart.animTranslation.z += dy;
            } else if (currentMode == TransformMode.ROTATE) {
                float sens = 5.0f;
                if (activeAxis == 'X' || activeAxis == ' ') selectedPart.animRotation.x -= dy * sens;
                if (activeAxis == 'Y' || activeAxis == ' ') selectedPart.animRotation.y += dx * sens;
                if (activeAxis == 'Z') selectedPart.animRotation.z += dx * sens;
            }
        }

        lastMousePos.set(mx, my);

        // Hover list
        int panelX = 10;
        int panelY = 80;
        int panelW = 220;
        hoveredPartIndex = -1;
        if (mx >= panelX && mx <= panelX + panelW) {
            int itemY = panelY + 40;
            for (int i = 0; i < allParts.size(); i++) {
                if (my >= itemY - 5 && my <= itemY + 20) {
                    hoveredPartIndex = i;
                    break;
                }
                itemY += 22;
            }
        }
    }

    private boolean pickPart3D(float mx, float my) {
        int sw = GameLoop.getInstance().getWindow().getWidth();
        int sh = GameLoop.getInstance().getWindow().getHeight();
        Matrix4f projection = new Matrix4f().setPerspective((float)Math.toRadians(70.0f), (float)sw/sh, 0.01f, 1000.0f);
        Matrix4f invProjView = new Matrix4f(projection).invert();

        float nx = (2.0f * mx / sw) - 1.0f;
        float ny = 1.0f - (2.0f * my / sh);
        Vector4f near = new Vector4f(nx, ny, -1.0f, 1.0f).mul(invProjView);
        near.div(near.w);
        Vector4f far = new Vector4f(nx, ny, 1.0f, 1.0f).mul(invProjView);
        far.div(far.w);

        Vector3f rayOrigin = new Vector3f(near.x, near.y, near.z);
        Vector3f rayDir = new Vector3f(far.x - near.x, far.y - near.y, far.z - near.z).normalize();

        ModelNode bestPart = null;
        float minDistance = Float.MAX_VALUE;

        for (ModelNode part : allParts) {
            if (part.def == null || part.def.cubes == null) continue;
            Matrix4f invGlobal = new Matrix4f(part.globalMatrix).invert();
            Vector3f localOrigin = rayOrigin.mulPosition(invGlobal, new Vector3f());
            Vector3f localDir = rayDir.mulDirection(invGlobal, new Vector3f());

            float px = part.def.pivot != null ? part.def.pivot[0] : 0;
            float py = part.def.pivot != null ? part.def.pivot[1] : 0;
            float pz = part.def.pivot != null ? part.def.pivot[2] : 0;

            for (BoneDefinition.CubeDefinition cube : part.def.cubes) {
                float x0 = (cube.origin[0] - px) / 16.0f;
                float y0 = (cube.origin[1] - py) / 16.0f;
                float z0 = (cube.origin[2] - pz) / 16.0f;
                float x1 = x0 + (cube.size[0] / 16.0f);
                float y1 = y0 + (cube.size[1] / 16.0f);
                float z1 = z0 + (cube.size[2] / 16.0f);

                Vector2f res = new Vector2f();
                if (Intersectionf.intersectRayAab(localOrigin, localDir, new Vector3f(x0, y0, z0), new Vector3f(x1, y1, z1), res)) {
                    if (res.x < minDistance) {
                        minDistance = res.x;
                        bestPart = part;
                    }
                }
            }
        }
        if (bestPart != null) {
            selectedPart = bestPart;
            return true;
        }
        return false;
    }

    @Override
    public boolean handleKeyPress(int key) {
        if (key == GLFW_KEY_ESCAPE || key == GLFW_KEY_F8) {
            if (currentMode != TransformMode.NONE) {
                if (selectedPart != null) {
                    selectedPart.animTranslation.set(startAnimTranslation);
                    selectedPart.animRotation.set(startAnimRotation);
                }
                currentMode = TransformMode.NONE;
                activeAxis = ' ';
                return true;
            }
            GameLoop.getInstance().toggleAnimationEditor();
            return true;
        }
        
        if (key == GLFW_KEY_SPACE) {
            isPlaying = !isPlaying;
            return true;
        }

        if (selectedPart != null) {
            if (key == GLFW_KEY_G) {
                currentMode = TransformMode.GRAB;
                startAnimTranslation.set(selectedPart.animTranslation);
                startAnimRotation.set(selectedPart.animRotation);
                activeAxis = ' ';
                return true;
            }
            if (key == GLFW_KEY_R) {
                currentMode = TransformMode.ROTATE;
                startAnimTranslation.set(selectedPart.animTranslation);
                startAnimRotation.set(selectedPart.animRotation);
                activeAxis = ' ';
                return true;
            }
            if (currentMode != TransformMode.NONE) {
                if (key == GLFW_KEY_X) { activeAxis = 'X'; return true; }
                if (key == GLFW_KEY_Y) { activeAxis = 'Y'; return true; }
                if (key == GLFW_KEY_Z) { activeAxis = 'Z'; return true; }
            }
        }

        return false;
    }

    public void cleanup() {
        if (shader != null) shader.cleanup();
        if (viewmodel != null) viewmodel.cleanup();
    }
}
