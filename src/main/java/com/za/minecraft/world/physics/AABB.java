package com.za.minecraft.world.physics;

import org.joml.Vector3f;

public class AABB {
    private final Vector3f min;
    private final Vector3f max;
    
    public AABB(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        this.min = new Vector3f(minX, minY, minZ);
        this.max = new Vector3f(maxX, maxY, maxZ);
    }
    
    public AABB(Vector3f min, Vector3f max) {
        this.min = new Vector3f(min);
        this.max = new Vector3f(max);
    }
    
    public AABB offset(float x, float y, float z) {
        return new AABB(
            min.x + x, min.y + y, min.z + z,
            max.x + x, max.y + y, max.z + z
        );
    }
    
    public AABB offset(Vector3f offset) {
        return offset(offset.x, offset.y, offset.z);
    }
    
    public boolean intersects(AABB other) {
        return max.x > other.min.x && min.x < other.max.x &&
               max.y > other.min.y && min.y < other.max.y &&
               max.z > other.min.z && min.z < other.max.z;
    }
    
    public boolean contains(Vector3f point) {
        return point.x >= min.x && point.x <= max.x &&
               point.y >= min.y && point.y <= max.y &&
               point.z >= min.z && point.z <= max.z;
    }

    /**
     * Возвращает дистанцию до пересечения луча с этим AABB.
     * Использует алгоритм "Ray-Box Intersection".
     */
    public float intersectDist(Vector3f origin, Vector3f direction) {
        float t1 = (min.x - origin.x) / direction.x;
        float t2 = (max.x - origin.x) / direction.x;
        float t3 = (min.y - origin.y) / direction.y;
        float t4 = (max.y - origin.y) / direction.y;
        float t5 = (min.z - origin.z) / direction.z;
        float t6 = (max.z - origin.z) / direction.z;

        float tmin = Math.max(Math.max(Math.min(t1, t2), Math.min(t3, t4)), Math.min(t5, t6));
        float tmax = Math.min(Math.min(Math.max(t1, t2), Math.max(t3, t4)), Math.max(t5, t6));

        if (tmax < 0 || tmin > tmax) return -1.0f;
        return tmin;
    }
    
    public Vector3f getCenter() {
        return new Vector3f(
            (min.x + max.x) * 0.5f,
            (min.y + max.y) * 0.5f,
            (min.z + max.z) * 0.5f
        );
    }
    
    public Vector3f getSize() {
        return new Vector3f(
            max.x - min.x,
            max.y - min.y,
            max.z - min.z
        );
    }
    
    public Vector3f getMin() {
        return new Vector3f(min);
    }
    
    public Vector3f getMax() {
        return new Vector3f(max);
    }
    
    @Override
    public String toString() {
        return String.format("AABB[%.2f,%.2f,%.2f -> %.2f,%.2f,%.2f]", 
            min.x, min.y, min.z, max.x, max.y, max.z);
    }
}
