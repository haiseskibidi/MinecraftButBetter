package com.za.zenith.engine.graphics.ui;

import com.za.zenith.engine.core.GameLoop;
import com.za.zenith.engine.core.SettingsManager;
import com.za.zenith.engine.graphics.DynamicTextureAtlas;
import com.za.zenith.utils.I18n;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * SettingsScreen - Survivor's Tablet style settings menu.
 * Supports tabs, keybinds, and modular data-driven settings.
 */
public class SettingsScreen implements Screen {
    private enum Tab { CONTROLS, VIDEO, DEVELOPER }
    private Tab activeTab = Tab.CONTROLS;
    private String waitingActionId = null;
    private final ScrollPanel contentScroller = new ScrollPanel();
    
    // State for typing in numeric settings
    private String editingSettingId = null;
    private String editingSettingValue = "";
    
    private int sw, sh; // Cache for back action

    @Override
    public void init(int width, int height) {
        this.sw = width;
        this.sh = height;
    }

    @Override
    public void render(UIRenderer renderer, int sw, int sh, DynamicTextureAtlas atlas) {
        this.sw = sw;
        this.sh = sh;
        
        renderer.setupUIProjection(sw, sh);
        renderer.getPrimitivesRenderer().renderDarkenedBackground();
        
        int winW = 500;
        int winH = 420;
        int guiLeft = (sw - winW) / 2;
        int guiTop = (sh - winH) / 2;
        
        // Dynamically update bounds on render to prevent scale shifting
        contentScroller.setBounds(sw / 2 - 230, sh / 2 - 120, 460, 280);
        
        // Window Background
        renderer.getPrimitivesRenderer().renderRect(guiLeft - 2, guiTop - 2, winW + 4, winH + 4, sw, sh, 0.2f, 0.2f, 0.2f, 1.0f);
        renderer.getPrimitivesRenderer().renderRect(guiLeft, guiTop, winW, winH, sw, sh, 0.05f, 0.05f, 0.05f, 0.98f);
        renderer.getPrimitivesRenderer().renderRect(guiLeft, guiTop, winW, 3, sw, sh, 0.0f, 0.7f, 1.0f, 1.0f); // Accent

        // Title
        String title = I18n.get("menu.settings");
        int titleW = renderer.getFontRenderer().getStringWidth(title, 18);
        renderer.getFontRenderer().drawString(title, sw / 2 - titleW / 2, guiTop + 15, 18, sw, sh);

        // Tabs
        renderTabs(renderer, sw, sh, guiTop + 50);

        // Content
        contentScroller.begin(sw, sh);
        try {
            int x = guiLeft + 20;
            int startY = guiTop + 90;
            int y = (int) (startY - contentScroller.getOffset());
            int entryHeight = 30;

            SettingsManager sm = SettingsManager.getInstance();

            switch (activeTab) {
                case CONTROLS -> renderControls(renderer, x, y, entryHeight, sw, sh);
                case VIDEO -> renderVideo(renderer, x, y, entryHeight, sw, sh, sm);
                case DEVELOPER -> renderDeveloper(renderer, x, y, entryHeight, sw, sh, sm);
            }
        } finally {
            contentScroller.end();
        }
        
        contentScroller.renderScrollbar(renderer, sw, sh);

        // Back Button
        renderBackButton(renderer, sw, sh, guiTop + winH - 40);
    }

