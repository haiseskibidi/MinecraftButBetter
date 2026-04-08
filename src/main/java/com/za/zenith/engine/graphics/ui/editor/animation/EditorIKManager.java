package com.za.zenith.engine.graphics.ui.editor.animation;

import com.za.zenith.engine.graphics.model.ModelNode;
import com.za.zenith.engine.graphics.model.ik.FABRIKSolver;
import com.za.zenith.engine.graphics.model.ik.IKChain;
import com.za.zenith.engine.graphics.model.ik.constraints.HingeConstraint;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Universal IK Manager for Animation Editor.
 * Supports multiple concurrent chains (e.g., both hands and feet).
 */
public class EditorIKManager {
    private final Map<String, IKChain> chains = new HashMap<>();
    private final Map<String, Vector3f> targetWorldPositions = new HashMap<>();
    private final Map<String, Vector3f> poleWorldPositions = new HashMap<>();
    private boolean enabled = false;

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public Map<String, IKChain> getChains() { return chains; }
    public Vector3f getTargetPos(String effectorName) { return targetWorldPositions.get(effectorName); }
    public Vector3f getPolePos(String effectorName) { return poleWorldPositions.get(effectorName); }

    /**
     * Automatically detect and setup IK chains for hands and feet.
     */
    public void autoSetup(ModelNode root) {
        chains.clear();
        targetWorldPositions.clear();
        poleWorldPositions.clear();

        // Search for common effectors
        setupChainForEffector(root, "hand_r", 3);
        setupChainForEffector(root, "hand_l", 3);
        setupChainForEffector(root, "foot_r", 3);
        setupChainForEffector(root, "foot_l", 3);
    }

    private void setupChainForEffector(ModelNode root, String effectorName, int length) {
        ModelNode effector = findNode(root, effectorName);
        if (effector == null) return;

        List<ModelNode> nodes = new ArrayList<>();
        ModelNode current = effector;
        for (int i = 0; i < length && current != null; i++) {
            nodes.add(current);
            if (current == root) break;
            current = findNodeParent(root, current);
        }
        Collections.reverse(nodes);

        if (nodes.size() >= 2) {
            IKChain chain = new IKChain(nodes);
            Matrix4f m = effector.globalMatrix;
            Vector3f tPos = new Vector3f(m.m30(), m.m31(), m.m32());
            
            chains.put(effectorName, chain);
            targetWorldPositions.put(effectorName, tPos);

            // Setup Pole Target
            ModelNode mid = nodes.get(nodes.size() / 2);
            Matrix4f midM = mid.globalMatrix;
            Vector3f pPos = new Vector3f(midM.m30(), midM.m31(), midM.m32() - 0.5f);
            poleWorldPositions.put(effectorName, pPos);
            chain.hasPole = true;

            // Constraints (Hinge for elbows/knees)
            for (int i = 0; i < nodes.size() - 1; i++) {
                ModelNode n = nodes.get(i);
                if (n.name.toLowerCase().contains("forearm") || n.name.toLowerCase().contains("calf")) {
                    chain.constraints.put(n, new HingeConstraint(new Vector3f(1, 0, 0), -160, 0));
                }
            }
        }
    }

    private ModelNode findNode(ModelNode root, String name) {
        if (root.name.equals(name)) return root;
        for (ModelNode child : root.children) {
            ModelNode found = findNode(child, name);
            if (found != null) return found;
        }
        return null;
    }

    private ModelNode findNodeParent(ModelNode root, ModelNode target) {
        for (ModelNode child : root.children) {
            if (child == target) return root;
            ModelNode p = findNodeParent(child, target);
            if (p != null) return p;
        }
        return null;
    }

    public void update(ModelNode root) {
        if (!enabled || chains.isEmpty()) return;

        for (Map.Entry<String, IKChain> entry : chains.entrySet()) {
            String name = entry.getKey();
            IKChain chain = entry.getValue();
            
            chain.targetPosition.set(targetWorldPositions.get(name));
            if (chain.hasPole) {
                chain.poleTarget.set(poleWorldPositions.get(name));
            }
            
            FABRIKSolver.solve(chain);
        }
        
        // Final update for the entire model hierarchy
        root.updateGlobalMatrix(new Matrix4f().identity());
    }

    public void bakeToKeyframes(AnimationEditorState state) {
        for (IKChain chain : chains.values()) {
            for (ModelNode node : chain.nodes) {
                AnimationEditorState.EditorTrack track = state.tracks.computeIfAbsent(node.name, k -> new AnimationEditorState.EditorTrack());
                track.addKey(state.currentTime, node.animTranslation, node.animRotation);
            }
        }
    }
}


