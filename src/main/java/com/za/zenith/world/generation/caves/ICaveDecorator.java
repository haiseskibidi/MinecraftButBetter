package com.za.zenith.world.generation.caves;

import com.za.zenith.world.World;
import com.za.zenith.world.chunks.Chunk;

public interface ICaveDecorator {
    void decorate(World world, Chunk chunk);
}