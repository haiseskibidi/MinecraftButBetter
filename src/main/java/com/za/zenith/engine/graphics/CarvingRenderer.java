package com.za.zenith.engine.graphics;

import com.za.zenith.world.BlockPos;
import org.joml.Matrix4f;

import com.za.zenith.world.blocks.entity.BlockEntity;
import com.za.zenith.world.blocks.entity.StumpBlockEntity;
import com.za.zenith.world.blocks.BlockRegistry;
import com.za.zenith.world.blocks.BlockDefinition;
import com.za.zenith.world.blocks.Block;

/**
 * Универсальный рендерер динамических блоков (обтёсывание).
 */
public class CarvingRenderer {
    private Mesh fullFaceMesh;

    public void render(BlockEntity be, DynamicTextureAtlas atlas, Shader shader, Matrix4f modelMatrix, Renderer renderer, BlockPos breakingPos, float wobbleTimer) {
        if (be instanceof StumpBlockEntity stump) {
            renderStump(stump, atlas, shader, modelMatrix, breakingPos, wobbleTimer);
        }
    }

    private void renderStump(StumpBlockEntity stump, DynamicTextureAtlas atlas, Shader shader, Matrix4f modelMatrix, BlockPos breakingPos, float wobbleTimer) {
        int mask = stump.getCarvingMask();
        BlockPos pos = stump.getPos();
        
        if (stump.getWorld() == null) return;
        int blockType = stump.getWorld().getBlock(pos).getType();
        com.za.zenith.world.blocks.BlockTextures textures = com.za.zenith.world.blocks.BlockRegistry.getTextures(blockType);
        if (textures == null || textures.getTop() == null) return;
        
        float[] uv = atlas.uvFor(textures.getTop());
        if (uv == null) return;

        if (fullFaceMesh == null) {
            createFullFaceMesh();
        }

        shader.setBoolean("useMask", true);
        shader.setInt("faceMask", mask);
        shader.setBoolean("previewPass", false);
        shader.setFloat("overlayLayer", uv[2]);
        shader.setFloat("brightnessMultiplier", 1.1f);

        boolean isProxy = pos.equals(breakingPos);
        if (isProxy) {
            shader.setBoolean("uIsProxy", true);
        }

        // Use the exact same model matrix setup as the breaking proxy block
        // Centered at XZ=0, base at Y=0
        modelMatrix.identity().translate(pos.x() + 0.5f, pos.y(), pos.z() + 0.5f);
        shader.setMatrix4f("model", modelMatrix);
        
        fullFaceMesh.render();

        if (isProxy) {
            shader.setBoolean("uIsProxy", false);
        }

        shader.setBoolean("useMask", false);
        shader.setFloat("brightnessMultiplier", 1.0f);
    }

    private void createFullFaceMesh() {
        // Defined at Y=1.001 relative to block base (Y=0)
        // Center is at XZ=0
        float[] positions = { 
            -0.5f, 1.001f,  0.5f, // Bottom-Left
             0.5f, 1.001f,  0.5f, // Bottom-Right
             0.5f, 1.001f, -0.5f, // Top-Right
            -0.5f, 1.001f, -0.5f  // Top-Left
        };
        float[] normals = { 0, 1, 0,  0, 1, 0,  0, 1, 0,  0, 1, 0 };
        float[] texCoords = { 
            0, 1, 0, // BL
            1, 1, 0, // BR
            1, 0, 0, // TR
            0, 0, 0  // TL
        };
        float[] blockTypes = { 150, 150, 150, 150 }; 
        float[] neighborData = { 0, 0, 0, 0 };
        int[] indices = { 0, 1, 2, 2, 3, 0 };

        fullFaceMesh = new Mesh(positions, texCoords, normals, blockTypes, neighborData, indices);
    }

    public void cleanup() {
        if (fullFaceMesh != null) {
            fullFaceMesh.cleanup();
            fullFaceMesh = null;
        }
    }
}
