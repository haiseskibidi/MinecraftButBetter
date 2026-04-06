package com.za.minecraft.engine.graphics.model.ik.constraints;

import com.za.minecraft.engine.graphics.model.ModelNode;
import org.joml.Vector3f;

/**
 * Interface for limiting joint rotations during IK solving.
 */
public interface IKConstraint {
    /**
     * Corrects the position of the child node relative to its parent.
     * @param node The node whose rotation is being constrained.
     * @param currentPos The solved world position of the node.
     * @param parentPos The solved world position of the parent.
     */
    void apply(ModelNode node, Vector3f currentPos, Vector3f parentPos);
}
