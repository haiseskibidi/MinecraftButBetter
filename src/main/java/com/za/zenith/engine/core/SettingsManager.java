package com.za.zenith.engine.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * SettingsManager v1.0 - Centralized game settings and keybinds.
 * Phase 1 of Settings System implementation.
 */
public class SettingsManager {
    private static SettingsManager instance;
    private static final String OPTIONS_FILE = "options.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    // Settings fields
    private float fov = 70.0f;
    private float mouseSensitivity = 1.0f;
    private boolean devMode = false;
    private boolean debugOverlayVisible = false;
    private boolean vsync = true;
    private Map<String, Integer> keyBinds = new HashMap<>();

    private SettingsManager() {
        resetToDefaults();
    }

    public static SettingsManager getInstance() {
        if (instance == null) {
            instance = new SettingsManager();
        }
        return instance;
    }

    public void resetToDefaults() {
        fov = 70.0f;
        mouseSensitivity = 1.0f;
        devMode = false;
        vsync = true;
        
        keyBinds.clear();
        keyBinds.put("move_forward", GLFW.GLFW_KEY_W);
        keyBinds.put("move_back", GLFW.GLFW_KEY_S);
        keyBinds.put("move_left", GLFW.GLFW_KEY_A);
        keyBinds.put("move_right", GLFW.GLFW_KEY_D);
        keyBinds.put("jump", GLFW.GLFW_KEY_SPACE);
        keyBinds.put("sprint", GLFW.GLFW_KEY_LEFT_CONTROL);
        keyBinds.put("sneak", GLFW.GLFW_KEY_LEFT_SHIFT);
        keyBinds.put("inventory", GLFW.GLFW_KEY_E);
        keyBinds.put("drop", GLFW.GLFW_KEY_Q);
        keyBinds.put("pause", GLFW.GLFW_KEY_ESCAPE);
        keyBinds.put("journal", GLFW.GLFW_KEY_J);
        keyBinds.put("debug_menu", GLFW.GLFW_KEY_F3);
        keyBinds.put("editor_toggle", GLFW.GLFW_KEY_F8);
        keyBinds.put("live_inspector", GLFW.GLFW_KEY_F9);
    }

    public void load() {
        File file = new File(OPTIONS_FILE);
        if (!file.exists()) {
            save();
            return;
        }

        try (FileReader reader = new FileReader(file)) {
            SettingsManager loaded = GSON.fromJson(reader, SettingsManager.class);
            if (loaded != null) {
                this.fov = loaded.fov;
                this.mouseSensitivity = loaded.mouseSensitivity;
                this.devMode = loaded.devMode;
                this.vsync = loaded.vsync;
                if (loaded.keyBinds != null) {
                    this.keyBinds.putAll(loaded.keyBinds);
                }
            }
        } catch (IOException e) {
            System.err.println("Failed to load settings: " + e.getMessage());
        }
    }

    public void save() {
        try (FileWriter writer = new FileWriter(OPTIONS_FILE)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            System.err.println("Failed to save settings: " + e.getMessage());
        }
    }

    // Getters and Setters
    public float getFov() { return fov; }
    public void setFov(float fov) { this.fov = fov; }

    public float getMouseSensitivity() { return mouseSensitivity; }
    public void setMouseSensitivity(float mouseSensitivity) { this.mouseSensitivity = mouseSensitivity; }

    public boolean isDevMode() { return devMode; }
    public void setDevMode(boolean devMode) { this.devMode = devMode; }

    public boolean isDebugOverlayVisible() { return debugOverlayVisible; }
    public void setDebugOverlayVisible(boolean debugOverlayVisible) { this.debugOverlayVisible = debugOverlayVisible; }

    public boolean isVsync() { return vsync; }
    public void setVsync(boolean vsync) { this.vsync = vsync; }

    public int getKeyCode(String actionId) {
        return keyBinds.getOrDefault(actionId, GLFW.GLFW_KEY_UNKNOWN);
    }

    public void setKeyCode(String actionId, int keyCode) {
        keyBinds.put(actionId, keyCode);
    }
    
    public Map<String, Integer> getKeyBinds() {
        return keyBinds;
    }
}
