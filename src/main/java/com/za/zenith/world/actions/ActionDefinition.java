package com.za.zenith.world.actions;

public class ActionDefinition implements com.za.zenith.utils.LiveReloadable {
    private transient String sourcePath;

    @Override
    public String getSourcePath() { return sourcePath; }

    @Override
    public void setSourcePath(String path) { this.sourcePath = path; }

    public float staminaCostPerSecond;
    public float staminaCostPerUse;
    public float noiseLevel;
    public float hungerCostPerSecond;
    public float hungerCostPerUse;

    public ActionDefinition() {
        this(0.0f, 0.0f, 0.0f, 0.0f, 0.0f);
    }

    public ActionDefinition(float staminaCostPerSecond, float staminaCostPerUse, float noiseLevel, float hungerCostPerSecond, float hungerCostPerUse) {
        this.staminaCostPerSecond = staminaCostPerSecond;
        this.staminaCostPerUse = staminaCostPerUse;
        this.noiseLevel = noiseLevel;
        this.hungerCostPerSecond = hungerCostPerSecond;
        this.hungerCostPerUse = hungerCostPerUse;
    }
}
