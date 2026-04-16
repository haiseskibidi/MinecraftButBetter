package com.za.zenith.world.blocks.entity;

import com.za.zenith.world.BlockPos;

/**
 * Электрическая лампа.
 * Потребляет энергию для работы.
 */
public class LampBlockEntity extends BlockEntity implements ITickable, IEnergyStorage {
    private float energy = 0.0f;
    private final float maxEnergy = 10.0f;
    private final float consumption = 2.0f; // Потребление в секунду
    private boolean lit = false;

    public LampBlockEntity(BlockPos pos) {
        super(pos);
    }

    @Override
    public void update(float deltaTime) {
        boolean wasLit = lit;
        if (energy > 0) {
            energy -= consumption * deltaTime;
            lit = true;
        } else {
            energy = 0;
            lit = false;
        }

        if (wasLit != lit) {
            // Force light update in world
            int emission = lit ? 15 : 0;
            world.setBlockLight(pos, emission);
            // In a full implementation, LightEngine should handle the propagation
            // but for now we manually trigger it via world helper or similar
            // Since we don't have a direct 'propagate' call here, we use the fact
            // that LightEngine is meant to handle state changes.
            com.za.zenith.utils.Logger.info("Lamp at %s state: %s", pos, lit ? "ON" : "OFF");
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
        return 0; // Лампа только потребляет
    }

    @Override
    public float getEnergyStored() {
        return energy;
    }

    @Override
    public float getMaxEnergyStored() {
        return maxEnergy;
    }

    @Override
    public boolean canExtract() {
        return false;
    }
}


