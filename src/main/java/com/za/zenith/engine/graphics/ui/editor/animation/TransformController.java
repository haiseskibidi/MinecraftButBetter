package com.za.zenith.engine.graphics.ui.editor.animation;

import com.za.zenith.engine.graphics.model.ModelNode;
import com.za.zenith.engine.graphics.model.BoneDefinition;
import com.za.zenith.engine.graphics.model.Viewmodel;
import org.joml.Intersectionf;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.List;
import java.util.Map;

public class TransformController {
    
    public enum TransformMode { NONE, GRAB, ROTATE }
    
    private TransformMode currentMode = TransformMode.NONE;
    private char activeAxis = ' ';
    private boolean dragging = false;
    
    private final Vector3f startAnimTranslation = new Vector3f();
    private final Vector3f startAnimRotation = new Vector3f();
    private final Vector3f startWorldPivot = new Vector3f();
    private final Vector3f startHitPos = new Vector3f();
    private final Vector3f startHitDir = new Vector3f();
    private final Matrix4f startGlobalMatrix = new Matrix4f();
    private float startGrabDist = 0;

    public TransformMode getCurrentMode() { return currentMode; }
    public void setCurrentMode(TransformMode mode) { this.currentMode = mode; }
    
    public char getActiveAxis() { return activeAxis; }
    public void setActiveAxis(char activeAxis) { this.activeAxis = activeAxis; }
    public boolean isDragging() { return dragging; }

    public void prepareGrab(AnimationEditorState state, float mx, float my, int sw, int sh) {
        if (state.selectedPart == null) return;
        dragging = true;
        Matrix4f m = state.selectedPart.globalMatrix;
        startWorldPivot.set(m.m30(), m.m31(), m.m32());
        startGlobalMatrix.set(m);
        startAnimTranslation.set(state.selectedPart.animTranslation);
        startAnimRotation.set(state.selectedPart.animRotation);
        Vector3f ro = new Vector3f(), rd = new Vector3f();
        getRay(mx, my, sw, sh, state, ro, rd);
        startGrabDist = ro.distance(startWorldPivot);
        startHitPos.set(startWorldPivot);
    }

    public void handleDrag(float mx, float my, float lastMx, float lastMy, int sw, int sh, AnimationEditorState state, List<ModelNode> allParts) {
        if (currentMode == TransformMode.NONE || state.selectedPart == null) return;
        
        Vector3f ro = new Vector3f(), rd = new Vector3f();
        getRay(mx, my, sw, sh, state, ro, rd);

        if (currentMode == TransformMode.GRAB) {
            Vector3f targetWorldPos = new Vector3f();
            if (activeAxis == ' ') {
                targetWorldPos.set(rd).mul(startGrabDist).add(ro);
            } else {
                Vector3f axisDir = new Vector3f();
                if (activeAxis == 'X') axisDir.set(1, 0, 0);
                else if (activeAxis == 'Y') axisDir.set(0, 1, 0);
                else if (activeAxis == 'Z') axisDir.set(0, 0, 1);
                if (state.selectedPart != state.cameraNode) startGlobalMatrix.transformDirection(axisDir).normalize();
                targetWorldPos.set(getClosestPointOnLine(ro, rd, startWorldPivot, axisDir));
            }

            Vector3f worldDelta = new Vector3f(targetWorldPos).sub(startHitPos);
            
            if (state.selectedPart == state.cameraNode) {
                state.selectedPart.animTranslation.set(startAnimTranslation).add(worldDelta);
            } else if (state.ikManager.isEnabled()) {
                // Check if current selected part is an effector of any IK chain
                boolean handledByIK = false;
                for (Map.Entry<String, com.za.zenith.engine.graphics.model.ik.IKChain> entry : state.ikManager.getChains().entrySet()) {
                    com.za.zenith.engine.graphics.model.ik.IKChain chain = entry.getValue();
                    if (chain.nodes.get(chain.nodes.size() - 1) == state.selectedPart) {
                        state.ikManager.getTargetPos(entry.getKey()).set(startWorldPivot).add(worldDelta);
                        handledByIK = true;
                        break;
                    }
                }
                
                if (!handledByIK) {
                    applyStandardTranslation(state, worldDelta, allParts);
                }
            } else {
                applyStandardTranslation(state, worldDelta, allParts);
            }
        } else if (currentMode == TransformMode.ROTATE) {
            // ... (rest of rotate logic)
            if (activeAxis == 'S') {
                float ang = (mx - lastMx) * 0.05f;
                state.selectedPart.animRotation.z += ang;
            } else if (activeAxis != ' ') {
                Vector3f axisDir = new Vector3f();
                if (activeAxis == 'X') axisDir.set(1, 0, 0);
                else if (activeAxis == 'Y') axisDir.set(0, 1, 0);
                else if (activeAxis == 'Z') axisDir.set(0, 0, 1);
                if (state.selectedPart != state.cameraNode) startGlobalMatrix.transformDirection(axisDir).normalize();
                
                Vector3f hit = new Vector3f();
                if (intersectRayPlane(ro, rd, startWorldPivot, axisDir, hit)) {
                    Vector3f currentHitDir = new Vector3f(hit).sub(startWorldPivot).normalize();
                    float cos = startHitDir.dot(currentHitDir);
                    float angle = (float) Math.acos(Math.max(-1, Math.min(1, cos)));
                    Vector3f cross = new Vector3f(startHitDir).cross(currentHitDir);
                    if (cross.dot(axisDir) < 0) angle = -angle;
                    state.selectedPart.animRotation.set(startAnimRotation).add(
                        activeAxis == 'X' ? angle : 0,
                        activeAxis == 'Y' ? angle : 0,
                        activeAxis == 'Z' ? angle : 0
                    );
                }
            }
        }
    }