    private void renderTabs(UIRenderer renderer, int sw, int sh, int tabY) {
        int tabWidth = 120;
        int startX = sw / 2 - (tabWidth * 3) / 2;
        
        for (Tab t : Tab.values()) {
            String label = switch (t) {
                case CONTROLS -> I18n.get("settings.tab.controls");
                case VIDEO -> I18n.get("settings.tab.video");
                case DEVELOPER -> I18n.get("settings.tab.developer");
            };
            
            boolean active = activeTab == t;
            int x = startX + t.ordinal() * tabWidth;
            
            if (active) {
                renderer.getPrimitivesRenderer().renderRect(x + 5, tabY, tabWidth - 10, 25, sw, sh, 0.1f, 0.3f, 0.5f, 0.5f);
                renderer.getPrimitivesRenderer().renderRect(x + 5, tabY + 23, tabWidth - 10, 2, sw, sh, 0.0f, 0.6f, 1.0f, 1.0f);
            }
            
            int labelW = renderer.getFontRenderer().getStringWidth(label, 14);
            renderer.getFontRenderer().drawString(label, x + (tabWidth - labelW) / 2, tabY + 5, 14, sw, sh, active ? 1.0f : 0.6f, active ? 1.0f : 0.6f, active ? 1.0f : 0.6f, 1.0f);
        }
    }

    private void renderControls(UIRenderer renderer, int x, int y, int h, int sw, int sh) {
        SettingsManager sm = SettingsManager.getInstance();
        int currentY = y;
        
        List<String> actions = new ArrayList<>(sm.getKeyBinds().keySet());
        actions.sort(String::compareTo);

        for (String actionId : actions) {
            String label = I18n.get("settings.action." + actionId);
            int keyCode = sm.getKeyCode(actionId);
            String keyName = waitingActionId != null && waitingActionId.equals(actionId) 
                ? I18n.get("settings.waiting") 
                : GLFW.glfwGetKeyName(keyCode, 0);
            
            if (keyName == null) {
                if (keyCode == GLFW.GLFW_KEY_SPACE) keyName = "SPACE";
                else if (keyCode == GLFW.GLFW_KEY_ESCAPE) keyName = "ESC";
                else if (keyCode == GLFW.GLFW_KEY_LEFT_SHIFT) keyName = "LSHIFT";
                else if (keyCode == GLFW.GLFW_KEY_LEFT_CONTROL) keyName = "LCTRL";
                else if (keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL) keyName = "RCTRL";
                else if (keyCode == GLFW.GLFW_KEY_LEFT_ALT) keyName = "LALT";
                else if (keyCode >= GLFW.GLFW_KEY_F1 && keyCode <= GLFW.GLFW_KEY_F12) keyName = "F" + (keyCode - GLFW.GLFW_KEY_F1 + 1);
                else if (keyCode == -1) keyName = "NONE";
                else keyName = "KEY " + keyCode;
            }

            renderer.getFontRenderer().drawString(label, x + 10, currentY + 8, 14, sw, sh);
            
            int btnWidth = 150;
            int btnX = x + 300;
            boolean hovered = isMouseOver(getMx(), getMy(), btnX, currentY, btnWidth, 24);
            
            renderer.getPrimitivesRenderer().renderRect(btnX, currentY, btnWidth, 24, sw, sh, 0.15f, 0.15f, 0.15f, 0.9f);
            if (hovered || (waitingActionId != null && waitingActionId.equals(actionId))) {
                renderer.getPrimitivesRenderer().renderRect(btnX, currentY, btnWidth, 24, sw, sh, 0.0f, 0.6f, 1.0f, 0.4f);
            }
            
            String displayKey = keyName.toUpperCase();
            int kw = renderer.getFontRenderer().getStringWidth(displayKey, 14);
            renderer.getFontRenderer().drawString(displayKey, btnX + (btnWidth - kw)/2, currentY + 5, 14, sw, sh);
            
            currentY += h + 4;
        }
        
        contentScroller.updateContentHeight(currentY - y);
    }

    private void renderVideo(UIRenderer renderer, int x, int y, int h, int sw, int sh, SettingsManager sm) {
        renderNumericSetting(renderer, "fov", I18n.get("settings.fov"), sm.getFov(), false, x, y, sw, sh);
        renderBooleanSetting(renderer, "vsync", I18n.get("settings.vsync"), sm.isVsync(), x, y + h + 5, sw, sh);
        renderNumericSetting(renderer, "sensitivity", I18n.get("settings.sensitivity"), sm.getMouseSensitivity(), true, x, y + (h + 5) * 2, sw, sh);
        
        contentScroller.updateContentHeight((h + 5) * 3);
    }

