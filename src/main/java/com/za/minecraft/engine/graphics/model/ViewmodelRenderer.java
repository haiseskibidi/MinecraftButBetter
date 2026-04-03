package com.za.minecraft.engine.graphics.model;

import com.za.minecraft.engine.graphics.Shader;
import com.za.minecraft.engine.graphics.DynamicTextureAtlas;
import com.za.minecraft.engine.graphics.Mesh;
import com.za.minecraft.world.items.ItemStack;
import com.za.minecraft.world.items.ItemMeshGenerator;
import com.za.minecraft.world.chunks.ChunkMeshGenerator;
import com.za.minecraft.world.blocks.Block;
import org.joml.Matrix4f;

public class ViewmodelRenderer {
    private final HeldItemRenderer heldItemRenderer = new HeldItemRenderer();

    public void render(Viewmodel viewmodel, Shader shader, DynamicTextureAtlas atlas, com.za.minecraft.entities.Player player, ItemStack mainHand, ItemStack offHand, float handHeat, float itemHeat) {
        renderNode(viewmodel.root, shader, handHeat);
        
        // Main hand attachment
        ModelNode attachR = viewmodel.getNode("item_attachment_r");
        if (attachR != null && mainHand != null && mainHand.getItem() != null) {
            heldItemRenderer.render(attachR.globalMatrix, mainHand, shader, atlas, true, itemHeat);
        }
        
        // Off hand attachment
        ModelNode attachL = viewmodel.getNode("item_attachment_l");
        if (attachL != null && offHand != null && offHand.getItem() != null) {
            heldItemRenderer.render(attachL.globalMatrix, offHand, shader, atlas, false, 0.0f);
        }
    }

    private void renderNode(ModelNode node, Shader shader, float heat) {
        if (node.mesh != null) {
            shader.setMatrix4f("model", node.globalMatrix);
            
            float partWeight = 0.0f;
            if (node.name.contains("hand") || node.name.contains("finger")) {
                partWeight = 1.0f;
            } else if (node.name.contains("forearm")) {
                partWeight = 0.6f;
            } else if (node.name.contains("shoulder")) {
                partWeight = 0.3f;
            }

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
