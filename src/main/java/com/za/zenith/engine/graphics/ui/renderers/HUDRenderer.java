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

    private GUIConfig.HUDElementConfig getHUDConfig(String elementId) {
        GUIConfig config = GUIRegistry.get(com.za.zenith.utils.Identifier.of("zenith:hud"));
        if (config != null && config.hudElements != null) {
            return config.hudElements.get(elementId);
        }
        return null;
    }

    private int[] calculateElementPos(GUIConfig.HUDElementConfig cfg, int sw, int sh, int width, int height) {
        int baseX = sw / 2, baseY = sh / 2;
        
        String anchor = cfg.anchor.toLowerCase();
        if (anchor.contains("top")) baseY = 0;
        else if (anchor.contains("bottom")) baseY = sh;
        
        if (anchor.contains("left")) baseX = 0;
        else if (anchor.contains("right")) baseX = sw;
        
        // Special case for centered anchors
        if (anchor.equals("bottom_center")) { baseX = sw / 2; baseY = sh; }
        else if (anchor.equals("top_center")) { baseX = sw / 2; baseY = 0; }

        int alignX = 0;
        if (cfg.alignX.equals("center")) alignX = -width / 2; else if (cfg.alignX.equals("right")) alignX = -width;
        int alignY = 0;
        if (cfg.alignY.equals("center")) alignY = -height / 2; else if (cfg.alignY.equals("bottom")) alignY = -height;

        int offsetX = InventoryLayout.calculateCoord(cfg.x, sw, 0, 0);
        int offsetY = InventoryLayout.calculateCoord(cfg.y, sh, 0, 0);

        return new int[]{baseX + alignX + offsetX, baseY + alignY + offsetY};
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
            GUIConfig.HUDElementConfig nameCfg = getHUDConfig("item_name");
            if (nameCfg == null) {
                // Legacy Fallback
                String name = I18n.get(selected.getItem().getName());
                int nameSize = 20;
                int textWidth = renderer.getFontRenderer().getStringWidth(name, nameSize);
                int x = (screenWidth - textWidth) / 2;
                int y = slots.get(0).getY() - 35;
                renderer.getFontRenderer().drawString(name, x, y, nameSize, screenWidth, screenHeight);
            } else if (nameCfg.visible) {
                com.za.zenith.world.items.stats.RarityDefinition rarity = com.za.zenith.world.items.stats.RarityRegistry.get(selected.getRarity());
                String rarityColor = (rarity != null) ? rarity.colorCode() : "$f";
                String nameText = rarityColor + "$l" + selected.getDisplayName();
                
                int nameSize = nameCfg.fontSize;
                int textWidth = renderer.getFontRenderer().getStringWidth(nameText, nameSize);
                
                // Dynamic Scaling Logic
                while (textWidth > nameCfg.maxWidth && nameSize > nameCfg.minFontSize) {
                    nameSize--;
                    textWidth = renderer.getFontRenderer().getStringWidth(nameText, nameSize);
                }

                int[] pos = calculateElementPos(nameCfg, screenWidth, screenHeight, textWidth, nameSize);
                renderer.getFontRenderer().drawString(nameText, pos[0], pos[1], nameSize, screenWidth, screenHeight);
            }
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

    public void renderLootboxOpening(int sw, int sh) {
        float timer = GameLoop.getInstance().getInputManager().getLootboxOpeningTimer();
        if (timer <= 0) return;

        ItemStack stack = GameLoop.getInstance().getInputManager().getLootboxStack();
        if (stack == null) return;

        com.za.zenith.world.items.component.LootboxComponent comp = stack.getItem().getComponent(com.za.zenith.world.items.component.LootboxComponent.class);
        if (comp == null) return;

        float progress = timer / comp.openingTime();
        
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        int barWidth = 100;
        int barHeight = 4;
        int x = (sw - barWidth) / 2;
        int y = (sh / 2) + 80;

        // Background
        renderer.getPrimitivesRenderer().renderRect(x, y, barWidth, barHeight, sw, sh, 0.1f, 0.1f, 0.1f, 0.5f);
        // Progress
        renderer.getPrimitivesRenderer().renderRect(x, y, (int)(barWidth * progress), barHeight, sw, sh, 1.0f, 1.0f, 1.0f, 0.9f);

        // Text
        String text = I18n.get("ui.opening_case") + " " + (int)(progress * 100) + "%";
        int textSize = 14;
        int textWidth = renderer.getFontRenderer().getStringWidth(text, textSize);
        renderer.getFontRenderer().drawString(text, (sw - textWidth) / 2, y - 18, textSize, sw, sh, 1.0f, 1.0f, 1.0f, 1.0f);

        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }

    public void renderHunger(Hotbar hotbar, int screenWidth, int screenHeight, float hunger) {
        GUIConfig.HUDElementConfig cfg = getHUDConfig("hunger");
        if (cfg != null && !cfg.visible) return;

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        String text = I18n.format("gui.hunger", hunger);
        int textSize = (cfg != null) ? cfg.fontSize : 18;
        int textWidth = renderer.getFontRenderer().getStringWidth(text, textSize);
        
        int x, y;
        if (cfg != null) {
            int[] pos = calculateElementPos(cfg, screenWidth, screenHeight, textWidth, (int)(textSize * 1.2f));
            x = pos[0]; y = pos[1];
            renderer.getShader().setUniform("tintColor", cfg.color[0], cfg.color[1], cfg.color[2], cfg.color[3]);
        } else {
            x = (screenWidth + (int)(Hotbar.HOTBAR_WIDTH * Hotbar.HOTBAR_SCALE)) / 2 + 20;
            y = hotbar != null ? hotbar.getScreenY(screenHeight) + 10 : screenHeight - 60;
        }

        renderer.getFontRenderer().drawString(text, x, y, textSize, screenWidth, screenHeight);
        renderer.getShader().setUniform("tintColor", 1.0f, 1.0f, 1.0f, 1.0f);

        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }

    public void renderStamina(Hotbar hotbar, int screenWidth, int screenHeight, float stamina) {
        if (stamina >= 0.99f) return;
        
        GUIConfig.HUDElementConfig cfg = getHUDConfig("stamina");
        if (cfg != null && !cfg.visible) return;

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        Shader uiShader = renderer.getShader();
        uiShader.use();
        uiShader.setInt("useTexture", 0);
        uiShader.setInt("isSlot", 0);
        
        int width = (cfg != null) ? cfg.width : 120;
        int height = (cfg != null) ? cfg.height : 8;
        int x, y;
        if (cfg != null) {
            int[] pos = calculateElementPos(cfg, screenWidth, screenHeight, width, height);
            x = pos[0]; y = pos[1];
        } else {
            x = (screenWidth + (int)(Hotbar.HOTBAR_WIDTH * Hotbar.HOTBAR_SCALE)) / 2 + 20;
            y = hotbar != null ? hotbar.getScreenY(screenHeight) + 32 : screenHeight - 38;
        }

        float scaleX = (float)width / screenWidth;
        float scaleY = (float)height / screenHeight;
        float posX = (2.0f * x / screenWidth) - 1.0f + scaleX;
        float posY = 1.0f - (2.0f * y / screenHeight) - scaleY;

        glBindVertexArray(renderer.getQuadVAO());
        
        uiShader.setUniform("scale", scaleX, scaleY, 0.0f, 0.0f);
        uiShader.setUniform("position_offset", posX, posY, 0.0f, 0.0f);
        float[] bgCol = (cfg != null) ? cfg.backgroundColor : new float[]{0.0f, 0.0f, 0.0f, 0.8f};
        uiShader.setUniform("tintColor", bgCol[0], bgCol[1], bgCol[2], bgCol[3]);
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
            
            float r, g, b;
            if (cfg != null) {
                r = cfg.color[0]; g = cfg.color[1]; b = cfg.color[2];
            } else {
                r = 1.0f - stamina; g = stamina; b = 0.0f;
            }
            uiShader.setUniform("tintColor", r, g, b, 0.9f);
            glDrawElements(GL_TRIANGLES, renderer.getQuadIndicesLength(), GL_UNSIGNED_INT, 0);
        }

        glBindVertexArray(0);
        uiShader.setUniform("tintColor", 1.0f, 1.0f, 1.0f, 1.0f);

        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }

    public void renderNoise(Hotbar hotbar, int screenWidth, int screenHeight, float noise) {
        GUIConfig.HUDElementConfig cfg = getHUDConfig("noise");
        if (cfg != null && !cfg.visible) return;

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        String text = I18n.format("gui.noise", (int)(noise * 100));
        int textSize = (cfg != null) ? cfg.fontSize : 18;
        int textWidth = renderer.getFontRenderer().getStringWidth(text, textSize);
        
        int x, y;
        if (cfg != null) {
            int[] pos = calculateElementPos(cfg, screenWidth, screenHeight, textWidth, (int)(textSize * 1.2f));
            x = pos[0]; y = pos[1];
        } else {
            x = (screenWidth - (int)(Hotbar.HOTBAR_WIDTH * Hotbar.HOTBAR_SCALE)) / 2 - textWidth - 20;
            y = hotbar != null ? hotbar.getScreenY(screenHeight) + 10 : screenHeight - 60;
        }

        float r, g, b;
        if (cfg != null) {
            r = cfg.color[0]; g = cfg.color[1]; b = cfg.color[2];
        } else {
            r = 1.0f; g = 1.0f - noise; b = 1.0f - noise;
        }
        renderer.getShader().setUniform("tintColor", r, g, b, 1.0f);

        renderer.getFontRenderer().drawString(text, x, y, textSize, screenWidth, screenHeight);
        renderer.getShader().setUniform("tintColor", 1.0f, 1.0f, 1.0f, 1.0f);

        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }

    public void renderLogo(int screenWidth, int screenHeight) {
        GUIConfig.HUDElementConfig cfg = getHUDConfig("logo");
        if (cfg == null || !cfg.visible || cfg.texture == null) return;

        int width = cfg.width;
        int height = cfg.height;
        int[] pos = calculateElementPos(cfg, screenWidth, screenHeight, width, height);
        
        // Если путь не содержит src/main/resources, добавляем его
        String texturePath = cfg.texture;
        if (!texturePath.startsWith("src/main/resources/")) {
            texturePath = "src/main/resources/" + texturePath;
        }
        
        renderer.getPrimitivesRenderer().renderExternalImage(texturePath, pos[0], pos[1], width, height, screenWidth, screenHeight);
    }
}