    private void applyStandardTranslation(AnimationEditorState state, Vector3f worldDelta, List<ModelNode> allParts) {
        ModelNode root = findRoot(allParts);
        ModelNode parent = findParent(root, state.selectedPart);
        Matrix4f invParent = new Matrix4f();
        if (parent != null) parent.globalMatrix.invert(invParent);
        Vector3f localDelta = new Vector3f();
        worldDelta.mulDirection(invParent, localDelta);
        state.selectedPart.animTranslation.set(startAnimTranslation).add(localDelta);
    }

    private Viewmodel getViewmodel() {
        com.za.zenith.engine.graphics.ui.Screen s = com.za.zenith.engine.graphics.ui.ScreenManager.getInstance().getActiveScreen();
        if (s instanceof AnimationEditorScreen editor) return editor.getViewmodel();
        return null;
    }

    public boolean pickGizmo(float mx, float my, int sw, int sh, AnimationEditorState state) {
        if (state.selectedPart == null) return false;
        Vector3f ro = new Vector3f(), rd = new Vector3f();
        getRay(mx, my, sw, sh, state, ro, rd);
        Matrix4f model = state.selectedPart.globalMatrix;
        Vector3f pos = new Vector3f(model.m30(), model.m31(), model.m32());
        float hitRadius = 0.015f, axisLen = 0.22f, ringRadius = 0.2f;

        Vector3f xDir = new Vector3f(1, 0, 0); model.transformDirection(xDir).normalize();
        Vector3f hitX = getClosestPointOnLine(ro, rd, pos, xDir);
        if (hitX.distance(pos) < axisLen && hitX.distance(getClosestPointOnRay(ro, rd, hitX)) < hitRadius) {
            setupGizmoPick(state, TransformMode.GRAB, 'X', hitX); return true;
        }
        Vector3f yDir = new Vector3f(0, 1, 0); model.transformDirection(yDir).normalize();
        Vector3f hitY = getClosestPointOnLine(ro, rd, pos, yDir);
        if (hitY.distance(pos) < axisLen && hitY.distance(getClosestPointOnRay(ro, rd, hitY)) < hitRadius) {
            setupGizmoPick(state, TransformMode.GRAB, 'Y', hitY); return true;
        }
        Vector3f zDir = new Vector3f(0, 0, 1); model.transformDirection(zDir).normalize();
        Vector3f hitZ = getClosestPointOnLine(ro, rd, pos, zDir);
        if (hitZ.distance(pos) < axisLen && hitZ.distance(getClosestPointOnRay(ro, rd, hitZ)) < hitRadius) {
            setupGizmoPick(state, TransformMode.GRAB, 'Z', hitZ); return true;
        }

        Vector3f viewDir = new Vector3f(0, 0, 1);
        Vector3f hitSRing = new Vector3f();
        if (intersectRayPlane(ro, rd, pos, viewDir, hitSRing)) {
            if (Math.abs(hitSRing.distance(pos) - (ringRadius * 1.5f)) < 0.03f) {
                setupGizmoPick(state, TransformMode.ROTATE, 'S', hitSRing); return true;
            }
        }

        Vector3f closestToCenter = getClosestPointOnRay(ro, rd, pos);
        if (closestToCenter.distance(pos) < 0.02f) return false;

        Vector3f hitXRing = new Vector3f();
        if (intersectRayPlane(ro, rd, pos, xDir, hitXRing) && Math.abs(hitXRing.distance(pos) - ringRadius) < 0.02f) {
            setupGizmoPick(state, TransformMode.ROTATE, 'X', hitXRing); return true;
        }
        Vector3f hitYRing = new Vector3f();
        if (intersectRayPlane(ro, rd, pos, yDir, hitYRing) && Math.abs(hitYRing.distance(pos) - ringRadius) < 0.02f) {
            setupGizmoPick(state, TransformMode.ROTATE, 'Y', hitYRing); return true;
        }
        Vector3f hitZRing = new Vector3f();
        if (intersectRayPlane(ro, rd, pos, zDir, hitZRing) && Math.abs(hitZRing.distance(pos) - ringRadius) < 0.02f) {
            setupGizmoPick(state, TransformMode.ROTATE, 'Z', hitZRing); return true;
        }

        return false;
    }

