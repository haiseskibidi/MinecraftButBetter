package com.za.minecraft.engine.graphics.ui.editor.animation;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.za.minecraft.engine.graphics.Camera;
import com.za.minecraft.engine.graphics.DynamicTextureAtlas;
import com.za.minecraft.engine.graphics.Shader;
import com.za.minecraft.engine.graphics.Mesh;
import com.za.minecraft.engine.graphics.model.*;
import com.za.minecraft.engine.graphics.ui.Hotbar;
import com.za.minecraft.engine.graphics.ui.Screen;
import com.za.minecraft.engine.graphics.ui.UIRenderer;
import com.za.minecraft.engine.core.GameLoop;
import com.za.minecraft.utils.Identifier;
import com.za.minecraft.utils.Logger;
import com.za.minecraft.world.items.Item;
import com.za.minecraft.world.items.ItemRegistry;
import com.za.minecraft.world.items.ItemStack;
import org.joml.Intersectionf;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/**
 * Advanced Animation Editor Screen.
 */
public class AnimationEditorScreen implements Screen {
    
    private record EditorKeyframe(float time, Vector3f pos, Vector3f rot) {}
    
    private static class EditorTrack {
        final List<EditorKeyframe> keyframes = new ArrayList<>();
        void addKey(float time, Vector3f pos, Vector3f rot) {
            keyframes.removeIf(k -> Math.abs(k.time - time) < 0.005f);
            keyframes.add(new EditorKeyframe(time, new Vector3f(pos), new Vector3f(rot)));
            keyframes.sort(Comparator.comparingDouble(k -> k.time));
        }
        void removeNear(float time) {
            keyframes.removeIf(k -> Math.abs(k.time - time) < 0.05f);
        }
        void evaluate(float time, Vector3f outPos, Vector3f outRot) {
            if (keyframes.isEmpty()) return;
            if (keyframes.size() == 1) {
                outPos.set(keyframes.get(0).pos);
                outRot.set(keyframes.get(0).rot);
                return;
            }
            EditorKeyframe first = keyframes.get(0);
            if (time <= first.time) { outPos.set(first.pos); outRot.set(first.rot); return; }
            EditorKeyframe last = keyframes.get(keyframes.size() - 1);
            if (time >= last.time) { outPos.set(last.pos); outRot.set(last.rot); return; }
            for (int i = 0; i < keyframes.size() - 1; i++) {
                EditorKeyframe k1 = keyframes.get(i);
                EditorKeyframe k2 = keyframes.get(i + 1);
                if (time >= k1.time && time <= k2.time) {
                    float t = (time - k1.time) / (k2.time - k1.time);
                    k1.pos.lerp(k2.pos, t, outPos);
                    k1.rot.lerp(k2.rot, t, outRot);
                    return;
                }
            }
        }
    }

    private Viewmodel viewmodel;
    private final ViewmodelRenderer viewmodelRenderer = new ViewmodelRenderer();
    private Shader shader;
    private final Camera camera = new Camera();
    private final Map<String, EditorTrack> tracks = new HashMap<>();
    private final List<ModelNode> allParts = new ArrayList<>();
    private ModelNode selectedPart;
    private int hoveredPartIndex = -1;
    private ItemStack heldStack;

    private float currentTime = 0.0f;
    private boolean isPlaying = false;
    private long lastFrameTime;
    private boolean isScrubbing = false;

    private enum TransformMode { NONE, GRAB, ROTATE }
    private TransformMode currentMode = TransformMode.NONE;
    private char activeAxis = ' '; 
    
    private Vector2f lastMousePos = new Vector2f();
    private Vector3f startAnimTranslation = new Vector3f();
    private Vector3f startAnimRotation = new Vector3f();

    private boolean initialized = false;

    @Override
    public void init(int screenWidth, int screenHeight) {
        if (!initialized) {
            ViewmodelDefinition handsDef = ModelRegistry.getViewmodel(Identifier.of("minecraft:hands"));
            if (handsDef != null) {
                viewmodel = new Viewmodel(handsDef);
                allParts.clear();
                collectParts(viewmodel.root);
                Logger.info("Editor: Initialized with " + allParts.size() + " parts.");
            }
            heldStack = new ItemStack(ItemRegistry.getItem(Identifier.of("minecraft:stone_axe")));
            shader = new Shader("src/main/resources/shaders/viewmodel_vertex.glsl", "src/main/resources/shaders/viewmodel_fragment.glsl");
            camera.getPosition().set(0, 0, 0);
            camera.getRotation().set(0, 0, 0);
            lastFrameTime = System.currentTimeMillis();
            initialized = true;
        }
        camera.updateAspectRatio((float) screenWidth / screenHeight);
    }

