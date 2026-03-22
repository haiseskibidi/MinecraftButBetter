package com.za.minecraft.world.blocks;

import com.za.minecraft.utils.I18n;
import com.za.minecraft.utils.Identifier;
import com.za.minecraft.world.BlockPos;
import com.za.minecraft.world.blocks.entity.BlockEntity;
import com.za.minecraft.world.physics.VoxelShape;

public class BlockDefinition {
    private final int id;
    private final Identifier identifier;
    private final String name;
    private final boolean solid;
    private final boolean transparent;
    private float hardness = 1.0f; // Default hardness
    private String requiredTool = "none"; // pickaxe, shovel, axe, crowbar, knife
    private String dropItem = null; // Identifier of the item to drop (null = drop self)
    private boolean canSupportScavenge = false; // Whether items like sticks can spawn on top
    private PlacementType placementType = PlacementType.DEFAULT;
    private BlockTextures textures;
    private boolean fullCube = true;

    // Default shape is a full cube. Subclasses can override.
    protected VoxelShape shape = VoxelShape.FULL_CUBE;

    public BlockDefinition(int id, String name, boolean solid, boolean transparent) {
        this.id = id;
        this.identifier = Identifier.of(name.replace("block.", "").replace(".", ":"));
        this.name = name;
        this.solid = solid;
        this.transparent = transparent;
    }

    public BlockDefinition(int id, Identifier identifier, String translationKey, boolean solid, boolean transparent) {
        this.id = id;
        this.identifier = identifier;
        this.name = translationKey;
        this.solid = solid;
        this.transparent = transparent;
    }

    public BlockDefinition setTextures(BlockTextures textures) {
        this.textures = textures;
        return this;
    }

    public BlockDefinition setShape(VoxelShape shape) {
        this.shape = shape;
        this.fullCube = (shape == VoxelShape.FULL_CUBE);
        return this;
    }

    /**
     * Автоматически генерирует форму блока на основе его текстуры.
     * Используется для неполных блоков без явно заданной формы в JSON.
     */
    public void autoGenerateShape() {
        if (textures == null) return;
        
        // Для прозрачных блоков (как кресты) или неполных берем основную текстуру
        String texPath = textures.getTop(); 
        com.za.minecraft.world.physics.AABB texAABB = com.za.minecraft.utils.TextureAABBGenerator.generateAABB(texPath);
        
        if (texAABB != null) {
            this.shape = new VoxelShape(texAABB);
            this.fullCube = false;
        }
    }

    public BlockDefinition setHardness(float hardness) {
        this.hardness = hardness;
        return this;
    }

    public BlockDefinition setRequiredTool(String tool) {
        this.requiredTool = tool.toLowerCase();
        return this;
    }

    public BlockDefinition setPlacementType(PlacementType type) {
        this.placementType = type;
        return this;
    }

    public int getId() {
        return id;
    }

    public Identifier getIdentifier() {
        return identifier;
    }

    public String getName() {
        return I18n.get(name);
    }

    public float getHardness() {
        return hardness;
    }

    public String getRequiredTool() {
        return requiredTool;
    }

    public String getDropItem() {
        return dropItem;
    }

    public BlockDefinition setDropItem(String dropItem) {
        this.dropItem = dropItem;
        return this;
    }

    public boolean canSupportScavenge() {
        return canSupportScavenge;
    }

    public BlockDefinition setSupportScavenge(boolean support) {
        this.canSupportScavenge = support;
        return this;
    }

    public PlacementType getPlacementType() {
        return placementType;
    }


    public boolean isSolid() {
        return solid;
    }

    public boolean isTransparent() {
        return transparent;
    }

    public boolean isFullCube() {
        return fullCube;
    }


    public BlockDefinition setFullCube(boolean fullCube) {
        this.fullCube = fullCube;
        return this;
    }

    public BlockTextures getTextures() {
        return textures;
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
