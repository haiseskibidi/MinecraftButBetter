package com.za.zenith.world.particles;

import org.joml.Vector3f;

/**
 * Осколок блока. Обладает информацией о своем положении в сетке разрушения.
 */
public class ShardParticle extends Particle {
    public static final int MAT_GENERIC = 0;
    public static final int MAT_WOOD = 1;
    public static final int MAT_LEAVES = 2;

    private final int blockType;
    private final int materialType;
    private final int textureLayer;
    private final boolean tinted;
    private final byte gx, gy, gz; // Координаты в сетке (0..gridSize-1)
    private final byte gridSize;   // Размер сетки (например, 3)
    private final float baseScale;
    private final float seed; // Для случайных UV и формы
    private boolean grounded = false;
    private float flutterTimer = 0;

    public ShardParticle(Vector3f pos, Vector3f vel, float life, int blockType, int materialType, int textureLayer, boolean tinted, int gx, int gy, int gz, int gridSize, float baseScale) {
        super(pos, vel, life);
        this.blockType = blockType;
        this.materialType = materialType;
        this.textureLayer = textureLayer;
        this.tinted = tinted;
        this.gx = (byte)gx;
        this.gy = (byte)gy;
        this.gz = (byte)gz;
        this.gridSize = (byte)gridSize;
        this.baseScale = baseScale;
        this.seed = (float)Math.random();
        this.flutterTimer = (float)(Math.random() * Math.PI * 2.0);
        
        // Рандомное вращение при спавне
        this.angularVelocity.set(
            (float)(Math.random() - 0.5) * 15.0f,
            (float)(Math.random() - 0.5) * 15.0f,
            (float)(Math.random() - 0.5) * 15.0f
        );
        
        // Уменьшаем базовый масштаб в 2 раза (0.5f), чтобы осколки были мелкими
        this.scale = (baseScale / gridSize) * 0.5f;
    }

    public float getSeed() { return seed; }
    public int getMaterialType() { return materialType; }
    public int getTextureLayer() { return textureLayer; }
    public boolean isTinted() { return tinted; }

    @Override
    public void update(float deltaTime) {
        if (!grounded) {
            if (materialType == MAT_LEAVES) {
                // Листва: планирует в воздухе
                velocity.y -= 4.0f * deltaTime; // Медленное падение
                velocity.mul(0.95f); // Сопротивление воздуха
                
                flutterTimer += deltaTime * 5.0f;
                position.x += (float)Math.sin(flutterTimer + seed * 10.0f) * 0.5f * deltaTime;
                position.z += (float)Math.cos(flutterTimer * 0.7f + seed * 5.0f) * 0.5f * deltaTime;
            } else {
                // Дерево/Камень: стандартная гравитация
                velocity.y -= 15.0f * deltaTime;
            }
            super.update(deltaTime);
        } else {
            lifeTime -= deltaTime;
            if (lifeTime <= 0) removed = true;
        }
        
        // Плавное уменьшение перед исчезновением
        float ratio = Math.max(0, lifeTime / maxLifeTime);
        this.scale = (ratio * (baseScale / gridSize) * 0.5f) * (grounded ? 0.7f : 1.0f); 
    }

    public void setGrounded(boolean grounded) {
        this.grounded = grounded;
        if (grounded) {
            velocity.set(0);
            angularVelocity.set(0);
        }
    }

    public int getBlockType() { return blockType; }
    public byte getGx() { return gx; }
    public byte getGy() { return gy; }
    public byte getGz() { return gz; }
    public byte getGridSize() { return gridSize; }
}
