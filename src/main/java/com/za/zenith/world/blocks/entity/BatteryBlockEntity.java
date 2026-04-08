package com.za.zenith.world.blocks.entity;

import com.za.zenith.world.BlockPos;

/**
 * Аккумулятор для хранения энергии.
 * Является пассивным узлом (не имеет метода update), 
 * так как кабели сами забирают или отдают ему энергию.
 */
public class BatteryBlockEntity extends BlockEntity implements IEnergyStorage {
    private float energy = 0.0f;
    private final float maxEnergy = 10000.0f;

    public BatteryBlockEntity(BlockPos pos) {
        super(pos);
    }

    @Override
    public float receiveEnergy(float amount, boolean simulate) {
        float space = maxEnergy - energy;
        float accepted = Math.min(space, amount);
        if (!simulate) {
            energy += accepted;
        }
        return accepted;
    }

    @Override
    public float extractEnergy(float amount, boolean simulate) {
        float extracted = Math.min(energy, amount);
        if (!simulate) {
            energy -= extracted;
        }
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
