package com.za.zenith.utils;

/**
 * LiveReloadable - Interface for objects that need custom logic
 * when their properties are changed via the Live Inspector.
 */
public interface LiveReloadable {
    /**
     * Called immediately after a field is modified in the Inspector.
     * Use this to rebuild meshes, reset timers, or refresh dependent systems.
     */
    default void onLiveReload() {}
    
    /**
     * @return The relative path to the source JSON file (e.g. "zenith/blocks/dirt.json")
     */
    String getSourcePath();
    
    /**
     * Sets the source path (used by DataLoader).
     */
    void setSourcePath(String path);
}
