package com.za.minecraft.engine.graphics.model;

import com.za.minecraft.engine.graphics.Mesh;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.ArrayList;
import java.util.List;

public class ModelNode {
    public final String name;
    public final BoneDefinition def;
    public final List<ModelNode> children = new ArrayList<>();
    public Mesh mesh; // Set by ViewmodelRenderer
    
    // Base transform from definition
    public final Vector3f basePivot;
    public final Vector3f baseRotation; // in radians
    
    // Animation overrides
    public final Vector3f animTranslation = new Vector3f();
    public final Vector3f animRotation = new Vector3f(); // in radians
    
    // Computed matrices
    public final Matrix4f localMatrix = new Matrix4f();
    public final Matrix4f globalMatrix = new Matrix4f();
    
    public ModelNode(String name, BoneDefinition def) {
        this.name = name;
        this.def = def;
        this.basePivot = def != null && def.pivot != null ? new Vector3f(def.pivot[0] / 16.0f, def.pivot[1] / 16.0f, def.pivot[2] / 16.0f) : new Vector3f();
        this.baseRotation = def != null && def.rotation != null ? 
            new Vector3f((float)Math.toRadians(def.rotation[0]), (float)Math.toRadians(def.rotation[1]), (float)Math.toRadians(def.rotation[2])) : new Vector3f();
    }
    
    public void updateGlobalMatrix(Matrix4f parentGlobal) {
        // Local matrix: Translation to parent-relative origin + rotation
        localMatrix.identity()
            .translate(basePivot)
            .translate(animTranslation)
            .rotateX(baseRotation.x + animRotation.x)
            .rotateY(baseRotation.y + animRotation.y)
            .rotateZ(baseRotation.z + animRotation.z);
            
        if (parentGlobal != null) {
            parentGlobal.mul(localMatrix, globalMatrix);
        } else {
            globalMatrix.set(localMatrix);
        }
        
        for (ModelNode child : children) {
            child.updateGlobalMatrix(globalMatrix);
        }
    }

    public void cleanup() {
        if (mesh != null) {
            mesh.cleanup();
            mesh = null;
        }
        for (ModelNode child : children) {
            child.cleanup();
        }
    }
}
