package com.za.zenith.engine.graphics.ui;

import com.za.zenith.engine.core.GameLoop;
import com.za.zenith.engine.graphics.DynamicTextureAtlas;
import com.za.zenith.utils.Identifier;
import com.za.zenith.world.blocks.BlockRegistry;
import com.za.zenith.world.items.ItemRegistry;
import org.lwjgl.glfw.GLFW;

import java.lang.reflect.Field;
import java.util.Collection;

/**
 * DevInspectorScreen - Live Registry Inspector.
 * Phase 5: Live editing of any JSON-driven data.
 */
public class DevInspectorScreen implements Screen {
    private enum RegistryType { BLOCKS, ITEMS, ANIMATIONS }
    private RegistryType activeRegistry = RegistryType.BLOCKS;
    private final ScrollPanel sidebarScroller = new ScrollPanel();
    private final ScrollPanel propertiesScroller = new ScrollPanel();
    private Object selectedObject;
    private Identifier selectedId;
    private String selectedAnimationKey;

    @Override
    public void init(int width, int height) {
        sidebarScroller.setBounds(20, 100, 240, height - 150);
        propertiesScroller.setBounds(280, 100, width - 300, height - 150);
    }

    @Override
    public void render(UIRenderer renderer, int sw, int sh, DynamicTextureAtlas atlas) {
        renderer.getPrimitivesRenderer().renderDarkenedBackground();
        
        // Background Panels
        renderer.getPrimitivesRenderer().renderRect(10, 10, sw - 20, sh - 20, sw, sh, 0.02f, 0.02f, 0.02f, 0.98f);
        renderer.getPrimitivesRenderer().renderRect(10, 10, sw - 20, 40, sw, sh, 0.0f, 0.4f, 0.7f, 1.0f);
        renderer.getFontRenderer().drawString("ZENITH LIVE INSPECTOR", 30, 20, 18, sw, sh);

        // Registry Tabs
        renderTabs(renderer, sw, sh);

        // Sidebar (IDs)
        sidebarScroller.begin(sw, sh);
        renderSidebar(renderer, sw, sh);
        sidebarScroller.end();
        sidebarScroller.renderScrollbar(renderer, sw, sh);

        // Properties
        propertiesScroller.begin(sw, sh);
        renderProperties(renderer, sw, sh);
        propertiesScroller.end();
        propertiesScroller.renderScrollbar(renderer, sw, sh);
        
        // Footer
        renderer.getFontRenderer().drawString("ESC: Close | Mouse Wheel: Scroll | Click: Select", 30, sh - 30, 12, sw, sh, 0.5f, 0.5f, 0.5f, 1.0f);
    }

    private void renderTabs(UIRenderer renderer, int sw, int sh) {
        int x = 30;
        int y = 65;
        for (RegistryType type : RegistryType.values()) {
            boolean active = activeRegistry == type;
            int tw = renderer.getFontRenderer().getStringWidth(type.name(), 14);
            if (active) renderer.getPrimitivesRenderer().renderRect(x - 10, y - 5, tw + 20, 25, sw, sh, 0.0f, 0.6f, 1.0f, 0.3f);
            renderer.getFontRenderer().drawString(type.name(), x, y, 14, sw, sh, active ? 0.0f : 0.8f, active ? 0.8f : 0.8f, active ? 1.0f : 0.8f, 1.0f);
            x += tw + 40;
        }
    }

    private void renderSidebar(UIRenderer renderer, int sw, int sh) {
        int startY = 100;
        int y = (int) (startY - sidebarScroller.getOffset());
        int h = 22;
        
        if (activeRegistry == RegistryType.ANIMATIONS) {
            java.util.Set<String> keys = com.za.zenith.entities.parkour.animation.AnimationRegistry.getKeys();
            for (String key : keys) {
                boolean selected = key.equals(selectedAnimationKey);
                if (selected) renderer.getPrimitivesRenderer().renderRect(20, y - 2, 230, h, sw, sh, 0.0f, 0.6f, 1.0f, 0.2f);
                renderer.getFontRenderer().drawString(key, 30, y, 12, sw, sh, selected ? 0.0f : 0.9f, selected ? 0.8f : 0.9f, selected ? 1.0f : 0.9f, 1.0f);
                y += h;
            }
        } else {
            Collection<Identifier> ids = switch (activeRegistry) {
                case BLOCKS -> BlockRegistry.getRegistry().getIds();
                case ITEMS -> ItemRegistry.getRegistry().getIds();
                default -> java.util.Collections.emptyList();
            };

            for (Identifier id : ids) {
                boolean selected = id.equals(selectedId);
                if (selected) renderer.getPrimitivesRenderer().renderRect(20, y - 2, 230, h, sw, sh, 0.0f, 0.6f, 1.0f, 0.2f);
                renderer.getFontRenderer().drawString(id.toString(), 30, y, 12, sw, sh, selected ? 0.0f : 0.9f, selected ? 0.8f : 0.9f, selected ? 1.0f : 0.9f, 1.0f);
                y += h;
            }
        }
        sidebarScroller.updateContentHeight(y - (startY - sidebarScroller.getOffset()));
    }