    private void collectParts(ModelNode node) {
        if (node != null && node.name != null && !node.name.equals("root")) { 
            allParts.add(node);
            tracks.put(node.name, new EditorTrack());
        }
        for (ModelNode child : node.children) collectParts(child);
    }

    @Override public boolean isScene() { return true; }

    private void update() {
        long now = System.currentTimeMillis();
        float deltaTime = (now - lastFrameTime) / 1000.0f;
        lastFrameTime = now;
        if (isPlaying) {
            currentTime += deltaTime;
            if (currentTime > 1.0f) currentTime = 0.0f;
        }
        for (ModelNode part : allParts) {
            if (currentMode != TransformMode.NONE && part == selectedPart) continue;
            EditorTrack track = tracks.get(part.name);
            if (track != null && !track.keyframes.isEmpty()) track.evaluate(currentTime, part.animTranslation, part.animRotation);
        }
    }

    @Override
    public void render(UIRenderer renderer, int sw, int sh, DynamicTextureAtlas atlas) {
        if (viewmodel == null) return;
        update();
        glClearColor(0.1f, 0.1f, 0.12f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glEnable(GL_DEPTH_TEST);
        glDepthFunc(GL_LESS);
        glDisable(GL_CULL_FACE); 
        
        shader.use();
        atlas.bind();
        shader.setMatrix4f("projection", new Matrix4f().setPerspective((float)Math.toRadians(70.0f), (float)sw/sh, 0.01f, 1000.0f));
        shader.setMatrix4f("view", new Matrix4f().identity()); 
        shader.setVector3f("lightDirection", new Vector3f(0.4f, -0.8f, 0.4f).normalize());
        shader.setVector3f("lightColor", new Vector3f(0.3f, 0.3f, 0.25f));
        shader.setVector3f("ambientLight", new Vector3f(0.8f, 0.8f, 0.85f));
        shader.setFloat("uTime", (float)glfwGetTime());
        shader.setVector3f("uCondition", new Vector3f(0, 0, 0)); 

        if (!viewmodel.root.children.isEmpty() && viewmodel.root.children.get(0).mesh == null) viewmodel.initMeshes(atlas);
        viewmodel.updateHierarchy(new Matrix4f().identity());
        
        renderRecursive(viewmodel.root, atlas);

        glDisable(GL_DEPTH_TEST);
        renderer.setupUIProjection(sw, sh);
        renderUI(renderer, sw, sh, atlas);
    }

    private void renderRecursive(ModelNode node, DynamicTextureAtlas atlas) {
        boolean isSelected = (node == selectedPart);
        if (node.mesh != null) {
            shader.setMatrix4f("model", node.globalMatrix);
            float weight = node.name.contains("hand") ? 1.0f : (node.name.contains("forearm") ? 0.6f : 0.3f);
            
            if (isSelected) {
                shader.setBoolean("isHand", true);
                shader.setFloat("uHandPartWeight", 1.0f);
                shader.setFloat("uMiningHeat", 1.0f);
            } else {
                shader.setBoolean("isHand", weight > 0.01f);
                shader.setFloat("uHandPartWeight", weight);
                shader.setFloat("uMiningHeat", 0.0f);
            }
            node.mesh.render();
            shader.setBoolean("isHand", false);
        }

        if (node.name.equals("item_attachment_r") && heldStack != null) {
            float heat = isSelected ? 1.0f : 0.0f;
            if (isSelected) {
                shader.setBoolean("isHand", true);
                shader.setFloat("uHandPartWeight", 1.0f);
            } else {
                shader.setBoolean("isHand", false);
            }
            viewmodelRenderer.getHeldItemRenderer().render(node.globalMatrix, heldStack, shader, atlas, true, heat);
            shader.setBoolean("isHand", false);
        }

        for (ModelNode child : node.children) renderRecursive(child, atlas);
    }

    private void renderUI(UIRenderer renderer, int sw, int sh, DynamicTextureAtlas atlas) {
        renderer.getFontRenderer().drawString("ANIMATION STUDIO", 20, 20, 24, sw, sh, 1.0f, 0.7f, 0.0f, 1.0f);
        
        // Dev Panel
        int slotSize = (int)(18 * Hotbar.HOTBAR_SCALE);
        int spacing = (int)(2 * Hotbar.HOTBAR_SCALE);
        int devX = sw - (7 * (slotSize + spacing)) - 25;
        renderer.renderDeveloperPanel(devX, 40, slotSize, spacing, sw, sh, atlas);

        if (heldStack != null) {
            int cols = 7;
            float offset = renderer.getDevScroller().getOffset();
            List<Item> items = new ArrayList<>(ItemRegistry.getAllItems().values());
            for (int i = 0; i < items.size(); i++) {
                if (items.get(i).getId() == heldStack.getItem().getId()) {
                    int x = devX + (i % cols) * (slotSize + spacing);
                    int y = 40 + (i / cols) * (slotSize + spacing) - (int)offset;
                    if (y + slotSize > 40 && y < 40 + renderer.getDevScroller().getHeight())
                        renderer.renderHighlight(x, y, slotSize, sw, sh, 0.0f, 1.0f, 0.0f, 0.4f);
                    break;
                }
            }
        }

        // Parts List
        renderer.renderRect(10, 80, 220, sh - 200, sw, sh, 0.05f, 0.05f, 0.05f, 0.9f);
        renderer.getFontRenderer().drawString("PARTS", 20, 90, 18, sw, sh, 0.4f, 0.7f, 1.0f, 1.0f);
        int itemY = 130;
        for (int i = 0; i < allParts.size(); i++) {
            ModelNode part = allParts.get(i);
            boolean sel = (part == selectedPart);
            if (sel) renderer.renderRect(15, itemY - 2, 210, 20, sw, sh, 0.2f, 0.4f, 0.8f, 1.0f);
            else if (i == hoveredPartIndex) renderer.renderRect(15, itemY - 2, 210, 20, sw, sh, 0.2f, 0.2f, 0.2f, 1.0f);
            renderer.getFontRenderer().drawString(part.name, 25, itemY, 14, sw, sh, 1, 1, 1, 1);
            itemY += 22;
        }

        // Properties
        if (selectedPart != null) {
            int px = 240, py = 80;
            renderer.renderRect(px, py, 220, 260, sw, sh, 0.05f, 0.05f, 0.05f, 0.9f);
            renderer.getFontRenderer().drawString("PROPERTIES", px + 10, py + 10, 18, sw, sh, 1, 0.8f, 0, 1);
            renderer.getFontRenderer().drawString(selectedPart.name, px + 10, py + 35, 12, sw, sh, 0.6f, 0.6f, 0.6f, 1);
            int iy = py + 70;
            drawTransformInfo(renderer, "Anim X", selectedPart.animTranslation.x, px + 15, iy, sw, sh);
            drawTransformInfo(renderer, "Anim Y", selectedPart.animTranslation.y, px + 15, iy + 25, sw, sh);
            drawTransformInfo(renderer, "Anim Z", selectedPart.animTranslation.z, px + 15, iy + 50, sw, sh);
            drawTransformInfo(renderer, "Pitch", (float)Math.toDegrees(selectedPart.animRotation.x), px + 15, iy + 90, sw, sh);
            drawTransformInfo(renderer, "Yaw", (float)Math.toDegrees(selectedPart.animRotation.y), px + 15, iy + 115, sw, sh);
            drawTransformInfo(renderer, "Roll", (float)Math.toDegrees(selectedPart.animRotation.z), px + 15, iy + 140, sw, sh);
            renderer.getFontRenderer().drawString("[G] Move | [R] Rot | [K] Key", px + 15, py + 220, 12, sw, sh, 0.5f, 0.5f, 0.5f, 1);
            renderer.getFontRenderer().drawString("[Ctrl+S] Export JSON", px + 15, py + 240, 12, sw, sh, 0.4f, 1, 0.4f, 1);
        }

        // Timeline
        renderer.renderRect(10, sh - 90, sw - 20, 80, sw, sh, 0.05f, 0.05f, 0.05f, 0.9f);
        int bx = 60, bw = sw - 120, by = sh - 45;
        renderer.renderRect(bx, by, bw, 10, sw, sh, 0.2f, 0.2f, 0.2f, 1);
        if (selectedPart != null) {
            EditorTrack track = tracks.get(selectedPart.name);
            if (track != null) for (EditorKeyframe k : track.keyframes) renderer.renderRect(bx + (int)(k.time * bw) - 3, by - 5, 6, 20, sw, sh, 1, 1, 1, 1);
        }
        renderer.renderRect(bx + (int)(currentTime * bw) - 2, by - 15, 4, 40, sw, sh, 1, 0.2f, 0.2f, 1);
        renderer.getFontRenderer().drawString(String.format("%.2f", currentTime), bx + (int)(currentTime * bw) - 10, by + 30, 12, sw, sh, 1, 0.4f, 0.4f, 1);
    }

    private void drawTransformInfo(UIRenderer renderer, String label, float value, int x, int y, int sw, int sh) {
        renderer.getFontRenderer().drawString(label + ":", x, y, 12, sw, sh, 0.6f, 0.6f, 0.6f, 1);
        renderer.getFontRenderer().drawString(String.format("%.3f", value), x + 65, y, 12, sw, sh, 1, 1, 1, 1);
    }

    @Override
    public boolean handleMouseClick(float mx, float my, int button) {
        if (currentMode != TransformMode.NONE) { currentMode = TransformMode.NONE; activeAxis = ' '; return true; }
        int sw = GameLoop.getInstance().getWindow().getWidth(), sh = GameLoop.getInstance().getWindow().getHeight();
        
        Item devItem = getDevItemAt(mx, my, sw, sh);
        if (devItem != null) { heldStack = new ItemStack(devItem); Logger.info("Selected Item: " + devItem.getIdentifier()); return true; }

        if (my >= sh - 100) { isScrubbing = true; currentTime = Math.max(0, Math.min(1, (mx - 60) / (float)(sw - 120))); return true; }
        if (pickPart3D(mx, my)) return true;
        if (mx <= 230 && my >= 120) {
            int idx = (int)((my - 130) / 22);
            if (idx >= 0 && idx < allParts.size()) { selectedPart = allParts.get(idx); return true; }
        }
        return false;
    }

    private Item getDevItemAt(float mx, float my, int sw, int sh) {
        UIRenderer ui = GameLoop.getInstance().getRenderer().getUIRenderer();
        if (!ui.getDevScroller().isMouseOver(mx, my)) return null;
        int cols = 7, slotSize = (int)(18 * Hotbar.HOTBAR_SCALE), spacing = (int)(2 * Hotbar.HOTBAR_SCALE);
        int devX = sw - (cols * (slotSize + spacing)) - 25;
        List<Item> items = new ArrayList<>(ItemRegistry.getAllItems().values());
        float off = ui.getDevScroller().getOffset();
        for (int i = 0; i < items.size(); i++) {
            int x = devX + (i % cols) * (slotSize + spacing), y = 40 + (i / cols) * (slotSize + spacing) - (int)off;
            if (my >= 40 && my <= 40 + ui.getDevScroller().getHeight() && mx >= x && mx <= x + slotSize && my >= y && my <= y + slotSize) return items.get(i);
        }
        return null;
    }

    @Override public void handleMouseRelease(int button) { if (button == 0) isScrubbing = false; }
    @Override public boolean handleScroll(double yoffset) {
        UIRenderer ui = GameLoop.getInstance().getRenderer().getUIRenderer();
        if (ui.getDevScroller().isMouseOver(lastMousePos.x, lastMousePos.y)) { ui.getDevScroller().handleScroll(yoffset); return true; }
        return false;
    }

    @Override
    public void handleMouseMove(float mx, float my) {
        if (isScrubbing) {
            int sw = GameLoop.getInstance().getWindow().getWidth();
            currentTime = Math.max(0, Math.min(1, (mx - 60) / (float)(sw - 120)));
        } else if (currentMode != TransformMode.NONE && selectedPart != null) {
            float dx = (mx - lastMousePos.x) * 0.01f, dy = (my - lastMousePos.y) * 0.01f;
            if (currentMode == TransformMode.GRAB) {
                if (activeAxis == 'X' || activeAxis == ' ') selectedPart.animTranslation.x += dx;
                if (activeAxis == 'Y' || activeAxis == ' ') selectedPart.animTranslation.y -= dy;
                if (activeAxis == 'Z') selectedPart.animTranslation.z += dy;
            } else {
                float s = 5.0f;
                if (activeAxis == 'X' || activeAxis == ' ') selectedPart.animRotation.x -= dy * s;
                if (activeAxis == 'Y' || activeAxis == ' ') selectedPart.animRotation.y += dx * s;
                if (activeAxis == 'Z') selectedPart.animRotation.z += dx * s;
            }
        }
        lastMousePos.set(mx, my);
        hoveredPartIndex = (mx <= 230 && my >= 130) ? (int)((my - 130) / 22) : -1;
        if (hoveredPartIndex >= allParts.size()) hoveredPartIndex = -1;
    }

    private boolean pickPart3D(float mx, float my) {
        int sw = GameLoop.getInstance().getWindow().getWidth(), sh = GameLoop.getInstance().getWindow().getHeight();
        Matrix4f projection = new Matrix4f().setPerspective((float)Math.toRadians(70.0f), (float)sw/sh, 0.01f, 1000.0f);
        Matrix4f inv = new Matrix4f(projection).invert();

        float nx = (2.0f * mx / sw) - 1.0f;
        float ny = 1.0f - (2.0f * my / sh);
        Vector4f near = new Vector4f(nx, ny, -1.0f, 1.0f).mul(inv); near.div(near.w);
        Vector4f far = new Vector4f(nx, ny, 1.0f, 1.0f).mul(inv); far.div(far.w);

        Vector3f ro = new Vector3f(near.x, near.y, near.z);
        Vector3f rd = new Vector3f(far.x - near.x, far.y - near.y, far.z - near.z).normalize();
        
        ModelNode best = null; float minD = Float.MAX_VALUE;
        DynamicTextureAtlas atlas = GameLoop.getInstance().getRenderer().getAtlas();

        for (ModelNode p : allParts) {
            // Case 1: Held Item
            if (p.name.equals("item_attachment_r") && heldStack != null) {
                Mesh mesh = viewmodelRenderer.getHeldItemRenderer().getOrGenerateMesh(heldStack.getItem(), atlas);
                if (mesh != null) {
                    Matrix4f itemLocal = new Matrix4f();
                    Item item = heldStack.getItem();
                    // Sync with HeldItemRenderer logic
                    if (item.isBlock()) {
                        itemLocal.translate(0, 0.15f, -mesh.getMax().z * 0.4f)
                                 .rotateX((float)Math.toRadians(30)).rotateY((float)Math.toRadians(15)).scale(0.4f);
                    } else {
                        Vector3f go = mesh.getGraspOffset();
                        itemLocal.translate(0, -0.1f, 0).rotateY((float)Math.toRadians(-90)).scale(0.85f).translate(-go.x, -go.y, -go.z);
                    }
                    Matrix4f itemWorld = new Matrix4f(p.globalMatrix).mul(itemLocal);
                    Matrix4f invItem = new Matrix4f(itemWorld).invert();
                    Vector3f lro = ro.mulPosition(invItem, new Vector3f());
                    Vector3f lrd = rd.mulDirection(invItem, new Vector3f());
                    Vector2f res = new Vector2f();
                    if (Intersectionf.intersectRayAab(lro, lrd, mesh.getMin(), mesh.getMax(), res)) {
                        float t = res.x < 0 ? res.y : res.x;
                        if (t > 0 && t < minD) {
                            minD = t;
                            best = p;
                        }
                    }
                }
            }

            // Case 2: Body Parts
            if (p.def == null || p.def.cubes == null) continue;
            Matrix4f invG = new Matrix4f(p.globalMatrix).invert();
            Vector3f lro = ro.mulPosition(invG, new Vector3f());
            Vector3f lrd = rd.mulDirection(invG, new Vector3f());
            float px = p.def.pivot[0], py = p.def.pivot[1], pz = p.def.pivot[2];

            for (BoneDefinition.CubeDefinition c : p.def.cubes) {
                Vector3f min = new Vector3f((c.origin[0] - px)/16f, (c.origin[1] - py)/16f, (c.origin[2] - pz)/16f);
                Vector3f max = new Vector3f(min).add(new Vector3f(c.size[0]/16f, c.size[1]/16f, c.size[2]/16f));
                Vector2f res = new Vector2f();
                if (Intersectionf.intersectRayAab(lro, lrd, min, max, res)) {
                    float t = res.x < 0 ? res.y : res.x;
                    if (t > 0 && t < minD) {
                        minD = t;
                        best = p;
                    }
                }
            }
        }

        if (best != null) {
            selectedPart = best;
            return true;
        }
        return false;
    }

    @Override
    public boolean handleKeyPress(int key) {
        if (key == GLFW_KEY_S && glfwGetKey(GameLoop.getInstance().getWindow().getWindowHandle(), GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS) { saveAnimation(); return true; }
        if (key == GLFW_KEY_ESCAPE || key == GLFW_KEY_F8) {
            if (currentMode != TransformMode.NONE) { selectedPart.animTranslation.set(startAnimTranslation); selectedPart.animRotation.set(startAnimRotation); currentMode = TransformMode.NONE; return true; }
            GameLoop.getInstance().toggleAnimationEditor(); return true;
        }
        if (key == GLFW_KEY_SPACE) { isPlaying = !isPlaying; return true; }
        if (selectedPart != null) {
            if (key == GLFW_KEY_G) { currentMode = TransformMode.GRAB; startAnimTranslation.set(selectedPart.animTranslation); startAnimRotation.set(selectedPart.animRotation); return true; }
            if (key == GLFW_KEY_R) { currentMode = TransformMode.ROTATE; startAnimTranslation.set(selectedPart.animTranslation); startAnimRotation.set(selectedPart.animRotation); return true; }
            if (key == GLFW_KEY_K) { tracks.get(selectedPart.name).addKey(currentTime, selectedPart.animTranslation, selectedPart.animRotation); return true; }
            if (key == GLFW_KEY_DELETE) { tracks.get(selectedPart.name).removeNear(currentTime); return true; }
            if (currentMode != TransformMode.NONE) {
                if (key == GLFW_KEY_X) { activeAxis = 'X'; return true; }
                if (key == GLFW_KEY_Y) { activeAxis = 'Y'; return true; }
                if (key == GLFW_KEY_Z) { activeAxis = 'Z'; return true; }
            }
        }
        return false;
    }

    private void saveAnimation() {
        Map<String, Object> root = new HashMap<>(); root.put("looping", false); root.put("duration", 1.0);
        Map<String, List<List<Object>>> jsonTracks = new LinkedHashMap<>();
        for (Map.Entry<String, EditorTrack> entry : tracks.entrySet()) {
            if (entry.getValue().keyframes.isEmpty()) continue;
            String prefix = entry.getKey().contains("attachment") ? "item_" : entry.getKey() + ":";
            addTracksForBone(jsonTracks, prefix, entry.getValue());
        }
        root.put("tracks", jsonTracks);
        try (FileWriter writer = new FileWriter("src/main/resources/minecraft/animations/editor_output.json")) {
            writer.write(new GsonBuilder().setPrettyPrinting().create().toJson(root));
            Logger.info("Saved to resources.");
        } catch (IOException e) { Logger.error(e.getMessage()); }
    }

    private void addTracksForBone(Map<String, List<List<Object>>> jsonTracks, String prefix, EditorTrack track) {
        String[] suffixes = {"x", "y", "z", "pitch", "yaw", "roll"};
        for (String suffix : suffixes) {
            List<List<Object>> keyList = new ArrayList<>();
            for (EditorKeyframe k : track.keyframes) {
                float val = suffix.equals("x")?k.pos.x*16:suffix.equals("y")?k.pos.y*16:suffix.equals("z")?k.pos.z*16:
                            suffix.equals("pitch")?(float)Math.toDegrees(k.rot.x):suffix.equals("yaw")?(float)Math.toDegrees(k.rot.y):(float)Math.toDegrees(k.rot.z);
                List<Object> key = new ArrayList<>(); key.add(k.time); key.add(val); key.add("smootherstep"); keyList.add(key);
            }
            jsonTracks.put(prefix + suffix, keyList);
        }
    }
    public void cleanup() { if (shader != null) shader.cleanup(); if (viewmodel != null) viewmodel.cleanup(); }
}
