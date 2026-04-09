package com.za.zenith.world.particles;

import org.joml.Vector3f;
import org.joml.Vector2f;

/**
 * Классический воксельный осколок. 
 * Использует билбординг и микро-текстуры (snippets).
 */
public class ShardParticle extends Particle {
    public static final int MAT_GENERIC = 0;
    public static final int MAT_WOOD = 1;
    public static final int MAT_LEAVES = 2;

    private final int textureLayer;
    private final org.joml.Vector3f color = new org.joml.Vector3f(1, 1, 1);
    private final org.joml.Vector2f snippetOffset = new org.joml.Vector2f();

    public ShardParticle(Vector3f pos, Vector3f vel, float life, float scale, int textureLayer, org.joml.Vector3f color) {
        super(pos, vel, life);
        this.scale = scale;
        this.textureLayer = textureLayer;
        if (color != null) this.color.set(color);
        
        // Выбираем случайный кусок 4x4 внутри текстуры 16x16
        // Смещение в долях 0..1 (шаг 1/4 = 0.25)
        this.snippetOffset.set(
            (float)Math.floor(Math.random() * 4) * 0.25f,
            (float)Math.floor(Math.random() * 4) * 0.25f
        );
        
        // Случайное вращение и скорость вращения
        this.roll = (float)(Math.random() * Math.PI * 2.0);
        this.rollVelocity = (float)(Math.random() - 0.5) * 10.0f;
    }

    @Override
    public void update(float deltaTime) {
        // Гравитация
        velocity.y -= 18.0f * deltaTime;
        
        // Сопротивление воздуха (плавное замедление)
        velocity.mul(0.98f);
        
        super.update(deltaTime);
        
        // Дополнительное уменьшение в самом конце
        if (getLifeRatio() < 0.2f) {
            scale *= 0.95f;
        }
    }

    public int getTextureLayer() { return textureLayer; }
    public org.joml.Vector3f getColor() { return color; }
    public Vector2f getSnippetOffset() { return snippetOffset; }
}
