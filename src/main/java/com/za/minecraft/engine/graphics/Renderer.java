package com.za.minecraft.engine.graphics;

import com.za.minecraft.engine.core.Window;
import com.za.minecraft.engine.graphics.ui.UIRenderer;
import com.za.minecraft.network.GameClient;
import com.za.minecraft.world.World;
import com.za.minecraft.world.chunks.Chunk;
import com.za.minecraft.world.chunks.ChunkMeshGenerator;
import com.za.minecraft.world.physics.RaycastResult;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

public class Renderer {
    private Shader blockShader;
    private DynamicTextureAtlas atlas;
    private final Map<Chunk, Mesh> chunkMeshes;
    private final Matrix4f modelMatrix;
    private DebugRenderer debugRenderer;
    private Framebuffer framebuffer;
    private PostProcessor postProcessor;
    private UIRenderer uiRenderer;
    private boolean fxaaEnabled = false;
    private Mesh highlightMesh;
    private Mesh playerMesh;
    
    public Renderer() {
        this.chunkMeshes = new ConcurrentHashMap<>();
        this.modelMatrix = new Matrix4f();
    }
    
    public void init(int windowWidth, int windowHeight) {
        blockShader = new Shader(
            "src/main/resources/shaders/vertex.glsl",
            "src/main/resources/shaders/fragment.glsl"
        );
        
        atlas = new DynamicTextureAtlas(16);
        for (String key : com.za.minecraft.world.blocks.BlockRegistry.allTextureKeys()) {
            String path = "src/main/resources/" + key;
            atlas.add(key, path);
        }
        atlas.build();
        
        blockShader.use();
        blockShader.setVector3f("lightDirection", new Vector3f(0.2f, -1.0f, 0.2f).normalize());
        blockShader.setVector3f("lightColor", new Vector3f(0.3f, 0.3f, 0.25f)); // Слабый directional свет
        blockShader.setVector3f("ambientLight", new Vector3f(0.85f, 0.85f, 0.9f)); // Яркий ambient как в Minecraft
        blockShader.setInt("textureSampler", 0);
        
        // Передаем UV координаты grass_block_top.png для правильного окрашивания
        float[] grassTopUV = atlas.uvFor("minecraft/textures/block/grass_block_top.png");
        blockShader.setUniform("grassTopUV", grassTopUV[0], grassTopUV[1], grassTopUV[2], grassTopUV[5]); // min_u, min_v, max_u, max_v
        
        // Передаем UV координаты oak_leaves.png для окрашивания листвы
        float[] leavesUV = atlas.uvFor("minecraft/textures/block/oak_leaves.png");
        blockShader.setUniform("leavesUV", leavesUV[0], leavesUV[1], leavesUV[2], leavesUV[5]); // min_u, min_v, max_u, max_v
        
        framebuffer = new Framebuffer(windowWidth, windowHeight);
        postProcessor = new PostProcessor();
        postProcessor.init();
        
        uiRenderer = new UIRenderer();
        uiRenderer.init();
        
        // debugRenderer = new DebugRenderer();
        // debugRenderer.init(); // Временно отключено
    }
    
    public void render(Window window, Camera camera, World world, RaycastResult highlightedBlock, GameClient networkClient) {
        // Resize framebuffer if window size changed
        framebuffer.resize(window.getWidth(), window.getHeight());
        
        // Render scene to framebuffer
        framebuffer.bind();
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
        renderScene(camera, world, networkClient);
        
        // Render block highlighting
        if (highlightedBlock != null && highlightedBlock.isHit()) {
            renderBlockHighlight(camera, highlightedBlock);
        }
        
        // Render to screen with post-processing
        framebuffer.unbind();
        glViewport(0, 0, window.getWidth(), window.getHeight());
        glClear(GL_COLOR_BUFFER_BIT);
        
        if (fxaaEnabled) {
            postProcessor.processFXAA(
                framebuffer.getColorTextureId(), 
                window.getWidth(), 
                window.getHeight()
            );
        } else {
            // Simple passthrough without post-processing
            postProcessor.processPassthrough(framebuffer.getColorTextureId());
        }
        
        // Рендерим UI элементы поверх всего
        uiRenderer.renderCrosshair(window.getWidth(), window.getHeight());
    }
    
