package com.za.zenith.world.blocks.entity;

/**
 * Интерфейс для сущностей блоков, которые должны обновляться каждый игровой тик.
 */
public interface ITickable {
    void update(float deltaTime);
    
    default boolean shouldTick() {
        return true;
    }
}
