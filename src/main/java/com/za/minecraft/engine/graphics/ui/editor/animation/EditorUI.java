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
 */
public class EditorUI {
    
    public void render(UIRenderer renderer, AnimationEditorState state, int sw, int sh, DynamicTextureAtlas atlas, List<ModelNode> allParts) {
        renderer.getFontRenderer().drawString("ANIMATION STUDIO", 20, 20, 24, sw, sh, 1.0f, 0.7f, 0.0f, 1.0f);
        
        // 1. Dev Panel (Right)
        int slotSize = (int)(18 * Hotbar.HOTBAR_SCALE);
        int spacing = (int)(2 * Hotbar.HOTBAR_SCALE);
        int devX = sw - (7 * (slotSize + spacing)) - 25;
        renderer.renderDeveloperPanel(devX, 40, slotSize, spacing, sw, sh, atlas);

        // Highlight selected item
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
        List<RenderNode> flatList = new ArrayList<>();
        flattenTree(state.selectedPart, allParts.isEmpty() ? null : findRoot(allParts), 0, flatList);
        
        for (int i = 0; i < flatList.size(); i++) {
            RenderNode rn = flatList.get(i);
            int itemY = startY + (i * 22);
            boolean sel = (rn.node == state.selectedPart);
            
            if (sel) renderer.renderRect(15, itemY - 2, 210, 20, sw, sh, 0.2f, 0.4f, 0.8f, 1.0f);
            else if (i == state.hoveredPartIndex) renderer.renderRect(15, itemY - 2, 210, 20, sw, sh, 0.2f, 0.2f, 0.2f, 1.0f);
            
            String prefix = rn.depth > 0 ? "  ".repeat(rn.depth) + "└ " : "";
            renderer.getFontRenderer().drawString(prefix + rn.node.name, 25, itemY, 14, sw, sh, 1, 1, 1, 1);
        }

        // 3. Properties (Middle-Left)
        if (state.selectedPart != null) {
            int px = 240, py = 80;
            int panelH = 320;
            renderer.renderRect(px, py, 220, panelH, sw, sh, 0.05f, 0.05f, 0.05f, 0.9f);
            
            int curY = py + 10;
            renderer.getFontRenderer().drawString("PROPERTIES", px + 10, curY, 18, sw, sh, 1, 0.8f, 0, 1);
            curY += 25;
            renderer.getFontRenderer().drawString(state.selectedPart.name, px + 10, curY, 12, sw, sh, 0.6f, 0.6f, 0.6f, 1);
            
            curY += 35; // Start transforms
            drawTransformInfo(renderer, "Anim X", state.selectedPart.animTranslation.x, px + 15, curY, sw, sh); curY += 22;
            drawTransformInfo(renderer, "Anim Y", state.selectedPart.animTranslation.y, px + 15, curY, sw, sh); curY += 22;
            drawTransformInfo(renderer, "Anim Z", state.selectedPart.animTranslation.z, px + 15, curY, sw, sh); curY += 22;
            
            curY += 10; // Rotation group
            drawTransformInfo(renderer, "Pitch", (float)Math.toDegrees(state.selectedPart.animRotation.x), px + 15, curY, sw, sh); curY += 22;
            drawTransformInfo(renderer, "Yaw", (float)Math.toDegrees(state.selectedPart.animRotation.y), px + 15, curY, sw, sh); curY += 22;
            drawTransformInfo(renderer, "Roll", (float)Math.toDegrees(state.selectedPart.animRotation.z), px + 15, curY, sw, sh); curY += 22;
            
            curY += 15;
            AnimationEditorState.EditorTrack track = state.tracks.get(state.selectedPart.name);
            String easingStr = track != null ? track.easing.name() : "NONE";
            renderer.getFontRenderer().drawString("Easing: " + easingStr, px + 15, curY, 12, sw, sh, 1, 0.7f, 0.4f, 1);
            curY += 18;
            renderer.getFontRenderer().drawString("[Shift+1-4] Change Easing", px + 15, curY, 10, sw, sh, 0.5f, 0.5f, 0.5f, 1);

            // Stick hints to the bottom of the panel
            renderer.getFontRenderer().drawString("[G] Move | [R] Rot | [K] Key", px + 15, py + panelH - 45, 12, sw, sh, 0.5f, 0.5f, 0.5f, 1);
            renderer.getFontRenderer().drawString("[Ctrl+S] Export JSON", px + 15, py + panelH - 25, 12, sw, sh, 0.4f, 1, 0.4f, 1);
        }

        // 4. Timeline (Bottom)
        renderer.renderRect(10, sh - 90, sw - 20, 80, sw, sh, 0.05f, 0.05f, 0.05f, 0.9f);
        int bx = 60, bw = sw - 120, by = sh - 45;
        renderer.renderRect(bx, by, bw, 10, sw, sh, 0.2f, 0.2f, 0.2f, 1);
        
        // Render Keyframe Markers
        if (state.selectedPart != null) {
            AnimationEditorState.EditorTrack track = state.tracks.get(state.selectedPart.name);
            if (track != null) {
                for (AnimationEditorState.EditorKeyframe k : track.keyframes) {
                    float kx = bx + (k.time() * bw);
                    boolean isCurrent = Math.abs(k.time() - state.currentTime) < 0.01f;
                    
                    // Draw a diamond
                    float r = isCurrent ? 1.0f : 0.7f;
                    float g = isCurrent ? 0.8f : 0.7f;
                    float b = isCurrent ? 0.0f : 0.7f;
                    
                    renderer.renderRect((int)kx - 4, by - 4, 8, 8, sw, sh, r, g, b, 1.0f); // Diamond base
                    renderer.renderRect((int)kx - 2, by - 6, 4, 12, sw, sh, r, g, b, 1.0f); // Vertical stretch
                }
            }
        }
        
        // Playhead
        renderer.renderRect(bx + (int)(state.currentTime * bw) - 2, by - 15, 4, 40, sw, sh, 1, 0.2f, 0.2f, 1);
        renderer.getFontRenderer().drawString(String.format("%.2f", state.currentTime), bx + (int)(state.currentTime * bw) - 10, by + 30, 12, sw, sh, 1, 0.4f, 0.4f, 1);
        
        String status = state.isPlaying ? "PLAYING" : "PAUSED";
        // Move status further right to avoid "ANIMATION STUDIO" overlap
        renderer.getFontRenderer().drawString(status, 400, 25, 16, sw, sh, state.isPlaying ? 0.2f : 1.0f, state.isPlaying ? 1.0f : 0.2f, 0.2f, 1.0f);
    }

    private void drawTransformInfo(UIRenderer renderer, String label, float value, int x, int y, int sw, int sh) {
        renderer.getFontRenderer().drawString(label + ":", x, y, 12, sw, sh, 0.6f, 0.6f, 0.6f, 1);
        renderer.getFontRenderer().drawString(String.format("%.3f", value), x + 85, y, 12, sw, sh, 1, 1, 1, 1);
    }

    private static class RenderNode {
        ModelNode node;
        int depth;
        RenderNode(ModelNode node, int depth) { this.node = node; this.depth = depth; }
    }

    private ModelNode findRoot(List<ModelNode> allParts) {
        for (ModelNode node : allParts) {
            if (node.name.equals("root")) return node;
        }
        return allParts.get(0);
    }

    private void flattenTree(ModelNode selected, ModelNode node, int depth, List<RenderNode> out) {
        if (node == null) return;
        if (!node.name.equals("root")) {
            out.add(new RenderNode(node, depth - 1));
        }
        for (ModelNode child : node.children) {
            flattenTree(selected, child, depth + 1, out);
        }
    }
}
