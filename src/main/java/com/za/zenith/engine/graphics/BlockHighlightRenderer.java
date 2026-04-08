package com.za.zenith.engine.graphics;

import com.za.zenith.world.BlockPos;
import com.za.zenith.world.World;
import com.za.zenith.world.blocks.Block;
import com.za.zenith.world.physics.AABB;
import com.za.zenith.world.physics.RaycastResult;
import com.za.zenith.world.physics.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.lwjgl.opengl.GL11.*;

public class BlockHighlightRenderer {
    private final Map<VoxelShape, Mesh> highlightMeshes = new ConcurrentHashMap<>();

    public void render(Camera camera, World world, RaycastResult highlightedBlock, Shader blockShader, Matrix4f modelMatrix, float alpha, BlockPos breakingPos, Block currentBreakingBlock, float wobbleTimer) {
        BlockPos pos = highlightedBlock.getBlockPos();
        Block block = world.getBlock(pos);
        VoxelShape shape = block.getShape();

        if (shape == null || shape.getBoxes().isEmpty()) return;

        Mesh mesh = highlightMeshes.computeIfAbsent(shape, this::createMeshForShape);

        glDepthMask(false);
        glDisable(GL_CULL_FACE);
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        glLineWidth(3.0f);
        
        blockShader.use();
        blockShader.setMatrix4f("projection", camera.getProjectionMatrix());
        blockShader.setMatrix4f("view", camera.getViewMatrix(alpha));
        
        // Vertices are now shifted by -0.5 on X and Z, so we center at pos.x + 0.5 and pos.z + 0.5.
        modelMatrix.identity()
            .translate(pos.x() + 0.5f, pos.y(), pos.z() + 0.5f)
            .scale(1.002f);
            
        blockShader.setMatrix4f("model", modelMatrix);
        blockShader.setInt("highlightPass", 1);
        blockShader.setVector3f("highlightColor", new Vector3f(0.2f, 0.2f, 0.2f));
        
        boolean isProxy = false;
        if (pos.equals(breakingPos) && currentBreakingBlock != null) {
            isProxy = true;
            com.za.zenith.world.blocks.BlockDefinition def = com.za.zenith.world.blocks.BlockRegistry.getBlock(currentBreakingBlock.getType());
            String animName = (def != null && def.getWobbleAnimation() != null) ? def.getWobbleAnimation() : "block_wobble";
            
            com.za.zenith.entities.parkour.animation.AnimationProfile profile = com.za.zenith.entities.parkour.animation.AnimationRegistry.get(animName);
            
            float scaleX = 1.0f, scaleY = 1.0f, scaleZ = 1.0f;
            float offsetX = 0.0f, offsetY = 0.0f, offsetZ = 0.0f;
            float shake = 0.0f;
            
            if (profile != null) {
                float normTimer = wobbleTimer / Math.max(0.001f, profile.getDuration());
                scaleX = profile.evaluate("scale_x", normTimer, 1.0f);
                scaleY = profile.evaluate("scale_y", normTimer, 1.0f);
                scaleZ = profile.evaluate("scale_z", normTimer, 1.0f);
                offsetX = profile.evaluate("offset_x", normTimer, 0.0f);
                offsetY = profile.evaluate("offset_y", normTimer, 0.0f);
                offsetZ = profile.evaluate("offset_z", normTimer, 0.0f);
                shake = profile.evaluate("shake", normTimer, 0.0f);
            }
            
            blockShader.setVector3f("uWobbleScale", new Vector3f(scaleX, scaleY, scaleZ));
            blockShader.setVector3f("uWobbleOffset", new Vector3f(offsetX, offsetY, offsetZ));
            blockShader.setFloat("uWobbleShake", shake);
            blockShader.setFloat("uWobbleTime", wobbleTimer);
        }
        
        blockShader.setBoolean("uIsProxy", isProxy);

        glEnable(GL_POLYGON_OFFSET_LINE);
        glPolygonOffset(-1.0f, -1.0f);
        
        mesh.render(GL_LINES);
        
        glDisable(GL_POLYGON_OFFSET_LINE);
        blockShader.setBoolean("uIsProxy", false);
        blockShader.setInt("highlightPass", 0);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        glEnable(GL_CULL_FACE);
        glDepthMask(true);
    }

