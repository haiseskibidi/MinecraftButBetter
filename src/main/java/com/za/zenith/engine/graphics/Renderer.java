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
import com.za.zenith.utils.Logger;
import org.joml.FrustumIntersection;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.za.zenith.engine.core.GameLoop;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

import java.util.concurrent.*;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.HashSet;

public class Renderer {
    private Shader blockShader;
    private DynamicTextureAtlas atlas;
    private final Map<Chunk, ChunkMeshGenerator.ChunkMeshResult> chunkMeshes;
    private final Matrix4f modelMatrix;
    private final FrustumIntersection frustum = new FrustumIntersection();
    private final Matrix4f frustumMatrix = new Matrix4f();
    private DebugRenderer debugRenderer;
    private Framebuffer msaaFramebuffer;
    private Framebuffer resolveFramebuffer;
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
    private final Map<com.za.zenith.world.BlockPos, Mesh> persistentHoleCache = new java.util.HashMap<>();
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
    private final Map<com.za.zenith.world.BlockPos, Mesh> proxyMeshCache = new java.util.HashMap<>();
    private float breakingProgress = 0.0f;
    private float wobbleTimer = 0.0f;

    private final Map<com.za.zenith.world.items.Item, Mesh> itemMeshCache = new java.util.HashMap<>();
    private final Map<com.za.zenith.entities.EntityDefinition, Mesh> entityDefMeshCache = new java.util.HashMap<>();

    // L1 Entity Light Cache
    private Chunk lastEntityChunk;
    private com.za.zenith.world.chunks.ChunkPos lastEntityChunkPos;

    // Async Meshing
    private final com.za.zenith.utils.PriorityExecutorService meshExecutor = new com.za.zenith.utils.PriorityExecutorService(
        Math.min(2, Math.max(1, Runtime.getRuntime().availableProcessors() / 2)),
        r -> {
            Thread t = new Thread(r, "MeshGenerator");
            t.setDaemon(true);
            t.setPriority(Thread.MIN_PRIORITY);
            return t;
        }
    );
    private final Map<Chunk, Future<ChunkMeshGenerator.RawChunkMeshResult>> pendingUpdates = new ConcurrentHashMap<>();

    private final List<Chunk> visibleChunks = new java.util.ArrayList<>();
    private final Vector3f lastSortPos = new Vector3f(Float.MAX_VALUE);
    private float lastSortYaw = -1000f;
    private long frameCounter = 0;
    
    // Vector Pools to avoid "new Vector3f" in hot loops
    private final Vector3f vPool1 = new Vector3f();
    private final Vector3f vPool2 = new Vector3f();
    private final Vector3f vPool3 = new Vector3f();

    public Renderer() {
        this.chunkMeshes = new ConcurrentHashMap<>();
        this.modelMatrix = new Matrix4f();
        this.lightDirection = new Vector3f(0.2f, -1.0f, 0.2f).normalize();
    }

    public void onChunkUnload(Chunk chunk) {
        if (chunk == null) return;
        
        // 1. Cancel pending update
        Future<?> future = pendingUpdates.remove(chunk);
        if (future != null) future.cancel(true);
        
        // 2. Cleanup GPU resources
        ChunkMeshGenerator.ChunkMeshResult result = chunkMeshes.remove(chunk);
        if (result != null) {
            if (result.opaqueMesh != null) result.opaqueMesh.cleanup();
            if (result.translucentMesh != null) result.translucentMesh.cleanup();
        }
    }

