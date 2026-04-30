package com.za.zenith.engine.graphics;

import com.za.zenith.world.blocks.Block;
import com.za.zenith.world.chunks.ChunkMeshGenerator;
import com.za.zenith.world.items.Item;
import com.za.zenith.entities.EntityDefinition;

import java.util.HashMap;
import java.util.Map;

/**
 * Unified cache for meshes to prevent redundant mesh generation 
 * across different render systems.
 */
public class MeshRegistry {
    private static final Map<Integer, Mesh> blockMeshCache = new HashMap<>();
    private static final Map<Item, Mesh> itemMeshCache = new HashMap<>();
    private static final Map<EntityDefinition, Mesh> entityDefMeshCache = new HashMap<>();

    public static Mesh getBlockMesh(int blockId, DynamicTextureAtlas atlas) {
        return blockMeshCache.computeIfAbsent(blockId, id -> 
            ChunkMeshGenerator.generateSingleBlockMesh(new Block(id), atlas, null, null));
    }

    public static Mesh getItemMesh(Item item, DynamicTextureAtlas atlas) {
        return itemMeshCache.computeIfAbsent(item, i -> 
            i.isBlock() ? ChunkMeshGenerator.generateSingleBlockMesh(new Block(i.getId()), atlas, null, null) 
                        : com.za.zenith.world.items.ItemMeshGenerator.generateItemMesh(i.getTexturePath(), atlas, i.getId()));
    }

    public static Mesh getEntityMesh(EntityDefinition def, DynamicTextureAtlas atlas) {
        return entityDefMeshCache.computeIfAbsent(def, d -> 
            "item".equals(d.modelType()) ? com.za.zenith.world.items.ItemMeshGenerator.generateItemMesh(d.texture(), atlas, 0) 
                                         : ChunkMeshGenerator.generateSingleBlockMesh(new Block(com.za.zenith.world.blocks.BlockRegistry.getBlock(com.za.zenith.utils.Identifier.of(d.texture())).getId()), atlas, null, null));
    }

    public static void rebuild() {
        blockMeshCache.values().forEach(Mesh::cleanup);
        blockMeshCache.clear();
        itemMeshCache.values().forEach(Mesh::cleanup);
        itemMeshCache.clear();
        entityDefMeshCache.values().forEach(Mesh::cleanup);
        entityDefMeshCache.clear();
    }

    public static void cleanup() {
        rebuild();
    }
}
