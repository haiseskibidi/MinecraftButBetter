package com.za.zenith.world.chunks;

import com.za.zenith.engine.graphics.Mesh;
import com.za.zenith.world.World;
import com.za.zenith.world.BlockPos;
import com.za.zenith.world.blocks.Block;
import com.za.zenith.world.blocks.Blocks;
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
                
                // Simple AO based on neighbors
                float ao = calculateAO(world, wx, wy, wz, face, vx, vy, vz);
                aoData.add(ao);

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
                float topU = (fullUv != null) ? (fullUv[0] * (1 - lu) + fullUv[3] * lu) : 0;
                float topV = (fullUv != null) ? (fullUv[1] * (1 - lu) + fullUv[4] * lu) : 0;
                float botU = (fullUv != null) ? (fullUv[9] * (1 - lu) + fullUv[6] * lu) : 0;
                float botV = (fullUv != null) ? (fullUv[10] * (1 - lu) + fullUv[7] * lu) : 0;
                
                float layer = (fullUv != null) ? fullUv[2] : 0; 

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

            if (side1 && side2) return 0.2f;
            return 1.0f - ((side1 ? 1 : 0) + (side2 ? 1 : 0) + (corner ? 1 : 0)) * 0.25f;
        }

        private boolean isSolid(World world, int x, int y, int z) {
            if (world == null) return false;
            Block b = world.getBlock(x, y, z);
            return b.getType() != 0 && com.za.zenith.world.blocks.BlockRegistry.getBlock(b.getType()).isSolid();
        }

        private float[] calculateSmoothLight(World world, int x, int y, int z, int face, float vx, float vy, float vz) {
            if (world == null) return new float[]{15f, 0f};
            Direction d = Direction.values()[face];
            int fx = x + d.getDx();
            int fy = y + d.getDy();
            int fz = z + d.getDz();

            int nx = (vx > 0.0f) ? 1 : -1;
            int ny = (vy > 0.0f) ? 1 : -1;
            int nz = (vz > 0.0f) ? 1 : -1;

            float totalSun = 0;
            float totalBlock = 0;
            int count = 0;

            int[][] samples;
            switch(face) {
                case 0: case 1: samples = new int[][]{{0,0,0}, {nx,0,0}, {0,ny,0}, {nx,ny,0}}; break;
                case 2: case 3: samples = new int[][]{{0,0,0}, {0,ny,0}, {0,0,nz}, {0,ny,nz}}; break;
                default: samples = new int[][]{{0,0,0}, {nx,0,0}, {0,0,nz}, {nx,0,nz}}; break;
            }

            for (int[] s : samples) {
                int sx = fx + s[0], sy = fy + s[1], sz = fz + s[2];
                totalSun += world.getSunlight(sx, sy, sz);
                totalBlock += world.getBlockLight(sx, sy, sz);
                count++;
            }

            return new float[]{totalSun / count, totalBlock / count};
        }

        void addRawQuad(float[] fp, float[] uv, float[] fn, float blockTypeId, float overlayLayer, boolean canSway, float weightOffset) {
            float minY = fp[1], maxY = fp[1];
            for (int v = 1; v < 4; v++) {
                minY = Math.min(minY, fp[v*3+1]);
                maxY = Math.max(maxY, fp[v*3+1]);
            }
            for (int v = 0; v < 4; v++) {
                positions.add(fp[v*3]); positions.add(fp[v*3+1]); positions.add(fp[v*3+2]);
                normals.add(fn[v*3]); normals.add(fn[v*3+1]); normals.add(fn[v*3+2]);
                texCoords.add(uv[v*3]); texCoords.add(uv[v*3+1]); texCoords.add(uv[v*3+2]); texCoords.add(overlayLayer); 
                blockTypes.add(blockTypeId); neighborData.add(0.0f);
                float weight = canSway ? weightOffset + ((fp[v*3+1] > minY + 0.001f) ? 1.0f : 0.0f) : 0.0f;
                weights.add(weight);
                lightData.add(15f); lightData.add(0f); aoData.add(1.0f);
            }
            for (int idx : FACE_INDICES) indices.add(vertexIndex + idx);
            vertexIndex += 4;
        }

        Mesh build() {
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
            return new Mesh(p, t, n, b, nd, w, l, a, ind); 
        }
    }

    public static Mesh generateSingleBlockMesh(Block block, DynamicTextureAtlas atlas, World world, BlockPos pos) {
        MeshData data = new MeshData();
        com.za.zenith.world.blocks.BlockDefinition def = com.za.zenith.world.blocks.BlockRegistry.getBlock(block.getType());
        if (def == null) return null;
        boolean isTranslucent = def.hasTag("zenith:glass");
        float finalBlockType = (float)block.getType();
        if (def.isTinted()) finalBlockType = -(finalBlockType + 1.0f);
        int wx = (pos != null) ? pos.x() : 0, wy = (pos != null) ? pos.y() : 0, wz = (pos != null) ? pos.z() : 0;

        if (def.getPlacementType() == com.za.zenith.world.blocks.PlacementType.CROSS_PLANE || def.getPlacementType() == com.za.zenith.world.blocks.PlacementType.DOUBLE_PLANT) {
            float[] uvs = BlockTextureMapper.uvFor(block, 0, atlas);
            float weightOffset = (def.getPlacementType() == com.za.zenith.world.blocks.PlacementType.DOUBLE_PLANT && block.getMetadata() == 1) ? 1.0f : 0.0f;
            addCrossPlane(data, -0.5f, 0, -0.5f, 0, 0, 1, 1, uvs, finalBlockType, uvs[2], weightOffset);
            addCrossPlane(data, -0.5f, 0, -0.5f, 0, 1, 1, 0, uvs, finalBlockType, uvs[2], weightOffset);
            return data.build();
        }

        VoxelShape shape = block.getShape();
        if (shape == null) return null;
        for (AABB box : shape.getBoxes()) {
            Vector3f min = box.getMin(), max = box.getMax();
            float[][] facePositions = {{min.x, min.y, max.z,  max.x, min.y, max.z,  max.x, max.y, max.z,  min.x, max.y, max.z}, {max.x, min.y, min.z,  min.x, min.y, min.z,  min.x, max.y, min.z,  max.x, max.y, min.z}, {max.x, min.y, max.z,  max.x, min.y, min.z,  max.x, max.y, min.z,  max.x, max.y, max.z}, {min.x, min.y, min.z,  min.x, min.y, max.z,  min.x, max.y, max.z,  min.x, max.y, min.z}, {min.x, max.y, max.z,  max.x, max.y, max.z,  max.x, max.y, min.z,  min.x, max.y, min.z}, {min.x, min.y, min.z,  max.x, min.y, min.z,  max.x, min.y, max.z,  min.x, min.y, max.z}};
            for (int face = 0; face < 6; face++) {
                float fType = finalBlockType;
                if (isTranslucent) fType = -(fType + 2000.0f);
                float overlay = -1.0f;
                if (def.isTinted() && def.getTextures() != null) {
                    String sideKey = def.getTextures().getTextureForFace(face);
                    if (face < 4 && def.getTextures().getInner() != null && !def.getTextures().getInner().equals(sideKey)) {
                        float[] innerUv = atlas.uvFor(def.getTextures().getInner());
                        if (innerUv != null) overlay = innerUv[2];
                    }
                }
                data.addFace(facePositions[face], FACE_NORMALS[face], fType, BlockTextureMapper.uvFor(block, face, atlas), face, -0.5f, 0, -0.5f, 0, overlay, def.isSway(), world, wx, wy, wz);
            }
        }
        return data.build();
    }

    public static Mesh generateHoleMesh(BlockPos pos, World world, DynamicTextureAtlas atlas) {
        MeshData data = new MeshData();
        Block block = world.getBlock(pos);
        com.za.zenith.world.blocks.BlockDefinition def = com.za.zenith.world.blocks.BlockRegistry.getBlock(block.getType());
        if (def == null) return null;
        VoxelShape shape = block.getShape();
        if (shape == null) return null;
        for (AABB box : shape.getBoxes()) {
            Vector3f min = box.getMin(), max = box.getMax();
            float[][] facePositions = {{min.x, min.y, max.z,  max.x, min.y, max.z,  max.x, max.y, max.z,  min.x, max.y, max.z}, {max.x, min.y, min.z,  min.x, min.y, min.z,  min.x, max.y, min.z,  max.x, max.y, min.z}, {max.x, min.y, max.z,  max.x, min.y, min.z,  max.x, max.y, min.z,  max.x, max.y, max.z}, {min.x, min.y, min.z,  min.x, min.y, max.z,  min.x, max.y, max.z,  min.x, max.y, min.z}, {min.x, max.y, max.z,  max.x, max.y, max.z,  max.x, max.y, min.z,  min.x, max.y, min.z}, {min.x, min.y, min.z,  max.x, min.y, min.z,  max.x, min.y, max.z,  min.x, min.y, max.z}};
            for (int face = 0; face < 6; face++) {
                data.addFace(facePositions[face], FACE_NORMALS[face], (float)block.getType(), BlockTextureMapper.uvFor(block, face, atlas), face, -0.5f, 0, -0.5f, 0, -1.0f, def.isSway(), world, pos.x(), pos.y(), pos.z());
            }
        }
        return data.build();
    }

    public static ChunkMeshResult generateMesh(Chunk chunk, World world, DynamicTextureAtlas atlas) {
        MeshData opaque = new MeshData(), translucent = new MeshData();
        int[][][] faceNeighbors = {{{-1,0,0}, {1,0,0}, {0,-1,0}, {0,1,0}}, {{1,0,0}, {-1,0,0}, {0,-1,0}, {0,1,0}}, {{0,0,1}, {0,0,-1}, {0,-1,0}, {0,1,0}}, {{0,0,-1}, {0,0,1}, {0,-1,0}, {0,1,0}}, {{-1,0,0}, {1,0,0}, {0,0,1}, {0,0,-1}}, {{-1,0,0}, {1,0,0}, {0,0,-1}, {0,0,1}}};
        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int y = 0; y < Chunk.CHUNK_HEIGHT; y++) {
                for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                    Block block = chunk.getBlock(x, y, z);
                    if (block.isAir()) continue;
                    com.za.zenith.world.blocks.BlockDefinition def = com.za.zenith.world.blocks.BlockRegistry.getBlock(block.getType());
                    VoxelShape shape = block.getShape();
                    if (shape == null) continue;
                    boolean isTranslucent = def.hasTag("zenith:glass");
                    MeshData current = isTranslucent ? translucent : opaque;
                    int worldX = chunk.getPosition().x() * Chunk.CHUNK_SIZE + x, worldY = y, worldZ = chunk.getPosition().z() * Chunk.CHUNK_SIZE + z;
                    for (AABB box : shape.getBoxes()) {
                        Vector3f min = box.getMin(), max = box.getMax();
                        float[][] facePositions = {{min.x, min.y, max.z,  max.x, min.y, max.z,  max.x, max.y, max.z,  min.x, max.y, max.z}, {max.x, min.y, min.z,  min.x, min.y, min.z,  min.x, max.y, min.z,  max.x, max.y, min.z}, {max.x, min.y, max.z,  max.x, min.y, min.z,  max.x, max.y, min.z,  max.x, max.y, max.z}, {min.x, min.y, min.z,  min.x, min.y, max.z,  min.x, max.y, max.z,  min.x, max.y, min.z}, {min.x, max.y, max.z,  max.x, max.y, max.z,  max.x, max.y, min.z,  min.x, max.y, min.z}, {min.x, min.y, min.z,  max.x, min.y, min.z,  max.x, min.y, max.z,  min.x, min.y, max.z}};
                        for (int face = 0; face < 6; face++) {
                            Direction dir = Direction.values()[face];
                            Block neighbor = world.getBlock(worldX + dir.getDx(), worldY + dir.getDy(), worldZ + dir.getDz());
                            com.za.zenith.world.blocks.BlockDefinition nDef = com.za.zenith.world.blocks.BlockRegistry.getBlock(neighbor.getType());
                            boolean drawFace = true, onBoundary = false;
                            switch (face) { case 0: onBoundary = (box.getMax().z == 1.0f); break; case 1: onBoundary = (box.getMin().z == 0.0f); break; case 2: onBoundary = (box.getMax().x == 1.0f); break; case 3: onBoundary = (box.getMin().x == 0.0f); break; case 4: onBoundary = (box.getMax().y == 1.0f); break; case 5: onBoundary = (box.getMin().y == 0.0f); break; }
                            if (def.isAlwaysRender() || !onBoundary) drawFace = true;
                            else if (neighbor.isAir() || (nDef != null && nDef.hasTag("treecapitator"))) drawFace = true;
                            else if (neighbor.isFullCube() && !neighbor.isTransparent() && !nDef.isAlwaysRender()) drawFace = false;
                            else drawFace = !(isTranslucent && nDef != null && nDef.hasTag("zenith:glass"));
                            if (drawFace) {
                                float nMask = 0;
                                if (isTranslucent) { for (int i = 0; i < 4; i++) { if (com.za.zenith.world.blocks.BlockRegistry.getBlock(world.getBlock(worldX + faceNeighbors[face][i][0], worldY + faceNeighbors[face][i][1], worldZ + faceNeighbors[face][i][2]).getType()).hasTag("zenith:glass")) nMask += (float)Math.pow(2, i); } }
                                float fType = (float)block.getType(), overlay = -1.0f;
                                if (isTranslucent) fType = -(fType + 2000.0f);
                                else if (def.isTinted()) {
                                    fType = -(fType + 1.0f);
                                    if (def.getTextures() != null) {
                                        if (face == 4) overlay = BlockTextureMapper.uvFor(block, face, atlas)[2];
                                        else if (face < 4 && def.getTextures().getInner() != null && !def.getTextures().getInner().equals(def.getTextures().getTextureForFace(face))) { float[] iuv = atlas.uvFor(def.getTextures().getInner()); if (iuv != null) overlay = iuv[2]; }
                                    }
                                }
                                current.addFace(facePositions[face], FACE_NORMALS[face], fType, BlockTextureMapper.uvFor(block, face, atlas), face, (float)x, (float)y, (float)z, nMask, overlay, def.isSway(), world, worldX, worldY, worldZ);
                            }
                        }
                    }
                }
            }
        }
        return new ChunkMeshResult(opaque.build(), translucent.build());
    }

    private static void addCrossPlane(MeshData data, float ox, float oy, float oz, float x0, float z0, float x1, float z1, float[] uvs, float blockTypeId, float overlayLayer, float weightOffset) {
        float l = uvs[2];
        data.addRawQuad(new float[]{ox+x0, oy, oz+z0,  ox+x1, oy, oz+z1,  ox+x1, oy+1.0f, oz+z1,  ox+x0, oy+1.0f, oz+z0}, new float[]{uvs[0], uvs[1], l, uvs[3], uvs[4], l, uvs[3], uvs[7], l, uvs[0], uvs[10], l}, new float[]{0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0}, blockTypeId, overlayLayer, true, weightOffset);
        data.addRawQuad(new float[]{ox+x0, oy+1.0f, oz+z0,  ox+x1, oy+1.0f, oz+z1,  ox+x1, oy, oz+z1,  ox+x0, oy, oz+z0}, new float[]{uvs[0], uvs[10], l, uvs[3], uvs[7], l, uvs[3], uvs[4], l, uvs[0], uvs[1], l}, new float[]{0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0}, blockTypeId, overlayLayer, true, weightOffset);
    }
}
