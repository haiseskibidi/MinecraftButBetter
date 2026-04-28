package com.za.zenith.engine.graphics.model;

import com.za.zenith.engine.graphics.DynamicTextureAtlas;
import com.za.zenith.engine.graphics.Mesh;
import com.za.zenith.engine.graphics.Shader;
import com.za.zenith.world.blocks.Block;
import com.za.zenith.world.chunks.ChunkMeshGenerator;
import com.za.zenith.world.items.Item;
import com.za.zenith.world.items.ItemStack;
import org.joml.Matrix4f;

import java.util.HashMap;
import java.util.Map;

/**
 * Рендерер мешей предметов. Теперь только отрисовывает по готовой матрице.
 */
public class HeldItemRenderer {
    private final Map<Integer, Mesh> itemMeshCache = new HashMap<>();

    public void render(Matrix4f modelMatrix, ItemStack stack, Shader shader, DynamicTextureAtlas atlas, float heat) {
        if (stack == null || stack.getItem() == null) return;
        
        Item item = stack.getItem();
        Mesh mesh = getOrGenerateMesh(item, atlas);
        
        if (mesh != null) {
            shader.setBoolean("isHand", false);
            shader.setFloat("uMiningHeat", heat);
            shader.setMatrix4f("model", modelMatrix);
            mesh.render(shader);
        }
    }

    public Mesh getOrGenerateMesh(Item item, DynamicTextureAtlas atlas) {
        Mesh mesh = itemMeshCache.get(item.getId());
        if (mesh == null) {
            if (item.isBlock()) {
                mesh = ChunkMeshGenerator.generateSingleBlockMesh(new Block(item.getId()), atlas, null, null);
            } else {
                mesh = com.za.zenith.world.items.ItemMeshGenerator.generateItemMesh(
                    item.getTexturePath(), atlas, item.getId()
                );
            }
            if (mesh != null) {
                itemMeshCache.put(item.getId(), mesh);
                item.setVisualBounds(mesh.getMin(), mesh.getMax());
                float gw = mesh.getGripWidth(mesh.getGraspOffset().y, 0.1f);
                if (gw > 0.001f) {
                    item.setGripWidth(gw);
                } else {
                    item.setGripWidth(mesh.getMax().x - mesh.getMin().x);
                }
            }
        }
        return mesh;
    }

    public void cleanup() {
        for (Mesh mesh : itemMeshCache.values()) {
            if (mesh != null) mesh.cleanup();
        }
        itemMeshCache.clear();
    }
}
