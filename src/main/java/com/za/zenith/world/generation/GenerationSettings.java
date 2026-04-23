package com.za.zenith.world.generation;

import com.za.zenith.utils.LiveReloadable;
import com.za.zenith.utils.Logger;

public class GenerationSettings implements LiveReloadable {
    private static GenerationSettings instance = new GenerationSettings();
    private transient String sourcePath;

    @Override
    public String getSourcePath() { return sourcePath; }

    @Override
    public void setSourcePath(String path) { this.sourcePath = path; }

    @Override
    public void onLiveReload() {
        Logger.info("GenerationSettings: Applied live changes");
    }

    public int initialRenderDistance = 3;
    public int activeRenderDistance = 12;
    public int unloadDistance = 14;

    public static GenerationSettings getInstance() {
        return instance;
    }

    public static void setInstance(GenerationSettings newInstance) {
        instance = newInstance;
    }
}
