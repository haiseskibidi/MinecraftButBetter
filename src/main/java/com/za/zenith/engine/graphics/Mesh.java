package com.za.zenith.engine.graphics;

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
    private final int neighborDataVboId;
    private final int eboId;
    private final int vertexCount;
    private final float[] positions;
    private org.joml.Vector3f graspOffset = new org.joml.Vector3f(0);
    
    public void setGraspOffset(org.joml.Vector3f offset) { this.graspOffset = offset; }
    public org.joml.Vector3f getGraspOffset() { return graspOffset; }
    
    public Mesh(float[] positions, float[] texCoords, float[] normals, int[] indices) {
        this(positions, texCoords, normals, new float[positions.length / 3], new float[positions.length / 3], indices);
    }
    
    public Mesh(float[] positions, float[] texCoords, float[] normals, float[] blockTypes, int[] indices) {
        this(positions, texCoords, normals, blockTypes, new float[positions.length / 3], indices);
    }

    public Mesh(float[] positions, float[] texCoords, float[] normals, float[] blockTypes, float[] neighborData, int[] indices) {
        this.positions = positions;
        if (indices.length == 0 || positions.length == 0) {
            this.vertexCount = 0;
            this.vaoId = -1;
            this.posVboId = -1;
            this.texVboId = -1;
            this.normalVboId = -1;
            this.blockTypeVboId = -1;
            this.neighborDataVboId = -1;
            this.eboId = -1;
            return;
        }
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
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 0, 0);
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

        neighborDataVboId = glGenBuffers();
        FloatBuffer neighborDataBuffer = memAllocFloat(neighborData.length);
        neighborDataBuffer.put(neighborData).flip();
        glBindBuffer(GL_ARRAY_BUFFER, neighborDataVboId);
        glBufferData(GL_ARRAY_BUFFER, neighborDataBuffer, GL_STATIC_DRAW);
        glVertexAttribPointer(4, 1, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(4);
        memFree(neighborDataBuffer);
        
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
    
    public org.joml.Vector3f getMin() {
        org.joml.Vector3f min = new org.joml.Vector3f(Float.MAX_VALUE);
        for (int i = 0; i < positions.length; i += 3) {
            min.x = Math.min(min.x, positions[i]);
            min.y = Math.min(min.y, positions[i+1]);
            min.z = Math.min(min.z, positions[i+2]);
        }
        return min;
    }

    public org.joml.Vector3f getMax() {
        org.joml.Vector3f max = new org.joml.Vector3f(-Float.MAX_VALUE);
        for (int i = 0; i < positions.length; i += 3) {
            max.x = Math.max(max.x, positions[i]);
            max.y = Math.max(max.y, positions[i+1]);
            max.z = Math.max(max.z, positions[i+2]);
        }
        return max;
    }

    /**
     * Возвращает смещение центра меша по горизонтали (X, Z) относительно его локального нуля.
     */
    public org.joml.Vector3f getHorizontalCenterOffset() {
        org.joml.Vector3f min = getMin();
        org.joml.Vector3f max = getMax();
        return new org.joml.Vector3f(
            (min.x + max.x) * 0.5f,
            0, // Высоту не трогаем, так как предметы должны стоять на земле
            (min.z + max.z) * 0.5f
        );
    }

    public void cleanup() {
        glDeleteBuffers(posVboId);
        glDeleteBuffers(texVboId);
        glDeleteBuffers(normalVboId);
        glDeleteBuffers(blockTypeVboId);
        glDeleteBuffers(neighborDataVboId);
        glDeleteBuffers(eboId);
        glDeleteVertexArrays(vaoId);
    }
}


