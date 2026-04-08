package com.za.zenith.engine.graphics.model;

import com.za.zenith.engine.graphics.Shader;
import com.za.zenith.engine.graphics.DynamicTextureAtlas;
import com.za.zenith.engine.graphics.Mesh;
import com.za.zenith.world.items.ItemStack;
import org.joml.Matrix4f;

/**
 * Рендерер для отображения рук игрока и предметов в них.
 */
public class ViewmodelRenderer {
    private final HeldItemRenderer heldItemRenderer = new HeldItemRenderer();

    public HeldItemRenderer getHeldItemRenderer() {
        return heldItemRenderer;
    }

    public void render(Viewmodel viewmodel, Shader shader, DynamicTextureAtlas atlas, com.za.zenith.entities.Player player, ItemStack mainHand, ItemStack offHand, float handHeat, float itemHeat) {
        if (viewmodel == null) return;

        shader.use();
        atlas.bind();

        // 1. Отрисовка скелета рук
        renderNode(viewmodel.root, shader, handHeat);

        // 2. Отрисовка предметов в точках привязки
        ModelNode mainHandNode = findNode(viewmodel.root, "item_attachment_r");
        if (mainHandNode != null && mainHand != null) {
            heldItemRenderer.render(mainHandNode.globalMatrix, mainHand, shader, atlas, true, itemHeat);
        }

        ModelNode offHandNode = findNode(viewmodel.root, "item_attachment_l");
        if (offHandNode != null && offHand != null) {
            heldItemRenderer.render(offHandNode.globalMatrix, offHand, shader, atlas, false, itemHeat);
        }
    }

    private ModelNode findNode(ModelNode root, String name) {
        if (root.name.equals(name)) return root;
        for (ModelNode child : root.children) {
            ModelNode found = findNode(child, name);
            if (found != null) return found;
        }
        return null;
    }

    private void renderNode(ModelNode node, Shader shader, float heat) {
        if (node.mesh != null) {
            shader.setMatrix4f("model", node.globalMatrix);
            
            float partWeight = 0.0f;
            if (node.name.contains("hand") || node.name.contains("finger")) partWeight = 1.0f;
            else if (node.name.contains("forearm")) partWeight = 0.6f;
            else if (node.name.contains("shoulder")) partWeight = 0.3f;

            shader.setBoolean("isHand", partWeight > 0.01f);
            shader.setFloat("uHandPartWeight", partWeight);
            shader.setFloat("uMiningHeat", heat);
            
            node.mesh.render();
            shader.setBoolean("isHand", false);
        }
        for (ModelNode child : node.children) {
            renderNode(child, shader, heat);
        }
    }

    public void cleanup() {
        heldItemRenderer.cleanup();
    }
}


