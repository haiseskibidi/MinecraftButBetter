package com.za.zenith.utils;

import java.util.Arrays;

public class FloatArrayList {
    private float[] data;
    private int size;

    public FloatArrayList() {
        this(1024);
    }

    public FloatArrayList(int capacity) {
        data = new float[capacity];
        size = 0;
    }

    public void add(float element) {
        if (size == data.length) {
            data = Arrays.copyOf(data, data.length * 2);
        }
        data[size++] = element;
    }

    public float get(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + size);
        return data[index];
    }

    public int size() {
        return size;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public void clear() {
        size = 0;
    }

    public float[] getInternalArray() {
        return data;
    }

    public void trimToSize() {
        if (size < data.length) {
            data = Arrays.copyOf(data, size);
        }
    }

    public float[] toArray() {
        return Arrays.copyOf(data, size);
    }
}
