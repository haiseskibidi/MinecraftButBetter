package com.za.zenith.engine.graphics.model.ik;

import com.za.zenith.engine.graphics.model.ModelNode;
import com.za.zenith.engine.graphics.model.ik.constraints.IKConstraint;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Container for a sequence of bones to be solved by IK.
 */
public class IKChain {
    public final List<ModelNode> nodes = new ArrayList<>();
    public final List<Float> lengths = new ArrayList<>();
    public final Map<ModelNode, IKConstraint> constraints = new HashMap<>();
    
    public final Vector3f targetPosition = new Vector3f();
    public final Vector3f poleTarget = new Vector3f();
    public boolean hasPole = false;
    
    public float tolerance = 0.001f;
    public int maxIterations = 15;
    
    public IKChain(List<ModelNode> nodes) {
        this.nodes.addAll(nodes);
        calculateLengths();
    }
    
    public void calculateLengths() {
        lengths.clear();
        for (int i = 0; i < nodes.size() - 1; i++) {
            ModelNode current = nodes.get(i);
            ModelNode next = nodes.get(i + 1);
            // In our system, the distance between nodes is effectively the pivot of the child
            // because ModelNode.basePivot is relative to parent.
            lengths.add(next.basePivot.length());
        }
    }
    
    public float getTotalLength() {
        float sum = 0;
        for (float l : lengths) sum += l;
        return sum;
    }
}


