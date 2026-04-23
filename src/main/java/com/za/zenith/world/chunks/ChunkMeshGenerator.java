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

    public record RawMeshData(float[] positions, float[] texCoords, float[] normals, float[] blockTypes, float[] neighborData, float[] weights, float[] lightData, float[] aoData, int[] indices) {
        public Mesh createMesh() {
            return new Mesh(positions, texCoords, normals, blockTypes, neighborData, weights, lightData, aoData, indices);
        }
    }

    public record RawChunkMeshResult(RawMeshData opaque, RawMeshData translucent, long version) {
        public ChunkMeshResult upload() {
            Mesh opaqueMesh = opaque != null ? opaque.createMesh() : null;
            Mesh translucentMesh = translucent != null ? translucent.createMesh() : null;
            return new ChunkMeshResult(opaqueMesh, translucentMesh, version);
        }
    }

    private static class MeshData {
        List<Float> positions = new ArrayList<>();
        List<Float> texCoords = new ArrayList<>();
        List<Float> normals = new ArrayList<>();
        List<Float> blockTypes = new ArrayList<>();
        List<Float> neighborData = new ArrayList<>(); 
        List<Float> weights = new ArrayList<>();
        List<Float> lightData = new ArrayList<>();
        List<Float> aoData = new ArrayList<>();
        List<Integer> indices = new ArrayList<>();
        int vertexIndex = 0;

        void addFace(float[] fp, float[] fn, float blockTypeId, float[] fullUv, int face, float ox, float oy, float oz, float neighborMask, float overlayLayer, boolean canSway, World world, int wx, int wy, int wz) {
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
                float ao = calculateAO(world, wx, wy, wz, face, vx, vy, vz);
                aoData.add(ao + packedPos * 10.0f);

                // Smooth Lighting
                float[] light = calculateSmoothLight(world, wx, wy, wz, face, vx, vy, vz);
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

        private float calculateAO(World world, int x, int y, int z, int face, float vx, float vy, float vz) {
            if (world == null) return 1.0f;
            
            int nx = (vx > 0.0f) ? 1 : -1;
            int ny = (vy > 0.0f) ? 1 : -1;
            int nz = (vz > 0.0f) ? 1 : -1;

            boolean side1 = false, side2 = false, corner = false;
            
            switch (face) {
                case 0: // North (+Z)
                case 1: // South (-Z)
                    int fz = z + (face == 0 ? 1 : -1);
                    side1 = isSolid(world, x + nx, y, fz);
                    side2 = isSolid(world, x, y + ny, fz);
                    corner = isSolid(world, x + nx, y + ny, fz);
                    break;
                case 2: // East (+X)
                case 3: // West (-X)
                    int fx = x + (face == 2 ? 1 : -1);
                    side1 = isSolid(world, fx, y + ny, z);
                    side2 = isSolid(world, fx, y, z + nz);
                    corner = isSolid(world, fx, y + ny, z + nz);
                    break;
                case 4: // Up (+Y)
                case 5: // Down (-Y)
                    int fy = y + (face == 4 ? 1 : -1);
                    side1 = isSolid(world, x + nx, fy, z);
                    side2 = isSolid(world, x, fy, z + nz);
                    corner = isSolid(world, x + nx, fy, z + nz);
                    break;
            }

            if (side1 && side2) return 0.3f;
            return 1.0f - ((side1 ? 1 : 0) + (side2 ? 1 : 0) + (corner ? 1 : 0)) * 0.2f;
        }

        private boolean isSolid(World world, int x, int y, int z) {
            if (world == null) return false;
            Block b = world.getBlock(x, y, z);
            if (b.getType() == 0) return false;
            com.za.zenith.world.blocks.BlockDefinition def = com.za.zenith.world.blocks.BlockRegistry.getBlock(b.getType());
            // Leaves are semi-transparent for AO to keep trees bright
            if (def != null && def.getIdentifier().toString().contains("leaves")) return false;
            return def != null && def.isSolid() && !def.isTransparent();
        }

        private float[] calculateSmoothLight(World world, int x, int y, int z, int face, float vx, float vy, float vz) {
            if (world == null) return new float[]{15f, 0f};
            // Face normal direction
            com.za.zenith.utils.Direction dir = com.za.zenith.utils.Direction.values()[face];
            int fx = x + dir.getDx();
            int fy = y + dir.getDy();
            int fz = z + dir.getDz();

            // Vertex offset directions
            int nx = (vx > 0.0f) ? 1 : -1;
            int ny = (vy > 0.0f) ? 1 : -1;
            int nz = (vz > 0.0f) ? 1 : -1;

            float totalSun = 0;
            float totalBlock = 0;
            int count = 0;

            // Cell-Biased Lighting: Get the light level of the block the face is pointing to
            float centralSun = world.getSunlight(fx, fy, fz);
            float centralBlock = world.getBlockLight(fx, fy, fz);

            // Sample 4 voxels around the vertex in the face plane
            int[][] samples;
            switch(face) {
                case 0: case 1: // Z face
                    samples = new int[][]{{0,0,0}, {nx,0,0}, {0,ny,0}, {nx,ny,0}}; break;
                case 2: case 3: // X face
                    samples = new int[][]{{0,0,0}, {0,ny,0}, {0,0,nz}, {0,ny,nz}}; break;
                default: // Y face
                    samples = new int[][]{{0,0,0}, {nx,0,0}, {0,0,nz}, {nx,0,nz}}; break;
            }

            for (int[] s : samples) {
                int sx = fx + s[0];
                int sy = fy + s[1];
                int sz = fz + s[2];
                
                Chunk chunk = world.getChunk(com.za.zenith.world.chunks.ChunkPos.fromBlockPos(sx, sz));
                if (chunk != null && sy >= 0 && sy < Chunk.CHUNK_HEIGHT) {
                    totalSun += chunk.getSunlight(sx & 15, sy, sz & 15);
                    totalBlock += chunk.getBlockLight(sx & 15, sy, sz & 15);
                } else {
                    totalSun += (sy >= 128) ? 15 : 0; // Unloaded chunks are bright only at high altitudes
                    totalBlock += 0;
                }
                count++;
            }

            if (count == 0) return new float[]{centralSun, centralBlock};
            
            // Apply bias: Vertex can't be darker than the block it touches
            return new float[]{
                Math.max(centralSun, totalSun / count),
                Math.max(centralBlock, totalBlock / count)
            };
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
            float[] p = new float[positions.size()], t = new float[texCoords.size()], n = new float[normals.size()], b = new float[blockTypes.size()], nd = new float[neighborData.size()], w = new float[weights.size()], l = new float[lightData.size()], a = new float[aoData.size()];
            int[] ind = new int[indices.size()];
            for(int i=0; i<p.length; i++) p[i]=positions.get(i);
            for(int i=0; i<t.length; i++) t[i]=texCoords.get(i);
            for(int i=0; i<n.length; i++) n[i]=normals.get(i);
            for(int i=0; i<b.length; i++) b[i]=blockTypes.get(i);
            for(int i=0; i<nd.length; i++) nd[i]=neighborData.get(i);
            for(int i=0; i<w.length; i++) w[i]=weights.get(i);
            for(int i=0; i<l.length; i++) l[i]=lightData.get(i);
            for(int i=0; i<a.length; i++) a[i]=aoData.get(i);
            for(int i=0; i<ind.length; i++) ind[i]=indices.get(i);
            return new RawMeshData(p, t, n, b, nd, w, l, a, ind); 
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
                data.addFace(facePositions[face], FACE_NORMALS[face], faceBlockType, BlockTextureMapper.uvFor(block, face, atlas), face, -0.5f, 0, -0.5f, 0, overlayLayer, def.isSway(), world, wx, wy, wz);
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

                    data.addFace(facePositions[oppFace], FACE_NORMALS[oppFace], faceBlockType, BlockTextureMapper.uvFor(nBlock, oppFace, atlas), oppFace, ox, oy, oz, 0, overlayLayer, nDef.isSway(), world, nPos.x(), nPos.y(), nPos.z());
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

                    com.za.zenith.world.blocks.BlockDefinition def = com.za.zenith.world.blocks.BlockRegistry.getBlock(block.getType());
                    
                    float finalBlockType = (float)block.getType();
                    if (def != null && def.isTinted()) {
                        finalBlockType = -(finalBlockType + 1.0f);
                    }

                    if (def.getPlacementType() == com.za.zenith.world.blocks.PlacementType.CROSS_PLANE || def.getPlacementType() == com.za.zenith.world.blocks.PlacementType.DOUBLE_PLANT) {
                        float[] uvs = BlockTextureMapper.uvFor(block, 0, atlas);
                        float overlayLayer = uvs[2]; // Для травы используем её основной слой как оверлей для анимации
                        float weightOffset = (def.getPlacementType() == com.za.zenith.world.blocks.PlacementType.DOUBLE_PLANT && block.getMetadata() == 1) ? 1.0f : 0.0f;
                        addCrossPlane(opaque, (float)x, (float)y, (float)z, 0, 0, 1, 1, uvs, finalBlockType, overlayLayer, weightOffset);
                        addCrossPlane(opaque, (float)x, (float)y, (float)z, 0, 1, 1, 0, uvs, finalBlockType, overlayLayer, weightOffset);
                        continue;
                    }

                    VoxelShape shape = block.getShape();
                    if (shape == null) continue;

                    boolean isTranslucent = def != null && def.hasTag("zenith:glass");
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
                        com.za.zenith.world.blocks.BlockDefinition neighborDef = com.za.zenith.world.blocks.BlockRegistry.getBlock(neighbor.getType());

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
                        } else if (isTranslucent && neighborDef != null && neighborDef.hasTag("zenith:glass")) {
                            drawFace = false;
                        } else {
                            drawFace = true;
                        }

                        if (drawFace) {
                            float neighborMask = 0;
                            if (isTranslucent) {
                                for (int i = 0; i < 4; i++) {
                                    Block n = world.getBlock(worldX + faceNeighbors[face][i][0], worldY + faceNeighbors[face][i][1], worldZ + faceNeighbors[face][i][2]);
                                    com.za.zenith.world.blocks.BlockDefinition nDef = com.za.zenith.world.blocks.BlockRegistry.getBlock(n.getType());
                                    if (nDef != null && nDef.hasTag("zenith:glass")) {
                                        neighborMask += (float)Math.pow(2, i);
                                    }
                                }
                            }
                            
                            // APPLY TINT & GLASS FLAGS for shader
                            float faceBlockType = (float)block.getType();
                            float overlayLayer = -1.0f;
                            if (isTranslucent) {
                                // Glass flag: offset by -2000
                                faceBlockType = -(faceBlockType + 2000.0f);
                            } else if (def != null && def.isTinted()) {
                                faceBlockType = -(faceBlockType + 1.0f);
                                if (def.getTextures() != null) {
                                    String innerKey = def.getTextures().getInner();
                                    String sideKey = def.getTextures().getTextureForFace(face);
                                    if (face == 4) {
                                        // Top face always sways if tinted
                                        overlayLayer = BlockTextureMapper.uvFor(block, face, atlas)[2];
                                    } else if (face < 4 && innerKey != null && !innerKey.equals(sideKey)) {
                                        float[] innerUv = atlas.uvFor(innerKey);
                                        if (innerUv != null) {
                                            overlayLayer = innerUv[2];
                                        }
                                    }
                                }
                            }
                            
                            current.addFace(facePositions[face], FACE_NORMALS[face], faceBlockType, BlockTextureMapper.uvFor(block, face, atlas), face, (float)x, (float)y, (float)z, neighborMask, overlayLayer, def.isSway(), world, worldX, worldY, worldZ);
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


