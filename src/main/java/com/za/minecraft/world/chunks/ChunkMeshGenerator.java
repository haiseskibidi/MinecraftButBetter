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
                // fullUv now contains 12 values: U, V, W for 4 vertices
                // index: 0,1,2 (V0), 3,4,5 (V1), 6,7,8 (V2), 9,10,11 (V3)
                float topU = fullUv[0] * (1 - lu) + fullUv[3] * lu;
                float topV = fullUv[1] * (1 - lu) + fullUv[4] * lu;
                float botU = fullUv[9] * (1 - lu) + fullUv[6] * lu;
                float botV = fullUv[10] * (1 - lu) + fullUv[7] * lu;
                
                float layer = fullUv[2]; // Layer is constant for all vertices of one face

                texCoords.add(topU * (1 - lv) + botU * lv);
                texCoords.add(topV * (1 - lv) + botV * lv);
                texCoords.add(layer);
            }
            for (int idx : FACE_INDICES) indices.add(vertexIndex + idx);
            vertexIndex += 4;
        }

        void addRawQuad(float[] fp, float[] uv, float[] fn, float blockTypeId) {
            for (int v = 0; v < 4; v++) {
                positions.add(fp[v*3]);
                positions.add(fp[v*3+1]);
                positions.add(fp[v*3+2]);
                normals.add(fn[v*3]);
                normals.add(fn[v*3+1]);
                normals.add(fn[v*3+2]);
                // uv is now 12 values (UVW per vertex)
                texCoords.add(uv[v*3]);
                texCoords.add(uv[v*3+1]);
                texCoords.add(uv[v*3+2]);
                blockTypes.add(blockTypeId);
                neighborData.add(0.0f);
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
        com.za.minecraft.world.blocks.BlockDefinition def = com.za.minecraft.world.blocks.BlockRegistry.getBlock(block.getType());
        
        float finalBlockType = (float)block.getType();
        if (def != null && def.isTinted()) {
            finalBlockType = -(finalBlockType + 1.0f);
        }

        if (def.getPlacementType() == com.za.minecraft.world.blocks.PlacementType.CROSS_PLANE || def.getPlacementType() == com.za.minecraft.world.blocks.PlacementType.DOUBLE_PLANT) {
            float[] uvs = BlockTextureMapper.uvFor(block, 0, atlas);
            addCrossPlane(data, 0, 0, 0, 0, 0, 1, 1, uvs, finalBlockType);
            addCrossPlane(data, 0, 0, 0, 0, 1, 1, 0, uvs, finalBlockType);
            return data.build();
        }

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
                float faceBlockType = (float)block.getType();
                if (def != null && def.isTinted()) {
                    boolean isGrassBlock = def.getIdentifier().getPath().contains("grass_block");
                    if (!isGrassBlock || face == 4) {
                        faceBlockType = -(faceBlockType + 1.0f);
                    }
                }
                data.addFace(facePositions[face], FACE_NORMALS[face], faceBlockType, BlockTextureMapper.uvFor(block, face, atlas), face, -0.5f, 0, -0.5f, 0);
            }
        }
        return data.build();
    }

    public static Mesh generateCustomAABBMesh(Block block, AABB box, DynamicTextureAtlas atlas) {
        MeshData data = new MeshData();
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
        return data.build();
    }

    public static Mesh generateHoleMesh(com.za.minecraft.world.BlockPos pos, com.za.minecraft.world.World world, DynamicTextureAtlas atlas) {
        MeshData data = new MeshData();
        int[] oppositeFaces = {1, 0, 3, 2, 5, 4}; // N(0)->S(1), S(1)->N(0), E(2)->W(3), W(3)->E(2), U(4)->D(5), D(5)->U(4)
        
        for (int face = 0; face < 6; face++) {
            com.za.minecraft.utils.Direction dir = com.za.minecraft.utils.Direction.values()[face];
            com.za.minecraft.world.BlockPos nPos = pos.offset(dir.getDx(), dir.getDy(), dir.getDz());
            Block nBlock = world.getBlock(nPos);
            com.za.minecraft.world.blocks.BlockDefinition nDef = com.za.minecraft.world.blocks.BlockRegistry.getBlock(nBlock.getType());
            
            if (nBlock.getType() != 0 && nDef != null && nDef.getPlacementType() == com.za.minecraft.world.blocks.PlacementType.DEFAULT) {
                int oppFace = oppositeFaces[face];
                
                VoxelShape shape = nBlock.getShape();
                if (shape == null) continue;
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
                    
                    float faceBlockType = (float)nBlock.getType();
                    if (nDef.isTinted()) {
                        boolean isGrassBlock = nDef.getIdentifier().getPath().contains("grass_block");
                        if (!isGrassBlock || oppFace == 4) {
                            faceBlockType = -(faceBlockType + 1.0f);
                        }
                    }
                    
                    // We render the neighbor's opposite face, shifted by the direction offset
                    float ox = dir.getDx();
                    float oy = dir.getDy();
                    float oz = dir.getDz();
                    
                    data.addFace(facePositions[oppFace], FACE_NORMALS[oppFace], faceBlockType, BlockTextureMapper.uvFor(nBlock, oppFace, atlas), oppFace, ox, oy, oz, 0);
                }
            }
        }
        
        if (data.positions.isEmpty()) return null;
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
                    
                    float finalBlockType = (float)block.getType();
                    if (def != null && def.isTinted()) {
                        finalBlockType = -(finalBlockType + 1.0f);
                    }

                    if (def.getPlacementType() == com.za.minecraft.world.blocks.PlacementType.CROSS_PLANE || def.getPlacementType() == com.za.minecraft.world.blocks.PlacementType.DOUBLE_PLANT) {
                        float[] uvs = BlockTextureMapper.uvFor(block, 0, atlas);
                        addCrossPlane(opaque, (float)x, (float)y, (float)z, 0, 0, 1, 1, uvs, finalBlockType);
                        addCrossPlane(opaque, (float)x, (float)y, (float)z, 0, 1, 1, 0, uvs, finalBlockType);
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
                        
                    for (int face = 0; face < 6; face++) {
                        Direction dir = Direction.values()[face];
                        BlockPos nPos = new BlockPos(worldX + dir.getDx(), worldY + dir.getDy(), worldZ + dir.getDz());
                        Block neighbor = world.getBlock(nPos);
                        
                        boolean drawFace = true;
                        com.za.minecraft.world.blocks.BlockDefinition neighborDef = com.za.minecraft.world.blocks.BlockRegistry.getBlock(neighbor.getType());

                        boolean onBoundary = false;
                        switch (face) {
                            case 0: onBoundary = (box.getMax().z == 1.0f); break; 
                            case 1: onBoundary = (box.getMin().z == 0.0f); break; 
                            case 2: onBoundary = (box.getMax().x == 1.0f); break; 
                            case 3: onBoundary = (box.getMin().x == 0.0f); break; 
                            case 4: onBoundary = (box.getMax().y == 1.0f); break; 
                            case 5: onBoundary = (box.getMin().y == 0.0f); break; 
                        }

                        if (def.isAlwaysRender() || !onBoundary) {
                            drawFace = true;
                        } else if (neighbor.isAir() || (neighborDef != null && neighborDef.hasTag("treecapitator"))) {
                            drawFace = true;
                        } else if (neighbor.isFullCube() && !neighbor.isTransparent() && !neighborDef.isAlwaysRender()) {
                            drawFace = false;
                        } else if (isTranslucent && neighbor.getType() == block.getType()) {
                            drawFace = false;
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
                            
                            // APPLY TINT: Only top face for grass blocks, all faces for others (like leaves)
                            float faceBlockType = (float)block.getType();
                            if (def != null && def.isTinted()) {
                                boolean isGrassBlock = def.getIdentifier().getPath().contains("grass_block");
                                if (!isGrassBlock || face == 4) {
                                    faceBlockType = -(faceBlockType + 1.0f);
                                }
                            }
                            
                            current.addFace(facePositions[face], FACE_NORMALS[face], faceBlockType, BlockTextureMapper.uvFor(block, face, atlas), face, (float)x, (float)y, (float)z, neighborMask);
                        }
                    }
                    }
                }
            }
        }
        return new ChunkMeshResult(opaque.build(), translucent.build());
    }

    private static void addCrossPlane(MeshData data, float ox, float oy, float oz, float x0, float z0, float x1, float z1, float[] uvs, float blockTypeId) {
        float l = uvs[2];
        data.addRawQuad(
            new float[]{ox+x0, oy, oz+z0,  ox+x1, oy, oz+z1,  ox+x1, oy+1.0f, oz+z1,  ox+x0, oy+1.0f, oz+z0},
            new float[]{
                uvs[0], uvs[1], l,
                uvs[3], uvs[4], l,
                uvs[3], uvs[7], l,
                uvs[0], uvs[10], l
            },
            new float[]{0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0},
            blockTypeId
        );
        data.addRawQuad(
            new float[]{ox+x0, oy+1.0f, oz+z0,  ox+x1, oy+1.0f, oz+z1,  ox+x1, oy, oz+z1,  ox+x0, oy, oz+z0},
            new float[]{
                uvs[0], uvs[10], l,
                uvs[3], uvs[7], l,
                uvs[3], uvs[4], l,
                uvs[0], uvs[1], l
            },
            new float[]{0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0},
            blockTypeId
        );
    }
}
