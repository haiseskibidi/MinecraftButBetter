package com.za.minecraft.engine.graphics.ui;

import com.za.minecraft.engine.graphics.Mesh;
import com.za.minecraft.engine.graphics.Shader;
import com.za.minecraft.engine.graphics.DynamicTextureAtlas;
import com.za.minecraft.world.blocks.Block;
import com.za.minecraft.world.chunks.ChunkMeshGenerator;
import com.za.minecraft.world.items.Item;
import com.za.minecraft.world.items.ItemStack;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

/**
 * Специализированный рендерер для отрисовки 3D моделей блоков в интерфейсе.
 */
public class InventoryBlockRenderer {
    private final Map<Integer, Mesh> blockMeshCache = new HashMap<>();
    private final Matrix4f modelMatrix = new Matrix4f();
    private final Matrix4f projectionMatrix = new Matrix4f();
    private final Matrix4f viewMatrix = new Matrix4f();
    private Shader inventoryShader;

    public InventoryBlockRenderer() {
        inventoryShader = new Shader(
            "src/main/resources/shaders/inventory_block_vertex.glsl",
            "src/main/resources/shaders/inventory_block_fragment.glsl"
        );
    }

    public void renderBlock(Item item, float x, float y, float size, int screenWidth, int screenHeight, DynamicTextureAtlas atlas, float rotation) {
        if (item == null || !item.isBlock() || atlas == null) return;

        // 1. Получаем или создаем меш блока
        Mesh mesh = blockMeshCache.get(item.getId());
        if (mesh == null) {
            mesh = ChunkMeshGenerator.generateSingleBlockMesh(new Block(item.getId()), atlas);
            if (mesh != null) blockMeshCache.put(item.getId(), mesh);
        }

        if (mesh != null) {
            inventoryShader.use();
            atlas.bind();
            
            // Настройка 3D проекции для UI (Ортографическая)
            projectionMatrix.identity().setOrtho(0, screenWidth, screenHeight, 0, -100, 100);
            viewMatrix.identity(); 
            
            inventoryShader.setMatrix4f("projection", projectionMatrix);
            inventoryShader.setMatrix4f("view", viewMatrix);
            inventoryShader.setFloat("brightnessMultiplier", 1.0f);

            float centerX = x + size / 2.0f;
            float centerY = y + size / 2.0f;
            
            // Трансформации для изометрического вида
            modelMatrix.identity().translate(centerX, centerY, 0);
            
            float visualScale = size * 0.65f * item.getVisualScale();
            modelMatrix.rotateX((float) Math.toRadians(-30))
                       .rotateY((float) Math.toRadians(45 + rotation))
                       .scale(visualScale, -visualScale, visualScale)
                       .translate(0, -0.5f, 0);

            inventoryShader.setMatrix4f("model", modelMatrix);

            // Scissor и очистка глубины для изоляции слота
            // Сохраняем текущее состояние Scissor, чтобы не ломать родительские контейнеры (ScrollPanel)
            boolean wasScissor = glIsEnabled(GL_SCISSOR_TEST);
            int[] oldScissor = new int[4];
            if (wasScissor) {
                glGetIntegerv(GL_SCISSOR_BOX, oldScissor);
            }

            glEnable(GL_SCISSOR_TEST);
            
            // Текущий прямоугольник слота в координатах OpenGL (Y от низа)
            int slotX = (int)x;
            int slotY = screenHeight - (int)(y + size);
            int slotW = (int)size;
            int slotH = (int)size;

            if (wasScissor) {
                // Вычисляем пересечение с существующей областью (для вложенности в ScrollPanel)
                int interX = Math.max(slotX, oldScissor[0]);
                int interY = Math.max(slotY, oldScissor[1]);
                int interRight = Math.min(slotX + slotW, oldScissor[0] + oldScissor[2]);
                int interTop = Math.min(slotY + slotH, oldScissor[1] + oldScissor[3]);
                
                int interW = Math.max(0, interRight - interX);
                int interH = Math.max(0, interTop - interY);
                
                glScissor(interX, interY, interW, interH);
            } else {
                glScissor(slotX, slotY, slotW, slotH);
            }
            
            glEnable(GL_DEPTH_TEST);
            glDepthFunc(GL_LEQUAL);
            glClear(GL_DEPTH_BUFFER_BIT); 
            
            mesh.render();
            
            glDisable(GL_DEPTH_TEST);
            
            // Восстанавливаем предыдущее состояние Scissor
            if (wasScissor) {
                glScissor(oldScissor[0], oldScissor[1], oldScissor[2], oldScissor[3]);
            } else {
                glDisable(GL_SCISSOR_TEST);
            }
        }
    }

    public void cleanup() {
        for (Mesh mesh : blockMeshCache.values()) {
            if (mesh != null) mesh.cleanup();
        }
        blockMeshCache.clear();
        if (inventoryShader != null) inventoryShader.cleanup();
    }
}
