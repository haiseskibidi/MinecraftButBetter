package com.za.zenith.engine.graphics.ui;

import com.za.zenith.engine.core.GameLoop;
import com.za.zenith.entities.Player;

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

    public void openChest(com.za.zenith.world.inventory.IInventory container, com.za.zenith.entities.Player player, com.za.zenith.utils.Identifier guiId, int sw, int sh) {
        activeScreen = new ChestScreen(container, player, guiId);
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

    private int lastSw = -1, lastSh = -1;

    public void render(UIRenderer renderer, int sw, int sh, com.za.zenith.engine.graphics.DynamicTextureAtlas atlas) {
        if (activeScreen != null) {
            // Auto-reinit on resize
            if (sw != lastSw || sh != lastSh) {
                activeScreen.init(sw, sh);
                lastSw = sw;
                lastSh = sh;
            }
            activeScreen.render(renderer, sw, sh, atlas);
        }
    }

    public boolean isAnyScreenOpen() {
        return activeScreen != null;
    }
}


