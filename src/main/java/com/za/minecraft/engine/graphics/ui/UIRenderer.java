package com.za.minecraft.engine.graphics.ui;

import com.za.minecraft.engine.graphics.Shader;
import com.za.minecraft.engine.graphics.Texture;
import com.za.minecraft.utils.Logger;
import com.za.minecraft.world.items.Item;
import com.za.minecraft.world.items.ItemStack;
import com.za.minecraft.world.items.ToolItem;
import com.za.minecraft.world.blocks.Block;
import com.za.minecraft.world.blocks.BlockTextureMapper;
import com.za.minecraft.world.blocks.BlockRegistry;
import com.za.minecraft.world.items.ItemRegistry;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class UIRenderer {
    private Shader uiShader;
    private Texture crosshairTexture;
    private Texture hotbarTexture;
    private Texture hotbarSelectionTexture;
    private Map<Byte, Texture> itemTextures = new HashMap<>();
    
    private int quadVAO;
    private int quadVBO;
    private int quadEBO;
    private Hotbar hotbar;
    private PauseMenu pauseMenu;
    private FontRenderer fontRenderer;
    
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
        
        crosshairTexture = new Texture("src/main/resources/textures/crosshair.png", false, false);
        hotbarTexture = new Texture("src/main/resources/textures/gui/hotbar_slots.png", false, false);
        hotbarSelectionTexture = new Texture("src/main/resources/textures/gui/hotbar_selection.png", false, false);
        
        createQuad();
        
        fontRenderer = new FontRenderer();
        fontRenderer.init(uiShader);
        
        uiShader.use();
        uiShader.setInt("textureSampler", 0);
        
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
    
    public void renderCrosshair(int screenWidth, int screenHeight) {
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        uiShader.use();
        uiShader.setInt("useTexture", 1);
        uiShader.setUniform("tintColor", 1.0f, 1.0f, 1.0f, 1.0f);
        
        float crosshairSize = 16.0f;
        float scaleX = crosshairSize / screenWidth;
        float scaleY = crosshairSize / screenHeight;
        
        uiShader.setUniform("scale", scaleX, scaleY, 0.0f, 0.0f);
        uiShader.setUniform("position_offset", 0.0f, 0.0f, 0.0f, 0.0f);
        uiShader.setUniform("uvOffset", 0.0f, 0.0f, 0.0f, 0.0f);
        uiShader.setUniform("uvScale", 1.0f, 1.0f, 0.0f, 0.0f);
        
        crosshairTexture.bind();
        
        glBindVertexArray(quadVAO);
        glDrawElements(GL_TRIANGLES, QUAD_INDICES.length, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
        
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }
    
    public void renderHotbar(int screenWidth, int screenHeight, com.za.minecraft.engine.graphics.DynamicTextureAtlas atlas) {
        if (hotbar == null) return;
        
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        renderHotbarBackground(screenWidth, screenHeight);
        renderHotbarSelection(screenWidth, screenHeight);
        renderHotbarItems(screenWidth, screenHeight, atlas);
        
        // Отрисовка названия выбранного предмета
        ItemStack selected = hotbar.getSelectedItemStack();
        if (selected != null) {
            String name = selected.getItem().getName();
            int nameSize = 20;
            int textWidth = fontRenderer.getStringWidth(name, nameSize);
            int x = (screenWidth - textWidth) / 2;
            int y = hotbar.getScreenY(screenHeight) - 30; // Над хотбаром
            fontRenderer.drawString(name, x, y, nameSize, screenWidth, screenHeight);
        }

        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }
    
    public void renderMiningProgress(int screenWidth, int screenHeight, float progress) {
        if (progress <= 0.0f) return;

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        String text = String.format("Breaking: %d%%", (int)(progress * 100));
        int textSize = 18;
        int textWidth = fontRenderer.getStringWidth(text, textSize);
        int x = (screenWidth - textWidth) / 2;
        int y = (screenHeight / 2) + 30; // Чуть ниже прицела

        fontRenderer.drawString(text, x, y, textSize, screenWidth, screenHeight);

        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }

    public void renderHunger(int screenWidth, int screenHeight, float hunger) {
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        String text = String.format("Hunger: %.1f/20", hunger);
        int textSize = 18;
        int textWidth = fontRenderer.getStringWidth(text, textSize);
        
        // Позиция справа от хотбара
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

        String text = String.format("Noise: %d%%", (int)(noise * 100));
        int textSize = 18;
        int textWidth = fontRenderer.getStringWidth(text, textSize);
        
        // Позиция слева от хотбара
        int x = (screenWidth - (int)(Hotbar.HOTBAR_WIDTH * Hotbar.HOTBAR_SCALE)) / 2 - textWidth - 20;
        int y = hotbar.getScreenY(screenHeight) + 10;

        // Цвет текста меняется от белого к красному в зависимости от шума
        float r = 1.0f;
        float g = 1.0f - noise;
        float b = 1.0f - noise;
        uiShader.setUniform("tintColor", r, g, b, 1.0f);

        fontRenderer.drawString(text, x, y, textSize, screenWidth, screenHeight);
        uiShader.setUniform("tintColor", 1.0f, 1.0f, 1.0f, 1.0f);

        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }
    
    private void renderHotbarItems(int screenWidth, int screenHeight, com.za.minecraft.engine.graphics.DynamicTextureAtlas atlas) {
        float slotSizePx = 16.0f * Hotbar.HOTBAR_SCALE;
        
        for (int i = 0; i < Hotbar.HOTBAR_SLOTS; i++) {
            ItemStack stack = hotbar.getStackInSlot(i);
            if (stack == null) continue;
            
            Item item = stack.getItem();
            int slotX = hotbar.getSlotScreenX(screenWidth, i);
            int slotY = hotbar.getSlotScreenY(screenHeight);
            
            renderItemIcon(item, slotX, slotY, slotSizePx, screenWidth, screenHeight, atlas);
        }
    }

    private void renderItemIcon(Item item, int x, int y, float size, int screenWidth, int screenHeight, com.za.minecraft.engine.graphics.DynamicTextureAtlas atlas) {
        float scaleX = size / screenWidth;
        float scaleY = size / screenHeight;
        float posX = (2.0f * x / screenWidth) - 1.0f + scaleX;
        float posY = 1.0f - (2.0f * y / screenHeight) - scaleY;
        
        uiShader.use();
        uiShader.setUniform("scale", scaleX, scaleY, 0.0f, 0.0f);
        uiShader.setUniform("position_offset", posX, posY, 0.0f, 0.0f);
        uiShader.setUniform("tintColor", 1.0f, 1.0f, 1.0f, 1.0f);

        if (item.isBlock()) {
            // Это блок, берем из атласа
            atlas.bind();
            uiShader.setInt("useTexture", 1);
            float[] uv = BlockTextureMapper.uvFor(new Block(item.getId()), 0, atlas);
            float uMin = Math.min(uv[0], uv[4]);
            float vMin = Math.min(uv[1], uv[5]);
            float uMax = Math.max(uv[0], uv[4]);
            float vMax = Math.max(uv[1], uv[5]);
            uiShader.setUniform("uvOffset", uMin, vMin, 0.0f, 0.0f);
            uiShader.setUniform("uvScale", uMax - uMin, vMax - vMin, 0.0f, 0.0f);
        } else {
            // Это предмет (инструмент или еда)
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
        }
        
        glBindVertexArray(quadVAO);
        glDrawElements(GL_TRIANGLES, QUAD_INDICES.length, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    private void drawFallbackIcon(Item item) {
        uiShader.setInt("useTexture", 0);
        if (item.isTool()) {
            ToolItem tool = (ToolItem) item;
            switch (tool.getToolType()) {
                case KNIFE: uiShader.setUniform("tintColor", 0.7f, 0.7f, 0.7f, 1.0f); break;
                case PICKAXE: uiShader.setUniform("tintColor", 0.5f, 0.3f, 0.1f, 1.0f); break;
                case CROWBAR: uiShader.setUniform("tintColor", 0.2f, 0.6f, 0.8f, 1.0f); break;
                default: uiShader.setUniform("tintColor", 1.0f, 1.0f, 1.0f, 1.0f);
            }
        } else if (item.isFood()) {
            uiShader.setUniform("tintColor", 1.0f, 0.5f, 0.5f, 1.0f); // Розоватый для еды
        } else {
            uiShader.setUniform("tintColor", 1.0f, 1.0f, 1.0f, 1.0f);
        }
        uiShader.setUniform("uvOffset", 0.0f, 0.0f, 0.0f, 0.0f);
        uiShader.setUniform("uvScale", 1.0f, 1.0f, 0.0f, 0.0f);
    }
    
    public void renderInventory(int screenWidth, int screenHeight, com.za.minecraft.engine.graphics.DynamicTextureAtlas atlas) {
        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        renderDarkenedBackground();
        
        int padding = 50;
        int slotSize = 40;
        int spacing = 10;
        int columns = (screenWidth - padding * 2) / (slotSize + spacing);
        
        var allItems = ItemRegistry.getAllItems();
        int index = 0;
        
        for (var entry : allItems.entrySet()) {
            Item item = entry.getValue();
            if (item.getId() == 0 && !item.isTool()) continue; // Skip air
            
            int col = index % columns;
            int row = index / columns;
            
            int x = padding + col * (slotSize + spacing);
            int y = padding + row * (slotSize + spacing);
            
            renderItemIcon(item, x + slotSize/2, y + slotSize/2, slotSize/2.0f, screenWidth, screenHeight, atlas);
            index++;
        }
        
        renderHotbar(screenWidth, screenHeight, atlas);
        
        glEnable(GL_DEPTH_TEST);
        glDisable(GL_BLEND);
    }

    private void renderHotbarBackground(int screenWidth, int screenHeight) {
        hotbarTexture.bind();
        int hotbarX = hotbar.getScreenX(screenWidth);
        int hotbarY = hotbar.getScreenY(screenHeight);
        float hotbarWidth = Hotbar.HOTBAR_WIDTH * Hotbar.HOTBAR_SCALE;
        float hotbarHeight = Hotbar.HOTBAR_HEIGHT * Hotbar.HOTBAR_SCALE;
        float scaleX = hotbarWidth / screenWidth;
        float scaleY = hotbarHeight / screenHeight;
        float posX = (2.0f * hotbarX / screenWidth) - 1.0f + scaleX;
        float posY = 1.0f - (2.0f * hotbarY / screenHeight) - scaleY;
        uiShader.use();
        uiShader.setInt("useTexture", 1);
        uiShader.setUniform("scale", scaleX, scaleY, 0.0f, 0.0f);
        uiShader.setUniform("position_offset", posX, posY, 0.0f, 0.0f);
        uiShader.setUniform("uvOffset", 0.0f, 0.0f, 0.0f, 0.0f);
        uiShader.setUniform("uvScale", 1.0f, 1.0f, 0.0f, 0.0f);
        glBindVertexArray(quadVAO);
        glDrawElements(GL_TRIANGLES, QUAD_INDICES.length, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }
    
    private void renderHotbarSelection(int screenWidth, int screenHeight) {
        hotbarSelectionTexture.bind();
        int selectionX = hotbar.getSelectionScreenX(screenWidth);
        int selectionY = hotbar.getSelectionScreenY(screenHeight);
        float selectionWidth = Hotbar.HOTBAR_SELECTION_WIDTH * Hotbar.HOTBAR_SCALE;
        float selectionHeight = Hotbar.HOTBAR_SELECTION_HEIGHT * Hotbar.HOTBAR_SCALE;
        float scaleX = selectionWidth / screenWidth;
        float scaleY = selectionHeight / screenHeight;
        float posX = (2.0f * selectionX / screenWidth) - 1.0f + scaleX;
        float posY = 1.0f - (2.0f * selectionY / screenHeight) - scaleY;
        uiShader.setUniform("scale", scaleX, scaleY, 0.0f, 0.0f);
        uiShader.setUniform("position_offset", posX, posY, 0.0f, 0.0f);
        uiShader.setUniform("uvOffset", 0.0f, 0.0f, 0.0f, 0.0f);
        uiShader.setUniform("uvScale", 1.0f, 1.0f, 0.0f, 0.0f);
        glBindVertexArray(quadVAO);
        glDrawElements(GL_TRIANGLES, QUAD_INDICES.length, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    private void renderDarkenedBackground() {
        uiShader.use();
        uiShader.setInt("useTexture", 0);
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
        renderButton(centerX, centerY - spacing, buttonWidth, buttonHeight, screenWidth, screenHeight, null, 0.4f, 0.7f, 1.0f);
        renderButton(centerX, centerY + spacing, buttonWidth, buttonHeight, screenWidth, screenHeight, null, 1.0f, 0.4f, 0.4f);
    }
    
    private void renderButton(int x, int y, int width, int height, int screenWidth, int screenHeight, String text, float r, float g, float b) {
        uiShader.use();
        uiShader.setInt("useTexture", 0);
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
        
        if (text != null && !text.isEmpty()) {
            int textSize = 16;
            int textWidth = fontRenderer.getStringWidth(text, textSize);
            int textX = x - textWidth / 2;
            int textY = y - textSize / 2;
            fontRenderer.drawString(text, textX, textY, textSize, screenWidth, screenHeight);
        }
    }

    public void cleanup() {
        if (uiShader != null) uiShader.cleanup();
        if (crosshairTexture != null) crosshairTexture.cleanup();
        if (hotbarTexture != null) hotbarTexture.cleanup();
        if (hotbarSelectionTexture != null) hotbarSelectionTexture.cleanup();
        if (fontRenderer != null) fontRenderer.cleanup();
        glDeleteVertexArrays(quadVAO);
        glDeleteBuffers(quadVBO);
        glDeleteBuffers(quadEBO);
    }
}
