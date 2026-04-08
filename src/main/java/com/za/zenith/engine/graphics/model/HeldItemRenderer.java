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
 * Специализированный рендерер для отрисовки предметов в руках игрока.
 * Обеспечивает правильное позиционирование, вращение и масштабирование для разных типов предметов.
 */
public class HeldItemRenderer {
    private final Map<Integer, Mesh> itemMeshCache = new HashMap<>();
    
    // Дефолтные трансформы для разных типов предметов
    private static final Item.ViewmodelTransform DEFAULT_ITEM_TRANSFORM = new Item.ViewmodelTransform(
        0.0f, -0.1f, 0.0f,      // В центре кости attachment
        0.0f, -90.0f, 0.0f,     // Разворачиваем плоскостью к игроку (компенсация поворота кости)
        0.85f
    );

    private static final Item.ViewmodelTransform DEFAULT_BLOCK_TRANSFORM = new Item.ViewmodelTransform(
        0.0f, 0.15f, 0.0f,      // Подняли выше над кистью (Y=0.15), убрали базовый Z
        30.0f, 15.0f, 0.0f,     // Честная изометрия
        0.4f
    );

    public void render(Matrix4f parentMatrix, ItemStack stack, Shader shader, DynamicTextureAtlas atlas, boolean isMainHand, float heat) {
        if (stack == null || stack.getItem() == null) return;
        
        Item item = stack.getItem();
        Mesh mesh = getOrGenerateMesh(item, atlas);
        
        if (mesh != null) {
            Matrix4f modelMatrix = new Matrix4f(parentMatrix);
            Item.ViewmodelTransform t = getTransform(item);

            float tx = t.px;
            float ty = t.py;
            float tz = t.pz;

            // Динамическая коррекция для блоков: прижимаем "заднюю" грань к кисти.
            if (item.isBlock() && t == DEFAULT_BLOCK_TRANSFORM) {
                org.joml.Vector3f max = mesh.getMax();
                tz -= max.z * t.scale;
            }

            modelMatrix.translate(tx, ty, tz)
                       .rotateX((float)Math.toRadians(t.rx))
                       .rotateY((float)Math.toRadians(t.ry))
                       .rotateZ((float)Math.toRadians(t.rz))
                       .scale(t.scale);
            
            if (!item.isBlock()) {
                org.joml.Vector3f go = mesh.getGraspOffset();
                modelMatrix.translate(-go.x, -go.y, -go.z);
            }

            shader.setBoolean("isHand", false);
            shader.setFloat("uMiningHeat", heat);
            shader.setMatrix4f("model", modelMatrix);
            mesh.render();
        }
    }

    private Item.ViewmodelTransform getTransform(Item item) {
        Item.ViewmodelTransform t = item.getViewmodelTransform();
        if (t != null && t != Item.DEFAULT_TRANSFORM_MARKER) return t;

        if (item.isBlock()) return DEFAULT_BLOCK_TRANSFORM;
        return DEFAULT_ITEM_TRANSFORM;
    }

    public Mesh getOrGenerateMesh(Item item, DynamicTextureAtlas atlas) {
        Mesh mesh = itemMeshCache.get(item.getId());
        if (mesh == null) {
            if (item.isBlock()) {
                mesh = ChunkMeshGenerator.generateSingleBlockMesh(new Block(item.getId()), atlas);
            } else {
                mesh = com.za.zenith.world.items.ItemMeshGenerator.generateItemMesh(
                    item.getTexturePath(), atlas, item.getId()
                );
            }
            if (mesh != null) itemMeshCache.put(item.getId(), mesh);
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


