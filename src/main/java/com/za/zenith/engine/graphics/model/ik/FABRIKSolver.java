package com.za.zenith.engine.graphics.model.ik;

import com.za.zenith.engine.graphics.model.ModelNode;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Implementation of the Forward And Backward Reaching Inverse Kinematics algorithm.
 */
public class FABRIKSolver {

    public static void solve(IKChain chain) {
        if (chain.nodes.size() < 2) return;

        int n = chain.nodes.size();
        Vector3f[] positions = new Vector3f[n];
        
        // 1. Get current world positions
        for (int i = 0; i < n; i++) {
            Matrix4f m = chain.nodes.get(i).globalMatrix;
            positions[i] = new Vector3f(m.m30(), m.m31(), m.m32());
        }

        Vector3f rootPos = new Vector3f(positions[0]);
        float totalLength = chain.getTotalLength();
        float distToTarget = rootPos.distance(chain.targetPosition);

        // 2. Check if target is out of reach
        if (distToTarget > totalLength) {
            Vector3f dir = new Vector3f(chain.targetPosition).sub(rootPos).normalize();
            for (int i = 0; i < n - 1; i++) {
                positions[i + 1].set(dir).mul(chain.lengths.get(i)).add(positions[i]);
            }
        } else {
            // 3. Iterative FABRIK
            for (int iter = 0; iter < chain.maxIterations; iter++) {
                if (positions[n - 1].distance(chain.targetPosition) < chain.tolerance) break;

                // Forward Pass: Effector to Root
                positions[n - 1].set(chain.targetPosition);
                for (int i = n - 2; i >= 0; i--) {
                    float d = positions[i].distance(positions[i + 1]);
                    if (d < 0.00001f) continue;
                    float r = chain.lengths.get(i) / d;
                    // Formula: P_i = P_{i+1} + (P_i - P_{i+1}) * r
                    Vector3f diff = new Vector3f(positions[i]).sub(positions[i + 1]);
                    positions[i].set(positions[i + 1]).add(diff.mul(r));
                }

                // Backward Pass: Root to Effector
                positions[0].set(rootPos);
                for (int i = 0; i < n - 1; i++) {
                    float d = positions[i + 1].distance(positions[i]);
                    if (d < 0.00001f) continue;
                    float r = chain.lengths.get(i) / d;
                    positions[i + 1].set(positions[i]).add(new Vector3f(positions[i + 1]).sub(positions[i]).mul(r));

                    // --- APPLY CONSTRAINTS ---
                    com.za.zenith.engine.graphics.model.ik.constraints.IKConstraint constraint = chain.constraints.get(chain.nodes.get(i));
                    if (constraint != null) {
                        constraint.apply(chain.nodes.get(i), positions[i + 1], positions[i]);
                    }
                }
            }

            // --- POLE TARGET BENDING ---
            // After FABRIK iterations, we bend the joint towards the Pole Target
            if (chain.hasPole && n > 2) {
                for (int i = 1; i < n - 1; i++) {
                    Vector3f root = positions[i - 1];
                    Vector3f end = positions[i + 1];
                    Vector3f current = positions[i];

                    // Find projection of current joint onto the root-end line
                    Vector3f line = new Vector3f(end).sub(root);
                    float lineLen = line.length();
                    if (lineLen < 0.0001f) continue;
                    line.normalize();

                    Vector3f toCurrent = new Vector3f(current).sub(root);
                    float dot = toCurrent.dot(line);
                    Vector3f projection = new Vector3f(line).mul(dot).add(root);

                    // Current bend direction
                    Vector3f currentBend = new Vector3f(current).sub(projection);
                    float bendLen = currentBend.length();
                    if (bendLen < 0.0001f) continue;

                    // Desired bend direction (towards Pole Target)
                    Vector3f targetBend = new Vector3f(chain.poleTarget).sub(projection);
                    // Project targetBend onto the plane normal to the line
                    float targetDot = targetBend.dot(line);
                    targetBend.fma(-targetDot, line); 

                    if (targetBend.length() > 0.0001f) {
                        targetBend.normalize().mul(bendLen);
                        positions[i].set(projection).add(targetBend);
                    }
                }
            }
        }

        // 4. Update rotations (Baking)
        updateRotations(chain, positions);
    }

    private static void updateRotations(IKChain chain, Vector3f[] solvedPositions) {
        for (int i = 0; i < chain.nodes.size() - 1; i++) {
            ModelNode node = chain.nodes.get(i);
            ModelNode next = chain.nodes.get(i + 1);

            Quaternionf parentWorldRot = new Quaternionf();
            Matrix4f mParent = new Matrix4f();
            node.globalMatrix.mul(new Matrix4f(node.localMatrix).invert(), mParent);
            mParent.getNormalizedRotation(parentWorldRot);

            Vector3f desiredDirWorld = new Vector3f(solvedPositions[i + 1]).sub(solvedPositions[i]).normalize();
            Vector3f localBoneDir = new Vector3f(next.basePivot).normalize();

            Quaternionf baseRot = new Quaternionf().rotationXYZ(node.baseRotation.x, node.baseRotation.y, node.baseRotation.z);
            Quaternionf preRot = new Quaternionf(parentWorldRot).mul(baseRot);
            
            Vector3f desiredDirLocal = new Vector3f(desiredDirWorld);
            preRot.invert().transform(desiredDirLocal);

            Quaternionf animRot = new Quaternionf().rotateTo(localBoneDir, desiredDirLocal);
            animRot.getEulerAnglesXYZ(node.animRotation);
            
            node.updateGlobalMatrix(mParent);
        }
    }
}


