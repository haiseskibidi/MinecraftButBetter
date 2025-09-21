package com.za.minecraft.engine.graphics;

import com.za.minecraft.utils.Logger;
import org.lwjgl.system.MemoryStack;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.GL14.GL_TEXTURE_LOD_BIAS;
import static org.lwjgl.opengl.GL30.glGenerateMipmap;
import static org.lwjgl.stb.STBImage.*;
import static org.lwjgl.system.MemoryStack.stackPush;

public class DynamicTextureAtlas {
    private final int tileSize;
    private final Map<String, String> keyToPath = new LinkedHashMap<>();
    private final Map<String, int[]> keyToTile = new LinkedHashMap<>();
    private int textureId;
    private int width;
    private int height;

    public DynamicTextureAtlas(int tileSize) {
        this.tileSize = tileSize;
    }

    public void add(String key, String path) {
        keyToPath.put(key, path);
    }

    public void build() {
        int count = keyToPath.size();
        if (count == 0) {
            createWhiteTexture();
            return;
        }
        int tilesPerRow = (int) Math.ceil(Math.sqrt(count));
        width = tilesPerRow * tileSize;
        int rows = (int) Math.ceil((double) count / tilesPerRow);
        height = rows * tileSize;

        ByteBuffer atlas = ByteBuffer.allocateDirect(width * height * 4);

        int index = 0;
        stbi_set_flip_vertically_on_load(true);
        for (Map.Entry<String, String> e : keyToPath.entrySet()) {
            String key = e.getKey();
            String path = e.getValue();
            try (MemoryStack stack = stackPush()) {
                IntBuffer w = stack.mallocInt(1);
                IntBuffer h = stack.mallocInt(1);
                IntBuffer c = stack.mallocInt(1);
                ByteBuffer img = stbi_load(path, w, h, c, 4);
                if (img == null) {
                    Logger.error("Failed to load block texture: %s (%s)", path, stbi_failure_reason());
                    img = createSolidImage(tileSize, tileSize, (byte) 255, (byte) 0, (byte) 255, (byte) 255);
                    w.put(0, tileSize);
                    h.put(0, tileSize);
                }
                // Resize if needed (simple nearest neighbor if not 16x16)
                int iw = w.get(0), ih = h.get(0);
                if (iw != tileSize || ih != tileSize) {
                    ByteBuffer resized = ByteBuffer.allocateDirect(tileSize * tileSize * 4);
                    for (int y = 0; y < tileSize; y++) {
                        for (int x = 0; x < tileSize; x++) {
                            int sx = x * iw / tileSize;
                            int sy = y * ih / tileSize;
                            int src = (sy * iw + sx) * 4;
                            int dst = (y * tileSize + x) * 4;
                            resized.put(dst, img.get(src));
                            resized.put(dst + 1, img.get(src + 1));
                            resized.put(dst + 2, img.get(src + 2));
                            resized.put(dst + 3, img.get(src + 3));
                        }
                    }
                    img = resized;
                    iw = ih = tileSize;
                }

                int tileX = index % tilesPerRow;
                int tileY = index / tilesPerRow;
                keyToTile.put(key, new int[]{tileX, tileY});

                for (int y = 0; y < tileSize; y++) {
                    int destRow = (tileY * tileSize + y) * width * 4;
                    int srcRow = y * tileSize * 4;
                    for (int x = 0; x < tileSize; x++) {
                        int di = destRow + (tileX * tileSize + x) * 4;
                        int si = srcRow + x * 4;
                        atlas.put(di, img.get(si));
                        atlas.put(di + 1, img.get(si + 1));
                        atlas.put(di + 2, img.get(si + 2));
                        atlas.put(di + 3, img.get(si + 3));
                    }
                }
                stbi_image_free(img);
            }
            index++;
        }

        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST_MIPMAP_LINEAR); // Minecraft-style mipmaps for distance simplification
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, atlas);
        glGenerateMipmap(GL_TEXTURE_2D); // Essential for Minecraft-style distance simplification
        
        // Set LOD bias for Minecraft-style distance transitions
        glTexParameterf(GL_TEXTURE_2D, GL_TEXTURE_LOD_BIAS, -0.5f); // Earlier mipmap activation
        
        // Anisotropic filtering not needed with proper mipmaps
        // try {
        //     if (org.lwjgl.opengl.GL.getCapabilities().GL_EXT_texture_filter_anisotropic) {
        //         float max = org.lwjgl.opengl.GL11.glGetFloat(
        //             org.lwjgl.opengl.EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT
        //         );
        //         org.lwjgl.opengl.GL11.glTexParameterf(
        //             GL_TEXTURE_2D,
        //             org.lwjgl.opengl.EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT,
        //             Math.min(2.0f, Math.max(1.0f, max))
        //         );
        //     }
        // } catch (Throwable ignored) {}

        Logger.info("Built dynamic atlas %dx%d with %d tiles", width, height, count);
    }

    private void createWhiteTexture() {
        ByteBuffer buffer = createSolidImage(tileSize, tileSize, (byte) 255, (byte) 255, (byte) 255, (byte) 255);
        width = tileSize; height = tileSize;
        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, buffer);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
    }

    private ByteBuffer createSolidImage(int w, int h, byte r, byte g, byte b, byte a) {
        ByteBuffer img = ByteBuffer.allocateDirect(w * h * 4);
        for (int i = 0; i < w * h; i++) {
            int o = i * 4;
            img.put(o, r);
            img.put(o + 1, g);
            img.put(o + 2, b);
            img.put(o + 3, a);
        }
        return img;
    }

    public void bind() {
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    public void cleanup() {
        glDeleteTextures(textureId);
    }

    public float[] uvFor(String key) {
        int[] tile = keyToTile.get(key);
        if (tile == null) return new float[]{0,0, 1,0, 1,1, 0,1};
        float u0 = (float) (tile[0] * tileSize) / width;
        float v0 = (float) (tile[1] * tileSize) / height;
        float u1 = (float) ((tile[0] + 1) * tileSize) / width;
        float v1 = (float) ((tile[1] + 1) * tileSize) / height;
        
        
        return new float[]{u0, v0, u1, v0, u1, v1, u0, v1};
    }
}