    private void renderDeveloper(UIRenderer renderer, int x, int y, int h, int sw, int sh, SettingsManager sm) {
        renderBooleanSetting(renderer, "dev_mode", I18n.get("settings.dev_mode"), sm.isDevMode(), x, y, sw, sh);
        contentScroller.updateContentHeight(h + 5);
    }

    // --- MODULAR SETTING RENDERERS ---

    private void renderNumericSetting(UIRenderer renderer, String id, String label, float value, boolean isFloat, int x, int y, int sw, int sh) {
        int btnWidth = 460;
        renderer.getPrimitivesRenderer().renderRect(x, y, btnWidth, 26, sw, sh, 0.1f, 0.1f, 0.1f, 0.5f);
        renderer.getFontRenderer().drawString(label, x + 20, y + 6, 14, sw, sh);

        int rightX = x + btnWidth - 140;
        
        // [-] button
        boolean hoverMinus = isMouseOver(getMx(), getMy(), rightX, y + 2, 22, 22);
        renderer.getPrimitivesRenderer().renderRect(rightX, y + 2, 22, 22, sw, sh, 0.2f, 0.2f, 0.2f, 1.0f);
        if (hoverMinus) renderer.getPrimitivesRenderer().renderRect(rightX, y + 2, 22, 22, sw, sh, 0.0f, 0.6f, 1.0f, 0.4f);
        renderer.getFontRenderer().drawString("-", rightX + 8, y + 5, 14, sw, sh);
        
        // Value Box
        int boxX = rightX + 26;
        boolean isEditing = id.equals(editingSettingId);
        boolean hoverBox = isMouseOver(getMx(), getMy(), boxX, y + 2, 60, 22);
        renderer.getPrimitivesRenderer().renderRect(boxX, y + 2, 60, 22, sw, sh, 0.15f, 0.15f, 0.15f, 1.0f);
        
        if (isEditing) {
             renderer.getPrimitivesRenderer().renderRect(boxX, y + 2, 60, 22, sw, sh, 0.0f, 0.4f, 0.8f, 0.5f);
             String text = editingSettingValue + "_";
             int tw = renderer.getFontRenderer().getStringWidth(text, 14);
             renderer.getFontRenderer().drawString(text, boxX + 30 - tw/2, y + 5, 14, sw, sh);
        } else {
             if (hoverBox) renderer.getPrimitivesRenderer().renderRect(boxX, y + 2, 60, 22, sw, sh, 1.0f, 1.0f, 1.0f, 0.1f);
             String text = isFloat ? String.format(java.util.Locale.US, "%.2f", value) : String.valueOf((int)value);
             int tw = renderer.getFontRenderer().getStringWidth(text, 14);
             renderer.getFontRenderer().drawString(text, boxX + 30 - tw/2, y + 5, 14, sw, sh);
        }
        
        // [+] button
        int plusX = boxX + 64;
        boolean hoverPlus = isMouseOver(getMx(), getMy(), plusX, y + 2, 22, 22);
        renderer.getPrimitivesRenderer().renderRect(plusX, y + 2, 22, 22, sw, sh, 0.2f, 0.2f, 0.2f, 1.0f);
        if (hoverPlus) renderer.getPrimitivesRenderer().renderRect(plusX, y + 2, 22, 22, sw, sh, 0.0f, 0.6f, 1.0f, 0.4f);
        renderer.getFontRenderer().drawString("+", plusX + 7, y + 5, 14, sw, sh);
    }
    
