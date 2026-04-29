package com.za.zenith.engine.graphics;

import com.za.zenith.engine.core.Window;
import com.za.zenith.engine.graphics.ui.UIRenderer;
import com.za.zenith.network.GameClient;
import com.za.zenith.world.BlockPos;
import com.za.zenith.world.World;
import com.za.zenith.world.blocks.Block;
import com.za.zenith.world.chunks.Chunk;
import com.za.zenith.world.chunks.ChunkMeshGenerator;
import com.za.zenith.world.chunks.ChunkSection;
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
import java.util.concurrent.*;
import java.util.*;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

public class Renderer {
    private Shader blockShader;
    private DynamicTextureAtlas atlas;
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

    // MultiDraw & Pooling
    private MeshPool meshPool;
    private MultiDrawBatch opaqueBatch;
    private MultiDrawBatch translucentBatch;
    private int drawCallCount = 0;
    private int lastPoolVersion = 0;

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

    // Visibility & BFS
    private record SectionRenderNode(Chunk chunk, int sectionIdx) {}
    private record BFSNode(int cx, int cz, int secIdx, com.za.zenith.utils.Direction entryFace) {}
    private final List<SectionRenderNode> visibleSections = new ArrayList<>();
    private final Deque<BFSNode> bfsQueue = new ArrayDeque<>();
    private final Set<Long> visitedSections = new HashSet<>();
    private int lastCamSecX = Integer.MAX_VALUE;
    private int lastCamSecY = Integer.MAX_VALUE;
    private int lastCamSecZ = Integer.MAX_VALUE;
    private final Vector3f lastSortPos = new Vector3f(Float.MAX_VALUE);
    private long frameCounter = 0;
    
    // Vector Pools
    private final Vector3f vPool1 = new Vector3f();
    private final Vector3f vPool2 = new Vector3f();
    private final Vector3f vPool3 = new Vector3f();

    public Renderer() {
        this.modelMatrix = new Matrix4f();
        this.lightDirection = new Vector3f(0.2f, -1.0f, 0.2f).normalize();
    }

    public void onChunkUnload(Chunk chunk) {
        if (chunk == null) return;
        Future<?> future = pendingUpdates.remove(chunk);
        if (future != null) future.cancel(true);
        ChunkMeshGenerator.ChunkMeshResult result = chunk.getCurrentMeshResult();
        if (result != null) {
            result.cleanup();
            chunk.setCurrentMeshResult(null);
        }
    }