    public void setBreakingBlock(com.za.zenith.world.BlockPos pos, Block block, float progress, float timer, Vector3f localHitPoint, Vector3f localWeakSpot, Vector3f color, java.util.List<Vector4f> history, World world) {
        if (block == null) {
            this.breakingPos = null;
            this.currentBreakingBlock = null;
            this.breakingProgress = 0.0f;
            this.wobbleTimer = 0.0f;
            return;
        }
        
        if (currentBreakingBlock == null || !pos.equals(this.breakingPos) || currentBreakingBlock.getType() != block.getType() || currentBreakingBlock.getMetadata() != block.getMetadata()) {
            if (breakingMesh != null) breakingMesh.cleanup();
            breakingMesh = ChunkMeshGenerator.generateSingleBlockMesh(block, atlas, world, pos);
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
            previewMesh = ChunkMeshGenerator.generateSingleBlockMesh(block, atlas, null, null);
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
        
        // Setup MSAA Pipeline
        msaaFramebuffer = new Framebuffer(windowWidth, windowHeight, 4);
        resolveFramebuffer = new Framebuffer(windowWidth, windowHeight, 1);
        
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
        
        // Resize framebuffers if window changed
        msaaFramebuffer.resize(window.getWidth(), window.getHeight());
        resolveFramebuffer.resize(window.getWidth(), window.getHeight());
        
        // 1. RENDER SCENE TO MSAA BUFFER
        msaaFramebuffer.bind();
        glEnable(GL_MULTISAMPLE);
        glEnable(GL_SAMPLE_ALPHA_TO_COVERAGE);

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

        // Update LightManager
        com.za.zenith.world.lighting.LightManager.update(world, world.getPlayer());
        
        // Add Sun as a directional light source
        com.za.zenith.world.lighting.LightData sunData = new com.za.zenith.world.lighting.LightData();
        sunData.type = com.za.zenith.world.lighting.LightData.Type.DIRECTIONAL;
        sunData.color.set(sunCol).mul(sunIntensity).add(new Vector3f(moonCol).mul(moonIntensity * 0.5f));
        sunData.intensity = 1.0f;
        com.za.zenith.world.lighting.LightSource sunSource = new com.za.zenith.world.lighting.LightSource(sunData);
        sunSource.direction.set(finalLightDir);
        com.za.zenith.world.lighting.LightManager.addDirectionalLight(sunSource);

        Vector3f currentAmbient = new Vector3f(ambCol).mul(0.2f + 0.8f * sunIntensity + 0.3f * moonIntensity);

        renderScene(camera, world, networkClient, alpha, finalLightDir, currentAmbient);

        if (highlightedBlock != null && highlightedBlock.isHit()) renderBlockHighlight(camera, world, highlightedBlock, alpha);
        if (previewPos != null && previewMesh != null) renderPreviewBlock(camera, alpha);

        renderViewModel(camera, world.getPlayer(), currentAmbient);

        // Render Particles
        particleRenderer.render(camera, com.za.zenith.world.particles.ParticleManager.getInstance().getActiveParticles(), atlas, alpha, currentAmbient);

        msaaFramebuffer.unbind();
        glDisable(GL_SAMPLE_ALPHA_TO_COVERAGE);
        glDisable(GL_MULTISAMPLE);
        
        // 2. RESOLVE MSAA TO NORMAL BUFFER
        msaaFramebuffer.resolveTo(resolveFramebuffer);
        
        // 3. APPLY POST-PROCESSING AND UI
        glViewport(0, 0, window.getWidth(), window.getHeight());
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Don't double-smooth: if we use Hardware MSAA, we don't need FXAA blur on top
        boolean finalFXAA = fxaaEnabled && msaaFramebuffer.getSamples() <= 1;
        if (finalFXAA) postProcessor.processFXAA(resolveFramebuffer.getColorTextureId(), resolveFramebuffer.getDepthTextureId(), window.getWidth(), window.getHeight());
        else postProcessor.processPassthrough(resolveFramebuffer.getColorTextureId(), resolveFramebuffer.getDepthTextureId(), window.getWidth(), window.getHeight());

        uiRenderer.renderCrosshair(window.getWidth(), window.getHeight());
        uiRenderer.renderLogo(window.getWidth(), window.getHeight());
        uiRenderer.renderHotbar(window.getWidth(), window.getHeight(), atlas);
    }

    private void renderViewModel(Camera camera, com.za.zenith.entities.Player player, Vector3f currentAmbient) {
        if (player == null) return;
        glDisable(GL_CULL_FACE);
        glDepthRange(0.0, 0.05);
        
        viewmodelShader.use();
        atlas.bind();
        Matrix4f viewModelProjection = new Matrix4f().setPerspective((float)Math.toRadians(70.0f), camera.getAspectRatio(), 0.01f, 1000.0f);
        viewmodelShader.setMatrix4f("projection", viewModelProjection);
        viewmodelShader.setMatrix4f("view", new Matrix4f().identity());
        
        Matrix4f viewMatrix = camera.getViewMatrix(1.0f); // Use current view matrix for transformation
        java.util.List<com.za.zenith.world.lighting.LightSource> worldLights = com.za.zenith.world.lighting.LightManager.getActiveLights();
        java.util.List<com.za.zenith.world.lighting.LightSource> viewLights = new java.util.ArrayList<>();
        
        for (com.za.zenith.world.lighting.LightSource ls : worldLights) {
            com.za.zenith.world.lighting.LightSource vls = new com.za.zenith.world.lighting.LightSource(ls.data);
            viewMatrix.transformPosition(ls.position, vls.position);
            viewMatrix.transformDirection(ls.direction, vls.direction);
            viewLights.add(vls);
        }

        viewmodelShader.setLights("uLights", viewLights);
        viewmodelShader.setVector3f("ambientLight", currentAmbient);
        viewmodelShader.setVector3f("uGrassColor", ColorProvider.getGrassColor());
        
        // Normalize time
        float normalizedTime = (float)(org.lwjgl.glfw.GLFW.glfwGetTime() % 3600.0);
        viewmodelShader.setFloat("uTime", normalizedTime);
        
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
    
    private void renderScene(Camera camera, World world, com.za.zenith.network.GameClient networkClient, float alpha, Vector3f lightDir, Vector3f ambient) {
        frameCounter++;
        
        // Initialize frustum for culling
        frustumMatrix.set(camera.getProjectionMatrix()).mul(camera.getViewMatrix(alpha));
        frustum.set(frustumMatrix);

        blockShader.use();
        atlas.bind();
        blockShader.setMatrix4f("projection", camera.getProjectionMatrix());
        blockShader.setMatrix4f("view", camera.getViewMatrix(alpha));
        
        // Normalize time to maintain float precision for sine/cosine animation
        float normalizedTime = (float)(org.lwjgl.glfw.GLFW.glfwGetTime() % 3600.0);
        blockShader.setFloat("uTime", normalizedTime);
        blockShader.setFloat("uSwayOverride", -1.0f);
        blockShader.setVector3f("uGrassColor", ColorProvider.getGrassColor());
        
        blockShader.setLights("uLights", com.za.zenith.world.lighting.LightManager.getActiveLights());
        blockShader.setVector3f("ambientLight", ambient);

        // Collect all positions from damage map to hide them in chunk mesh
        java.util.Set<com.za.zenith.world.BlockPos> damagedPositions = world.getBlockDamageMap().keySet();
        int hiddenCount = Math.min(damagedPositions.size(), 16);
        blockShader.setInt("uHiddenCount", hiddenCount);
        int idx = 0;
        for (com.za.zenith.world.BlockPos dpos : damagedPositions) {
            if (idx >= 16) break;
            vPool1.set(dpos.x(), dpos.y(), dpos.z());
            blockShader.setVector3f("uHiddenPositions[" + idx + "]", vPool1);
            idx++;
        }

        blockShader.setBoolean("useMask", false);
        blockShader.setBoolean("previewPass", false);
        blockShader.setBoolean("isHand", false);
        blockShader.setFloat("brightnessMultiplier", 1.0f);
        blockShader.setInt("highlightPass", 0);
        blockShader.setBoolean("uIsProxy", false);
        blockShader.setInt("uHitCount", 0);
        blockShader.setFloat("uBreakingProgress", 0.0f);
        blockShader.setInt("uBreakingPattern", 0);
        vPool1.set(0.5f);
        blockShader.setVector3f("uBreakingHitPoint", vPool1);
        blockShader.setVector3f("uWeakSpotPos", vPool1);
        vPool2.set(1.0f, 1.0f, 1.0f);
        blockShader.setVector3f("uWeakSpotColor", vPool2);
        vPool3.set(-1.0f, -1.0f, -1.0f);
        blockShader.setVector3f("uOverrideLight", vPool3);
        
        if (breakingPos != null && breakingProgress < 1.0f) { // Skip hole ONLY for instant breaks (1.0)
            if (holeMesh == null || !breakingPos.equals(holePos)) {
                if (holeMesh != null) holeMesh.cleanup();
                holeMesh = ChunkMeshGenerator.generateHoleMesh(breakingPos, world, atlas);
                holePos = breakingPos;
            }
        } else {
            if (holeMesh != null) {
                holeMesh.cleanup();
                holeMesh = null;
                holePos = null;
            }
        }

        // 1. Process pending mesh updates (Check for finished tasks)
        long uploadStart = System.nanoTime();
        java.util.Iterator<Map.Entry<Chunk, Future<ChunkMeshGenerator.RawChunkMeshResult>>> it = pendingUpdates.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Chunk, Future<ChunkMeshGenerator.RawChunkMeshResult>> entry = it.next();
            if (entry.getValue().isDone()) {
                try {
                    ChunkMeshGenerator.RawChunkMeshResult rawResult = entry.getValue().get();
                    ChunkMeshGenerator.ChunkMeshResult result = rawResult.upload();
                    Chunk chunk = entry.getKey();

                    // Cleanup old mesh in MAIN thread
                    ChunkMeshGenerator.ChunkMeshResult old = chunkMeshes.get(chunk);
                    if (old != null) {
                        if (old.opaqueMesh != null) old.opaqueMesh.cleanup();
                        if (old.translucentMesh != null) old.translucentMesh.cleanup();
                    }

                    chunkMeshes.put(chunk, result);
                    chunk.setMeshUpdated(result.version);
                    it.remove();
                    
                    // Limit time spent uploading to ~2ms to keep frame rate stable
                    if (System.nanoTime() - uploadStart > 2_000_000) break;
                } catch (Exception e) {
                    if (!(e instanceof java.util.concurrent.CancellationException)) {
                        e.printStackTrace();
                    }
                    it.remove();
                }
            }
        }

        // 1.5 TASK CANCELLATION
        // Every 60 frames, clean up tasks for chunks that are too far away
        if (frameCounter % 60 == 0) {
            float dist = (world.getRenderDistance() + 4) * Chunk.CHUNK_SIZE;
            final float cancelRangeSq = dist * dist;
            Vector3f camPos = camera.getPosition();
            
            pendingUpdates.entrySet().removeIf(entry -> {
                Chunk c = entry.getKey();
                float cx = c.getPosition().x() * Chunk.CHUNK_SIZE + 8;
                float cz = c.getPosition().z() * Chunk.CHUNK_SIZE + 8;
                float distSq = camPos.distanceSquared(cx, camPos.y, cz);
                
                if (distSq > cancelRangeSq) {
                    entry.getValue().cancel(true);
                    return true;
                }
                return false;
            });
        }

        // 2. Schedule new mesh updates (with budget)
        int scheduledThisFrame = 0;
        int maxScheduledPerFrame = (breakingPos != null) ? 4 : 2; // Allow faster updates when mining
        
        visibleChunks.clear();
        Vector3f camPos = camera.getPosition();

        for (Chunk chunk : world.getLoadedChunks()) {
            // Frustum Culling
            float cx = chunk.getPosition().x() * Chunk.CHUNK_SIZE;
            float cz = chunk.getPosition().z() * Chunk.CHUNK_SIZE;
            
            boolean inFrustum = frustum.testAab(cx, 0, cz, cx + Chunk.CHUNK_SIZE, Chunk.CHUNK_HEIGHT, cz + Chunk.CHUNK_SIZE);
            if (!inFrustum) {
                continue;
            }

            visibleChunks.add(chunk);
            chunk.setLastSeenFrame(frameCounter);

            boolean needsMesh = chunk.needsMeshUpdate() || !chunkMeshes.containsKey(chunk);

            if (chunk.isReady() && needsMesh && !pendingUpdates.containsKey(chunk)) {
                if (scheduledThisFrame < maxScheduledPerFrame) {
                    int[] blockDataArr = com.za.zenith.utils.ArrayPool.rentBlockDataArray();
                    byte[] lightDataArr = com.za.zenith.utils.ArrayPool.rentLightDataArray();
                    Chunk.DataSnapshot snapshot = chunk.getSnapshot(blockDataArr, lightDataArr);
                    long version = chunk.getDirtyCounter();
                    
                    // AAA Priority: closer chunks + chunks in view get much higher priority
                    float distSq = camPos.distanceSquared(cx + 8, camPos.y, cz + 8);
                    int priority = (int)distSq;
                    
                    pendingUpdates.put(chunk, meshExecutor.submit(new com.za.zenith.utils.PriorityExecutorService.PrioritizedCallable<ChunkMeshGenerator.RawChunkMeshResult>() {
                        @Override
                        public int getPriority() { return priority; }
                        
                        @Override
                        public ChunkMeshGenerator.RawChunkMeshResult call() throws Exception {
                            try {
                                Chunk tempChunk = new Chunk(snapshot.position(), snapshot.blockData(), snapshot.lightData());
                                tempChunk.setDirtyCounter(version);
                                return ChunkMeshGenerator.generateRawMesh(tempChunk, world, atlas);
                            } finally {
                                com.za.zenith.utils.ArrayPool.returnBlockDataArray(snapshot.blockData());
                                com.za.zenith.utils.ArrayPool.returnLightDataArray(snapshot.lightData());
                            }
                        }
                    }));
                    scheduledThisFrame++;
                }
            }
        }
        
        // Smart re-sort
        float yaw = camera.getRotation().y;
        if (camPos.distanceSquared(lastSortPos) > 1.0f || Math.abs(yaw - lastSortYaw) > 10.0f) {
            visibleChunks.sort((c1, p2) -> {
                float d1 = camPos.distanceSquared(c1.getPosition().x() * Chunk.CHUNK_SIZE + 8, camPos.y, c1.getPosition().z() * Chunk.CHUNK_SIZE + 8);
                float d2 = camPos.distanceSquared(p2.getPosition().x() * Chunk.CHUNK_SIZE + 8, camPos.y, p2.getPosition().z() * Chunk.CHUNK_SIZE + 8);
                return Float.compare(d1, d2);
            });
            lastSortPos.set(camPos);
            lastSortYaw = yaw;
        }

        // 3. Render loaded meshes
        // Opaque: Front-to-back (already sorted)
        for (Chunk chunk : visibleChunks) {
            ChunkMeshGenerator.ChunkMeshResult result = chunkMeshes.get(chunk);
            if (result != null && result.opaqueMesh != null) {
                modelMatrix.identity().translate(chunk.getPosition().x() * Chunk.CHUNK_SIZE, 0, chunk.getPosition().z() * Chunk.CHUNK_SIZE);
                blockShader.setMatrix4f("model", modelMatrix);
                blockShader.setFloat("uChunkSpawnTime", chunk.getFirstSpawnTime());
                result.opaqueMesh.render();
            }
        }
        
        // Translucent: Back-to-front
        glDepthMask(false);
        for (int i = visibleChunks.size() - 1; i >= 0; i--) {
            Chunk chunk = visibleChunks.get(i);
            ChunkMeshGenerator.ChunkMeshResult result = chunkMeshes.get(chunk);
            if (result != null && result.translucentMesh != null) {
                modelMatrix.identity().translate(chunk.getPosition().x() * Chunk.CHUNK_SIZE, 0, chunk.getPosition().z() * Chunk.CHUNK_SIZE);
                blockShader.setMatrix4f("model", modelMatrix);
                blockShader.setFloat("uChunkSpawnTime", chunk.getFirstSpawnTime());
                result.translucentMesh.render();
            }
        }
        glDepthMask(true);
        
        if (holeMesh != null) {
            blockShader.setInt("uHiddenCount", 0); // Disable discard for hole mesh
            modelMatrix.identity().translate(breakingPos.x(), breakingPos.y(), breakingPos.z());
            blockShader.setMatrix4f("model", modelMatrix);
            holeMesh.render();
        }
        
        if (breakingPos != null && breakingMesh != null && currentBreakingBlock != null) {
            blockShader.setInt("uHiddenCount", 0); // Disable discard for proxy
            renderBreakingProxyBlock(camera, world, alpha);
        }

        // Restore hidden count for persistent scars rendering
        blockShader.setInt("uHiddenCount", hiddenCount);
        renderPersistentScars(camera, world, alpha);
        
        // Reset count before rendering entities to avoid accidental discards
        blockShader.setInt("uHiddenCount", 0);
        
        renderEntities(camera, world, alpha);
        renderBlockEntities(camera, world, alpha);
        renderPlayers(camera, world, networkClient, alpha);
    }

    private void renderPersistentScars(Camera camera, World world, float alpha) {
        if (world.getBlockDamageMap().isEmpty()) {
            if (!persistentHoleCache.isEmpty()) {
                persistentHoleCache.values().forEach(Mesh::cleanup);
                persistentHoleCache.clear();
            }
            if (!proxyMeshCache.isEmpty()) {
                proxyMeshCache.values().forEach(Mesh::cleanup);
                proxyMeshCache.clear();
            }
            return;
        }
        
        blockShader.use();
        
        // 1. Cleanup stale meshes
        persistentHoleCache.keySet().removeIf(pos -> {
            if (!world.getBlockDamageMap().containsKey(pos)) {
                Mesh m = persistentHoleCache.get(pos);
                if (m != null) m.cleanup();
                return true;
            }
            return false;
        });

        proxyMeshCache.keySet().removeIf(pos -> {
            if (!world.getBlockDamageMap().containsKey(pos)) {
                Mesh m = proxyMeshCache.get(pos);
                if (m != null) m.cleanup();
                return true;
            }
            return false;
        });

        // 2. Render all damaged blocks and their holes
        for (Map.Entry<com.za.zenith.world.BlockPos, World.BlockDamageInstance> entry : world.getBlockDamageMap().entrySet()) {
            com.za.zenith.world.BlockPos pos = entry.getKey();
            // Skip current breaking block as it has its own special rendering
            if (breakingPos != null && pos.equals(breakingPos)) continue;

            World.BlockDamageInstance info = entry.getValue();
            com.za.zenith.world.blocks.Block block = info.getBlock();
            if (block == null || block.isAir()) continue;

            // 2a. Render Hole Mesh (Neighbor faces)
            Mesh hole = persistentHoleCache.get(pos);
            if (hole == null) {
                hole = ChunkMeshGenerator.generateHoleMesh(pos, world, atlas);
                if (hole != null) persistentHoleCache.put(pos, hole);
            }
            
            if (hole != null) {
                blockShader.setBoolean("uIsProxy", false);
                blockShader.setInt("uHiddenCount", 0); // Temporary disable discard to show hole
                modelMatrix.identity().translate(pos.x(), pos.y(), pos.z());
                blockShader.setMatrix4f("model", modelMatrix);
                hole.render();
                // Restore hidden count for next operations
                java.util.Set<com.za.zenith.world.BlockPos> damagedPositions = world.getBlockDamageMap().keySet();
                blockShader.setInt("uHiddenCount", Math.min(damagedPositions.size(), 16));
            }

            // 2b. Render Proxy Mesh (The block itself with cracks)
            Mesh mesh = proxyMeshCache.get(pos);
            if (mesh == null) {
                mesh = ChunkMeshGenerator.generateSingleBlockMesh(block, atlas, world, pos);
                if (mesh != null) proxyMeshCache.put(pos, mesh);
            }

            if (mesh != null) {
                com.za.zenith.world.blocks.BlockDefinition def = com.za.zenith.world.blocks.BlockRegistry.getBlock(block.getType());
                blockShader.setBoolean("uIsProxy", true);
                blockShader.setVector3f("uWobbleScale", new Vector3f(1.0f));
                blockShader.setVector3f("uWobbleOffset", new Vector3f(0.0f));
                blockShader.setFloat("uWobbleShake", 0.0f);
                
                float maxHealth = def.getHardness() * 10.0f;
                blockShader.setFloat("uBreakingProgress", info.getDamage() / maxHealth);
                blockShader.setInt("uBreakingPattern", def.getBreakingPattern());
                blockShader.setVector3f("uWeakSpotPos", new Vector3f(0, -100, 0)); 
                
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
    }

    private void renderBreakingProxyBlock(Camera camera, World world, float alpha) {
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
        
        vPool1.set(scaleX, scaleY, scaleZ);
        blockShader.setVector3f("uWobbleScale", vPool1);
        vPool2.set(offsetX, offsetY, offsetZ);
        blockShader.setVector3f("uWobbleOffset", vPool2);
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

    private void setEntityLight(World world, Vector3f pos) {
        int x = (int) Math.floor(pos.x());
        int y = (int) Math.floor(pos.y());
        int z = (int) Math.floor(pos.z());
        
        com.za.zenith.world.chunks.ChunkPos cp = com.za.zenith.world.chunks.ChunkPos.fromBlockPos(x, z);
        if (lastEntityChunk == null || !cp.equals(lastEntityChunkPos)) {
            lastEntityChunk = world.getChunk(cp);
            lastEntityChunkPos = cp;
        }

        if (lastEntityChunk != null) {
            float sun = lastEntityChunk.getSunlight(x & 15, y, z & 15);
            float block = lastEntityChunk.getBlockLight(x & 15, y, z & 15);
            vPool1.set(sun, block, 1.0f);
            blockShader.setVector3f("uOverrideLight", vPool1);
        } else {
            vPool1.set(15.0f, 0.0f, 1.0f);
            blockShader.setVector3f("uOverrideLight", vPool1);
        }
    }

    private void renderEntities(Camera camera, World world, float alpha) {
        if (world.getEntities().isEmpty()) return;

        blockShader.use();
        for (com.za.zenith.entities.Entity entity : world.getEntities()) {
            Vector3f interpPos = entity.getInterpolatedPosition(alpha);
            
            // Optimization: Skip entities in non-loaded or non-ready chunks
            int cx = (int) Math.floor(interpPos.x) >> 4;
            int cz = (int) Math.floor(interpPos.z) >> 4;
            Chunk chunk = world.getChunk(new com.za.zenith.world.chunks.ChunkPos(cx, cz));
            if (chunk == null || !chunk.isReady()) continue;
            
            setEntityLight(world, interpPos);
            
            if (entity instanceof com.za.zenith.entities.ScoutEntity scout) {
                if (playerMesh == null) createPlayerMesh();
                modelMatrix.identity()
                    .translate(interpPos.x(), interpPos.y(), interpPos.z())
                    .rotateY(entity.getRotation().y);

                blockShader.setMatrix4f("model", modelMatrix);
                blockShader.setInt("highlightPass", 1);
                switch (scout.getCurrentState()) {
                    case CHASE: vPool1.set(1.0f, 0.0f, 0.0f); break;
                    case SEARCH: vPool1.set(1.0f, 0.5f, 0.0f); break;
                    default: vPool1.set(0.5f, 0.5f, 0.5f); break;
                }
                blockShader.setVector3f("highlightColor", vPool1);
                playerMesh.render();
            } else if (entity instanceof com.za.zenith.entities.ItemEntity itemEntity) {
                // Optimization: Cull distant items from rendering
                com.za.zenith.entities.Player player = world.getPlayer();
                if (player != null && interpPos.distanceSquared(player.getPosition()) > 24 * 24) continue;
                
                com.za.zenith.world.items.Item item = itemEntity.getStack().getItem();
                Mesh mesh = itemMeshCache.get(item);

                if (mesh == null) {
                    if (item.isBlock()) {
                        mesh = ChunkMeshGenerator.generateSingleBlockMesh(new Block(item.getId()), atlas, null, null);
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
                            mesh = ChunkMeshGenerator.generateSingleBlockMesh(new Block(blockDef.getId()), atlas, null, null);
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

                BlockPos pos = be.getPos();
                setEntityLight(world, vPool1.set(pos.x(), pos.y() + 1, pos.z()));

                for (int i = 0; i < 9; i++) {
                    com.za.zenith.world.items.ItemStack stack = stump.getStackInSlot(i);
                    if (stack == null) continue;

                    com.za.zenith.world.items.Item item = stack.getItem();
                    Mesh mesh = itemMeshCache.get(item);
                    if (mesh == null) {
                        if (item.isBlock()) {
                            mesh = ChunkMeshGenerator.generateSingleBlockMesh(new com.za.zenith.world.blocks.Block(item.getId()), atlas, null, null);
                        } else {
                            mesh = com.za.zenith.world.items.ItemMeshGenerator.generateItemMesh(item.getTexturePath(), atlas, item.getId());
                        }
                        if (mesh != null) itemMeshCache.put(item, mesh);
                    }

                    if (mesh != null) {
                        org.joml.Vector3f transform = com.za.zenith.world.blocks.CraftingLayoutEngine.getSlotTransform(i, totalItems);
                        float scale = item.isBlock() ? 0.4f : item.getDroppedScale() * 0.6f;
                        float finalScale = scale * transform.y; 

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

    private void renderPlayers(Camera camera, World world, com.za.zenith.network.GameClient networkClient, float alpha) {
        if (networkClient == null || !networkClient.isConnected()) return;
        if (playerMesh == null) createPlayerMesh();
        for (var p : networkClient.getRemotePlayers().values()) {
            setEntityLight(world, vPool1.set(p.getX(), p.getY(), p.getZ()));
            modelMatrix.identity().translate(p.getX(), p.getY(), p.getZ()).scale(0.6f, 1.8f, 0.6f);
            blockShader.setMatrix4f("model", modelMatrix);
            blockShader.setInt("highlightPass", 1);
            vPool1.set(0.3f, 0.6f, 1.0f);
            blockShader.setVector3f("highlightColor", vPool1);
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
        ChunkMeshGenerator.ChunkMeshResult res = ChunkMeshGenerator.generateMesh(chunk, world, atlas);
        chunkMeshes.put(chunk, res);
        chunk.setMeshUpdated(res.version);
    }

    public void rebuildAllChunks() {
        for (var r : chunkMeshes.values()) {
            if (r.opaqueMesh != null) r.opaqueMesh.cleanup();
            if (r.translucentMesh != null) r.translucentMesh.cleanup();
        }
        chunkMeshes.clear();
        blockMeshCache.values().forEach(Mesh::cleanup);
        blockMeshCache.clear();
        itemMeshCache.values().forEach(Mesh::cleanup);
        itemMeshCache.clear();
        Logger.info("Renderer: All meshes cleared for rebuild");
    }

    public void renderDebug(float fps, int w, int h) { if (debugRenderer != null) debugRenderer.renderFPS(fps, w, h); }
    public void toggleFXAA() { fxaaEnabled = !fxaaEnabled; }
    public DynamicTextureAtlas getAtlas() { return atlas; }
    public UIRenderer getUIRenderer() { return uiRenderer; }
    public void setHotbar(com.za.zenith.engine.graphics.ui.Hotbar h) { if (uiRenderer != null) uiRenderer.setHotbar(h); }
    
    public void cleanup() {
        meshExecutor.shutdown();
        try {
            if (!meshExecutor.awaitTermination(1, TimeUnit.SECONDS)) {
                meshExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            meshExecutor.shutdownNow();
        }

        persistentHoleCache.values().forEach(Mesh::cleanup);
        persistentHoleCache.clear();
        for (var r : chunkMeshes.values()) { if (r.opaqueMesh != null) r.opaqueMesh.cleanup(); if (r.translucentMesh != null) r.translucentMesh.cleanup(); }
        if (msaaFramebuffer != null) msaaFramebuffer.cleanup();
        if (resolveFramebuffer != null) resolveFramebuffer.cleanup();
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
