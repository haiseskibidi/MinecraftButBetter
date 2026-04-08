package com.za.zenith.engine.graphics.ui.editor.animation;

import com.za.zenith.engine.graphics.model.ModelNode;
import com.za.zenith.world.items.ItemStack;
import org.joml.Vector3f;

import java.util.*;

/**
 * Holds the persistent state of the animation being edited.
 */
public class AnimationEditorState {
    
    public enum EasingType { LINEAR, SINE_IN_OUT, QUAD_IN_OUT, CUBIC_IN_OUT }
    
    public record EditorKeyframe(float time, Vector3f pos, Vector3f rot) {}
    
    public static class EditorTrack {
        public final List<EditorKeyframe> keyframes = new ArrayList<>();
        public EasingType easing = EasingType.LINEAR;
        
        public void addKey(float time, Vector3f pos, Vector3f rot) {
            keyframes.removeIf(k -> Math.abs(k.time - time) < 0.005f);
            keyframes.add(new EditorKeyframe(time, new Vector3f(pos), new Vector3f(rot)));
            keyframes.sort(Comparator.comparingDouble(k -> k.time));
        }

        public void removeNear(float time) {
            keyframes.removeIf(k -> Math.abs(k.time - time) < 0.05f);
        }

        public void evaluate(float time, Vector3f outPos, Vector3f outRot) {
            if (keyframes.isEmpty()) return;
            if (keyframes.size() == 1) {
                outPos.set(keyframes.get(0).pos);
                outRot.set(keyframes.get(0).rot);
                return;
            }

            EditorKeyframe first = keyframes.get(0);
            if (time <= first.time) { outPos.set(first.pos); outRot.set(first.rot); return; }
            EditorKeyframe last = keyframes.get(keyframes.size() - 1);
            if (time >= last.time) { outPos.set(last.pos); outRot.set(last.rot); return; }

            for (int i = 0; i < keyframes.size() - 1; i++) {
                EditorKeyframe k1 = keyframes.get(i);
                EditorKeyframe k2 = keyframes.get(i + 1);
                if (time >= k1.time && time <= k2.time) {
                    float t = (time - k1.time) / (k2.time - k1.time);
                    float alpha = applyEasing(t);
                    k1.pos.lerp(k2.pos, alpha, outPos);
                    k1.rot.lerp(k2.rot, alpha, outRot);
                    return;
                }
            }
        }

        private float applyEasing(float t) {
            switch (easing) {
                case SINE_IN_OUT: return (float) (-(Math.cos(Math.PI * t) - 1) / 2.0);
                case QUAD_IN_OUT: return t < 0.5 ? 2 * t * t : 1 - (float)Math.pow(-2 * t + 2, 2) / 2;
                case CUBIC_IN_OUT: return t < 0.5 ? 4 * t * t * t : 1 - (float)Math.pow(-2 * t + 2, 3) / 2;
                default: return t;
            }
        }
    }

    // --- State Variables ---
    public float currentTime = 0.0f;
    public boolean isPlaying = false;
    public boolean isScrubbing = false;
    
    public final Map<String, EditorTrack> tracks = new HashMap<>();
    public ModelNode selectedPart;
    public int hoveredPartIndex = -1;
    public ItemStack heldStack;

    // UI Interaction
    public String editingProperty = null; // Name of the property being typed
    public String editingValue = "";      // Current string buffer
    
    // Virtual Camera node
    public final ModelNode cameraNode = new ModelNode("Camera", null);
    public boolean onionSkinning = false;
    
    public final EditorIKManager ikManager = new EditorIKManager();

    public void evaluateAll(List<ModelNode> parts, boolean isTransforming) {
        // Evaluate bones
        for (ModelNode part : parts) {
            if (isTransforming && part == selectedPart) continue;
            
            // Reset to default
            part.animTranslation.set(0, 0, 0);
            part.animRotation.set(0, 0, 0);
            
            EditorTrack track = tracks.get(part.name);
            if (track != null && !track.keyframes.isEmpty()) {
                track.evaluate(currentTime, part.animTranslation, part.animRotation);
            }
        }
        // Evaluate camera
        if (!(isTransforming && selectedPart == cameraNode)) {
            cameraNode.animTranslation.set(0, 0, 0);
            cameraNode.animRotation.set(0, 0, 0);
            EditorTrack camTrack = tracks.get("Camera");
            if (camTrack != null && !camTrack.keyframes.isEmpty()) {
                camTrack.evaluate(currentTime, cameraNode.animTranslation, cameraNode.animRotation);
            }
        }
    }

    public record FlatNode(ModelNode node, int depth) {}

    public List<FlatNode> getFlatPartList(ModelNode root) {
        List<FlatNode> list = new ArrayList<>();
        list.add(new FlatNode(cameraNode, 0)); 
        flatten(root, 0, list);
        return list;
    }

    private void flatten(ModelNode node, int depth, List<FlatNode> out) {
        if (node == null) return;
        if (!node.name.equals("root")) {
            out.add(new FlatNode(node, depth));
        }
        int nextDepth = node.name.equals("root") ? depth : depth + 1;
        for (ModelNode child : node.children) flatten(child, nextDepth, out);
    }
}


