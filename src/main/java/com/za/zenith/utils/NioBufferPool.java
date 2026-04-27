package com.za.zenith.utils;

import org.lwjgl.system.MemoryUtil;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;

public class NioBufferPool {
    private static final ConcurrentLinkedQueue<FloatBuffer> floatBuffers = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<IntBuffer> intBuffers = new ConcurrentLinkedQueue<>();
    private static final int MAX_POOL_SIZE = 32;

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
            if (floatBuffers.size() < MAX_POOL_SIZE) {
                floatBuffers.offer(buffer);
            } else {
                MemoryUtil.memFree(buffer);
            }
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
            if (intBuffers.size() < MAX_POOL_SIZE) {
                intBuffers.offer(buffer);
            } else {
                MemoryUtil.memFree(buffer);
            }
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