    private void setupGizmoPick(AnimationEditorState state, TransformMode mode, char axis, Vector3f hitPos) {
        currentMode = mode;
        activeAxis = axis;
        dragging = true;
        startAnimTranslation.set(state.selectedPart.animTranslation);
        startAnimRotation.set(state.selectedPart.animRotation);
        Matrix4f m = state.selectedPart.globalMatrix;
        startWorldPivot.set(m.m30(), m.m31(), m.m32());
        startGlobalMatrix.set(m);
        startHitPos.set(hitPos);
        startHitDir.set(hitPos).sub(startWorldPivot).normalize();
    }

    public boolean pickPart3D(float mx, float my, int sw, int sh, AnimationEditorState state, List<ModelNode> allParts) {
        Vector3f ro = new Vector3f(), rd = new Vector3f();
        getRay(mx, my, sw, sh, state, ro, rd);
        ModelNode best = null; float minD = Float.MAX_VALUE;
        for (ModelNode p : allParts) {
            if (p == state.cameraNode || p.def == null || p.def.cubes == null) continue;
            Matrix4f invG = new Matrix4f(p.globalMatrix).invert();
            Vector3f lro = ro.mulPosition(invG, new Vector3f()), lrd = rd.mulDirection(invG, new Vector3f());
            
            for (BoneDefinition.CubeDefinition c : p.def.cubes) {
                // В новой системе c.x/y/z уже относительны пивота.
                // Но нам нужно учесть инверсию Z в меше: glZ = -(cubeZ + depth)
                float minX = c.x / 16.0f;
                float minY = c.y / 16.0f;
                float minZ = -(c.z / 16.0f + c.size[2] / 16.0f);
                
                Vector3f min = new Vector3f(minX, minY, minZ);
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

    public void getRay(float mx, float my, int sw, int sh, AnimationEditorState state, Vector3f outRo, Vector3f outRd) {
        float fov = (float)Math.toRadians(70.0f) + state.cameraNode.animTranslation.z;
        Matrix4f proj = new Matrix4f().setPerspective(fov, (float)sw/sh, 0.01f, 1000.0f);
        Matrix4f view = new Matrix4f().identity()
            .rotateZ(-state.cameraNode.animRotation.z)
            .rotateX(-state.cameraNode.animRotation.x)
            .translate(-state.cameraNode.animTranslation.x, -state.cameraNode.animTranslation.y, 0);
        Matrix4f inv = new Matrix4f(proj).mul(view).invert();
        float nx = (2.0f * mx / sw) - 1.0f, ny = 1.0f - (2.0f * my / sh);
        Vector4f near = new Vector4f(nx, ny, -1, 1).mul(inv); near.div(near.w);
        Vector4f far = new Vector4f(nx, ny, 1, 1).mul(inv); far.div(far.w);
        outRo.set(near.x, near.y, near.z);
        outRd.set(far.x - near.x, far.y - near.y, far.z - near.z).normalize();
    }

    private Vector3f getClosestPointOnLine(Vector3f ro, Vector3f rd, Vector3f linePos, Vector3f lineDir) {
        Vector3f p13 = new Vector3f(ro).sub(linePos);
        float d1343 = p13.dot(lineDir), d4321 = lineDir.dot(rd), d1321 = p13.dot(rd), d4343 = lineDir.dot(lineDir), d2121 = rd.dot(rd);
        float denom = d2121 * d4343 - d4321 * d4321;
        if (Math.abs(denom) < 0.0001f) return new Vector3f(linePos);
        float numer = d1343 * d4321 - d1321 * d4343;
        float s = numer / denom, t = (d1343 + d4321 * s) / d4343;
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

    private Vector3f getClosestPointOnRay(Vector3f ro, Vector3f rd, Vector3f point) {
        Vector3f w = new Vector3f(point).sub(ro);
        float t = w.dot(rd);
        return t < 0 ? new Vector3f(ro) : new Vector3f(rd).mul(t).add(ro);
    }

    private ModelNode findRoot(List<ModelNode> allParts) {
        for (ModelNode n : allParts) if (n.name.equals("root")) return n;
        return allParts.isEmpty() ? null : allParts.get(0);
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

    public void stopDragging() {
        dragging = false;
    }

    public void cancelTransform(AnimationEditorState state) {
        if (state.selectedPart != null) {
            state.selectedPart.animTranslation.set(startAnimTranslation);
            state.selectedPart.animRotation.set(startAnimRotation);
        }
        currentMode = TransformMode.NONE;
        activeAxis = ' ';
        dragging = false;
    }
}


