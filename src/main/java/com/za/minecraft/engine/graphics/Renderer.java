package com.za.minecraft.engine.graphics;

import com.za.minecraft.engine.core.Window;
import com.za.minecraft.engine.graphics.ui.UIRenderer;
import com.za.minecraft.network.GameClient;
import com.za.minecraft.world.BlockPos;
import com.za.minecraft.world.World;
import com.za.minecraft.world.blocks.Block;
import com.za.minecraft.world.blocks.Blocks;
import com.za.minecraft.world.chunks.Chunk;

import com.za.minecraft.world.chunks.ChunkMeshGenerator;
import com.za.minecraft.world.physics.RaycastResult;
import com.za.minecraft.world.items.ItemStack;
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
    private boolean fxaaEnabled = false;
    private Mesh highlightMesh;
    private Mesh playerMesh;
    private Mesh previewMesh;
    private final Vector3f lightDirection;
    
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
        
        blockShader = new Shader(
            "src/main/resources/shaders/vertex.glsl",
            "src/main/resources/shaders/fragment.glsl"
        );
        
        atlas = new DynamicTextureAtlas(16);
        // Add all block textures
        for (com.za.minecraft.world.blocks.BlockDefinition def : com.za.minecraft.world.blocks.BlockRegistry.getRegistry().values()) {
            if (def.getTextures() != null) {
                for (int face = 0; face < 6; face++) {
                    String key = def.getTextures().getTextureForFace(face);
                    if (key != null) {
                        atlas.add(key, "src/main/resources/" + key);
                    }
                }
            }
            if (def.getUpperTexture() != null) {
                atlas.add(def.getUpperTexture(), "src/main/resources/" + def.getUpperTexture());
            }
        }
        // Add all item textures
        for (com.za.minecraft.world.items.Item item : com.za.minecraft.world.items.ItemRegistry.getAllItems().values()) {
            String tex = item.getTexturePath();
            if (tex != null && !tex.isEmpty()) {
                String path = "src/main/resources/" + tex;
                atlas.add(tex, path);
            }
        }
        // Add all entity textures
        for (com.za.minecraft.entities.EntityDefinition def : com.za.minecraft.entities.EntityRegistry.getAll().values()) {
            if ("item".equals(def.modelType())) {
                String tex = def.texture();
                if (tex != null && !tex.isEmpty()) {
                    String path = "src/main/resources/" + tex;
                    atlas.add(tex, path);
                }
            }
        }
        atlas.build();
        
        blockShader.use();
        blockShader.setVector3f("lightDirection", new Vector3f(0.2f, -1.0f, 0.2f).normalize());
        blockShader.setVector3f("lightColor", new Vector3f(0.3f, 0.3f, 0.25f)); // Слабый directional свет
        blockShader.setVector3f("ambientLight", new Vector3f(0.85f, 0.85f, 0.9f)); // Яркий ambient как в Minecraft
        blockShader.setInt("textureSampler", 0);
        
        // Передаем UV координаты grass_block_top.png для правильного окрашивания
        float[] grassTopUV = atlas.uvFor("minecraft/textures/block/grass_block_top.png");
        blockShader.setUniform("grassTopUV", grassTopUV[0], grassTopUV[1], grassTopUV[2], grassTopUV[5]);
        
        // Передаем UV координаты oak_leaves.png для окрашивания листвы
        float[] leavesUV = atlas.uvFor("minecraft/textures/block/oak_leaves.png");
        blockShader.setUniform("leavesUV", leavesUV[0], leavesUV[1], leavesUV[2], leavesUV[5]);

        // Connected Glass UVs
        float[] glassUV = atlas.uvFor("minecraft/textures/block/glass.png");
        blockShader.setUniform("glassUV", glassUV[0], glassUV[1], glassUV[2], glassUV[5]);
        
        framebuffer = new Framebuffer(windowWidth, windowHeight);
        postProcessor = new PostProcessor();
        postProcessor.init();
        
        uiRenderer = new UIRenderer();
        uiRenderer.init();
    }
    
    public void render(Window window, Camera camera, World world, RaycastResult highlightedBlock, com.za.minecraft.network.GameClient networkClient) {
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
        
        // Render preview block
        if (previewPos != null && previewMesh != null) {
            renderPreviewBlock(camera);
        }

        // Render View Model (Hand and Item)
        renderViewModel(camera, world.getPlayer());
        
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
            postProcessor.processPassthrough(framebuffer.getColorTextureId());
        }
        
        uiRenderer.renderCrosshair(window.getWidth(), window.getHeight());
        uiRenderer.renderHotbar(window.getWidth(), window.getHeight(), atlas);
        uiRenderer.renderPauseMenu(window.getWidth(), window.getHeight());
    }

    private void renderViewModel(Camera camera, com.za.minecraft.entities.Player player) {
        if (player == null) return;

        // --- View Model State ---
        glDisable(GL_CULL_FACE); // Disable culling for view model to prevent disappearing polygons
        glClear(GL_DEPTH_BUFFER_BIT); // Clear depth to draw on top
        
        blockShader.use();
        atlas.bind();
        
        // Use a fixed FOV for the view model (Minecraft style)
        Matrix4f viewModelProjection = new Matrix4f().setPerspective((float)Math.toRadians(70.0f), camera.getAspectRatio(), 0.01f, 1000.0f);
        blockShader.setMatrix4f("projection", viewModelProjection);
        blockShader.setMatrix4f("view", new Matrix4f().identity());
        blockShader.setBoolean("viewModelPass", true);
        
        // Use fixed camera-relative light direction for view model
        blockShader.setVector3f("lightDirection", new Vector3f(0.4f, -0.8f, 0.4f).normalize());
        
        Matrix4f viewModelMatrix = new Matrix4f().identity();
        
        float intensity = player.getBobIntensity();
        float bobX = (float) Math.sin(player.getWalkBobTimer() * 0.5f) * 0.05f * intensity;
        float bobY = (float) Math.sin(player.getWalkBobTimer()) * 0.04f * intensity;

        float swing = player.getSwingProgress();
        float swingAngle = (float) Math.sin(swing * Math.PI) * 1.2f;
        float swingMoveX = (float) Math.sin(swing * Math.PI) * 0.4f;
        float swingMoveY = (float) Math.sin(swing * Math.PI) * 0.2f;

        // --- Render Held Item ---
        ItemStack stack = player.getInventory().getSelectedItemStack();
        if (stack != null) {
            com.za.minecraft.world.items.Item item = stack.getItem();
            int currentTypeId = item.getId();
            boolean isBlock = item.isBlock();
            com.za.minecraft.world.items.Item.ViewmodelTransform transform = item.getViewmodelTransform();

            // Cache item mesh if type or category changed
            if (lastHeldTypeId != currentTypeId || lastHeldIsBlock != isBlock) {
                if (heldItemMesh != null) heldItemMesh.cleanup();
                
                if (isBlock) {
                    heldItemMesh = ChunkMeshGenerator.generateSingleBlockMesh(new Block(currentTypeId), atlas);
                } else {
                    heldItemMesh = com.za.minecraft.world.items.ItemMeshGenerator.generateItemMesh(item.getTexturePath(), atlas, currentTypeId);
                }
                lastHeldTypeId = currentTypeId;
                lastHeldIsBlock = isBlock;
            }

            if (heldItemMesh != null) {
                viewModelMatrix.identity();
                
                float px = transform.px + bobX - (isBlock ? swingMoveX : swingMoveX * 0.5f);
                float py = transform.py + bobY - (isBlock ? swingMoveY : swingMoveY * 0.8f);
                float pz = transform.pz + (swingMoveX * 0.2f);
                
                float rx = (float)Math.toRadians(transform.rx) - (isBlock ? swingAngle * 0.4f : swingAngle * 1.5f);
                float ry = (float)Math.toRadians(transform.ry) + (isBlock ? swingAngle * 0.2f : swingAngle * 0.2f);
                float rz = (float)Math.toRadians(transform.rz) + (isBlock ? 0.1f : 0.0f);

                viewModelMatrix.translate(px, py, pz)
                    .rotateX(rx)
                    .rotateY(ry)
                    .rotateZ(rz)
                    .scale(transform.scale);
                
                blockShader.setMatrix4f("model", viewModelMatrix);
                blockShader.setInt("highlightPass", 0);
                heldItemMesh.render();
            }
        } else {
            lastHeldTypeId = -1;
            if (heldItemMesh != null) {
                heldItemMesh.cleanup();
                heldItemMesh = null;
            }
        }

        // --- Cleanup View Model State ---
        blockShader.setInt("highlightPass", 0);
        blockShader.setBoolean("viewModelPass", false);
        blockShader.setVector3f("lightDirection", lightDirection); // Restore world light dir
        glEnable(GL_CULL_FACE);
    }
    
    private void renderPreviewBlock(Camera camera) {
        glDisable(GL_CULL_FACE);
        blockShader.use();
        blockShader.setMatrix4f("projection", camera.getProjectionMatrix());
        blockShader.setMatrix4f("view", camera.getViewMatrix());
        
        modelMatrix.identity().translate(previewPos.x(), previewPos.y(), previewPos.z());
        blockShader.setMatrix4f("model", modelMatrix);
        
        blockShader.setInt("previewPass", 1);
        blockShader.setFloat("previewAlpha", 0.35f);
        
        previewMesh.render();
        
        blockShader.setInt("previewPass", 0);
        glEnable(GL_CULL_FACE);
    }
    
    private void renderScene(Camera camera, World world, com.za.minecraft.network.GameClient networkClient) {
        blockShader.use();
        atlas.bind();
        blockShader.setMatrix4f("projection", camera.getProjectionMatrix());
        blockShader.setMatrix4f("view", camera.getViewMatrix());
        
        for (Chunk chunk : world.getLoadedChunks()) {
            if (chunk.needsMeshUpdate() || !chunkMeshes.containsKey(chunk)) {
                updateChunkMesh(chunk, world);
            }
            
            ChunkMeshGenerator.ChunkMeshResult result = chunkMeshes.get(chunk);
            if (result != null && result.opaqueMesh != null) {
                modelMatrix.identity().translate(
                    chunk.getPosition().x() * Chunk.CHUNK_SIZE,
                    0,
                    chunk.getPosition().z() * Chunk.CHUNK_SIZE
                );
                blockShader.setMatrix4f("model", modelMatrix);
                result.opaqueMesh.render();
            }
        }
        
        glDepthMask(false);
        for (Chunk chunk : world.getLoadedChunks()) {
            ChunkMeshGenerator.ChunkMeshResult result = chunkMeshes.get(chunk);
            if (result != null && result.translucentMesh != null) {
                modelMatrix.identity().translate(
                    chunk.getPosition().x() * Chunk.CHUNK_SIZE,
                    0,
                    chunk.getPosition().z() * Chunk.CHUNK_SIZE
                );
                blockShader.setMatrix4f("model", modelMatrix);
                result.translucentMesh.render();
            }
        }
        glDepthMask(true);
        
        renderEntities(camera, world);
        renderBlockEntities(camera, world);
        renderPlayers(camera, networkClient);
    }
    
    private void renderBlockEntities(Camera camera, World world) {
        if (world.getBlockEntities().isEmpty()) return;
        
        blockShader.use();
        for (com.za.minecraft.world.blocks.entity.BlockEntity be : world.getBlockEntities().values()) {
            if (be instanceof com.za.minecraft.world.blocks.entity.ICraftingSurface surface) {
                int totalItems = surface.getActiveSlotsCount();
                if (totalItems == 0) continue;

                for (int i = 0; i < 9; i++) {
                    com.za.minecraft.world.items.ItemStack stack = surface.getStackInSlot(i);
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
                        float finalScale = scale * transform.y; // transform.y is the layout scale factor
                        
                        BlockPos pos = be.getPos();
                        modelMatrix.identity()
                            .translate(pos.x() + 0.5f + transform.x, pos.y() + 1.01f, pos.z() + 0.5f + transform.z);
                        
                        if (item.isBlock()) {
                            modelMatrix.scale(finalScale)
                                       .translate(-0.5f, 0, -0.5f);
                        } else {
                            modelMatrix.rotateX(1.57f) // Lay flat
                                       .scale(finalScale)
                                       .translate(0, -0.5f, 0);
                        }
                        
                        blockShader.setMatrix4f("model", modelMatrix);
                        blockShader.setInt("highlightPass", 0);
                        mesh.render();
                    }
                }
            }
        }
    }

    private void renderEntities(Camera camera, World world) {
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
                        .translate(entity.getPosition().x(), entity.getPosition().y() + 0.2f + bob, entity.getPosition().z());
                    if (item.isBlock()) {
                        modelMatrix.rotateY(itemEntity.getRotation().y)
                                   .rotateX(0.2f) // Slight tilt
                                   .scale(scale)
                                   .translate(-0.5f, -0.5f, -0.5f); // Center the block mesh
                    } else {
                        // Billboard: face the camera but also spin around own Y axis
                        modelMatrix.rotateY(-camera.getRotation().y)
                                   .rotateX(camera.getRotation().x)
                                   .rotateY(itemEntity.getRotation().y) // Spin effect
                                   .scale(scale)
                                   .translate(0, -0.5f, 0); // Center sprite mesh vertically
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
                        .rotateX(1.57f) // Lay flat on ground
                        .scale(scale)
                        .translate(0, -0.5f, 0); // Center sprite mesh
                    
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
                        // Если это блок, то texture в JSON - это его Identifier
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
                        // Блоки центрируются по осям X и Z, и лежат на земле Y=0
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

    private Mesh createItemSpriteMesh(com.za.minecraft.world.items.Item item) {
        float[] uv = atlas.uvFor(item.getTexturePath());
        if (uv == null) uv = new float[]{0, 0, 1, 1};

        float[] positions = {
            -0.5f, -0.5f, 0.0f,
             0.5f, -0.5f, 0.0f,
             0.5f,  0.5f, 0.0f,
            -0.5f,  0.5f, 0.0f
        };
        float[] texCoords = {
            uv[0], uv[3],
            uv[2], uv[3],
            uv[2], uv[1],
            uv[0], uv[1]
        };
        float[] normals = {
            0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1
        };
        int[] indices = { 0, 1, 2, 2, 3, 0 };
        float[] blockTypes = { -1, -1, -1, -1 };
        
        return new Mesh(positions, texCoords, normals, blockTypes, indices);
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
    
    public DynamicTextureAtlas getAtlas() {
        return atlas;
    }
    
    public UIRenderer getUIRenderer() {
        return uiRenderer;
    }
    
    public void setHotbar(com.za.minecraft.engine.graphics.ui.Hotbar hotbar) {
        if (uiRenderer != null) {
            uiRenderer.setHotbar(hotbar);
        }
    }
    
    public void setPauseMenu(com.za.minecraft.engine.graphics.ui.PauseMenu pauseMenu) {
        if (uiRenderer != null) {
            uiRenderer.setPauseMenu(pauseMenu);
        }
    }
    
    private void updateChunkMesh(Chunk chunk, World world) {
        ChunkMeshGenerator.ChunkMeshResult oldMesh = chunkMeshes.get(chunk);
        if (oldMesh != null) {
            if (oldMesh.opaqueMesh != null) oldMesh.opaqueMesh.cleanup();
            if (oldMesh.translucentMesh != null) oldMesh.translucentMesh.cleanup();
        }
        ChunkMeshGenerator.ChunkMeshResult newMesh = ChunkMeshGenerator.generateMesh(chunk, world, atlas);
        chunkMeshes.put(chunk, newMesh);
        chunk.setMeshUpdated();
    }
    
    private void renderBlockHighlight(Camera camera, RaycastResult highlightedBlock) {
        glDepthMask(false);
        glDisable(GL_CULL_FACE);
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);
        glLineWidth(3.0f);
        
        blockShader.use();
        blockShader.setMatrix4f("projection", camera.getProjectionMatrix());
        blockShader.setMatrix4f("view", camera.getViewMatrix());
        
        modelMatrix.identity().translate(
            highlightedBlock.getBlockPos().x(),
            highlightedBlock.getBlockPos().y(),
            highlightedBlock.getBlockPos().z()
        );
        modelMatrix.scale(1.002f, 1.002f, 1.002f);
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
        glLineWidth(1.0f);
    }
    
    private void createHighlightMesh() {
        float[] positions = {
            0, 0, 0, 1, 0, 0, 1, 1, 0, 0, 1, 0,
            0, 0, 1, 1, 0, 1, 1, 1, 1, 0, 1, 1
        };
        float[] texCoords = new float[positions.length / 3 * 2];
        float[] normals = new float[positions.length];
        float[] blockTypes = new float[positions.length / 3];
        int[] indices = {
            0, 1, 1, 5, 5, 4, 4, 0,
            3, 2, 2, 6, 6, 7, 7, 3,
            0, 3, 1, 2, 5, 6, 4, 7
        };
        highlightMesh = new Mesh(positions, texCoords, normals, blockTypes, indices);
    }
    
    private void renderPlayers(Camera camera, com.za.minecraft.network.GameClient networkClient) {
        if (networkClient == null || !networkClient.isConnected()) return;
        
        blockShader.setInt("highlightPass", 0);
        if (playerMesh == null) createPlayerMesh();
        
        for (var player : networkClient.getRemotePlayers().values()) {
            modelMatrix.identity()
                .translate(player.getX(), player.getY(), player.getZ())
                .scale(0.6f, 1.8f, 0.6f);
            
            blockShader.setMatrix4f("model", modelMatrix);
            blockShader.setInt("highlightPass", 1);
            blockShader.setVector3f("highlightColor", new Vector3f(0.3f, 0.6f, 1.0f));
            playerMesh.render();
        }
        blockShader.setInt("highlightPass", 0);
    }
    
    private void createPlayerMesh() {
        // Standard cube with 24 vertices to have distinct normals per face
        float[] positions = {
            // Front face (+Z)
            -0.5f, -1.0f,  0.5f,  0.5f, -1.0f,  0.5f,  0.5f,  1.0f,  0.5f, -0.5f,  1.0f,  0.5f,
            // Back face (-Z)
            -0.5f, -1.0f, -0.5f, -0.5f,  1.0f, -0.5f,  0.5f,  1.0f, -0.5f,  0.5f, -1.0f, -0.5f,
            // Left face (-X)
            -0.5f, -1.0f, -0.5f, -0.5f, -1.0f,  0.5f, -0.5f,  1.0f,  0.5f, -0.5f,  1.0f, -0.5f,
            // Right face (+X)
             0.5f, -1.0f,  0.5f,  0.5f, -1.0f, -0.5f,  0.5f,  1.0f, -0.5f,  0.5f,  1.0f,  0.5f,
            // Top face (+Y)
            -0.5f,  1.0f,  0.5f,  0.5f,  1.0f,  0.5f,  0.5f,  1.0f, -0.5f, -0.5f,  1.0f, -0.5f,
            // Bottom face (-Y)
            -0.5f, -1.0f, -0.5f,  0.5f, -1.0f, -0.5f,  0.5f, -1.0f,  0.5f, -0.5f, -1.0f,  0.5f
        };

        float[] normals = {
            // Front
            0, 0, 1,  0, 0, 1,  0, 0, 1,  0, 0, 1,
            // Back
            0, 0, -1, 0, 0, -1, 0, 0, -1, 0, 0, -1,
            // Left
            -1, 0, 0, -1, 0, 0, -1, 0, 0, -1, 0, 0,
            // Right
            1, 0, 0,  1, 0, 0,  1, 0, 0,  1, 0, 0,
            // Top
            0, 1, 0,  0, 1, 0,  0, 1, 0,  0, 1, 0,
            // Bottom
            0, -1, 0, 0, -1, 0, 0, -1, 0, 0, -1, 0
        };

        int[] indices = {
            0, 1, 2, 2, 3, 0,       // Front
            4, 5, 6, 6, 7, 4,       // Back
            8, 9, 10, 10, 11, 8,    // Left
            12, 13, 14, 14, 15, 12, // Right
            16, 17, 18, 18, 19, 16, // Top
            20, 21, 22, 22, 23, 20  // Bottom
        };

        float[] texCoords = new float[positions.length / 3 * 2];
        float[] blockTypes = new float[positions.length / 3];
        playerMesh = new Mesh(positions, texCoords, normals, blockTypes, indices);
    }

    
    public void cleanup() {
        for (ChunkMeshGenerator.ChunkMeshResult result : chunkMeshes.values()) {
            if (result.opaqueMesh != null) result.opaqueMesh.cleanup();
            if (result.translucentMesh != null) result.translucentMesh.cleanup();
        }
        chunkMeshes.clear();
        if (framebuffer != null) framebuffer.cleanup();
        if (postProcessor != null) postProcessor.cleanup();
        if (uiRenderer != null) uiRenderer.cleanup();
        if (highlightMesh != null) highlightMesh.cleanup();
        if (playerMesh != null) playerMesh.cleanup();
        if (previewMesh != null) previewMesh.cleanup();
        if (heldItemMesh != null) heldItemMesh.cleanup();
        
        for (Mesh mesh : itemMeshCache.values()) {
            if (mesh != null) mesh.cleanup();
        }
        itemMeshCache.clear();
        
        if (atlas != null) atlas.cleanup();
        if (blockShader != null) blockShader.cleanup();
    }
}
