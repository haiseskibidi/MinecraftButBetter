package com.za.minecraft.world.generation;

import com.za.minecraft.world.World;
import com.za.minecraft.world.BlockPos;
import com.za.minecraft.world.blocks.Block;
import com.za.minecraft.world.blocks.BlockType;
import com.za.minecraft.world.chunks.Chunk;
import java.util.Random;

public class TerrainGenerator {
    private static final int SEA_LEVEL = 62;
    private static final int BEDROCK_LEVEL = 5;
    
    private final NoiseGenerator heightNoise;
    private final NoiseGenerator caveNoise;
    private final NoiseGenerator densityNoise;
    private final NoiseGenerator oreNoise;
    private final NoiseGenerator riverNoise;
    private final BiomeGenerator biomeGenerator;
    private final TreeGenerator treeGenerator;
    private final Random random;
    
    public TerrainGenerator(long seed) {
        this.heightNoise = new NoiseGenerator(seed);
        this.caveNoise = new NoiseGenerator(seed + 1000);
        this.densityNoise = new NoiseGenerator(seed + 4000);
        this.oreNoise = new NoiseGenerator(seed + 5000);
        this.riverNoise = new NoiseGenerator(seed + 6000);
        this.biomeGenerator = new BiomeGenerator(seed);
        this.treeGenerator = new TreeGenerator(seed);
        this.random = new Random(seed);
    }
    
