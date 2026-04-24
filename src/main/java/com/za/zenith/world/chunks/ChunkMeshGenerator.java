package com.za.zenith.world.chunks;

import com.za.zenith.engine.graphics.Mesh;
import com.za.zenith.world.World;
import com.za.zenith.world.BlockPos;
import com.za.zenith.world.blocks.Block;
import com.za.zenith.world.physics.AABB;
import com.za.zenith.world.physics.VoxelShape;
import com.za.zenith.engine.graphics.DynamicTextureAtlas;
import com.za.zenith.world.blocks.BlockTextureMapper;
import com.za.zenith.utils.Direction;
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
        public final long version;
        public ChunkMeshResult(Mesh opaqueMesh, Mesh translucentMesh, long version) {
            this.opaqueMesh = opaqueMesh;
            this.translucentMesh = translucentMesh;
            this.version = version;
        }
    }

    public record RawMeshData(float[] positions, int posLen, float[] texCoords, int texLen, float[] normals, int normLen, float[] blockTypes, int btLen, float[] neighborData, int ndLen, float[] weights, int wLen, float[] lightData, int lLen, float[] aoData, int aoLen, int[] indices, int idxLen) {
        public Mesh createMesh() {
            return new Mesh(positions, posLen, texCoords, texLen, normals, normLen, blockTypes, btLen, neighborData, ndLen, weights, wLen, lightData, lLen, aoData, aoLen, indices, idxLen);
        }
    }

    public record RawChunkMeshResult(RawMeshData opaque, RawMeshData translucent, long version) {
        public ChunkMeshResult upload() {
            Mesh opaqueMesh = opaque != null ? opaque.createMesh() : null;
            Mesh translucentMesh = translucent != null ? translucent.createMesh() : null;
            return new ChunkMeshResult(opaqueMesh, translucentMesh, version);
        }
    }

    private static class ChunkNeighborhood {
        private final Chunk[][] neighborhood = new Chunk[3][3];
        private final int centerChunkX;
        private final int centerChunkZ;

        public ChunkNeighborhood(World world, int cx, int cz) {
            this.centerChunkX = cx;
            this.centerChunkZ = cz;
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    neighborhood[dx + 1][dz + 1] = world.getChunkInternal(cx + dx, cz + dz);
                }
            }
        }

        public Chunk getChunk(int worldX, int worldZ) {
            int dx = (worldX >> 4) - centerChunkX + 1;
            int dz = (worldZ >> 4) - centerChunkZ + 1;
            if (dx < 0 || dx > 2 || dz < 0 || dz > 2) return null;
            return neighborhood[dx][dz];
        }

        public int getRawBlockData(int x, int y, int z) {
            if (y < 0 || y >= Chunk.CHUNK_HEIGHT) return 0;
            Chunk c = getChunk(x, z);
            if (c == null) return 0;
            return c.getRawBlockData(x & 15, y, z & 15);
        }

        public int getSunlight(int x, int y, int z) {
            if (y >= Chunk.CHUNK_HEIGHT) return 15;
            if (y < 0) return 0;
            Chunk c = getChunk(x, z);
            if (c == null) return (y >= 128) ? 15 : 0;
            return c.getSunlight(x & 15, y, z & 15);
        }

        public int getBlockLight(int x, int y, int z) {
            if (y < 0 || y >= Chunk.CHUNK_HEIGHT) return 0;
            Chunk c = getChunk(x, z);
            if (c == null) return 0;
            return c.getBlockLight(x & 15, y, z & 15);
        }
    }

    private static class MeshData {
        com.za.zenith.utils.FloatArrayList positions = new com.za.zenith.utils.FloatArrayList(8192);
        com.za.zenith.utils.FloatArrayList texCoords = new com.za.zenith.utils.FloatArrayList(8192);
        com.za.zenith.utils.FloatArrayList normals = new com.za.zenith.utils.FloatArrayList(8192);
        com.za.zenith.utils.FloatArrayList blockTypes = new com.za.zenith.utils.FloatArrayList(2048);
        com.za.zenith.utils.FloatArrayList neighborData = new com.za.zenith.utils.FloatArrayList(2048); 
        com.za.zenith.utils.FloatArrayList weights = new com.za.zenith.utils.FloatArrayList(2048);
        com.za.zenith.utils.FloatArrayList lightData = new com.za.zenith.utils.FloatArrayList(4096);
        com.za.zenith.utils.FloatArrayList aoData = new com.za.zenith.utils.FloatArrayList(2048);
        com.za.zenith.utils.IntArrayList indices = new com.za.zenith.utils.IntArrayList(8192);
        int vertexIndex = 0;

        void addFace(float[] fp, float[] fn, float blockTypeId, float[] fullUv, int face, float ox, float oy, float oz, float neighborMask, float overlayLayer, boolean canSway, ChunkNeighborhood neighborhood, int wx, int wy, int wz) {
            float minY = fp[1], maxY = fp[1];
            for (int v = 1; v < 4; v++) {
                minY = Math.min(minY, fp[v*3+1]);
                maxY = Math.max(maxY, fp[v*3+1]);
            }

            int packedPos = 0;
            if (ox >= 0.0f && oy >= 0.0f && oz >= 0.0f) {
                packedPos = (int)ox + ((int)oz) * 16 + ((int)oy) * 256;
            }

            for (int v = 0; v < 4; v++) {
                float px = fp[v*3] + ox;
                float py = fp[v*3+1] + oy;
                float pz = fp[v*3+2] + oz;
                positions.add(px);
                positions.add(py);
                positions.add(pz);
                normals.add(fn[v*3]);
                normals.add(fn[v*3+1]);
                normals.add(fn[v*3+2]);
                blockTypes.add(blockTypeId);
                neighborData.add(neighborMask);
                
                float weight = 0.0f;
                if (canSway) {
                    if (maxY > minY) {
                        weight = (fp[v*3+1] > minY + 0.001f) ? 1.0f : 0.0f;
                    } else {
                        weight = (face == 4) ? 1.0f : 0.0f;
                    }
                }
                weights.add(weight);

                // LIGHT & AO Calculation
                float vx = fp[v*3];
                float vy = fp[v*3+1];
                float vz = fp[v*3+2];
                
                // Determine vertex position relative to block center to find neighbors
                int dx = vx > 0.5f ? 1 : -1;
                int dy = vy > 0.5f ? 1 : -1;
                int dz = vz > 0.5f ? 1 : -1;

                // Simple AO based on neighbors
                float ao = calculateAO(neighborhood, wx, wy, wz, face, vx, vy, vz);
                aoData.add(ao + packedPos * 10.0f);

                // Smooth Lighting
                float[] light = calculateSmoothLight(neighborhood, wx, wy, wz, face, vx, vy, vz);
                lightData.add(light[0]); // Sun
                lightData.add(light[1]); // Block
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
                float topU = fullUv[0] * (1 - lu) + fullUv[3] * lu;
                float topV = fullUv[1] * (1 - lu) + fullUv[4] * lu;
                float botU = fullUv[9] * (1 - lu) + fullUv[6] * lu;
                float botV = fullUv[10] * (1 - lu) + fullUv[7] * lu;
                
                float layer = fullUv[2]; 

                texCoords.add(topU * (1 - lv) + botU * lv);
                texCoords.add(topV * (1 - lv) + botV * lv);
                texCoords.add(layer);
                texCoords.add(overlayLayer);
            }
            for (int idx : FACE_INDICES) indices.add(vertexIndex + idx);
            vertexIndex += 4;
        }

        private float calculateAO(ChunkNeighborhood neighborhood, int x, int y, int z, int face, float vx, float vy, float vz) {
            if (neighborhood == null) return 1.0f;
            
            int nx = (vx > 0.0f) ? 1 : -1;
            int ny = (vy > 0.0f) ? 1 : -1;
            int nz = (vz > 0.0f) ? 1 : -1;

            int side1, side2, corner;
            
            switch (face) {
                case 0: // North (+Z)
                case 1: // South (-Z)
                    int fz = z + (face == 0 ? 1 : -1);
                    side1 = isSolid(neighborhood, x + nx, y, fz) ? 1 : 0;
                    side2 = isSolid(neighborhood, x, y + ny, fz) ? 1 : 0;
                    corner = isSolid(neighborhood, x + nx, y + ny, fz) ? 1 : 0;
                    break;
                case 2: // East (+X)
                case 3: // West (-X)
                    int fx = x + (face == 2 ? 1 : -1);
                    side1 = isSolid(neighborhood, fx, y + ny, z) ? 1 : 0;
                    side2 = isSolid(neighborhood, fx, y, z + nz) ? 1 : 0;
                    corner = isSolid(neighborhood, fx, y + ny, z + nz) ? 1 : 0;
                    break;
                case 4: // Up (+Y)
                case 5: // Down (-Y)
                    int fy = y + (face == 4 ? 1 : -1);
                    side1 = isSolid(neighborhood, x + nx, fy, z) ? 1 : 0;
                    side2 = isSolid(neighborhood, x, fy, z + nz) ? 1 : 0;
                    corner = isSolid(neighborhood, x + nx, fy, z + nz) ? 1 : 0;
                    break;
                default: return 1.0f;
            }

            if (side1 == 1 && side2 == 1) return 0.3f;
            return 1.0f - (side1 + side2 + corner) * 0.2f;
        }

        private boolean isSolid(ChunkNeighborhood neighborhood, int x, int y, int z) {
            if (neighborhood == null) return false;
            int rawData = neighborhood.getRawBlockData(x, y, z);
            int type = rawData >> 8;
            if (type == 0) return false;
            com.za.zenith.world.blocks.BlockDefinition def = com.za.zenith.world.blocks.BlockRegistry.getBlock(type);
            if (def == null) return false;
            if (def.is(com.za.zenith.world.blocks.BlockDefinition.FLAG_LEAVES)) return false;
            return def.is(com.za.zenith.world.blocks.BlockDefinition.FLAG_SOLID) && !def.is(com.za.zenith.world.blocks.BlockDefinition.FLAG_TRANSPARENT);
        }

        private static final int[][] SAMPLES_Z = {{0,0,0}, {1,0,0}, {0,1,0}, {1,1,0}};
        private static final int[][] SAMPLES_Z_NEG = {{0,0,0}, {-1,0,0}, {0,1,0}, {-1,1,0}};
        private static final int[][] SAMPLES_X = {{0,0,0}, {0,1,0}, {0,0,1}, {0,1,1}};
        private static final int[][] SAMPLES_X_NEG = {{0,0,0}, {0,1,0}, {0,0,-1}, {0,1,-1}};
        private static final int[][] SAMPLES_Y = {{0,0,0}, {1,0,0}, {0,0,1}, {1,0,1}};
        private static final int[][] SAMPLES_Y_NEG = {{0,0,0}, {-1,0,0}, {0,0,-1}, {-1,0,-1}};

        private static float[] lightBuffer = new float[2];

        private float[] calculateSmoothLight(ChunkNeighborhood neighborhood, int x, int y, int z, int face, float vx, float vy, float vz) {
            if (neighborhood == null) {
                lightBuffer[0] = 15f; lightBuffer[1] = 0f;
                return lightBuffer;
            }
            
            Direction dir = Direction.values()[face];
            int fx = x + dir.getDx();
            int fy = y + dir.getDy();
            int fz = z + dir.getDz();

            int nx = (vx > 0.0f) ? 1 : -1;
            int ny = (vy > 0.0f) ? 1 : -1;
            int nz = (vz > 0.0f) ? 1 : -1;

            float totalSun = 0;
            float totalBlock = 0;

            float centralSun = neighborhood.getSunlight(fx, fy, fz);
            float centralBlock = neighborhood.getBlockLight(fx, fy, fz);

            int[][] samples;
            if (face == 0) samples = nx > 0 ? SAMPLES_Z : SAMPLES_Z_NEG;
            else if (face == 1) samples = nx > 0 ? SAMPLES_Z : SAMPLES_Z_NEG; // Simplified, sign handled by Dir
            else if (face == 2) samples = nz > 0 ? SAMPLES_X : SAMPLES_X_NEG;
            else if (face == 3) samples = nz > 0 ? SAMPLES_X : SAMPLES_X_NEG;
            else samples = nx > 0 ? SAMPLES_Y : SAMPLES_Y_NEG;
            
            // Re-evaluating: face direction already offsets fx,fy,fz.
            // We need 4 voxels touching the vertex in the plane of the face.
            // Let's use a more robust way without allocations.
            
            for (int i = 0; i < 4; i++) {
                int sx = fx, sy = fy, sz = fz;
                if (face < 2) { // Z face
                    if (i == 1 || i == 3) sx += nx;
                    if (i == 2 || i == 3) sy += ny;
                } else if (face < 4) { // X face
                    if (i == 1 || i == 3) sy += ny;
                    if (i == 2 || i == 3) sz += nz;
                } else { // Y face
                    if (i == 1 || i == 3) sx += nx;
                    if (i == 2 || i == 3) sz += nz;
                }
                
                totalSun += neighborhood.getSunlight(sx, sy, sz);
                totalBlock += neighborhood.getBlockLight(sx, sy, sz);
            }

            lightBuffer[0] = Math.max(centralSun, totalSun * 0.25f);
            lightBuffer[1] = Math.max(centralBlock, totalBlock * 0.25f);
            return lightBuffer;
        }

        void addRawQuad(float[] fp, float[] uv, float[] fn, float blockTypeId, float overlayLayer, boolean canSway, float weightOffset) {
            float minY = fp[1], maxY = fp[1];
            for (int v = 1; v < 4; v++) {
                minY = Math.min(minY, fp[v*3+1]);
                maxY = Math.max(maxY, fp[v*3+1]);
            }

            for (int v = 0; v < 4; v++) {
                positions.add(fp[v*3]);
                positions.add(fp[v*3+1]);
                positions.add(fp[v*3+2]);
                normals.add(fn[v*3]);
                normals.add(fn[v*3+1]);
                normals.add(fn[v*3+2]);
                
                texCoords.add(uv[v*3]);
                texCoords.add(uv[v*3+1]);
                texCoords.add(uv[v*3+2]);
                texCoords.add(overlayLayer); 
                
                blockTypes.add(blockTypeId);
                neighborData.add(0.0f);
                
                float weight = 0.0f;
                if (canSway) {
                    weight = weightOffset + ((fp[v*3+1] > minY + 0.001f) ? 1.0f : 0.0f);
                }
                weights.add(weight);

                // For raw quads (cross planes), use full light
                lightData.add(15f);
                lightData.add(0f);
                aoData.add(1.0f);
            }
            for (int idx : FACE_INDICES) indices.add(vertexIndex + idx);
            vertexIndex += 4;
        }

        RawMeshData buildRaw() {
            if (positions.isEmpty()) return null;
            return new RawMeshData(
                positions.getInternalArray(), positions.size(),
                texCoords.getInternalArray(), texCoords.size(),
                normals.getInternalArray(), normals.size(),
                blockTypes.getInternalArray(), blockTypes.size(),
                neighborData.getInternalArray(), neighborData.size(),
                weights.getInternalArray(), weights.size(),
                lightData.getInternalArray(), lightData.size(),
                aoData.getInternalArray(), aoData.size(),
                indices.getInternalArray(), indices.size()
            ); 
        }

        Mesh build() {
            RawMeshData raw = buildRaw();
            return raw != null ? raw.createMesh() : null;
        }
    }

    public static Mesh generateSingleBlockMesh(Block block, DynamicTextureAtlas atlas, World world, BlockPos pos) {
        MeshData data = new MeshData();
        com.za.zenith.world.blocks.BlockDefinition def = com.za.zenith.world.blocks.BlockRegistry.getBlock(block.getType());
        if (def == null) return null;
        
        boolean isTranslucent = def.hasTag("zenith:glass");
        
        float finalBlockType = (float)block.getType();
        if (def.isTinted()) {
            finalBlockType = -(finalBlockType + 1.0f);
        }

        int wx = (pos != null) ? pos.x() : 0;
        int wy = (pos != null) ? pos.y() : 0;
        int wz = (pos != null) ? pos.z() : 0;

        if (def.getPlacementType() == com.za.zenith.world.blocks.PlacementType.CROSS_PLANE || def.getPlacementType() == com.za.zenith.world.blocks.PlacementType.DOUBLE_PLANT) {
            float[] uvs = BlockTextureMapper.uvFor(block, 0, atlas);
            float overlayLayer = uvs[2];
            float weightOffset = (def.getPlacementType() == com.za.zenith.world.blocks.PlacementType.DOUBLE_PLANT && block.getMetadata() == 1) ? 1.0f : 0.0f;
            addCrossPlane(data, -0.5f, 0, -0.5f, 0, 0, 1, 1, uvs, finalBlockType, overlayLayer, weightOffset);
            addCrossPlane(data, -0.5f, 0, -0.5f, 0, 1, 1, 0, uvs, finalBlockType, overlayLayer, weightOffset);
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
                if (isTranslucent) {
                    faceBlockType = -(faceBlockType + 2000.0f);
                } else if (def.isTinted()) {
                    faceBlockType = -(faceBlockType + 1.0f);
                }
                float overlayLayer = -1.0f;
                if (def.isTinted()) {
                    if (def.getTextures() != null) {
                        String innerKey = def.getTextures().getInner();
                        String sideKey = def.getTextures().getTextureForFace(face);
                        if (face < 4 && innerKey != null && !innerKey.equals(sideKey)) {
                            float[] innerUv = atlas.uvFor(innerKey);
                            if (innerUv != null) {
                                overlayLayer = innerUv[2];
                            }
                        }
                    }
                }
                data.addFace(facePositions[face], FACE_NORMALS[face], faceBlockType, BlockTextureMapper.uvFor(block, face, atlas), face, -0.5f, 0, -0.5f, 0, overlayLayer, def.isSway(), null, wx, wy, wz);
            }
        }
        return data.build();
    }

    public static Mesh generateCustomAABBMesh(Block block, AABB box, DynamicTextureAtlas atlas) {
        MeshData data = new MeshData();
        com.za.zenith.world.blocks.BlockDefinition def = com.za.zenith.world.blocks.BlockRegistry.getBlock(block.getType());
        boolean canSway = def != null && def.isSway();
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
            data.addFace(facePositions[face], FACE_NORMALS[face], (float) block.getType(), BlockTextureMapper.uvFor(block, face, atlas), face, 0, 0, 0, 0, -1.0f, canSway, null, 0, 0, 0);
        }
        return data.build();
    }

    public static Mesh generateHoleMesh(BlockPos pos, World world, DynamicTextureAtlas atlas) {
        MeshData data = new MeshData();
        int[] oppositeFaces = {1, 0, 3, 2, 5, 4}; // N(0)->S(1), S(1)->N(0), E(2)->W(3), W(3)->E(2), U(4)->D(5), D(5)->U(4)
        ChunkNeighborhood neighborhood = new ChunkNeighborhood(world, pos.x() >> 4, pos.z() >> 4);

        for (int face = 0; face < 6; face++) {
            Direction dir = Direction.values()[face];
            BlockPos nPos = new BlockPos(pos.x() + dir.getDx(), pos.y() + dir.getDy(), pos.z() + dir.getDz());
            Block nBlock = world.getBlock(nPos);
            com.za.zenith.world.blocks.BlockDefinition nDef = com.za.zenith.world.blocks.BlockRegistry.getBlock(nBlock.getType());

            if (nBlock.getType() != 0 && nDef != null && nDef.getPlacementType() == com.za.zenith.world.blocks.PlacementType.DEFAULT) {
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
                    float overlayLayer = -1.0f;
                    if (nDef.isTinted()) {
                        faceBlockType = -(faceBlockType + 1.0f);
                        if (nDef.getTextures() != null) {
                            String innerKey = nDef.getTextures().getInner();
                            String sideKey = nDef.getTextures().getTextureForFace(oppFace);
                            if (oppFace < 4 && innerKey != null && !innerKey.equals(sideKey)) {
                                float[] innerUv = atlas.uvFor(innerKey);
                                if (innerUv != null) {
                                    overlayLayer = innerUv[2];
                                }
                            }
                        }
                    }

                    // We render the neighbor's opposite face, shifted by the direction offset
                    float ox = dir.getDx();
                    float oy = dir.getDy();
                    float oz = dir.getDz();

                    data.addFace(facePositions[oppFace], FACE_NORMALS[oppFace], faceBlockType, BlockTextureMapper.uvFor(nBlock, oppFace, atlas), oppFace, ox, oy, oz, 0, overlayLayer, nDef.isSway(), neighborhood, nPos.x(), nPos.y(), nPos.z());
                }
            }
        }

        if (data.positions.isEmpty()) return null;
        return data.build();
    }

    public static ChunkMeshResult generateMesh(Chunk chunk, World world, DynamicTextureAtlas atlas) {
        return generateRawMesh(chunk, world, atlas).upload();
    }

    public static RawChunkMeshResult generateRawMesh(Chunk chunk, World world, DynamicTextureAtlas atlas) {
        MeshData opaque = new MeshData();
        MeshData translucent = new MeshData();
        
        int cx = chunk.getPosition().x();
        int cz = chunk.getPosition().z();
        ChunkNeighborhood neighborhood = new ChunkNeighborhood(world, cx, cz);

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
                    int rawData = chunk.getRawBlockData(x, y, z);
                    int blockType = rawData >> 8;
                    if (blockType == 0) continue;

                    Block block = chunk.getBlock(x, y, z);
                    com.za.zenith.world.blocks.BlockDefinition def = com.za.zenith.world.blocks.BlockRegistry.getBlock(blockType);
                    
                    float finalBlockType = (float)blockType;
                    if (def != null && def.isTinted()) {
                        finalBlockType = -(finalBlockType + 1.0f);
                    }

                    if (def.getPlacementType() == com.za.zenith.world.blocks.PlacementType.CROSS_PLANE || def.getPlacementType() == com.za.zenith.world.blocks.PlacementType.DOUBLE_PLANT) {
                        float[] uvs = BlockTextureMapper.uvFor(block, 0, atlas);
                        float overlayLayer = uvs[2]; 
                        float weightOffset = (def.getPlacementType() == com.za.zenith.world.blocks.PlacementType.DOUBLE_PLANT && block.getMetadata() == 1) ? 1.0f : 0.0f;
                        addCrossPlane(opaque, (float)x, (float)y, (float)z, 0, 0, 1, 1, uvs, finalBlockType, overlayLayer, weightOffset);
                        addCrossPlane(opaque, (float)x, (float)y, (float)z, 0, 1, 1, 0, uvs, finalBlockType, overlayLayer, weightOffset);
                        continue;
                    }

                    VoxelShape shape = block.getShape();
                    if (shape == null) continue;

                    boolean isTranslucent = def != null && def.hasTag("zenith:glass");
                    MeshData current = isTranslucent ? translucent : opaque;

                    int worldX = cx * Chunk.CHUNK_SIZE + x;
                    int worldY = y;
                    int worldZ = cz * Chunk.CHUNK_SIZE + z;

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
                            int nx = worldX + dir.getDx();
                            int ny = worldY + dir.getDy();
                            int nz = worldZ + dir.getDz();
                            
                            int nRaw = neighborhood.getRawBlockData(nx, ny, nz);
                            int nType = nRaw >> 8;
                            
                            boolean drawFace = true;
                            com.za.zenith.world.blocks.BlockDefinition neighborDef = com.za.zenith.world.blocks.BlockRegistry.getBlock(nType);

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
                            } else if (nType == 0 || (neighborDef != null && neighborDef.hasTag("treecapitator"))) {
                                drawFace = true;
                            } else if ((neighborDef == null || !neighborDef.isTransparent()) && !neighborDef.isAlwaysRender()) {
                                drawFace = false; // neighbor is solid full block
                            } else if (isTranslucent && neighborDef != null && neighborDef.hasTag("zenith:glass")) {
                                drawFace = false;
                            } else {
                                drawFace = true;
                            }

                            if (drawFace) {
                                float neighborMask = 0;
                                if (isTranslucent) {
                                    for (int i = 0; i < 4; i++) {
                                        int rawN = neighborhood.getRawBlockData(worldX + faceNeighbors[face][i][0], worldY + faceNeighbors[face][i][1], worldZ + faceNeighbors[face][i][2]);
                                        com.za.zenith.world.blocks.BlockDefinition nDef = com.za.zenith.world.blocks.BlockRegistry.getBlock(rawN >> 8);
                                        if (nDef != null && nDef.hasTag("zenith:glass")) {
                                            neighborMask += (float)Math.pow(2, i);
                                        }
                                    }
                                }
                                
                                float faceBlockType = (float)blockType;
                                float overlayLayer = -1.0f;
                                if (isTranslucent) {
                                    faceBlockType = -(faceBlockType + 2000.0f);
                                } else if (def != null && def.isTinted()) {
                                    faceBlockType = -(faceBlockType + 1.0f);
                                    if (def.getTextures() != null) {
                                        String innerKey = def.getTextures().getInner();
                                        String sideKey = def.getTextures().getTextureForFace(face);
                                        if (face == 4) {
                                            overlayLayer = BlockTextureMapper.uvFor(block, face, atlas)[2];
                                        } else if (face < 4 && innerKey != null && !innerKey.equals(sideKey)) {
                                            float[] innerUv = atlas.uvFor(innerKey);
                                            if (innerUv != null) {
                                                overlayLayer = innerUv[2];
                                            }
                                        }
                                    }
                                }
                                
                                current.addFace(facePositions[face], FACE_NORMALS[face], faceBlockType, BlockTextureMapper.uvFor(block, face, atlas), face, (float)x, (float)y, (float)z, neighborMask, overlayLayer, def.isSway(), neighborhood, worldX, worldY, worldZ);
                            }
                        }
                    }
                }
            }
        }
        return new RawChunkMeshResult(opaque.buildRaw(), translucent.buildRaw(), chunk.getDirtyCounter());
    }

    private static void addCrossPlane(MeshData data, float ox, float oy, float oz, float x0, float z0, float x1, float z1, float[] uvs, float blockTypeId, float overlayLayer, float weightOffset) {
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
            blockTypeId,
            overlayLayer,
            true,
            weightOffset
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
            blockTypeId,
            overlayLayer,
            true,
            weightOffset
        );
    }
}


