package com.za.minecraft.engine.graphics.model;

import com.za.minecraft.engine.graphics.DynamicTextureAtlas;
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
        this.root = new ModelNode("root", null); // Abstract root
        
        // Build the tree
        Map<String, ModelNode> tempNodes = new HashMap<>();
        for (BoneDefinition bone : def.bones) {
            ModelNode node = new ModelNode(bone.name, bone);
            tempNodes.put(bone.name, node);
            nodesMap.put(bone.name, node);
        }
        
        for (BoneDefinition bone : def.bones) {
            ModelNode node = tempNodes.get(bone.name);
            if (bone.parent != null && tempNodes.containsKey(bone.parent)) {
                ModelNode parentNode = tempNodes.get(bone.parent);
                parentNode.children.add(node);
                
                // Calculate relative pivot: this bone's pivot minus parent's pivot
                if (bone.pivot != null && parentNode.def.pivot != null) {
                    node.basePivot.set(
                        (bone.pivot[0] - parentNode.def.pivot[0]) / 16.0f,
                        (bone.pivot[1] - parentNode.def.pivot[1]) / 16.0f,
                        (bone.pivot[2] - parentNode.def.pivot[2]) / 16.0f
                    );
                }
            } else {
                root.children.add(node);
                // For root children, the pivot is already relative to [0,0,0]
                if (bone.pivot != null) {
                    node.basePivot.set(bone.pivot[0] / 16.0f, bone.pivot[1] / 16.0f, bone.pivot[2] / 16.0f);
                }
            }
        }
    }

    public ModelNode getNode(String name) {
        return nodesMap.get(name);
    }

    public List<ModelNode> getAllNodes() {
        return new java.util.ArrayList<>(nodesMap.values());
    }
    
    public void initMeshes(DynamicTextureAtlas atlas) {
        String texturePath = def.texture != null ? def.texture : "minecraft/textures/entity/hands.png";
        if (atlas.uvFor(texturePath) == null) {
            texturePath = "minecraft/textures/default.png";
        }
        for (ModelNode node : nodesMap.values()) {
            if (node.def != null && node.def.cubes != null && !node.def.cubes.isEmpty()) {
                node.mesh = ViewmodelMeshGenerator.generateBoneMesh(node.def, texturePath, atlas);
            }
        }
    }
    
    public void updateHierarchy(Matrix4f baseMatrix) {
        root.updateGlobalMatrix(baseMatrix);
    }

    public void cleanup() {
        root.cleanup();
    }
}
