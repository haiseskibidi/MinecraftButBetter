package com.za.zenith.engine.graphics.ui.renderers;

import com.za.zenith.engine.core.GameLoop;
import com.za.zenith.engine.graphics.Shader;
import com.za.zenith.engine.graphics.Texture;
import com.za.zenith.engine.graphics.ui.UIRenderer;
import com.za.zenith.entities.Player;
import com.za.zenith.world.World;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;

import java.nio.ByteBuffer;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.glBindVertexArray;

/**
 * Рендерер миникарты (радара).
 * Отвечает за сэмплирование мира, обновление динамической текстуры и отрисовку элементов интерфейса.
 */
public class MinimapRenderer {
    private static final int MAP_SIZE = 128; // Texture resolution
    private final UIRenderer renderer;
    private final Texture mapTexture;
    private final ByteBuffer buffer;
    private final Shader minimapShader;
    
    private float updateTimer = 0;
    private static final float UPDATE_INTERVAL = 0.1f; // 10 times per second
    
    private Vector3f lastPlayerPos = new Vector3f();

    public MinimapRenderer(UIRenderer renderer) {
        this.renderer = renderer;
        this.buffer = BufferUtils.createByteBuffer(MAP_SIZE * MAP_SIZE * 4);
        
        // Create an empty procedural texture
        byte[] emptyData = new byte[MAP_SIZE * MAP_SIZE * 4];
        this.mapTexture = new Texture(MAP_SIZE, MAP_SIZE, emptyData);
        
        this.minimapShader = new Shader(
            "src/main/resources/shaders/minimap_vertex.glsl",
            "src/main/resources/shaders/minimap_fragment.glsl"
        );
    }

    public void update(Player player, World world, float deltaTime) {
        updateTimer += deltaTime;
        
        float distSq = player.getPosition().distanceSquared(lastPlayerPos);
        
        // Update map only if moved significantly or interval passed (Optimized: distSq > 1.0)
        if (updateTimer >= UPDATE_INTERVAL || distSq > 1.0f) {
            updateTimer = 0;
            lastPlayerPos.set(player.getPosition());
            sampleWorld(player, world);
        }
    }

    private void sampleWorld(Player player, World world) {
        int px = (int) Math.floor(player.getPosition().x);
        int pz = (int) Math.floor(player.getPosition().z);
        
        int half = MAP_SIZE / 2;
        buffer.clear();
        
        for (int z = 0; z < MAP_SIZE; z++) {
            for (int x = 0; x < MAP_SIZE; x++) {
                int worldX = px + (x - half);
                int worldZ = pz + (z - half);
                
                int color = world.getFastSurfaceColor(worldX, worldZ);
                
                // ABGR format in int, need to write RGBA or as-is depending on endianness
                // Our Texture uses GL_RGBA, so we write R, G, B, A
                buffer.put((byte) (color & 0xFF));         // R
                buffer.put((byte) ((color >> 8) & 0xFF));  // G
                buffer.put((byte) ((color >> 16) & 0xFF)); // B
                buffer.put((byte) 255);                    // A
            }
        }
        buffer.flip();
        mapTexture.update(buffer);
    }

    public void render(int x, int y, int size, Player player, int sw, int sh) {
        minimapShader.use();
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        glDisable(GL_DEPTH_TEST);

        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        mapTexture.bind();
        minimapShader.setInt("textureSampler", 0);
        minimapShader.setFloat("uTime", (float) org.lwjgl.glfw.GLFW.glfwGetTime());
        minimapShader.setFloat("uPulse", (float) Math.sin(org.lwjgl.glfw.GLFW.glfwGetTime() * 4.0f) * 0.5f + 0.5f);
        minimapShader.setFloat("uNoiseLevel", player.getNoiseLevel());
        
        // Rotate map based on player yaw
        float rotation = (float) Math.toRadians(player.getRotation().y);
        minimapShader.setFloat("uRotation", rotation);

        float scaleX = (float) size / sw;
        float scaleY = (float) size / sh;
        float posX = (2.0f * (x + size / 2.0f) / sw) - 1.0f;
        float posY = 1.0f - (2.0f * (y + size / 2.0f) / sh);

        minimapShader.setUniform("scale", scaleX, scaleY, 0.0f, 0.0f);
        minimapShader.setUniform("position_offset", posX, posY, 0.0f, 0.0f);

        glBindVertexArray(renderer.getQuadVAO());
        glDrawElements(GL_TRIANGLES, renderer.getQuadIndicesLength(), GL_UNSIGNED_INT, 0);
        
        renderMarkers(x, y, size, player, sw, sh);
        
        glEnable(GL_DEPTH_TEST);
    }

    private void renderMarkers(int x, int y, int size, Player player, int sw, int sh) {
        // Player marker (Center)
        int centerX = x + size / 2;
        int centerY = y + size / 2;
        
        // Draw player as a small white triangle/circle
        renderer.getPrimitivesRenderer().renderRect(centerX - 2, centerY - 2, 4, 4, sw, sh, 1.0f, 1.0f, 1.0f, 1.0f);
        
        // Get locator component from either main hand or offhand
        com.za.zenith.world.items.component.LocatorComponent locator = player.getInventory().getActiveComponent(com.za.zenith.world.items.component.LocatorComponent.class);
        if (locator == null) return;

        // Draw entities (Mobs)
        World world = GameLoop.getInstance().getWorld();
        if (world == null) return;
        
        float mapRange = MAP_SIZE / 2.0f;
        float playerYaw = (float) Math.toRadians(player.getRotation().y);
        float cosR = (float) Math.cos(-playerYaw);
        float sinR = (float) Math.sin(-playerYaw);

        for (com.za.zenith.entities.Entity entity : world.getEntities()) {
            if (entity == player || entity.isRemoved()) continue;
            
            float dx = entity.getPosition().x - player.getPosition().x;
            float dz = entity.getPosition().z - player.getPosition().z;
            
            if (Math.abs(dx) > mapRange || Math.abs(dz) > mapRange) continue;
            
            // Rotate relative position to match map rotation
            float rx = dx * cosR - dz * sinR;
            float rz = dx * sinR + dz * cosR;
            
            // Check circular bounds
            float distSq = rx * rx + rz * rz;
            if (distSq > mapRange * mapRange) continue;
            
            int mx = centerX + (int)(rx * (size / (float)MAP_SIZE));
            int my = centerY + (int)(rz * (size / (float)MAP_SIZE));
            
            float r = 1, g = 1, b = 1;
            if (entity instanceof com.za.zenith.entities.ScoutEntity) {
                r = 1.0f; g = 0.2f; b = 0.2f; // Red for enemies
            } else if (entity instanceof com.za.zenith.entities.ItemEntity) {
                r = 0.8f; g = 0.8f; b = 1.0f; // Blueish for items
            }
            
            renderer.getPrimitivesRenderer().renderRect(mx - 2, my - 2, 4, 4, sw, sh, r, g, b, 1.0f);
        }
    }

    public void cleanup() {
        mapTexture.cleanup();
        minimapShader.cleanup();
    }
}
