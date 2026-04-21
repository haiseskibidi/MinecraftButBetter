package com.za.zenith.engine.graphics.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.za.zenith.engine.core.GameLoop;
import com.za.zenith.engine.graphics.DynamicTextureAtlas;
import com.za.zenith.utils.Identifier;
import com.za.zenith.utils.LiveReloadable;
import com.za.zenith.utils.Logger;
import com.za.zenith.world.blocks.BlockRegistry;
import com.za.zenith.world.items.ItemRegistry;
import org.lwjgl.glfw.GLFW;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * DevInspectorScreen - Advanced Live Registry & Config Editor.
 * Features: Undo/Redo, Choice Editor (arrows), Reset to Default, Hot-Reload.
 */
public class DevInspectorScreen implements Screen {
    private enum RegistryType { BLOCKS, ITEMS, ANIMATIONS, ACTIONS, CONFIGS }
    private RegistryType activeRegistry = RegistryType.BLOCKS;
    private final ScrollPanel sidebarScroller = new ScrollPanel();
    private final ScrollPanel propertiesScroller = new ScrollPanel();
    
    private Object rootObject;
    private String rootTitle;
    private final Set<String> expandedNodes = new HashSet<>();
    
    private String editingPath = null;
    private String editingValue = "";
    private Field editingField = null;
    private Object editingParent = null;

    // Undo/Redo System
    private static class ChangeRecord {
        final Object target;
        final Field field;
        final Object oldValue;
        final Object newValue;
        ChangeRecord(Object target, Field field, Object oldValue, Object newValue) {
            this.target = target;
            this.field = field;
            this.oldValue = oldValue;
            this.newValue = newValue;
        }
    }
    private final Deque<ChangeRecord> undoStack = new ArrayDeque<>();
    
    // Choice Editor Mapping
    private static final Map<String, List<String>> CHOICE_MAP = new HashMap<>();
    static {
        CHOICE_MAP.put("pathType", List.of("linear", "arc", "bezier"));
        CHOICE_MAP.put("pathInterpolation", List.of("linear", "quad_in", "quad_out", "quad_in_out", "cubic_in", "cubic_out", "cubic_in_out", "sine", "smootherstep"));
        CHOICE_MAP.put("interpolation", List.of("linear", "quad_in", "quad_out", "quad_in_out", "cubic_in", "cubic_out", "cubic_in_out", "sine", "smootherstep"));
        CHOICE_MAP.put("placementType", List.of("DEFAULT", "SLAB", "STAIRS", "LOG", "DOUBLE_PLANT", "WALL", "FENCE"));
        CHOICE_MAP.put("requiredTool", List.of("none", "pickaxe", "shovel", "axe", "crowbar", "knife"));
        CHOICE_MAP.put("type", List.of("default", "food", "tool", "equipment", "bag", "magnetic", "lootbox", "texture", "procedural", "pixels"));
    }

    private static final Gson GSON_PRETTY = new GsonBuilder().setPrettyPrinting().create();

    @Override
    public void init(int width, int height) {
        sidebarScroller.setBounds(20, 100, 240, height - 180);
        propertiesScroller.setBounds(280, 100, width - 300, height - 180);
    }

    @Override
    public void render(UIRenderer renderer, int sw, int sh, DynamicTextureAtlas atlas) {
        renderer.setupUIProjection(sw, sh);
        renderer.getPrimitivesRenderer().renderDarkenedBackground();
        
        // Panels
        renderer.getPrimitivesRenderer().renderRect(10, 10, sw - 20, sh - 20, sw, sh, 0.02f, 0.02f, 0.02f, 0.98f);
        renderer.getPrimitivesRenderer().renderRect(10, 10, sw - 20, 40, sw, sh, 0.0f, 0.4f, 0.7f, 1.0f);
        renderer.getFontRenderer().drawString("ZENITH PRO EDITOR v2.5", 30, 20, 18, sw, sh);

        // Registry Tabs
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
        
        // Toolbar / Footer
        renderFooter(renderer, sw, sh);
    }

