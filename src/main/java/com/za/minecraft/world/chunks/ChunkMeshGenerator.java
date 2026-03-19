package com.za.minecraft.world.chunks;

import com.za.minecraft.engine.graphics.Mesh;
import com.za.minecraft.world.blocks.Block;
import com.za.minecraft.world.physics.AABB;
import com.za.minecraft.world.physics.VoxelShape;
import com.za.minecraft.engine.graphics.DynamicTextureAtlas;
import com.za.minecraft.world.blocks.BlockTextureMapper;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;

public class ChunkMeshGenerator {
    private static final float[][] FACE_NORMALS = new float[][]{
        {0,0,1, 0,0,1, 0,0,1, 0,0,1},
        {0,0,-1, 0,0,-1, 0,0,-1, 0,0,-1},
        {1,0,0, 1,0,0, 1,0,0, 1,0,0},
        {-1,0,0, -1,0,0, -1,0,0, -1,0,0},
        {0,1,0, 0,1,0, 0,1,0, 0,1,0},
        {0,-1,0, 0,-1,0, 0,-1,0, 0,-1,0}
    };
    private static final int[] FACE_INDICES = {0,1,2, 2,3,0};
    
    public static class ChunkMeshResult {
        public final Mesh opaqueMesh;
        public final Mesh translucentMesh;
        public ChunkMeshResult(Mesh opaqueMesh, Mesh translucentMesh) {
            this.opaqueMesh = opaqueMesh;
            this.translucentMesh = translucentMesh;
        }
    }

    private static class MeshData {
        List<Float> positions = new ArrayList<>();
        List<Float> texCoords = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Float> blockTypes = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        int vertexIndex = 0;

        void addFace(float[] fp, float[] fn, float blockTypeId, float[] fullUv, int face, float ox, float oy, float oz) {
            for (int v = 0; v < 4; v++) {
                positions.add(fp[v*3] + ox);
                positions.add(fp[v*3+1] + oy);
                positions.add(fp[v*3+2] + oz);
                normals.add(fn[v*3]);
                normals.add(fn[v*3+1]);
                normals.add(fn[v*3+2]);
                blockTypes.add(blockTypeId);
            }
            for (int v = 0; v < 4; v++) {
                float vx = fp[v*3];
                float vy = fp[v*3+1];
                float vz = fp[v*3+2];
                float lu = 0, lv = 0;
                switch (face) {
                    case 0: lu = vx; lv = vy; break;
                    case 1: lu = 1.0f - vx; lv = vy; break;
                    case 2: lu = 1.0f - vz; lv = vy; break;
                    case 3: lu = vz; lv = vy; break;
                    case 4: lu = vx; lv = 1.0f - vz; break;
                    case 5: lu = vx; lv = vz; break;
                }
                float topU = fullUv[0] * (1 - lu) + fullUv[2] * lu;
                float topV = fullUv[1] * (1 - lu) + fullUv[3] * lu;
                float botU = fullUv[6] * (1 - lu) + fullUv[4] * lu;
                float botV = fullUv[7] * (1 - lu) + fullUv[5] * lu;
                texCoords.add(topU * (1 - lv) + botU * lv);
                texCoords.add(topV * (1 - lv) + botV * lv);
            }
            for (int idx : FACE_INDICES) indices.add(vertexIndex + idx);
            vertexIndex += 4;
        }

        Mesh build() {
            if (positions.isEmpty()) return null;
            float[] p = new float[positions.size()], t = new float[texCoords.size()], n = new float[normals.size()], b = new float[blockTypes.size()];
            int[] ind = new int[indices.size()];
            for(int i=0; i<p.length; i++) p[i]=positions.get(i);
            for(int i=0; i<t.length; i++) t[i]=texCoords.get(i);
            for(int i=0; i<n.length; i++) n[i]=normals.get(i);
            for(int i=0; i<b.length; i++) b[i]=blockTypes.get(i);
            for(int i=0; i<ind.length; i++) ind[i]=indices.get(i);
            return new Mesh(p, t, n, b, ind);
        }
    }

