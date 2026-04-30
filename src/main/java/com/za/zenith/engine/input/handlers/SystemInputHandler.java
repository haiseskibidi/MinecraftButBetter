package com.za.zenith.engine.input.handlers;

import com.za.zenith.engine.core.GameLoop;
import com.za.zenith.engine.core.PlayerMode;
import com.za.zenith.engine.core.SettingsManager;
import com.za.zenith.engine.core.Window;
import com.za.zenith.entities.Player;
import com.za.zenith.world.items.ItemStack;

public class SystemInputHandler {
    private boolean fKeyPressed = false;
    private boolean gKeyPressed = false;
    private boolean rKeyPressed = false;
    private boolean zKeyPressed = false;
    private boolean f3KeyPressed = false;
    private boolean f9KeyPressed = false;
    private boolean qKeyPressed = false;
    
    private boolean verticalMode = false;
    
    public void update(Window window, Player player, com.za.zenith.engine.input.InputManager manager) {
        boolean anyScreen = com.za.zenith.engine.graphics.ui.ScreenManager.getInstance().isAnyScreenOpen();
        boolean nappingOpen = GameLoop.getInstance().isNappingOpen();
        boolean inventoryOpen = GameLoop.getInstance().isInventoryOpen();
        boolean paused = GameLoop.getInstance().isPaused();
        
        // F - Toggle Fly
        boolean fKeyCurrentlyPressed = manager.isActionPressed("toggle_fly");
        if (fKeyCurrentlyPressed && !fKeyPressed && !anyScreen && !nappingOpen) {
            player.setFlying(!player.isFlying());
        }
        fKeyPressed = fKeyCurrentlyPressed;

        // F3 - Debug Menu / Developer Mode
        boolean f3KeyCurrentlyPressed = manager.isActionPressed("debug_menu");
        if (f3KeyCurrentlyPressed && !f3KeyPressed && !anyScreen && !nappingOpen) {
            boolean visible = !SettingsManager.getInstance().isDebugOverlayVisible();
            SettingsManager.getInstance().setDebugOverlayVisible(visible);
            
            PlayerMode newMode = visible ? PlayerMode.DEVELOPER : PlayerMode.SURVIVAL;
            player.setMode(newMode);
            com.za.zenith.utils.Logger.info("Debug HUD: %b, Player mode: %s", visible, newMode);
        }
        f3KeyPressed = f3KeyCurrentlyPressed;

        // F9 - Live Inspector
        boolean f9KeyCurrentlyPressed = manager.isActionPressed("live_inspector");
        if (f9KeyCurrentlyPressed && !f9KeyPressed && !anyScreen && !nappingOpen) {
            com.za.zenith.engine.graphics.ui.ScreenManager.getInstance().openScreen(
                new com.za.zenith.engine.graphics.ui.DevInspectorScreen(), window.getWidth(), window.getHeight());
            manager.disableMouseCapture(window);
        }
        f9KeyPressed = f9KeyCurrentlyPressed;
        
        // R - Vertical Mode (for slabs)
        boolean rKeyCurrentlyPressed = manager.isActionPressed("toggle_vertical_mode");
        if (rKeyCurrentlyPressed && !rKeyPressed && !anyScreen && !nappingOpen) {
            verticalMode = !verticalMode;
        }
        rKeyPressed = rKeyCurrentlyPressed;
        
        // G - Toggle FXAA
        boolean gKeyCurrentlyPressed = manager.isActionPressed("toggle_fxaa");
        if (gKeyCurrentlyPressed && !gKeyPressed && !anyScreen && !nappingOpen) {
            GameLoop.getInstance().getRenderer().toggleFXAA();
        }
        gKeyPressed = gKeyCurrentlyPressed;

        // Z - Sort Inventory
        boolean zKeyCurrentlyPressed = manager.isActionPressed("sort_inventory");
        if (zKeyCurrentlyPressed && !zKeyPressed && inventoryOpen) {
            player.getInventory().sortMainInventory();
        }
        zKeyPressed = zKeyCurrentlyPressed;

        // Q - Drop Item
        boolean qKeyCurrentlyPressed = manager.isActionPressed("drop");
        if (qKeyCurrentlyPressed && !qKeyPressed && !paused && !nappingOpen) {
            if (inventoryOpen) {
                if (manager.getHoveredSlot() != null) {
                    ItemStack stack = manager.getHoveredSlot().getStack();
                    if (stack != null) {
                        boolean ctrlPressed = window.isKeyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL) || window.isKeyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL);
                        manager.dropStack(stack, player, GameLoop.getInstance().getWorld(), GameLoop.getInstance().getCamera(), ctrlPressed);
                        if (stack.getCount() <= 0) manager.getHoveredSlot().setStack(null);
                    }
                }
            } else {
                ItemStack stack = player.getInventory().getSelectedItemStack();
                if (stack != null) {
                    boolean ctrlPressed = window.isKeyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_LEFT_CONTROL) || window.isKeyPressed(org.lwjgl.glfw.GLFW.GLFW_KEY_RIGHT_CONTROL);
                    manager.dropStack(stack, player, GameLoop.getInstance().getWorld(), GameLoop.getInstance().getCamera(), ctrlPressed);
                    if (stack.getCount() <= 0) player.getInventory().setStackInSlot(player.getInventory().getSelectedSlot(), null);
                }
            }
        }
        qKeyPressed = qKeyCurrentlyPressed;
    }
    
    public boolean isVerticalMode() {
        return verticalMode;
    }
}