package com.za.zenith.engine.graphics.ui;

import com.google.gson.*;
import com.za.zenith.engine.core.GameLoop;
import com.za.zenith.engine.graphics.DynamicTextureAtlas;
import com.za.zenith.engine.resources.AssetManager;
import com.za.zenith.utils.Logger;
import org.lwjgl.glfw.GLFW;

import java.util.*;
import java.util.stream.Collectors;

/**
 * DevInspectorScreen v4.0 - Data-Driven JSON Editor.
 * No more Reflection hacks. Pure JSON manipulation.
 */
public class DevInspectorScreen implements Screen {
    private enum Tab { BLOCKS, ITEMS, CONFIGS, ACTIONS, OTHERS }

    private static Tab activeTab = Tab.BLOCKS;
    private static String selectedPath = null;
    private static JsonElement rootElement = null;
    private static final Set<String> expandedNodes = new HashSet<>(List.of(""));
    private static float sidebarScroll = 0;
    private static float propertiesScroll = 0;

    private final ScrollPanel sidebarScroller = new ScrollPanel();
    private final ScrollPanel propertiesScroller = new ScrollPanel();

    private String editingPath = null;
    private String editingValue = "";
    private JsonElement editingElement = null;
    
    private String searchFilter = "";

    @Override
    public void init(int width, int height) {
        sidebarScroller.setBounds(20, 100, 260, height - 180);
        propertiesScroller.setBounds(300, 100, width - 320, height - 180);
        sidebarScroller.setScrollY(sidebarScroll);
        propertiesScroller.setScrollY(propertiesScroll);
    }

    @Override
    public void render(UIRenderer renderer, int sw, int sh, DynamicTextureAtlas atlas) {
        renderer.setupUIProjection(sw, sh);
        renderer.getPrimitivesRenderer().renderDarkenedBackground();

        // Main Panel
        renderer.getPrimitivesRenderer().renderRect(10, 10, sw - 20, sh - 20, sw, sh, 0.02f, 0.02f, 0.02f, 0.98f);
        renderer.getPrimitivesRenderer().renderRect(10, 10, sw - 20, 40, sw, sh, 0.0f, 0.4f, 0.7f, 1.0f);
        renderer.getFontRenderer().drawString("ZENITH JSON INSPECTOR v4.0", 30, 20, 18, sw, sh);

        renderTabs(renderer, sw, sh);

        // Sidebar
        sidebarScroller.begin(sw, sh);
        renderSidebar(renderer, sw, sh);
        sidebarScroller.end();
        sidebarScroller.renderScrollbar(renderer, sw, sh);

        // Properties
        propertiesScroller.begin(sw, sh);
        renderProperties(renderer, sw, sh);
        propertiesScroller.end();
        propertiesScroller.renderScrollbar(renderer, sw, sh);

        renderFooter(renderer, sw, sh);
    }

    private void renderTabs(UIRenderer renderer, int sw, int sh) {
        int x = 20;
        for (Tab tab : Tab.values()) {
            boolean active = activeTab == tab;
            int tw = 100;
            if (active) renderer.getPrimitivesRenderer().renderRect(x, 60, tw, 30, sw, sh, 0.0f, 0.6f, 1.0f, 0.8f);
            else renderer.getPrimitivesRenderer().renderRect(x, 60, tw, 30, sw, sh, 0.1f, 0.1f, 0.1f, 0.8f);
            
            renderer.getFontRenderer().drawString(tab.name(), x + 15, 68, 14, sw, sh, 1, 1, 1, 1);
            
            if (GameLoop.getInstance().getInputManager().isMouseButtonPressed(0) && isHovered(x, 60, tw, 30)) {
                activeTab = tab;
            }
            x += tw + 10;
        }
    }

    private void renderSidebar(UIRenderer renderer, int sw, int sh) {
        int y = (int) (110 - sidebarScroller.getOffset());
        int h = 25;
        
        List<String> paths = AssetManager.getLoadedPaths().stream()
                .filter(this::matchesTab)
                .sorted()
                .collect(Collectors.toList());

        for (String path : paths) {
            boolean selected = path.equals(selectedPath);
            if (selected) renderer.getPrimitivesRenderer().renderRect(25, y - 2, 245, h, sw, sh, 0.0f, 0.6f, 1.0f, 0.3f);
            
            String label = path.substring(path.lastIndexOf('/') + 1);
            renderer.getFontRenderer().drawString(label, 35, y, 12, sw, sh, selected ? 0.0f : 0.8f, selected ? 0.8f : 0.8f, selected ? 1.0f : 0.8f, 1.0f);
            y += h;
        }
        sidebarScroller.updateContentHeight(y - (110 - sidebarScroller.getOffset()));
    }

