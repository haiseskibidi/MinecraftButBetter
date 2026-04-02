package com.za.minecraft.world.blocks;

/**
 * Settings for block breaking logic.
 * @param strategy "default" (continuous/discrete hits) or "weak_spots" (Rust-style)
 * @param precision Radius of the weak spot hitbox (relative to block size 0-1)
 * @param missMultiplier Damage multiplier applied when missing the weak spot
 */
public record MiningSettings(String strategy, float precision, float missMultiplier) {
    public static final MiningSettings DEFAULT = new MiningSettings("default", 0.2f, 1.0f);
}
