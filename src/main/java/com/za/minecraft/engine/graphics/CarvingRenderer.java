package com.za.minecraft.engine.graphics;

import com.za.minecraft.world.BlockPos;
import com.za.minecraft.world.blocks.entity.StumpBlockEntity;
import org.joml.Matrix4f;

/**
 * Универсальный рендерер накладок (Decals) с поддержкой шейдерного маскирования.
 */
public class CarvingRenderer {
    private Mesh fullFaceMesh;

    public void render(StumpBlockEntity stump, DynamicTextureAtlas atlas, Shader shader, Matrix4f modelMatrix) {
        int mask = stump.getCarvingMask();
        BlockPos pos = stump.getPos();
        
        // Получаем UV для обтёсанного дерева
        float[] uv = atlas.uvFor("stripped_oak_log_top.png");
        if (uv == null) return;

        if (fullFaceMesh == null) {
            createFullFaceMesh();
        }

        shader.setBoolean("useMask", true);
        shader.setInt("faceMask", mask);
        shader.setUniform("overlayUV", uv[0], uv[1], uv[2], uv[3]);
        shader.setFloat("brightnessMultiplier", 1.25f);

        // Сдвигаем на 1.01f вверх, чтобы точно быть над блоком
        modelMatrix.identity().translate(pos.x(), pos.y() + 1.01f, pos.z());
        shader.setMatrix4f("model", modelMatrix);
        
        fullFaceMesh.render();

        shader.setBoolean("useMask", false);
        shader.setFloat("brightnessMultiplier", 1.0f);
    }

    private void createFullFaceMesh() {
        // Вершины в порядке Counter-Clockwise для взгляда СВЕРХУ
        float[] positions = { 
            0, 0, 1, // Bottom-Left (Z=1)
            1, 0, 1, // Bottom-Right (Z=1)
            1, 0, 0, // Top-Right (Z=0)
            0, 0, 0  // Top-Left (Z=0)
        };
        float[] normals = { 0, 1, 0,  0, 1, 0,  0, 1, 0,  0, 1, 0 };
        
        // Маппинг UV: 
        // В OpenGL 0,0 - это обычно Top-Left в контексте атласа (если не перевернуто)
        // Но для маски нам нужно, чтобы X шел вправо, а Y «вниз» (как в сетке битов)
        float[] texCoords = { 
            0, 1, // BL
            1, 1, // BR
            1, 0, // TR
            0, 0  // TL
        };
        float[] blockTypes = { 0, 0, 0, 0 };
        int[] indices = { 0, 1, 2, 2, 3, 0 };

        fullFaceMesh = new Mesh(positions, texCoords, normals, blockTypes, indices);
    }

    public void cleanup() {
        if (fullFaceMesh != null) {
            fullFaceMesh.cleanup();
            fullFaceMesh = null;
        }
    }
}
