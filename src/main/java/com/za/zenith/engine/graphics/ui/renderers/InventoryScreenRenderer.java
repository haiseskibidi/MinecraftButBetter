package com.za.zenith.engine.graphics.ui.renderers;

import com.za.zenith.engine.core.GameLoop;
import com.za.zenith.engine.core.PlayerMode;
import com.za.zenith.engine.graphics.ui.Hotbar;
import com.za.zenith.engine.graphics.ui.InventoryScreen;
import com.za.zenith.engine.graphics.ui.Screen;
import com.za.zenith.engine.graphics.ui.ScreenManager;
import com.za.zenith.engine.graphics.ui.ScrollPanel;
import com.za.zenith.engine.graphics.ui.SlotUI;
import com.za.zenith.engine.graphics.ui.UIRenderer;
import com.za.zenith.engine.graphics.ui.UISearchBar;
import com.za.zenith.entities.Player;
import com.za.zenith.utils.I18n;
import com.za.zenith.world.items.Item;
import com.za.zenith.world.items.ItemRegistry;
import com.za.zenith.world.items.ItemStack;
import com.za.zenith.world.items.ItemSearchEngine;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

public class InventoryScreenRenderer {
    private final UIRenderer renderer;
    private final ScrollPanel devScroller = new ScrollPanel();
    private int lastSw = 0, lastSh = 0;
    private PlayerMode lastMode = null;

    private final UISearchBar devSearchBar = new UISearchBar("gui.search_placeholder");

    public InventoryScreenRenderer(UIRenderer renderer) {
        this.renderer = renderer;
        devScroller.setBounds(0, 0, 0, 0); // Will be set in render
    }

    public void renderInventory(Hotbar hotbar, int screenWidth, int screenHeight, com.za.zenith.engine.graphics.DynamicTextureAtlas atlas) {
        if (hotbar == null) return;
        Player player = hotbar.getPlayer();
        if (player == null) return;

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        // Restore full-screen dimming (helps visibility of placeholders)
        renderer.getPrimitivesRenderer().renderDarkenedBackground();
        
        ScreenManager screenManager = ScreenManager.getInstance();
        if (!screenManager.isAnyScreenOpen()) {
            screenManager.openPlayerInventory(player, screenWidth, screenHeight);
        }
        
        Screen activeScreen = screenManager.getActiveScreen();
        
        PlayerMode currentMode = player.getMode();
        if (screenWidth != lastSw || screenHeight != lastSh || currentMode != lastMode) {
            activeScreen.init(screenWidth, screenHeight);
            lastSw = screenWidth;
            lastSh = screenHeight;
            lastMode = currentMode;
        }
        
        activeScreen.render(renderer, screenWidth, screenHeight, atlas);

        if (activeScreen instanceof InventoryScreen invScreen) {
            int slotSize = (int)(18 * Hotbar.HOTBAR_SCALE);
            float hmx = GameLoop.getInstance().getInputManager().getCurrentMousePos().x;
            float hmy = GameLoop.getInstance().getInputManager().getCurrentMousePos().y;
            SlotUI hoveredUI = invScreen.getSlotAt(hmx, hmy);
            java.util.Set<com.za.zenith.entities.inventory.Slot> dragged = GameLoop.getInstance().getInputManager().getDraggedSlots();
            
            List<SlotUI> slots = invScreen.getSlots();
            for (SlotUI ui : slots) {
                if (dragged.contains(ui.getSlot())) {
                    renderer.getPrimitivesRenderer().renderHighlight(ui.getX(), ui.getY(), slotSize, screenWidth, screenHeight, 0.2f, 0.6f, 1.0f, 0.4f);
                } else if (ui == hoveredUI) {
                    renderer.getPrimitivesRenderer().renderHighlight(ui.getX(), ui.getY(), slotSize, screenWidth, screenHeight, 1.0f, 1.0f, 1.0f, 0.3f);
                }
            }

            if (player.getMode() == PlayerMode.DEVELOPER) {
                int spacing = (int)(2 * Hotbar.HOTBAR_SCALE);
                int devX = screenWidth - (7 * (slotSize + spacing)) - 25;
                renderDeveloperPanel(devX, 64, slotSize, spacing, screenWidth, screenHeight, atlas);
            }

            ItemStack held = GameLoop.getInstance().getInputManager().getHeldStack();
            if (held != null) {
                float mx = GameLoop.getInstance().getInputManager().getCurrentMousePos().x;
                float my = GameLoop.getInstance().getInputManager().getCurrentMousePos().y;
                String animId = "held_stack";
                renderer.getSlotRenderer().renderSlot((int)mx - slotSize/2, (int)my - slotSize/2, slotSize, held, null, screenWidth, screenHeight, atlas, false, animId, false);
            }
            
            renderInventoryTooltip(invScreen, slotSize, player, screenWidth, screenHeight);
        }
        
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }

    public List<Item> getFilteredDevItems() {
        List<Item> allItems = new ArrayList<>(ItemRegistry.getAllItems().values());
        
        // Apply Search Filter
        allItems = com.za.zenith.world.items.ItemSearchEngine.filter(allItems, devSearchBar.getQuery());
        
        allItems.sort((a, b) -> {
            // Blocks first
            if (a.isBlock() != b.isBlock()) {
                return a.isBlock() ? -1 : 1;
            }
            // Then alphabetical by identifier
            return a.getIdentifier().toString().compareTo(b.getIdentifier().toString());
        });
        return allItems;
    }