    @Override
    public boolean handleMouseClick(float mx, float my, int button) {
        if (button != 0) return false;

        // Tabs
        int tx = 20;
        for (Tab tab : Tab.values()) {
            if (isHovered((int)mx, (int)my, tx, 60, 100, 30)) {
                activeTab = tab;
                return true;
            }
            tx += 110;
        }

        // Sidebar entries
        if (sidebarScroller.isMouseOver(mx, my)) {
            int y = (int) (110 - sidebarScroller.getOffset());
            List<String> paths = AssetManager.getLoadedPaths().stream()
                    .filter(this::matchesTab)
                    .sorted()
                    .collect(Collectors.toList());
            for (String path : paths) {
                if (isHovered((int)mx, (int)my, 25, y, 245, 25)) {
                    selectPath(path);
                    return true;
                }
                y += 25;
            }
        }

        // Properties (Recursive Json Tree)
        if (propertiesScroller.isMouseOver(mx, my)) {
            // Logic for tree expansion and value clicking is now handled in a stateful way
            // We'll trigger a flag that renderProperties will check, or just keep it simple:
            lastClickX = mx; lastClickY = my;
            pendingClick = true;
        }

        return false;
    }

    private float lastClickX, lastClickY;
    private boolean pendingClick = false;

    private boolean isHovered(int mx, int my, int x, int y, int w, int h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private boolean matchesTab(String path) {
        return switch (activeTab) {
            case BLOCKS -> path.contains("/blocks/");
            case ITEMS -> path.contains("/items/");
            case CONFIGS -> path.contains("/registry/");
            case ACTIONS -> path.contains("/actions/");
            case OTHERS -> !path.contains("/blocks/") && !path.contains("/items/") && !path.contains("/registry/") && !path.contains("/actions/");
        };
    }

    private void selectPath(String path) {
        if (path.equals(selectedPath)) return;
        selectedPath = path;
        String snapshot = AssetManager.getSnapshot(path);
        if (snapshot != null) {
            rootElement = AssetManager.getGson().fromJson(snapshot, JsonElement.class);
        }
    }

    private void renderProperties(UIRenderer renderer, int sw, int sh) {
        if (rootElement == null) {
            renderer.getFontRenderer().drawString("Select a resource to edit", 320, 150, 16, sw, sh, 0.4f, 0.4f, 0.4f, 1.0f);
            return;
        }

        int y = (int) (110 - propertiesScroller.getOffset());
        renderer.getFontRenderer().drawString(selectedPath, 320, y, 14, sw, sh, 0.0f, 0.8f, 1.0f, 1.0f);
        y += 30;

        y = renderJsonElement(renderer, rootElement, "root", "", 320, y, 0, sw, sh);
        
        propertiesScroller.updateContentHeight(y - (110 - propertiesScroller.getOffset()));
    }

    private int renderJsonElement(UIRenderer renderer, JsonElement el, String label, String path, int x, int y, int depth, int sw, int sh) {
        if (el.isJsonObject()) {
            return renderJsonObject(renderer, el.getAsJsonObject(), label, path, x, y, depth, sw, sh);
        } else if (el.isJsonArray()) {
            return renderJsonArray(renderer, el.getAsJsonArray(), label, path, x, y, depth, sw, sh);
        } else {
            return renderJsonValue(renderer, el.getAsJsonPrimitive(), label, path, x, y, sw, sh);
        }
    }

    private int renderJsonObject(UIRenderer renderer, JsonObject obj, String label, String path, int x, int y, int depth, int sw, int sh) {
        boolean expanded = expandedNodes.contains(path);
        String prefix = expanded ? "[-] " : "[+] ";
        renderer.getFontRenderer().drawString(prefix + label, x, y, 14, sw, sh, 0.9f, 0.9f, 0.0f, 1.0f);
        
        if (GameLoop.getInstance().getInputManager().isMouseButtonPressed(0) && isHovered(x, y, 200, 20)) {
            if (expanded) expandedNodes.remove(path);
            else expandedNodes.add(path);
        }
        
        y += 22;
        if (expanded) {
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                String childPath = path.isEmpty() ? entry.getKey() : path + "." + entry.getKey();
                y = renderJsonElement(renderer, entry.getValue(), entry.getKey(), childPath, x + 20, y, depth + 1, sw, sh);
            }
        }
        return y;
    }

    private int renderJsonArray(UIRenderer renderer, JsonArray arr, String label, String path, int x, int y, int depth, int sw, int sh) {
        boolean expanded = expandedNodes.contains(path);
        String prefix = expanded ? "[-] " : "[+] ";
        renderer.getFontRenderer().drawString(prefix + label + " [" + arr.size() + "]", x, y, 14, sw, sh, 0.0f, 0.8f, 0.5f, 1.0f);
        
        if (GameLoop.getInstance().getInputManager().isMouseButtonPressed(0) && isHovered(x, y, 200, 20)) {
            if (expanded) expandedNodes.remove(path);
            else expandedNodes.add(path);
        }
        
        y += 22;
        if (expanded) {
            for (int i = 0; i < arr.size(); i++) {
                String childPath = path + "[" + i + "]";
                y = renderJsonElement(renderer, arr.get(i), "Item " + i, childPath, x + 20, y, depth + 1, sw, sh);
            }
        }
        return y;
    }

