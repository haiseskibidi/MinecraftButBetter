package com.za.minecraft.world.items;

import com.za.minecraft.engine.graphics.Mesh;
import com.za.minecraft.engine.graphics.DynamicTextureAtlas;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.stb.STBImage.*;

public class ItemMeshGenerator {

    public static Mesh generateItemMesh(String texturePath, DynamicTextureAtlas atlas, byte itemId) {
        if (texturePath == null || texturePath.isEmpty() || atlas == null) return null;
        
        String resourcePath = texturePath.replace("src/main/resources/", "");
        ByteBuffer image = null;
        int width, height;

        try (var is = ItemMeshGenerator.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (is == null) return null;
            byte[] data = is.readAllBytes();
            
            ByteBuffer buffer = MemoryUtil.memAlloc(data.length);
            buffer.put(data).flip();
            
            int[] w = new int[1];
            int[] h = new int[1];
            int[] c = new int[1];
            // Flip to match atlas logic
            stbi_set_flip_vertically_on_load(true);
            image = stbi_load_from_memory(buffer, w, h, c, 4);
            MemoryUtil.memFree(buffer);
            
            if (image == null) return null;
            width = w[0];
            height = h[0];
        } catch (Exception e) {
            if (image != null) stbi_image_free(image);
            return null;
        }

        List<Float> positions = new ArrayList<>();
        List<Float> texCoords = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        float thickness = 0.0625f; 
        float h = thickness / 2.0f; // half thickness
        
        float[] uvs = atlas.uvFor(texturePath);
        if (uvs == null) {
            stbi_image_free(image);
            return null;
        }
        
        float u0 = uvs[0], v0 = uvs[1], u1 = uvs[4], v1 = uvs[5];
        float uSize = u1 - u0;
        float vSize = v1 - v0;

        // Micro-padding to prevent texture bleeding
        float uE = uSize / (width * 10.0f);
        float vE = vSize / (height * 10.0f);

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                if (isOpaque(image, x, y, width, height)) {
                    // Mesh coords: x centered, y from 0 to 1
                    float x0 = (float) x / width - 0.5f;
                    float y0 = (float) y / height;
                    float x1 = (float) (x + 1) / width - 0.5f;
                    float y1 = (float) (y + 1) / height;
                    
                    // Texture coordinates (v0 is bottom, v1 is top due to flip)
                    float pu0 = u0 + (float) x / width * uSize + uE;
                    float pv0 = v0 + (float) y / height * vSize + vE; 
                    float pu1 = u0 + (float) (x + 1) / width * uSize - uE;
                    float pv1 = v0 + (float) (y + 1) / height * vSize - vE;

                    // 1. FRONT FACE (+Z)
                    addQuad(positions, texCoords, normals, indices,
                        x0, y0, h,  x1, y0, h,  x1, y1, h,  x0, y1, h,
                        pu0, pv0, pu1, pv0, pu1, pv1, pu0, pv1, 0, 0, 1);

                    // 2. BACK FACE (-Z) - CCW from outside
                    addQuad(positions, texCoords, normals, indices,
                        x0, y0, -h,  x0, y1, -h,  x1, y1, -h,  x1, y0, -h,
                        pu0, pv0, pu0, pv1, pu1, pv1, pu1, pv0, 0, 0, -1);
                    
                    // Sides sampling (pixel center)
                    float mu = (pu0 + pu1) * 0.5f;
                    float mv = (pv0 + pv1) * 0.5f;

                    // 3. TOP (+Y) - CCW from outside
                    if (!isOpaque(image, x, y + 1, width, height)) {
                        addQuad(positions, texCoords, normals, indices,
                            x0, y1, h,  x1, y1, h,  x1, y1, -h,  x0, y1, -h,
                            mu, mv, mu, mv, mu, mv, mu, mv, 0, 1, 0);
                    }
                    // 4. BOTTOM (-Y) - CCW from outside
                    if (!isOpaque(image, x, y - 1, width, height)) {
                        addQuad(positions, texCoords, normals, indices,
                            x0, y0, -h,  x1, y0, -h,  x1, y0, h,  x0, y0, h,
                            mu, mv, mu, mv, mu, mv, mu, mv, 0, -1, 0);
                    }
                    // 5. LEFT (-X) - CCW from outside
                    if (!isOpaque(image, x - 1, y, width, height)) {
                        addQuad(positions, texCoords, normals, indices,
                            x0, y0, -h,  x0, y0, h,  x0, y1, h,  x0, y1, -h,
                            mu, mv, mu, mv, mu, mv, mu, mv, -1, 0, 0);
                    }
                    // 6. RIGHT (+X) - CCW from outside
                    if (!isOpaque(image, x + 1, y, width, height)) {
                        addQuad(positions, texCoords, normals, indices,
                            x1, y0, h,  x1, y0, -h,  x1, y1, -h,  x1, y1, h,
                            mu, mv, mu, mv, mu, mv, mu, mv, 1, 0, 0);
                    }
                }
            }
        }

        stbi_image_free(image);

        float[] posArr = new float[positions.size()];
        for (int i = 0; i < positions.size(); i++) posArr[i] = positions.get(i);
        float[] texArr = new float[texCoords.size()];
        for (int i = 0; i < texCoords.size(); i++) texArr[i] = texCoords.get(i);
        float[] normArr = new float[normals.size()];
        for (int i = 0; i < normals.size(); i++) normArr[i] = normals.get(i);
        int[] indArr = new int[indices.size()];
        for (int i = 0; i < indices.size(); i++) indArr[i] = indices.get(i);

        float[] blockTypes = new float[posArr.length / 3];
        for (int i = 0; i < blockTypes.length; i++) blockTypes[i] = itemId;

        return new Mesh(posArr, texArr, normArr, blockTypes, indArr);
    }

    private static boolean isOpaque(ByteBuffer image, int x, int y, int width, int height) {
        if (x < 0 || x >= width || y < 0 || y >= height) return false;
        int alpha = image.get((y * width + x) * 4 + 3) & 0xFF;
        return alpha > 30; 
    }

    private static void addQuad(List<Float> pos, List<Float> tex, List<Float> norm, List<Integer> ind,
                               float x0, float y0, float z0,
                               float x1, float y1, float z1,
                               float x2, float y2, float z2,
                               float x3, float y3, float z3,
                               float u0, float v0, float u1, float v1, float u2, float v2, float u3, float v3,
                               float nx, float ny, float nz) {
        int start = pos.size() / 3;
        pos.add(x0); pos.add(y0); pos.add(z0);
        pos.add(x1); pos.add(y1); pos.add(z1);
        pos.add(x2); pos.add(y2); pos.add(z2);
        pos.add(x3); pos.add(y3); pos.add(z3);
        tex.add(u0); tex.add(v0);
        tex.add(u1); tex.add(v1);
        tex.add(u2); tex.add(v2);
        tex.add(u3); tex.add(v3);
        for (int i = 0; i < 4; i++) {
            norm.add(nx); norm.add(ny); norm.add(nz);
        }
        ind.add(start); ind.add(start + 1); ind.add(start + 2);
        ind.add(start + 2); ind.add(start + 3); ind.add(start);
    }
}
