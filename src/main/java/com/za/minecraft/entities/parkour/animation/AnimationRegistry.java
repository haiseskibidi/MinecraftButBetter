package com.za.minecraft.entities.parkour.animation;

import java.util.HashMap;
import java.util.Map;

public class AnimationRegistry {
    private static final Map<String, ParkourAnimation> animations = new HashMap<>();

    public static void register(String name, ParkourAnimation animation) {
        animations.put(name, animation);
    }

    public static ParkourAnimation get(String name) {
        return animations.get(name);
    }
}
