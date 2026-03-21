package com.za.minecraft.world.chunks;

import com.za.minecraft.engine.graphics.Mesh;
import com.za.minecraft.world.World;
import com.za.minecraft.world.BlockPos;
import com.za.minecraft.world.blocks.Block;
import com.za.minecraft.world.blocks.Blocks;
import com.za.minecraft.world.physics.AABB;
import com.za.minecraft.world.physics.VoxelShape;
import com.za.minecraft.engine.graphics.DynamicTextureAtlas;
import com.za.minecraft.world.blocks.BlockTextureMapper;
import com.za.minecraft.utils.Direction;
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
        List<Float> neighborData = new ArrayList<>(); // Packed: 4-bits neighbor info
        List<Integer> indices = new ArrayList<>();
        int vertexIndex = 0;

        void addFace(float[] fp, float[] fn, float blockTypeId, float[] fullUv, int face, float ox, float oy, float oz, float neighborMask) {
            for (int v = 0; v < 4; v++) {
                positions.add(fp[v*3] + ox);
                positions.add(fp[v*3+1] + oy);
                positions.add(fp[v*3+2] + oz);
                normals.add(fn[v*3]);
                normals.add(fn[v*3+1]);
                normals.add(fn[v*3+2]);
                blockTypes.add(blockTypeId);
                neighborData.add(neighborMask);
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
            float[] p = new float[positions.size()], t = new float[texCoords.size()], n = new float[normals.size()], b = new float[blockTypes.size()], nd = new float[neighborData.size()];
            int[] ind = new int[indices.size()];
            for(int i=0; i<p.length; i++) p[i]=positions.get(i);
            for(int i=0; i<t.length; i++) t[i]=texCoords.get(i);
            for(int i=0; i<n.length; i++) n[i]=normals.get(i);
            for(int i=0; i<b.length; i++) b[i]=blockTypes.get(i);
            for(int i=0; i<nd.length; i++) nd[i]=neighborData.get(i);
            for(int i=0; i<ind.length; i++) ind[i]=indices.get(i);
            return new Mesh(p, t, n, b, nd, ind); // Added neighborData attribute to Mesh
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
                data.addFace(facePositions[face], FACE_NORMALS[face], (float) block.getType(), BlockTextureMapper.uvFor(block, face, atlas), face, 0, 0, 0, 0);
            }
        }
        return data.build();
    }

    public static ChunkMeshResult generateMesh(Chunk chunk, World world, DynamicTextureAtlas atlas) {
        MeshData opaque = new MeshData();
        MeshData translucent = new MeshData();
        Direction[] directions = Direction.values();
        
        // Neighbor directions for each face (relative coordinates)
        // Correctly mapped to local UVs: [Negative_H, Positive_H, Negative_V, Positive_V]
        // Mask bits: bit0: -H, bit1: +H, bit2: -V, bit3: +V
        int[][][] faceNeighbors = new int[][][]{
            {{-1,0,0}, {1,0,0}, {0,-1,0}, {0,1,0}}, // Face 0: NORTH (+Z) -> lu=vx, lv=vy. -H is -X, +H is +X, -V is -Y, +V is +Y
            {{1,0,0}, {-1,0,0}, {0,-1,0}, {0,1,0}}, // Face 1: SOUTH (-Z) -> lu=1-vx, lv=vy. -H is +X, +H is -X, -V is -Y, +V is +Y
            {{0,0,1}, {0,0,-1}, {0,-1,0}, {0,1,0}}, // Face 2: EAST (+X)  -> lu=1-vz, lv=vy. -H is +Z, +H is -Z, -V is -Y, +V is +Y
            {{0,0,-1}, {0,0,1}, {0,-1,0}, {0,1,0}}, // Face 3: WEST (-X)  -> lu=vz, lv=vy. -H is -Z, +H is +Z, -V is -Y, +V is +Y
            {{-1,0,0}, {1,0,0}, {0,0,1}, {0,0,-1}}, // Face 4: UP (+Y)    -> lu=vx, lv=1-vz. -H is -X, +H is +X, -V is +Z, +V is -Z
            {{-1,0,0}, {1,0,0}, {0,0,-1}, {0,0,1}}  // Face 5: DOWN (-Y)  -> lu=vx, lv=vz. -H is -X, +H is +X, -V is -Z, +V is +Z
        };

        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int y = 0; y < Chunk.CHUNK_HEIGHT; y++) {
                for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                    Block block = chunk.getBlock(x, y, z);
                    if (block.isAir()) continue;
                    com.za.minecraft.world.blocks.BlockDefinition def = com.za.minecraft.world.blocks.BlockRegistry.getBlock(block.getType());
                    if (def.getPlacementType() == com.za.minecraft.world.blocks.PlacementType.CROSS_PLANE) {
                        float[] uvs = BlockTextureMapper.uvFor(block, 0, atlas);
                        float u0 = uvs[0], v0 = uvs[1], u1 = uvs[4], v1 = uvs[5];
                        
                        // Plane 1: (0,0,0) to (1,1,1)
                        addCrossPlane(opaque, (float)x, (float)y, (float)z, 0, 0, 0, 1, 1, 1, u0, v0, u1, v1, (float)block.getType());
                        // Plane 2: (1,0,0) to (0,1,1)
                        addCrossPlane(opaque, (float)x, (float)y, (float)z, 1, 0, 0, 0, 1, 1, u0, v0, u1, v1, (float)block.getType());
                        continue;
                    }

                    VoxelShape shape = block.getShape();
                    if (shape == null) continue;

                    boolean isTranslucent = (block.getType() == com.za.minecraft.world.blocks.Blocks.GLASS.getId());
                    MeshData current = isTranslucent ? translucent : opaque;

                    int worldX = chunk.getPosition().x() * Chunk.CHUNK_SIZE + x;
                    int worldY = y;
                    int worldZ = chunk.getPosition().z() * Chunk.CHUNK_SIZE + z;

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
                        
                        // Use directions enum for neighbor checks
                        for (int face = 0; face < 6; face++) {
                            Direction dir = directions[face];
                            Block neighbor = world.getBlock(worldX + dir.getDx(), worldY + dir.getDy(), worldZ + dir.getDz());
                            
                            boolean drawFace = true;
                            if (neighbor.isAir()) {
                                drawFace = true;
                            } else if (neighbor.isFullCube() && !neighbor.isTransparent()) {
                                drawFace = false;
                            } else if (isTranslucent && neighbor.getType() == block.getType()) {
                                drawFace = false; // Internal faces between same translucent type are not needed
                            } else {
                                drawFace = true;
                            }

                            if (drawFace) {
                                float neighborMask = 0;
                                if (isTranslucent) {
                                    for (int i = 0; i < 4; i++) {
                                        Block n = world.getBlock(worldX + faceNeighbors[face][i][0], worldY + faceNeighbors[face][i][1], worldZ + faceNeighbors[face][i][2]);
                                        if (n.getType() == block.getType()) {
                                            neighborMask += (float)Math.pow(2, i);
                                        }
                                    }
                                }
                                current.addFace(facePositions[face], FACE_NORMALS[face], (float) block.getType(), BlockTextureMapper.uvFor(block, face, atlas), face, (float)x, (float)y, (float)z, neighborMask);
                            }
                        }
                    }
                }
            }
        }
        return new ChunkMeshResult(opaque.build(), translucent.build());
    }

    private static void addCrossPlane(MeshData data, float ox, float oy, float oz, float x0, float y0, float z0, float x1, float y1, float z1, float u0, float v0, float u1, float v1, float blockTypeId) {
        // Делаем модель чуть меньше и центрируем
        float scale = 0.8f;
        float offset = (1.0f - scale) / 2.0f;
        
        float px0 = ox + offset + x0 * scale;
        float py0 = oy + y0 * scale;
        float pz0 = oz + offset + z0 * scale;
        
        float px1 = ox + offset + x1 * scale;
        float py1 = oy + y1 * scale;
        float pz1 = oz + offset + z1 * scale;

        // Вершины для плоскости (двусторонняя)
        float[] pos = {
            px0, py0, pz0,  px1, py0, pz1,  px1, oy + scale, pz1,  px0, oy + scale, pz0
        };
        
        // Нормаль (усредненная вверх для травы/палок)
        float[] norm = {0, 1, 0,  0, 1, 0,  0, 1, 0,  0, 1, 0};
        
        // Добавляем две стороны плоскости
        data.addFace(pos, norm, blockTypeId, new float[]{u0, v0, u1, v0, u1, v1, u0, v1}, 4, 0, 0, 0, 0);
        
        // Обратная сторона (инвертируем порядок вершин для корректного cull face, если он включен)
        float[] posRev = {
            px0, oy + scale, pz0,  px1, oy + scale, pz1,  px1, py0, pz1,  px0, py0, pz0
        };
        data.addFace(posRev, norm, blockTypeId, new float[]{u0, v1, u1, v1, u1, v0, u0, v0}, 4, 0, 0, 0, 0);
    }
}
