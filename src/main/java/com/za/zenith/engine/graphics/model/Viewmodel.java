package com.za.zenith.engine.graphics.model;

import com.za.zenith.engine.graphics.DynamicTextureAtlas;
import org.joml.Matrix4f;
import java.util.HashMap;
import java.util.Map;
import java.util.List;

public class Viewmodel {
    public final ViewmodelDefinition def;
    public final ModelNode root;
    private final Map<String, ModelNode> nodesMap = new HashMap<>();

    public Viewmodel(ViewmodelDefinition def) {
        this.def = def;
        this.root = new ModelNode("root", null);
        
        // 1. Create all nodes
        for (BoneDefinition bone : def.bones) {
            ModelNode node = new ModelNode(bone.name, bone);
            nodesMap.put(bone.name, node);
        }
        
        // 2. Build tree structure
        for (BoneDefinition bone : def.bones) {
            ModelNode node = nodesMap.get(bone.name);
            
            // Конвертация: в OpenGL Z направлен НА игрока, а в JSON - ОТ игрока.
            // Поэтому инвертируем Z.
            node.basePivot.set(bone.x / 16.0f, bone.y / 16.0f, -bone.z / 16.0f);

            if (bone.parent != null && nodesMap.containsKey(bone.parent)) {
                nodesMap.get(bone.parent).children.add(node);
            } else {
                root.children.add(node);
            }
        }
    }

    public ModelNode getNode(String name) {
        return nodesMap.get(name);
    }

    public java.util.List<ModelNode> getAllNodes() {
        return new java.util.ArrayList<>(nodesMap.values());
    }

    public void updateHierarchy(Matrix4f baseMatrix) {
        root.updateGlobalMatrix(baseMatrix);
    }

    public void initMeshes(DynamicTextureAtlas atlas) {
        String texturePath = def.texture != null ? def.texture : "zenith/textures/default.png";
        for (ModelNode node : nodesMap.values()) {
            if (node.def != null && node.def.cubes != null && !node.def.cubes.isEmpty()) {
                node.mesh = ViewmodelMeshGenerator.generateBoneMesh(node.def, texturePath, atlas);
            }
        }
    }

    public void cleanup() {
        root.cleanup();
    }
}
