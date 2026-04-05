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
    private final Vector3f startWorldPivot = new Vector3f();
    private final Vector3f startHitPos = new Vector3f();
    private final Vector3f startHitDir = new Vector3f();
    private float startGrabDist = 0;

    public TransformMode getCurrentMode() { return currentMode; }
    public char getActiveAxis() { return activeAxis; }

    public void handleMouseClick(float mx, float my, int button, AnimationEditorState state, List<ModelNode> allParts, AnimationEditorRenderer renderer) {
        int sw = GameLoop.getInstance().getWindow().getWidth(), sh = GameLoop.getInstance().getWindow().getHeight();
        
        if (currentMode != TransformMode.NONE) {
            if (button == 0) { // Confirm
                confirmTransform(state);
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

        // 1. Check Dev Panel
        Item devItem = getDevItemAt(mx, my, sw, sh);
        if (devItem != null) {
            state.heldStack = new ItemStack(devItem);
            return;
        }

        // 2. Check Timeline
        if (my >= sh - 100) {
            state.isScrubbing = true;
            float targetTime = Math.max(0, Math.min(1, (mx - 60) / (float)(sw - 120)));
            
            // Shift-snap to keyframes
            long windowHandle = GameLoop.getInstance().getWindow().getWindowHandle();
            boolean shift = glfwGetKey(windowHandle, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS || glfwGetKey(windowHandle, GLFW_KEY_RIGHT_SHIFT) == GLFW_PRESS;
            if (shift && state.selectedPart != null) {
                AnimationEditorState.EditorTrack track = state.tracks.get(state.selectedPart.name);
                if (track != null) {
                    for (AnimationEditorState.EditorKeyframe k : track.keyframes) {
                        if (Math.abs(k.time() - targetTime) < 0.03f) {
                            targetTime = k.time();
                            break;
                        }
                    }
                }
            }
            state.currentTime = targetTime;
            return;
        }

        // 3. 3D Picking (Gizmos first, then bones)
        if (pickGizmo(mx, my, sw, sh, state)) {
            startAnimTranslation.set(state.selectedPart.animTranslation);
            startAnimRotation.set(state.selectedPart.animRotation);
            Matrix4f m = state.selectedPart.globalMatrix;
            startWorldPivot.set(m.m30(), m.m31(), m.m32());
            
            startHitDir.set(startHitPos).sub(startWorldPivot).normalize();
            return;
        }
        if (pickPart3D(mx, my, sw, sh, state, allParts, renderer)) return;

        // 4. Parts List
        if (mx <= 230 && my >= 120) {
            List<ModelNode> flat = state.getFlatPartList(findRoot(allParts));
            int idx = (int)((my - 130) / 22);
            if (idx >= 0 && idx < flat.size()) {
                state.selectedPart = flat.get(idx);
            }
        }
    }

    private void confirmTransform(AnimationEditorState state) {
        if (state.selectedPart != null) {
            AnimationEditorState.EditorTrack track = state.tracks.get(state.selectedPart.name);
            if (track != null) {
                track.addKey(state.currentTime, state.selectedPart.animTranslation, state.selectedPart.animRotation);
            }
        }
    }

    private void prepareGrab(AnimationEditorState state, float mx, float my, int sw, int sh) {
        if (state.selectedPart == null) return;
        Matrix4f m = state.selectedPart.globalMatrix;
        startWorldPivot.set(m.m30(), m.m31(), m.m32());
        startAnimTranslation.set(state.selectedPart.animTranslation);
        startAnimRotation.set(state.selectedPart.animRotation);
        
        Vector3f ro = new Vector3f(), rd = new Vector3f();
        getRay(mx, my, sw, sh, ro, rd);
        startGrabDist = ro.distance(startWorldPivot);
        startHitPos.set(startWorldPivot);
    }

    public void handleMouseMove(float mx, float my, AnimationEditorState state, List<ModelNode> allParts) {
        int sw = GameLoop.getInstance().getWindow().getWidth(), sh = GameLoop.getInstance().getWindow().getHeight();
        
        if (state.isScrubbing) {
            state.currentTime = Math.max(0, Math.min(1, (mx - 60) / (float)(sw - 120)));
        } else if (currentMode != TransformMode.NONE && state.selectedPart != null) {
            Vector3f ro = new Vector3f(), rd = new Vector3f();
            getRay(mx, my, sw, sh, ro, rd);

            if (currentMode == TransformMode.GRAB) {
                Vector3f currentHitPos = new Vector3f();
                if (activeAxis == ' ') {
                    currentHitPos.set(rd).mul(startGrabDist).add(ro);
                } else {
                    Vector3f axisDir = new Vector3f();
                    if (activeAxis == 'X') axisDir.set(1, 0, 0);
                    else if (activeAxis == 'Y') axisDir.set(0, 1, 0);
                    else if (activeAxis == 'Z') axisDir.set(0, 0, 1);
                    state.selectedPart.globalMatrix.transformDirection(axisDir).normalize();
                    currentHitPos.set(getClosestPointOnLine(ro, rd, startWorldPivot, axisDir));
                }

                Vector3f worldDelta = new Vector3f(currentHitPos).sub(startHitPos);
                ModelNode rootNode = findRoot(allParts);
                ModelNode parent = findParent(rootNode, state.selectedPart);
                Matrix4f invParent = new Matrix4f();
                if (parent != null) parent.globalMatrix.invert(invParent);
                
                Vector3f localDelta = new Vector3f();
                worldDelta.mulDirection(invParent, localDelta);
                state.selectedPart.animTranslation.set(startAnimTranslation).add(localDelta);
            } else if (currentMode == TransformMode.ROTATE) {
                if (activeAxis == 'S') {
                    // Project pivot to screen space
                    Matrix4f proj = new Matrix4f().setPerspective((float)Math.toRadians(70.0f), (float)sw/sh, 0.01f, 1000.0f);
                    Vector4f screenPivot = new Vector4f(startWorldPivot, 1).mul(proj);
                    screenPivot.div(screenPivot.w);
                    float pivotX = (screenPivot.x + 1.0f) * 0.5f * sw;
                    float pivotY = (1.0f - screenPivot.y) * 0.5f * sh;

                    float startAngle = (float) Math.atan2(lastMousePos.y - pivotY, lastMousePos.x - pivotX);
                    float currentAngle = (float) Math.atan2(my - pivotY, mx - pivotX);
                    state.selectedPart.animRotation.z = startAnimRotation.z + (currentAngle - startAngle);
                } else {
                    Vector3f axisDir = new Vector3f();
                    if (activeAxis == 'X') axisDir.set(1, 0, 0);
                    else if (activeAxis == 'Y') axisDir.set(0, 1, 0);
                    else if (activeAxis == 'Z') axisDir.set(0, 0, 1);
                    state.selectedPart.globalMatrix.transformDirection(axisDir).normalize();

                    Vector3f hit = new Vector3f();
                    if (intersectRayPlane(ro, rd, startWorldPivot, axisDir, hit)) {
                        Vector3f currentHitDir = new Vector3f(hit).sub(startWorldPivot).normalize();
                        float cos = startHitDir.dot(currentHitDir);
                        float angle = (float) Math.acos(Math.max(-1, Math.min(1, cos)));
                        Vector3f cross = new Vector3f(startHitDir).cross(currentHitDir);
                        if (cross.dot(axisDir) < 0) angle = -angle;

                        if (activeAxis == 'X') state.selectedPart.animRotation.x = startAnimRotation.x + angle;
                        if (activeAxis == 'Y') state.selectedPart.animRotation.y = startAnimRotation.y + angle;
                        if (activeAxis == 'Z') state.selectedPart.animRotation.z = startAnimRotation.z + angle;
                    }
                }
            }
        }
        lastMousePos.set(mx, my);
        
        List<ModelNode> flat = state.getFlatPartList(findRoot(allParts));
        state.hoveredPartIndex = (mx <= 230 && my >= 130) ? (int)((my - 130) / 22) : -1;
        if (state.hoveredPartIndex >= flat.size()) state.hoveredPartIndex = -1;
    }

    private ModelNode findParent(ModelNode root, ModelNode target) {
        if (root == null || target == null) return null;
        for (ModelNode child : root.children) {
            if (child == target) return root;
            ModelNode found = findParent(child, target);
            if (found != null) return found;
        }
        return null;
    }

    private void getRay(float mx, float my, int sw, int sh, Vector3f outRo, Vector3f outRd) {
        Matrix4f inv = new Matrix4f().setPerspective((float)Math.toRadians(70.0f), (float)sw/sh, 0.01f, 1000.0f).invert();
        float nx = (2.0f * mx / sw) - 1.0f, ny = 1.0f - (2.0f * my / sh);
        Vector4f near = new Vector4f(nx, ny, -1, 1).mul(inv); near.div(near.w);
        Vector4f far = new Vector4f(nx, ny, 1, 1).mul(inv); far.div(far.w);
        outRo.set(near.x, near.y, near.z);
        outRd.set(far.x - near.x, far.y - near.y, far.z - near.z).normalize();
    }

    private Vector3f getClosestPointOnLine(Vector3f ro, Vector3f rd, Vector3f linePos, Vector3f lineDir) {
        Vector3f p13 = new Vector3f(ro).sub(linePos);
        float d1343 = p13.dot(lineDir);
        float d4321 = lineDir.dot(rd);
        float d1321 = p13.dot(rd);
        float d4343 = lineDir.dot(lineDir);
        float d2121 = rd.dot(rd);
        float denom = d2121 * d4343 - d4321 * d4321;
        if (Math.abs(denom) < 0.0001f) return new Vector3f(linePos);
        float numer = d1343 * d4321 - d1321 * d4343;
        float s = numer / denom;
        float t = (d1343 + d4321 * s) / d4343;
        return new Vector3f(lineDir).mul(t).add(linePos);
    }

    private boolean intersectRayPlane(Vector3f ro, Vector3f rd, Vector3f p, Vector3f n, Vector3f outHit) {
        float denom = n.dot(rd);
        if (Math.abs(denom) < 0.0001f) return false;
        float t = new Vector3f(p).sub(ro).dot(n) / denom;
        if (t < 0) return false;
        outHit.set(rd).mul(t).add(ro);
        return true;
    }

    private boolean pickGizmo(float mx, float my, int sw, int sh, AnimationEditorState state) {
        if (state.selectedPart == null) return false;
        Vector3f ro = new Vector3f(), rd = new Vector3f();
        getRay(mx, my, sw, sh, ro, rd);

        Matrix4f model = state.selectedPart.globalMatrix;
        Vector3f pos = new Vector3f(model.m30(), model.m31(), model.m32());
        float hitRadius = 0.015f; 
        float axisLen = 0.22f;
        float ringRadius = 0.2f;

        // Check axes first
        Vector3f xDir = new Vector3f(1, 0, 0); model.transformDirection(xDir).normalize();
        Vector3f hitX = getClosestPointOnLine(ro, rd, pos, xDir);
        if (hitX.distance(pos) < axisLen && hitX.distance(getClosestPointOnRay(ro, rd, hitX)) < hitRadius) {
            currentMode = TransformMode.GRAB; activeAxis = 'X'; startHitPos.set(hitX); return true;
        }
        Vector3f yDir = new Vector3f(0, 1, 0); model.transformDirection(yDir).normalize();
        Vector3f hitY = getClosestPointOnLine(ro, rd, pos, yDir);
        if (hitY.distance(pos) < axisLen && hitY.distance(getClosestPointOnRay(ro, rd, hitY)) < hitRadius) {
            currentMode = TransformMode.GRAB; activeAxis = 'Y'; startHitPos.set(hitY); return true;
        }
        Vector3f zDir = new Vector3f(0, 0, 1); model.transformDirection(zDir).normalize();
        Vector3f hitZ = getClosestPointOnLine(ro, rd, pos, zDir);
        if (hitZ.distance(pos) < axisLen && hitZ.distance(getClosestPointOnRay(ro, rd, hitZ)) < hitRadius) {
            currentMode = TransformMode.GRAB; activeAxis = 'Z'; startHitPos.set(hitZ); return true;
        }

        // Check White Ring (Screen Space outer rim)
        Vector3f viewDir = new Vector3f(0, 0, 1); // View direction in studio
        Vector3f hitSRing = new Vector3f();
        if (intersectRayPlane(ro, rd, pos, viewDir, hitSRing)) {
            if (Math.abs(hitSRing.distance(pos) - (ringRadius * 1.5f)) < 0.03f) {
                currentMode = TransformMode.ROTATE; activeAxis = 'S'; startHitPos.set(hitSRing); return true;
            }
        }

        // Deadzone check for inner rings
        Vector3f closestToCenter = getClosestPointOnRay(ro, rd, pos);
        if (closestToCenter.distance(pos) < 0.02f) return false;

        // Check inner rings
        Vector3f hitXRing = new Vector3f();
        if (intersectRayPlane(ro, rd, pos, xDir, hitXRing) && Math.abs(hitXRing.distance(pos) - ringRadius) < 0.02f) {
            currentMode = TransformMode.ROTATE; activeAxis = 'X'; startHitPos.set(hitXRing); return true;
        }
        Vector3f hitYRing = new Vector3f();
        if (intersectRayPlane(ro, rd, pos, yDir, hitYRing) && Math.abs(hitYRing.distance(pos) - ringRadius) < 0.02f) {
            currentMode = TransformMode.ROTATE; activeAxis = 'Y'; startHitPos.set(hitYRing); return true;
        }
        Vector3f hitZRing = new Vector3f();
        if (intersectRayPlane(ro, rd, pos, zDir, hitZRing) && Math.abs(hitZRing.distance(pos) - ringRadius) < 0.02f) {
            currentMode = TransformMode.ROTATE; activeAxis = 'Z'; startHitPos.set(hitZRing); return true;
        }

        return false;
    }

    private Vector3f getClosestPointOnRay(Vector3f ro, Vector3f rd, Vector3f point) {
        Vector3f w = new Vector3f(point).sub(ro);
        float t = w.dot(rd);
        if (t < 0) return new Vector3f(ro);
        return new Vector3f(rd).mul(t).add(ro);
    }

    private boolean intersectLine(Vector3f ro, Vector3f rd, Vector3f a, Vector3f b, float radius) {
        Vector3f ba = new Vector3f(b).sub(a);
        Vector3f oa = new Vector3f(ro).sub(a);
        float baba = ba.dot(ba), rdba = rd.dot(ba), oaba = oa.dot(ba), rdma = rd.dot(oa);
        float a1 = baba, b1 = -rdba, c1 = oaba, a2 = rdba, b2 = -1.0f, c2 = rdma;
        float det = a1 * b2 - a2 * b1;
        if (Math.abs(det) < 0.0001f) return false;
        float t = (c1 * b2 - c2 * b1) / det, s = (a1 * c2 - a2 * c1) / det;
        if (t < 0 || t > 1) return false;
        return new Vector3f(ba).mul(t).add(a).distance(new Vector3f(rd).mul(s).add(ro)) < radius;
    }

    private ModelNode findRoot(List<ModelNode> allParts) {
        for (ModelNode n : allParts) if (n.name.equals("root")) return n;
        return allParts.isEmpty() ? null : allParts.get(0);
    }

    public boolean handleKeyPress(int key, AnimationEditorState state) {
        long window = GameLoop.getInstance().getWindow().getWindowHandle();
        int sw = GameLoop.getInstance().getWindow().getWidth(), sh = GameLoop.getInstance().getWindow().getHeight();
        boolean ctrl = glfwGetKey(window, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS;
        boolean shift = glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS || glfwGetKey(window, GLFW_KEY_RIGHT_SHIFT) == GLFW_PRESS;
        
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
            if (shift) {
                AnimationEditorState.EditorTrack track = state.tracks.get(state.selectedPart.name);
                if (track != null) {
                    if (key == GLFW_KEY_1) { track.easing = AnimationEditorState.EasingType.LINEAR; return true; }
                    if (key == GLFW_KEY_2) { track.easing = AnimationEditorState.EasingType.SINE_IN_OUT; return true; }
                    if (key == GLFW_KEY_3) { track.easing = AnimationEditorState.EasingType.QUAD_IN_OUT; return true; }
                    if (key == GLFW_KEY_4) { track.easing = AnimationEditorState.EasingType.CUBIC_IN_OUT; return true; }
                }
            }

            if (key == GLFW_KEY_G) {
                currentMode = TransformMode.GRAB;
                prepareGrab(state, lastMousePos.x, lastMousePos.y, sw, sh);
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

    public void handleMouseRelease(int button, AnimationEditorState state) {
        if (currentMode != TransformMode.NONE) {
            confirmTransform(state);
            currentMode = TransformMode.NONE;
            activeAxis = ' ';
            Logger.info("Transform finished (Auto-Key).");
        }
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
        Vector3f ro = new Vector3f(), rd = new Vector3f();
        getRay(mx, my, sw, sh, ro, rd);
        ModelNode best = null; float minD = Float.MAX_VALUE;
        com.za.minecraft.engine.graphics.DynamicTextureAtlas atlas = GameLoop.getInstance().getRenderer().getAtlas();
        for (ModelNode p : allParts) {
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
