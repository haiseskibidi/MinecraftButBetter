package com.za.zenith.utils;

import java.util.Arrays;

public class FloatArrayList {
    private float[] data;
    private int size;
    private final int initialCapacity;

    public FloatArrayList() {
        this(1024);
    }

    public FloatArrayList(int capacity) {
        this.initialCapacity = capacity;
        data = new float[capacity];
        size = 0;
    }

    public void add(float element) {
        if (size == data.length) {
            data = Arrays.copyOf(data, Math.max(16, data.length * 2));
        }
        data[size++] = element;
    }

    public float get(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        return data[index];
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }

    /**
     * Стандартная очистка без изменения емкости массива.
     */
    public void clear() {
        size = 0;
    }

    /**
     * Очистка с проверкой емкости. Если массив превысил лимит, он будет сброшен до начального размера.
     * Помогает предотвратить утечки в ThreadLocal.
     */
    public void clear(int maxRetention) {
        size = 0;
        if (data.length > maxRetention) {
            data = new float[initialCapacity];
        }
    }

    public float[] getInternalArray() {
        return data;
    }

    public float[] toArray() {
        return Arrays.copyOf(data, size);
    }
}
