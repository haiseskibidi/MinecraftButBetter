package com.za.zenith.entities.parkour.animation;

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

    public void sort() {
        keyframes.sort(Comparator.comparingDouble(Keyframe::time));
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
        float easedT = EasingRegistry.evaluate(easing, t);
        return start + (end - start) * easedT;
    }
}


