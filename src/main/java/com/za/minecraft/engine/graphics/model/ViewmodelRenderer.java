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
    private Mesh heldItemMeshR;
    private Mesh heldItemMeshL;
    private int lastHeldTypeIdR = -1;
    private int lastHeldTypeIdL = -1;

    public void render(Viewmodel viewmodel, Shader shader, DynamicTextureAtlas atlas, ItemStack mainHand, ItemStack offHand) {
        renderNode(viewmodel.root, shader);
        
        // Main hand attachment
        ModelNode attachR = viewmodel.getNode("item_attachment_r");
        if (attachR != null && mainHand != null && mainHand.getItem() != null) {
            renderHeldItem(attachR.globalMatrix, mainHand, shader, atlas, true);
        }
        
        // Off hand attachment
        ModelNode attachL = viewmodel.getNode("item_attachment_l");
        if (attachL != null && offHand != null && offHand.getItem() != null) {
            renderHeldItem(attachL.globalMatrix, offHand, shader, atlas, false);
        }
    }

    private void renderNode(ModelNode node, Shader shader) {
        if (node.mesh != null) {
            shader.setMatrix4f("model", node.globalMatrix);
            node.mesh.render();
        }
        for (ModelNode child : node.children) {
            renderNode(child, shader);
        }
    }

    private void renderHeldItem(Matrix4f parentMatrix, ItemStack stack, Shader shader, DynamicTextureAtlas atlas, boolean isMainHand) {
        com.za.minecraft.world.items.Item item = stack.getItem();
        int lastId = isMainHand ? lastHeldTypeIdR : lastHeldTypeIdL;
        
        if (lastId != item.getId()) {
            if (isMainHand) {
                if (heldItemMeshR != null) heldItemMeshR.cleanup();
                heldItemMeshR = createItemMesh(item, atlas);
                lastHeldTypeIdR = item.getId();
            } else {
                if (heldItemMeshL != null) heldItemMeshL.cleanup();
                heldItemMeshL = createItemMesh(item, atlas);
                lastHeldTypeIdL = item.getId();
            }
        }

        Mesh mesh = isMainHand ? heldItemMeshR : heldItemMeshL;
        if (mesh != null) {
            com.za.minecraft.world.items.Item.ViewmodelTransform t = item.getViewmodelTransform();
            
            Matrix4f modelMatrix = new Matrix4f(parentMatrix);
            
            // Reverted to stable relative offset logic
            float palmX = isMainHand ? 0.3125f : -0.3125f;
            
            modelMatrix.translate(t.px - palmX, t.py, t.pz) 
                       .rotateX((float)Math.toRadians(t.rx))
                       .rotateY((float)Math.toRadians(t.ry))
                       .rotateZ((float)Math.toRadians(t.rz))
                       .scale(t.scale);
            
            shader.setMatrix4f("model", modelMatrix);
            mesh.render();
        }
    }

    private Mesh createItemMesh(com.za.minecraft.world.items.Item item, DynamicTextureAtlas atlas) {
        if (item.isBlock()) return ChunkMeshGenerator.generateSingleBlockMesh(new Block(item.getId()), atlas);
        return ItemMeshGenerator.generateItemMesh(item.getTexturePath(), atlas, item.getId());
    }

    public void cleanup() {
        if (heldItemMeshR != null) heldItemMeshR.cleanup();
        if (heldItemMeshL != null) heldItemMeshL.cleanup();
    }
}
