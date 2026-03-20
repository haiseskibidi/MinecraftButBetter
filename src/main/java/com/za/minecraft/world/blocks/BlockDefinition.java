package com.za.minecraft.world.blocks;

import com.za.minecraft.world.BlockPos;
import com.za.minecraft.world.blocks.entity.BlockEntity;
import com.za.minecraft.world.physics.VoxelShape;

public class BlockDefinition {
    private final byte id;
    private final String name;
    private final boolean solid;
    private final boolean transparent;
    private float hardness = 1.0f; // Default hardness
    private BlockTextures textures;
    
    // Default shape is a full cube. Subclasses can override.
    protected VoxelShape shape = VoxelShape.FULL_CUBE;

    public BlockDefinition(int id, String name, boolean solid, boolean transparent) {
        this.id = (byte) id;
        this.name = name;
        this.solid = solid;
        this.transparent = transparent;
    }

    public BlockDefinition setTextures(BlockTextures textures) {
        this.textures = textures;
        return this;
    }
    
    public BlockDefinition setShape(VoxelShape shape) {
        this.shape = shape;
        return this;
    }

    public byte getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isSolid() {
        return solid;
    }

    public boolean isTransparent() {
        return transparent;
    }

    public boolean isFullCube() {
        return true;
    }

    public BlockTextures getTextures() {
        return textures;
    }

    public float getHardness() {
        return hardness;
    }

    public BlockDefinition setHardness(float hardness) {
        this.hardness = hardness;
        return this;
    }
    
    public VoxelShape getShape(byte metadata) {
        return shape;
    }

    /**
     * Создает новую сущность блока для данного определения.
     * Переопределяется в подклассах для блоков с логикой.
     */
    public BlockEntity createBlockEntity(BlockPos pos) {
        return null;
    }
}
