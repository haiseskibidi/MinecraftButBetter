package com.za.minecraft.world.actions;

public record ActionDefinition(
        float staminaCostPerSecond,
        float staminaCostPerUse,
        float noiseLevel,
        float hungerCostPerSecond,
        float hungerCostPerUse
) {
    public ActionDefinition() {
        this(0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
    }
}
