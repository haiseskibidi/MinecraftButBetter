package com.za.minecraft.engine.graphics;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.system.MemoryUtil.*;

public class Mesh {
    private final int vaoId;
    private final int posVboId;
    private final int texVboId;
    private final int normalVboId;
    private final int blockTypeVboId;
    private final int eboId;
    private final int vertexCount;
    
    public Mesh(float[] positions, float[] texCoords, float[] normals, int[] indices) {
        this(positions, texCoords, normals, new float[positions.length / 3], indices);
    }
    
    public Mesh(float[] positions, float[] texCoords, float[] normals, float[] blockTypes, int[] indices) {
        this.vertexCount = indices.length;
        
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);
        
        posVboId = glGenBuffers();
        FloatBuffer posBuffer = memAllocFloat(positions.length);
        posBuffer.put(positions).flip();
        glBindBuffer(GL_ARRAY_BUFFER, posVboId);
        glBufferData(GL_ARRAY_BUFFER, posBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);
        memFree(posBuffer);
        
        texVboId = glGenBuffers();
        FloatBuffer texBuffer = memAllocFloat(texCoords.length);
        texBuffer.put(texCoords).flip();
        glBindBuffer(GL_ARRAY_BUFFER, texVboId);
        glBufferData(GL_ARRAY_BUFFER, texBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(1);
        memFree(texBuffer);
        
        normalVboId = glGenBuffers();
        FloatBuffer normalBuffer = memAllocFloat(normals.length);
        normalBuffer.put(normals).flip();
        glBindBuffer(GL_ARRAY_BUFFER, normalVboId);
        glBufferData(GL_ARRAY_BUFFER, normalBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(2, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(2);
        memFree(normalBuffer);
        
        blockTypeVboId = glGenBuffers();
        FloatBuffer blockTypeBuffer = memAllocFloat(blockTypes.length);
        blockTypeBuffer.put(blockTypes).flip();
        glBindBuffer(GL_ARRAY_BUFFER, blockTypeVboId);
        glBufferData(GL_ARRAY_BUFFER, blockTypeBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(3, 1, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(3);
        memFree(blockTypeBuffer);
        
        eboId = glGenBuffers();
        IntBuffer indicesBuffer = memAllocInt(indices.length);
        indicesBuffer.put(indices).flip();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);
        memFree(indicesBuffer);
        
        glBindVertexArray(0);
    }
    
    public void render() {
        glBindVertexArray(vaoId);
        glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }
    
    public void render(int glMode) {
        glBindVertexArray(vaoId);
        glDrawElements(glMode, vertexCount, GL_UNSIGNED_INT, 0);
        glBindVertexArray(0);
    }
    
    public void cleanup() {
        glDeleteBuffers(posVboId);
        glDeleteBuffers(texVboId);
        glDeleteBuffers(normalVboId);
        glDeleteBuffers(blockTypeVboId);
        glDeleteBuffers(eboId);
        glDeleteVertexArrays(vaoId);
    }
}
