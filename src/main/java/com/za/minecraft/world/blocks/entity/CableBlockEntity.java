package com.za.minecraft.world.blocks.entity;

import com.za.minecraft.world.BlockPos;
import com.za.minecraft.utils.Direction;

/**
 * Кабель для передачи энергии.
 * Соединяет соседние IEnergyStorage и распределяет энергию между ними.
 */
public class CableBlockEntity extends BlockEntity implements ITickable, IEnergyStorage {
    private float energy = 0.0f;
    private final float maxEnergy = 100.0f; // Небольшой внутренний буфер
    private final float transferRate = 50.0f; // Энергии в секунду на одно соединение

    public CableBlockEntity(BlockPos pos) {
        super(pos);
    }

    @Override
    public void update(float deltaTime) {
        if (world == null) return;

        // Опрашиваем 6 соседних направлений через Direction enum
        for (Direction dir : Direction.values()) {
            BlockPos neighborPos = dir.offset(pos);
            BlockEntity be = world.getBlockEntity(neighborPos);
            
            if (be instanceof IEnergyStorage storage) {
                // Если у соседа меньше энергии, чем у нас, отдаем ему часть
                if (storage.canReceive() && energy > storage.getEnergyStored()) {
                    float toTransfer = Math.min(transferRate * deltaTime, (energy - storage.getEnergyStored()) / 2.0f);
                    float accepted = storage.receiveEnergy(toTransfer, false);
                    this.energy -= accepted;
                } 
                // Если у соседа больше энергии и он может отдавать, забираем у него
                else if (storage.canExtract() && energy < storage.getEnergyStored()) {
                    float toPull = Math.min(transferRate * deltaTime, (storage.getEnergyStored() - energy) / 2.0f);
                    float extracted = storage.extractEnergy(toPull, false);
                    this.energy += extracted;
                }
            }
        }
    }

    @Override
    public float receiveEnergy(float amount, boolean simulate) {
        float space = maxEnergy - energy;
        float accepted = Math.min(space, amount);
        if (!simulate) energy += accepted;
        return accepted;
    }

    @Override
    public float extractEnergy(float amount, boolean simulate) {
        float extracted = Math.min(energy, amount);
        if (!simulate) energy -= extracted;
        return extracted;
    }

    @Override
    public float getEnergyStored() {
        return energy;
    }

    @Override
    public float getMaxEnergyStored() {
        return maxEnergy;
    }
}
