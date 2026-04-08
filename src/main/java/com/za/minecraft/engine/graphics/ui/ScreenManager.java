package com.za.minecraft.engine.graphics.ui;

import com.za.minecraft.engine.core.GameLoop;
import com.za.minecraft.entities.Player;

/**
 * Manages the current active GUI screen.
 */
public class ScreenManager {
    private static ScreenManager instance;
    private Screen activeScreen;

    private ScreenManager() {}

    public static ScreenManager getInstance() {
        if (instance == null) instance = new ScreenManager();
        return instance;
    }

    public void openPlayerInventory(Player player, int sw, int sh) {
        activeScreen = new PlayerInventoryScreen(player);
        activeScreen.init(sw, sh);
    }

    public void openChest(com.za.minecraft.world.inventory.IInventory container, com.za.minecraft.entities.Player player, int sw, int sh) {
        activeScreen = new ChestScreen(container, player);
        activeScreen.init(sw, sh);
    }

    public void openScreen(Screen screen, int sw, int sh) {
        this.activeScreen = screen;
        if (activeScreen != null) {
            activeScreen.init(sw, sh);
        }
    }

    public void closeScreen() {
        activeScreen = null;
    }

    public Screen getActiveScreen() {
        return activeScreen;
    }

    public void render(UIRenderer renderer, int sw, int sh, com.za.minecraft.engine.graphics.DynamicTextureAtlas atlas) {
        if (activeScreen != null) {
            activeScreen.render(renderer, sw, sh, atlas);
        }
    }

    public boolean isAnyScreenOpen() {
        return activeScreen != null;
    }
}
