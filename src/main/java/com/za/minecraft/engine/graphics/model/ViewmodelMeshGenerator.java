package com.za.minecraft.engine.graphics.model;

import com.za.minecraft.engine.graphics.Mesh;
import com.za.minecraft.engine.graphics.DynamicTextureAtlas;
import java.util.ArrayList;
import java.util.List;

public class ViewmodelMeshGenerator {

    public static Mesh generateBoneMesh(BoneDefinition bone, String texturePath, DynamicTextureAtlas atlas) {
        if (bone.cubes == null || bone.cubes.isEmpty()) return null;

        float[] atlasUvs = atlas.uvFor(texturePath);
        if (atlasUvs == null) return null;
        float layer = atlasUvs[2];

        List<Float> positions = new ArrayList<>();
        List<Float> texCoords = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();

        // Assuming a standard texture size (16x16 for blocks, 64x64 for skins)
        float texSize = 16.0f; 

        for (BoneDefinition.CubeDefinition cube : bone.cubes) {
            // Origin and size are in "voxels" (1/16 of a block)
            // Vertices are relative to the bone's pivot
            float px = bone.pivot != null ? bone.pivot[0] : 0;
            float py = bone.pivot != null ? bone.pivot[1] : 0;
            float pz = bone.pivot != null ? bone.pivot[2] : 0;

            float x = (cube.origin[0] - px) / 16.0f;
            float y = (cube.origin[1] - py) / 16.0f;
            float z = (cube.origin[2] - pz) / 16.0f;
            float w = cube.size[0] / 16.0f;
            float h = cube.size[1] / 16.0f;
            float d = cube.size[2] / 16.0f;

            int u = cube.uv[0];
            int v = cube.uv[1];

            // 6 faces of the voxel cube
            // Top
            addQuad(positions, texCoords, normals, indices,
                x, y + h, z + d,  x + w, y + h, z + d,  x + w, y + h, z,  x, y + h, z,
                u + d, v, w, d, texSize, layer, 0, 1, 0);
            // Bottom
            addQuad(positions, texCoords, normals, indices,
                x, y, z,  x + w, y, z,  x + w, y, z + d,  x, y, z + d,
                u + d + w, v, w, d, texSize, layer, 0, -1, 0);
            // Front
            addQuad(positions, texCoords, normals, indices,
                x, y, z + d,  x + w, y, z + d,  x + w, y + h, z + d,  x, y + h, z + d,
                u + d, v + d, w, h, texSize, layer, 0, 0, 1);
            // Back
            addQuad(positions, texCoords, normals, indices,
                x + w, y, z,  x, y, z,  x, y + h, z,  x + w, y + h, z,
                u + 2 * d + w, v + d, w, h, texSize, layer, 0, 0, -1);
            // Left
            addQuad(positions, texCoords, normals, indices,
                x, y, z,  x, y, z + d,  x, y + h, z + d,  x, y + h, z,
                u, v + d, d, h, texSize, layer, -1, 0, 0);
            // Right
            addQuad(positions, texCoords, normals, indices,
                x + w, y, z + d,  x + w, y, z,  x + w, y + h, z,  x + w, y + h, z + d,
                u + d + w, v + d, d, h, texSize, layer, 1, 0, 0);
        }

        return finalizeMesh(positions, texCoords, normals, indices);
    }

    private static void addQuad(List<Float> pos, List<Float> tex, List<Float> norm, List<Integer> ind,
                               float x0, float y0, float z0, float x1, float y1, float z1,
                               float x2, float y2, float z2, float x3, float y3, float z3,
                               float u, float v, float w, float h, float texSize, float layer,
                               float nx, float ny, float nz) {
        int start = pos.size() / 3;
        pos.add(x0); pos.add(y0); pos.add(z0);
        pos.add(x1); pos.add(y1); pos.add(z1);
        pos.add(x2); pos.add(y2); pos.add(z2);
        pos.add(x3); pos.add(y3); pos.add(z3);

        float u0 = u / texSize;
        float v0 = v / texSize;
        float u1 = (u + w) / texSize;
        float v1 = (v + h) / texSize;

        // Note: Engine expects (u, v, layer)
        tex.add(u0); tex.add(v0); tex.add(layer);
        tex.add(u1); tex.add(v0); tex.add(layer);
        tex.add(u1); tex.add(v1); tex.add(layer);
        tex.add(u0); tex.add(v1); tex.add(layer);

        for (int i = 0; i < 4; i++) {
            norm.add(nx); norm.add(ny); norm.add(nz);
        }

        ind.add(start); ind.add(start + 1); ind.add(start + 2);
        ind.add(start + 2); ind.add(start + 3); ind.add(start);
    }

    private static Mesh finalizeMesh(List<Float> p, List<Float> t, List<Float> n, List<Integer> ind) {
        float[] posArr = new float[p.size()];
        for (int i = 0; i < p.size(); i++) posArr[i] = p.get(i);
        float[] texArr = new float[t.size()];
        for (int i = 0; i < t.size(); i++) texArr[i] = t.get(i);
        float[] normArr = new float[n.size()];
        for (int i = 0; i < n.size(); i++) normArr[i] = n.get(i);
        int[] indArr = new int[ind.size()];
        for (int i = 0; i < ind.size(); i++) indArr[i] = ind.get(i);
        
        float[] blockTypes = new float[posArr.length / 3];

        return new Mesh(posArr, texArr, normArr, blockTypes, indArr);
    }
}
