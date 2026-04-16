package com.za.zenith.engine.graphics;

import com.za.zenith.engine.core.Window;
import com.za.zenith.engine.graphics.ui.UIRenderer;
import com.za.zenith.network.GameClient;
import com.za.zenith.world.BlockPos;
import com.za.zenith.world.World;
import com.za.zenith.world.blocks.Block;
import com.za.zenith.world.chunks.Chunk;
import com.za.zenith.world.chunks.ChunkMeshGenerator;
import com.za.zenith.world.physics.RaycastResult;
import com.za.zenith.world.items.ItemStack;
import com.za.zenith.engine.graphics.model.ViewmodelRenderer;
import com.za.zenith.engine.graphics.model.Viewmodel;
import com.za.zenith.engine.graphics.model.ModelNode;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.za.zenith.engine.core.GameLoop;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL11.*;

public class Renderer {
    private Shader blockShader;
    private DynamicTextureAtlas atlas;
    private final Map<Chunk, ChunkMeshGenerator.ChunkMeshResult> chunkMeshes;
    private final Matrix4f modelMatrix;
    private DebugRenderer debugRenderer;
    private Framebuffer framebuffer;
    private PostProcessor postProcessor;
    private UIRenderer uiRenderer;
    private CarvingRenderer carvingRenderer;
    private BlockHighlightRenderer highlightRenderer;
    private final ParticleRenderer particleRenderer = new ParticleRenderer();
    private final SkyRenderer skyRenderer = new SkyRenderer();
    private boolean fxaaEnabled = false;
    private Mesh playerMesh;
    private Mesh previewMesh;
    private final Map<Integer, Mesh> blockMeshCache = new java.util.HashMap<>();
    private final Vector3f lightDirection;
    private final Vector3f lightColor = new Vector3f(1.0f, 0.95f, 0.85f);
    private final Vector3f ambientLight = new Vector3f(0.4f, 0.45f, 0.55f);
    
    // Viewmodel
    private final ViewmodelRenderer viewmodelRenderer = new ViewmodelRenderer();
    private Shader viewmodelShader;
    private final com.za.zenith.engine.graphics.vfx.MiningVFXManager vfxManager = new com.za.zenith.engine.graphics.vfx.MiningVFXManager();
    
    // View Model caching
    private Mesh heldItemMesh;
    private int lastHeldTypeId = -1;
    private boolean lastHeldIsBlock = false;
    
    private Block currentPreviewBlock;
    private com.za.zenith.world.BlockPos previewPos;
    
    // Impact Wobble
    private Block currentBreakingBlock;
    private com.za.zenith.world.BlockPos breakingPos;
    private Vector3f breakingHitPoint = new Vector3f();
    private Vector3f weakSpotPos = new Vector3f();
    private Vector3f weakSpotColor = new Vector3f(1.0f, 1.0f, 1.0f);
    private final Vector4f[] hitHistory = new Vector4f[16];
    private int hitCount = 0;
    private Mesh breakingMesh;
    private Mesh holeMesh;
    private com.za.zenith.world.BlockPos holePos;
    private float breakingProgress = 0.0f;
    private float wobbleTimer = 0.0f;

    private final Map<com.za.zenith.world.items.Item, Mesh> itemMeshCache = new java.util.HashMap<>();
    private final Map<com.za.zenith.entities.EntityDefinition, Mesh> entityDefMeshCache = new java.util.HashMap<>();

    public Renderer() {
        this.chunkMeshes = new ConcurrentHashMap<>();
        this.modelMatrix = new Matrix4f();
        this.lightDirection = new Vector3f(0.2f, -1.0f, 0.2f).normalize();
    }

    public void setBreakingBlock(com.za.zenith.world.BlockPos pos, Block block, float progress, float timer, Vector3f localHitPoint, Vector3f localWeakSpot, Vector3f color, java.util.List<Vector4f> history) {
        if (block == null) {
            this.breakingPos = null;
            this.currentBreakingBlock = null;
            this.breakingProgress = 0.0f;
            this.wobbleTimer = 0.0f;
            return;
        }
        
        if (currentBreakingBlock == null || !pos.equals(this.breakingPos) || currentBreakingBlock.getType() != block.getType() || currentBreakingBlock.getMetadata() != block.getMetadata()) {
            if (breakingMesh != null) breakingMesh.cleanup();
            breakingMesh = ChunkMeshGenerator.generateSingleBlockMesh(block, atlas);
            currentBreakingBlock = block;
        }
        this.breakingPos = pos;
        this.breakingProgress = progress;
        this.wobbleTimer = timer;
        if (localHitPoint != null) this.breakingHitPoint.set(localHitPoint);
        if (localWeakSpot != null) this.weakSpotPos.set(localWeakSpot);
        if (color != null) this.weakSpotColor.set(color);
        
        this.hitCount = history != null ? Math.min(16, history.size()) : 0;
        if (history != null) {
            for (int i = 0; i < hitCount; i++) {
                if (hitHistory[i] == null) hitHistory[i] = new Vector4f();
                hitHistory[i].set(history.get(i));
            }
        }
    }

