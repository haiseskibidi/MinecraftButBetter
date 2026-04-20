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
    private static final float FADE_SPEED = 8.0f;
    
    private InteractionRule currentRule = null;
    private String currentBlockName = "";
    private float currentProgress = -1.0f;
    private Identifier lastBlockId = null;

    public InteractionRenderer(UIRenderer renderer) {
        this.renderer = renderer;
    }

    public void updateAndRender(RaycastResult hit, int sw, int sh) {
        float dt = GameLoop.getInstance().getTimer().getDeltaF();
        boolean hasInfo = false;

        if (hit != null && hit.isHit()) {
            com.za.zenith.world.blocks.Block block = GameLoop.getInstance().getWorld().getBlock(hit.getBlockPos());
            com.za.zenith.world.blocks.BlockDefinition def = BlockRegistry.getBlock(block.getType());
            com.za.zenith.world.blocks.entity.BlockEntity be = GameLoop.getInstance().getWorld().getBlockEntity(hit.getBlockPos());
            com.za.zenith.world.items.ItemStack held = GameLoop.getInstance().getPlayer().getInventory().getSelectedItemStack();

            currentRule = InteractionManager.getBestRule(GameLoop.getInstance().getPlayer(), def.getIdentifier(), be, held);
            
            if (currentRule != null || be instanceof BlockInfoProvider) {
                hasInfo = true;
                currentBlockName = def.getName();
                
                if (be instanceof BlockInfoProvider provider) {
                    currentProgress = provider.getInteractionProgress();
                } else {
                    currentProgress = -1.0f;
                }
                
                lastBlockId = def.getIdentifier();
            }
        }

        // Fade logic
        if (hasInfo) alpha = Math.min(1.0f, alpha + dt * FADE_SPEED);
        else alpha = Math.max(0.0f, alpha - dt * FADE_SPEED * 0.5f);

        if (alpha > 0.01f) {
            renderPlate(sw, sh);
        }
    }

    private void renderPlate(int sw, int sh) {
        int centerX = sw / 2;
        int centerY = sh / 2 - 45; // Position above crosshair
        
        String hintText = "";
        if (currentRule != null) {
            StringBuilder sb = new String(I18n.get(currentRule.hint())).contains("[") ? new StringBuilder() : new StringBuilder("[");
            
            if (!sb.isEmpty()) {
                if (currentRule.sneak() != null && currentRule.sneak()) sb.append("Shift + ");
                String btn = (currentRule.button() != null) ? currentRule.button() : "RMB";
                sb.append(btn.equals("RMB") ? "ПКМ" : "ЛКМ").append("] ");
            }
            
            hintText = sb.toString() + I18n.get(currentRule.hint());
        }

        int nameSize = 12;
        int hintSize = 10;
        int nameW = renderer.getFontRenderer().getStringWidth(currentBlockName, nameSize);
        int hintW = renderer.getFontRenderer().getStringWidth(hintText, hintSize);
        
        int width = Math.max(140, Math.max(nameW, hintW) + 24);
        int height = (currentProgress >= 0) ? 45 : 34;
        
        // --- 1. Background (SDF Plate) ---
        renderer.getPrimitivesRenderer().renderRect(centerX - width/2, centerY - height/2, width, height, sw, sh, 0.05f, 0.05f, 0.05f, 0.6f * alpha);
        
        // --- 2. Title (Block Name) ---
        renderer.getFontRenderer().drawString(currentBlockName, centerX - nameW/2, centerY - height/2 + 6, nameSize, sw, sh, 1, 1, 1, alpha);

        // --- 3. Hint ---
        if (!hintText.isEmpty()) {
            renderer.getFontRenderer().drawString(hintText, centerX - hintW/2, centerY - height/2 + 20, hintSize, sw, sh, 0.8f, 0.8f, 0.8f, alpha);
        }

        // --- 4. Progress Bar ---
        if (currentProgress >= 0) {
            int barW = width - 20;
            int barH = 3;
            int bx = centerX - barW/2;
            int by = centerY + height/2 - 8;
            
            renderer.getPrimitivesRenderer().renderRect(bx, by, barW, barH, sw, sh, 0.2f, 0.2f, 0.2f, 0.4f * alpha);
            renderer.getPrimitivesRenderer().renderRect(bx, by, (int)(barW * currentProgress), barH, sw, sh, 1.0f, 1.0f, 1.0f, 0.9f * alpha);
        }
    }
}
