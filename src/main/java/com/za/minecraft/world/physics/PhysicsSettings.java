package com.za.minecraft.world.physics;

/**
 * Global physics and player configuration loaded from JSON.
 */
public class PhysicsSettings {
    private static PhysicsSettings instance = new PhysicsSettings();

    // Player Dimensions
    public float playerWidth = 0.6f;
    public float standingHeight = 1.8f;
    public float sneakingHeight = 1.45f;
    public float standingEyeHeight = 1.62f;
    public float sneakingEyeHeight = 1.25f;

    // Movement Multipliers
    public float sprintMultiplier = 1.45f;
    public float sneakSpeedMultiplier = 0.3f;
    public float flySprintMultiplier = 1.8f;

    // Parkour
    public float maxGrabHeight = 2.45f;
    public float minGrabHeight = 1.25f;
    public float grabDistance = 0.7f;
    public float climbDuration = 0.45f;

    // Base Physics
    public float jumpVelocity = 8.0f;
    public float baseMoveSpeed = 3.8f;
    public float flySpeed = 9.0f;
    
    // Input
    public float mouseSensitivity = 0.002f;

    public static PhysicsSettings getInstance() {
        return instance;
    }

    public static void setInstance(PhysicsSettings newSettings) {
        instance = newSettings;
    }
}