    private void renderScene(Camera camera, World world, GameClient networkClient) {
        blockShader.use();
        atlas.bind();
        blockShader.setMatrix4f("projection", camera.getProjectionMatrix());
        blockShader.setMatrix4f("view", camera.getViewMatrix());
        
        for (Chunk chunk : world.getLoadedChunks()) {
            if (chunk.needsMeshUpdate() || !chunkMeshes.containsKey(chunk)) {
                updateChunkMesh(chunk);
            }
            
            Mesh mesh = chunkMeshes.get(chunk);
            if (mesh != null) {
                modelMatrix.identity().translate(
                    chunk.getPosition().x() * Chunk.CHUNK_SIZE,
                    0,
                    chunk.getPosition().z() * Chunk.CHUNK_SIZE
                );
                
                blockShader.setMatrix4f("model", modelMatrix);
                mesh.render();
            }
        }
        
        // Render other players
        renderPlayers(camera, networkClient);
    }
    
    public void renderDebug(float fps, int windowWidth, int windowHeight) {
        if (debugRenderer != null) {
            debugRenderer.renderFPS(fps, windowWidth, windowHeight);
        }
    }
    
    public void toggleFXAA() {
        fxaaEnabled = !fxaaEnabled;
        com.za.minecraft.utils.Logger.info("FXAA toggled: %s", fxaaEnabled ? "ON" : "OFF");
    }
    
    public boolean isFXAAEnabled() {
        return fxaaEnabled;
    }
    
    private void updateChunkMesh(Chunk chunk) {
        Mesh oldMesh = chunkMeshes.get(chunk);
        if (oldMesh != null) {
            oldMesh.cleanup();
        }
        Mesh newMesh = ChunkMeshGenerator.generateMesh(chunk, atlas);
        chunkMeshes.put(chunk, newMesh);
        chunk.setMeshUpdated();
    }
    
    private void renderBlockHighlight(Camera camera, RaycastResult highlightedBlock) {
        // Disable depth writing but keep depth testing for proper z-ordering
        glDepthMask(false);
        glDisable(GL_CULL_FACE);
        
        // Use line mode for wireframe
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        glLineWidth(3.0f);
        
        // Use the block shader but with override color pass
        blockShader.use();
        blockShader.setMatrix4f("projection", camera.getProjectionMatrix());
        blockShader.setMatrix4f("view", camera.getViewMatrix());
        
        // Position matrix for the highlighted block
        modelMatrix.identity().translate(
            highlightedBlock.getBlockPos().x(),
            highlightedBlock.getBlockPos().y(),
            highlightedBlock.getBlockPos().z()
        );
        blockShader.setMatrix4f("model", modelMatrix);
        
        // Enable highlight pass in shader with grey color
        blockShader.setInt("highlightPass", 1);
        blockShader.setVector3f("highlightColor", new Vector3f(0.2f, 0.2f, 0.2f));
        
        // Render the highlight mesh
        if (highlightMesh == null) {
            createHighlightMesh();
        }
        // Slight scale to pull lines outward to avoid z-fighting
        modelMatrix.scale(1.002f, 1.002f, 1.002f);
        blockShader.setMatrix4f("model", modelMatrix);
        // Keep depth test enabled but use polygon offset to avoid z-fighting
        glEnable(GL_POLYGON_OFFSET_LINE);
        glPolygonOffset(-1.0f, -1.0f);
        highlightMesh.render(GL_LINES);
        glDisable(GL_POLYGON_OFFSET_LINE);
        
        // Disable highlight pass
        blockShader.setInt("highlightPass", 0);
        
        // Restore render state
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        glEnable(GL_CULL_FACE);
        glDepthMask(true);
        glLineWidth(1.0f);
    }
    
    private void createHighlightMesh() {
        // Cube vertices for wireframe outline
        float[] positions = {
            // 8 vertices of a unit cube
            0, 0, 0,  // 0: bottom-front-left
            1, 0, 0,  // 1: bottom-front-right
            1, 1, 0,  // 2: top-front-right
            0, 1, 0,  // 3: top-front-left
            0, 0, 1,  // 4: bottom-back-left
            1, 0, 1,  // 5: bottom-back-right
            1, 1, 1,  // 6: top-back-right
            0, 1, 1   // 7: top-back-left
        };
        
        // Empty texture coordinates and normals (not used for wireframe)
        float[] texCoords = new float[positions.length / 3 * 2];
        float[] normals = new float[positions.length];
        float[] blockTypes = new float[positions.length / 3];
        
        // Line indices for wireframe cube (each pair forms a line)
        int[] indices = {
            // Bottom face edges
            0, 1,  1, 5,  5, 4,  4, 0,
            // Top face edges  
            3, 2,  2, 6,  6, 7,  7, 3,
            // Vertical edges connecting bottom and top
            0, 3,  1, 2,  5, 6,  4, 7
        };
        
        highlightMesh = new Mesh(positions, texCoords, normals, blockTypes, indices);
    }
    
