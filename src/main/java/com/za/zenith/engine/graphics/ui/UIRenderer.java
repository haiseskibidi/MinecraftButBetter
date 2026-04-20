package com.za.zenith.engine.graphics.ui;

import com.za.zenith.engine.core.GameLoop;
import com.za.zenith.engine.graphics.Shader;
import com.za.zenith.engine.graphics.ui.crosshair.CrosshairManager;
import com.za.zenith.engine.graphics.ui.crosshair.CrosshairRenderer;
import com.za.zenith.engine.graphics.ui.crosshair.CrosshairRegistry;
import com.za.zenith.engine.graphics.ui.renderers.*;
import com.za.zenith.utils.Logger;
import org.lwjgl.system.MemoryUtil;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class UIRenderer {
    private Shader uiShader;
    private final CrosshairManager crosshairManager = new CrosshairManager();
    private final CrosshairRenderer crosshairRenderer = new CrosshairRenderer();
    
    private int quadVAO;
    private int quadVBO;
    private int quadEBO;
    private Hotbar hotbar;
    private PauseMenu pauseMenu;
    private FontRenderer fontRenderer;
    private final InventoryBlockRenderer blockRenderer = new InventoryBlockRenderer();
    
    // Sub-renderers
    private UIPrimitives primitivesRenderer;
    private SlotRenderer slotRenderer;
    private HUDRenderer hudRenderer;
    private InventoryScreenRenderer inventoryScreenRenderer;
    private MenuRenderer menuRenderer;
    private MinimapRenderer minimapRenderer;
    private com.za.zenith.engine.graphics.ui.interaction.InteractionRenderer interactionRenderer;
    private com.za.zenith.engine.graphics.ui.blueprints.BlueprintRenderer blueprintRenderer;
    
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
        
        primitivesRenderer = new UIPrimitives(this);
        slotRenderer = new SlotRenderer(this);
        hudRenderer = new HUDRenderer(this);
        inventoryScreenRenderer = new InventoryScreenRenderer(this);
        menuRenderer = new MenuRenderer(this);
        minimapRenderer = new MinimapRenderer(this);
        blueprintRenderer = new com.za.zenith.engine.graphics.ui.blueprints.BlueprintRenderer();
        interactionRenderer = new com.za.zenith.engine.graphics.ui.interaction.InteractionRenderer(this);
        com.za.zenith.engine.graphics.ui.interaction.InteractionManager.init();

        Logger.info("UI Renderer initialized (Modular)");
    }

    public com.za.zenith.engine.graphics.ui.interaction.InteractionRenderer getInteractionRenderer() {
        return interactionRenderer;
    }

    public MinimapRenderer getMinimapRenderer() { return minimapRenderer; }

    public com.za.zenith.engine.graphics.ui.blueprints.BlueprintRenderer getBlueprintRenderer() { return blueprintRenderer; }

    public void setHotbar(Hotbar hotbar) { this.hotbar = hotbar; }
    public void setPauseMenu(PauseMenu pauseMenu) { this.pauseMenu = pauseMenu; }
    
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

    // --- DELEGATED HUD RENDERING ---
    
    public void renderHotbar(int screenWidth, int screenHeight, com.za.zenith.engine.graphics.DynamicTextureAtlas atlas) {
        hudRenderer.renderHotbar(hotbar, screenWidth, screenHeight, atlas);
    }

    public void renderFiringProgress(int screenWidth, int screenHeight, float progress) {
        hudRenderer.renderFiringProgress(screenWidth, screenHeight, progress);
    }

    public void renderHUDOverlay(int screenWidth, int screenHeight) {
        hudRenderer.renderOverlay(hotbar, screenWidth, screenHeight);
    }

    public void renderLogo(int screenWidth, int screenHeight) {
        hudRenderer.renderLogo(screenWidth, screenHeight);
    }

    public void renderLootboxOpening(int sw, int sh) {
        hudRenderer.renderLootboxOpening(sw, sh);
    }

    // --- DELEGATED INVENTORY & SLOTS ---

    public void renderSlot(int x, int y, int size, com.za.zenith.world.items.ItemStack stack, String placeholder, int screenWidth, int screenHeight, com.za.zenith.engine.graphics.DynamicTextureAtlas atlas) {
        slotRenderer.renderSlot(x, y, size, stack, placeholder, screenWidth, screenHeight, atlas);
    }

    public void renderSlot(int x, int y, int size, com.za.zenith.world.items.ItemStack stack, String placeholder, int screenWidth, int screenHeight, com.za.zenith.engine.graphics.DynamicTextureAtlas atlas, boolean isHovered, String animId, boolean drawBackground) {
        slotRenderer.renderSlot(x, y, size, stack, placeholder, screenWidth, screenHeight, atlas, isHovered, animId, drawBackground);
    }

    public void renderInventory(int screenWidth, int screenHeight, com.za.zenith.engine.graphics.DynamicTextureAtlas atlas) {
        inventoryScreenRenderer.renderInventory(hotbar, screenWidth, screenHeight, atlas);
    }

    // --- DELEGATED MENUS ---

    public void renderPauseMenu(int screenWidth, int screenHeight) {
        menuRenderer.renderPauseMenu(pauseMenu, screenWidth, screenHeight);
    }

    // --- DELEGATED PRIMITIVES ---

    public void renderRect(int x, int y, int width, int height, int sw, int sh, float r, float g, float b, float a) {
        primitivesRenderer.renderRect(x, y, width, height, sw, sh, r, g, b, a);
    }

    public void renderGroupBackground(int x, int y, int width, int height, GUIConfig.BackgroundConfig bg) {
        int sw = GameLoop.getInstance().getWindow().getWidth();
        int sh = GameLoop.getInstance().getWindow().getHeight();
        int p = bg.padding;
        primitivesRenderer.renderRect(x - p, y - p, width + p * 2, height + p * 2, sw, sh, bg.color[0], bg.color[1], bg.color[2], bg.color[3]);
    }

    public void renderHighlight(int x, int y, int size, int sw, int sh, float r, float g, float b, float a) {
        primitivesRenderer.renderHighlight(x, y, size, sw, sh, r, g, b, a);
    }

    public void renderExternalImage(String path, int x, int y, int width, int height, int sw, int sh) {
        primitivesRenderer.renderExternalImage(path, x, y, width, height, sw, sh);
    }

    public void renderDeveloperPanel(int devX, int startY, int slotSize, int spacing, int sw, int sh, com.za.zenith.engine.graphics.DynamicTextureAtlas atlas) {
        inventoryScreenRenderer.renderDeveloperPanel(devX, startY, slotSize, spacing, sw, sh, atlas);
    }

    public ScrollPanel getDevScroller() {
        return inventoryScreenRenderer.getDevScroller();
    }

    // --- GETTERS FOR SUB-RENDERERS ---

    public Shader getShader() { return uiShader; }
    public int getQuadVAO() { return quadVAO; }
    public int getQuadIndicesLength() { return QUAD_INDICES.length; }
    public FontRenderer getFontRenderer() { return fontRenderer; }
    public InventoryBlockRenderer getBlockRenderer() { return blockRenderer; }
    
    public UIPrimitives getPrimitivesRenderer() { return primitivesRenderer; }
    public SlotRenderer getSlotRenderer() { return slotRenderer; }
    public HUDRenderer getHudRenderer() { return hudRenderer; }
    public InventoryScreenRenderer getInventoryScreenRenderer() { return inventoryScreenRenderer; }
    public MenuRenderer getMenuRenderer() { return menuRenderer; }

    public void cleanup() {
        if (uiShader != null) uiShader.cleanup();
        if (fontRenderer != null) fontRenderer.cleanup();
        if (minimapRenderer != null) minimapRenderer.cleanup();
        crosshairRenderer.cleanup();
        blockRenderer.cleanup();
        glDeleteVertexArrays(quadVAO);
        glDeleteBuffers(quadVBO);
        glDeleteBuffers(quadEBO);
    }
}