    public static Mesh generateSingleBlockMesh(Block block, DynamicTextureAtlas atlas) {
        MeshData data = new MeshData();
        VoxelShape shape = block.getShape();
        if (shape == null) return null;
        for (AABB box : shape.getBoxes()) {
            Vector3f min = box.getMin(), max = box.getMax();
            float[][] facePositions = new float[][]{
                {min.x, min.y, max.z,  max.x, min.y, max.z,  max.x, max.y, max.z,  min.x, max.y, max.z},
                {max.x, min.y, min.z,  min.x, min.y, min.z,  min.x, max.y, min.z,  max.x, max.y, min.z},
                {max.x, min.y, max.z,  max.x, min.y, min.z,  max.x, max.y, min.z,  max.x, max.y, max.z},
                {min.x, min.y, min.z,  min.x, min.y, max.z,  min.x, max.y, max.z,  min.x, max.y, min.z},
                {min.x, max.y, max.z,  max.x, max.y, max.z,  max.x, max.y, min.z,  min.x, max.y, min.z},
                {min.x, min.y, min.z,  max.x, min.y, min.z,  max.x, min.y, max.z,  min.x, min.y, max.z}
            };
            for (int face = 0; face < 6; face++) {
                data.addFace(facePositions[face], FACE_NORMALS[face], (float) block.getType(), BlockTextureMapper.uvFor(block, face, atlas), face, 0, 0, 0);
            }
        }
        return data.build();
    }

    public static ChunkMeshResult generateMesh(Chunk chunk, DynamicTextureAtlas atlas) {
        MeshData opaque = new MeshData();
        MeshData translucent = new MeshData();
        int[][] neighborOffsets = new int[][]{{0,0,1},{0,0,-1},{1,0,0},{-1,0,0},{0,1,0},{0,-1,0}};

        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int y = 0; y < Chunk.CHUNK_HEIGHT; y++) {
                for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                    Block block = chunk.getBlock(x, y, z);
                    if (block.isAir()) continue;
                    VoxelShape shape = block.getShape();
                    if (shape == null) continue;

                    // Стекло и подобные блоки (но не листья, листья - непрозрачные с альфа-тестом)
                    boolean isTranslucent = (block.getType() == com.za.minecraft.world.blocks.BlockType.GLASS);
                    MeshData current = isTranslucent ? translucent : opaque;

                    for (AABB box : shape.getBoxes()) {
                        Vector3f min = box.getMin(), max = box.getMax();
                        float[][] facePositions = new float[][]{
                            {min.x, min.y, max.z,  max.x, min.y, max.z,  max.x, max.y, max.z,  min.x, max.y, max.z},
                            {max.x, min.y, min.z,  min.x, min.y, min.z,  min.x, max.y, min.z,  max.x, max.y, min.z},
                            {max.x, min.y, max.z,  max.x, min.y, min.z,  max.x, max.y, min.z,  max.x, max.y, max.z},
                            {min.x, min.y, min.z,  min.x, min.y, max.z,  min.x, max.y, max.z,  min.x, max.y, min.z},
                            {min.x, max.y, max.z,  max.x, max.y, max.z,  max.x, max.y, min.z,  min.x, max.y, min.z},
                            {min.x, min.y, min.z,  max.x, min.y, min.z,  max.x, min.y, max.z,  min.x, min.y, max.z}
                        };
                        for (int face = 0; face < 6; face++) {
                            boolean drawFace = true, onBoundary = false;
                            switch (face) {
                                case 0: onBoundary = (max.z >= 1.0f); break;
                                case 1: onBoundary = (min.z <= 0.0f); break;
                                case 2: onBoundary = (max.x >= 1.0f); break;
                                case 3: onBoundary = (min.x <= 0.0f); break;
                                case 4: onBoundary = (max.y >= 1.0f); break;
                                case 5: onBoundary = (min.y <= 0.0f); break;
                            }
                            if (onBoundary) {
                                Block neighbor = chunk.getBlock(x + neighborOffsets[face][0], y + neighborOffsets[face][1], z + neighborOffsets[face][2]);
                                
                                // Culling logic fix:
                                // 1. If neighbor is a full opaque cube, hide our face.
                                // 2. If neighbor is identical to us AND it's a type that supports culling (like glass or full cubes), hide face.
                                // 3. For stairs and slabs, we NEVER hide the face unless the neighbor is a full opaque cube.
                                
                                if (!neighbor.isAir() && !neighbor.isTransparent()) {
                                    // Neighbor is full opaque cube (Stone, etc.)
                                    drawFace = false;
                                } else if (neighbor.getType() == block.getType() && isTranslucent) {
                                    // Two glass blocks: hide internal face
                                    drawFace = false;
                                } else if (neighbor.getType() == block.getType() && !block.isTransparent()) {
                                    // Two identical opaque full blocks (should be handled by first case, but for safety)
                                    drawFace = false;
                                }
                            }
                            if (drawFace) {
                                current.addFace(facePositions[face], FACE_NORMALS[face], (float) block.getType(), BlockTextureMapper.uvFor(block, face, atlas), face, (float)x, (float)y, (float)z);
                            }
                        }
                    }
                }
            }
        }
        return new ChunkMeshResult(opaque.build(), translucent.build());
    }
}
