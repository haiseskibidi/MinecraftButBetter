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
    private static final int MIN_HEIGHT = 1;
    private static final int MAX_HEIGHT = 128;
    
    private final PerlinNoise heightNoise;
    private final PerlinNoise caveNoise;
    private final PerlinNoise densityNoise;
    private final PerlinNoise oreNoise;
    private final BiomeGenerator biomeGenerator;
    private final TreeGenerator treeGenerator;
    private final Random random;
    
    public TerrainGenerator(long seed) {
        this.heightNoise = new PerlinNoise(seed);
        this.caveNoise = new PerlinNoise(seed + 1000);
        this.densityNoise = new PerlinNoise(seed + 4000);
        this.oreNoise = new PerlinNoise(seed + 5000);
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
		
		if (localRandom.nextDouble() < 0.15) {
			int startX = chunkX * Chunk.CHUNK_SIZE + localRandom.nextInt(Chunk.CHUNK_SIZE);
			int startZ = chunkZ * Chunk.CHUNK_SIZE + localRandom.nextInt(Chunk.CHUNK_SIZE);
			int startY = 15 + localRandom.nextInt(40);
			growLargeCave(world, startX, startY, startZ, localRandom);
		}
		
		int smallCaves = 1 + localRandom.nextInt(2);
		for (int i = 0; i < smallCaves; i++) {
			int startX = chunkX * Chunk.CHUNK_SIZE + localRandom.nextInt(Chunk.CHUNK_SIZE);
			int startZ = chunkZ * Chunk.CHUNK_SIZE + localRandom.nextInt(Chunk.CHUNK_SIZE);
			int startY = 10 + localRandom.nextInt(50);
			growSmallCave(world, startX, startY, startZ, localRandom);
		}
		
		placeOreVeins(world, chunkX, chunkZ, localRandom, BlockType.COAL_ORE, 10, 128, 7, 0.76);
		placeOreVeins(world, chunkX, chunkZ, localRandom, BlockType.IRON_ORE, 8, 64, 6, 0.78);
		placeOreVeins(world, chunkX, chunkZ, localRandom, BlockType.GOLD_ORE, 4, 32, 5, 0.82);
	}

	private void growSmallCave(World world, int x, int y, int z, Random rng) {
		int length = 40 + rng.nextInt(60);
		double dx = rng.nextDouble() * 2 - 1;
		double dy = (rng.nextDouble() - 0.5) * 0.4;
		double dz = rng.nextDouble() * 2 - 1;
		double speed = 0.8;
		int state = 1;
		for (int s = 0; s < length; s++) {
			if (state == 0) break;
			dx += (rng.nextDouble() - 0.5) * 0.3;
			dy += (rng.nextDouble() - 0.5) * 0.2;
			dz += (rng.nextDouble() - 0.5) * 0.3;
			double invLen = 1.0 / Math.sqrt(dx*dx + dy*dy + dz*dz);
			dx *= invLen * speed;
			dy *= invLen * speed;
			dz *= invLen * speed;
			x += (int)Math.round(dx);
			y += (int)Math.round(dy);
			z += (int)Math.round(dz);
			if (y <= 5 || y >= Chunk.CHUNK_HEIGHT - 8) break;
			int radius = 1 + rng.nextInt(2);
			carveSphere(world, x, y, z, radius);
			double continueProb = 0.85;
			if (rng.nextDouble() > continueProb) state = 0; else state = 1;
		}
	}
	
	private void growLargeCave(World world, int x, int y, int z, Random rng) {
		int length = 100 + rng.nextInt(150);
		double dx = rng.nextDouble() * 2 - 1;
		double dy = (rng.nextDouble() - 0.5) * 0.5;
		double dz = rng.nextDouble() * 2 - 1;
		double speed = 1.5;
		int state = 1;
		for (int s = 0; s < length; s++) {
			if (state == 0) break;
			dx += (rng.nextDouble() - 0.5) * 0.6;
			dy += (rng.nextDouble() - 0.5) * 0.3;
			dz += (rng.nextDouble() - 0.5) * 0.6;
			double invLen = 1.0 / Math.sqrt(dx*dx + dy*dy + dz*dz);
			dx *= invLen * speed;
			dy *= invLen * speed;
			dz *= invLen * speed;
			x += (int)Math.round(dx);
			y += (int)Math.round(dy);
			z += (int)Math.round(dz);
			if (y <= 5 || y >= Chunk.CHUNK_HEIGHT - 8) break;
			int radius = 3 + rng.nextInt(3);
			if (rng.nextDouble() < 0.2) {
				radius = 5 + rng.nextInt(4);
			}
			carveSphere(world, x, y, z, radius);
			double continueProb = 0.94;
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
        double result = 0;
        double amplitude = 1;
        double frequency = 1;
        double maxValue = 0;
        
        for (int i = 0; i < 4; i++) {
            double sampleX = x / 80.0 * frequency;
            double sampleZ = z / 80.0 * frequency;
            
            double perlinValue = heightNoise.noise(sampleX, sampleZ);
            result += perlinValue * amplitude;
            
            maxValue += amplitude;
            amplitude *= 0.5;
            frequency *= 2;
        }
        
        result = result / maxValue;
        
        double continentalnessX = x / 400.0;
        double continentalnessZ = z / 400.0;
        double continentalness = heightNoise.noise(continentalnessX, continentalnessZ);
        
        double baseHeight = 64;
        double heightVariation = 20 + continentalness * 15;
        double height = baseHeight + result * heightVariation;
        
        return (int)Math.round(height);
    }


    private double octave3D(PerlinNoise n, double x, double y, double z, int octaves, double persistence, double scale) {
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
        
        if (y > 8 && y < surfaceHeight - 5) {
            double spaghetti = Math.abs(octave3D(caveNoise, x + 500, y, z, 3, 0.5, 0.035));
            if (spaghetti < 0.08 && y > 10 && y < 55) {
                return new Block(BlockType.AIR);
            }
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