    private void renderTabs(UIRenderer renderer, int sw, int sh) {
        int x = 30;
        int y = 65;
        for (RegistryType type : RegistryType.values()) {
            boolean active = activeRegistry == type;
            int tw = renderer.getFontRenderer().getStringWidth(type.name(), 14);
            if (active) renderer.getPrimitivesRenderer().renderRect(x - 10, y - 5, tw + 20, 25, sw, sh, 0.0f, 0.6f, 1.0f, 0.3f);
            renderer.getFontRenderer().drawString(type.name(), x, y, 14, sw, sh, active ? 1.0f : 0.6f, active ? 1.0f : 0.6f, active ? 1.0f : 0.6f, 1.0f);
            x += tw + 30;
        }
    }

    private void renderSidebar(UIRenderer renderer, int sw, int sh) {
        int startY = 100;
        int y = (int) (startY - sidebarScroller.getOffset());
        int h = 22;
        
        switch (activeRegistry) {
            case BLOCKS -> {
                for (Identifier id : BlockRegistry.getRegistry().getIds()) {
                    renderSidebarEntry(renderer, id.toString(), id.toString(), y, h, sw, sh);
                    y += h;
                }
            }
            case ITEMS -> {
                for (Identifier id : ItemRegistry.getRegistry().getIds()) {
                    renderSidebarEntry(renderer, id.toString(), id.toString(), y, h, sw, sh);
                    y += h;
                }
            }
            case ANIMATIONS -> {
                for (String key : com.za.zenith.entities.parkour.animation.AnimationRegistry.getKeys()) {
                    renderSidebarEntry(renderer, key, key, y, h, sw, sh);
                    y += h;
                }
            }
            case ACTIONS -> {
                for (Identifier id : com.za.zenith.world.actions.ActionRegistry.getKeys()) {
                    renderSidebarEntry(renderer, id.toString(), id.toString(), y, h, sw, sh);
                    y += h;
                }
            }
            case CONFIGS -> {
                renderSidebarEntry(renderer, "Physics Settings", "physics", y, h, sw, sh); y += h;
                renderSidebarEntry(renderer, "World Settings", "world", y, h, sw, sh); y += h;
                renderSidebarEntry(renderer, "Sky Settings", "sky", y, h, sw, sh); y += h;
            }
        }
        sidebarScroller.updateContentHeight(y - (startY - sidebarScroller.getOffset()));
    }

    private void renderSidebarEntry(UIRenderer renderer, String label, String key, int y, int h, int sw, int sh) {
        boolean selected = key.equals(rootTitle);
        if (selected) renderer.getPrimitivesRenderer().renderRect(20, y - 2, 230, h, sw, sh, 0.0f, 0.6f, 1.0f, 0.2f);
        renderer.getFontRenderer().drawString(label, 30, y, 12, sw, sh, selected ? 0.0f : 0.9f, selected ? 0.8f : 0.9f, selected ? 1.0f : 0.9f, 1.0f);
    }

    private void renderProperties(UIRenderer renderer, int sw, int sh) {
        if (rootObject == null) {
            renderer.getFontRenderer().drawString("Select an entry to begin editing", 300, 150, 16, sw, sh, 0.4f, 0.4f, 0.4f, 1.0f);
            return;
        }

        int startY = 110;
        int y = (int) (startY - propertiesScroller.getOffset());
        
        renderer.getFontRenderer().drawString(rootTitle, 300, y, 16, sw, sh, 0.0f, 0.8f, 1.0f, 1.0f);
        
        // Reset Button
        int rw = 80;
        if (isMouseOver(getMx(), getMy(), sw - 100, y - 5, rw, 22)) 
            renderer.getPrimitivesRenderer().renderRect(sw - 100, y - 5, rw, 22, sw, sh, 1.0f, 0.2f, 0.2f, 0.4f);
        else 
            renderer.getPrimitivesRenderer().renderRect(sw - 100, y - 5, rw, 22, sw, sh, 0.2f, 0.2f, 0.2f, 0.6f);
        renderer.getFontRenderer().drawString("RESET", sw - 85, y, 12, sw, sh);

        y += 40;
        y = renderObject(renderer, rootObject, "root", "", 300, y, 0, sw, sh);
        propertiesScroller.updateContentHeight(y - (startY - propertiesScroller.getOffset()));
    }