    public void setPreviewBlock(com.za.zenith.world.BlockPos pos, Block block) {
        if (block == null) {
            this.previewPos = null;
            this.currentPreviewBlock = null;
            return;
        }
        if (currentPreviewBlock == null || currentPreviewBlock.getType() != block.getType() || currentPreviewBlock.getMetadata() != block.getMetadata()) {
            if (previewMesh != null) previewMesh.cleanup();
            previewMesh = ChunkMeshGenerator.generateSingleBlockMesh(block, atlas);
            currentPreviewBlock = block;
        }
        this.previewPos = pos;
    }
    
    public void init(int windowWidth, int windowHeight) {
        glEnable(GL_DEPTH_TEST);
        glEnable(GL_CULL_FACE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        blockShader = new Shader("src/main/resources/shaders/vertex.glsl", "src/main/resources/shaders/fragment.glsl");
        atlas = new DynamicTextureAtlas(16);
        for (com.za.zenith.world.blocks.BlockDefinition def : com.za.zenith.world.blocks.BlockRegistry.getRegistry().values()) {
            if (def.getTextures() != null) {
                for (int face = 0; face < 7; face++) {
                    String key = def.getTextures().getTextureForFace(face);
                    if (key != null) atlas.add(key, "src/main/resources/" + key);
                }
            }
            if (def.getUpperTexture() != null) atlas.add(def.getUpperTexture(), "src/main/resources/" + def.getUpperTexture());
        }
        for (com.za.zenith.world.items.Item item : com.za.zenith.world.items.ItemRegistry.getAllItems().values()) {
            String tex = item.getTexturePath();
            if (tex != null && !tex.isEmpty()) atlas.add(tex, "src/main/resources/" + tex);
        }
        
        // Add all viewmodels to atlas
        for (com.za.zenith.engine.graphics.model.ViewmodelDefinition vmDef : com.za.zenith.engine.graphics.model.ModelRegistry.getAllViewmodels()) {
            if (vmDef.texture != null) atlas.add(vmDef.texture, "src/main/resources/" + vmDef.texture);
        }

        for (com.za.zenith.entities.EntityDefinition def : com.za.zenith.entities.EntityRegistry.getAll().values()) {
            if ("item".equals(def.modelType())) {
                String tex = def.texture();
                if (tex != null && !tex.isEmpty()) atlas.add(tex, "src/main/resources/" + tex);
            }
        }
        atlas.build();
        blockShader.use();
        blockShader.setVector3f("lightDirection", new Vector3f(0.2f, -1.0f, 0.2f).normalize());
        blockShader.setVector3f("lightColor", new Vector3f(0.3f, 0.3f, 0.25f));
        blockShader.setVector3f("ambientLight", new Vector3f(0.85f, 0.85f, 0.9f));
        blockShader.setInt("textureSampler", 0);
        float[] glassUV = atlas.uvFor("zenith/textures/block/glass.png");
        blockShader.setFloat("glassLayer", glassUV[2]);
        framebuffer = new Framebuffer(windowWidth, windowHeight);
        postProcessor = new PostProcessor();
        postProcessor.init();
        uiRenderer = new UIRenderer();
        uiRenderer.init();
        viewmodelShader = new Shader("src/main/resources/shaders/viewmodel_vertex.glsl", "src/main/resources/shaders/viewmodel_fragment.glsl");
        carvingRenderer = new CarvingRenderer();
        highlightRenderer = new BlockHighlightRenderer();
        particleRenderer.init();
        skyRenderer.init();
    }
    
    public void render(Window window, Camera camera, World world, RaycastResult highlightedBlock, com.za.zenith.network.GameClient networkClient, float alpha, float deltaTime) {
        vfxManager.update(deltaTime, world.getPlayer(), GameLoop.getInstance().getInputManager().getMiningController());
        framebuffer.resize(window.getWidth(), window.getHeight());
        framebuffer.bind();

        // Calculate time of day
        com.za.zenith.world.WorldSettings worldSettings = com.za.zenith.world.WorldSettings.getInstance();
        float worldTime = world.getWorldTime();
        float dayLength = worldSettings.dayLength;
        float timeRatio = worldTime / dayLength;
        
        // 6000 is noon (0.25). 18000 is midnight (0.75).
        float angle = (timeRatio - 0.25f) * (float)Math.PI * 2.0f;

        lightDirection.set(0.2f, -(float)Math.cos(angle), (float)Math.sin(angle)).normalize();
        
        float sunIntensity = Math.max(0.0f, (float)Math.cos(angle));
        float moonIntensity = Math.max(0.0f, -(float)Math.cos(angle));

        // Use light from the moon if it's night
        Vector3f finalLightDir = new Vector3f(lightDirection);
        if (moonIntensity > sunIntensity) {
            finalLightDir.negate();
        }

        // Sky color interpolation
        Vector3f daySky = new Vector3f(0.6f, 0.8f, 1.0f);
        Vector3f nightSky = new Vector3f(0.02f, 0.02f, 0.05f);
        Vector3f currentSky = new Vector3f(nightSky).lerp(daySky, sunIntensity);

        glClearColor(currentSky.x, currentSky.y, currentSky.z, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Render Sky (Sun/Moon)
        skyRenderer.render(camera, lightDirection);

        // Blend Light Colors
        Vector3f sunCol = new Vector3f(worldSettings.sunLightColor[0], worldSettings.sunLightColor[1], worldSettings.sunLightColor[2]);
        Vector3f moonCol = new Vector3f(worldSettings.moonLightColor[0], worldSettings.moonLightColor[1], worldSettings.moonLightColor[2]);
        Vector3f ambCol = new Vector3f(worldSettings.ambientColor[0], worldSettings.ambientColor[1], worldSettings.ambientColor[2]);

        Vector3f currentLightColor = new Vector3f(sunCol).mul(sunIntensity).add(new Vector3f(moonCol).mul(moonIntensity * 0.5f));
        Vector3f currentAmbient = new Vector3f(ambCol).mul(0.2f + 0.8f * sunIntensity + 0.3f * moonIntensity);

        Vector3f playerLightPos = world.getPlayer() != null ? new Vector3f(world.getPlayer().getPosition()).add(0, 1.5f, 0) : new Vector3f();
        float playerLightLevel = 0;
        if (world.getPlayer() != null && world.getPlayer().getInventory().getSelectedItemStack() != null) {
            playerLightLevel = world.getPlayer().getInventory().getSelectedItemStack().getItem().getLightLevel();
        }

        renderScene(camera, world, networkClient, alpha, finalLightDir, currentLightColor, currentAmbient, playerLightPos, playerLightLevel);

        if (highlightedBlock != null && highlightedBlock.isHit()) renderBlockHighlight(camera, world, highlightedBlock, alpha);
        if (previewPos != null && previewMesh != null) renderPreviewBlock(camera, alpha);

        renderViewModel(camera, world.getPlayer());

        // Render Particles
        particleRenderer.render(camera, com.za.zenith.world.particles.ParticleManager.getInstance().getActiveParticles(), atlas, alpha, finalLightDir, currentLightColor, currentAmbient);

        framebuffer.unbind();
        glViewport(0, 0, window.getWidth(), window.getHeight());
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        if (fxaaEnabled) postProcessor.processFXAA(framebuffer.getColorTextureId(), framebuffer.getDepthTextureId(), window.getWidth(), window.getHeight());
        else postProcessor.processPassthrough(framebuffer.getColorTextureId(), framebuffer.getDepthTextureId(), window.getWidth(), window.getHeight());

        uiRenderer.renderCrosshair(window.getWidth(), window.getHeight());
        uiRenderer.renderLogo(window.getWidth(), window.getHeight());
        uiRenderer.renderHotbar(window.getWidth(), window.getHeight(), atlas);
        uiRenderer.renderPauseMenu(window.getWidth(), window.getHeight());
    }

    private void renderViewModel(Camera camera, com.za.zenith.entities.Player player) {        if (player == null) return;
        glDisable(GL_CULL_FACE);
        glDepthRange(0.0, 0.05);
        
        viewmodelShader.use();
        atlas.bind();
        Matrix4f viewModelProjection = new Matrix4f().setPerspective((float)Math.toRadians(70.0f), camera.getAspectRatio(), 0.01f, 1000.0f);
        viewmodelShader.setMatrix4f("projection", viewModelProjection);
        viewmodelShader.setMatrix4f("view", new Matrix4f().identity());
        viewmodelShader.setVector3f("lightDirection", new Vector3f(0.4f, -0.8f, 0.4f).normalize());
        viewmodelShader.setVector3f("lightColor", lightColor);
        viewmodelShader.setVector3f("ambientLight", ambientLight);
        viewmodelShader.setVector3f("uGrassColor", ColorProvider.getGrassColor());
        viewmodelShader.setFloat("uTime", (float)org.lwjgl.glfw.GLFW.glfwGetTime());
        
        // Состояние рук
        viewmodelShader.setVector3f("uCondition", new Vector3f(player.getDirt(), player.getBlood(), player.getWetness()));

        Viewmodel vm = player.getViewmodel();
        if (vm != null) {
            if (!vm.root.children.isEmpty() && vm.root.children.get(0).mesh == null) {
                vm.initMeshes(atlas);
            }
            
            ItemStack mainHand = player.getInventory().getSelectedItemStack();
            ItemStack offHand = player.getInventory().getStack(com.za.zenith.entities.Inventory.SLOT_OFFHAND);
            viewmodelRenderer.render(vm, viewmodelShader, atlas, player, mainHand, offHand, vfxManager.getHandHeat(), vfxManager.getItemHeat());
        }

        glDepthRange(0.0, 1.0); 
        glEnable(GL_CULL_FACE);
    }
    
    private void renderPreviewBlock(Camera camera, float alpha) {
        glDisable(GL_CULL_FACE);
        blockShader.use();
        blockShader.setMatrix4f("projection", camera.getProjectionMatrix());
        blockShader.setMatrix4f("view", camera.getViewMatrix(alpha));
        // Add 0.5f to x and z to center the mesh which is offset by -0.5f in ChunkMeshGenerator
        modelMatrix.identity().translate(previewPos.x() + 0.5f, previewPos.y(), previewPos.z() + 0.5f);
        blockShader.setMatrix4f("model", modelMatrix);
        blockShader.setInt("previewPass", 1);
        blockShader.setFloat("previewAlpha", 0.35f);
        previewMesh.render();
        blockShader.setInt("previewPass", 0);
        glEnable(GL_CULL_FACE);
    }
    
    private void renderScene(Camera camera, World world, com.za.zenith.network.GameClient networkClient, float alpha, Vector3f lightDir, Vector3f lightCol, Vector3f ambient, Vector3f playerLightPos, float playerLightLevel) {
        blockShader.use();
        atlas.bind();
        blockShader.setMatrix4f("projection", camera.getProjectionMatrix());
        blockShader.setMatrix4f("view", camera.getViewMatrix(alpha));
        blockShader.setFloat("uTime", (float)org.lwjgl.glfw.GLFW.glfwGetTime());
        blockShader.setFloat("uSwayOverride", -1.0f);
        blockShader.setVector3f("uGrassColor", ColorProvider.getGrassColor());
        
        blockShader.setVector3f("lightDirection", lightDir);
        blockShader.setVector3f("lightColor", lightCol);
        blockShader.setVector3f("ambientLight", ambient);
        blockShader.setVector3f("uPlayerLightPos", playerLightPos);
        blockShader.setVector3f("uPlayerLightPos", playerLightPos);
        blockShader.setFloat("uPlayerLightLevel", playerLightLevel);

        blockShader.setBoolean("useMask", false);
        blockShader.setBoolean("previewPass", false);
        blockShader.setBoolean("isHand", false); // Ensure world blocks are not masked as hands
        blockShader.setFloat("brightnessMultiplier", 1.0f);
        blockShader.setInt("highlightPass", 0);
        blockShader.setBoolean("uIsProxy", false);
        blockShader.setInt("uHitCount", 0);
        blockShader.setFloat("uBreakingProgress", 0.0f);
        blockShader.setInt("uBreakingPattern", 0);
        blockShader.setVector3f("uBreakingHitPoint", new Vector3f(0.5f));
        blockShader.setVector3f("uWeakSpotPos", new Vector3f(0.5f));
        blockShader.setVector3f("uWeakSpotColor", new Vector3f(1.0f, 1.0f, 1.0f));
        
        if (breakingPos != null) {
            if (holeMesh == null || !breakingPos.equals(holePos)) {
                if (holeMesh != null) holeMesh.cleanup();
                holeMesh = ChunkMeshGenerator.generateHoleMesh(breakingPos, world, atlas);
                holePos = breakingPos;
            }
            blockShader.setVector3f("uHiddenBlockPos", new Vector3f(breakingPos.x(), breakingPos.y(), breakingPos.z()));
        } else {
            if (holeMesh != null) {
                holeMesh.cleanup();
                holeMesh = null;
                holePos = null;
            }
            blockShader.setVector3f("uHiddenBlockPos", new Vector3f(0, -100, 0)); // Hide logic disabled
        }

        for (Chunk chunk : world.getLoadedChunks()) {
            if (chunk.needsMeshUpdate() || !chunkMeshes.containsKey(chunk)) updateChunkMesh(chunk, world);
            ChunkMeshGenerator.ChunkMeshResult result = chunkMeshes.get(chunk);
            if (result != null && result.opaqueMesh != null) {
                modelMatrix.identity().translate(chunk.getPosition().x() * Chunk.CHUNK_SIZE, 0, chunk.getPosition().z() * Chunk.CHUNK_SIZE);
                blockShader.setMatrix4f("model", modelMatrix);
                result.opaqueMesh.render();
            }
        }
        glDepthMask(false);
        for (Chunk chunk : world.getLoadedChunks()) {
            ChunkMeshGenerator.ChunkMeshResult result = chunkMeshes.get(chunk);
            if (result != null && result.translucentMesh != null) {
                modelMatrix.identity().translate(chunk.getPosition().x() * Chunk.CHUNK_SIZE, 0, chunk.getPosition().z() * Chunk.CHUNK_SIZE);
                blockShader.setMatrix4f("model", modelMatrix);
                result.translucentMesh.render();
            }
        }
        glDepthMask(true);
        
        if (holeMesh != null) {
            blockShader.setVector3f("uHiddenBlockPos", new Vector3f(0, -100, 0)); // Stop discarding for hole
            modelMatrix.identity().translate(breakingPos.x(), breakingPos.y(), breakingPos.z());
            blockShader.setMatrix4f("model", modelMatrix);
            holeMesh.render();
        }
        
        if (breakingPos != null && breakingMesh != null && currentBreakingBlock != null) {
            renderBreakingProxyBlock(camera, alpha);
        }

        renderPersistentScars(camera, world, alpha);
        
        renderEntities(camera, world, alpha);
        renderBlockEntities(camera, world, alpha);
        renderPlayers(camera, networkClient, alpha);
    }

    private void renderPersistentScars(Camera camera, World world, float alpha) {
        if (world.getBlockDamageMap().isEmpty()) return;
        
        blockShader.use();
        glEnable(GL_POLYGON_OFFSET_FILL);
        glPolygonOffset(-1.0f, -1.0f);

        for (Map.Entry<com.za.zenith.world.BlockPos, World.BlockDamageInstance> entry : world.getBlockDamageMap().entrySet()) {
            com.za.zenith.world.BlockPos pos = entry.getKey();
            // Skip the block that is currently being broken with animations
            if (breakingPos != null && pos.equals(breakingPos)) continue;

            World.BlockDamageInstance info = entry.getValue();
            com.za.zenith.world.blocks.Block block = world.getBlock(pos);
            int typeId = block.getType();
            if (typeId == 0) continue; // Should not happen for damage map
            
            Mesh mesh = blockMeshCache.get(typeId);
            if (mesh == null) {
                mesh = ChunkMeshGenerator.generateSingleBlockMesh(block, atlas);
                if (mesh != null) blockMeshCache.put(typeId, mesh);
            }

            if (mesh != null) {
                com.za.zenith.world.blocks.BlockDefinition def = com.za.zenith.world.blocks.BlockRegistry.getBlock(typeId);
                
                blockShader.setBoolean("uIsProxy", true);
                blockShader.setVector3f("uWobbleScale", new Vector3f(1.0f));
                blockShader.setVector3f("uWobbleOffset", new Vector3f(0.0f));
                blockShader.setFloat("uWobbleShake", 0.0f);
                
                float maxHealth = def.getHardness() * 10.0f;
                blockShader.setFloat("uBreakingProgress", info.getDamage() / maxHealth);
                blockShader.setInt("uBreakingPattern", def.getBreakingPattern());
                blockShader.setVector3f("uWeakSpotPos", new Vector3f(0, -100, 0)); // No active weak spot
                
                int hitCount = Math.min(16, info.getHitHistory().size());
                blockShader.setInt("uHitCount", hitCount);
                for (int i = 0; i < hitCount; i++) {
                    blockShader.setVector4f("uHitHistory[" + i + "]", info.getHitHistory().get(i));
                }

                modelMatrix.identity().translate(pos.x() + 0.5f, pos.y(), pos.z() + 0.5f);
                blockShader.setMatrix4f("model", modelMatrix);
                mesh.render();
            }
        }
        
        blockShader.setBoolean("uIsProxy", false);
        glDisable(GL_POLYGON_OFFSET_FILL);
    }

    private void renderBreakingProxyBlock(Camera camera, float alpha) {
        blockShader.use();
        
        // Evaluate Animation Profile
        com.za.zenith.world.blocks.BlockDefinition def = com.za.zenith.world.blocks.BlockRegistry.getBlock(currentBreakingBlock.getType());
        String animName = (def != null && def.getWobbleAnimation() != null) ? def.getWobbleAnimation() : "block_wobble";
        
        com.za.zenith.entities.parkour.animation.AnimationProfile profile = com.za.zenith.entities.parkour.animation.AnimationRegistry.get(animName);
        
        float scaleX = 1.0f, scaleY = 1.0f, scaleZ = 1.0f;
        float offsetX = 0.0f, offsetY = 0.0f, offsetZ = 0.0f;
        float shake = 0.0f;
        
        if (profile != null) {
            float normTimer = wobbleTimer / Math.max(0.001f, profile.getDuration());
            scaleX = profile.evaluate("scale_x", normTimer, 1.0f);
            scaleY = profile.evaluate("scale_y", normTimer, 1.0f);
            scaleZ = profile.evaluate("scale_z", normTimer, 1.0f);
            offsetX = profile.evaluate("offset_x", normTimer, 0.0f);
            offsetY = profile.evaluate("offset_y", normTimer, 0.0f);
            offsetZ = profile.evaluate("offset_z", normTimer, 0.0f);
            shake = profile.evaluate("shake", normTimer, 0.0f);
        }
        
        blockShader.setVector3f("uWobbleScale", new Vector3f(scaleX, scaleY, scaleZ));
        blockShader.setVector3f("uWobbleOffset", new Vector3f(offsetX, offsetY, offsetZ));
        blockShader.setFloat("uWobbleShake", shake);
        blockShader.setFloat("uWobbleTime", wobbleTimer);
        blockShader.setFloat("uBreakingProgress", breakingProgress);
        blockShader.setInt("uBreakingPattern", def.getBreakingPattern());
        blockShader.setVector3f("uBreakingHitPoint", breakingHitPoint);
        blockShader.setVector3f("uWeakSpotPos", weakSpotPos);
        blockShader.setVector3f("uWeakSpotColor", weakSpotColor);
        blockShader.setInt("uHitCount", hitCount);
        for (int i = 0; i < hitCount; i++) {
            blockShader.setVector4f("uHitHistory[" + i + "]", hitHistory[i]);
        }
        
        if (breakingMesh != null) {
            blockShader.setBoolean("uIsProxy", true);
            // Add 0.5f to x and z to center the mesh which is offset by -0.5f in ChunkMeshGenerator
            modelMatrix.identity().translate(breakingPos.x() + 0.5f, breakingPos.y(), breakingPos.z() + 0.5f);
            blockShader.setMatrix4f("model", modelMatrix);
            breakingMesh.render();
            blockShader.setBoolean("uIsProxy", false);
        }
    }

    
    private void renderBlockHighlight(Camera camera, World world, RaycastResult highlightedBlock, float alpha) {
        if (highlightRenderer != null) {
            highlightRenderer.render(camera, world, highlightedBlock, blockShader, modelMatrix, alpha, breakingPos, currentBreakingBlock, wobbleTimer);
        }
    }

    private void renderEntities(Camera camera, World world, float alpha) {
        if (world.getEntities().isEmpty()) return;

        blockShader.use();
        for (com.za.zenith.entities.Entity entity : world.getEntities()) {
            Vector3f interpPos = entity.getInterpolatedPosition(alpha);
            
            if (entity instanceof com.za.zenith.entities.ScoutEntity scout) {
                if (playerMesh == null) createPlayerMesh();
                modelMatrix.identity()
                    .translate(interpPos.x(), interpPos.y(), interpPos.z())
                    .rotateY(entity.getRotation().y);

                blockShader.setMatrix4f("model", modelMatrix);
                blockShader.setInt("highlightPass", 1);
                switch (scout.getCurrentState()) {
                    case CHASE: blockShader.setVector3f("highlightColor", new Vector3f(1.0f, 0.0f, 0.0f)); break;
                    case SEARCH: blockShader.setVector3f("highlightColor", new Vector3f(1.0f, 0.5f, 0.0f)); break;
                    default: blockShader.setVector3f("highlightColor", new Vector3f(0.5f, 0.5f, 0.5f)); break;
                }
                playerMesh.render();
            } else if (entity instanceof com.za.zenith.entities.ItemEntity itemEntity) {
                com.za.zenith.world.items.Item item = itemEntity.getStack().getItem();
                Mesh mesh = itemMeshCache.get(item);

                if (mesh == null) {
                    if (item.isBlock()) {
                        mesh = ChunkMeshGenerator.generateSingleBlockMesh(new Block(item.getId()), atlas);
                    } else {
                        mesh = com.za.zenith.world.items.ItemMeshGenerator.generateItemMesh(item.getTexturePath(), atlas, item.getId());
                    }
                    if (mesh != null) itemMeshCache.put(item, mesh);
                }

                if (mesh != null) {
                    float age = itemEntity.getAge() + alpha * 0.016f; 
                    float bob = (float) Math.sin(age * 2.5f) * 0.02f;
                    
                    float baseScale = 0.45f;
                    if (item.isBlock()) {
                        com.za.zenith.world.blocks.BlockDefinition bDef = com.za.zenith.world.blocks.BlockRegistry.getBlock(item.getIdentifier());
                        if (bDef != null) {
                            // Вычисляем объем хитбокса
                            float volume = 0;
                            for (com.za.zenith.world.physics.AABB box : bDef.getShape((byte)0).getBoxes()) {
                                volume += (box.getMax().x - box.getMin().x) * 
                                          (box.getMax().y - box.getMin().y) * 
                                          (box.getMax().z - box.getMin().z);
                            }

                            if (volume > 0.5f) {
                                baseScale = 0.25f; // Полноразмерные блоки (в т.ч. листва) - маленькие
                            } else {
                                baseScale = 0.7f;  // Мелкие блоки (кувшины, свечи) - крупные
                            }
                        }
                    }
                    float scale = item.getDroppedScale() * baseScale;

                    // Items are centered at (0,0), so they sink by half. Blocks are 0..1, so they don't.
                    float yOffset = item.isBlock() ? 0.0f : scale * 0.5f;

                    Vector3f interpRot = entity.getInterpolatedRotation(alpha);

                    modelMatrix.identity()
                        .translate(interpPos.x(), interpPos.y() + bob + yOffset, interpPos.z())
                        .rotateX(interpRot.x)
                        .rotateY(interpRot.y)
                        .rotateZ(interpRot.z)
                        .scale(scale);

                    blockShader.setMatrix4f("model", modelMatrix);
                    blockShader.setInt("highlightPass", 0);
                    mesh.render();
                }
            } else if (entity instanceof com.za.zenith.entities.ResourceEntity resource) {
                com.za.zenith.world.items.Item item = resource.getStack().getItem();
                Mesh mesh = itemMeshCache.get(item);

                if (mesh == null) {
                    mesh = com.za.zenith.world.items.ItemMeshGenerator.generateItemMesh(item.getTexturePath(), atlas, item.getId());
                    if (mesh != null) itemMeshCache.put(item, mesh);
                }

                if (mesh != null) {
                    float scale = item.getDroppedScale();
                    float thicknessOffset = 0.03125f * scale;
                    
                    modelMatrix.identity()
                        .translate(interpPos.x(), interpPos.y() + thicknessOffset, interpPos.z())
                        .rotateY(resource.getRotation().y)
                        .rotateX(1.5708f) 
                        .scale(scale);

                    blockShader.setMatrix4f("model", modelMatrix);
                    blockShader.setInt("highlightPass", 0);
                    mesh.render();
                }
            } else if (entity instanceof com.za.zenith.entities.DecorationEntity decoration) {
                com.za.zenith.entities.EntityDefinition def = decoration.getDefinition();
                if (def == null) continue;

                Mesh mesh = entityDefMeshCache.get(def);
                if (mesh == null) {
                    if ("item".equals(def.modelType())) {
                        mesh = com.za.zenith.world.items.ItemMeshGenerator.generateItemMesh(def.texture(), atlas, 0);
                    } else if ("block".equals(def.modelType())) {
                        com.za.zenith.utils.Identifier blockId = com.za.zenith.utils.Identifier.of(def.texture());
                        com.za.zenith.world.blocks.BlockDefinition blockDef = com.za.zenith.world.blocks.BlockRegistry.getBlock(blockId);
                        if (blockDef != null) {
                            mesh = ChunkMeshGenerator.generateSingleBlockMesh(new Block(blockDef.getId()), atlas);
                        }
                    }
                    if (mesh != null) entityDefMeshCache.put(def, mesh);
                }

                if (mesh != null) {
                    org.joml.Vector3f scale = def.visualScale();
                    modelMatrix.identity()
                        .translate(interpPos.x(), interpPos.y(), interpPos.z())
                        .rotateY(entity.getRotation().y)
                        .scale(scale.x, scale.y, scale.z);

                    blockShader.setMatrix4f("model", modelMatrix);
                    blockShader.setInt("highlightPass", 0);
                    mesh.render();
                }
            } else {
                if (playerMesh == null) createPlayerMesh();
                modelMatrix.identity()
                    .translate(interpPos.x(), interpPos.y(), interpPos.z())
                    .rotateY(entity.getRotation().y);

                blockShader.setMatrix4f("model", modelMatrix);
                blockShader.setInt("highlightPass", 0);
                playerMesh.render();
            }
        }
        blockShader.setInt("highlightPass", 0);
    }

    private void renderBlockEntities(Camera camera, World world, float alpha) {
        if (world.getBlockEntities().isEmpty()) return;

        blockShader.use();
        for (com.za.zenith.world.blocks.entity.BlockEntity be : world.getBlockEntities().values()) {
            carvingRenderer.render(be, atlas, blockShader, modelMatrix, this, breakingPos, wobbleTimer);

            if (be instanceof com.za.zenith.world.blocks.entity.ICraftingSurface stump) {
                int totalItems = stump.getActiveSlotsCount();
                if (totalItems == 0) continue;

                for (int i = 0; i < 9; i++) {
                    com.za.zenith.world.items.ItemStack stack = stump.getStackInSlot(i);
                    if (stack == null) continue;

                    com.za.zenith.world.items.Item item = stack.getItem();
                    Mesh mesh = itemMeshCache.get(item);
                    if (mesh == null) {
                        if (item.isBlock()) {
                            mesh = ChunkMeshGenerator.generateSingleBlockMesh(new com.za.zenith.world.blocks.Block(item.getId()), atlas);
                        } else {
                            mesh = com.za.zenith.world.items.ItemMeshGenerator.generateItemMesh(item.getTexturePath(), atlas, item.getId());
                        }
                        if (mesh != null) itemMeshCache.put(item, mesh);
                    }

                    if (mesh != null) {
                        org.joml.Vector3f transform = com.za.zenith.world.blocks.CraftingLayoutEngine.getSlotTransform(i, totalItems);
                        float scale = item.isBlock() ? 0.4f : item.getDroppedScale() * 0.6f;
                        float finalScale = scale * transform.y; 

                        BlockPos pos = be.getPos();
                        modelMatrix.identity()
                            .translate(pos.x() + 0.5f + transform.x, pos.y() + 1.02f, pos.z() + 0.5f + transform.z);

                        if (item.isBlock()) {
                            modelMatrix.scale(finalScale);
                        } else {
                            modelMatrix.rotateX(1.5708f) 
                                       .scale(finalScale);
                        }

                        blockShader.setMatrix4f("model", modelMatrix);
                        blockShader.setInt("highlightPass", 0);
                        mesh.render();
                    }
                }
            }
        }
    }

    private void renderPlayers(Camera camera, com.za.zenith.network.GameClient networkClient, float alpha) {
        if (networkClient == null || !networkClient.isConnected()) return;
        if (playerMesh == null) createPlayerMesh();
        for (var p : networkClient.getRemotePlayers().values()) {
            modelMatrix.identity().translate(p.getX(), p.getY(), p.getZ()).scale(0.6f, 1.8f, 0.6f);
            blockShader.setMatrix4f("model", modelMatrix);
            blockShader.setInt("highlightPass", 1);
            blockShader.setVector3f("highlightColor", new Vector3f(0.3f, 0.6f, 1.0f));
            playerMesh.render();
        }
        blockShader.setInt("highlightPass", 0);
    }

    private void createPlayerMesh() {
        float[] p = {-0.5f,-1,0.5f, 0.5f,-1,0.5f, 0.5f,1,0.5f, -0.5f,1,0.5f, -0.5f,-1,-0.5f, -0.5f,1,-0.5f, 0.5f,1,-0.5f, 0.5f,-1,-0.5f, -0.5f,-1,-0.5f, -0.5f,-1,0.5f, -0.5f,1,0.5f, -0.5f,1,-0.5f, 0.5f,-1,0.5f, 0.5f,-1,-0.5f, 0.5f,1,-0.5f, 0.5f,1,0.5f, -0.5f,1,0.5f, 0.5f,1,0.5f, 0.5f,1,-0.5f, -0.5f,1,-0.5f, -0.5f,-1,-0.5f, 0.5f,-1,-0.5f, 0.5f,-1,0.5f, -0.5f,-1,0.5f};
        float[] n = {0,0,1, 0,0,1, 0,0,1, 0,0,1, 0,0,-1, 0,0,-1, 0,0,-1, 0,0,-1, -1,0,0, -1,0,0, -1,0,0, -1,0,0, 1,0,0, 1,0,0, 1,0,0, 1,0,0, 0,1,0, 0,1,0, 0,1,0, 0,1,0, 0,-1,0, 0,-1,0, 0,-1,0, 0,-1,0};
        int[] ind = {0,1,2, 2,3,0, 4,5,6, 6,7,4, 8,9,10, 10,11,8, 12,13,14, 14,15,12, 16,17,18, 18,19,16, 20,21,22, 22,23,20};
        playerMesh = new Mesh(p, new float[48], n, new float[24], ind);
    }

    private void updateChunkMesh(Chunk chunk, World world) {
        ChunkMeshGenerator.ChunkMeshResult old = chunkMeshes.get(chunk);
        if (old != null) { if (old.opaqueMesh != null) old.opaqueMesh.cleanup(); if (old.translucentMesh != null) old.translucentMesh.cleanup(); }
        chunkMeshes.put(chunk, ChunkMeshGenerator.generateMesh(chunk, world, atlas));
        chunk.setMeshUpdated();
    }

    public void renderDebug(float fps, int w, int h) { if (debugRenderer != null) debugRenderer.renderFPS(fps, w, h); }
    public void toggleFXAA() { fxaaEnabled = !fxaaEnabled; }
    public DynamicTextureAtlas getAtlas() { return atlas; }
    public UIRenderer getUIRenderer() { return uiRenderer; }
    public void setHotbar(com.za.zenith.engine.graphics.ui.Hotbar h) { if (uiRenderer != null) uiRenderer.setHotbar(h); }
    public void setPauseMenu(com.za.zenith.engine.graphics.ui.PauseMenu m) { if (uiRenderer != null) uiRenderer.setPauseMenu(m); }
    
    public void cleanup() {
        for (var r : chunkMeshes.values()) { if (r.opaqueMesh != null) r.opaqueMesh.cleanup(); if (r.translucentMesh != null) r.translucentMesh.cleanup(); }
        if (framebuffer != null) framebuffer.cleanup();
        if (postProcessor != null) postProcessor.cleanup();
        if (uiRenderer != null) uiRenderer.cleanup();
        if (highlightRenderer != null) highlightRenderer.cleanup();
        if (playerMesh != null) playerMesh.cleanup();
        if (previewMesh != null) previewMesh.cleanup();
        if (heldItemMesh != null) heldItemMesh.cleanup();
        for (var m : blockMeshCache.values()) m.cleanup();
        for (var m : itemMeshCache.values()) m.cleanup();
        if (carvingRenderer != null) carvingRenderer.cleanup();
        particleRenderer.cleanup();
        skyRenderer.cleanup();
        if (atlas != null) atlas.cleanup();
        if (blockShader != null) blockShader.cleanup();
    }
}


