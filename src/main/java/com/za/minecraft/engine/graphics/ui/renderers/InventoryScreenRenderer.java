package com.za.minecraft.engine.graphics.ui.renderers;

import com.za.minecraft.engine.core.GameLoop;
import com.za.minecraft.engine.core.PlayerMode;
import com.za.minecraft.engine.graphics.ui.Hotbar;
import com.za.minecraft.engine.graphics.ui.InventoryScreen;
import com.za.minecraft.engine.graphics.ui.Screen;
import com.za.minecraft.engine.graphics.ui.ScreenManager;
import com.za.minecraft.engine.graphics.ui.ScrollPanel;
import com.za.minecraft.engine.graphics.ui.SlotUI;
import com.za.minecraft.engine.graphics.ui.UIRenderer;
import com.za.minecraft.entities.Player;
import com.za.minecraft.utils.I18n;
import com.za.minecraft.world.items.Item;
import com.za.minecraft.world.items.ItemRegistry;
import com.za.minecraft.world.items.ItemStack;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;

public class InventoryScreenRenderer {
    private final UIRenderer renderer;
    private final ScrollPanel devScroller = new ScrollPanel();
    private int lastSw = 0, lastSh = 0;
    private PlayerMode lastMode = null;

    public InventoryScreenRenderer(UIRenderer renderer) {
        this.renderer = renderer;
        devScroller.setBounds(0, 0, 0, 0); // Will be set in render
    }

    public void renderInventory(Hotbar hotbar, int screenWidth, int screenHeight, com.za.minecraft.engine.graphics.DynamicTextureAtlas atlas) {
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
            java.util.Set<com.za.minecraft.entities.inventory.Slot> dragged = GameLoop.getInstance().getInputManager().getDraggedSlots();
            
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

    public void renderDeveloperPanel(int devX, int startY, int slotSize, int spacing, int sw, int sh, com.za.minecraft.engine.graphics.DynamicTextureAtlas atlas) {
        int cols = 7;
        int rows = 14; 
        int padding = 12;
        int slotsWidth = cols * (slotSize + spacing) - spacing; 
        int devWidth = slotsWidth + padding * 2;
        int devHeight = rows * (slotSize + spacing) - spacing + padding * 2;
        
        int bgX = devX - padding;
        int bgY = startY - padding;
        renderer.getPrimitivesRenderer().renderRect(bgX, bgY - 24, devWidth, devHeight + 24, sw, sh, 0.05f, 0.05f, 0.05f, 0.95f); 
        renderer.getPrimitivesRenderer().renderRect(bgX, bgY - 24, devWidth, 24, sw, sh, 0.15f, 0.15f, 0.15f, 1.0f); 
        renderer.getFontRenderer().drawString(I18n.get("gui.developer_panel").toUpperCase(), devX, bgY - 18, 14, sw, sh, 0.0f, 0.6f, 1.0f, 1.0f);

        devScroller.setBounds(bgX, bgY, devWidth, devHeight);
        List<Item> allItems = new ArrayList<>(ItemRegistry.getAllItems().values());
        allItems.sort(java.util.Comparator.comparingInt(Item::getId));
        
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
                    String name = I18n.get(allItems.get(i).getName());
                    int tx = (int)mx + 12;
                    int ty = (int)my - 12;
                    int textWidth = renderer.getFontRenderer().getStringWidth(name, 14);
                    renderer.getPrimitivesRenderer().renderRect(tx, ty - 2, textWidth + 8, 20, sw, sh, 0.1f, 0.1f, 0.1f, 0.9f);
                    renderer.getFontRenderer().drawString(name, tx + 4, ty, 14, sw, sh);
                    break;
                }
            }
        }
    }

    private void renderInventoryTooltip(InventoryScreen screen, int slotSize, Player player, int sw, int sh) {
        float mx = GameLoop.getInstance().getInputManager().getCurrentMousePos().x;
        float my = GameLoop.getInstance().getInputManager().getCurrentMousePos().y;
        
        SlotUI hoveredUI = screen != null ? screen.getSlotAt(mx, my) : null;
        if (hoveredUI != null) {
            ItemStack hovered = hoveredUI.getSlot().getStack();
            if (hovered != null) {
                String name = I18n.get(hovered.getItem().getName());
                int nameSize = 16;
                int textWidth = renderer.getFontRenderer().getStringWidth(name, nameSize);
                int tx = (int)mx + 12;
                int ty = (int)my - 12;
                
                // Using highlight logic or rect logic for background
                renderer.getPrimitivesRenderer().renderRect(tx, ty - nameSize/2, textWidth + 8, nameSize + 8, sw, sh, 0.1f, 0.1f, 0.1f, 0.9f);
                renderer.getFontRenderer().drawString(name, tx + 4, ty + 4, nameSize, sw, sh);
            }
        }
    }

    public ScrollPanel getDevScroller() {
        return devScroller;
    }
}
