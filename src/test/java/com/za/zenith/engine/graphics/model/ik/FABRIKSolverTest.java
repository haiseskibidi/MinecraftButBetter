package com.za.zenith.engine.graphics.model.ik;

import com.za.zenith.engine.graphics.model.BoneDefinition;
import com.za.zenith.engine.graphics.model.ModelNode;
import com.za.zenith.engine.graphics.model.ik.constraints.HingeConstraint;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class FABRIKSolverTest {

    @Test
    public void testBasicIK() {
        // 1. Setup a simple 3-node chain (Length 1.0 each)
        BoneDefinition def1 = new BoneDefinition();
        def1.name = "root"; def1.pivot = new float[]{0, 0, 0};
        
        BoneDefinition def2 = new BoneDefinition();
        def2.name = "mid"; def2.pivot = new float[]{0, 16, 0}; // 1.0 unit up
        
        BoneDefinition def3 = new BoneDefinition();
        def3.name = "end"; def3.pivot = new float[]{0, 16, 0}; // 1.0 unit up from mid
        
        ModelNode n1 = new ModelNode("root", def1);
        ModelNode n2 = new ModelNode("mid", def2);
        ModelNode n3 = new ModelNode("end", def3);
        
        n1.children.add(n2);
        n2.children.add(n3);
        
        // Initial hierarchy update
        n1.updateGlobalMatrix(new Matrix4f().identity());
        
        IKChain chain = new IKChain(Arrays.asList(n1, n2, n3));
        
        // 2. Set target (Reachable)
        chain.targetPosition.set(0.5f, 0.5f, 0); // Somewhere diagonal
        chain.tolerance = 0.001f;
        
        // 3. Solve
        FABRIKSolver.solve(chain);
        
        // 4. Update hierarchy to see if rotations worked
        n1.updateGlobalMatrix(new Matrix4f().identity());
        
        Vector3f finalPos = new Vector3f(n3.globalMatrix.m30(), n3.globalMatrix.m31(), n3.globalMatrix.m32());
        float dist = finalPos.distance(chain.targetPosition);
        
        System.out.println("Final end effector position: " + finalPos);
        System.out.println("Distance to target: " + dist);
        
        assertTrue(dist < 0.05f, "End effector should be close to target. Dist: " + dist);
    }

    @Test
    public void testHingeConstraint() {
        // 1. Setup a simple 3-node chain (Length 1.0 each)
        BoneDefinition def1 = new BoneDefinition();
        def1.name = "root"; def1.pivot = new float[]{0, 0, 0};
        
        BoneDefinition def2 = new BoneDefinition();
        def2.name = "mid"; def2.pivot = new float[]{0, 16, 0}; // points +Y
        
        BoneDefinition def3 = new BoneDefinition();
        def3.name = "end"; def3.pivot = new float[]{0, 16, 0}; // points +Y from mid
        
        ModelNode n1 = new ModelNode("root", def1);
        ModelNode n2 = new ModelNode("mid", def2);
        ModelNode n3 = new ModelNode("end", def3);
        
        n1.children.add(n2);
        n2.children.add(n3);
        
        n1.updateGlobalMatrix(new Matrix4f().identity());
        IKChain chain = new IKChain(Arrays.asList(n1, n2, n3));

        // 2. Add Hinge: can only bend around X axis, between 0 and 90 degrees
        // (Meaning it can only bend "forward" in the XY plane)
        chain.constraints.put(n2, new HingeConstraint(new Vector3f(1, 0, 0), 0, 90));

        // 3. Set target that would force a backward bend (e.g. x=0, y=0.5)
        // If it was free, it would just fold back. With hinge 0-90, it should stay at 0 or 90.
        chain.targetPosition.set(0, 0.5f, 0); 
        
        FABRIKSolver.solve(chain);
        n1.updateGlobalMatrix(new Matrix4f().identity());

        float angle = (float) Math.toDegrees(n2.animRotation.x);
        System.out.println("Hinge angle: " + angle);
        
        assertTrue(angle >= -0.01f && angle <= 90.01f, "Angle should be clamped between 0 and 90. Got: " + angle);
    }
}


