package com.za.minecraft.world.blocks;

public class BlockTextures {
    private final String top;
    private final String bottom;
    private final String north;
    private final String south;
    private final String east;
    private final String west;
    
    // Constructor for single texture (all faces same)
    public BlockTextures(String texture) {
        this.top = texture;
        this.bottom = texture;
        this.north = texture;
        this.south = texture;
        this.east = texture;
        this.west = texture;
    }
    
    // Constructor for top/bottom/sides pattern (like grass, wood)
    public BlockTextures(String top, String bottom, String sides) {
        this.top = top;
        this.bottom = bottom;
        this.north = sides;
        this.south = sides;
        this.east = sides;
        this.west = sides;
    }
    
    // Constructor for full control over each face
    public BlockTextures(String top, String bottom, String north, String south, String east, String west) {
        this.top = top;
        this.bottom = bottom;
        this.north = north;
        this.south = south;
        this.east = east;
        this.west = west;
    }
    
    // Get texture for specific face (ChunkMeshGenerator face order)
    // 0:+Z(front/south), 1:-Z(back/north), 2:+X(right/east), 3:-X(left/west), 4:+Y(top), 5:-Y(bottom)
    public String getTextureForFace(int face) {
        return switch (face) {
            case 0 -> south;  // +Z front
            case 1 -> north;  // -Z back
            case 2 -> east;   // +X right
            case 3 -> west;   // -X left
            case 4 -> top;    // +Y top
            case 5 -> bottom; // -Y bottom
            default -> top;
        };
    }
    
    public String getTop() { return top; }
    public String getBottom() { return bottom; }
    public String getNorth() { return north; }
    public String getSouth() { return south; }
    public String getEast() { return east; }
    public String getWest() { return west; }
}
