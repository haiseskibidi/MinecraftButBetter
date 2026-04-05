package com.za.minecraft.engine.graphics.ui.editor.animation;

import com.za.minecraft.engine.core.GameLoop;
import com.za.minecraft.engine.graphics.Mesh;
import com.za.minecraft.engine.graphics.ui.Hotbar;
import com.za.minecraft.engine.graphics.ui.UIRenderer;
import com.za.minecraft.engine.graphics.model.ModelNode;
import com.za.minecraft.engine.graphics.model.BoneDefinition;
import com.za.minecraft.utils.Logger;
import com.za.minecraft.world.items.Item;
import com.za.minecraft.world.items.ItemRegistry;
import com.za.minecraft.world.items.ItemStack;
import org.joml.Intersectionf;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Handles complex input logic for the Animation Studio (Picking, Transforms, Hotkeys).
 */
public class EditorInputHandler {
    
    public enum TransformMode { NONE, GRAB, ROTATE }
    
    private TransformMode currentMode = TransformMode.NONE;
    private char activeAxis = ' '; 
    private final Vector2f lastMousePos = new Vector2f();
    private final Vector3f startAnimTranslation = new Vector3f();
    private final Vector3f startAnimRotation = new Vector3f();

    public TransformMode getCurrentMode() { return currentMode; }
    public char getActiveAxis() { return activeAxis; }

    public void handleMouseClick(float mx, float my, int button, AnimationEditorState state, List<ModelNode> allParts, AnimationEditorRenderer renderer) {
        if (currentMode != TransformMode.NONE) {
            if (button == 0) { // Confirm
                currentMode = TransformMode.NONE;
                activeAxis = ' ';
                Logger.info("Transform confirmed.");
            } else if (button == 1) { // Cancel
                if (state.selectedPart != null) {
                    state.selectedPart.animTranslation.set(startAnimTranslation);
                    state.selectedPart.animRotation.set(startAnimRotation);
                }
                currentMode = TransformMode.NONE;
                activeAxis = ' ';
                Logger.info("Transform cancelled.");
            }
            return;
        }

        int sw = GameLoop.getInstance().getWindow().getWidth(), sh = GameLoop.getInstance().getWindow().getHeight();
        
        // 1. Check Dev Panel
        Item devItem = getDevItemAt(mx, my, sw, sh);
        if (devItem != null) {
            state.heldStack = new ItemStack(devItem);
            return;
        }

        // 2. Check Timeline
        if (my >= sh - 100) {
            state.isScrubbing = true;
            state.currentTime = Math.max(0, Math.min(1, (mx - 60) / (float)(sw - 120)));
            return;
        }

        // 3. 3D Picking
        if (pickPart3D(mx, my, sw, sh, state, allParts, renderer)) return;

        // 4. Parts List
        if (mx <= 230 && my >= 120) {
            int idx = (int)((my - 130) / 22);
            if (idx >= 0 && idx < allParts.size()) {
                state.selectedPart = allParts.get(idx);
            }
        }
    }

    public void handleMouseMove(float mx, float my, AnimationEditorState state) {
        if (state.isScrubbing) {
            int sw = GameLoop.getInstance().getWindow().getWidth();
            state.currentTime = Math.max(0, Math.min(1, (mx - 60) / (float)(sw - 120)));
        } else if (currentMode != TransformMode.NONE && state.selectedPart != null) {
            float dx = (mx - lastMousePos.x) * 0.01f, dy = (my - lastMousePos.y) * 0.01f;
            if (currentMode == TransformMode.GRAB) {
                if (activeAxis == 'X' || activeAxis == ' ') state.selectedPart.animTranslation.x += dx;
                if (activeAxis == 'Y' || activeAxis == ' ') state.selectedPart.animTranslation.y -= dy;
                if (activeAxis == 'Z') state.selectedPart.animTranslation.z += dy;
            } else {
                float s = 5.0f;
                if (activeAxis == 'X' || activeAxis == ' ') state.selectedPart.animRotation.x -= dy * s;
                if (activeAxis == 'Y' || activeAxis == ' ') state.selectedPart.animRotation.y += dx * s;
                if (activeAxis == 'Z') state.selectedPart.animRotation.z += dx * s;
            }
        }
        lastMousePos.set(mx, my);
        state.hoveredPartIndex = (mx <= 230 && my >= 130) ? (int)((my - 130) / 22) : -1;
    }

