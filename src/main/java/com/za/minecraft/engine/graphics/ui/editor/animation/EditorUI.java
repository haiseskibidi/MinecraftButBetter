package com.za.minecraft.engine.graphics.ui.editor.animation;

import com.za.minecraft.engine.graphics.DynamicTextureAtlas;
import com.za.minecraft.engine.graphics.ui.Hotbar;
import com.za.minecraft.engine.graphics.ui.UIRenderer;
import com.za.minecraft.world.items.Item;
import com.za.minecraft.world.items.ItemRegistry;
import com.za.minecraft.engine.graphics.model.ModelNode;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Renders the 2D interface for the Animation Studio.
 * Modularized to separate UI logic from state.
 */
public class EditorUI {
    
    public void render(UIRenderer renderer, AnimationEditorState state, int sw, int sh, DynamicTextureAtlas atlas, List<ModelNode> allParts) {
        renderer.getFontRenderer().drawString("ANIMATION STUDIO (MODULAR)", 20, 20, 24, sw, sh, 1.0f, 0.7f, 0.0f, 1.0f);
        
        // 1. Dev Panel (Right)
        int slotSize = (int)(18 * Hotbar.HOTBAR_SCALE);
        int spacing = (int)(2 * Hotbar.HOTBAR_SCALE);
        int devX = sw - (7 * (slotSize + spacing)) - 25;
        renderer.renderDeveloperPanel(devX, 40, slotSize, spacing, sw, sh, atlas);

        if (state.heldStack != null) {
            int cols = 7;
            float offset = renderer.getDevScroller().getOffset();
            List<Item> items = new ArrayList<>(ItemRegistry.getAllItems().values());
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).getId() == state.heldStack.getItem().getId()) {
                    int x = devX + (i % cols) * (slotSize + spacing);
                    int y = 40 + (i / cols) * (slotSize + spacing) - (int)offset;
                    if (y + slotSize > 40 && y < 40 + renderer.getDevScroller().getHeight())
                        renderer.renderHighlight(x, y, slotSize, sw, sh, 0.0f, 1.0f, 0.0f, 0.4f);
                    break;
                }
            }
        }

        // 2. Parts List (Left)
        renderer.renderRect(10, 80, 220, sh - 200, sw, sh, 0.05f, 0.05f, 0.05f, 0.9f);
        renderer.getFontRenderer().drawString("PARTS", 20, 90, 18, sw, sh, 0.4f, 0.7f, 1.0f, 1.0f);
        
        int startY = 130;
        List<AnimationEditorState.FlatNode> flatList = state.getFlatPartList(allParts.isEmpty() ? null : findRoot(allParts));
        
        for (int i = 0; i < flatList.size(); i++) {
            AnimationEditorState.FlatNode fn = flatList.get(i);
            int itemY = startY + (i * 22);
            boolean sel = (fn.node() == state.selectedPart);
            
            if (sel) renderer.renderRect(15, itemY - 2, 210, 20, sw, sh, 0.2f, 0.4f, 0.8f, 1.0f);
            else if (i == state.hoveredPartIndex) renderer.renderRect(15, itemY - 2, 210, 20, sw, sh, 0.2f, 0.2f, 0.2f, 1.0f);
            
            String prefix = fn.depth() > 0 ? "  ".repeat(fn.depth()) + "└ " : "";
            renderer.getFontRenderer().drawString(prefix + fn.node().name, 25, itemY, 14, sw, sh, 1, 1, 1, 1);
        }

        // 3. Properties (Middle-Left)
        if (state.selectedPart != null) {
            int px = 240, py = 80;
            int panelH = 380;
            renderer.renderRect(px, py, 220, panelH, sw, sh, 0.05f, 0.05f, 0.05f, 0.9f);
            
            int curY = py + 10;
            renderer.getFontRenderer().drawString("PROPERTIES", px + 10, curY, 18, sw, sh, 1, 0.8f, 0, 1);
            curY += 25;
            renderer.getFontRenderer().drawString(state.selectedPart.name, px + 10, curY, 12, sw, sh, 0.6f, 0.6f, 0.6f, 1);
            
            boolean isCam = state.selectedPart.name.equals("Camera");
            
            curY += 35; // Start transforms
            String[] labels = isCam ? new String[]{"Cam X", "Cam Y", "FOV Off", "Cam Tilt", "Yaw (N/A)", "Cam Roll"} 
                                    : new String[]{"Anim X", "Anim Y", "Anim Z", "Pitch", "Yaw", "Roll"};
            
            float[] values = new float[6];
            values[0] = state.selectedPart.animTranslation.x;
            values[1] = state.selectedPart.animTranslation.y;
            values[2] = state.selectedPart.animTranslation.z;
            values[3] = (float)Math.toDegrees(state.selectedPart.animRotation.x);
            values[4] = (float)Math.toDegrees(state.selectedPart.animRotation.y);
            values[5] = (float)Math.toDegrees(state.selectedPart.animRotation.z);

            for (int i = 0; i < 6; i++) {
                boolean isEditing = labels[i].equals(state.editingProperty);
                drawTransformInfo(renderer, labels[i], isEditing ? state.editingValue : String.format("%.3f", values[i]), px + 15, curY, sw, sh, isEditing);
                curY += 22;
                if (i == 2) curY += 10; // Spacing after translation
            }
            
            curY += 15;
            AnimationEditorState.EditorTrack track = state.tracks.get(state.selectedPart.name);
            String easingStr = track != null ? track.easing.name() : "NONE";
            renderer.getFontRenderer().drawString("Easing: " + easingStr, px + 15, curY, 12, sw, sh, 1, 0.7f, 0.4f, 1);
            curY += 18;
            renderer.getFontRenderer().drawString("[Shift+1-4] Change Easing", px + 15, curY, 10, sw, sh, 0.5f, 0.5f, 0.5f, 1);
            
            renderer.getFontRenderer().drawString("[G] Move | [R] Rot | [K] Key", px + 15, py + panelH - 45, 12, sw, sh, 0.5f, 0.5f, 0.5f, 1);
            renderer.getFontRenderer().drawString("[Ctrl+S] Export JSON", px + 15, py + panelH - 25, 12, sw, sh, 0.4f, 1, 0.4f, 1);
        }

        // 4. Timeline
        renderer.renderRect(10, sh - 90, sw - 20, 80, sw, sh, 0.05f, 0.05f, 0.05f, 0.9f);
        int bx = 60, bw = sw - 120, by = sh - 45;
        renderer.renderRect(bx, by, bw, 10, sw, sh, 0.2f, 0.2f, 0.2f, 1);
        
        if (state.selectedPart != null) {
            AnimationEditorState.EditorTrack track = state.tracks.get(state.selectedPart.name);
            if (track != null) {
                for (AnimationEditorState.EditorKeyframe k : track.keyframes) {
                    float kx = bx + (k.time() * bw);
                    boolean isCurrent = Math.abs(k.time() - state.currentTime) < 0.01f;
                    float r = isCurrent ? 1.0f : 0.7f, g = isCurrent ? 0.8f : 0.7f, b = isCurrent ? 0.0f : 0.7f;
                    renderer.renderRect((int)kx - 4, by - 4, 8, 8, sw, sh, r, g, b, 1.0f);
                    renderer.renderRect((int)kx - 2, by - 6, 4, 12, sw, sh, r, g, b, 1.0f);
                }
            }
        }
        
        renderer.renderRect(bx + (int)(state.currentTime * bw) - 2, by - 15, 4, 40, sw, sh, 1, 0.2f, 0.2f, 1);
        renderer.getFontRenderer().drawString(String.format("%.2f", state.currentTime), bx + (int)(state.currentTime * bw) - 10, by + 30, 12, sw, sh, 1, 0.4f, 0.4f, 1);
        
        String status = state.isPlaying ? "PLAYING" : "PAUSED";
        renderer.getFontRenderer().drawString(status, 400, 25, 16, sw, sh, state.isPlaying ? 0.2f : 1.0f, state.isPlaying ? 1.0f : 0.2f, 0.2f, 1.0f);
    }

    private void drawTransformInfo(UIRenderer renderer, String label, String value, int x, int y, int sw, int sh, boolean isEditing) {
        renderer.getFontRenderer().drawString(label + ":", x, y, 12, sw, sh, 0.6f, 0.6f, 0.6f, 1);
        renderer.renderRect(x + 65, y - 2, 12, 14, sw, sh, 0.15f, 0.15f, 0.15f, 1.0f);
        renderer.getFontRenderer().drawString("-", x + 68, y, 12, sw, sh, 1, 1, 1, 1);
        
        if (isEditing) {
            renderer.renderRect(x + 80, y - 2, 80, 16, sw, sh, 0.2f, 0.3f, 0.5f, 1.0f);
            renderer.getFontRenderer().drawString(value + "_", x + 85, y, 12, sw, sh, 1, 1, 1, 1);
        } else {
            renderer.getFontRenderer().drawString(value, x + 85, y, 12, sw, sh, 1, 1, 1, 1);
        }
        
        renderer.renderRect(x + 165, y - 2, 12, 14, sw, sh, 0.15f, 0.15f, 0.15f, 1.0f);
        renderer.getFontRenderer().drawString("+", x + 168, y, 12, sw, sh, 1, 1, 1, 1);
    }

    private ModelNode findRoot(List<ModelNode> allParts) {
        for (ModelNode n : allParts) if (n.name.equals("root")) return n;
        return allParts.isEmpty() ? null : allParts.get(0);
    }
}