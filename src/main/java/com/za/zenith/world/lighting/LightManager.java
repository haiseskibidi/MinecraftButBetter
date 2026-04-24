package com.za.zenith.world.lighting;

import com.za.zenith.entities.Player;
import com.za.zenith.world.World;
import com.za.zenith.world.blocks.Block;
import com.za.zenith.world.blocks.BlockDefinition;
import com.za.zenith.world.blocks.BlockRegistry;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class LightManager {
    public static final int MAX_DYNAMIC_LIGHTS = 8;
    
    private static final List<LightSource> directionalLights = new ArrayList<>();
    private static final List<LightSource> activeDynamicLights = new ArrayList<>();

    private static int updateCounter = 0;
    private static final List<LightSource> cachedWorldLights = new ArrayList<>();

    public static void update(World world, Player player) {
        activeDynamicLights.clear();
        if (world == null || player == null) return;

        float time = (float)org.lwjgl.glfw.GLFW.glfwGetTime();

        // 1. Held Item Light
        if (player.getInventory().getSelectedItemStack() != null) {
            com.za.zenith.world.items.Item item = player.getInventory().getSelectedItemStack().getItem();
            if (item.getLightData() != null) {
                activeDynamicLights.add(createSource(item.getLightData(), new Vector3f(player.getPosition()).add(0, 1.5f, 0), player.getRotation(), time));
            }
        }

        // 2. Scan World (Reliable scan around player) - THROTTLED to once per 20 frames
        if (updateCounter++ % 20 == 0) {
            cachedWorldLights.clear();
            int px = (int)player.getPosition().x;
            int py = (int)player.getPosition().y;
            int pz = (int)player.getPosition().z;
            int range = 12; 
            
            for (int x = px - range; x <= px + range; x++) {
                for (int y = py - range; y <= py + range; y++) {
                    for (int z = pz - range; z <= pz + range; z++) {
                        int raw = world.getRawBlockData(x, y, z);
                        int type = raw >> 8;
                        if (type != 0) {
                            BlockDefinition def = BlockRegistry.getBlock(type);
                            if (def != null && def.getEmission() > 0) {
                                cachedWorldLights.add(createSource(def.getLightData(), new Vector3f(x + 0.5f, y + 0.5f, z + 0.5f), new Vector3f(0, -1, 0), time));
                            }
                        }
                    }
                }
            }
        }
        
        activeDynamicLights.addAll(cachedWorldLights);

        // 3. Sorting by distance to player
        activeDynamicLights.sort(Comparator.comparingDouble(l -> l.position.distanceSquared(player.getPosition())));
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
    }
}
