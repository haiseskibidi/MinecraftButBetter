package com.za.zenith.engine.graphics;

import com.za.zenith.world.particles.Particle;
import com.za.zenith.world.particles.ShardParticle;
import com.za.zenith.world.blocks.BlockRegistry;
import com.za.zenith.world.blocks.BlockDefinition;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL33.*;

/**
 * Рендерер для системы частиц с использованием Instancing.
 */
public class ParticleRenderer {
    private Shader shader;
    private int vaoId;
    private int vboId; // Mesh VBO
    private int eboId;
    private int instanceVboId; // Instance data VBO
    
    // Mesh для одного треугольника (основа процедурного осколка)
    private static final float[] SHARD_VERTICES = {
        // Position           // Normal           // UV (будут пересчитаны в шейдере)
         0.0f,  0.5f,  0.0f,  0.0f,  0.0f,  1.0f,  0.5f, 0.0f, // Top
        -0.5f, -0.5f,  0.0f,  0.0f,  0.0f,  1.0f,  0.0f, 1.0f, // Bottom-Left
         0.5f, -0.5f,  0.0f,  0.0f,  0.0f,  1.0f,  1.0f, 1.0f  // Bottom-Right
    };

    private static final int[] SHARD_INDICES = { 0, 1, 2 };

    // Данные инстанса: Pos(3), Rot(4), GridInfo(4), TexIndex(1), Seed(1), MaterialType(1), BlockInfo(2: type, tinted) = 16 float
    private static final int INSTANCE_DATA_SIZE = 16;
    private FloatBuffer instanceBuffer;

    public void init() {
        shader = new Shader("src/main/resources/shaders/particle_vertex.glsl", "src/main/resources/shaders/particle_fragment.glsl");
        
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        // Vertices
        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, SHARD_VERTICES, GL_STATIC_DRAW);
        
        // POS (0), NORMAL (1), TEX (2)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 8 * 4, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 8 * 4, 3 * 4);
        glEnableVertexAttribArray(1);
        glVertexAttribPointer(2, 2, GL_FLOAT, false, 8 * 4, 6 * 4);
        glEnableVertexAttribArray(2);

        // Indices
        eboId = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, SHARD_INDICES, GL_STATIC_DRAW);

        // Instance data VBO
        instanceVboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, instanceVboId);
        
        // instPos (3) - location 3
        glEnableVertexAttribArray(3);
        glVertexAttribPointer(3, 3, GL_FLOAT, false, INSTANCE_DATA_SIZE * 4, 0);
        glVertexAttribDivisor(3, 1);
        
        // instRot (4) - location 4
        glEnableVertexAttribArray(4);
        glVertexAttribPointer(4, 4, GL_FLOAT, false, INSTANCE_DATA_SIZE * 4, 3 * 4);
        glVertexAttribDivisor(4, 1);
        
        // instGridInfo (4): gx, gy, gz, gridSize - location 5
        glEnableVertexAttribArray(5);
        glVertexAttribPointer(5, 4, GL_FLOAT, false, INSTANCE_DATA_SIZE * 4, 7 * 4);
        glVertexAttribDivisor(5, 1);
        
        // instTexData (2): texIndex, randomSeed - location 6
        glEnableVertexAttribArray(6);
        glVertexAttribPointer(6, 2, GL_FLOAT, false, INSTANCE_DATA_SIZE * 4, 11 * 4);
        glVertexAttribDivisor(6, 1);

        // instMaterialType (1) - location 7
        glEnableVertexAttribArray(7);
        glVertexAttribPointer(7, 1, GL_FLOAT, false, INSTANCE_DATA_SIZE * 4, 13 * 4);
        glVertexAttribDivisor(7, 1);

        // instBlockInfo (2): type, isTinted - location 8
        glEnableVertexAttribArray(8);
        glVertexAttribPointer(8, 2, GL_FLOAT, false, INSTANCE_DATA_SIZE * 4, 14 * 4);
        glVertexAttribDivisor(8, 1);

        instanceBuffer = BufferUtils.createFloatBuffer(4000 * INSTANCE_DATA_SIZE);
        
        glBindVertexArray(0);
    }

    public void render(Camera camera, List<Particle> particles, DynamicTextureAtlas atlas, float alpha, Vector3f lightDir, Vector3f lightCol, Vector3f ambient) {
        if (particles.isEmpty()) return;

        shader.use();
        shader.setMatrix4f("projection", camera.getProjectionMatrix());
        shader.setMatrix4f("view", camera.getViewMatrix(alpha));
        shader.setVector3f("lightDirection", lightDir);
        shader.setVector3f("lightColor", lightCol);
        shader.setVector3f("ambientLight", ambient);
        shader.setFloat("uTime", (float)org.lwjgl.glfw.GLFW.glfwGetTime());
        
        glDisable(GL_CULL_FACE); // Частицы теперь плоские, видны с двух сторон

        glBindVertexArray(vaoId);
        glBindBuffer(GL_ARRAY_BUFFER, instanceVboId);

        int rendered = 0;
        instanceBuffer.clear();

        for (Particle p : particles) {
            if (p instanceof ShardParticle shard) {
                instanceBuffer.put(p.getPosition().x).put(p.getPosition().y).put(p.getPosition().z());
                instanceBuffer.put(p.getRotation().x).put(p.getRotation().y).put(p.getRotation().z).put(p.getRotation().w);
                instanceBuffer.put(shard.getGx()).put(shard.getGy()).put(shard.getGz()).put(shard.getGridSize());
                
                instanceBuffer.put((float)shard.getTextureLayer());
                instanceBuffer.put(shard.getSeed()); 
                instanceBuffer.put((float)shard.getMaterialType());

                instanceBuffer.put((float)shard.getBlockType());
                instanceBuffer.put(shard.isTinted() ? 1.0f : 0.0f);
                rendered++;
                
                if (rendered >= 4000) break;
            }
        }

        instanceBuffer.flip();
        glBufferData(GL_ARRAY_BUFFER, instanceBuffer, GL_DYNAMIC_DRAW);
        
        glDrawElementsInstanced(GL_TRIANGLES, SHARD_INDICES.length, GL_UNSIGNED_INT, 0, rendered);
        
        glBindVertexArray(0);
        glEnable(GL_CULL_FACE);
    }

    public void cleanup() {
        glDeleteBuffers(vboId);
        glDeleteBuffers(eboId);
        glDeleteBuffers(instanceVboId);
        glDeleteVertexArrays(vaoId);
        if (shader != null) shader.cleanup();
    }
}
