package com.za.zenith.world.items.stats;

import com.za.zenith.utils.Identifier;
import java.util.*;

/**
 * Calculates and manages a set of stats for an entity or item.
 */
public class StatContainer {
    private final Map<Identifier, Float> baseValues = new HashMap<>();
    private final Map<Identifier, List<StatModifier>> modifiers = new HashMap<>();
    private final Map<Identifier, Float> finalValuesCache = new HashMap<>();
    private boolean dirty = true;

    public void setBase(Identifier id, float value) {
        baseValues.put(id, value);
        markDirty();
    }

    public float getBase(Identifier id) {
        if (baseValues.containsKey(id)) return baseValues.get(id);
        StatDefinition def = StatRegistry.get(id);
        return def != null ? def.defaultValue() : 0.0f;
    }

    public void addModifier(Identifier statId, StatModifier modifier) {
        modifiers.computeIfAbsent(statId, k -> new ArrayList<>()).add(modifier);
        markDirty();
    }

    public void removeModifiersFrom(Identifier sourceId) {
        for (List<StatModifier> mods : modifiers.values()) {
            mods.removeIf(m -> m.source().equals(sourceId));
        }
        markDirty();
    }

    public void clearModifiers() {
        modifiers.clear();
        markDirty();
    }

    public void markDirty() {
        dirty = true;
        finalValuesCache.clear();
    }

    public float get(Identifier id) {
        if (!dirty && finalValuesCache.containsKey(id)) {
            return finalValuesCache.get(id);
        }

        float value = calculateFinal(id);
        finalValuesCache.put(id, value);
        return value;
    }

    private float calculateFinal(Identifier id) {
        float base = getBase(id);
        List<StatModifier> mods = modifiers.get(id);
        if (mods == null || mods.isEmpty()) return base;

        float sumAdd = 0;
        float sumMultBase = 0;
        float productMultTotal = 1.0f;

        for (StatModifier mod : mods) {
            switch (mod.operation()) {
                case ADD -> sumAdd += mod.value();
                case MULTIPLY_BASE -> sumMultBase += mod.value();
                case MULTIPLY_TOTAL -> productMultTotal *= (1.0f + mod.value());
            }
        }

        float result = (base + sumAdd + base * sumMultBase) * productMultTotal;
        
        StatDefinition def = StatRegistry.get(id);
        if (def != null) {
            result = Math.clamp(result, def.minValue(), def.maxValue());
        }
        
        return result;
    }

    public Map<Identifier, Float> getAllStats() {
        if (dirty) {
            // Re-calculate everything if dirty
            for (Identifier id : baseValues.keySet()) get(id);
            for (Identifier id : modifiers.keySet()) get(id);
            dirty = false;
        }
        return Collections.unmodifiableMap(finalValuesCache);
    }
}
