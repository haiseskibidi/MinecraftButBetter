package com.za.minecraft.entities.parkour.animation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AnimationTrack {
    private final List<Keyframe> keyframes = new ArrayList<>();
    private boolean mirror = false;

    public void addKeyframe(Keyframe keyframe) {
        keyframes.add(keyframe);
        keyframes.sort(Comparator.comparingDouble(Keyframe::time));
    }

    public boolean isMirror() {
        return mirror;
    }

    public void setMirror(boolean mirror) {
        this.mirror = mirror;
    }

    public float evaluate(float t) {
        if (keyframes.isEmpty()) return 0.0f;
        if (t <= keyframes.get(0).time()) return keyframes.get(0).value();
        if (t >= keyframes.get(keyframes.size() - 1).time()) return keyframes.get(keyframes.size() - 1).value();

        for (int i = 0; i < keyframes.size() - 1; i++) {
            Keyframe current = keyframes.get(i);
            Keyframe next = keyframes.get(i + 1);

            if (t >= current.time() && t <= next.time()) {
                float segmentT = (t - current.time()) / (next.time() - current.time());
                return interpolate(current.value(), next.value(), segmentT, current.easing());
            }
        }
        return 0.0f;
    }

    private float interpolate(float start, float end, float t, String easing) {
        float easedT = switch (easing.toLowerCase()) {
            case "sine" -> (float) (Math.sin(t * Math.PI / 2.0));
            case "sine_pi" -> (float) (Math.sin(t * Math.PI));
            case "smoothstep" -> t * t * (3 - 2 * t);
            case "smootherstep" -> t * t * t * (t * (t * 6 - 15) + 10);
            case "quad_in" -> t * t;
            case "quad_out" -> 1.0f - (1.0f - t) * (1.0f - t);
            case "linear" -> t;
            default -> t;
        };
        return start + (end - start) * easedT;
    }
}
