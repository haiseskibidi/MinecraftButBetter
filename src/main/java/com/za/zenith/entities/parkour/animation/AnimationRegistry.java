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
        
        List<AnimationProfile> group = groups.computeIfAbsent(groupName, k -> new ArrayList<>());
        // Replace if exists with same name, otherwise add
        boolean found = false;
        for (int i = 0; i < group.size(); i++) {
            if (group.get(i).getName().equals(name)) {
                group.set(i, animation);
                found = true;
                break;
            }
        }
        if (!found) group.add(animation);

        // Also register full name as a separate group for direct access
        List<AnimationProfile> directGroup = groups.computeIfAbsent(name, k -> new ArrayList<>());
        directGroup.clear(); // For direct name, we always want the latest one
        directGroup.add(animation);
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

    public static java.util.Set<String> getKeys() {
        return groups.keySet();
    }
}


