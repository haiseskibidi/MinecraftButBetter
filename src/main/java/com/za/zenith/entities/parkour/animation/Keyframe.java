package com.za.zenith.entities.parkour.animation;

/**
 * Keyframe for animation tracks. 
 * Converted to a class for better reflection and serialization support.
 */
public class Keyframe {
    public float time;
    public float value;
    public String easing;

    public Keyframe() {}

    public Keyframe(float time, float value, String easing) {
        this.time = time;
        this.value = value;
        this.easing = easing;
    }

    // Record-like getters for compatibility with AnimationTrack
    public float time() { return time; }
    public float value() { return value; }
    public String easing() { return easing; }
}
