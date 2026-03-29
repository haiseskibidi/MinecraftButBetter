package com.za.minecraft.engine.graphics.model;

import org.joml.Vector3f;
import org.joml.Matrix4f;

public class TwoBoneIK {
    /**
     * Solves IK for a two-bone chain.
     * @param shoulder The root bone of the chain.
     * @param forearm The middle bone of the chain.
     * @param targetLocal Target position relative to the root's parent space.
     * @param l1 Length of the first bone.
     * @param l2 Length of the second bone.
     * @param flip Invert the elbow direction.
     */
    public static void solve(ModelNode shoulder, ModelNode forearm, Vector3f targetLocal, float l1, float l2, boolean flip) {
        // Distance to target
        float dist = targetLocal.length();
        dist = Math.max(0.01f, Math.min(dist, l1 + l2 - 0.0001f));

        // Law of cosines for the interior angle at the elbow
        // dist^2 = l1^2 + l2^2 - 2*l1*l2 * cos(beta)
        float cosBeta = (l1 * l1 + l2 * l2 - dist * dist) / (2 * l1 * l2);
        float beta = (float) Math.acos(Math.max(-1, Math.min(1, cosBeta)));

        // Forearm rotation (local)
        // We assume bending is on the X axis
        forearm.animRotation.x = (float) Math.PI - beta;
        if (flip) forearm.animRotation.x = -forearm.animRotation.x;

        // Shoulder rotation to point at target
        // Interior angle at shoulder (alpha)
        // l2^2 = l1^2 + dist^2 - 2*l1*dist * cos(alpha)
        float cosAlpha = (l1 * l1 + dist * dist - l2 * l2) / (2 * l1 * dist);
        float alpha = (float) Math.acos(Math.max(-1, Math.min(1, cosAlpha)));

        // Base orientation towards target
        float pitch = (float) Math.atan2(-targetLocal.y, Math.sqrt(targetLocal.x * targetLocal.x + targetLocal.z * targetLocal.z));
        float yaw = (float) Math.atan2(targetLocal.x, targetLocal.z);

        shoulder.animRotation.x = pitch + (flip ? alpha : -alpha);
        shoulder.animRotation.y = yaw;
        shoulder.animRotation.z = 0;
    }
}
