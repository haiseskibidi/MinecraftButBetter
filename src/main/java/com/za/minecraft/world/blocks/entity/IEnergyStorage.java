package com.za.minecraft.world.blocks.entity;

/**
 * Интерфейс для всех блоков, способных хранить, принимать или отдавать энергию.
 */
public interface IEnergyStorage {
    /**
     * Пытается добавить энергию в хранилище.
     * @return количество реально принятой энергии.
     */
    float receiveEnergy(float amount, boolean simulate);

    /**
     * Пытается извлечь энергию из хранилища.
     * @return количество реально извлеченной энергии.
     */
    float extractEnergy(float amount, boolean simulate);

    float getEnergyStored();

    float getMaxEnergyStored();

    default boolean canExtract() { return true; }
    default boolean canReceive() { return true; }
}
