package com.za.minecraft.world.blocks;

import com.za.minecraft.utils.I18n;
import com.za.minecraft.utils.Identifier;
import com.za.minecraft.world.BlockPos;
import com.za.minecraft.world.blocks.entity.BlockEntity;
import com.za.minecraft.world.physics.VoxelShape;

import java.util.ArrayList;
import java.util.List;

public class BlockDefinition {
    private final int id;
    private final Identifier identifier;
    private final String name;
    private final boolean solid;
    private final boolean transparent;
    private float hardness = 1.0f; // Default hardness
    private String requiredTool = "none"; // pickaxe, shovel, axe, crowbar, knife
    
    // Legacy support fields
    private String dropItem = null;
    private float dropChance = 1.0f;
    
    // Advanced drop rules
    private final List<DropRule> dropRules = new ArrayList<>();
    private final List<String> tags = new ArrayList<>();
    
    private boolean canSupportScavenge = false;
    private int fellingStages = 0;
    private boolean alwaysRender = false;
    private boolean replaceable = false;
    private String upperTexture = null; // Текстура для верхней части DOUBLE_PLANT
    private PlacementType placementType = PlacementType.DEFAULT;
    private BlockTextures textures;
    private boolean fullCube = true;

    // ... в методе getTextures() или аналогичном ...
    public void setUpperTexture(String upperTexture) {
        this.upperTexture = upperTexture;
    }

    public String getUpperTexture() {
        return upperTexture;
    }

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

    public BlockDefinition addDropRule(DropRule rule) {
        this.dropRules.add(rule);
        return this;
    }

    public BlockDefinition addTag(String tag) {
        if (!tags.contains(tag)) {
            tags.add(tag);
        }
        return this;
    }

    public boolean hasTag(String tag) {
        return tags.contains(tag);
    }

    public List<DropRule> getDropRules() {
        return dropRules;
    }

    public BlockDefinition setTextures(BlockTextures textures) {
        this.textures = textures;
        return this;
    }

    public BlockDefinition setShape(VoxelShape shape) {
        this.shape = shape;
        this.fullCube = shape.isFullCube();
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

    public float getDropChance() {
        return dropChance;
    }

    public BlockDefinition setDropChance(float dropChance) {
        this.dropChance = dropChance;
        return this;
    }

    public boolean canSupportScavenge() {
        return canSupportScavenge;
    }

    public BlockDefinition setSupportScavenge(boolean support) {
        this.canSupportScavenge = support;
        return this;
    }

    public int getFellingStages() {
        return fellingStages;
    }

    public BlockDefinition setFellingStages(int fellingStages) {
        this.fellingStages = fellingStages;
        return this;
    }

    public PlacementType getPlacementType() {
        return placementType;
    }

    public boolean isAlwaysRender() {
        return alwaysRender;
    }

    public BlockDefinition setAlwaysRender(boolean alwaysRender) {
        this.alwaysRender = alwaysRender;
        return this;
    }

    public boolean isReplaceable() {
        return replaceable;
    }

    public BlockDefinition setReplaceable(boolean replaceable) {
        this.replaceable = replaceable;
        return this;
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
     * Вызывается при нажатии ПКМ по блоку.
     * @param hitX Относительная координата X клика (0.0-1.0)
     * @param hitY Относительная координата Y клика (0.0-1.0)
     * @param hitZ Относительная координата Z клика (0.0-1.0)
     * @return true, если действие было поглощено и стандартная обработка не требуется.
     */
    public boolean onUse(com.za.minecraft.world.World world, BlockPos pos, com.za.minecraft.entities.Player player, com.za.minecraft.world.items.ItemStack heldStack, float hitX, float hitY, float hitZ) {
        return false;
    }

    /**
     * Вызывается при нажатии ЛКМ по блоку.
     */
    public boolean onLeftClick(com.za.minecraft.world.World world, BlockPos pos, com.za.minecraft.entities.Player player, com.za.minecraft.world.items.ItemStack heldStack, float hitX, float hitY, float hitZ, boolean isNewClick) {
        return false;
    }

    /**
     * Создает новую сущность блока для данного определения.
     * Переопределяется в подклассах для блоков с логикой.
     */
    public BlockEntity createBlockEntity(BlockPos pos) {
        return null;
    }

    /**
     * Вызывается непосредственно перед тем, как блок будет заменен на воздух или другой блок игроком.
     */
    public void onDestroyed(com.za.minecraft.world.World world, BlockPos pos, Block block, com.za.minecraft.entities.Player player) {
    }

    /**
     * Вызывается, когда игрок завершил разрушение блока (breakingProgress >= 1.0).
     * Если возвращает true, блок удаляется из мира.
     * Если возвращает false, блок остается (используется для многоступенчатого срубания).
     */
    public boolean onBlockBreak(com.za.minecraft.world.World world, BlockPos pos, Block block, com.za.minecraft.entities.Player player) {
        return true;
    }
}
