package com.za.zenith.engine.graphics;

import com.za.zenith.world.particles.Particle;
import com.za.zenith.world.particles.ShardParticle;
import com.za.zenith.world.blocks.BlockRegistry;
import com.za.zenith.world.blocks.BlockDefinition;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import java.nio.FloatBuffer;
import java.util.List;

import static org.lwjgl.opengl.GL33.*;

/**
 * Рендерер классических воксельных частиц.
 * Использует инстансинг и GPU билбординг.
 */
public class ParticleRenderer {
    private Shader shader;
    private int vaoId, vboId, eboId, instanceVboId;

    // Unit Quad (Квадрат 1x1)
    private static final float[] QUAD_VERTICES = {
        -0.5f, -0.5f, 0.0f,  0.0f, 1.0f, // BL
         0.5f, -0.5f, 0.0f,  1.0f, 1.0f, // BR
         0.5f,  0.5f, 0.0f,  1.0f, 0.0f, // TR
        -0.5f,  0.5f, 0.0f,  0.0f, 0.0f  // TL
    };

    private static final int[] QUAD_INDICES = { 0, 1, 2, 2, 3, 0 };

    // Данные инстанса: 
    // Pos(3), Roll(1), Scale(1), Alpha(1), OverlayLayer(1), TexLayer(1), SnippetOffset(2), Color(3) = 13 float
    private static final int INSTANCE_DATA_SIZE = 13;
    private FloatBuffer instanceBuffer;

    public void init() {
        shader = new Shader("src/main/resources/shaders/particle_vertex.glsl", "src/main/resources/shaders/particle_fragment.glsl");
        
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        // Vertices
        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, QUAD_VERTICES, GL_STATIC_DRAW);
        
        // POS (0), TEX (1)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * 4, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * 4, 3 * 4);
        glEnableVertexAttribArray(1);

        // Indices
        eboId = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, QUAD_INDICES, GL_STATIC_DRAW);

        // Instance data VBO
        instanceVboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, instanceVboId);
        
        // instPosRoll (4): x, y, z, roll - location 2
        glEnableVertexAttribArray(2);
        glVertexAttribPointer(2, 4, GL_FLOAT, false, INSTANCE_DATA_SIZE * 4, 0);
        glVertexAttribDivisor(2, 1);
        
        // instVisual (3): scale, alpha, overlayLayer - location 3
        glEnableVertexAttribArray(3);
        glVertexAttribPointer(3, 3, GL_FLOAT, false, INSTANCE_DATA_SIZE * 4, 4 * 4);
        glVertexAttribDivisor(3, 1);

        // instColor (3): r, g, b - location 4
        glEnableVertexAttribArray(4);
        glVertexAttribPointer(4, 3, GL_FLOAT, false, INSTANCE_DATA_SIZE * 4, 10 * 4);
        glVertexAttribDivisor(4, 1);

        // instTexData (3): texLayer, snipU, snipV - location 5
        glEnableVertexAttribArray(5);
        glVertexAttribPointer(5, 3, GL_FLOAT, false, INSTANCE_DATA_SIZE * 4, 7 * 4);
        glVertexAttribDivisor(5, 1);

        instanceBuffer = BufferUtils.createFloatBuffer(5000 * INSTANCE_DATA_SIZE);
        
        glBindVertexArray(0);
    }

    public void render(Camera camera, List<Particle> particles, DynamicTextureAtlas atlas, float alpha, Vector3f ambient) {
        if (particles.isEmpty()) return;

        shader.use();
        shader.setLights("uLights", com.za.zenith.world.lighting.LightManager.getActiveLights());
        
        // Обычный блендинг. Выключаем Culling!
        glDisable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA); 

        glBindVertexArray(vaoId);
        glBindBuffer(GL_ARRAY_BUFFER, instanceVboId);

        int rendered = 0;
        instanceBuffer.clear();

        for (Particle p : particles) {
            if (p instanceof ShardParticle shard) {
                // Pos(3), Roll(1)
                instanceBuffer.put(p.getPosition().x).put(p.getPosition().y).put(p.getPosition().z()).put(p.getRoll());
                // Scale(1), Alpha(1), OverlayLayer(1)
                instanceBuffer.put(p.getScale()).put(p.getAlpha()).put((float)shard.getOverlayLayer());
                // Layer(1), Snippet(2)
                instanceBuffer.put((float)shard.getTextureLayer()).put(shard.getSnippetOffset().x).put(shard.getSnippetOffset().y);
                // Color(3)
                instanceBuffer.put(shard.getColor().x).put(shard.getColor().y).put(shard.getColor().z);
                
                rendered++;
                if (rendered >= 5000) break;
            }
        }

        instanceBuffer.flip();
        glBufferData(GL_ARRAY_BUFFER, instanceBuffer, GL_DYNAMIC_DRAW);
        
        glDrawElementsInstanced(GL_TRIANGLES, QUAD_INDICES.length, GL_UNSIGNED_INT, 0, rendered);
        
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
