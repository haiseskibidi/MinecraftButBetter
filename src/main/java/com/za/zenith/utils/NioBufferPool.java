package com.za.zenith.utils;

import org.lwjgl.system.MemoryUtil;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NioBufferPool {
    private static final int FLOAT_SIZE = 4;
    private static final int INT_SIZE = 4;
    
    // Simple pool for small/medium buffers frequently used in main thread
    private static final ConcurrentLinkedQueue<FloatBuffer> floatBuffers = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<IntBuffer> intBuffers = new ConcurrentLinkedQueue<>();

    public static FloatBuffer rentFloat(int capacity) {
        FloatBuffer buffer = floatBuffers.poll();
        if (buffer == null) {
            return MemoryUtil.memAllocFloat(capacity);
        }
        if (buffer.capacity() < capacity) {
            MemoryUtil.memFree(buffer);
            return MemoryUtil.memAllocFloat(capacity);
        }
        buffer.clear();
        return buffer;
    }

    public static void returnFloat(FloatBuffer buffer) {
        if (buffer != null) {
            floatBuffers.offer(buffer);
        }
    }

    public static IntBuffer rentInt(int capacity) {
        IntBuffer buffer = intBuffers.poll();
        if (buffer == null) {
            return MemoryUtil.memAllocInt(capacity);
        }
        if (buffer.capacity() < capacity) {
            MemoryUtil.memFree(buffer);
            return MemoryUtil.memAllocInt(capacity);
        }
        buffer.clear();
        return buffer;
    }

    public static void returnInt(IntBuffer buffer) {
        if (buffer != null) {
            intBuffers.offer(buffer);
        }
    }
    
    public static void clearPools() {
        FloatBuffer fb;
        while ((fb = floatBuffers.poll()) != null) {
            MemoryUtil.memFree(fb);
        }
        IntBuffer ib;
        while ((ib = intBuffers.poll()) != null) {
            MemoryUtil.memFree(ib);
        }
    }
}
