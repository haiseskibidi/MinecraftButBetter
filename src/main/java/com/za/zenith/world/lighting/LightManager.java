package com.za.zenith.world.lighting;

import com.za.zenith.entities.Player;
import com.za.zenith.world.World;
import com.za.zenith.world.blocks.BlockDefinition;
import com.za.zenith.world.blocks.BlockRegistry;
import com.za.zenith.world.chunks.Chunk;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LightManager {
    public static final int MAX_DYNAMIC_LIGHTS = 8;
    
    private static final Map<com.za.zenith.world.BlockPos, LightSource> activeEmitters = new ConcurrentHashMap<>();
    private static final List<LightSource> directionalLights = new ArrayList<>();
    private static final List<LightSource> activeDynamicLights = new ArrayList<>(); // Result for renderer

    public static void update(World world, Player player) {
        activeDynamicLights.clear();
        if (world == null || player == null) return;

        float time = (float)org.lwjgl.glfw.GLFW.glfwGetTime();

        // 1. Held Item Light (Dynamic)
        if (player.getInventory().getSelectedItemStack() != null) {
            com.za.zenith.world.items.Item item = player.getInventory().getSelectedItemStack().getItem();
            if (item.getLightData() != null) {
                activeDynamicLights.add(createSource(item.getLightData(), new Vector3f(player.getPosition()).add(0, 1.5f, 0), player.getRotation(), time));
            }
        }

        // 2. Block Emitters (Static/Registered)
        Vector3f pPos = player.getPosition();
        float renderRangeSq = 48 * 48; // Standard block light render distance
        for (LightSource source : activeEmitters.values()) {
            if (source.position.distanceSquared(pPos) < renderRangeSq) {
                activeDynamicLights.add(source);
            }
        }

        // 3. Sorting by distance to player
        activeDynamicLights.sort(Comparator.comparingDouble(l -> l.position.distanceSquared(player.getPosition())));
    }

    public static void onBlockChange(World world, com.za.zenith.world.BlockPos pos, int blockType) {
        if (blockType == 0) {
            activeEmitters.remove(pos);
            return;
        }

        BlockDefinition def = BlockRegistry.getBlock(blockType);
        if (def != null && def.getEmission() > 0) {
            LightSource source = createSource(def.getLightData(), new Vector3f(pos.x() + 0.5f, pos.y() + 0.5f, pos.z() + 0.5f), new Vector3f(0, -1, 0), (float)org.lwjgl.glfw.GLFW.glfwGetTime());
            activeEmitters.put(new com.za.zenith.world.BlockPos(pos.x(), pos.y(), pos.z()), source);
        } else {
            activeEmitters.remove(pos);
        }
    }

    public static void onChunkLoad(Chunk chunk) {
        // Scan chunk for emitters once upon load
        for (int x = 0; x < Chunk.CHUNK_SIZE; x++) {
            for (int y = 0; y < Chunk.CHUNK_HEIGHT; y++) {
                for (int z = 0; z < Chunk.CHUNK_SIZE; z++) {
                    int type = chunk.getBlockType(x, y, z);
                    if (type != 0) {
                        BlockDefinition def = BlockRegistry.getBlock(type);
                        if (def != null && def.getEmission() > 0) {
                            com.za.zenith.world.BlockPos pos = chunk.toWorldPos(x, y, z);
                            LightSource source = createSource(def.getLightData(), new Vector3f(pos.x() + 0.5f, pos.y() + 0.5f, pos.z() + 0.5f), new Vector3f(0, -1, 0), (float)org.lwjgl.glfw.GLFW.glfwGetTime());
                            activeEmitters.put(pos, source);
                        }
                    }
                }
            }
        }
    }

    public static void onChunkUnload(Chunk chunk) {
        if (chunk == null) return;
        int minX = chunk.getPosition().x() * Chunk.CHUNK_SIZE;
        int minZ = chunk.getPosition().z() * Chunk.CHUNK_SIZE;
        int maxX = minX + Chunk.CHUNK_SIZE;
        int maxZ = minZ + Chunk.CHUNK_SIZE;

        activeEmitters.keySet().removeIf(pos -> pos.x() >= minX && pos.x() < maxX && pos.z() >= minZ && pos.z() < maxZ);
    }

    private static LightSource createSource(LightData data, Vector3f pos, Vector3f dir, float time) {
        LightSource ls = new LightSource(data, pos);
        ls.direction.set(dir);
        if (data.flicker) {
            float f = (float)(Math.sin(time * 15.0) * 0.1 + Math.sin(time * 7.0) * 0.05 + 0.9);
            ls.data = new LightData();
            ls.data.type = data.type;
            ls.data.color.set(data.color);
            ls.data.intensity = data.intensity * f;
            ls.data.radius = data.radius;
            ls.data.spotAngle = data.spotAngle;
        }
        return ls;
    }

    public static void addDirectionalLight(LightSource source) {
        directionalLights.clear(); // Assume single main directional light for now
        directionalLights.add(source);
    }

    public static List<LightSource> getActiveLights() {
        List<LightSource> result = new ArrayList<>(directionalLights);
        int slots = MAX_DYNAMIC_LIGHTS - result.size();
        if (slots > 0) {
            int toAdd = Math.min(slots, activeDynamicLights.size());
            for (int i = 0; i < toAdd; i++) {
                result.add(activeDynamicLights.get(i));
            }
        }
        return result;
    }

    public static void clearAll() {
        activeDynamicLights.clear();
        directionalLights.clear();
        activeEmitters.clear();
    }
}