    private int renderObject(UIRenderer renderer, Object obj, String name, String path, int x, int y, int depth, int sw, int sh) {
        if (obj == null) {
            renderer.getFontRenderer().drawString(name + ": null", x, y, 14, sw, sh, 0.5f, 0.5f, 0.5f, 1.0f);
            return y + 22;
        }

        Class<?> clazz = obj.getClass();
        if (isSimpleType(clazz)) {
            return renderValue(renderer, obj, name, path, x, y, sw, sh);
        }

        boolean expanded = expandedNodes.contains(path);
        String prefix = expanded ? "[-] " : "[>] ";
        renderer.getFontRenderer().drawString(prefix + name, x, y, 14, sw, sh, 0.0f, 0.7f, 1.0f, 1.0f);
        
        int nextY = y + 22;
        if (expanded) {
            if (obj instanceof Map<?,?> map) {
                for (Map.Entry<?,?> entry : map.entrySet()) {
                    String key = String.valueOf(entry.getKey());
                    nextY = renderObject(renderer, entry.getValue(), key, path + "." + key, x + 20, nextY, depth + 1, sw, sh);
                }
            } else if (obj instanceof List<?> list) {
                for (int i = 0; i < list.size(); i++) {
                    nextY = renderObject(renderer, list.get(i), "[" + i + "]", path + "." + i, x + 20, nextY, depth + 1, sw, sh);
                }
            } else if (clazz.isArray()) {
                int len = Array.getLength(obj);
                for (int i = 0; i < len; i++) {
                    nextY = renderObject(renderer, Array.get(obj, i), "[" + i + "]", path + "." + i, x + 20, nextY, depth + 1, sw, sh);
                }
            } else {
                Class<?> c = clazz;
                while (c != null && c != Object.class) {
                    for (Field field : c.getDeclaredFields()) {
                        if (Modifier.isStatic(field.getModifiers()) || field.getName().equals("sourcePath")) continue;
                        field.setAccessible(true);
                        try {
                            nextY = renderObject(renderer, field.get(obj), field.getName(), path.isEmpty() ? field.getName() : path + "." + field.getName(), x + 20, nextY, depth + 1, sw, sh);
                        } catch (Exception e) {}
                    }
                    c = c.getSuperclass();
                }
            }
        }
        return nextY;
    }

    private int renderValue(UIRenderer renderer, Object val, String name, String path, int x, int y, int sw, int sh) {
        String label = name + ": ";
        int lw = renderer.getFontRenderer().getStringWidth(label, 14);
        renderer.getFontRenderer().drawString(label, x, y, 14, sw, sh, 0.7f, 0.7f, 0.7f, 1.0f);
        
        boolean isEditing = path.equals(editingPath);
        String valStr = isEditing ? editingValue + "_" : String.valueOf(val);
        
        if (CHOICE_MAP.containsKey(name)) {
            // Render choice buttons [<] Value [>]
            renderer.getFontRenderer().drawString("[<]", x + lw, y, 14, sw, sh, 0.0f, 0.6f, 1.0f, 1.0f);
            int vw = renderer.getFontRenderer().getStringWidth(valStr, 14);
            renderer.getFontRenderer().drawString(valStr, x + lw + 30, y, 14, sw, sh, 1.0f, 1.0f, 1.0f, 1.0f);
            renderer.getFontRenderer().drawString("[>]", x + lw + 30 + vw + 10, y, 14, sw, sh, 0.0f, 0.6f, 1.0f, 1.0f);
        } else if (val instanceof Boolean b) {
            String bStr = b ? "[ YES ]" : "[  NO  ]";
            renderer.getFontRenderer().drawString(bStr, x + lw, y, 14, sw, sh, b ? 0.0f : 0.8f, b ? 1.0f : 0.4f, b ? 0.5f : 0.4f, 1.0f);
        } else {
            renderer.getFontRenderer().drawString(valStr, x + lw, y, 14, sw, sh, isEditing ? 0.0f : 1.0f, isEditing ? 1.0f : 1.0f, isEditing ? 0.0f : 1.0f, 1.0f);
        }
        
        return y + 22;
    }

