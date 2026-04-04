package com.za.minecraft.engine.graphics.ui;

import com.za.minecraft.utils.Identifier;
import com.za.minecraft.engine.graphics.Shader;
import com.za.minecraft.engine.graphics.Texture;
import com.za.minecraft.utils.Logger;
import com.za.minecraft.engine.core.GameLoop;
import com.za.minecraft.engine.core.PlayerMode;
import com.za.minecraft.entities.Player;
import com.za.minecraft.world.items.Item;
import com.za.minecraft.world.items.ItemStack;
import com.za.minecraft.world.blocks.Block;
import com.za.minecraft.world.blocks.BlockTextureMapper;
import com.za.minecraft.world.blocks.BlockRegistry;
import com.za.minecraft.world.items.ItemRegistry;
import com.za.minecraft.engine.graphics.ui.crosshair.CrosshairManager;
import com.za.minecraft.engine.graphics.ui.crosshair.CrosshairRenderer;
import com.za.minecraft.engine.graphics.ui.crosshair.CrosshairRegistry;
import com.za.minecraft.engine.graphics.ui.crosshair.CrosshairDefinition;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class UIRenderer {
    private Shader uiShader;
    private Shader blockShader;
    private final CrosshairManager crosshairManager = new CrosshairManager();
    private final CrosshairRenderer crosshairRenderer = new CrosshairRenderer();
    private Map<Integer, Texture> itemTextures = new HashMap<>();
    private Map<String, Texture> externalTextures = new HashMap<>();
    
    private int quadVAO;
    private int quadVBO;
    private int quadEBO;
    private Hotbar hotbar;
    private PauseMenu pauseMenu;
    private FontRenderer fontRenderer;
    private final ScrollPanel devScroller = new ScrollPanel();
    private final InventoryBlockRenderer blockRenderer = new InventoryBlockRenderer();
    private int lastSw = 0, lastSh = 0;
    private com.za.minecraft.engine.core.PlayerMode lastMode = null;
    
    private static final float[] QUAD_VERTICES = {
        -1.0f, -1.0f, 0.0f, 1.0f,
         1.0f, -1.0f, 1.0f, 1.0f,
         1.0f,  1.0f, 1.0f, 0.0f,
        -1.0f,  1.0f, 0.0f, 0.0f
    };
    
    private static final int[] QUAD_INDICES = {
        0, 1, 2,
        2, 3, 0
    };
    
    public void init() {
        uiShader = new Shader(
            "src/main/resources/shaders/ui_vertex.glsl",
            "src/main/resources/shaders/ui_fragment.glsl"
        );
        
        CrosshairRegistry.load();
        
        createQuad();
        
        fontRenderer = new FontRenderer();
        fontRenderer.init(uiShader);
        
        uiShader.use();
        uiShader.setInt("textureSampler", 0);
        uiShader.setInt("arraySampler", 1);
        
        devScroller.setBounds(0, 0, 0, 0); // Will be set in render

        Logger.info("UI Renderer initialized");
    }

    public void setHotbar(Hotbar hotbar) {
        this.hotbar = hotbar;
    }
    
    public void setPauseMenu(PauseMenu pauseMenu) {
        this.pauseMenu = pauseMenu;
    }
    
    private void createQuad() {
        quadVAO = glGenVertexArrays();
        quadVBO = glGenBuffers();
        quadEBO = glGenBuffers();
        
        glBindVertexArray(quadVAO);
        
        FloatBuffer vertexBuffer = MemoryUtil.memAllocFloat(QUAD_VERTICES.length);
        vertexBuffer.put(QUAD_VERTICES).flip();
        
        glBindBuffer(GL_ARRAY_BUFFER, quadVBO);
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);
        
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);
        
        IntBuffer indexBuffer = MemoryUtil.memAllocInt(QUAD_INDICES.length);
        indexBuffer.put(QUAD_INDICES).flip();
        
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, quadEBO);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW);
        
        glBindVertexArray(0);
        
        MemoryUtil.memFree(vertexBuffer);
        MemoryUtil.memFree(indexBuffer);
    }
    
    public void setupUIProjection(int sw, int sh) {
        uiShader.use();
        uiShader.setUniform("scale", 1.0f, 1.0f, 0.0f, 0.0f);
        uiShader.setUniform("position_offset", 0.0f, 0.0f, 0.0f, 0.0f);
        uiShader.setUniform("uvOffset", 0.0f, 0.0f, 0.0f, 0.0f);
        uiShader.setUniform("uvScale", 1.0f, 1.0f, 0.0f, 0.0f);
        uiShader.setUniform("tintColor", 1.0f, 1.0f, 1.0f, 1.0f);
        uiShader.setInt("isGrayscale", 0);
        uiShader.setInt("isSlot", 0);
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    }

    public void renderCrosshair(int screenWidth, int screenHeight) {
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        float delta = GameLoop.getInstance().getTimer().getDeltaF();
        crosshairManager.update(delta);
        
        crosshairRenderer.render(crosshairManager, uiShader, screenWidth, screenHeight, 1.0f);
        
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }
    
    public void renderHotbar(int screenWidth, int screenHeight, com.za.minecraft.engine.graphics.DynamicTextureAtlas atlas) {
        if (hotbar == null) return;
        
        // Hide HUD Hotbar if any screen is open (to avoid double hotbar)
        if (ScreenManager.getInstance().isAnyScreenOpen()) return;

        GUIConfig config = GUIRegistry.get(com.za.minecraft.utils.Identifier.of("minecraft:hotbar"));
        if (config == null || !config.hudVisible) return;

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        // Use standard layout system for hotbar
        int slotSize = (int)(20 * Hotbar.HOTBAR_SCALE);
        int spacing = (int)(2 * Hotbar.HOTBAR_SCALE);
        LayoutResult layout = InventoryLayout.generateLayout(screenWidth, screenHeight, slotSize, spacing, hotbar.getPlayer(), config);
        List<SlotUI> slots = layout.slots;

        // Render backgrounds if NOT part of a global background (usually hotbar is separate)
        if (layout.globalBackground != null) {
            renderGroupBackground(layout.globalBackground.getX(), layout.globalBackground.getY(), layout.globalBackground.getWidth(), layout.globalBackground.getHeight(), config.background);
        } else {
            // Fallback for single group if no includeGroups defined
            if ("solid".equals(config.background.type)) {
                for (GroupUI group : layout.groups) {
                    renderGroupBackground(group.getX(), group.getY(), group.getWidth(), group.getHeight(), config.background);
                }
            }
        }

        int selectedSlot = hotbar.getSelectedSlot();
        float mx = com.za.minecraft.engine.core.GameLoop.getInstance().getInputManager().getCurrentMousePos().x;
        float my = com.za.minecraft.engine.core.GameLoop.getInstance().getInputManager().getCurrentMousePos().y;

        for (int i = 0; i < slots.size(); i++) {
            SlotUI ui = slots.get(i);
            ItemStack stack = ui.getSlot().getStack();
            
            boolean isHovered = mx >= ui.getX() && mx <= ui.getX() + slotSize && my >= ui.getY() && my <= ui.getY() + slotSize;
            String animId = "hotbar_" + i;
            
            // Render slot background and item
            renderSlot(ui.getX(), ui.getY(), slotSize, stack, null, screenWidth, screenHeight, atlas, isHovered, animId, true);
            
            // Render selection frame for active slot
            if (i == selectedSlot) {
                UIEffectsRenderer.renderSelection(this, uiShader, quadVAO, ui.getX(), ui.getY(), slotSize, screenWidth, screenHeight, config.selection);
            }
        }
        
        ItemStack selected = hotbar.getSelectedItemStack();
        if (selected != null && !slots.isEmpty()) {
            String name = com.za.minecraft.utils.I18n.get(selected.getItem().getName());
            int nameSize = 20;
            int textWidth = fontRenderer.getStringWidth(name, nameSize);
            int x = (screenWidth - textWidth) / 2;
            
            // Text is above the first slot
            int y = slots.get(0).getY() - 35;
            fontRenderer.drawString(name, x, y, nameSize, screenWidth, screenHeight);
        }

        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }

    public void renderSlot(int x, int y, int size, ItemStack stack, String placeholder, int screenWidth, int screenHeight, com.za.minecraft.engine.graphics.DynamicTextureAtlas atlas) {
        renderSlot(x, y, size, stack, placeholder, screenWidth, screenHeight, atlas, false, "static_slot_" + x + "_" + y, true);
    }

    public void renderSlot(int x, int y, int size, ItemStack stack, String placeholder, int screenWidth, int screenHeight, com.za.minecraft.engine.graphics.DynamicTextureAtlas atlas, boolean isHovered, String animId, boolean drawBackground) {
        float delta = GameLoop.getInstance().getTimer().getDeltaF();
        float hoverProgress = UIAnimationManager.getHoverProgress(animId, isHovered, delta);

        if (drawBackground) {
            // Slot background with SDF Shape (TEMPORARILY DISABLED)
            uiShader.use();
            uiShader.setInt("useTexture", 0);
            uiShader.setInt("isSlot", 0); // Disable SDF Octagon for now
            
            float scaleX = (float)size / screenWidth;
            float scaleY = (float)size / screenHeight;
            float posX = (2.0f * x / screenWidth) - 1.0f + scaleX;
            float posY = 1.0f - (2.0f * y / screenHeight) - scaleY;
            
            uiShader.setUniform("scale", scaleX, scaleY, 0.0f, 0.0f);
            uiShader.setUniform("position_offset", posX, posY, 0.0f, 0.0f);
            uiShader.setFloat("hoverProgress", hoverProgress);
            
            // Dynamic background color (slightly darker base for better glow pop)
            float bgBrightness = 0.35f; 
            uiShader.setUniform("tintColor", bgBrightness, bgBrightness, bgBrightness, 0.9f);
            
            glBindVertexArray(quadVAO);
            glDrawElements(GL_TRIANGLES, QUAD_INDICES.length, GL_UNSIGNED_INT, 0);
        }
        
        uiShader.setInt("isSlot", 0); // Disable SDF for items

        if (stack != null) {
            float itemRotation = hoverProgress * 360.0f; // Full rotation speed on hover
            float itemScaleMod = 1.0f + (float)Math.sin(System.currentTimeMillis() * 0.005f) * 0.05f * hoverProgress;
            
            renderItemIcon(stack.getItem(), x + 2, y + 2, (size - 4) * itemScaleMod, screenWidth, screenHeight, atlas, itemRotation, hoverProgress);
            
            if (stack.getCount() > 1) {
                String count = String.valueOf(stack.getCount());
                fontRenderer.drawString(count, x + size - fontRenderer.getStringWidth(count, 14), y + size - 14, 14, screenWidth, screenHeight);
            }
            
            // Draw durability bar
            if (stack.getItem().isTool()) {
                com.za.minecraft.world.items.component.ToolComponent tool = stack.getItem().getComponent(com.za.minecraft.world.items.component.ToolComponent.class);
                float dur = (float)stack.getDurability() / tool.maxDurability();
                if (dur < 1.0f) {
                    renderDurabilityBar(x + 2, y + size - 4, size - 4, dur, screenWidth, screenHeight);
                }
            }
        } else if (placeholder != null && !placeholder.isEmpty()) {
            renderPlaceholder(x, y, size, placeholder, screenWidth, screenHeight);
        }
        glBindVertexArray(0);
    }

    private void renderItemIcon(Item item, float x, float y, float size, int screenWidth, int screenHeight, com.za.minecraft.engine.graphics.DynamicTextureAtlas atlas, float rotation, float hoverProgress) {
        if (item.isBlock()) {
            blockRenderer.renderBlock(item, x, y, size, screenWidth, screenHeight, atlas, rotation);
            return;
        }

        // 2D Item Sway and Breath
        float scaleX = size / screenWidth;
        float scaleY = size / screenHeight;
        float posX = (2.0f * x / screenWidth) - 1.0f + scaleX;
        float posY = 1.0f - (2.0f * y / screenHeight) - scaleY;
        
        uiShader.use();
        uiShader.setUniform("scale", scaleX, scaleY, 0.0f, 0.0f);
        uiShader.setUniform("position_offset", posX, posY, 0.0f, 0.0f);
        uiShader.setUniform("tintColor", 1.0f, 1.0f, 1.0f, 1.0f);
        uiShader.setInt("useArray", 0);
        
        glActiveTexture(GL_TEXTURE0);
        String path = item.getTexturePath();
        if (path != null && !path.isEmpty()) {
            Texture tex = itemTextures.get(item.getId());
            if (tex == null) {
                try {
                    tex = new Texture("src/main/resources/" + path, false, false);
                    itemTextures.put(item.getId(), tex);
                } catch (Exception e) {
                    Logger.error("Failed to load item texture: " + path);
                }
            }
            
            if (tex != null) {
                tex.bind();
                uiShader.setInt("useTexture", 1);
                uiShader.setUniform("uvOffset", 0.0f, 0.0f, 0.0f, 0.0f);
                uiShader.setUniform("uvScale", 1.0f, 1.0f, 0.0f, 0.0f);
            } else {
                drawFallbackIcon(item);
            }
        } else {
            drawFallbackIcon(item);
        }
        
        glBindVertexArray(quadVAO);
        glDrawElements(GL_TRIANGLES, QUAD_INDICES.length, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    private void renderPlaceholder(float x, float y, float size, String placeholder, int screenWidth, int screenHeight) {
        float ghostSize = (size - 4) * 0.7f;
        float gX = x + (size - ghostSize) / 2.0f;
        float gY = y + (size - ghostSize) / 2.0f;
        
        Texture tex = null;
        try {
            String path = placeholder.contains("/") ? placeholder : "minecraft/textures/item/" + placeholder + ".png";
            if (!path.startsWith("src/")) path = "src/main/resources/" + path;
            int placeholderId = path.hashCode();
            tex = itemTextures.get(placeholderId);
            if (tex == null) {
                tex = new Texture(path, false, false);
                itemTextures.put(placeholderId, tex);
            }
        } catch (Exception e) {}

        if (tex != null) {
            float gsX = ghostSize / screenWidth;
            float gsY = ghostSize / screenHeight;
            float gpX = (2.0f * gX / screenWidth) - 1.0f + gsX;
            float gpY = 1.0f - (2.0f * gY / screenHeight) - gsY;

            uiShader.use();
            glActiveTexture(GL_TEXTURE0);
            tex.bind();
            uiShader.setInt("useTexture", 1);
            uiShader.setInt("isGrayscale", 1);
            uiShader.setUniform("scale", gsX, gsY, 0.0f, 0.0f);
            uiShader.setUniform("position_offset", gpX, gpY, 0.0f, 0.0f);
            uiShader.setUniform("tintColor", 1.0f, 1.0f, 1.0f, 0.4f); 
            glBindVertexArray(quadVAO);
            glDrawElements(GL_TRIANGLES, QUAD_INDICES.length, GL_UNSIGNED_INT, 0);
            uiShader.setInt("isGrayscale", 0);
        }
    }

    public void renderFiringProgress(int screenWidth, int screenHeight, float progress) {
        if (progress <= 0.0f) return;

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        String text = com.za.minecraft.utils.I18n.format("gui.firing_progress", (int)(progress * 100));
        int textSize = 18;
        int textWidth = fontRenderer.getStringWidth(text, textSize);
        int x = (screenWidth - textWidth) / 2;
        int y = (screenHeight / 2) + 30;

        fontRenderer.drawString(text, x, y, textSize, screenWidth, screenHeight);

        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }

    public void renderHunger(int screenWidth, int screenHeight, float hunger) {
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        String text = com.za.minecraft.utils.I18n.format("gui.hunger", hunger);
        int textSize = 18;
        int textWidth = fontRenderer.getStringWidth(text, textSize);
        
        int x = (screenWidth + (int)(Hotbar.HOTBAR_WIDTH * Hotbar.HOTBAR_SCALE)) / 2 + 20;
        int y = hotbar.getScreenY(screenHeight) + 10;

        fontRenderer.drawString(text, x, y, textSize, screenWidth, screenHeight);

        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }

    public void renderNoise(int screenWidth, int screenHeight, float noise) {
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        String text = com.za.minecraft.utils.I18n.format("gui.noise", (int)(noise * 100));
        int textSize = 18;
        int textWidth = fontRenderer.getStringWidth(text, textSize);
        
        int x = (screenWidth - (int)(Hotbar.HOTBAR_WIDTH * Hotbar.HOTBAR_SCALE)) / 2 - textWidth - 20;
        int y = hotbar.getScreenY(screenHeight) + 10;

        float r = 1.0f;
        float g = 1.0f - noise;
        float b = 1.0f - noise;
        uiShader.setUniform("tintColor", r, g, b, 1.0f);

        fontRenderer.drawString(text, x, y, textSize, screenWidth, screenHeight);
        uiShader.setUniform("tintColor", 1.0f, 1.0f, 1.0f, 1.0f);

        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }

    private void drawFallbackIcon(Item item) {
        uiShader.setInt("useTexture", 0);
        // Рисуем ярко-пурпурный квадрат для визуализации отсутствующей текстуры (Missing Texture)
        uiShader.setUniform("tintColor", 1.0f, 0.0f, 1.0f, 0.8f);
        uiShader.setUniform("uvOffset", 0.0f, 0.0f, 0.0f, 0.0f);
        uiShader.setUniform("uvScale", 1.0f, 1.0f, 0.0f, 0.0f);
    }
    
    public void renderInventory(int screenWidth, int screenHeight, com.za.minecraft.engine.graphics.DynamicTextureAtlas atlas) {
        if (hotbar == null) return;
        Player player = hotbar.getPlayer();
        if (player == null) return;

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        renderDarkenedBackground();
        
        ScreenManager screenManager = ScreenManager.getInstance();
        if (!screenManager.isAnyScreenOpen()) {
            screenManager.openPlayerInventory(player, screenWidth, screenHeight);
        }
        
        Screen activeScreen = screenManager.getActiveScreen();
        
        // Dynamic re-init if resolution or player mode changed
        com.za.minecraft.engine.core.PlayerMode currentMode = player.getMode();
        if (screenWidth != lastSw || screenHeight != lastSh || currentMode != lastMode) {
            activeScreen.init(screenWidth, screenHeight);
            lastSw = screenWidth;
            lastSh = screenHeight;
            lastMode = currentMode;
        }
        
        activeScreen.render(this, screenWidth, screenHeight, atlas);

        if (activeScreen instanceof InventoryScreen invScreen) {
            int slotSize = (int)(18 * Hotbar.HOTBAR_SCALE);

            // Draw Highlights (Hover and Drag)
            float hmx = com.za.minecraft.engine.core.GameLoop.getInstance().getInputManager().getCurrentMousePos().x;
            float hmy = com.za.minecraft.engine.core.GameLoop.getInstance().getInputManager().getCurrentMousePos().y;
            SlotUI hoveredUI = invScreen.getSlotAt(hmx, hmy);
            java.util.Set<com.za.minecraft.entities.inventory.Slot> dragged = com.za.minecraft.engine.core.GameLoop.getInstance().getInputManager().getDraggedSlots();
            
            for (SlotUI ui : invScreen.getSlots()) {
                if (dragged.contains(ui.getSlot())) {
                    renderHighlight(ui.getX(), ui.getY(), slotSize, screenWidth, screenHeight, 0.2f, 0.6f, 1.0f, 0.4f);
                } else if (ui == hoveredUI) {
                    renderHighlight(ui.getX(), ui.getY(), slotSize, screenWidth, screenHeight, 1.0f, 1.0f, 1.0f, 0.3f);
                }
            }

            // Developer Panel (Creative Menu)
            if (player.getMode() == PlayerMode.DEVELOPER) {
                int spacing = (int)(2 * Hotbar.HOTBAR_SCALE);
                int devX = screenWidth - (7 * (slotSize + spacing)) - 25;
                renderDeveloperPanel(devX, 40, slotSize, spacing, screenWidth, screenHeight, atlas);
            }

            // --- ON TOP OF EVERYTHING ---

            // Draw Held Stack
            ItemStack held = com.za.minecraft.engine.core.GameLoop.getInstance().getInputManager().getHeldStack();
            if (held != null) {
                float mx = com.za.minecraft.engine.core.GameLoop.getInstance().getInputManager().getCurrentMousePos().x;
                float my = com.za.minecraft.engine.core.GameLoop.getInstance().getInputManager().getCurrentMousePos().y;
                String animId = "held_stack";
                // FIXED: drawBackground = false for held stack to avoid grey background
                renderSlot((int)mx - slotSize/2, (int)my - slotSize/2, slotSize, held, null, screenWidth, screenHeight, atlas, false, animId, false);
            }
            
            // Draw Tooltip for hovered slot
            renderInventoryTooltip(invScreen, slotSize, player, screenWidth, screenHeight);
        }
        
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }

    public void renderRect(int x, int y, int width, int height, int sw, int sh, float r, float g, float b, float a) {
        uiShader.use();
        uiShader.setInt("useTexture", 0);
        uiShader.setInt("isSlot", 0);
        
        float scaleX = (float)width / sw;
        float scaleY = (float)height / sh;
        float posX = (2.0f * x / sw) - 1.0f + scaleX;
        float posY = 1.0f - (2.0f * y / sh) - scaleY;

        uiShader.setUniform("scale", scaleX, scaleY, 0.0f, 0.0f);
        uiShader.setUniform("position_offset", posX, posY, 0.0f, 0.0f);
        uiShader.setUniform("tintColor", r, g, b, a);

        glBindVertexArray(quadVAO);
        glDrawElements(GL_TRIANGLES, QUAD_INDICES.length, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    public void renderGroupBackground(int x, int y, int width, int height, GUIConfig.BackgroundConfig bg) {
        int sw = GameLoop.getInstance().getWindow().getWidth();
        int sh = GameLoop.getInstance().getWindow().getHeight();
        int p = bg.padding;
        renderRect(x - p, y - p, width + p * 2, height + p * 2, sw, sh, bg.color[0], bg.color[1], bg.color[2], bg.color[3]);
    }

    public void renderHighlight(int x, int y, int size, int sw, int sh, float r, float g, float b, float a) {
        uiShader.use();
        uiShader.setInt("useTexture", 0);
        uiShader.setInt("isSlot", 1); // Use SDF for highlights too!
        uiShader.setUniform("uvOffset", 0.0f, 0.0f, 0.0f, 0.0f);
        uiShader.setUniform("uvScale", 1.0f, 1.0f, 0.0f, 0.0f);
        
        float scaleX = (float)size / sw;
        float scaleY = (float)size / sh;
        float posX = (2.0f * x / sw) - 1.0f + scaleX;
        float posY = 1.0f - (2.0f * y / sh) - scaleY;
        
        uiShader.setUniform("scale", scaleX, scaleY, 0.0f, 0.0f);
        uiShader.setUniform("position_offset", posX, posY, 0.0f, 0.0f);
        uiShader.setUniform("tintColor", r, g, b, a);
        
        glBindTexture(GL_TEXTURE_2D, 0); 
        glBindVertexArray(quadVAO);
        glDrawElements(GL_TRIANGLES, QUAD_INDICES.length, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
        uiShader.setInt("isSlot", 0);
    }

    public void renderDeveloperPanel(int devX, int startY, int slotSize, int spacing, int sw, int sh, com.za.minecraft.engine.graphics.DynamicTextureAtlas atlas) {
        int cols = 7;
        int rows = 14; // Visible rows
        int padding = 12;
        int slotsWidth = cols * (slotSize + spacing) - spacing; // Width of just the slots
        int devWidth = slotsWidth + padding * 2;
        int devHeight = rows * (slotSize + spacing) - spacing + padding * 2;
        
        // 1. Background (Symmetric)
        int bgX = devX - padding;
        int bgY = startY - padding;
        renderRect(bgX, bgY - 24, devWidth, devHeight + 24, sw, sh, 0.05f, 0.05f, 0.05f, 0.95f); // Main BG
        renderRect(bgX, bgY - 24, devWidth, 24, sw, sh, 0.15f, 0.15f, 0.15f, 1.0f); // Title bar
        fontRenderer.drawString(com.za.minecraft.utils.I18n.get("gui.developer_panel").toUpperCase(), devX, bgY - 18, 14, sw, sh, 0.0f, 0.6f, 1.0f, 1.0f);

        // 2. Setup Scroller (Matching BG bounds for interaction)
        devScroller.setBounds(bgX, bgY, devWidth, devHeight);
        List<Item> allItems = new ArrayList<>(ItemRegistry.getAllItems().values());
        int totalRows = (allItems.size() + cols - 1) / cols;
        devScroller.updateContentHeight(totalRows * (slotSize + spacing));

        // 3. Render Content with Scissor
        devScroller.begin(sw, sh);
        float offset = devScroller.getOffset();
        float mx = com.za.minecraft.engine.core.GameLoop.getInstance().getInputManager().getCurrentMousePos().x;
        float my = com.za.minecraft.engine.core.GameLoop.getInstance().getInputManager().getCurrentMousePos().y;

        for (int i = 0; i < allItems.size(); i++) {
            int col = i % cols;
            int row = i / cols;
            
            int x = devX + col * (slotSize + spacing);
            int y = startY + row * (slotSize + spacing) - (int)offset;
            
            // Culling check
            if (y + slotSize < startY || y > startY + devHeight) continue;
            
            boolean isHovered = mx >= x && mx <= x + slotSize && my >= y && my <= y + slotSize;
            renderSlot(x, y, slotSize, new ItemStack(allItems.get(i)), null, sw, sh, atlas, isHovered, "dev_" + i, true);
        }
        devScroller.end();

        // 4. Scrollbar
        devScroller.renderScrollbar(this, sw, sh);

        // 5. Tooltip
        if (devScroller.isMouseOver(mx, my)) {
            for (int i = 0; i < allItems.size(); i++) {
                int col = i % cols;
                int row = i / cols;
                int x = devX + col * (slotSize + spacing);
                int y = startY + row * (slotSize + spacing) - (int)offset;
                
                if (mx >= x && mx <= x + slotSize && my >= y && my <= y + slotSize) {
                    String name = com.za.minecraft.utils.I18n.get(allItems.get(i).getName());
                    int tx = (int)mx + 12;
                    int ty = (int)my - 12;
                    int textWidth = fontRenderer.getStringWidth(name, 14);
                    renderRect(tx, ty - 2, textWidth + 8, 20, sw, sh, 0.1f, 0.1f, 0.1f, 0.9f);
                    fontRenderer.drawString(name, tx + 4, ty, 14, sw, sh);
                    break;
                }
            }
        }
    }

    public ScrollPanel getDevScroller() {
        return devScroller;
    }

    private void renderInventoryTooltip(InventoryScreen screen, int slotSize, Player player, int sw, int sh) {
        float mx = com.za.minecraft.engine.core.GameLoop.getInstance().getInputManager().getCurrentMousePos().x;
        float my = com.za.minecraft.engine.core.GameLoop.getInstance().getInputManager().getCurrentMousePos().y;
        
        SlotUI hoveredUI = screen != null ? screen.getSlotAt(mx, my) : null;
        if (hoveredUI != null) {
            ItemStack hovered = hoveredUI.getSlot().getStack();
            if (hovered != null) {
                String name = com.za.minecraft.utils.I18n.get(hovered.getItem().getName());
                int nameSize = 16;
                int textWidth = fontRenderer.getStringWidth(name, nameSize);
                int tx = (int)mx + 12;
                int ty = (int)my - 12;
                
                renderButton(tx + textWidth/2, ty + nameSize/2, textWidth + 8, nameSize + 8, sw, sh, null, 0.1f, 0.1f, 0.1f);
                fontRenderer.drawString(name, tx + 4, ty + 4, nameSize, sw, sh);
            }
        }
    }

    private void renderDurabilityBar(int x, int y, int width, float progress, int screenWidth, int screenHeight) {
        uiShader.use();
        uiShader.setInt("useTexture", 0);
        uiShader.setInt("isSlot", 0);
        float scaleX = (float)width / screenWidth;
        float scaleY = 2.0f / screenHeight;
        float posX = (2.0f * x / screenWidth) - 1.0f + scaleX;
        float posY = 1.0f - (2.0f * y / screenHeight) - scaleY;
        
        // Background (black)
        uiShader.setUniform("scale", scaleX, scaleY, 0.0f, 0.0f);
        uiShader.setUniform("position_offset", posX, posY, 0.0f, 0.0f);
        uiShader.setUniform("tintColor", 0f, 0f, 0f, 1.0f);
        glDrawElements(GL_TRIANGLES, QUAD_INDICES.length, GL_UNSIGNED_INT, 0);
        
        // Progress (green to red)
        float barWidth = scaleX * progress;
        uiShader.setUniform("scale", barWidth, scaleY, 0.0f, 0.0f);
        uiShader.setUniform("position_offset", posX - (scaleX - barWidth), posY, 0.0f, 0.0f);
        uiShader.setUniform("tintColor", 1.0f - progress, progress, 0.0f, 1.0f);
        glDrawElements(GL_TRIANGLES, QUAD_INDICES.length, GL_UNSIGNED_INT, 0);
    }

    private void renderDarkenedBackground() {
        uiShader.use();
        uiShader.setInt("useTexture", 0);
        uiShader.setInt("isSlot", 0);
        uiShader.setUniform("scale", 1.0f, 1.0f, 0.0f, 0.0f);
        uiShader.setUniform("position_offset", 0.0f, 0.0f, 0.0f, 0.0f);
        uiShader.setUniform("uvOffset", 0.0f, 0.0f, 0.0f, 0.0f);
        uiShader.setUniform("uvScale", 1.0f, 1.0f, 0.0f, 0.0f);
        uiShader.setUniform("tintColor", 0.0f, 0.0f, 0.0f, 0.6f);
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindVertexArray(quadVAO);
        glDrawElements(GL_TRIANGLES, QUAD_INDICES.length, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
        uiShader.setUniform("tintColor", 1.0f, 1.0f, 1.0f, 1.0f);
    }

    public void renderPauseMenu(int screenWidth, int screenHeight) {
        if (pauseMenu == null || !pauseMenu.isVisible()) return;
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        renderDarkenedBackground();
        renderMenuButtons(screenWidth, screenHeight);
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }
    
    private void renderMenuButtons(int screenWidth, int screenHeight) {
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        int buttonWidth = pauseMenu.getButtonWidth();
        int buttonHeight = pauseMenu.getButtonHeight();
        int spacing = pauseMenu.getButtonSpacing();
        
        renderButton(centerX, centerY - spacing, buttonWidth, buttonHeight, screenWidth, screenHeight, 
            com.za.minecraft.utils.I18n.get("menu.resume"), 0.4f, 0.7f, 1.0f);
        renderButton(centerX, centerY + spacing, buttonWidth, buttonHeight, screenWidth, screenHeight, 
            com.za.minecraft.utils.I18n.get("menu.exit"), 1.0f, 0.4f, 0.4f);
    }
    
    private void renderButton(int x, int y, int width, int height, int screenWidth, int screenHeight, String text, float r, float g, float b) {
        uiShader.use();
        uiShader.setInt("useTexture", 0);
        uiShader.setInt("isSlot", 1); // Use SDF for buttons too
        float scaleX = (float)width / screenWidth;
        float scaleY = (float)height / screenHeight;
        float posX = (2.0f * x / screenWidth) - 1.0f;
        float posY = 1.0f - (2.0f * y / screenHeight);
        uiShader.setUniform("scale", scaleX, scaleY, 0.0f, 0.0f);
        uiShader.setUniform("position_offset", posX, posY, 0.0f, 0.0f);
        uiShader.setUniform("uvOffset", 0.0f, 0.0f, 0.0f, 0.0f);
        uiShader.setUniform("uvScale", 1.0f, 1.0f, 0.0f, 0.0f);
        uiShader.setUniform("tintColor", r, g, b, 0.9f);
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindVertexArray(quadVAO);
        glDrawElements(GL_TRIANGLES, QUAD_INDICES.length, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
        uiShader.setInt("isSlot", 0);
        
        if (text != null && !text.isEmpty()) {
            int textSize = 16;
            int textWidth = fontRenderer.getStringWidth(text, textSize);
            int textX = x - textWidth / 2;
            int textY = y - textSize / 2;
            fontRenderer.drawString(text, textX, textY, textSize, screenWidth, screenHeight);
        }
    }

    public void renderExternalImage(String path, int x, int y, int width, int height, int sw, int sh) {
        Texture tex = externalTextures.get(path);
        if (tex == null) {
            try {
                tex = new Texture("src/main/resources/" + path, false, false);
                externalTextures.put(path, tex);
            } catch (Exception e) {
                Logger.error("Failed to load external image: " + path);
                return;
            }
        }
        
        if (tex != null) {
            tex.bind();
            uiShader.use();
            uiShader.setInt("useTexture", 1);
            uiShader.setInt("useArray", 0);
            uiShader.setInt("isGrayscale", 0);
            uiShader.setInt("isSlot", 0);
            
            float scaleX = (float)width / sw;
            float scaleY = (float)height / sh;
            float posX = (2.0f * x / sw) - 1.0f + scaleX;
            float posY = 1.0f - (2.0f * y / sw) - scaleY;
            
            uiShader.setUniform("scale", scaleX, scaleY, 0.0f, 0.0f);
            uiShader.setUniform("position_offset", posX, posY, 0.0f, 0.0f);
            uiShader.setUniform("uvOffset", 0.0f, 0.0f, 0.0f, 0.0f);
            uiShader.setUniform("uvScale", 1.0f, 1.0f, 0.0f, 0.0f);
            uiShader.setUniform("tintColor", 1.0f, 1.0f, 1.0f, 1.0f);
            
            glBindVertexArray(quadVAO);
            glDrawElements(GL_TRIANGLES, QUAD_INDICES.length, GL_UNSIGNED_INT, 0);
            glBindVertexArray(0);
        }
    }

    public FontRenderer getFontRenderer() {
        return fontRenderer;
    }

    public void cleanup() {
        if (uiShader != null) uiShader.cleanup();
        if (fontRenderer != null) fontRenderer.cleanup();
        crosshairRenderer.cleanup();
        blockRenderer.cleanup();
        glDeleteVertexArrays(quadVAO);
        glDeleteBuffers(quadVBO);
        glDeleteBuffers(quadEBO);
    }
}
