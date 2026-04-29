package com.za.zenith.engine.graphics;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL43.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import com.za.zenith.utils.Logger;

/**
 * MeshPool manages large shared buffers to avoid frequent state changes.
 */
public class MeshPool {
    public static final int VERTEX_BUFFER_SIZE = 1024 * 1024 * 1024; // 1 GB
    public static final int INDEX_BUFFER_SIZE = 512 * 1024 * 1024;   // 512 MB
    public static final int STRIDE = 7 * Float.BYTES;
    
    private final int vboId;
    private final int eboId;
    
    private int vertexOffset = 0;
    private int indexOffset = 0;
    private int version = 0;

    public MeshPool() {
        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, VERTEX_BUFFER_SIZE, GL_DYNAMIC_DRAW);

        eboId = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, INDEX_BUFFER_SIZE, GL_DYNAMIC_DRAW);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
        Logger.info("MeshPool: Initialized with 1GB Vertex and 512MB Index buffers");
    }

    public synchronized Allocation allocate(FloatBuffer vertices, IntBuffer indices) {
        int vSize = vertices.remaining() * Float.BYTES;
        int iSize = indices.remaining() * Integer.BYTES;

        if (vertexOffset + vSize > VERTEX_BUFFER_SIZE || indexOffset + iSize > INDEX_BUFFER_SIZE) {
            version++;
            vertexOffset = 0;
            indexOffset = 0;
            Logger.warn("MeshPool: Buffer wrap-around! Version incremented to " + version + ". Clearing world meshes...");
        }

        int currentVOffset = vertexOffset;
        int currentIOffset = indexOffset;

        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferSubData(GL_ARRAY_BUFFER, currentVOffset, vertices);

        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        glBufferSubData(GL_ELEMENT_ARRAY_BUFFER, currentIOffset, indices);

        vertexOffset += vSize;
        indexOffset += iSize;

        return new Allocation(currentVOffset / STRIDE, currentIOffset / Integer.BYTES, indices.remaining(), version);
    }

    public int getVboId() { return vboId; }
    public int getEboId() { return eboId; }
    public int getVersion() { return version; }

    public record Allocation(int baseVertex, int firstIndex, int indexCount, int poolVersion) {}
    
    public void cleanup() {
        glDeleteBuffers(vboId);
        glDeleteBuffers(eboId);
    }
}