    private void renderFooter(UIRenderer renderer, int sw, int sh) {
        renderer.getPrimitivesRenderer().renderRect(10, sh - 40, sw - 20, 30, sw, sh, 0.05f, 0.05f, 0.05f, 1.0f);
        String info = "CTRL+Z: Undo | ENTER: Save | Click Value to Edit | Mouse Scroll: Pan";
        if (!undoStack.isEmpty()) info += " | Last change: " + undoStack.peek().field.getName();
        renderer.getFontRenderer().drawString(info, 30, sh - 30, 12, sw, sh, 0.6f, 0.6f, 0.6f, 1.0f);
    }

    private boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive() || clazz == String.class || clazz == Integer.class || clazz == Float.class || 
               clazz == Boolean.class || clazz == Double.class || clazz == Identifier.class || clazz.isEnum();
    }

    @Override
    public boolean handleMouseClick(float mx, float my, int button) {
        // Tabs
        if (my > 50 && my < 90) {
            int x = 30;
            for (RegistryType type : RegistryType.values()) {
                int tw = rendererWidth(type.name());
                if (mx > x - 10 && mx < x + tw + 10) {
                    activeRegistry = type;
                    selectRoot(null, null);
                    sidebarScroller.reset();
                    return true;
                }
                x += tw + 30;
            }
        }

        // Reset Button
        if (rootObject != null && isMouseOver(mx, my, GameLoop.getInstance().getWindow().getWidth() - 100, (int)(110 - propertiesScroller.getOffset()), 80, 22)) {
            resetCurrentObject();
            return true;
        }

        // Sidebar
        if (mx > 20 && mx < 260 && my > 100) {
            int y = (int) (100 - sidebarScroller.getOffset());
            handleSidebarClick(mx, my, y);
            return true;
        }

        // Properties
        if (mx > 280 && rootObject != null) {
            applyEditing();
            handlePropertyInteraction(rootObject, "root", "", 300, (int)(150 - propertiesScroller.getOffset()), mx, my);
            return true;
        }

        return false;
    }

    private int rendererWidth(String s) { 
        com.za.zenith.engine.graphics.Renderer r = GameLoop.getInstance().getRenderer();
        if (r == null || r.getUIRenderer() == null) return s.length() * 8; // Fallback
        return r.getUIRenderer().getFontRenderer().getStringWidth(s, 14); 
    }
    private float getMx() { return GameLoop.getInstance().getInputManager().getCurrentMousePos().x; }
    private float getMy() { return GameLoop.getInstance().getInputManager().getCurrentMousePos().y; }
    private boolean isMouseOver(float mx, float my, int x, int y, int w, int h) { return mx >= x && mx <= x + w && my >= y && my <= y + h; }

    private void handleSidebarClick(float mx, float my, int y) {
        int h = 22;
        switch (activeRegistry) {
            case BLOCKS -> {
                for (Identifier id : BlockRegistry.getRegistry().getIds()) {
                    if (my > y && my < y + h) selectRoot(BlockRegistry.getBlock(id), id.toString());
                    y += h;
                }
            }
            case ITEMS -> {
                for (Identifier id : ItemRegistry.getRegistry().getIds()) {
                    if (my > y && my < y + h) selectRoot(ItemRegistry.getItem(id), id.toString());
                    y += h;
                }
            }
            case ANIMATIONS -> {
                for (String key : com.za.zenith.entities.parkour.animation.AnimationRegistry.getKeys()) {
                    if (my > y && my < y + h) selectRoot(com.za.zenith.entities.parkour.animation.AnimationRegistry.get(key), key);
                    y += h;
                }
            }
            case ACTIONS -> {
                for (Identifier id : com.za.zenith.world.actions.ActionRegistry.getKeys()) {
                    if (my > y && my < y + h) selectRoot(com.za.zenith.world.actions.ActionRegistry.get(id), id.toString());
                    y += h;
                }
            }
            case CONFIGS -> {
                if (my > y && my < y + h) selectRoot(com.za.zenith.world.physics.PhysicsSettings.getInstance(), "Physics Settings"); y += h;
                if (my > y && my < y + h) selectRoot(com.za.zenith.world.WorldSettings.getInstance(), "World Settings"); y += h;
                if (my > y && my < y + h) selectRoot(com.za.zenith.engine.graphics.SkySettings.getInstance(), "Sky Settings"); y += h;
            }
        }
    }

    private int handlePropertyInteraction(Object obj, String name, String path, int x, int y, float mx, float my) {
        if (obj == null) return y + 22;
        Class<?> clazz = obj.getClass();
        
        if (isSimpleType(clazz)) {
            if (my > y && my < y + 20) {
                if (CHOICE_MAP.containsKey(name)) {
                    int lw = rendererWidth(name + ": ");
                    if (mx > x + lw && mx < x + lw + 25) cycleChoice(obj, name, path, -1);
                    else if (mx > x + lw + 30) cycleChoice(obj, name, path, 1);
                } else if (obj instanceof Boolean b) {
                    applyChange(obj, !b, name, path);
                } else {
                    editingPath = path;
                    editingValue = String.valueOf(obj);
                    findEditingParent(rootObject, "", path);
                }
            }
            return y + 22;
        }

        if (my > y && my < y + 20) {
            if (expandedNodes.contains(path)) expandedNodes.remove(path);
            else expandedNodes.add(path);
            return y + 22;
        }

        int nextY = y + 22;
        if (expandedNodes.contains(path)) {
            if (obj instanceof Map<?,?> map) {
                for (Map.Entry<?,?> entry : map.entrySet()) {
                    nextY = handlePropertyInteraction(entry.getValue(), String.valueOf(entry.getKey()), path + "." + entry.getKey(), x + 20, nextY, mx, my);
                }
            } else if (obj instanceof List<?> list) {
                for (int i = 0; i < list.size(); i++) {
                    nextY = handlePropertyInteraction(list.get(i), "[" + i + "]", path + "." + i, x + 20, nextY, mx, my);
                }
            } else if (clazz.isArray()) {
                int len = Array.getLength(obj);
                for (int i = 0; i < len; i++) {
                    nextY = handlePropertyInteraction(Array.get(obj, i), "[" + i + "]", path + "." + i, x + 20, nextY, mx, my);
                }
            } else {
                Class<?> c = clazz;
                while (c != null && c != Object.class) {
                    for (Field field : c.getDeclaredFields()) {
                        if (Modifier.isStatic(field.getModifiers()) || field.getName().equals("sourcePath")) continue;
                        try {
                            field.setAccessible(true);
                            nextY = handlePropertyInteraction(field.get(obj), field.getName(), path.isEmpty() ? field.getName() : path + "." + field.getName(), x + 20, nextY, mx, my);
                        } catch (Exception e) {}
                    }
                    c = c.getSuperclass();
                }
            }
        }
        return nextY;
    }

    private void cycleChoice(Object currentVal, String name, String path, int dir) {
        List<String> choices = CHOICE_MAP.get(name);
        int idx = choices.indexOf(String.valueOf(currentVal));
        if (idx == -1) idx = 0;
        idx = (idx + dir + choices.size()) % choices.size();
        applyChange(currentVal, choices.get(idx), name, path);
    }

    private void applyChange(Object oldVal, Object newVal, String name, String path) {
        findEditingParent(rootObject, "", path);
        if (editingField != null && editingParent != null) {
            try {
                editingField.setAccessible(true);
                // Convert type if needed
                Object finalVal = newVal;
                if (editingField.getType() == Identifier.class && newVal instanceof String s) finalVal = Identifier.of(s);
                
                undoStack.push(new ChangeRecord(editingParent, editingField, oldVal, finalVal));
                editingField.set(editingParent, finalVal);
                
                if (rootObject instanceof LiveReloadable lr) {
                    lr.onLiveReload();
                    saveToJson(lr);
                }
                Logger.info("Inspector: Changed " + name + " to " + finalVal);
            } catch (Exception e) {
                Logger.error("Failed to apply change: " + e.getMessage());
            }
        }
        editingField = null; editingParent = null;
    }

    private void selectRoot(Object obj, String title) {
        rootObject = obj; rootTitle = title;
        expandedNodes.clear(); expandedNodes.add("");
        propertiesScroller.reset();
    }

    private void resetCurrentObject() {
        if (rootObject instanceof LiveReloadable lr && lr.getSourcePath() != null) {
            Logger.info("Inspector: Resetting " + rootTitle + " from disk...");
            // Simple approach: trigger a reload of all data
            com.za.zenith.world.DataLoader.loadAll();
            // Re-find our object in the new registry
            if (activeRegistry == RegistryType.BLOCKS) rootObject = BlockRegistry.getBlock(Identifier.of(rootTitle));
            else if (activeRegistry == RegistryType.ITEMS) rootObject = ItemRegistry.getItem(Identifier.of(rootTitle));
            // etc...
            if (rootObject instanceof LiveReloadable newLr) newLr.onLiveReload();
        }
    }

    private void findEditingParent(Object obj, String currentPath, String targetPath) {
        if (obj == null) return;
        Class<?> clazz = obj.getClass();
        if (isSimpleType(clazz)) return;
        if (obj instanceof Map || obj instanceof List || clazz.isArray()) return;

        Class<?> c = clazz;
        while (c != null && c != Object.class) {
            for (Field field : c.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue;
                String fieldPath = currentPath.isEmpty() ? field.getName() : currentPath + "." + field.getName();
                if (fieldPath.equals(targetPath)) {
                    editingParent = obj; editingField = field; return;
                }
                if (!isSimpleType(field.getType())) {
                    try { field.setAccessible(true); findEditingParent(field.get(obj), fieldPath, targetPath); } catch (Exception e) {}
                }
            }
            c = c.getSuperclass();
        }
    }

    private void applyEditing() {
        if (editingPath == null || editingField == null || editingParent == null) return;
        try {
            editingField.setAccessible(true);
            Class<?> type = editingField.getType();
            Object newVal = null;
            if (type == int.class || type == Integer.class) newVal = Integer.parseInt(editingValue);
            else if (type == float.class || type == Float.class) newVal = Float.parseFloat(editingValue);
            else if (type == long.class || type == Long.class) newVal = Long.parseLong(editingValue);
            else if (type == boolean.class || type == Boolean.class) newVal = Boolean.parseBoolean(editingValue);
            else if (type == String.class) newVal = editingValue;
            else if (type == Identifier.class) newVal = Identifier.of(editingValue);
            
            if (newVal != null) {
                undoStack.push(new ChangeRecord(editingParent, editingField, editingField.get(editingParent), newVal));
                editingField.set(editingParent, newVal);
                if (rootObject instanceof LiveReloadable lr) {
                    lr.onLiveReload();
                    saveToJson(lr);
                }
            }
        } catch (Exception e) { Logger.error("Edit failed: " + e.getMessage()); }
        editingPath = null; editingField = null; editingParent = null;
    }

    private void saveToJson(LiveReloadable lr) {
        String relPath = lr.getSourcePath();
        if (relPath == null) return;
        File file = new File(new File(System.getProperty("user.dir"), "src/main/resources"), relPath);
        try (FileWriter writer = new FileWriter(file)) {
            GSON_PRETTY.toJson(lr, writer);
        } catch (Exception e) { Logger.error("Save failed: " + e.getMessage()); }
    }

    @Override
    public boolean handleKeyPress(int key) {
        boolean ctrl = GameLoop.getInstance().getWindow().isKeyPressed(GLFW.GLFW_KEY_LEFT_CONTROL);
        if (ctrl && key == GLFW.GLFW_KEY_Z && !undoStack.isEmpty()) {
            ChangeRecord last = undoStack.pop();
            try {
                last.field.setAccessible(true);
                last.field.set(last.target, last.oldValue);
                if (rootObject instanceof LiveReloadable lr) {
                    lr.onLiveReload();
                    saveToJson(lr);
                }
                Logger.info("Inspector: Undo " + last.field.getName());
            } catch (Exception e) {}
            return true;
        }

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

    @Override public boolean handleChar(int codepoint) { if (editingPath != null) { editingValue += (char) codepoint; return true; } return false; }
    @Override public boolean handleScroll(double yoffset) {
        float mx = GameLoop.getInstance().getInputManager().getCurrentMousePos().x;
        if (mx < 270) sidebarScroller.handleScroll(yoffset); else propertiesScroller.handleScroll(yoffset);
        return true;
    }
}
