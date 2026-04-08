package com.za.zenith.engine.graphics.ui.renderers;

import com.za.zenith.engine.core.GameLoop;
import com.za.zenith.engine.graphics.Shader;
import com.za.zenith.engine.graphics.ui.GUIConfig;
import com.za.zenith.engine.graphics.ui.GUIRegistry;
import com.za.zenith.engine.graphics.ui.Hotbar;
import com.za.zenith.engine.graphics.ui.InventoryLayout;
import com.za.zenith.engine.graphics.ui.LayoutResult;
import com.za.zenith.engine.graphics.ui.ScreenManager;
import com.za.zenith.engine.graphics.ui.SlotUI;
import com.za.zenith.engine.graphics.ui.GroupUI;
import com.za.zenith.engine.graphics.ui.UIEffectsRenderer;
import com.za.zenith.engine.graphics.ui.UIRenderer;
import com.za.zenith.utils.I18n;
import com.za.zenith.world.items.ItemStack;

import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

public class HUDRenderer {
    private final UIRenderer renderer;

    public HUDRenderer(UIRenderer renderer) {
        this.renderer = renderer;
    }

    public void renderHotbar(Hotbar hotbar, int screenWidth, int screenHeight, com.za.zenith.engine.graphics.DynamicTextureAtlas atlas) {
        if (hotbar == null) return;
        
        if (ScreenManager.getInstance().isAnyScreenOpen()) return;

        GUIConfig config = GUIRegistry.get(com.za.zenith.utils.Identifier.of("zenith:hotbar"));
        if (config == null || !config.hudVisible) return;

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        int slotSize = (int)(20 * Hotbar.HOTBAR_SCALE);
        int spacing = (int)(2 * Hotbar.HOTBAR_SCALE);
        java.util.Map<String, com.za.zenith.world.inventory.IInventory> inventories = new java.util.HashMap<>();
        inventories.put("player", hotbar.getPlayer().getInventory());
        
        LayoutResult layout = InventoryLayout.generateLayout(screenWidth, screenHeight, slotSize, spacing, hotbar.getPlayer(), config, inventories);
        List<SlotUI> slots = layout.slots;

        if (layout.globalBackground != null) {
            renderer.getPrimitivesRenderer().renderGroupBackground(layout.globalBackground.getX(), layout.globalBackground.getY(), layout.globalBackground.getWidth(), layout.globalBackground.getHeight(), config.background);
        } else {
            if ("solid".equals(config.background.type)) {
                for (GroupUI group : layout.groups) {
                    renderer.getPrimitivesRenderer().renderGroupBackground(group.getX(), group.getY(), group.getWidth(), group.getHeight(), config.background);
                }
            }
        }

        int selectedSlot = hotbar.getSelectedSlot();
        float mx = GameLoop.getInstance().getInputManager().getCurrentMousePos().x;
        float my = GameLoop.getInstance().getInputManager().getCurrentMousePos().y;

        for (int i = 0; i < slots.size(); i++) {
            SlotUI ui = slots.get(i);
            ItemStack stack = ui.getSlot().getStack();
            
            boolean isHovered = mx >= ui.getX() && mx <= ui.getX() + slotSize && my >= ui.getY() && my <= ui.getY() + slotSize;
            String animId = "hotbar_" + i;
            
            renderer.getSlotRenderer().renderSlot(ui.getX(), ui.getY(), slotSize, stack, null, screenWidth, screenHeight, atlas, isHovered, animId, true);
            
            if (i == selectedSlot) {
                UIEffectsRenderer.renderSelection(renderer, renderer.getShader(), renderer.getQuadVAO(), ui.getX(), ui.getY(), slotSize, screenWidth, screenHeight, config.selection);
            }
        }
        
        ItemStack selected = hotbar.getSelectedItemStack();
        if (selected != null && !slots.isEmpty()) {
            String name = I18n.get(selected.getItem().getName());
            int nameSize = 20;
            int textWidth = renderer.getFontRenderer().getStringWidth(name, nameSize);
            int x = (screenWidth - textWidth) / 2;
            
            int y = slots.get(0).getY() - 35;
            renderer.getFontRenderer().drawString(name, x, y, nameSize, screenWidth, screenHeight);
        }

        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }

    public void renderFiringProgress(int screenWidth, int screenHeight, float progress) {
        if (progress <= 0.0f) return;

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        String text = I18n.format("gui.firing_progress", (int)(progress * 100));
        int textSize = 18;
        int textWidth = renderer.getFontRenderer().getStringWidth(text, textSize);
        int x = (screenWidth - textWidth) / 2;
        int y = (screenHeight / 2) + 30;

        renderer.getFontRenderer().drawString(text, x, y, textSize, screenWidth, screenHeight);

        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }

