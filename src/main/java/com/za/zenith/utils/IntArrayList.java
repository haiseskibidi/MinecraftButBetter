package com.za.zenith.utils;

import java.util.Arrays;

public class IntArrayList {
    private int[] data;
    private int size;
    private final int initialCapacity;

    public IntArrayList() {
        this(1024);
    }

    public IntArrayList(int capacity) {
        this.initialCapacity = capacity;
        data = new int[capacity];
        size = 0;
    }

    public void add(int element) {
        if (size == data.length) {
            data = Arrays.copyOf(data, Math.max(16, data.length * 2));
        }
        data[size++] = element;
    }

    public int get(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        return data[index];
    }

    public int size() { return size; }
    public boolean isEmpty() { return size == 0; }

    public void clear() {
        size = 0;
    }

    public void clear(int maxRetention) {
        size = 0;
        if (data.length > maxRetention) {
            data = new int[initialCapacity];
        }
    }

    public int[] getInternalArray() {
        return data;
    }

    public int[] toArray() {
        return Arrays.copyOf(data, size);
    }
}
