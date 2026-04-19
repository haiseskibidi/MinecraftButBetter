package com.za.zenith.engine.graphics.ui.renderers;

import com.za.zenith.engine.core.GameLoop;
import com.za.zenith.engine.graphics.Shader;
import com.za.zenith.engine.graphics.Texture;
import com.za.zenith.engine.graphics.ui.UIRenderer;
import com.za.zenith.engine.graphics.ui.UIAnimationManager;
import com.za.zenith.utils.Logger;
import com.za.zenith.world.items.Item;
import com.za.zenith.world.items.ItemStack;

import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.GL_TEXTURE0;
import static org.lwjgl.opengl.GL13.glActiveTexture;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

public class SlotRenderer {
    private final UIRenderer renderer;
    private final Map<Integer, Texture> itemTextures = new HashMap<>();

    public SlotRenderer(UIRenderer renderer) {
        this.renderer = renderer;
    }

    public void renderSlot(int x, int y, int size, ItemStack stack, String placeholder, int screenWidth, int screenHeight, com.za.zenith.engine.graphics.DynamicTextureAtlas atlas) {
        renderSlot(x, y, size, stack, placeholder, screenWidth, screenHeight, atlas, false, "static_slot_" + x + "_" + y, true);
    }

    public void renderSlot(int x, int y, int size, ItemStack stack, String placeholder, int screenWidth, int screenHeight, com.za.zenith.engine.graphics.DynamicTextureAtlas atlas, boolean isHovered, String animId, boolean drawBackground) {
        float delta = GameLoop.getInstance().getTimer().getDeltaF();
        float hoverProgress = UIAnimationManager.getHoverProgress(animId, isHovered, delta);
        Shader uiShader = renderer.getShader();

        if (drawBackground) {
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
            
            float bgBrightness = 0.35f; 
            uiShader.setUniform("tintColor", bgBrightness, bgBrightness, bgBrightness, 0.9f);
            
            glBindVertexArray(renderer.getQuadVAO());
            glDrawElements(GL_TRIANGLES, renderer.getQuadIndicesLength(), GL_UNSIGNED_INT, 0);
        }
        
        uiShader.setInt("isSlot", 0);

        if (stack != null) {
            float itemRotation = hoverProgress * 360.0f;
            float itemScaleMod = 1.0f + (float)Math.sin(System.currentTimeMillis() * 0.005f) * 0.05f * hoverProgress;
            
            renderItemIcon(stack.getItem(), x + 2, y + 2, (size - 4) * itemScaleMod, screenWidth, screenHeight, atlas, itemRotation, hoverProgress);
            
            if (stack.getCount() > 1) {
                String count = String.valueOf(stack.getCount());
                renderer.getFontRenderer().drawString(count, x + size - renderer.getFontRenderer().getStringWidth(count, 14), y + size - 14, 14, screenWidth, screenHeight);
            }
            
            if (stack.getItem().isTool()) {
                com.za.zenith.world.items.component.ToolComponent tool = stack.getItem().getComponent(com.za.zenith.world.items.component.ToolComponent.class);
                if (tool != null && tool.maxDurability() != -1) {
                    float dur = (float)stack.getDurability() / tool.maxDurability();
                    if (dur < 1.0f) {
                        renderDurabilityBar(x + 2, y + size - 4, size - 4, dur, screenWidth, screenHeight);
                    }
                }
            }
        } else if (placeholder != null && !placeholder.isEmpty()) {
            renderPlaceholder(x, y, size, placeholder, screenWidth, screenHeight);
        }
        glBindVertexArray(0);
    }

    private void renderItemIcon(Item item, float x, float y, float size, int screenWidth, int screenHeight, com.za.zenith.engine.graphics.DynamicTextureAtlas atlas, float rotation, float hoverProgress) {
        if (item.isBlock()) {
            renderer.getBlockRenderer().renderBlock(item, x, y, size, screenWidth, screenHeight, atlas, rotation);
            return;
        }

        Shader uiShader = renderer.getShader();
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
                drawFallbackIcon();
            }
        } else {
            drawFallbackIcon();
        }
        
        glBindVertexArray(renderer.getQuadVAO());
        glDrawElements(GL_TRIANGLES, renderer.getQuadIndicesLength(), GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }

    private void renderPlaceholder(float x, float y, float size, String placeholder, int screenWidth, int screenHeight) {
        float ghostSize = (size - 4) * 0.7f;
        float gX = x + (size - ghostSize) / 2.0f;
        float gY = y + (size - ghostSize) / 2.0f;
        
        Texture tex = null;
        try {
            String path = placeholder.contains("/") ? placeholder : "zenith/textures/item/" + placeholder + ".png";
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

            Shader uiShader = renderer.getShader();
            uiShader.use();
            glActiveTexture(GL_TEXTURE0);
            tex.bind();
            uiShader.setInt("useTexture", 1);
            uiShader.setInt("isGrayscale", 1);
            uiShader.setUniform("scale", gsX, gsY, 0.0f, 0.0f);
            uiShader.setUniform("position_offset", gpX, gpY, 0.0f, 0.0f);
            uiShader.setUniform("tintColor", 1.0f, 1.0f, 1.0f, 0.4f); 
            glBindVertexArray(renderer.getQuadVAO());
            glDrawElements(GL_TRIANGLES, renderer.getQuadIndicesLength(), GL_UNSIGNED_INT, 0);
            uiShader.setInt("isGrayscale", 0);
        }
    }

    private void renderDurabilityBar(int x, int y, int width, float progress, int screenWidth, int screenHeight) {
        Shader uiShader = renderer.getShader();
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
        glDrawElements(GL_TRIANGLES, renderer.getQuadIndicesLength(), GL_UNSIGNED_INT, 0);
        
        // Progress (green to red)
        float barWidth = scaleX * progress;
        uiShader.setUniform("scale", barWidth, scaleY, 0.0f, 0.0f);
        uiShader.setUniform("position_offset", posX - (scaleX - barWidth), posY, 0.0f, 0.0f);
        uiShader.setUniform("tintColor", 1.0f - progress, progress, 0.0f, 1.0f);
        glDrawElements(GL_TRIANGLES, renderer.getQuadIndicesLength(), GL_UNSIGNED_INT, 0);
    }

    private void drawFallbackIcon() {
        Shader uiShader = renderer.getShader();
        uiShader.setInt("useTexture", 0);
        uiShader.setUniform("tintColor", 1.0f, 0.0f, 1.0f, 0.8f);
        uiShader.setUniform("uvOffset", 0.0f, 0.0f, 0.0f, 0.0f);
        uiShader.setUniform("uvScale", 1.0f, 1.0f, 0.0f, 0.0f);
    }
}