    private int renderJsonValue(UIRenderer renderer, JsonPrimitive primitive, String label, String path, int x, int y, int sw, int sh) {
        String fullLabel = label + ": ";
        int lw = renderer.getFontRenderer().getStringWidth(fullLabel, 14);
        renderer.getFontRenderer().drawString(fullLabel, x, y, 14, sw, sh, 0.7f, 0.7f, 0.7f, 1.0f);
        
        boolean isEditing = path.equals(editingPath);
        String valStr = isEditing ? editingValue + "_" : primitive.getAsString();
        
        renderer.getFontRenderer().drawString(valStr, x + lw, y, 14, sw, sh, isEditing ? 0.0f : 1.0f, isEditing ? 1.0f : 1.0f, isEditing ? 0.0f : 1.0f, 1.0f);
        
        if (GameLoop.getInstance().getInputManager().isMouseButtonPressed(0) && isHovered(x + lw, y, 400, 20)) {
            editingPath = path;
            editingValue = primitive.getAsString();
            editingElement = primitive;
        }
        
        return y + 22;
    }

    private void applyEditing() {
        if (editingElement == null || selectedPath == null) return;
        
        try {
            // Update the element in our local Json tree
            updateJsonValue(rootElement, editingPath, editingValue);
            // Save to disk and reload
            AssetManager.saveAndReload(selectedPath, rootElement);
        } catch (Exception e) {
            Logger.error("Failed to apply JSON edit: " + e.getMessage());
        }
        
        editingPath = null;
        editingElement = null;
    }

    private void updateJsonValue(JsonElement root, String path, String value) {
        String[] parts = path.split("\\.");
        JsonElement current = root;
        
        for (int i = 0; i < parts.length - 1; i++) {
            current = getChild(current, parts[i]);
        }
        
        String lastPart = parts[parts.length - 1];
        setChildValue(current, lastPart, value);
    }

    private JsonElement getChild(JsonElement parent, String part) {
        if (part.contains("[")) {
            String name = part.substring(0, part.indexOf('['));
            int index = Integer.parseInt(part.substring(part.indexOf('[') + 1, part.indexOf(']')));
            return parent.getAsJsonObject().get(name).getAsJsonArray().get(index);
        }
        return parent.getAsJsonObject().get(part);
    }

    private void setChildValue(JsonElement parent, String part, String value) {
        JsonObject obj = parent.getAsJsonObject();
        JsonElement existing = obj.get(part);
        
        if (existing.isJsonPrimitive()) {
            JsonPrimitive p = existing.getAsJsonPrimitive();
            if (p.isBoolean()) obj.addProperty(part, Boolean.parseBoolean(value));
            else if (p.isNumber()) obj.addProperty(part, Double.parseDouble(value));
            else obj.addProperty(part, value);
        }
    }

    private void renderFooter(UIRenderer renderer, int sw, int sh) {
        renderer.getPrimitivesRenderer().renderRect(10, sh - 40, sw - 20, 30, sw, sh, 0.05f, 0.05f, 0.05f, 1.0f);
        String info = "ENTER: Save & Reload | ESC: Close | Click value to edit";
        renderer.getFontRenderer().drawString(info, 30, sh - 30, 12, sw, sh, 0.6f, 0.6f, 0.6f, 1.0f);
    }

    private boolean isHovered(int x, int y, int w, int h) {
        double mx = GameLoop.getInstance().getInputManager().getMouseX();
        double my = GameLoop.getInstance().getInputManager().getMouseY();
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    @Override
    public boolean handleKeyPress(int key) {
        if (editingPath != null) {
            if (key == GLFW.GLFW_KEY_BACKSPACE && !editingValue.isEmpty()) editingValue = editingValue.substring(0, editingValue.length() - 1);
            else if (key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER) applyEditing();
            else if (key == GLFW.GLFW_KEY_ESCAPE) editingPath = null;
            return true;
        }
        if (key == GLFW.GLFW_KEY_ESCAPE || key == GLFW.GLFW_KEY_F9) {
            ScreenManager.getInstance().closeScreen();
            GameLoop.getInstance().getInputManager().enableMouseCapture(GameLoop.getInstance().getWindow());
            return true;
        }
        return false;
    }

    @Override
    public void handleCharInput(char c) {
        if (editingPath != null) {
            editingValue += c;
        }
    }

    private int rendererWidth(String s) { return s.length() * 8; }
}
