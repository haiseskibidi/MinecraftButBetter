package com.za.minecraft.entities.parkour.animation;

import java.util.HashMap;
import java.util.Map;

public class AnimationProfile {
    private final String name;
    private float duration = 0.5f;
    private String durationKey = null;
    private boolean looping = false;
    private final Map<String, AnimationTrack> tracks = new HashMap<>();
    
    // Path settings
    private String pathType = "linear";
    private String pathInterpolation = "linear";
    private float apexYOffset = 0.0f;
    
    // Jitter settings
    private boolean jitterEnabled = false;
    private float jitterStart = 0.0f;
    private float jitterEnd = 1.0f;
    private float jitterIntensity = 0.0f;

    public AnimationProfile(String name) {
        this.name = name;
    }

    public void addTrack(String param, AnimationTrack track) {
        tracks.put(param, track);
    }

    public float evaluate(String param, float t, float multiplier) {
        AnimationTrack track = tracks.get(param);
        if (track == null) return 0.0f;
        
        float finalT = looping ? t % 1.0f : t;
        float val = track.evaluate(finalT);
        return track.isMirrored() ? val * multiplier : val;
    }

    public String getName() { return name; }
    public float getDuration() { return duration; }
    public void setDuration(float duration) { this.duration = duration; }
    public String getDurationKey() { return durationKey; }
    public void setDurationKey(String durationKey) { this.durationKey = durationKey; }
    public boolean isLooping() { return looping; }
    public void setLooping(boolean looping) { this.looping = looping; }
    public String getPathType() { return pathType; }
    public void setPathType(String pathType) { this.pathType = pathType; }
    public String getPathInterpolation() { return pathInterpolation; }
    public void setPathInterpolation(String pathInterpolation) { this.pathInterpolation = pathInterpolation; }
    public float getApexYOffset() { return apexYOffset; }
    public void setApexYOffset(float apexYOffset) { this.apexYOffset = apexYOffset; }
    public boolean isJitterEnabled() { return jitterEnabled; }
    public void setJitterEnabled(boolean jitterEnabled) { this.jitterEnabled = jitterEnabled; }
    public float getJitterStart() { return jitterStart; }
    public void setJitterStart(float jitterStart) { this.jitterStart = jitterStart; }
    public float getJitterEnd() { return jitterEnd; }
    public void setJitterEnd(float jitterEnd) { this.jitterEnd = jitterEnd; }
    public float getJitterIntensity() { return jitterIntensity; }
    public void setJitterIntensity(float jitterIntensity) { this.jitterIntensity = jitterIntensity; }
}
