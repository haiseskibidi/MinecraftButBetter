package com.za.zenith.engine.graphics.ui.interaction;

import com.za.zenith.engine.core.GameLoop;
import com.za.zenith.engine.graphics.ui.UIRenderer;
import com.za.zenith.utils.I18n;
import com.za.zenith.utils.Identifier;
import com.za.zenith.world.blocks.BlockRegistry;
import com.za.zenith.world.physics.RaycastResult;
import org.joml.Vector4f;

import static org.lwjgl.opengl.GL11.*;

/**
 * Рендерер контекстных подсказок взаимодействия.
 */
public class InteractionRenderer {
    private final UIRenderer renderer;
    private float alpha = 0.0f;
    private static final float FADE_IN_SPEED = 12.0f;
    private static final float FADE_OUT_SPEED = 6.0f;
    
    private InteractionRule lastRule = null;
    private String lastBlockName = "";
    private float lastProgress = -1.0f;
    
    private float lerpWidth = 140;
    private float lerpHeight = 34;
    private float targetAlpha = 0;
    private float visibilityTimer = 0;

    public InteractionRenderer(UIRenderer renderer) {
        this.renderer = renderer;
    }

    public void updateAndRender(RaycastResult hit, int sw, int sh) {
        float dt = GameLoop.getInstance().getTimer().getDeltaF();
        boolean hasTarget = false;

        if (hit != null && hit.isHit()) {
            com.za.zenith.world.blocks.Block block = GameLoop.getInstance().getWorld().getBlock(hit.getBlockPos());
            com.za.zenith.world.blocks.BlockDefinition def = BlockRegistry.getBlock(block.getType());
            com.za.zenith.world.blocks.entity.BlockEntity be = GameLoop.getInstance().getWorld().getBlockEntity(hit.getBlockPos());
            com.za.zenith.world.items.ItemStack held = GameLoop.getInstance().getPlayer().getInventory().getSelectedItemStack();

            // Calculate local hit coordinates (0.0 to 1.0)
            org.joml.Vector3f localHit = new org.joml.Vector3f(
                hit.getHitPoint().x - hit.getBlockPos().x(),
                hit.getHitPoint().y - hit.getBlockPos().y(),
                hit.getHitPoint().z - hit.getBlockPos().z()
            );

            InteractionRule rule = InteractionManager.getBestRule(GameLoop.getInstance().getPlayer(), def.getIdentifier(), be, held, hit.getSide(), localHit);
            
            if (rule != null) {
                hasTarget = true;
                lastRule = rule;
                lastBlockName = def.getName();
                lastProgress = (be instanceof BlockInfoProvider provider) ? provider.getInteractionProgress() : -1.0f;
                visibilityTimer = 0.15f; 
            }
        }

        if (visibilityTimer > 0) {
            visibilityTimer -= dt;
            targetAlpha = 1.0f;
        } else {
            targetAlpha = hasTarget ? 1.0f : 0.0f;
        }

        // Smooth Alpha
        if (alpha < targetAlpha) alpha = Math.min(targetAlpha, alpha + dt * FADE_IN_SPEED);
        else alpha = Math.max(targetAlpha, alpha - dt * FADE_OUT_SPEED);

        if (alpha > 0.001f) {
            renderPlate(sw, sh, dt);
        }
    }

    private void renderPlate(int sw, int sh, float dt) {
        int centerX = sw / 2;
        int centerY = sh / 2 - 60; // Slightly higher to fit 2 lines
        
        String hintText = "";
        if (lastRule != null) {
            StringBuilder sb = new StringBuilder();
            if (lastRule.sneak() != null && lastRule.sneak()) sb.append("Shift + ");
            String btn = (lastRule.button() != null) ? lastRule.button() : "RMB";
            sb.append(btn.equals("RMB") ? "ПКМ" : "ЛКМ");
            
            hintText = "[" + sb.toString() + "] " + I18n.get(lastRule.hint());
        }

        // Apply Markdown header formatting to the block name
        String headerName = "$b$l" + lastBlockName.toUpperCase();
        
        int fontSize = 10;
        int nameW = renderer.getFontRenderer().getStringWidth(headerName, fontSize);
        int hintW = renderer.getFontRenderer().getStringWidth(hintText, fontSize);
        
        float targetW = Math.max(120, Math.max(nameW, hintW) + 24);
        float targetH = (lastProgress >= 0) ? 46 : 36;

        // Smooth size transition
        lerpWidth += (targetW - lerpWidth) * Math.min(1.0f, dt * 15.0f);
        lerpHeight += (targetH - lerpHeight) * Math.min(1.0f, dt * 15.0f);
        
        // --- 1. Background ---
        renderer.getPrimitivesRenderer().renderRect(centerX - (int)lerpWidth/2, centerY - (int)lerpHeight/2, (int)lerpWidth, (int)lerpHeight, sw, sh, 0.02f, 0.02f, 0.02f, 0.75f * alpha);
        
        // --- 2. Title (Markdown-styled Header) ---
        renderer.getFontRenderer().drawString(headerName, centerX - nameW/2, centerY - (int)lerpHeight/2 + 6, fontSize, sw, sh, 1, 1, 1, alpha);
        
        // --- 3. Hint (Second line) ---
        if (!hintText.isEmpty()) {
            renderer.getFontRenderer().drawString(hintText, centerX - hintW/2, centerY - (int)lerpHeight/2 + 20, fontSize, sw, sh, 0.85f, 0.85f, 0.85f, alpha);
        }

        // --- 4. Progress Bar ---
        if (lastProgress >= 0) {
            int barW = (int)lerpWidth - 24;
            int barH = 3;
            int bx = centerX - barW/2;
            int by = centerY + (int)lerpHeight/2 - 10;
            
            renderer.getPrimitivesRenderer().renderRect(bx, by, barW, barH, sw, sh, 0.1f, 0.1f, 0.1f, 0.5f * alpha);
            renderer.getPrimitivesRenderer().renderRect(bx, by, (int)(barW * lastProgress), barH, sw, sh, 1.0f, 1.0f, 1.0f, 0.9f * alpha);
        }
    }
}