    private void renderPlayers(Camera camera, GameClient networkClient) {
        if (networkClient == null || !networkClient.isConnected()) return;
        
        // Убираем highlight режим если был включен
        blockShader.setInt("highlightPass", 0);
        
        // Create player mesh if needed
        if (playerMesh == null) {
            createPlayerMesh();
        }
        
        // Render each remote player as a colored cube
        for (var player : networkClient.getRemotePlayers().values()) {
            modelMatrix.identity()
                .translate(player.getX(), player.getY(), player.getZ())
                .scale(0.6f, 1.8f, 0.6f); // Player size (width, height, width)
            
            blockShader.setMatrix4f("model", modelMatrix);
            
            // Override color to make players stand out (blue-ish)
            blockShader.setInt("highlightPass", 1);
            blockShader.setVector3f("highlightColor", new Vector3f(0.3f, 0.6f, 1.0f)); // Blue color
            
            playerMesh.render();
        }
        
        // Restore normal rendering
        blockShader.setInt("highlightPass", 0);
    }
    
    private void createPlayerMesh() {
        // Simple cube for representing players
        float[] positions = {
            // Front face
            -0.5f, -1.0f,  0.5f,
             0.5f, -1.0f,  0.5f,
             0.5f,  1.0f,  0.5f,
            -0.5f,  1.0f,  0.5f,
            
            // Back face
            -0.5f, -1.0f, -0.5f,
             0.5f, -1.0f, -0.5f,
             0.5f,  1.0f, -0.5f,
            -0.5f,  1.0f, -0.5f,
            
            // Left face
            -0.5f, -1.0f, -0.5f,
            -0.5f, -1.0f,  0.5f,
            -0.5f,  1.0f,  0.5f,
            -0.5f,  1.0f, -0.5f,
            
            // Right face
             0.5f, -1.0f, -0.5f,
             0.5f, -1.0f,  0.5f,
             0.5f,  1.0f,  0.5f,
             0.5f,  1.0f, -0.5f,
            
            // Top face
            -0.5f,  1.0f, -0.5f,
             0.5f,  1.0f, -0.5f,
             0.5f,  1.0f,  0.5f,
            -0.5f,  1.0f,  0.5f,
            
            // Bottom face
            -0.5f, -1.0f, -0.5f,
             0.5f, -1.0f, -0.5f,
             0.5f, -1.0f,  0.5f,
            -0.5f, -1.0f,  0.5f
        };
        
        int[] indices = {
            // Front face
            0, 1, 2,   2, 3, 0,
            // Back face
            4, 6, 5,   6, 4, 7,
            // Left face
            8, 9, 10,  10, 11, 8,
            // Right face
            12, 14, 13, 14, 12, 15,
            // Top face
            16, 17, 18, 18, 19, 16,
            // Bottom face
            20, 22, 21, 22, 20, 23
        };
        
        // Create dummy texture coords and normals (needed for Mesh constructor)
        float[] texCoords = new float[positions.length / 3 * 2]; // 2 coords per vertex
        float[] normals = new float[positions.length]; // 3 coords per vertex  
        float[] blockTypes = new float[positions.length / 3]; // 1 type per vertex
        
        playerMesh = new Mesh(positions, texCoords, normals, blockTypes, indices);
    }
    
    public void cleanup() {
        for (Mesh mesh : chunkMeshes.values()) {
            mesh.cleanup();
        }
        chunkMeshes.clear();
        
        if (debugRenderer != null) {
            debugRenderer.cleanup();
        }
        
        if (framebuffer != null) {
            framebuffer.cleanup();
        }
        
        if (postProcessor != null) {
            postProcessor.cleanup();
        }
        
        if (uiRenderer != null) {
            uiRenderer.cleanup();
        }
        
        if (highlightMesh != null) {
            highlightMesh.cleanup();
        }
        
        if (playerMesh != null) {
            playerMesh.cleanup();
        }
        
        if (atlas != null) {
            atlas.cleanup();
        }
        
        if (blockShader != null) {
            blockShader.cleanup();
        }
    }
}
