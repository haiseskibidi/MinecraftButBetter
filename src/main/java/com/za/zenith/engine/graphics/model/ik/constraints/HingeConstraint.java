package com.za.zenith.engine.graphics.model.ik.constraints;

import com.za.zenith.engine.graphics.model.ModelNode;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Constrains a joint to rotate only around a specific axis within a given angle range.
 */
public class HingeConstraint implements IKConstraint {
    private final Vector3f axis;
    private final float minAngle; // in radians
    private final float maxAngle; // in radians

    public HingeConstraint(Vector3f axis, float minAngleDeg, float maxAngleDeg) {
        this.axis = new Vector3f(axis).normalize();
        this.minAngle = (float) Math.toRadians(minAngleDeg);
        this.maxAngle = (float) Math.toRadians(maxAngleDeg);
    }

    @Override
    public void apply(ModelNode node, Vector3f currentPos, Vector3f parentPos) {
        // Find the child node to get the default bone direction
        // In our FABRIK implementation, 'node' is the parent of the segment being solved.
        // We assume the first child is the one in the chain.
        if (node.children.isEmpty()) return;
        ModelNode next = node.children.get(0); // Simplified assumption for MVP

        // 1. Vector from parent to current in world space
        Vector3f boneVecWorld = new Vector3f(currentPos).sub(parentPos);
        float length = boneVecWorld.length();
        if (length < 0.0001f) return;
        boneVecWorld.normalize();

        // 2. Transform bone vector to node's local space (before its own rotation)
        Matrix4f mParent = new Matrix4f();
        node.globalMatrix.mul(new Matrix4f(node.localMatrix).invert(), mParent);
        Quaternionf parentRot = new Quaternionf();
        mParent.getNormalizedRotation(parentRot);
        Quaternionf baseRot = new Quaternionf().rotationXYZ(node.baseRotation.x, node.baseRotation.y, node.baseRotation.z);
        Quaternionf preRot = new Quaternionf(parentRot).mul(baseRot);

        Vector3f boneVecLocal = new Vector3f(boneVecWorld);
        preRot.invert().transform(boneVecLocal);

        // 3. Project boneVecLocal onto the plane perpendicular to the hinge axis
        // And remove any component along the axis
        float dot = boneVecLocal.dot(axis);
        boneVecLocal.sub(new Vector3f(axis).mul(dot)).normalize();

        // 4. Calculate angle relative to default direction
        Vector3f defaultDir = new Vector3f(next.basePivot).normalize();
        
        // Project defaultDir onto same plane too (it should already be there if axis is perpendicular)
        Vector3f refDir = new Vector3f(defaultDir);
        float dDot = refDir.dot(axis);
        refDir.sub(new Vector3f(axis).mul(dDot)).normalize();

        // Calculate signed angle around axis
        Vector3f cross = new Vector3f(refDir).cross(boneVecLocal);
        float angle = (float) Math.atan2(cross.dot(axis), refDir.dot(boneVecLocal));

        // 5. Clamp angle
        float clamped = Math.max(minAngle, Math.min(maxAngle, angle));

        // 6. Reconstruct local vector
        Quaternionf clampRot = new Quaternionf().setAngleAxis(clamped, axis.x, axis.y, axis.z);
        boneVecLocal.set(refDir);
        clampRot.transform(boneVecLocal);

        // 7. Back to world space and update currentPos
        preRot.transform(boneVecLocal);
        currentPos.set(boneVecLocal).mul(length).add(parentPos);
    }
}


