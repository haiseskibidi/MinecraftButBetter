package com.za.zenith.world.blocks;

import org.joml.Vector3f;

/**
 * Settings for block breaking logic.
 * @param strategy "default" (continuous/discrete hits) or "weak_spots" (Rust-style)
 * @param precision Radius of the weak spot hitbox (relative to block size 0-1)
 * @param missMultiplier Damage multiplier applied when missing the weak spot
 * @param weakSpotColor Color of the fresh chip/spark (default is wood-like yellowish)
 */
public record MiningSettings(String strategy, float precision, float missMultiplier, Vector3f weakSpotColor) {
    public static final MiningSettings DEFAULT = new MiningSettings("default", 0.2f, 1.0f, new Vector3f(1.0f, 1.0f, 1.0f));
}