    public void setBreakingBlock(com.za.zenith.world.BlockPos pos, Block block, float progress, float timer, Vector3f localHitPoint, Vector3f localWeakSpot, Vector3f color, List<Vector4f> history, World world) {
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
        
        RenderContext.init();
        blockShader = new Shader("src/main/resources/shaders/vertex.glsl", "src/main/resources/shaders/fragment.glsl");
        atlas = new DynamicTextureAtlas(16);

        // Initialize Batching
        meshPool = new MeshPool();
        opaqueBatch = new MultiDrawBatch(meshPool);
        translucentBatch = new MultiDrawBatch(meshPool);

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
        if (glassUV != null) blockShader.setFloat("glassLayer", glassUV[2]);
        
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
    
    public void render(Window window, Camera camera, World world, RaycastResult highlightedBlock, GameClient networkClient, float alpha, float deltaTime) {
        vfxManager.update(deltaTime, world.getPlayer(), GameLoop.getInstance().getInputManager().getMiningController());
        
        com.za.zenith.world.WorldSettings worldSettings = com.za.zenith.world.WorldSettings.getInstance();
        float timeRatio = world.getWorldTime() / worldSettings.dayLength;
        float angle = (timeRatio - 0.25f) * (float)Math.PI * 2.0f;
        lightDirection.set(0.2f, -(float)Math.cos(angle), (float)Math.sin(angle)).normalize();
        float sunIntensity = Math.max(0.0f, (float)Math.cos(angle));
        float moonIntensity = Math.max(0.0f, -(float)Math.cos(angle));
        Vector3f finalLightDir = new Vector3f(lightDirection);
        if (moonIntensity > sunIntensity) finalLightDir.negate();
        Vector3f ambCol = new Vector3f(worldSettings.ambientColor[0], worldSettings.ambientColor[1], worldSettings.ambientColor[2]);
        Vector3f currentAmbient = new Vector3f(ambCol).mul(0.2f + 0.8f * sunIntensity + 0.3f * moonIntensity);

        RenderContext.update(world, camera, alpha, finalLightDir, currentAmbient);

        msaaFramebuffer.resize(window.getWidth(), window.getHeight());
        resolveFramebuffer.resize(window.getWidth(), window.getHeight());
        
        msaaFramebuffer.bind();
        glEnable(GL_MULTISAMPLE);
        glEnable(GL_SAMPLE_ALPHA_TO_COVERAGE);

        Vector3f daySky = new Vector3f(0.6f, 0.8f, 1.0f);
        Vector3f nightSky = new Vector3f(0.02f, 0.02f, 0.05f);
        Vector3f currentSky = new Vector3f(nightSky).lerp(daySky, sunIntensity);
        glClearColor(currentSky.x, currentSky.y, currentSky.z, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        skyRenderer.render(camera, lightDirection);
        com.za.zenith.world.lighting.LightManager.update(world, world.getPlayer());
        
        com.za.zenith.world.lighting.LightData sunData = new com.za.zenith.world.lighting.LightData();
        sunData.type = com.za.zenith.world.lighting.LightData.Type.DIRECTIONAL;
        sunData.color.set(new Vector3f(worldSettings.sunLightColor[0], worldSettings.sunLightColor[1], worldSettings.sunLightColor[2])).mul(sunIntensity).add(new Vector3f(worldSettings.moonLightColor[0], worldSettings.moonLightColor[1], worldSettings.moonLightColor[2]).mul(moonIntensity * 0.5f));
        sunData.intensity = 1.0f;
        com.za.zenith.world.lighting.LightSource sunSource = new com.za.zenith.world.lighting.LightSource(sunData);
        sunSource.direction.set(finalLightDir);
        com.za.zenith.world.lighting.LightManager.addDirectionalLight(sunSource);

        renderScene(camera, world, networkClient, alpha, finalLightDir, currentAmbient);

        if (highlightedBlock != null && highlightedBlock.isHit()) renderBlockHighlight(camera, world, highlightedBlock, alpha);
        if (previewPos != null && previewMesh != null) renderPreviewBlock(camera, alpha);

        renderViewModel(camera, world.getPlayer(), currentAmbient);
        particleRenderer.render(camera, com.za.zenith.world.particles.ParticleManager.getInstance().getActiveParticles(), atlas, alpha, currentAmbient);

        msaaFramebuffer.unbind();
        glDisable(GL_SAMPLE_ALPHA_TO_COVERAGE);
        glDisable(GL_MULTISAMPLE);
        
        msaaFramebuffer.resolveTo(resolveFramebuffer);
        glViewport(0, 0, window.getWidth(), window.getHeight());
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        if (fxaaEnabled && msaaFramebuffer.getSamples() <= 1) postProcessor.processFXAA(resolveFramebuffer.getColorTextureId(), resolveFramebuffer.getDepthTextureId(), window.getWidth(), window.getHeight());
        else postProcessor.processPassthrough(resolveFramebuffer.getColorTextureId(), resolveFramebuffer.getDepthTextureId(), window.getWidth(), window.getHeight());

        uiRenderer.renderCrosshair(window.getWidth(), window.getHeight());
        uiRenderer.renderLogo(window.getWidth(), window.getHeight());
        uiRenderer.renderHotbar(window.getWidth(), window.getHeight(), atlas);
    }

    private void renderScene(Camera camera, World world, GameClient networkClient, float alpha, Vector3f lightDir, Vector3f ambient) {
        frameCounter++;
        
        // --- MeshPool Wrap-around Detection ---
        if (meshPool.getVersion() != lastPoolVersion) {
            lastPoolVersion = meshPool.getVersion();
            Logger.warn("Renderer: MeshPool wrapped! Purging all chunk meshes to prevent corruption.");
            for (Chunk c : world.getLoadedChunks()) {
                ChunkMeshGenerator.ChunkMeshResult result = c.getCurrentMeshResult();
                if (result != null) { result.cleanup(); c.setCurrentMeshResult(null); }
                c.setMeshUpdated(-1); // Force rebuild in new pool generation
            }
            lastCamSecX = Integer.MAX_VALUE; // Force BFS recalculation
        }

        frustumMatrix.set(camera.getProjectionMatrix()).mul(camera.getViewMatrix(alpha));
        frustum.set(frustumMatrix);

        blockShader.use();
        atlas.bind();
        blockShader.setFloat("uSwayOverride", -1.0f);
        blockShader.setLights("uLights", com.za.zenith.world.lighting.LightManager.getActiveLights());

        Set<Long> damagedPositions = world.getBlockDamageMap().keySet();
        int idx = 0;
        if (breakingPos != null) {
            blockShader.setVector3f("uHiddenPositions[0]", (float)breakingPos.x(), (float)breakingPos.y(), (float)breakingPos.z());
            idx = 1;
        }
        for (long packedPos : damagedPositions) {
            if (idx >= 16) break;
            int bx = World.unpackBlockX(packedPos), by = World.unpackBlockY(packedPos), bz = World.unpackBlockZ(packedPos);
            if (breakingPos != null && bx == breakingPos.x() && by == breakingPos.y() && bz == breakingPos.z()) continue; 
            blockShader.setVector3f("uHiddenPositions[" + idx + "]", (float)bx, (float)by, (float)bz);
            idx++;
        }
        int hiddenCount = idx;
        blockShader.setInt("uHiddenCount", hiddenCount);

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
        
        if (breakingPos != null && breakingProgress < 1.0f) {
            if (holeMesh == null || !breakingPos.equals(holePos)) {
                if (holeMesh != null) holeMesh.cleanup();
                holeMesh = ChunkMeshGenerator.generateHoleMesh(breakingPos, world, atlas);
                holePos = breakingPos;
            }
        } else if (holeMesh != null) {
            holeMesh.cleanup(); holeMesh = null; holePos = null;
        }

        Vector3f camPos = camera.getPosition();
        int camChunkX = (int) Math.floor(camPos.x / Chunk.CHUNK_SIZE);
        int camChunkZ = (int) Math.floor(camPos.z / Chunk.CHUNK_SIZE);
        int camSecY = (int) Math.floor(camPos.y / ChunkSection.SECTION_SIZE);
        int renderDist = world.getRenderDistance();

        boolean movedSection = camChunkX != lastCamSecX || camSecY != lastCamSecY || camChunkZ != lastCamSecZ;
        if (movedSection || frameCounter % 5 == 0) {
            visibleSections.clear(); visitedSections.clear(); bfsQueue.clear();
            
            // CRITICAL FIX: Always seed BFS from camera position, even if chunk is null
            int startSecIdx = Math.min(Math.max(camSecY, 0), Chunk.NUM_SECTIONS - 1);
            bfsQueue.add(new BFSNode(camChunkX, camChunkZ, startSecIdx, null));
            visitedSections.add(packSectionPos(camChunkX, camChunkZ, startSecIdx));

            int poolVer = meshPool.getVersion();
            while (!bfsQueue.isEmpty()) {
                BFSNode node = bfsQueue.poll();
                
                float sx = node.cx * 16, sy = node.secIdx * 16, sz = node.cz * 16;
                if (!frustum.testAab(sx, sy, sz, sx + 16, sy + 16, sz + 16)) continue;

                Chunk chunk = world.getChunk(node.cx, node.cz);
                if (chunk == null) {
                    for (com.za.zenith.utils.Direction dir : com.za.zenith.utils.Direction.values()) {
                        int ncx = node.cx+dir.getDx(), ncz = node.cz+dir.getDz(), nsec = node.secIdx+dir.getDy();
                        if (nsec < 0 || nsec >= Chunk.NUM_SECTIONS) continue;
                        if (Math.abs(ncx-camChunkX) > renderDist || Math.abs(ncz-camChunkZ) > renderDist) continue;
                        if (visitedSections.add(packSectionPos(ncx, ncz, nsec))) bfsQueue.add(new BFSNode(ncx, ncz, nsec, dir.getOpposite()));
                    }
                    continue;
                }

                ChunkSection section = chunk.getSections()[node.secIdx];
                if (section == null || section.isEmpty()) {
                    for (com.za.zenith.utils.Direction dir : com.za.zenith.utils.Direction.values()) {
                        int ncx = node.cx+dir.getDx(), ncz = node.cz+dir.getDz(), nsec = node.secIdx+dir.getDy();
                        if (nsec < 0 || nsec >= Chunk.NUM_SECTIONS) continue;
                        if (Math.abs(ncx-camChunkX) > renderDist || Math.abs(ncz-camChunkZ) > renderDist) continue;
                        if (visitedSections.add(packSectionPos(ncx, ncz, nsec))) bfsQueue.add(new BFSNode(ncx, ncz, nsec, dir.getOpposite()));
                    }
                    continue;
                }

                ChunkMeshGenerator.ChunkMeshResult result = chunk.getCurrentMeshResult();
                if (result != null) {
                    Mesh mO = result.opaqueSections()[node.secIdx];
                    Mesh mT = result.translucentSections()[node.secIdx];
                    if (mO != null || mT != null) {
                        Mesh valid = (mO != null) ? mO : mT;
                        if (valid.getPool() == null || valid.getPoolVersion() == poolVer) {
                            visibleSections.add(new SectionRenderNode(chunk, node.secIdx));
                        }
                    }
                }
                
                for (com.za.zenith.utils.Direction dir : com.za.zenith.utils.Direction.values()) {
                    if (node.entryFace != null && !section.canSeeThrough(node.entryFace, dir)) continue;
                    int ncx = node.cx+dir.getDx(), ncz = node.cz+dir.getDz(), nsec = node.secIdx+dir.getDy();
                    if (nsec < 0 || nsec >= Chunk.NUM_SECTIONS) continue;
                    if (Math.abs(ncx-camChunkX) > renderDist || Math.abs(ncz-camChunkZ) > renderDist) continue;
                    if (visitedSections.add(packSectionPos(ncx, ncz, nsec))) bfsQueue.add(new BFSNode(ncx, ncz, nsec, dir.getOpposite()));
                }
            }
            visibleSections.sort((s1, s2) -> {
                float d1 = camPos.distanceSquared(s1.chunk.getPosition().x()*16+8, s1.sectionIdx*16+8, s1.chunk.getPosition().z()*16+8);
                float d2 = camPos.distanceSquared(s2.chunk.getPosition().x()*16+8, s2.sectionIdx*16+8, s2.chunk.getPosition().z()*16+8);
                return Float.compare(d1, d2);
            });
            lastCamSecX = camChunkX; lastCamSecY = camSecY; lastCamSecZ = camChunkZ;
        }

        long uploadStart = System.nanoTime();
        List<Map.Entry<Chunk, Future<ChunkMeshGenerator.RawChunkMeshResult>>> finishedTasks = new ArrayList<>();
        for (var entry : pendingUpdates.entrySet()) if (entry.getValue().isDone()) finishedTasks.add(entry);
        
        finishedTasks.sort((e1, e2) -> {
            float d1 = camPos.distanceSquared(e1.getKey().getPosition().x()*16+8, camPos.y, e1.getKey().getPosition().z()*16+8);
            float d2 = camPos.distanceSquared(e2.getKey().getPosition().x()*16+8, camPos.y, e2.getKey().getPosition().z()*16+8);
            return Float.compare(d1, d2);
        });

        for (var entry : finishedTasks) {
            try {
                ChunkMeshGenerator.RawChunkMeshResult raw = entry.getValue().get();
                Chunk chunk = entry.getKey();
                if (world.getChunk(chunk.getPosition()) != chunk) { raw.cleanup(); pendingUpdates.remove(chunk); continue; }
                ChunkMeshGenerator.ChunkMeshResult res = raw.upload(meshPool); raw.cleanup();
                ChunkMeshGenerator.ChunkMeshResult old = chunk.getCurrentMeshResult();
                if (old != null) old.cleanup();
                chunk.setCurrentMeshResult(res); chunk.setMeshUpdated(res.version());
                pendingUpdates.remove(chunk);
                if (System.nanoTime() - uploadStart > 2_000_000) break; 
            } catch (Exception e) { pendingUpdates.remove(entry.getKey()); }
        }

        int scheduled = 0, maxSchedule = (breakingPos != null) ? 4 : 2;
        int x = 0, z = 0, dx = 0, dz = -1, checkRadius = renderDist + 1;
        for (int i = 0; i < (checkRadius * 2 + 1) * (checkRadius * 2 + 1); i++) {
            if (scheduled >= maxSchedule) break;
            if (Math.abs(x) <= checkRadius && Math.abs(z) <= checkRadius) {
                Chunk chunk = world.getChunk(camChunkX + x, camChunkZ + z);
                if (chunk != null && chunk.isReady() && !pendingUpdates.containsKey(chunk)) {
                    if (chunk.needsMeshUpdate() || chunk.getCurrentMeshResult() == null) {
                        int[] bd = com.za.zenith.utils.ArrayPool.rentBlockDataArray();
                        byte[] ld = com.za.zenith.utils.ArrayPool.rentLightDataArray();
                        Chunk.DataSnapshot snapshot = chunk.getSnapshot(bd, ld);
                        long version = chunk.getDirtyCounter();
                        float distSq = camPos.distanceSquared((camChunkX + x) * 16 + 8, camPos.y, (camChunkZ + z) * 16 + 8);
                        float chunkSpawnTime = chunk.getFirstSpawnTime();
                        pendingUpdates.put(chunk, meshExecutor.submit(new com.za.zenith.utils.PriorityExecutorService.PrioritizedCallable<>() {
                            @Override public int getPriority() { return (int)distSq; }
                            @Override public ChunkMeshGenerator.RawChunkMeshResult call() throws Exception {
                                try {
                                    Chunk temp = new Chunk(snapshot.position(), snapshot.blockData(), snapshot.lightData());
                                    temp.setDirtyCounter(version);
                                    temp.setFirstSpawnTime(chunkSpawnTime);
                                    return ChunkMeshGenerator.generateRawMesh(temp, world, atlas);
                                } finally { com.za.zenith.utils.ArrayPool.returnBlockDataArray(snapshot.blockData()); com.za.zenith.utils.ArrayPool.returnLightDataArray(snapshot.lightData()); }
                            }
                        }));
                        scheduled++;
                    }
                }
            }
            if (x == z || (x < 0 && x == -z) || (x > 0 && x == 1 - z)) { int t = dx; dx = -dz; dz = t; }
            x += dx; z += dz;
        }

        drawCallCount = 0;
        blockShader.setBoolean("uIsCompressed", true);
        blockShader.setBoolean("uIsBatch", true);
        
        opaqueBatch.reset();
        for (SectionRenderNode node : visibleSections) {
            ChunkMeshGenerator.ChunkMeshResult res = node.chunk.getCurrentMeshResult();
            if (res != null && res.opaqueSections()[node.sectionIdx] != null) {
                Mesh m = res.opaqueSections()[node.sectionIdx];
                if (m.getPool() != null) {
                    opaqueBatch.addMesh(m, node.chunk.getPosition().x() * 16, 0, node.chunk.getPosition().z() * 16, res.spawnTime());
                } else {
                    blockShader.setBoolean("uIsBatch", false);
                    modelMatrix.identity().translate(node.chunk.getPosition().x() * 16, 0, node.chunk.getPosition().z() * 16);
                    blockShader.setMatrix4f("model", modelMatrix);
                    blockShader.setFloat("uChunkSpawnTime", res.spawnTime());
                    m.render(blockShader);
                    drawCallCount++;
                    blockShader.setBoolean("uIsBatch", true);
                }
            }
        }
        opaqueBatch.render();
        drawCallCount++;

        glDepthMask(false);
        translucentBatch.reset();
        for (int i = visibleSections.size() - 1; i >= 0; i--) {
            SectionRenderNode node = visibleSections.get(i);
            ChunkMeshGenerator.ChunkMeshResult res = node.chunk.getCurrentMeshResult();
            if (res != null && res.translucentSections()[node.sectionIdx] != null) {
                Mesh m = res.translucentSections()[node.sectionIdx];
                if (m.getPool() != null) {
                    translucentBatch.addMesh(m, node.chunk.getPosition().x() * 16, 0, node.chunk.getPosition().z() * 16, res.spawnTime());
                } else {
                    blockShader.setBoolean("uIsBatch", false);
                    modelMatrix.identity().translate(node.chunk.getPosition().x() * 16, 0, node.chunk.getPosition().z() * 16);
                    blockShader.setMatrix4f("model", modelMatrix);
                    blockShader.setFloat("uChunkSpawnTime", res.spawnTime());
                    m.render(blockShader);
                    drawCallCount++;
                    blockShader.setBoolean("uIsBatch", true);
                }
            }
        }
        translucentBatch.render();
        drawCallCount++;
        glDepthMask(true);
        
        blockShader.setBoolean("uIsBatch", false);
        blockShader.setInt("uHiddenCount", 0);
        blockShader.setFloat("uChunkSpawnTime", -100.0f);
        vPool3.set(-1.0f, -1.0f, -1.0f); blockShader.setVector3f("uOverrideLight", vPool3);
        if (holeMesh != null) {
            modelMatrix.identity().translate(breakingPos.x(), breakingPos.y(), breakingPos.z());
            blockShader.setMatrix4f("model", modelMatrix);
            Chunk c = world.getChunk(com.za.zenith.world.chunks.ChunkPos.fromBlockPos(breakingPos.x(), breakingPos.z()));
            if (c != null) blockShader.setFloat("uChunkSpawnTime", c.getFirstSpawnTime());
            holeMesh.render(blockShader);
        }
        if (breakingPos != null && breakingMesh != null && currentBreakingBlock != null) renderBreakingProxyBlock(camera, world, alpha);
        renderPersistentScars(camera, world, alpha, hiddenCount);
        renderEntities(camera, world, alpha);
        renderBlockEntities(camera, world, alpha);
        renderPlayers(camera, world, networkClient, alpha);
    }

    private long packSectionPos(int cx, int cz, int secIdx) {
        return (((long)cx & 0xFFFFFFL) << 32) | (((long)cz & 0xFFFFFFL) << 8) | (secIdx & 0xFF);
    }

    private void renderViewModel(Camera camera, com.za.zenith.entities.Player player, Vector3f currentAmbient) {
        if (player == null) return;
        glDisable(GL_CULL_FACE); glDepthRange(0.0, 0.05);
        viewmodelShader.use(); atlas.bind();
        Matrix4f vmProj = new Matrix4f().setPerspective((float)Math.toRadians(70.0f), camera.getAspectRatio(), 0.01f, 1000.0f);
        viewmodelShader.setMatrix4f("projection", vmProj); viewmodelShader.setMatrix4f("view", new Matrix4f().identity());
        Matrix4f vMat = camera.getViewMatrix(1.0f); 
        List<com.za.zenith.world.lighting.LightSource> worldLights = com.za.zenith.world.lighting.LightManager.getActiveLights();
        List<com.za.zenith.world.lighting.LightSource> viewLights = new ArrayList<>();
        for (var ls : worldLights) {
            com.za.zenith.world.lighting.LightSource vls = new com.za.zenith.world.lighting.LightSource(ls.data);
            vMat.transformPosition(ls.position, vls.position); vMat.transformDirection(ls.direction, vls.direction);
            viewLights.add(vls);
        }
        viewmodelShader.setLights("uLights", viewLights);
        viewmodelShader.setVector3f("uCondition", new Vector3f(player.getDirt(), player.getBlood(), player.getWetness()));
        Viewmodel vm = player.getViewmodel();
        if (vm != null) {
            if (!vm.root.children.isEmpty() && vm.root.children.get(0).mesh == null) vm.initMeshes(atlas);
            ItemStack mainHand = player.getInventory().getSelectedItemStack();
            ItemStack offHand = player.getInventory().getStack(com.za.zenith.entities.Inventory.SLOT_OFFHAND);
            viewmodelRenderer.render(vm, viewmodelShader, atlas, player, mainHand, offHand, vfxManager.getHandHeat(), vfxManager.getItemHeat());
        }
        glDepthRange(0.0, 1.0); glEnable(GL_CULL_FACE);
    }
    
    private void renderPreviewBlock(Camera camera, float alpha) {
        glDisable(GL_CULL_FACE); blockShader.use(); blockShader.setBoolean("uIsCompressed", false);
        blockShader.setFloat("uSwayOverride", 0.0f); blockShader.setFloat("uChunkSpawnTime", -100.0f);
        modelMatrix.identity().translate(previewPos.x() + 0.5f, previewPos.y(), previewPos.z() + 0.5f);
        blockShader.setMatrix4f("model", modelMatrix); blockShader.setInt("previewPass", 1);
        blockShader.setFloat("previewAlpha", 0.35f); previewMesh.render(blockShader);
        blockShader.setInt("previewPass", 0); glEnable(GL_CULL_FACE);
    }

    private void renderPersistentScars(Camera camera, World world, float alpha, int hiddenCount) {
        if (world.getBlockDamageMap().isEmpty()) {
            persistentHoleCache.values().forEach(Mesh::cleanup); persistentHoleCache.clear();
            proxyMeshCache.values().forEach(Mesh::cleanup); proxyMeshCache.clear();
            return;
        }
        blockShader.use();
        persistentHoleCache.keySet().removeIf(pos -> {
            if (!world.getBlockDamageMap().containsKey(World.packBlockPos(pos.x(), pos.y(), pos.z()))) {
                Mesh m = persistentHoleCache.get(pos); if (m != null) m.cleanup(); return true;
            }
            return false;
        });
        proxyMeshCache.keySet().removeIf(pos -> {
            if (!world.getBlockDamageMap().containsKey(World.packBlockPos(pos.x(), pos.y(), pos.z()))) {
                Mesh m = proxyMeshCache.get(pos); if (m != null) m.cleanup(); return true;
            }
            return false;
        });
        for (var entry : world.getBlockDamageMap().entrySet()) {
            long packed = entry.getKey(); int bx = World.unpackBlockX(packed), by = World.unpackBlockY(packed), bz = World.unpackBlockZ(packed);
            BlockPos pos = new BlockPos(bx, by, bz); if (breakingPos != null && pos.equals(breakingPos)) continue;
            World.BlockDamageInstance info = entry.getValue(); Block block = info.getBlock();
            if (block == null || block.isAir()) continue;
            Mesh hole = persistentHoleCache.get(pos);
            if (hole == null) { hole = ChunkMeshGenerator.generateHoleMesh(pos, world, atlas); if (hole != null) persistentHoleCache.put(pos, hole); }
            if (hole != null) {
                blockShader.setBoolean("uIsProxy", false); blockShader.setInt("uHiddenCount", 0);
                modelMatrix.identity().translate(pos.x(), pos.y(), pos.z()); blockShader.setMatrix4f("model", modelMatrix);
                Chunk c = world.getChunk(com.za.zenith.world.chunks.ChunkPos.fromBlockPos(pos.x(), pos.z()));
                if (c != null) blockShader.setFloat("uChunkSpawnTime", c.getFirstSpawnTime());
                hole.render(blockShader); blockShader.setInt("uHiddenCount", hiddenCount);
            }
            Mesh mesh = proxyMeshCache.get(pos);
            if (mesh == null) { mesh = ChunkMeshGenerator.generateSingleBlockMesh(block, atlas, world, pos); if (mesh != null) proxyMeshCache.put(pos, mesh); }
            if (mesh != null) {
                var def = com.za.zenith.world.blocks.BlockRegistry.getBlock(block.getType());
                blockShader.setBoolean("uIsProxy", true);
                Chunk c = world.getChunk(com.za.zenith.world.chunks.ChunkPos.fromBlockPos(pos.x(), pos.z()));
                if (c != null) blockShader.setFloat("uChunkSpawnTime", c.getFirstSpawnTime());
                vPool1.set(1.0f); blockShader.setVector3f("uWobbleScale", vPool1);
                vPool1.set(0.0f); blockShader.setVector3f("uWobbleOffset", vPool1);
                blockShader.setFloat("uWobbleShake", 0.0f);
                blockShader.setFloat("uBreakingProgress", info.getDamage() / (def.getHardness() * 10.0f));
                blockShader.setInt("uBreakingPattern", def.getBreakingPattern());
                vPool1.set(0, -100, 0); blockShader.setVector3f("uWeakSpotPos", vPool1); 
                int hc = Math.min(16, info.getHitHistory().size()); blockShader.setInt("uHitCount", hc);
                for (int i = 0; i < hc; i++) blockShader.setVector4f("uHitHistory[" + i + "]", info.getHitHistory().get(i));
                modelMatrix.identity().translate(pos.x() + 0.5f, pos.y(), pos.z() + 0.5f);
                blockShader.setMatrix4f("model", modelMatrix); mesh.render(blockShader);
            }
        }
        blockShader.setBoolean("uIsProxy", false);
    }

    private void renderBreakingProxyBlock(Camera camera, World world, float alpha) {
        blockShader.use(); blockShader.setBoolean("uIsCompressed", false); blockShader.setFloat("uSwayOverride", -1.0f);
        Chunk c = world.getChunk(com.za.zenith.world.chunks.ChunkPos.fromBlockPos(breakingPos.x(), breakingPos.z()));
        blockShader.setFloat("uChunkSpawnTime", c != null ? c.getFirstSpawnTime() : -100.0f);
        var def = com.za.zenith.world.blocks.BlockRegistry.getBlock(currentBreakingBlock.getType());
        String animName = (def != null && def.getWobbleAnimation() != null) ? def.getWobbleAnimation() : "block_wobble";
        var profile = com.za.zenith.entities.parkour.animation.AnimationRegistry.get(animName);
        float sx = 1, sy = 1, sz = 1, ox = 0, oy = 0, oz = 0, sh = 0;
        if (profile != null) {
            float nt = wobbleTimer / Math.max(0.001f, profile.getDuration());
            sx = profile.evaluate("scale_x", nt, 1); sy = profile.evaluate("scale_y", nt, 1); sz = profile.evaluate("scale_z", nt, 1);
            ox = profile.evaluate("offset_x", nt, 0); oy = profile.evaluate("offset_y", nt, 0); oz = profile.evaluate("offset_z", nt, 0);
            sh = profile.evaluate("shake", nt, 0);
        }
        blockShader.setVector3f("uWobbleScale", vPool1.set(sx, sy, sz));
        blockShader.setVector3f("uWobbleOffset", vPool2.set(ox, oy, oz));
        blockShader.setFloat("uWobbleShake", sh); blockShader.setFloat("uWobbleTime", wobbleTimer);
        blockShader.setFloat("uBreakingProgress", breakingProgress); blockShader.setInt("uBreakingPattern", def.getBreakingPattern());
        blockShader.setVector3f("uBreakingHitPoint", breakingHitPoint); blockShader.setVector3f("uWeakSpotPos", weakSpotPos);
        blockShader.setVector3f("uWeakSpotColor", weakSpotColor); blockShader.setInt("uHitCount", hitCount);
        for (int i = 0; i < hitCount; i++) blockShader.setVector4f("uHitHistory[" + i + "]", hitHistory[i]);
        if (breakingMesh != null) {
            blockShader.setBoolean("uIsProxy", true);
            modelMatrix.identity().translate(breakingPos.x() + 0.5f, breakingPos.y(), breakingPos.z() + 0.5f);
            blockShader.setMatrix4f("model", modelMatrix); breakingMesh.render(blockShader);
            blockShader.setBoolean("uIsProxy", false);
        }
    }

    private void renderBlockHighlight(Camera camera, World world, RaycastResult highlightedBlock, float alpha) {
        if (highlightRenderer != null) highlightRenderer.render(camera, world, highlightedBlock, blockShader, modelMatrix, alpha, breakingPos, currentBreakingBlock, wobbleTimer);
    }

    private void setEntityLight(World world, Vector3f pos) {
        int x = (int) Math.floor(pos.x()), y = (int) Math.floor(pos.y()), z = (int) Math.floor(pos.z());
        var cp = com.za.zenith.world.chunks.ChunkPos.fromBlockPos(x, z);
        if (lastEntityChunk == null || !cp.equals(lastEntityChunkPos)) { lastEntityChunk = world.getChunkInternal(cp.x(), cp.z()); lastEntityChunkPos = cp; }
        if (lastEntityChunk != null) {
            vPool1.set(lastEntityChunk.getSunlight(x&15,y,z&15), lastEntityChunk.getBlockLight(x&15,y,z&15), 1.0f);
            blockShader.setVector3f("uOverrideLight", vPool1); blockShader.setFloat("uChunkSpawnTime", lastEntityChunk.getFirstSpawnTime());
        } else {
            blockShader.setVector3f("uOverrideLight", vPool1.set(15,0,1)); blockShader.setFloat("uChunkSpawnTime", -100.0f);
        }
    }

    private void renderEntities(Camera camera, World world, float alpha) {
        if (world.getEntities().isEmpty()) return;
        blockShader.use(); blockShader.setBoolean("uIsProxy", false);
        for (var entity : world.getEntities()) {
            Vector3f interpPos = entity.getInterpolatedPosition(alpha);
            if (!frustum.testAab(entity.getBoundingBox().getMin(), entity.getBoundingBox().getMax())) continue;
            setEntityLight(world, interpPos);
            if (entity instanceof com.za.zenith.entities.ScoutEntity scout) {
                if (playerMesh == null) createPlayerMesh();
                modelMatrix.identity().translate(interpPos.x(), interpPos.y(), interpPos.z()).rotateY(entity.getRotation().y);
                blockShader.setMatrix4f("model", modelMatrix); blockShader.setInt("highlightPass", 1);
                switch (scout.getCurrentState()) {
                    case CHASE: vPool1.set(1,0,0); break;
                    case SEARCH: vPool1.set(1,0.5f,0); break;
                    default: vPool1.set(0.5f,0.5f,0.5f); break;
                }
                blockShader.setVector3f("highlightColor", vPool1); playerMesh.render(blockShader);
            } else if (entity instanceof com.za.zenith.entities.ItemEntity itemEntity) {
                var p = world.getPlayer(); if (p != null && interpPos.distanceSquared(p.getPosition()) > 576) continue;
                var item = itemEntity.getStack().getItem();
                Mesh mesh = itemMeshCache.computeIfAbsent(item, i -> i.isBlock() ? ChunkMeshGenerator.generateSingleBlockMesh(new Block(i.getId()), atlas, null, null) : com.za.zenith.world.items.ItemMeshGenerator.generateItemMesh(i.getTexturePath(), atlas, i.getId()));
                if (mesh != null) {
                    float age = itemEntity.getAge() + alpha * 0.016f;
                    float scale = item.getDroppedScale() * (item.isBlock() ? 0.25f : 0.45f);
                    float yOff = item.isBlock() ? 0 : scale * 0.5f;
                    var rot = entity.getInterpolatedRotation(alpha);
                    modelMatrix.identity().translate(interpPos.x(), interpPos.y() + (float)Math.sin(age*2.5f)*0.02f + yOff, interpPos.z()).rotateX(rot.x).rotateY(rot.y).rotateZ(rot.z).scale(scale);
                    blockShader.setMatrix4f("model", modelMatrix); blockShader.setInt("highlightPass", 0); mesh.render(blockShader);
                }
            } else if (entity instanceof com.za.zenith.entities.ResourceEntity resource) {
                var item = resource.getStack().getItem();
                Mesh mesh = itemMeshCache.computeIfAbsent(item, i -> com.za.zenith.world.items.ItemMeshGenerator.generateItemMesh(i.getTexturePath(), atlas, i.getId()));
                if (mesh != null) {
                    float s = item.getDroppedScale();
                    modelMatrix.identity().translate(interpPos.x(), interpPos.y() + 0.03125f*s, interpPos.z()).rotateY(resource.getRotation().y).rotateX(1.5708f).scale(s);
                    blockShader.setMatrix4f("model", modelMatrix); blockShader.setInt("highlightPass", 0); mesh.render(blockShader);
                }
            } else if (entity instanceof com.za.zenith.entities.DecorationEntity decoration) {
                var def = decoration.getDefinition(); if (def == null) continue;
                Mesh mesh = entityDefMeshCache.computeIfAbsent(def, d -> "item".equals(d.modelType()) ? com.za.zenith.world.items.ItemMeshGenerator.generateItemMesh(d.texture(), atlas, 0) : ChunkMeshGenerator.generateSingleBlockMesh(new Block(com.za.zenith.world.blocks.BlockRegistry.getBlock(com.za.zenith.utils.Identifier.of(d.texture())).getId()), atlas, null, null));
                if (mesh != null) {
                    var s = def.visualScale();
                    modelMatrix.identity().translate(interpPos.x(), interpPos.y(), interpPos.z()).rotateY(entity.getRotation().y).scale(s.x, s.y, s.z);
                    blockShader.setMatrix4f("model", modelMatrix); blockShader.setInt("highlightPass", 0); mesh.render(blockShader);
                }
            } else {
                if (playerMesh == null) createPlayerMesh();
                modelMatrix.identity().translate(interpPos.x(), interpPos.y(), interpPos.z()).rotateY(entity.getRotation().y);
                blockShader.setMatrix4f("model", modelMatrix); blockShader.setInt("highlightPass", 0); playerMesh.render(blockShader);
            }
        }
        blockShader.setInt("highlightPass", 0);
    }

    private void renderBlockEntities(Camera camera, World world, float alpha) {
        if (world.getBlockEntities().isEmpty()) return;
        blockShader.use();
        for (var be : world.getBlockEntities().values()) {
            carvingRenderer.render(be, atlas, blockShader, modelMatrix, this, breakingPos, wobbleTimer);
            if (be instanceof com.za.zenith.world.blocks.entity.ICraftingSurface stump) {
                int count = stump.getActiveSlotsCount(); if (count == 0) continue;
                var p = be.getPos(); setEntityLight(world, vPool1.set(p.x(), p.y()+1, p.z()));
                for (int i = 0; i < 9; i++) {
                    var stack = stump.getStackInSlot(i); if (stack == null) continue;
                    var item = stack.getItem();
                    Mesh mesh = itemMeshCache.computeIfAbsent(item, it -> it.isBlock() ? ChunkMeshGenerator.generateSingleBlockMesh(new Block(it.getId()), atlas, null, null) : com.za.zenith.world.items.ItemMeshGenerator.generateItemMesh(it.getTexturePath(), atlas, it.getId()));
                    if (mesh != null) {
                        var t = com.za.zenith.world.blocks.CraftingLayoutEngine.getSlotTransform(i, count);
                        float s = (item.isBlock() ? 0.4f : item.getDroppedScale() * 0.6f) * t.y;
                        modelMatrix.identity().translate(p.x()+0.5f+t.x, p.y()+1.02f, p.z()+0.5f+t.z);
                        if (item.isBlock()) modelMatrix.scale(s); else modelMatrix.rotateX(1.5708f).scale(s);
                        blockShader.setMatrix4f("model", modelMatrix); blockShader.setInt("highlightPass", 0); mesh.render(blockShader);
                    }
                }
            }
        }
    }

    private void renderPlayers(Camera camera, World world, GameClient networkClient, float alpha) {
        if (networkClient == null || !networkClient.isConnected()) return;
        if (playerMesh == null) createPlayerMesh();
        for (var p : networkClient.getRemotePlayers().values()) {
            setEntityLight(world, vPool1.set(p.getX(), p.getY(), p.getZ()));
            modelMatrix.identity().translate(p.getX(), p.getY(), p.getZ()).scale(0.6f, 1.8f, 0.6f);
            blockShader.setMatrix4f("model", modelMatrix); blockShader.setInt("highlightPass", 1);
            blockShader.setVector3f("highlightColor", vPool1.set(0.3f, 0.6f, 1)); playerMesh.render(blockShader);
        }
        blockShader.setInt("highlightPass", 0);
    }

    private void createPlayerMesh() {
        float[] p = {-0.5f,-1,0.5f, 0.5f,-1,0.5f, 0.5f,1,0.5f, -0.5f,1,0.5f, -0.5f,-1,-0.5f, -0.5f,1,-0.5f, 0.5f,1,-0.5f, 0.5f,-1,-0.5f, -0.5f,-1,-0.5f, -0.5f,-1,0.5f, -0.5f,1,0.5f, -0.5f,1,-0.5f, 0.5f,-1,0.5f, 0.5f,-1,-0.5f, 0.5f,1,-0.5f, 0.5f,1,0.5f, -0.5f,1,0.5f, 0.5f,1,0.5f, 0.5f,1,-0.5f, -0.5f,1,-0.5f, -0.5f,-1,-0.5f, 0.5f,-1,-0.5f, 0.5f,-1,0.5f, -0.5f,-1,0.5f};
        float[] n = {0,0,1, 0,0,1, 0,0,1, 0,0,1, 0,0,-1, 0,0,-1, 0,0,-1, 0,0,-1, -1,0,0, -1,0,0, -1,0,0, -1,0,0, 1,0,0, 1,0,0, 1,0,0, 1,0,0, 0,1,0, 0,1,0, 0,1,0, 0,1,0, 0,-1,0, 0,-1,0, 0,-1,0, 0,-1,0};
        int[] ind = {0,1,2, 2,3,0, 4,5,6, 6,7,4, 8,9,10, 10,11,8, 12,13,14, 14,15,12, 16,17,18, 18,19,16, 20,21,22, 22,23,20};
        playerMesh = new Mesh(p, new float[48], n, new float[24], ind);
    }

    public void rebuildAllChunks() {
        blockMeshCache.values().forEach(Mesh::cleanup); blockMeshCache.clear();
        itemMeshCache.values().forEach(Mesh::cleanup); itemMeshCache.clear();
        Logger.info("Renderer: All meshes cleared for rebuild");
    }

    public void renderDebug(float fps, int w, int h) { if (debugRenderer != null) debugRenderer.renderFPS(fps, w, h); }
    public void toggleFXAA() { fxaaEnabled = !fxaaEnabled; }
    public DynamicTextureAtlas getAtlas() { return atlas; }
    public UIRenderer getUIRenderer() { return uiRenderer; }
    public void setHotbar(com.za.zenith.engine.graphics.ui.Hotbar h) { if (uiRenderer != null) uiRenderer.setHotbar(h); }
    public int getVisibleSectionsCount() { return visibleSections.size(); }
    public int getDrawCallCount() { return drawCallCount; }
    
    public void cleanup() {
        meshExecutor.shutdown();
        try { if (!meshExecutor.awaitTermination(1, TimeUnit.SECONDS)) meshExecutor.shutdownNow(); } catch (InterruptedException e) { meshExecutor.shutdownNow(); }
        persistentHoleCache.values().forEach(Mesh::cleanup); persistentHoleCache.clear();
        if (msaaFramebuffer != null) msaaFramebuffer.cleanup();
        if (resolveFramebuffer != null) resolveFramebuffer.cleanup();
        if (postProcessor != null) postProcessor.cleanup();
        if (uiRenderer != null) uiRenderer.cleanup();
        if (highlightRenderer != null) highlightRenderer.cleanup();
        if (playerMesh != null) playerMesh.cleanup();
        if (previewMesh != null) previewMesh.cleanup();
        if (heldItemMesh != null) heldItemMesh.cleanup();
        blockMeshCache.values().forEach(Mesh::cleanup);
        itemMeshCache.values().forEach(Mesh::cleanup);
        if (carvingRenderer != null) carvingRenderer.cleanup();
        particleRenderer.cleanup();
        skyRenderer.cleanup();
        if (atlas != null) atlas.cleanup();
        if (blockShader != null) blockShader.cleanup();
        com.za.zenith.utils.NioBufferPool.clearPools();
    }
}