    public void generateTerrain(Chunk chunk) {
        int chunkX = chunk.getPosition().x();
        int chunkZ = chunk.getPosition().z();
        
        // Generate basic terrain
        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                int worldX = chunkX * Chunk.CHUNK_SIZE + x;
                int worldZ = chunkZ * Chunk.CHUNK_SIZE + z;
                
                BiomeGenerator.Biome biome = biomeGenerator.getBiome(worldX, worldZ);
                int height = generateHeight(worldX, worldZ, biome);
                
                for (int y = 0; y < Chunk.CHUNK_HEIGHT; y++) {
                    Block block = generateBlock(worldX, y, worldZ, height, biome);
                    chunk.setBlock(x, y, z, block);
                }
            }
        }
    }
    
    public void generateStructures(World world, Chunk chunk) {
        int chunkX = chunk.getPosition().x();
        int chunkZ = chunk.getPosition().z();
		
		generateCavesAndOres(world, chunk);
		
        // Generate trees
        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                int worldX = chunkX * Chunk.CHUNK_SIZE + x;
                int worldZ = chunkZ * Chunk.CHUNK_SIZE + z;
                
                BiomeGenerator.Biome biome = biomeGenerator.getBiome(worldX, worldZ);
                int surfaceY = findSurfaceLevel(world, worldX, worldZ);
                
                if (surfaceY > SEA_LEVEL && surfaceY < Chunk.CHUNK_HEIGHT - 10) {
                    treeGenerator.tryGenerateTree(world, worldX, surfaceY + 1, worldZ, biome.getTreeDensity());
                }
                }
            }
        }

    private void generateCavesAndOres(World world, Chunk chunk) {
		int chunkX = chunk.getPosition().x();
		int chunkZ = chunk.getPosition().z();
		long baseSeed = ((long)chunkX * 73428767L) ^ ((long)chunkZ * 912931L) ^ random.nextLong();
		Random localRandom = new Random(baseSeed);
		int caveSeeds = 2 + localRandom.nextInt(3);
		for (int i = 0; i < caveSeeds; i++) {
			int startX = chunkX * Chunk.CHUNK_SIZE + localRandom.nextInt(Chunk.CHUNK_SIZE);
			int startZ = chunkZ * Chunk.CHUNK_SIZE + localRandom.nextInt(Chunk.CHUNK_SIZE);
			int startY = 20 + localRandom.nextInt(40);
			growCaveMarkov(world, startX, startY, startZ, localRandom);
		}
		placeOreVeins(world, chunkX, chunkZ, localRandom, BlockType.COAL_ORE, 8, 128, 6, 0.78);
		placeOreVeins(world, chunkX, chunkZ, localRandom, BlockType.IRON_ORE, 8, 64, 5, 0.80);
		placeOreVeins(world, chunkX, chunkZ, localRandom, BlockType.GOLD_ORE, 8, 32, 4, 0.85);
	}

	private void growCaveMarkov(World world, int x, int y, int z, Random rng) {
		int length = 60 + rng.nextInt(80);
		double dx = rng.nextDouble() * 2 - 1;
		double dy = (rng.nextDouble() - 0.5) * 0.5;
		double dz = rng.nextDouble() * 2 - 1;
		double speed = 0.9;
		int state = 1;
		for (int s = 0; s < length; s++) {
			if (state == 0) break;
			dx += (rng.nextDouble() - 0.5) * 0.4;
			dy += (rng.nextDouble() - 0.5) * 0.2;
			dz += (rng.nextDouble() - 0.5) * 0.4;
			double invLen = 1.0 / Math.sqrt(dx*dx + dy*dy + dz*dz);
			dx *= invLen * speed;
			dy *= invLen * speed;
			dz *= invLen * speed;
			x += (int)Math.round(dx);
			y += (int)Math.round(dy);
			z += (int)Math.round(dz);
			if (y <= 5 || y >= Chunk.CHUNK_HEIGHT - 8) break;
			carveSphere(world, x, y, z, rng.nextBoolean() ? 2 : 1);
			double continueProb = 0.88;
			if (rng.nextDouble() > continueProb) state = 0; else state = 1;
		}
	}

	private void carveSphere(World world, int cx, int cy, int cz, int radius) {
		for (int dx = -radius; dx <= radius; dx++) {
			for (int dy = -radius; dy <= radius; dy++) {
				for (int dz = -radius; dz <= radius; dz++) {
					int wx = cx + dx;
					int wy = cy + dy;
					int wz = cz + dz;
					if (wy < 1 || wy >= Chunk.CHUNK_HEIGHT) continue;
					double dist = dx*dx + dy*dy + dz*dz;
					if (dist <= radius * radius + 0.5) {
						Block b = world.getBlock(wx, wy, wz);
						if (b.getType() != BlockType.BEDROCK) {
							world.setBlock(new BlockPos(wx, wy, wz), new Block(BlockType.AIR));
                        }
                    }
                }
            }
        }
    }

	private void placeOreVeins(World world, int chunkX, int chunkZ, Random rng, BlockType ore, int attempts, int maxY, int maxLength, double stayProb) {
		for (int i = 0; i < attempts; i++) {
			int x = chunkX * Chunk.CHUNK_SIZE + rng.nextInt(Chunk.CHUNK_SIZE);
			int z = chunkZ * Chunk.CHUNK_SIZE + rng.nextInt(Chunk.CHUNK_SIZE);
			int y = 6 + rng.nextInt(Math.max(1, Math.min(maxY, Chunk.CHUNK_HEIGHT - 6)));
			int length = 8 + rng.nextInt(maxLength);
			int state = 1;
			for (int s = 0; s < length; s++) {
				if (state == 0) break;
				Block current = world.getBlock(x, y, z);
				if (current.getType() == BlockType.STONE || current.getType() == BlockType.DIRT) {
					world.setBlock(new BlockPos(x, y, z), new Block(ore));
				}
				switch (rng.nextInt(6)) {
					case 0 -> x += 1;
					case 1 -> x -= 1;
					case 2 -> z += 1;
					case 3 -> z -= 1;
					case 4 -> y = Math.max(2, y + 1);
					default -> y = Math.max(2, y - 1);
				}
				if (y >= maxY) y -= 2;
				if (y <= 2) y += 2;
				if (rng.nextDouble() > stayProb) state = 0; else state = 1;
			}
		}
	}
    
    private int generateHeight(int x, int z, BiomeGenerator.Biome biome) {
        double warpX = heightNoise.octaveNoise(x * 0.004, z * 0.004, 2, 0.55, 1.0) * 35.0;
        double warpZ = heightNoise.octaveNoise(x * 0.004 + 100.0, z * 0.004 + 100.0, 2, 0.55, 1.0) * 35.0;
        double xw = x + warpX;
        double zw = z + warpZ;

        double continental = heightNoise.octaveNoise(xw * 0.0008, zw * 0.0008, 4, 0.55, 1.0) * 18.0;
        double ridges = Math.abs(heightNoise.noise(xw * 0.003, zw * 0.003));
        ridges = ridges * ridges * ridges * 35.0 - 6.0;
        double hills = heightNoise.octaveNoise(xw * 0.006, zw * 0.006, 3, 0.6, 1.0) * 10.0;
        double erosion = heightNoise.octaveNoise(xw * 0.02, zw * 0.02, 2, 0.5, 1.0) * -6.0;

        double r = Math.abs(riverNoise.noise(x * 0.003, z * 0.003));
        double river = 1.0 - smoothStep(0.0, 0.06, r);

        double h = biome.getBaseHeight() + continental + hills + ridges + erosion - river * 12.0;
        int finalHeight = (int) Math.round(h);
        int minH = SEA_LEVEL - 6;
        int maxH = biome.getMaxHeight();
        if (finalHeight < minH) finalHeight = minH;
        if (finalHeight > maxH) finalHeight = maxH;
        return finalHeight;
    }

    private double smoothStep(double edge0, double edge1, double v) {
        double x = (v - edge0) / (edge1 - edge0);
        if (x < 0.0) x = 0.0;
        if (x > 1.0) x = 1.0;
        return x * x * (3.0 - 2.0 * x);
    }

    private double octave3D(NoiseGenerator n, double x, double y, double z, int octaves, double persistence, double scale) {
        double value = 0.0;
        double amplitude = 1.0;
        double frequency = scale;
        double max = 0.0;
        for (int i = 0; i < octaves; i++) {
            value += n.noise(x * frequency, y * frequency, z * frequency) * amplitude;
            max += amplitude;
            amplitude *= persistence;
            frequency *= 2.0;
        }
        return value / max;
    }
    
    private Block generateBlock(int x, int y, int z, int surfaceHeight, BiomeGenerator.Biome biome) {
        // Bedrock layer (simplified to reduce object creation)
        if (y == 0) {
            return new Block(BlockType.BEDROCK);
        }
        if (y <= BEDROCK_LEVEL && random.nextFloat() < 0.3) {
            return new Block(BlockType.BEDROCK);
        }
        
        // Air above surface
        if (y > surfaceHeight) {
            return new Block(BlockType.AIR);
        }
        
        // Water for areas below sea level
        if (y <= SEA_LEVEL && y > surfaceHeight) {
            return new Block(BlockType.AIR); // Would be water if we had it
        }
        
        if (y == surfaceHeight) {
            BlockType surface = biome.getSurfaceBlock();
            boolean nearSea = Math.abs(surfaceHeight - SEA_LEVEL) <= 2;
            if (nearSea) surface = BlockType.SAND;
            return new Block(surface);
        }
        
        if (biome.getSurfaceBlock() == BlockType.SAND) {
            if (y >= surfaceHeight - 4) return new Block(BlockType.SAND);
        } else {
            if (y >= surfaceHeight - 3) return new Block(BlockType.DIRT);
        }
        
        if (y > 6 && y < surfaceHeight - 5) {
            double cavern = octave3D(caveNoise, x, y, z, 4, 0.55, 0.02);
            double carve = octave3D(densityNoise, x + 100.0, y, z + 100.0, 3, 0.6, 0.03);
            if (cavern > 0.42 && carve > 0.40) return new Block(BlockType.AIR);
            double worm = Math.sin(x * 0.08) + Math.cos(z * 0.08);
            if (worm > 1.6 && y > 15 && y < 55) return new Block(BlockType.AIR);
        }
        
        if (y < 80) {
            double vein = Math.abs(octave3D(oreNoise, x + 200.0, y, z - 200.0, 3, 0.6, 0.03));
            if (y < 128 && vein > 0.70 && oreNoise.noise(x * 0.10, y * 0.10, z * 0.10) > 0.20) {
                return new Block(BlockType.COAL_ORE);
            }
            if (y < 64 && vein > 0.78 && oreNoise.noise(x * 0.12 + 100.0, y * 0.12, z * 0.12 + 100.0) > 0.25) {
                return new Block(BlockType.IRON_ORE);
            }
            if (y < 32 && vein > 0.85 && oreNoise.noise(x * 0.15 + 200.0, y * 0.15, z * 0.15 + 200.0) > 0.35) {
                return new Block(BlockType.GOLD_ORE);
            }
        }
        
        return new Block(BlockType.STONE);
    }
    
    private int findSurfaceLevel(World world, int x, int z) {
        for (int y = Chunk.CHUNK_HEIGHT - 1; y >= 0; y--) {
            Block block = world.getBlock(x, y, z);
            if (!block.isAir()) {
                return y;
            }
        }
        return SEA_LEVEL;
    }
}