    private void renderProperties(UIRenderer renderer, int sw, int sh) {
        if (selectedObject == null) {
            renderer.getFontRenderer().drawString("Select an entry to inspect properties", 300, 150, 16, sw, sh, 0.4f, 0.4f, 0.4f, 1.0f);
            return;
        }

        int startY = 110;
        int y = (int) (startY - propertiesScroller.getOffset());
        int h = 25;
        
        String title = (selectedId != null) ? selectedId.toString() : selectedAnimationKey;
        renderer.getFontRenderer().drawString("Properties for: " + title, 300, y, 16, sw, sh, 0.0f, 0.8f, 1.0f, 1.0f);
        y += 40;

        // Reflection view
        Class<?> clazz = selectedObject.getClass();
        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                field.setAccessible(true);
                try {
                    Object val = field.get(selectedObject);
                    String label = field.getName();
                    String valStr = (val instanceof java.util.List) ? "List[" + ((java.util.List<?>)val).size() + "]" : String.valueOf(val);
                    
                    renderer.getFontRenderer().drawString(label, 300, y, 14, sw, sh, 0.7f, 0.7f, 0.7f, 1.0f);
                    int labelW = renderer.getFontRenderer().getStringWidth(label + ": ", 14);
                    renderer.getFontRenderer().drawString(valStr, 300 + labelW, y, 14, sw, sh, 1.0f, 1.0f, 1.0f, 1.0f);
                    
                    y += h;
                } catch (Exception e) {}
            }
            clazz = clazz.getSuperclass();
        }
        propertiesScroller.updateContentHeight(y - (startY - propertiesScroller.getOffset()));
    }

    @Override
    public boolean handleMouseClick(float mx, float my, int button) {
        // Tab switching
        if (my > 50 && my < 90) {
            int x = 30;
            for (RegistryType type : RegistryType.values()) {
                int tw = 80;
                if (mx > x - 10 && mx < x + tw) {
                    activeRegistry = type;
                    selectedId = null;
                    selectedAnimationKey = null;
                    selectedObject = null;
                    sidebarScroller.reset();
                    return true;
                }
                x += tw + 20;
            }
        }
        
        // Sidebar selection
        if (mx > 20 && mx < 260 && my > 100) {
            int y = (int) (100 - sidebarScroller.getOffset());
            if (activeRegistry == RegistryType.ANIMATIONS) {
                java.util.Set<String> keys = com.za.zenith.entities.parkour.animation.AnimationRegistry.getKeys();
                for (String key : keys) {
                    if (my > y - 2 && my < y + 20) {
                        selectedAnimationKey = key;
                        selectedId = null;
                        selectedObject = com.za.zenith.entities.parkour.animation.AnimationRegistry.get(key);
                        propertiesScroller.reset();
                        return true;
                    }
                    y += 22;
                }
            } else {
                Collection<Identifier> ids = switch (activeRegistry) {
                    case BLOCKS -> BlockRegistry.getRegistry().getIds();
                    case ITEMS -> ItemRegistry.getRegistry().getIds();
                    default -> java.util.Collections.emptyList();
                };
                for (Identifier id : ids) {
                    if (my > y - 2 && my < y + 20) {
                        selectedId = id;
                        selectedAnimationKey = null;
                        selectedObject = switch (activeRegistry) {
                            case BLOCKS -> BlockRegistry.getBlock(id);
                            case ITEMS -> ItemRegistry.getItem(id);
                            default -> null;
                        };
                        propertiesScroller.reset();
                        return true;
                    }
                    y += 22;
                }
            }
        }
        
        return false;
    }

    @Override
    public boolean handleKeyPress(int key) {
        if (key == GLFW.GLFW_KEY_ESCAPE) {
            ScreenManager.getInstance().closeScreen();
            GameLoop.getInstance().getInputManager().enableMouseCapture(GameLoop.getInstance().getWindow());
            return true;
        }
        return false;
    }

    @Override
    public boolean handleScroll(double yoffset) {
        float mx = GameLoop.getInstance().getInputManager().getCurrentMousePos().x;
        if (mx < 270) sidebarScroller.handleScroll(yoffset);
        else propertiesScroller.handleScroll(yoffset);
        return true;
    }
}
