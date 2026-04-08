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
    private Identifier nextStage = null;
    private boolean alwaysRender = false;
    private boolean replaceable = false;
    private boolean tinted = false;
    private String upperTexture = null;
 // Текстура для верхней части DOUBLE_PLANT
    private PlacementType placementType = PlacementType.DEFAULT;
    private BlockTextures textures;
    private boolean fullCube = true;
    private float soilingAmount = 0.0f;
    private float cleaningAmount = 0.0f;
    private float firingTemperature = 0.0f;
    private String wobbleAnimation = "block_wobble";
    private int breakingPattern = 0; // 0=Generic, 1=Wood, 2=Stone, etc.
    private MiningSettings miningSettings = MiningSettings.DEFAULT;
    private float interactionCooldown = -1.0f; // -1 means use PhysicsSettings.baseMiningCooldown
    private float healingSpeed = 0.1f; // Default: heals 10% of max health per second

    public float getHealingSpeed() {
        return healingSpeed;
    }

    public BlockDefinition setHealingSpeed(float healingSpeed) {
        this.healingSpeed = healingSpeed;
        return this;
    }

    public float getInteractionCooldown() {
        if (interactionCooldown < 0) {
            return com.za.minecraft.world.physics.PhysicsSettings.getInstance().baseMiningCooldown;
        }
        return interactionCooldown;
    }

    public void setInteractionCooldown(float interactionCooldown) {
        this.interactionCooldown = interactionCooldown;
    }

    // ... в методе getTextures() или аналогичном ...
    public String getWobbleAnimation() {
        return wobbleAnimation;
    }

    public MiningSettings getMiningSettings() {
        return miningSettings;
    }

    public void setMiningSettings(MiningSettings miningSettings) {
        this.miningSettings = miningSettings;
    }

    public void setWobbleAnimation(String wobbleAnimation) {
        this.wobbleAnimation = wobbleAnimation;
    }

    public int getBreakingPattern() {
        return breakingPattern;
    }

    public void setBreakingPattern(int breakingPattern) {
        this.breakingPattern = breakingPattern;
    }

    public void setUpperTexture(String upperTexture) {
        this.upperTexture = upperTexture;
    }

    public String getUpperTexture() {
        return upperTexture;
    }

    public float getFiringTemperature() {
        return firingTemperature;
    }

    public void setFiringTemperature(float firingTemperature) {
        this.firingTemperature = firingTemperature;
    }

    public float getSoilingAmount() {
        return soilingAmount;
    }

    public void setSoilingAmount(float soilingAmount) {
        this.soilingAmount = soilingAmount;
    }

    public float getCleaningAmount() {
        return cleaningAmount;
    }

    public void setCleaningAmount(float cleaningAmount) {
        this.cleaningAmount = cleaningAmount;
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
            // AAA Polish: Добавляем небольшой padding (0.05 блока), чтобы по мелким предметам было легче попасть.
            // Иначе хитбокс палки — это линия шириной в 2 пикселя.
            float padding = 0.05f;
            this.shape = new VoxelShape(new com.za.minecraft.world.physics.AABB(
                Math.max(0.0f, texAABB.minX() - padding), Math.max(0.0f, texAABB.minY() - padding), Math.max(0.0f, texAABB.minZ() - padding),
                Math.min(1.0f, texAABB.maxX() + padding), Math.min(1.0f, texAABB.maxY() + padding), Math.min(1.0f, texAABB.maxZ() + padding)
            ));
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

    public Identifier getNextStage() {
        return nextStage;
    }

    public BlockDefinition setNextStage(Identifier nextStage) {
        this.nextStage = nextStage;
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

    public boolean isTinted() {
        return tinted;
    }

    public BlockDefinition setTinted(boolean tinted) {
        this.tinted = tinted;
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
     * @return true, если блок имеет логику взаимодействия на ПКМ.
     */
    public boolean hasOnUse() {
        return false;
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