    private void renderBooleanSetting(UIRenderer renderer, String id, String label, boolean value, int x, int y, int sw, int sh) {
        int btnWidth = 460;
        renderer.getPrimitivesRenderer().renderRect(x, y, btnWidth, 26, sw, sh, 0.1f, 0.1f, 0.1f, 0.5f);
        renderer.getFontRenderer().drawString(label, x + 20, y + 6, 14, sw, sh);

        int rightX = x + btnWidth - 80;
        boolean hover = isMouseOver(getMx(), getMy(), rightX, y + 2, 70, 22);
        renderer.getPrimitivesRenderer().renderRect(rightX, y + 2, 70, 22, sw, sh, value ? 0.0f : 0.2f, value ? 0.6f : 0.2f, value ? 1.0f : 0.2f, 1.0f);
        if (hover) renderer.getPrimitivesRenderer().renderRect(rightX, y + 2, 70, 22, sw, sh, 1.0f, 1.0f, 1.0f, 0.2f);
        
        String text = value ? I18n.get("gui.yes") : I18n.get("gui.no");
        int tw = renderer.getFontRenderer().getStringWidth(text, 14);
        renderer.getFontRenderer().drawString(text, rightX + 35 - tw/2, y + 5, 14, sw, sh, 1.0f, 1.0f, 1.0f, 1.0f);
    }

    private void renderBackButton(UIRenderer renderer, int sw, int sh, int y) {
        String text = I18n.get("menu.back");
        int bw = 120;
        int bh = 28;
        int bx = sw / 2 - bw / 2;
        boolean hovered = isMouseOver(getMx(), getMy(), bx, y, bw, bh);
        
        renderer.getPrimitivesRenderer().renderRect(bx, y, bw, bh, sw, sh, 0.15f, 0.15f, 0.15f, 0.9f);
        if (hovered) renderer.getPrimitivesRenderer().renderRect(bx, y, bw, bh, sw, sh, 0.0f, 0.6f, 1.0f, 0.5f);
        
        int tw = renderer.getFontRenderer().getStringWidth(text, 14);
        renderer.getFontRenderer().drawString(text, sw / 2 - tw / 2, y + 7, 14, sw, sh);
    }

    // --- HELPERS & LOGIC ---

    private float getMx() { return GameLoop.getInstance().getInputManager().getCurrentMousePos().x; }
    private float getMy() { return GameLoop.getInstance().getInputManager().getCurrentMousePos().y; }

