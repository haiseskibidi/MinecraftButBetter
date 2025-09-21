package com.za.minecraft.world.chunks;

import com.za.minecraft.engine.graphics.Mesh;
import com.za.minecraft.world.blocks.Block;

import com.za.minecraft.engine.graphics.DynamicTextureAtlas;
import com.za.minecraft.world.blocks.BlockTextureMapper;
import java.util.ArrayList;
import java.util.List;

public class ChunkMeshGenerator {
    // Per-face data for a unit cube at origin, faces: +Z, -Z, +X, -X, +Y, -Y
    private static final float[][] FACE_POSITIONS = new float[][]{
        { // +Z front
            0,0,1,  1,0,1,  1,1,1,  0,1,1
        },
        { // -Z back
            1,0,0,  0,0,0,  0,1,0,  1,1,0
        },
        { // +X right
            1,0,1,  1,0,0,  1,1,0,  1,1,1
        },
        { // -X left
            0,0,0,  0,0,1,  0,1,1,  0,1,0
        },
        { // +Y top
            0,1,1,  1,1,1,  1,1,0,  0,1,0
        },
        { // -Y bottom
            0,0,0,  1,0,0,  1,0,1,  0,0,1
        }
    };
    private static final float[][] FACE_NORMALS = new float[][]{
        {0,0,1, 0,0,1, 0,0,1, 0,0,1},
        {0,0,-1, 0,0,-1, 0,0,-1, 0,0,-1},
        {1,0,0, 1,0,0, 1,0,0, 1,0,0},
        {-1,0,0, -1,0,0, -1,0,0, -1,0,0},
        {0,1,0, 0,1,0, 0,1,0, 0,1,0},
        {0,-1,0, 0,-1,0, 0,-1,0, 0,-1,0}
    };
    private static final int[] FACE_INDICES = {0,1,2, 2,3,0};
    
    public static Mesh generateMesh(Chunk chunk, DynamicTextureAtlas atlas) {
        List<Float> positions = new ArrayList<>();
        List<Float> texCoords = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Float> blockTypes = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        
        int vertexIndex = 0;
        
        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int y = 0; y < Chunk.CHUNK_HEIGHT; y++) {
                for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                    Block block = chunk.getBlock(x, y, z);
                    if (block.isAir()) continue;
                    // For each face, add only if neighbor is air
                    // neighbor offsets for faces: front(0,0,1), back(0,0,-1), right(1,0,0), left(-1,0,0), top(0,1,0), bottom(0,-1,0)
                    int[][] neighbor = new int[][]{{0,0,1},{0,0,-1},{1,0,0},{-1,0,0},{0,1,0},{0,-1,0}};
                    for (int face = 0; face < 6; face++) {
                        int nx = x + neighbor[face][0];
                        int ny = y + neighbor[face][1];
                        int nz = z + neighbor[face][2];
                        Block neighborBlock = chunk.getBlock(nx, ny, nz);
                        // Cull face if neighbor is solid and opaque
                        if (!neighborBlock.isAir() && !neighborBlock.isTransparent()) continue;
                        float[] fp = FACE_POSITIONS[face];
                        float[] fn = FACE_NORMALS[face];
                        float blockTypeId = (float) block.getType().getId();
                        for (int v = 0; v < 4; v++) {
                            positions.add(fp[v*3] + x);
                            positions.add(fp[v*3+1] + y);
                            positions.add(fp[v*3+2] + z);
                            normals.add(fn[v*3]);
                            normals.add(fn[v*3+1]);
                            normals.add(fn[v*3+2]);
                            blockTypes.add(blockTypeId);
                        }
                        float[] uv = BlockTextureMapper.uvFor(block, face, atlas);
                        for (float u : uv) texCoords.add(u);
                        for (int idx : FACE_INDICES) indices.add(vertexIndex + idx);
                        vertexIndex += 4;
                    }
                }
            }
        }
        
        if (positions.isEmpty()) {
            return new Mesh(new float[0], new float[0], new float[0], new float[0], new int[0]);
        }
        
        float[] posArray = new float[positions.size()];
        float[] texArray = new float[texCoords.size()];
        float[] normalArray = new float[normals.size()];
        float[] blockTypeArray = new float[blockTypes.size()];
        int[] indexArray = new int[indices.size()];
        
        for (int i = 0; i < positions.size(); i++) {
            posArray[i] = positions.get(i);
        }
        
        for (int i = 0; i < texCoords.size(); i++) {
            texArray[i] = texCoords.get(i);
        }
        
        for (int i = 0; i < normals.size(); i++) {
            normalArray[i] = normals.get(i);
        }
        
        for (int i = 0; i < blockTypes.size(); i++) {
            blockTypeArray[i] = blockTypes.get(i);
        }
        
        for (int i = 0; i < indices.size(); i++) {
            indexArray[i] = indices.get(i);
        }
        
        return new Mesh(posArray, texArray, normalArray, blockTypeArray, indexArray);
    }
}
