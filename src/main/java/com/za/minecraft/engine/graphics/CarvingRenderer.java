package com.za.minecraft.engine.graphics;

import com.za.minecraft.world.BlockPos;
import org.joml.Matrix4f;

import com.za.minecraft.world.blocks.entity.BlockEntity;
import com.za.minecraft.world.blocks.entity.StumpBlockEntity;
import com.za.minecraft.world.blocks.BlockRegistry;
import com.za.minecraft.world.blocks.BlockDefinition;
import com.za.minecraft.world.blocks.Block;

/**
 * Универсальный рендерер динамических блоков (обтёсывание).
 */
public class CarvingRenderer {
    private Mesh fullFaceMesh;

    public void render(BlockEntity be, DynamicTextureAtlas atlas, Shader shader, Matrix4f modelMatrix, Renderer renderer) {
        if (be instanceof StumpBlockEntity stump) {
            renderStump(stump, atlas, shader, modelMatrix);
        }
    }

    private void renderStump(StumpBlockEntity stump, DynamicTextureAtlas atlas, Shader shader, Matrix4f modelMatrix) {
        int mask = stump.getCarvingMask();
        BlockPos pos = stump.getPos();
        
        if (stump.getWorld() == null) return;
        int blockType = stump.getWorld().getBlock(pos).getType();
        com.za.minecraft.world.blocks.BlockTextures textures = com.za.minecraft.world.blocks.BlockRegistry.getTextures(blockType);
        if (textures == null || textures.getTop() == null) return;
        
        float[] uv = atlas.uvFor(textures.getTop());
        if (uv == null) return;

        if (fullFaceMesh == null) {
            createFullFaceMesh();
        }

        shader.setBoolean("useMask", true);
        shader.setInt("faceMask", mask);
        shader.setBoolean("previewPass", false); 
        shader.setUniform("overlayUV", uv[0], uv[1], uv[4], uv[5]);
        shader.setFloat("brightnessMultiplier", 1.25f);

        modelMatrix.identity().translate(pos.x(), pos.y() + 1.01f, pos.z());
        shader.setMatrix4f("model", modelMatrix);
        
        fullFaceMesh.render();

        shader.setBoolean("useMask", false);
        shader.setFloat("brightnessMultiplier", 1.0f);
    }

    private void createFullFaceMesh() {
        float[] positions = { 
            0, 0, 1, // Bottom-Left (Z=1)
            1, 0, 1, // Bottom-Right (Z=1)
            1, 0, 0, // Top-Right (Z=0)
            0, 0, 0  // Top-Left (Z=0)
        };
        float[] normals = { 0, 1, 0,  0, 1, 0,  0, 1, 0,  0, 1, 0 };
        float[] texCoords = { 
            0, 1, // BL
            1, 1, // BR
            1, 0, // TR
            0, 0  // TL
        };
        float[] blockTypes = { 150, 150, 150, 150 }; // Stump Top ID
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