    public void renderHunger(Hotbar hotbar, int screenWidth, int screenHeight, float hunger) {
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        String text = I18n.format("gui.hunger", hunger);
        int textSize = 18;
        int textWidth = renderer.getFontRenderer().getStringWidth(text, textSize);
        
        int x = (screenWidth + (int)(Hotbar.HOTBAR_WIDTH * Hotbar.HOTBAR_SCALE)) / 2 + 20;
        int y = hotbar != null ? hotbar.getScreenY(screenHeight) + 10 : screenHeight - 60;

        renderer.getFontRenderer().drawString(text, x, y, textSize, screenWidth, screenHeight);

        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }

    public void renderStamina(Hotbar hotbar, int screenWidth, int screenHeight, float stamina) {
        if (stamina >= 0.99f) return;
        
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        Shader uiShader = renderer.getShader();
        uiShader.use();
        uiShader.setInt("useTexture", 0);
        uiShader.setInt("isSlot", 0);
        
        int width = 120;
        int height = 8;
        int x = (screenWidth + (int)(Hotbar.HOTBAR_WIDTH * Hotbar.HOTBAR_SCALE)) / 2 + 20;
        int y = hotbar != null ? hotbar.getScreenY(screenHeight) + 32 : screenHeight - 38;

        float scaleX = (float)width / screenWidth;
        float scaleY = (float)height / screenHeight;
        float posX = (2.0f * x / screenWidth) - 1.0f + scaleX;
        float posY = 1.0f - (2.0f * y / screenHeight) - scaleY;

        glBindVertexArray(renderer.getQuadVAO());
        
        uiShader.setUniform("scale", scaleX, scaleY, 0.0f, 0.0f);
        uiShader.setUniform("position_offset", posX, posY, 0.0f, 0.0f);
        uiShader.setUniform("tintColor", 0.0f, 0.0f, 0.0f, 0.8f);
        glDrawElements(GL_TRIANGLES, renderer.getQuadIndicesLength(), GL_UNSIGNED_INT, 0);
        
        float innerScaleX = scaleX - (2.0f / screenWidth);
        float innerScaleY = scaleY - (2.0f / screenHeight);
        uiShader.setUniform("scale", innerScaleX, innerScaleY, 0.0f, 0.0f);
        uiShader.setUniform("position_offset", posX, posY, 0.0f, 0.0f);
        uiShader.setUniform("tintColor", 0.2f, 0.2f, 0.2f, 0.8f);
        glDrawElements(GL_TRIANGLES, renderer.getQuadIndicesLength(), GL_UNSIGNED_INT, 0);

        if (stamina > 0) {
            float barWidth = innerScaleX * stamina;
            uiShader.setUniform("scale", barWidth, innerScaleY, 0.0f, 0.0f);
            uiShader.setUniform("position_offset", posX - (innerScaleX - barWidth), posY, 0.0f, 0.0f);
            
            float r = 1.0f - stamina;
            float g = stamina;
            float b = 0.0f;
            uiShader.setUniform("tintColor", r, g, b, 0.9f);
            glDrawElements(GL_TRIANGLES, renderer.getQuadIndicesLength(), GL_UNSIGNED_INT, 0);
        }

        glBindVertexArray(0);
        uiShader.setUniform("tintColor", 1.0f, 1.0f, 1.0f, 1.0f);

        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }

    public void renderNoise(Hotbar hotbar, int screenWidth, int screenHeight, float noise) {
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        String text = I18n.format("gui.noise", (int)(noise * 100));
        int textSize = 18;
        int textWidth = renderer.getFontRenderer().getStringWidth(text, textSize);
        
        int x = (screenWidth - (int)(Hotbar.HOTBAR_WIDTH * Hotbar.HOTBAR_SCALE)) / 2 - textWidth - 20;
        int y = hotbar != null ? hotbar.getScreenY(screenHeight) + 10 : screenHeight - 60;

        float r = 1.0f;
        float g = 1.0f - noise;
        float b = 1.0f - noise;
        renderer.getShader().setUniform("tintColor", r, g, b, 1.0f);

        renderer.getFontRenderer().drawString(text, x, y, textSize, screenWidth, screenHeight);
        renderer.getShader().setUniform("tintColor", 1.0f, 1.0f, 1.0f, 1.0f);

        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }
}
