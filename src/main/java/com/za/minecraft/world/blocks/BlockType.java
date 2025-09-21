package com.za.minecraft.world.blocks;

public enum BlockType {
    AIR(0, "air", false, false),
    GRASS(1, "grass", true, false),
    DIRT(2, "dirt", true, false),
    STONE(3, "stone", true, false),
    WOOD(4, "wood", true, false),
    LEAVES(5, "leaves", true, true),
    OAK_PLANKS(6, "oak_planks", true, false),
    COBBLESTONE(7, "cobblestone", true, false),
    BEDROCK(8, "bedrock", true, false),
    SAND(9, "sand", true, false),
    GRAVEL(10, "gravel", true, false),
    GOLD_ORE(11, "gold_ore", true, false),
    IRON_ORE(12, "iron_ore", true, false),
    COAL_ORE(13, "coal_ore", true, false),
    BOOKSHELF(14, "bookshelf", true, false),
    MOSSY_COBBLESTONE(15, "mossy_cobblestone", true, false),
    OBSIDIAN(16, "obsidian", true, false);
    
    private final int id;
    private final String name;
    private final boolean solid;
    private final boolean transparent;
    
    BlockType(int id, String name, boolean solid, boolean transparent) {
        this.id = id;
        this.name = name;
        this.solid = solid;
        this.transparent = transparent;
    }
    
    public int getId() {
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
    
    public static BlockType fromId(int id) {
        for (BlockType type : values()) {
            if (type.id == id) {
                return type;
            }
        }
        return AIR;
    }
}
