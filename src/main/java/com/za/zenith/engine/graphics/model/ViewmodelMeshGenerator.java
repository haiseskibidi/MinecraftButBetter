package com.za.zenith.engine.graphics.model;

import com.za.zenith.engine.graphics.Mesh;
import com.za.zenith.engine.graphics.DynamicTextureAtlas;
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

        float texSize = 16.0f; 

        for (BoneDefinition.CubeDefinition cube : bone.cubes) {
            float w = cube.size[0] / 16.0f;
            float h = cube.size[1] / 16.0f;
            float d = cube.size[2] / 16.0f;

            float x = cube.x / 16.0f;
            float y = cube.y / 16.0f;
            // В OpenGL начало куба по Z должно быть самым дальним (минимальным),
            // чтобы он рос "на нас". Но мы хотим чтобы он рос "ОТ нас".
            // Поэтому glZ = -(cubeZ + depth)
            float z = -(cube.z / 16.0f + d);

            int u = cube.uv[0];
            int v = cube.uv[1];

            // Standard voxel cube generation
            addQuad(positions, texCoords, normals, indices,
                x, y + h, z + d,  x + w, y + h, z + d,  x + w, y + h, z,  x, y + h, z,
                u + d, v, w, d, texSize, layer, 0, 1, 0);
            addQuad(positions, texCoords, normals, indices,
                x, y, z,  x + w, y, z,  x + w, y, z + d,  x, y, z + d,
                u + d + w, v, w, d, texSize, layer, 0, -1, 0);
            addQuad(positions, texCoords, normals, indices,
                x, y, z + d,  x + w, y, z + d,  x + w, y + h, z + d,  x, y + h, z + d,
                u + d, v + d, w, h, texSize, layer, 0, 0, 1);
            addQuad(positions, texCoords, normals, indices,
                x + w, y, z,  x, y, z,  x, y + h, z,  x + w, y + h, z,
                u + 2 * d + w, v + d, w, h, texSize, layer, 0, 0, -1);
            addQuad(positions, texCoords, normals, indices,
                x, y, z,  x, y, z + d,  x, y + h, z + d,  x, y + h, z,
                u, v + d, d, h, texSize, layer, -1, 0, 0);
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

        tex.add(u0); tex.add(v0); tex.add(layer); tex.add(-1.0f);
        tex.add(u1); tex.add(v0); tex.add(layer); tex.add(-1.0f);
        tex.add(u1); tex.add(v1); tex.add(layer); tex.add(-1.0f);
        tex.add(u0); tex.add(v1); tex.add(layer); tex.add(-1.0f);

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
        return new Mesh(posArr, texArr, normArr, new float[posArr.length/3], indArr);
    }
}