    public boolean handleKeyPress(int key, AnimationEditorState state) {
        long window = GameLoop.getInstance().getWindow().getWindowHandle();
        boolean ctrl = glfwGetKey(window, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS;
        
        if (key == GLFW_KEY_S && ctrl) {
            AnimationExporter.export(state, "src/main/resources/minecraft/animations/editor_output.json");
            return true;
        }
        
        if (key == GLFW_KEY_ESCAPE || key == GLFW_KEY_F8) {
            if (currentMode != TransformMode.NONE) {
                if (state.selectedPart != null) {
                    state.selectedPart.animTranslation.set(startAnimTranslation);
                    state.selectedPart.animRotation.set(startAnimRotation);
                }
                currentMode = TransformMode.NONE;
                return true;
            }
            GameLoop.getInstance().toggleAnimationEditor();
            return true;
        }

        if (key == GLFW_KEY_SPACE) {
            state.isPlaying = !state.isPlaying;
            return true;
        }

        if (state.selectedPart != null) {
            if (key == GLFW_KEY_G) {
                currentMode = TransformMode.GRAB;
                startAnimTranslation.set(state.selectedPart.animTranslation);
                startAnimRotation.set(state.selectedPart.animRotation);
                return true;
            }
            if (key == GLFW_KEY_R) {
                currentMode = TransformMode.ROTATE;
                startAnimTranslation.set(state.selectedPart.animTranslation);
                startAnimRotation.set(state.selectedPart.animRotation);
                return true;
            }
            if (key == GLFW_KEY_K) {
                AnimationEditorState.EditorTrack track = state.tracks.get(state.selectedPart.name);
                if (track != null) track.addKey(state.currentTime, state.selectedPart.animTranslation, state.selectedPart.animRotation);
                return true;
            }
            if (key == GLFW_KEY_DELETE) {
                AnimationEditorState.EditorTrack track = state.tracks.get(state.selectedPart.name);
                if (track != null) track.removeNear(state.currentTime);
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

    public void handleMouseRelease(int button) {
        // Nothing specific yet, scrubbing state is handled in screen for now
    }

    public boolean handleScroll(double yoffset) {
        UIRenderer ui = GameLoop.getInstance().getRenderer().getUIRenderer();
        if (ui.getDevScroller().isMouseOver(lastMousePos.x, lastMousePos.y)) {
            ui.getDevScroller().handleScroll(yoffset);
            return true;
        }
        return false;
    }

    private Item getDevItemAt(float mx, float my, int sw, int sh) {
        UIRenderer ui = GameLoop.getInstance().getRenderer().getUIRenderer();
        if (!ui.getDevScroller().isMouseOver(mx, my)) return null;
        int cols = 7, slotSize = (int)(18 * Hotbar.HOTBAR_SCALE), spacing = (int)(2 * Hotbar.HOTBAR_SCALE);
        int devX = sw - (cols * (slotSize + spacing)) - 25;
        List<Item> items = new ArrayList<>(ItemRegistry.getAllItems().values());
        float off = ui.getDevScroller().getOffset();
        for (int i = 0; i < items.size(); i++) {
            int x = devX + (i % cols) * (slotSize + spacing), y = 40 + (i / cols) * (slotSize + spacing) - (int)off;
            if (my >= 40 && my <= 40 + ui.getDevScroller().getHeight() && mx >= x && mx <= x + slotSize && my >= y && my <= y + slotSize) return items.get(i);
        }
        return null;
    }

    private boolean pickPart3D(float mx, float my, int sw, int sh, AnimationEditorState state, List<ModelNode> allParts, AnimationEditorRenderer renderer) {
        Matrix4f inv = new Matrix4f().setPerspective((float)Math.toRadians(70.0f), (float)sw/sh, 0.01f, 1000.0f).invert();
        float nx = (2.0f * mx / sw) - 1.0f, ny = 1.0f - (2.0f * my / sh);
        Vector4f near = new Vector4f(nx, ny, -1, 1).mul(inv); near.div(near.w);
        Vector4f far = new Vector4f(nx, ny, 1, 1).mul(inv); far.div(far.w);
        Vector3f ro = new Vector3f(near.x, near.y, near.z), rd = new Vector3f(far.x - near.x, far.y - near.y, far.z - near.z).normalize();
        
        ModelNode best = null; float minD = Float.MAX_VALUE;
        com.za.minecraft.engine.graphics.DynamicTextureAtlas atlas = GameLoop.getInstance().getRenderer().getAtlas();

        for (ModelNode p : allParts) {
            // Item Check
            if (p.name.equals("item_attachment_r") && state.heldStack != null) {
                Mesh mesh = renderer.getViewmodelRenderer().getHeldItemRenderer().getOrGenerateMesh(state.heldStack.getItem(), atlas);
                if (mesh != null) {
                    Matrix4f itemLocal = new Matrix4f();
                    Item item = state.heldStack.getItem();
                    if (item.isBlock()) itemLocal.translate(0, 0.15f, -mesh.getMax().z * 0.4f).rotateX((float)Math.toRadians(30)).rotateY((float)Math.toRadians(15)).scale(0.4f);
                    else {
                        Vector3f go = mesh.getGraspOffset();
                        itemLocal.translate(0, -0.1f, 0).rotateY((float)Math.toRadians(-90)).scale(0.85f).translate(-go.x, -go.y, -go.z);
                    }
                    Matrix4f invItem = new Matrix4f(p.globalMatrix).mul(itemLocal).invert();
                    Vector3f iLro = ro.mulPosition(invItem, new Vector3f()), iLrd = rd.mulDirection(invItem, new Vector3f());
                    Vector2f res = new Vector2f();
                    if (Intersectionf.intersectRayAab(iLro, iLrd, mesh.getMin(), mesh.getMax(), res)) {
                        float t = res.x < 0 ? res.y : res.x;
                        if (t > 0 && t < minD) { minD = t; best = p; }
                    }
                }
            }
            // Bone Check
            if (p.def == null || p.def.cubes == null) continue;
            Matrix4f invG = new Matrix4f(p.globalMatrix).invert();
            Vector3f lro = ro.mulPosition(invG, new Vector3f()), lrd = rd.mulDirection(invG, new Vector3f());
            float px = p.def.pivot[0], py = p.def.pivot[1], pz = p.def.pivot[2];
            for (BoneDefinition.CubeDefinition c : p.def.cubes) {
                Vector3f min = new Vector3f((c.origin[0] - px)/16f, (c.origin[1] - py)/16f, (c.origin[2] - pz)/16f);
                Vector3f max = new Vector3f(min).add(new Vector3f(c.size[0]/16f, c.size[1]/16f, c.size[2]/16f));
                Vector2f res = new Vector2f();
                if (Intersectionf.intersectRayAab(lro, lrd, min, max, res)) {
                    float t = res.x < 0 ? res.y : res.x;
                    if (t > 0 && t < minD) { minD = t; best = p; }
                }
            }
        }
        if (best != null) { state.selectedPart = best; return true; }
        return false;
    }
}