    private boolean isMouseOver(float mx, float my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private void handleAction(String action) {
        switch (action) {
            case "back" -> {
                applyEditingValue();
                SettingsManager.getInstance().save();
                ScreenManager.getInstance().openScreen(new PauseScreen(), sw, sh);
            }
        }
    }

    private void applyEditingValue() {
        if (editingSettingId == null) return;
        try {
            float val = Float.parseFloat(editingSettingValue.isEmpty() ? "0" : editingSettingValue);
            SettingsManager sm = SettingsManager.getInstance();
            if (editingSettingId.equals("fov")) {
                if (val < 30) val = 30;
                if (val > 110) val = 110;
                sm.setFov(val);
            } else if (editingSettingId.equals("sensitivity")) {
                if (val < 0.01f) val = 0.01f;
                if (val > 5.0f) val = 5.0f;
                sm.setMouseSensitivity(val);
            }
        } catch (NumberFormatException e) {
            // Ignore invalid input gracefully
        }
        editingSettingId = null;
    }

    @Override
    public boolean handleMouseClick(float mx, float my, int button) {
        applyEditingValue(); // Commit any pending edits
        return true; // Block click-through to game
    }

    @Override
    public boolean handleMouseRelease(float mx, float my, int button) {
        int curSw = GameLoop.getInstance().getWindow().getWidth();
        int curSh = GameLoop.getInstance().getWindow().getHeight();
        
        int winW = 500;
        int winH = 420;
        int guiLeft = (curSw - winW) / 2;
        int guiTop = (curSh - winH) / 2;

        // Tab switching
        int tabY = guiTop + 50;
        int tabWidth = 120;
        int startX = curSw / 2 - (tabWidth * 3) / 2;
        for (Tab t : Tab.values()) {
            int x = startX + t.ordinal() * tabWidth;
            if (isMouseOver(mx, my, x + 5, tabY, tabWidth - 10, 25)) {
                activeTab = t;
                contentScroller.reset();
                return true;
            }
        }

        int startY_content = guiTop + 90;
        SettingsManager sm = SettingsManager.getInstance();

        if (activeTab == Tab.CONTROLS) {
            int x = guiLeft + 20;
            int y = (int) (startY_content - contentScroller.getOffset());
            int h = 30;
            int currentY = y;
            List<String> actions = new ArrayList<>(sm.getKeyBinds().keySet());
            actions.sort(String::compareTo);
            for (String actionId : actions) {
                if (isMouseOver(mx, my, x + 300, currentY, 150, 24)) {
                    waitingActionId = actionId;
                    return true;
                }
                currentY += h + 4;
            }
        } else if (activeTab == Tab.VIDEO) {
            int x = guiLeft + 20;
            int y = (int) (startY_content - contentScroller.getOffset());
            int h = 30;
            
            if (handleNumericClick("fov", sm.getFov(), 30, 110, 5, sm::setFov, x, y)) return true;
            if (handleBooleanClick("vsync", sm.isVsync(), sm::setVsync, x, y + h + 5)) return true;
            if (handleNumericClick("sensitivity", sm.getMouseSensitivity(), 0.01f, 5.0f, 0.1f, sm::setMouseSensitivity, x, y + (h + 5) * 2)) return true;
            
        } else if (activeTab == Tab.DEVELOPER) {
            int x = guiLeft + 20;
            int y = (int) (startY_content - contentScroller.getOffset());
            if (handleBooleanClick("dev_mode", sm.isDevMode(), sm::setDevMode, x, y)) return true;
        }
        
        // Back Button
        if (isMouseOver(mx, my, curSw / 2 - 60, guiTop + winH - 40, 120, 28)) {
            handleAction("back");
            return true;
        }
        
        return true; 
    }
    
    private boolean handleNumericClick(String id, float currentVal, float min, float max, float step, java.util.function.Consumer<Float> setter, int x, int y) {
        float mx = getMx(); float my = getMy();
        int btnWidth = 460;
        int rightX = x + btnWidth - 140;
        int boxX = rightX + 26;
        int plusX = boxX + 64;

        if (isMouseOver(mx, my, rightX, y + 2, 22, 22)) { // Minus
            float newVal = currentVal - step;
            if (newVal < min) newVal = min;
            setter.accept(newVal);
            return true;
        }
        if (isMouseOver(mx, my, boxX, y + 2, 60, 22)) { // Box (Typing)
            editingSettingId = id;
            editingSettingValue = "";
            return true;
        }
        if (isMouseOver(mx, my, plusX, y + 2, 22, 22)) { // Plus
            float newVal = currentVal + step;
            if (newVal > max) newVal = max;
            setter.accept(newVal);
            return true;
        }
        return false;
    }

    private boolean handleBooleanClick(String id, boolean currentVal, java.util.function.Consumer<Boolean> setter, int x, int y) {
        float mx = getMx(); float my = getMy();
        int btnWidth = 460;
        int rightX = x + btnWidth - 80;
        if (isMouseOver(mx, my, rightX, y + 2, 70, 22)) {
            setter.accept(!currentVal);
            return true;
        }
        return false;
    }

    @Override
    public boolean handleChar(int codepoint) {
        if (editingSettingId != null) {
            char c = (char) codepoint;
            if (Character.isDigit(c) || c == '.') {
                editingSettingValue += c;
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean handleKeyPress(int key) {
        if (editingSettingId != null) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !editingSettingValue.isEmpty()) {
                editingSettingValue = editingSettingValue.substring(0, editingSettingValue.length() - 1);
            } else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER || key == GLFW.GLFW_KEY_ESCAPE) {
                applyEditingValue();
            }
            return true;
        }
        
        if (waitingActionId != null) {
            if (key != GLFW.GLFW_KEY_ESCAPE) {
                SettingsManager.getInstance().setKeyCode(waitingActionId, key);
            }
            waitingActionId = null;
            return true;
        }
        
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            handleAction("back");
            return true;
        }
        
        return false;
    }

    @Override
    public boolean handleScroll(double yoffset) {
        contentScroller.handleScroll(yoffset);
        return true;
    }
}