    public void renderDeveloperPanel(int devX, int startY, int slotSize, int spacing, int sw, int sh, com.za.zenith.engine.graphics.DynamicTextureAtlas atlas) {
        int cols = 7;
        int rows = 14; 
        int padding = 12;
        int searchHeight = 24;
        
        int slotsWidth = cols * (slotSize + spacing) - spacing; 
        int devWidth = slotsWidth + padding * 2;
        int devHeight = rows * (slotSize + spacing) - spacing + padding * 2;
        
        int bgX = devX - padding;
        int bgY = startY - padding;
        
        // 1. Panel Background
        renderer.getPrimitivesRenderer().renderRect(bgX, bgY - 24 - searchHeight - 4, devWidth, devHeight + 24 + searchHeight + 4, sw, sh, 0.05f, 0.05f, 0.05f, 0.95f); 
        
        // 2. Title
        renderer.getPrimitivesRenderer().renderRect(bgX, bgY - 24 - searchHeight - 4, devWidth, 24, sw, sh, 0.15f, 0.15f, 0.15f, 1.0f); 
        renderer.getFontRenderer().drawString(I18n.get("gui.developer_panel").toUpperCase(), devX, bgY - 18 - searchHeight - 4, 14, sw, sh, 0.0f, 0.6f, 1.0f, 1.0f);

        // 3. Search Box
        devSearchBar.setBounds(bgX + 2, bgY - searchHeight - 2, devWidth - 4, searchHeight);
        devSearchBar.render(renderer, sw, sh);

        // 4. Item List
        devScroller.setBounds(bgX, bgY, devWidth, devHeight);
        List<Item> allItems = getFilteredDevItems();
        
        int totalRows = (allItems.size() + cols - 1) / cols;
        // Content height: padding top + slots height + padding bottom
        int slotsHeight = totalRows * (slotSize + spacing) - spacing;
        devScroller.updateContentHeight(slotsHeight + padding * 2);

        devScroller.begin(sw, sh);
        float offset = devScroller.getOffset();
        float mx = GameLoop.getInstance().getInputManager().getCurrentMousePos().x;
        float my = GameLoop.getInstance().getInputManager().getCurrentMousePos().y;

        for (int i = 0; i < allItems.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            
            int x = devX + col * (slotSize + spacing);
            int y = startY + row * (slotSize + spacing) - (int)offset;
            
            // CPU Culling (Match glScissor area)
            if (y + slotSize < bgY || y > bgY + devHeight) continue;
            
            boolean isHovered = mx >= x && mx <= x + slotSize && my >= y && my <= y + slotSize;
            renderer.getSlotRenderer().renderSlot(x, y, slotSize, new ItemStack(allItems.get(i)), null, sw, sh, atlas, isHovered, "dev_" + i, true);
        }
        devScroller.end();

        devScroller.renderScrollbar(renderer, sw, sh);

        if (devScroller.isMouseOver(mx, my)) {
            for (int i = 0; i < allItems.size(); i++) {
                int col = i % cols;
                int row = i / cols;
                int x = devX + col * (slotSize + spacing);
                int y = startY + row * (slotSize + spacing) - (int)offset;
                
                // Tooltip culling: only show for visible items
                if (y + slotSize < bgY || y > bgY + devHeight) continue;
                
                if (mx >= x && mx <= x + slotSize && my >= y && my <= y + slotSize) {
                    renderTooltip(I18n.get(allItems.get(i).getName()), mx, my, sw, sh);
                    break;
                }
            }
        }
    }

    public boolean handleMouseClick(float mx, float my, int button) {
        if (lastMode != PlayerMode.DEVELOPER) return false;
        return devSearchBar.handleMouseClick(mx, my, button);
    }

    public boolean handleKeyPress(int key) {
        if (lastMode != PlayerMode.DEVELOPER) return false;
        return devSearchBar.handleKeyPress(key);
    }

    public boolean handleChar(int codepoint) {
        if (lastMode != PlayerMode.DEVELOPER) return false;
        return devSearchBar.handleChar(codepoint);
    }

    private void renderTooltip(String text, float mx, float my, int sw, int sh) {
        if (text == null || text.isEmpty()) return;

        int textSize = 14;
        int textWidth = renderer.getFontRenderer().getStringWidth(text, textSize);
        int padding = 4;
        int rectWidth = textWidth + padding * 2;
        int rectHeight = textSize + padding * 2;

        // Adaptive position logic
        int tx = (int)mx + 12;
        int ty = (int)my - 12;

        // Flip to left if it would go off-screen on the right
        if (tx + rectWidth > sw) {
            tx = (int)mx - rectWidth - 12;
        }
        
        // Ensure it doesn't go off-screen on the bottom or top
        if (ty + rectHeight > sh) ty = sh - rectHeight - 2;
        if (ty < 0) ty = 2;

        renderer.getPrimitivesRenderer().renderRect(tx, ty - 2, rectWidth, rectHeight, sw, sh, 0.1f, 0.1f, 0.1f, 0.9f);
        renderer.getFontRenderer().drawString(text, tx + padding, ty, textSize, sw, sh);
    }

    private void renderInventoryTooltip(InventoryScreen screen, int slotSize, Player player, int sw, int sh) {
        float mx = GameLoop.getInstance().getInputManager().getCurrentMousePos().x;
        float my = GameLoop.getInstance().getInputManager().getCurrentMousePos().y;
        
        SlotUI hoveredUI = screen != null ? screen.getSlotAt(mx, my) : null;
        if (hoveredUI != null) {
            ItemStack hovered = hoveredUI.getSlot().getStack();
            if (hovered != null) {
                renderTooltip(I18n.get(hovered.getItem().getName()), mx, my, sw, sh);
            }
        }
    }

    public ScrollPanel getDevScroller() {
        return devScroller;
    }
}


