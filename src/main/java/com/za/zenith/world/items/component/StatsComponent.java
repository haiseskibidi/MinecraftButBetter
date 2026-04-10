package com.za.zenith.world.items.component;

import java.util.HashMap;
import java.util.Map;

/**
 * Component that stores RPG statistics for an item.
 */
public class StatsComponent implements ItemComponent {
    private final Map<StatType, Float> stats = new HashMap<>();

    public enum StatType {
        ATTACK_DAMAGE,
        ATTACK_SPEED,
        DEFENSE,
        CRIT_CHANCE,
        CRIT_DAMAGE,
        MOVEMENT_SPEED,
        STAMINA_REGEN,
        LIFESTEAL
    }

    public StatsComponent() {}

    public void set(StatType type, float value) {
        stats.put(type, value);
    }

    public float get(StatType type) {
        return stats.getOrDefault(type, 0.0f);
    }

    public Map<StatType, Float> getAllStats() {
        return stats;
    }
}
