package com.za.minecraft.engine.graphics.ui;

public class PauseMenu {
    private boolean visible = false;
    private boolean canPause = true;
    
    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 40;
    private static final int BUTTON_SPACING = 50;
    
    public enum ButtonType {
        RESUME,
        EXIT_GAME
    }
    
    public void show() {
        if (canPause) {
            visible = true;
        }
    }
    
    public void hide() {
        visible = false;
    }
    
    public void toggle() {
        if (canPause) {
            visible = !visible;
        }
    }
    
    public boolean isVisible() {
        return visible;
    }
    
    public void setCanPause(boolean canPause) {
        this.canPause = canPause;
        if (!canPause) {
            visible = false;
        }
    }
    
    public boolean canPause() {
        return canPause;
    }
    
    public ButtonType getHoveredButton(int mouseX, int mouseY, int screenWidth, int screenHeight) {
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        
        int resumeY = centerY - BUTTON_SPACING;
        int exitY = centerY + BUTTON_SPACING;
        
        if (mouseX >= centerX - BUTTON_WIDTH / 2 && mouseX <= centerX + BUTTON_WIDTH / 2) {
            if (mouseY >= resumeY - BUTTON_HEIGHT / 2 && mouseY <= resumeY + BUTTON_HEIGHT / 2) {
                return ButtonType.RESUME;
            }
            if (mouseY >= exitY - BUTTON_HEIGHT / 2 && mouseY <= exitY + BUTTON_HEIGHT / 2) {
                return ButtonType.EXIT_GAME;
            }
        }
        
        return null;
    }
    
    public int getButtonWidth() {
        return BUTTON_WIDTH;
    }
    
    public int getButtonHeight() {
        return BUTTON_HEIGHT;
    }
    
    public int getButtonSpacing() {
        return BUTTON_SPACING;
    }
}
