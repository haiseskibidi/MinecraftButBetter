package com.za.minecraft.engine.graphics;

public class TextureAtlas {
    private final Texture texture;
    private final int tileSize; // in pixels
    private final int tilesX;
    private final int tilesY;

    public TextureAtlas(String path, int tileSize) {
        this.texture = new Texture(path);
        this.tileSize = tileSize;
        this.tilesX = Math.max(1, texture.getWidth() / tileSize);
        this.tilesY = Math.max(1, texture.getHeight() / tileSize);
    }

    public void bind() {
        texture.bind();
    }

    public void cleanup() {
        texture.cleanup();
    }

    public float[] uvForTile(int tileX, int tileY) {
        float u0 = (float) (tileX * tileSize) / texture.getWidth();
        float v0 = (float) (tileY * tileSize) / texture.getHeight();
        float u1 = (float) ((tileX + 1) * tileSize) / texture.getWidth();
        float v1 = (float) ((tileY + 1) * tileSize) / texture.getHeight();
        return new float[]{
            u0, v0,
            u1, v0,
            u1, v1,
            u0, v1
        };
    }
}


