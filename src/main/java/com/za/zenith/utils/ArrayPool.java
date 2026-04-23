package com.za.zenith.utils;

import java.util.concurrent.ConcurrentLinkedQueue;

public class ArrayPool {
    private static final ConcurrentLinkedQueue<int[]> blockDataPool = new ConcurrentLinkedQueue<>();
    private static final ConcurrentLinkedQueue<byte[]> lightDataPool = new ConcurrentLinkedQueue<>();
    
    private static final int BLOCK_DATA_SIZE = 16 * 384 * 16;
    private static final int LIGHT_DATA_SIZE = 16 * 384 * 16;

    public static int[] rentBlockDataArray() {
        int[] arr = blockDataPool.poll();
        if (arr == null) {
            arr = new int[BLOCK_DATA_SIZE];
        }
        return arr;
    }

    public static void returnBlockDataArray(int[] arr) {
        if (arr != null && arr.length == BLOCK_DATA_SIZE) {
            blockDataPool.offer(arr);
        }
    }

    public static byte[] rentLightDataArray() {
        byte[] arr = lightDataPool.poll();
        if (arr == null) {
            arr = new byte[LIGHT_DATA_SIZE];
        }
        return arr;
    }

    public static void returnLightDataArray(byte[] arr) {
        if (arr != null && arr.length == LIGHT_DATA_SIZE) {
            lightDataPool.offer(arr);
        }
    }
}