    private Mesh createMeshForShape(VoxelShape shape) {
        List<Float> positionsList = new ArrayList<>();
        List<Integer> indicesList = new ArrayList<>();
        int vertexOffset = 0;

        java.util.Set<Float> xSet = new java.util.TreeSet<>();
        java.util.Set<Float> ySet = new java.util.TreeSet<>();
        java.util.Set<Float> zSet = new java.util.TreeSet<>();

        for (AABB box : shape.getBoxes()) {
            xSet.add(box.getMin().x); xSet.add(box.getMax().x);
            ySet.add(box.getMin().y); ySet.add(box.getMax().y);
            zSet.add(box.getMin().z); zSet.add(box.getMax().z);
        }

        Float[] X = xSet.toArray(new Float[0]);
        Float[] Y = ySet.toArray(new Float[0]);
        Float[] Z = zSet.toArray(new Float[0]);
        
        float eps = 0.001f;

        // X-axis segments
        for (int i = 0; i < X.length - 1; i++) {
            float mx = (X[i] + X[i+1]) / 2.0f;
            for (float y : Y) {
                for (float z : Z) {
                    int count = 0;
                    if (contains(shape, mx, y + eps, z + eps)) count++;
                    if (contains(shape, mx, y + eps, z - eps)) count++;
                    if (contains(shape, mx, y - eps, z + eps)) count++;
                    if (contains(shape, mx, y - eps, z - eps)) count++;
                    if (count == 1 || count == 3) {
                        positionsList.add(X[i]); positionsList.add(y); positionsList.add(z);
                        positionsList.add(X[i+1]); positionsList.add(y); positionsList.add(z);
                        indicesList.add(vertexOffset++); indicesList.add(vertexOffset++);
                    }
                }
            }
        }

        // Y-axis segments
        for (int j = 0; j < Y.length - 1; j++) {
            float my = (Y[j] + Y[j+1]) / 2.0f;
            for (float x : X) {
                for (float z : Z) {
                    int count = 0;
                    if (contains(shape, x + eps, my, z + eps)) count++;
                    if (contains(shape, x + eps, my, z - eps)) count++;
                    if (contains(shape, x - eps, my, z + eps)) count++;
                    if (contains(shape, x - eps, my, z - eps)) count++;
                    if (count == 1 || count == 3) {
                        positionsList.add(x); positionsList.add(Y[j]); positionsList.add(z);
                        positionsList.add(x); positionsList.add(Y[j+1]); positionsList.add(z);
                        indicesList.add(vertexOffset++); indicesList.add(vertexOffset++);
                    }
                }
            }
        }

        // Z-axis segments
        for (int k = 0; k < Z.length - 1; k++) {
            float mz = (Z[k] + Z[k+1]) / 2.0f;
            for (float x : X) {
                for (float y : Y) {
                    int count = 0;
                    if (contains(shape, x + eps, y + eps, mz)) count++;
                    if (contains(shape, x + eps, y - eps, mz)) count++;
                    if (contains(shape, x - eps, y + eps, mz)) count++;
                    if (contains(shape, x - eps, y - eps, mz)) count++;
                    if (count == 1 || count == 3) {
                        positionsList.add(x); positionsList.add(y); positionsList.add(Z[k]);
                        positionsList.add(x); positionsList.add(y); positionsList.add(Z[k+1]);
                        indicesList.add(vertexOffset++); indicesList.add(vertexOffset++);
                    }
                }
            }
        }

        float[] posArray = new float[positionsList.size()];
        for (int i = 0; i < positionsList.size(); i += 3) {
            posArray[i] = positionsList.get(i) - 0.5f;
            posArray[i+1] = positionsList.get(i+1);
            posArray[i+2] = positionsList.get(i+2) - 0.5f;
        }

        int[] indArray = new int[indicesList.size()];
        for (int i = 0; i < indicesList.size(); i++) indArray[i] = indicesList.get(i);

        int numVerts = vertexOffset; 
        float[] tcArray = new float[numVerts * 2];
        float[] nArray = new float[numVerts * 3];
        float[] btArray = new float[numVerts];

        return new Mesh(posArray, tcArray, nArray, btArray, indArray);
    }

    private boolean contains(VoxelShape shape, float x, float y, float z) {
        for (AABB box : shape.getBoxes()) {
            if (x >= box.getMin().x && x <= box.getMax().x &&
                y >= box.getMin().y && y <= box.getMax().y &&
                z >= box.getMin().z && z <= box.getMax().z) {
                return true;
            }
        }
        return false;
    }

    public void cleanup() {
        for (Mesh mesh : highlightMeshes.values()) {
            mesh.cleanup();
        }
        highlightMeshes.clear();
    }
}
