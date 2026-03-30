package com.za.minecraft.engine.graphics;

import com.za.minecraft.engine.core.Window;
import com.za.minecraft.engine.graphics.ui.UIRenderer;
import com.za.minecraft.network.GameClient;
import com.za.minecraft.world.BlockPos;
import com.za.minecraft.world.World;
import com.za.minecraft.world.blocks.Block;
import com.za.minecraft.world.chunks.Chunk;
import com.za.minecraft.world.chunks.ChunkMeshGenerator;
import com.za.minecraft.world.physics.RaycastResult;
import com.za.minecraft.world.items.ItemStack;
import com.za.minecraft.engine.graphics.model.ViewmodelRenderer;
import com.za.minecraft.engine.graphics.model.Viewmodel;
import com.za.minecraft.engine.graphics.model.ModelNode;
import org.joml.Matrix4f;
import org.joml.Vector3f;

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
    private boolean fxaaEnabled = false;
    private Mesh highlightMesh;
    private Mesh playerMesh;
    private Mesh previewMesh;
    private final Map<Integer, Mesh> blockMeshCache = new java.util.HashMap<>();
    private final Vector3f lightDirection;
    
    // Viewmodel
    private final ViewmodelRenderer viewmodelRenderer = new ViewmodelRenderer();
    
    // View Model caching
    private Mesh heldItemMesh;
    private int lastHeldTypeId = -1;
    private boolean lastHeldIsBlock = false;
    
    private Block currentPreviewBlock;
    private com.za.minecraft.world.BlockPos previewPos;
    private final Map<com.za.minecraft.world.items.Item, Mesh> itemMeshCache = new java.util.HashMap<>();
    private final Map<com.za.minecraft.entities.EntityDefinition, Mesh> entityDefMeshCache = new java.util.HashMap<>();
    
    public Renderer() {
        this.chunkMeshes = new ConcurrentHashMap<>();
        this.modelMatrix = new Matrix4f();
        this.lightDirection = new Vector3f(0.2f, -1.0f, 0.2f).normalize();
    }
    
    public void setPreviewBlock(com.za.minecraft.world.BlockPos pos, Block block) {
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
        for (com.za.minecraft.world.blocks.BlockDefinition def : com.za.minecraft.world.blocks.BlockRegistry.getRegistry().values()) {
            if (def.getTextures() != null) {
                for (int face = 0; face < 6; face++) {
                    String key = def.getTextures().getTextureForFace(face);
                    if (key != null) atlas.add(key, "src/main/resources/" + key);
                }
            }
            if (def.getUpperTexture() != null) atlas.add(def.getUpperTexture(), "src/main/resources/" + def.getUpperTexture());
        }
        for (com.za.minecraft.world.items.Item item : com.za.minecraft.world.items.ItemRegistry.getAllItems().values()) {
            String tex = item.getTexturePath();
            if (tex != null && !tex.isEmpty()) atlas.add(tex, "src/main/resources/" + tex);
        }
        
        // Add all viewmodels to atlas
        for (com.za.minecraft.engine.graphics.model.ViewmodelDefinition vmDef : com.za.minecraft.engine.graphics.model.ModelRegistry.getAllViewmodels()) {
            if (vmDef.texture != null) atlas.add(vmDef.texture, "src/main/resources/" + vmDef.texture);
        }

        for (com.za.minecraft.entities.EntityDefinition def : com.za.minecraft.entities.EntityRegistry.getAll().values()) {
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
        float[] glassUV = atlas.uvFor("minecraft/textures/block/glass.png");
        blockShader.setFloat("glassLayer", glassUV[2]);
        framebuffer = new Framebuffer(windowWidth, windowHeight);
        postProcessor = new PostProcessor();
        postProcessor.init();
        uiRenderer = new UIRenderer();
        uiRenderer.init();
        carvingRenderer = new CarvingRenderer();
    }
    
    public void render(Window window, Camera camera, World world, RaycastResult highlightedBlock, com.za.minecraft.network.GameClient networkClient, float alpha) {
        framebuffer.resize(window.getWidth(), window.getHeight());
        framebuffer.bind();
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
        renderScene(camera, world, networkClient, alpha);
        
        if (highlightedBlock != null && highlightedBlock.isHit()) renderBlockHighlight(camera, highlightedBlock, alpha);
        if (previewPos != null && previewMesh != null) renderPreviewBlock(camera, alpha);

        renderViewModel(camera, world.getPlayer());

        framebuffer.unbind();
        glViewport(0, 0, window.getWidth(), window.getHeight());
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
        if (fxaaEnabled) postProcessor.processFXAA(framebuffer.getColorTextureId(), framebuffer.getDepthTextureId(), window.getWidth(), window.getHeight());
        else postProcessor.processPassthrough(framebuffer.getColorTextureId(), framebuffer.getDepthTextureId(), window.getWidth(), window.getHeight());
        
        uiRenderer.renderCrosshair(window.getWidth(), window.getHeight());
        uiRenderer.renderHotbar(window.getWidth(), window.getHeight(), atlas);
        uiRenderer.renderPauseMenu(window.getWidth(), window.getHeight());
    }

    private void renderViewModel(Camera camera, com.za.minecraft.entities.Player player) {
        if (player == null) return;
        glDisable(GL_CULL_FACE);
        glDepthRange(0.0, 0.05);
        
        blockShader.use();
        atlas.bind();
        Matrix4f viewModelProjection = new Matrix4f().setPerspective((float)Math.toRadians(70.0f), camera.getAspectRatio(), 0.01f, 1000.0f);
        blockShader.setMatrix4f("projection", viewModelProjection);
        blockShader.setMatrix4f("view", new Matrix4f().identity());
        blockShader.setBoolean("viewModelPass", true);
        blockShader.setVector3f("lightDirection", new Vector3f(0.4f, -0.8f, 0.4f).normalize());
        
        // Состояние рук
        blockShader.setVector3f("uCondition", new Vector3f(player.getDirt(), player.getBlood(), player.getWetness()));

        Viewmodel vm = player.getViewmodel();
        if (vm != null) {
            if (!vm.root.children.isEmpty() && vm.root.children.get(0).mesh == null) {
                vm.initMeshes(atlas);
            }
            
            ItemStack mainHand = player.getInventory().getSelectedItemStack();
            ItemStack offHand = player.getInventory().getStack(com.za.minecraft.entities.Inventory.SLOT_OFFHAND);
            viewmodelRenderer.render(vm, blockShader, atlas, player, mainHand, offHand);
        }

        blockShader.setInt("highlightPass", 0);
        blockShader.setBoolean("viewModelPass", false);
        blockShader.setVector3f("lightDirection", lightDirection);
        
        glDepthRange(0.0, 1.0); 
        glEnable(GL_CULL_FACE);
    }
    
    private void renderPreviewBlock(Camera camera, float alpha) {
        glDisable(GL_CULL_FACE);
        blockShader.use();
        blockShader.setMatrix4f("projection", camera.getProjectionMatrix());
        blockShader.setMatrix4f("view", camera.getViewMatrix(alpha));
        modelMatrix.identity().translate(previewPos.x(), previewPos.y(), previewPos.z());
        blockShader.setMatrix4f("model", modelMatrix);
        blockShader.setInt("previewPass", 1);
        blockShader.setFloat("previewAlpha", 0.35f);
        previewMesh.render();
        blockShader.setInt("previewPass", 0);
        glEnable(GL_CULL_FACE);
    }
    
    private void renderScene(Camera camera, World world, com.za.minecraft.network.GameClient networkClient, float alpha) {
        blockShader.use();
        atlas.bind();
        blockShader.setMatrix4f("projection", camera.getProjectionMatrix());
        blockShader.setMatrix4f("view", camera.getViewMatrix(alpha));
        blockShader.setBoolean("useMask", false);
        blockShader.setBoolean("previewPass", false);
        blockShader.setFloat("brightnessMultiplier", 1.0f);
        blockShader.setInt("highlightPass", 0);
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
        renderEntities(camera, world, alpha);
        renderBlockEntities(camera, world, alpha);
        renderPlayers(camera, networkClient, alpha);
    }
    
    private void renderBlockHighlight(Camera camera, RaycastResult highlightedBlock, float alpha) {
        glDepthMask(false);
        glDisable(GL_CULL_FACE);
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        glLineWidth(3.0f);
        blockShader.use();
        blockShader.setMatrix4f("projection", camera.getProjectionMatrix());
        blockShader.setMatrix4f("view", camera.getViewMatrix(alpha));
        modelMatrix.identity().translate(highlightedBlock.getBlockPos().x(), highlightedBlock.getBlockPos().y(), highlightedBlock.getBlockPos().z()).scale(1.002f);
        blockShader.setMatrix4f("model", modelMatrix);
        blockShader.setInt("highlightPass", 1);
        blockShader.setVector3f("highlightColor", new Vector3f(0.2f, 0.2f, 0.2f));
        if (highlightMesh == null) createHighlightMesh();
        glEnable(GL_POLYGON_OFFSET_LINE);
        glPolygonOffset(-1.0f, -1.0f);
        highlightMesh.render(GL_LINES);
        glDisable(GL_POLYGON_OFFSET_LINE);
        blockShader.setInt("highlightPass", 0);
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        glEnable(GL_CULL_FACE);
        glDepthMask(true);
    }

    private void renderEntities(Camera camera, World world, float alpha) {
        if (world.getEntities().isEmpty()) return;

        blockShader.use();
        for (com.za.minecraft.entities.Entity entity : world.getEntities()) {
            if (entity instanceof com.za.minecraft.entities.ScoutEntity scout) {
                if (playerMesh == null) createPlayerMesh();
                modelMatrix.identity()
                    .translate(entity.getPosition().x(), entity.getPosition().y(), entity.getPosition().z())
                    .rotateY(entity.getRotation().y);

                blockShader.setMatrix4f("model", modelMatrix);
                blockShader.setInt("highlightPass", 1);
                switch (scout.getCurrentState()) {
                    case CHASE: blockShader.setVector3f("highlightColor", new Vector3f(1.0f, 0.0f, 0.0f)); break;
                    case SEARCH: blockShader.setVector3f("highlightColor", new Vector3f(1.0f, 0.5f, 0.0f)); break;
                    default: blockShader.setVector3f("highlightColor", new Vector3f(0.5f, 0.5f, 0.5f)); break;
                }
                playerMesh.render();
            } else if (entity instanceof com.za.minecraft.entities.ItemEntity itemEntity) {
                com.za.minecraft.world.items.Item item = itemEntity.getStack().getItem();
                Mesh mesh = itemMeshCache.get(item);

                if (mesh == null) {
                    if (item.isBlock()) {
                        mesh = ChunkMeshGenerator.generateSingleBlockMesh(new Block(item.getId()), atlas);
                    } else {
                        mesh = com.za.minecraft.world.items.ItemMeshGenerator.generateItemMesh(item.getTexturePath(), atlas, item.getId());
                    }
                    if (mesh != null) itemMeshCache.put(item, mesh);
                }

                if (mesh != null) {
                    float age = itemEntity.getAge();
                    float bob = (float) Math.sin(age * 2.5f) * 0.05f;
                    float scale = item.isBlock() ? 0.25f : item.getVisualScale() * 0.45f;

                    modelMatrix.identity()
                        .translate(entity.getPosition().x(), entity.getPosition().y() + 0.1f + bob, entity.getPosition().z());
                    
                    modelMatrix.rotateX(itemEntity.getRotation().x)
                               .rotateY(itemEntity.getRotation().y)
                               .rotateZ(itemEntity.getRotation().z)
                               .scale(scale);

                    if (item.isBlock()) {
                        modelMatrix.translate(-0.5f, 0, -0.5f);
                    }

                    blockShader.setMatrix4f("model", modelMatrix);
                    blockShader.setInt("highlightPass", 0);
                    mesh.render();
                }
            } else if (entity instanceof com.za.minecraft.entities.ResourceEntity resource) {
                com.za.minecraft.world.items.Item item = resource.getStack().getItem();
                Mesh mesh = itemMeshCache.get(item);

                if (mesh == null) {
                    mesh = com.za.minecraft.world.items.ItemMeshGenerator.generateItemMesh(item.getTexturePath(), atlas, item.getId());
                    if (mesh != null) itemMeshCache.put(item, mesh);
                }

                if (mesh != null) {
                    float scale = item.getVisualScale();
                    modelMatrix.identity()
                        .translate(entity.getPosition().x(), entity.getPosition().y() + 0.05f, entity.getPosition().z())
                        .rotateY(resource.getRotation().y)
                        .rotateX(1.5708f) 
                        .scale(scale);

                    blockShader.setMatrix4f("model", modelMatrix);
                    blockShader.setInt("highlightPass", 0);
                    mesh.render();
                }
            } else if (entity instanceof com.za.minecraft.entities.DecorationEntity decoration) {
                com.za.minecraft.entities.EntityDefinition def = decoration.getDefinition();
                if (def == null) continue;

                Mesh mesh = entityDefMeshCache.get(def);
                if (mesh == null) {
                    if ("item".equals(def.modelType())) {
                        mesh = com.za.minecraft.world.items.ItemMeshGenerator.generateItemMesh(def.texture(), atlas, 0);
                    } else if ("block".equals(def.modelType())) {
                        com.za.minecraft.utils.Identifier blockId = com.za.minecraft.utils.Identifier.of(def.texture());
                        com.za.minecraft.world.blocks.BlockDefinition blockDef = com.za.minecraft.world.blocks.BlockRegistry.getBlock(blockId);
                        if (blockDef != null) {
                            mesh = ChunkMeshGenerator.generateSingleBlockMesh(new Block(blockDef.getId()), atlas);
                        }
                    }
                    if (mesh != null) entityDefMeshCache.put(def, mesh);
                }

                if (mesh != null) {
                    org.joml.Vector3f scale = def.visualScale();
                    modelMatrix.identity()
                        .translate(entity.getPosition().x(), entity.getPosition().y(), entity.getPosition().z())
                        .rotateY(entity.getRotation().y);

                    if ("block".equals(def.modelType())) {
                        modelMatrix.scale(scale.x, scale.y, scale.z)
                                   .translate(-0.5f, 0, -0.5f);
                    } else {
                        modelMatrix.scale(scale.x, scale.y, scale.z);
                    }

                    blockShader.setMatrix4f("model", modelMatrix);
                    blockShader.setInt("highlightPass", 0);
                    mesh.render();
                }
            } else {
                if (playerMesh == null) createPlayerMesh();
                modelMatrix.identity()
                    .translate(entity.getPosition().x(), entity.getPosition().y(), entity.getPosition().z())
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
        for (com.za.minecraft.world.blocks.entity.BlockEntity be : world.getBlockEntities().values()) {
            carvingRenderer.render(be, atlas, blockShader, modelMatrix, this);

            if (be instanceof com.za.minecraft.world.blocks.entity.StumpBlockEntity stump) {
                int totalItems = stump.getActiveSlotsCount();
                if (totalItems == 0) continue;

                for (int i = 0; i < 9; i++) {
                    com.za.minecraft.world.items.ItemStack stack = stump.getStackInSlot(i);
                    if (stack == null) continue;

                    com.za.minecraft.world.items.Item item = stack.getItem();
                    Mesh mesh = itemMeshCache.get(item);
                    if (mesh == null) {
                        if (item.isBlock()) {
                            mesh = ChunkMeshGenerator.generateSingleBlockMesh(new com.za.minecraft.world.blocks.Block(item.getId()), atlas);
                        } else {
                            mesh = com.za.minecraft.world.items.ItemMeshGenerator.generateItemMesh(item.getTexturePath(), atlas, item.getId());
                        }
                        if (mesh != null) itemMeshCache.put(item, mesh);
                    }

                    if (mesh != null) {
                        org.joml.Vector3f transform = com.za.minecraft.world.blocks.CraftingLayoutEngine.getSlotTransform(i, totalItems);
                        float scale = item.isBlock() ? 0.4f : item.getVisualScale() * 0.6f;
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

    private void renderPlayers(Camera camera, com.za.minecraft.network.GameClient networkClient, float alpha) {
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

    private void createHighlightMesh() {
        float[] positions = {0,0,0, 1,0,0, 1,1,0, 0,1,0, 0,0,1, 1,0,1, 1,1,1, 0,1,1};
        float[] tc = new float[16]; float[] n = new float[24]; float[] bt = new float[8];
        int[] indices = {0,1, 1,5, 5,4, 4,0, 3,2, 2,6, 6,7, 7,3, 0,3, 1,2, 5,6, 4,7};
        highlightMesh = new Mesh(positions, tc, n, bt, indices);
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
    public void setHotbar(com.za.minecraft.engine.graphics.ui.Hotbar h) { if (uiRenderer != null) uiRenderer.setHotbar(h); }
    public void setPauseMenu(com.za.minecraft.engine.graphics.ui.PauseMenu m) { if (uiRenderer != null) uiRenderer.setPauseMenu(m); }
    
    public void cleanup() {
        for (var r : chunkMeshes.values()) { if (r.opaqueMesh != null) r.opaqueMesh.cleanup(); if (r.translucentMesh != null) r.translucentMesh.cleanup(); }
        if (framebuffer != null) framebuffer.cleanup();
        if (postProcessor != null) postProcessor.cleanup();
        if (uiRenderer != null) uiRenderer.cleanup();
        if (highlightMesh != null) highlightMesh.cleanup();
        if (playerMesh != null) playerMesh.cleanup();
        if (previewMesh != null) previewMesh.cleanup();
        if (heldItemMesh != null) heldItemMesh.cleanup();
        for (var m : blockMeshCache.values()) m.cleanup();
        for (var m : itemMeshCache.values()) m.cleanup();
        if (carvingRenderer != null) carvingRenderer.cleanup();
        if (atlas != null) atlas.cleanup();
        if (blockShader != null) blockShader.cleanup();
    }
}
