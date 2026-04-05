package com.za.minecraft.engine.graphics.ui.editor.animation;

import com.za.minecraft.engine.graphics.model.ModelNode;
import com.za.minecraft.world.items.ItemStack;
import org.joml.Vector3f;

import java.util.*;

/**
 * Holds the persistent state of the animation being edited.
 */
public class AnimationEditorState {
    
    public record EditorKeyframe(float time, Vector3f pos, Vector3f rot) {}
    
    public static class EditorTrack {
        public final List<EditorKeyframe> keyframes = new ArrayList<>();
        
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
                    k1.pos.lerp(k2.pos, t, outPos);
                    k1.rot.lerp(k2.rot, t, outRot);
                    return;
                }
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

    public void evaluateAll(List<ModelNode> parts, boolean isTransforming) {
        for (ModelNode part : parts) {
            // Don't overwrite with animation if the user is currently manually moving it
            if (isTransforming && part == selectedPart) continue;
            
            EditorTrack track = tracks.get(part.name);
            if (track != null && !track.keyframes.isEmpty()) {
                track.evaluate(currentTime, part.animTranslation, part.animRotation);
            }
        }
    }
}
