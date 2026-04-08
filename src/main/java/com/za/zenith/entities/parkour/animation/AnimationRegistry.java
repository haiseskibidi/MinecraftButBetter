package com.za.zenith.entities.parkour.animation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class AnimationRegistry {
    private static final Map<String, List<AnimationProfile>> groups = new HashMap<>();
    private static final Random random = new Random();

    public static void register(String name, AnimationProfile animation) {
        // Find group (e.g., "walk_1" -> "walk")
        String groupName = name;
        if (name.contains("_")) {
            groupName = name.substring(0, name.lastIndexOf('_'));
        }
        
        groups.computeIfAbsent(groupName, k -> new ArrayList<>()).add(animation);
        // Also register with full name for direct access
        groups.computeIfAbsent(name, k -> new ArrayList<>()).add(animation);
    }

    public static AnimationProfile get(String groupName) {
        List<AnimationProfile> list = groups.get(groupName);
        if (list == null || list.isEmpty()) return null;
        if (list.size() == 1) return list.get(0);
        return list.get(random.nextInt(list.size()));
    }

    public static boolean exists(String groupName) {
        return groups.containsKey(groupName);
    }
}
