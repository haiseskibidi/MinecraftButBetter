package com.za.zenith.world.generation.aquifers;

import com.za.zenith.utils.Identifier;
import com.za.zenith.world.blocks.BlockDefinition;
import com.za.zenith.world.blocks.BlockRegistry;
import com.za.zenith.world.blocks.Blocks;
import com.za.zenith.world.generation.SimplexNoise;

public class AquiferManager {
    private final SimplexNoise aquiferNoise;

    public AquiferManager(long seed) {
        this.aquiferNoise = new SimplexNoise(seed ^ 0x41515549);
    }

    /**
     * Determines the fluid state for a given coordinate.
     * @return The BlockDefinition of the fluid, or null if it should be empty air.
     */
    public BlockDefinition getFluidState(int x, int y, int z, boolean isUnderground) {
        BlockDefinition lava = BlockRegistry.getRegistry().get(Identifier.of("zenith:lava"));

        if (!isUnderground) {
            // Global Sea Level (logical 62 -> internal 190)
            // Only flood if we are open to the sky (not inside a cave)
            if (y < 190) {
                return Blocks.WATER;
            }
            return null;
        }

        // Deep lava lakes (logical -88 -> internal 40)
        if (y < 40) {
            return lava != null ? lava : Blocks.WATER; // Fallback if LAVA is not registered yet
        }

        // Localized sparse aquifers for caves
        double noiseVal = aquiferNoise.octaveNoise(x, z, 2, 0.5, 0.02);
        
        if (noiseVal > 0.7) {
            // Local fluid level oscillates between internal 60 and 140
            int localFluidLevel = (int) (60 + (noiseVal - 0.7) / 0.3 * 80);
            // Only fill a limited depth to create lakes, not infinite flooded columns
            if (y < localFluidLevel && y > localFluidLevel - 15) {
                return Blocks.WATER;
            }
        }

        return null;
    }
}
